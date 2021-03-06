package com.hwx.chatique.flow.video

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.hardware.camera2.*
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.media.Image
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import android.view.TextureView
import androidx.core.app.ActivityCompat
import com.hwx.chatique.flow.IProfileHolder
import com.hwx.chatique.helpers.ActivityHolder
import com.hwx.chatique.network.IStreamManager
import com.hwx.chatique.network.models.MessageMetaEvent
import com.hwx.chatique.network.models.MessageMetaType
import com.hwx.chatique.ui.communicationRoom.stolen.AutoFitSurfaceView
import com.hwx.chatique.ui.communicationRoom.stolen.AutoFitTextureView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit


interface IVideoController {
    val selfFps: Flow<Int>
    val opponentFps: Flow<Int>
    fun initSelfPreview(view: AutoFitTextureView)
    fun initOpponentPreview(view: AutoFitSurfaceView)
    fun startStreaming(chatId: String)
    fun startPlayback(chatId: String)
    fun putOpponentPayload(bytesPayload: MessageMetaEvent)
    fun switchCamera()

    fun stopStreaming(chatId: String)
    fun stopPlayback(chatId: String)
    fun stop()
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    )
}

class VideoController(
    private val activityHolder: ActivityHolder,
    private val profileHolder: IProfileHolder,
    private val streamManager: IStreamManager,
) : IVideoController {

    private companion object {
        const val REQUEST_CAMERA_PERMISSION = 1
        const val CAPTURE_IMAGE_FORMAT = ImageFormat.YUV_420_888
    }

    private var isStreaming = false
    private var targetChatId = ""
    private var weakTextureViewSelf: WeakReference<AutoFitTextureView>? = null
    private var weakTextureViewOpponent: WeakReference<AutoFitSurfaceView>? = null

    /**
     * A [Semaphore] to prevent the app from exiting before closing the camera.
     */
    private val mCameraOpenCloseLock = Semaphore(1)
    private var mImageReader: ImageReader? = null

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private var mBackgroundThread: HandlerThread? = null

    /**
     * A [Handler] for running tasks in the background.
     */
    private var mBackgroundHandler: Handler? = null

    /**
     * The [Size] of camera preview.
     */
    private var mPreviewSize: Size? = null

    /**
     * ID of the current [CameraDevice].
     */
    private var mCameraId: String = ""

    /**
     * A reference to the opened [CameraDevice].
     */
    private var mCameraDevice: CameraDevice? = null

    /**
     * [CaptureRequest.Builder] for the camera preview
     */
    private var mPreviewRequestBuilder: CaptureRequest.Builder? = null

    /**
     * [CaptureRequest] generated by [.mPreviewRequestBuilder]
     */
    private var mPreviewRequest: CaptureRequest? = null

    /**
     * A [CameraCaptureSession] for camera preview.
     */
    private var mCaptureSession: CameraCaptureSession? = null

    private var targetImageSize: Size? = null
        set(value) {
            targetImageWidthStr = value?.width?.toString().orEmpty()
            targetImageHeightStr = value?.height?.toString().orEmpty()
            field = value
        }
    private var targetImageHeightStr = ""
    private var targetImageWidthStr = ""
    private var sensorOrientation = 0

    private val bitmapOptions = BitmapFactory.Options().apply {
        inMutable = false
        inPreferredConfig = Bitmap.Config.HARDWARE
    }

    private val mutex = Mutex()
    private val mCaptureCallback: CaptureCallback = object : CaptureCallback() {}
    private var cameraPreviewWidth = 0
    private var cameraPreviewHeight = 0

    private var selfPrevSecStart = 0L
    private var selfTicksPerSecond = 0
    private var oppPrevSecStart = 0L
    private var oppTicksPerSecond = 0

    override val selfFps = MutableStateFlow(0)
    override val opponentFps = MutableStateFlow(0)

    private val surfaceTextureListenerSelf = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            if (!checkPermissions()) return
            if (isStreaming) {
                cameraPreviewWidth = width
                cameraPreviewHeight = height
                openCamera(width, height)
            }
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture) = true

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
    }

    private var isSurfaceReady = false
    private var targetCameraLensFacing = CameraCharacteristics.LENS_FACING_BACK

    private val surfaceCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            isSurfaceReady = true
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            isSurfaceReady = false
        }
    }

    private val mOnImageAvailableListener =
        OnImageAvailableListener { reader -> onImageAvailable(reader) }

    /**
     * [CameraDevice.StateCallback] is called when [CameraDevice] changes its state.
     */
    private val mStateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(cameraDevice: CameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release()
            mCameraDevice = cameraDevice
            createCameraPreviewSession()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
        }
    }

    override fun initSelfPreview(view: AutoFitTextureView) {
        weakTextureViewSelf = WeakReference(view)
        view.surfaceTextureListener = surfaceTextureListenerSelf

        if (!checkPermissions()) return
        startBackgroundThread()
    }

    override fun initOpponentPreview(view: AutoFitSurfaceView) {
        weakTextureViewOpponent = WeakReference(view)
        view.holder.addCallback(surfaceCallback)
    }

    override fun startStreaming(chatId: String) {
        if (isStreaming) return
        targetChatId = chatId
        if (!checkPermissions()) return
        isStreaming = true
    }

    override fun startPlayback(chatId: String) {
        //TODO("Not yet implemented")
    }

    override fun putOpponentPayload(it: MessageMetaEvent) {
        if (!isSurfaceReady) return
        val targetView = weakTextureViewOpponent?.get() ?: return

        val currentTime = System.currentTimeMillis()
        val diff = currentTime - oppPrevSecStart
        if (diff > 1000) {
            oppPrevSecStart = currentTime
            opponentFps.value = oppTicksPerSecond
            oppTicksPerSecond = 0
        } else {
            oppTicksPerSecond++
        }

        val desiredWidth = targetView.width
        val inputWidth = it.value.toInt()
        GlobalScope.launch(Dispatchers.Default) {

            val scaleFactor = desiredWidth.toFloat() / inputWidth
            bitmapOptions.outWidth = targetView.width
            bitmapOptions.outHeight = targetView.height
            bitmapOptions.inJustDecodeBounds = false
            val scaledBitmap =
                BitmapFactory.decodeByteArray(
                    it.bytesPayload,
                    0,
                    it.bytesPayload.size,
                    bitmapOptions
                )


            val matrix = Matrix().apply {
                postScale(scaleFactor, scaleFactor)
            }

            mutex.withLock {
                targetView.holder.surface.lockHardwareCanvas()?.let {
                    it.drawBitmap(scaledBitmap, matrix, null)
                    targetView.holder.surface.unlockCanvasAndPost(it)
                }
            }
        }
    }

    override fun switchCamera() {
        targetCameraLensFacing =
            if (targetCameraLensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                CameraCharacteristics.LENS_FACING_FRONT
            } else {
                CameraCharacteristics.LENS_FACING_BACK
            }
        mCameraDevice?.close()
        openCamera(cameraPreviewWidth, cameraPreviewHeight)
    }

    private fun onImageAvailable(reader: ImageReader?) {
        val currentTime = System.currentTimeMillis()
        val diff = currentTime - selfPrevSecStart
        if (diff > 1000) {
            selfPrevSecStart = currentTime
            selfFps.value = selfTicksPerSecond
            selfTicksPerSecond = 0
        } else {
            selfTicksPerSecond++
        }
        GlobalScope.launch(Dispatchers.Default) {
            reader ?: return@launch
            val image: Image? = try {
                reader.acquireLatestImage()
            } catch (e: IllegalStateException) {
                println("whoops")
                return@launch
            }
            image ?: return@launch
            val nv21Bytes = ImageConverter.YUV_420_888toNV21(image)
            val targetHeight = if (sensorOrientation == 90 || sensorOrientation == 270) {
                image.width
            } else image.height
            val targetWidth = if (sensorOrientation == 90 || sensorOrientation == 270) {
                image.height
            } else image.width
            val rotatedBytes =
                ImageConverter.rotateNV21(nv21Bytes, image.width, image.height, sensorOrientation)
            val jpegBytes = ImageConverter.NV21toJPEG(rotatedBytes, targetWidth, targetHeight)
            sendFrame(jpegBytes)
            image.close()
        }
    }

    private fun sendFrame(framePayload: ByteArray) {
        val event = MessageMetaEvent(
            "",
            "",
            targetChatId,
            MessageMetaType.VIDEO_CALL_PAYLOAD,
            targetImageHeightStr,
            -1,
            profileHolder.userId,
            framePayload,
        )
        streamManager.sendMessageToStream(IStreamManager.StreamId.META_EVENTS, event)
    }

    override fun stopStreaming(chatId: String) {
        //TODO("Not yet implemented")
    }

    override fun stopPlayback(chatId: String) {
        //TODO("Not yet implemented")
    }

    override fun stop() {
        stopBackgroundThread()
        mCameraDevice?.close()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CAMERA_PERMISSION &&
            grantResults.getOrNull(0) == PackageManager.PERMISSION_GRANTED
        ) {
            if (targetChatId.isNotEmpty()) {
                startStreaming(targetChatId)
            }
        }
    }

    private fun checkPermissions(): Boolean {
        val activity = activityHolder.activity ?: return false
        val hasPermission = ActivityCompat.checkSelfPermission(
            activity,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            ActivityCompat.requestPermissions(
                activity, arrayOf(
                    Manifest.permission.CAMERA
                ), REQUEST_CAMERA_PERMISSION
            )
            return false
        }
        return true
    }

    /**
     * Opens the camera specified by [mCameraId].
     */
    @Throws(SecurityException::class)
    private fun openCamera(width: Int, height: Int) {
        val activity = activityHolder.activity ?: return
        setUpCameraOutputs(width, height)
        configureTransform(width, height)
        val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.", e)
        }
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    private fun setUpCameraOutputs(width: Int, height: Int) {
        val activity = activityHolder.activity ?: return
        val textureView = weakTextureViewSelf?.get() ?: return
        val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)

                // We don't use a front facing camera in this sample.
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING) ?: continue
                if (facing != targetCameraLensFacing) continue

                val map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                ) ?: continue
                sensorOrientation =
                    characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) as Int
                val availableSizes = map.getOutputSizes(CAPTURE_IMAGE_FORMAT)
                val hdSize = availableSizes.find { it.height in 600..900 && it.width in 1100..1300 }
                    ?: Collections.min(
                        listOf(*availableSizes),
                        CompareSizesByArea()
                    )

                targetImageSize = hdSize
                mImageReader = ImageReader.newInstance(
                    hdSize.width, hdSize.height,
                    CAPTURE_IMAGE_FORMAT,  /*maxImages*/5
                )
                mImageReader?.setOnImageAvailableListener(
                    mOnImageAvailableListener, mBackgroundHandler
                )

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                val mPreviewSize = chooseOptimalSize(
                    map.getOutputSizes(
                        SurfaceTexture::class.java
                    ),
                    width, height, targetImageSize!!
                )
                this.mPreviewSize = mPreviewSize


                // We fit the aspect ratio of TextureView to the size of preview we picked.
                val orientation: Int = activity.resources.configuration.orientation
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    textureView.setAspectRatio(
                        mPreviewSize.width, mPreviewSize.height
                    )
                } else {
                    textureView.setAspectRatio(
                        mPreviewSize.height, mPreviewSize.width
                    )
                }
                mCameraId = cameraId
                return
            }
        } catch (e: CameraAccessException) {
            Log.w("AVX", "err on cameraId obtain", e)
        } catch (e: NullPointerException) {
            Log.w("AVX", "err on cameraId obtain", e)
        }
    }

    /**
     * Configures the necessary [Matrix] transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        val activity = activityHolder.activity ?: return
        val textureView = weakTextureViewSelf?.get() ?: return
        if (null == mPreviewSize) {
            return
        }
        val rotation = activity.windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(
            0f, 0f, mPreviewSize!!.height.toFloat(),
            mPreviewSize!!.width.toFloat()
        )
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale =
                (viewHeight.toFloat() / mPreviewSize!!.height).coerceAtLeast(viewWidth.toFloat() / mPreviewSize!!.width)
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        textureView.setTransform(matrix)
    }

    /**
     * Creates a new [CameraCaptureSession] for camera preview.
     */
    private fun createCameraPreviewSession() {
        val textureView = weakTextureViewSelf?.get() ?: return
        try {
            val texture: SurfaceTexture = textureView.surfaceTexture!!

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize!!.width, mPreviewSize!!.height)

            // This is the output Surface we need to start preview.
            val surface = Surface(texture)
            val mImageSurface = mImageReader!!.surface


            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder =
                mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            mPreviewRequestBuilder?.addTarget(surface)
            mPreviewRequestBuilder?.addTarget(mImageSurface)


            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice?.createCaptureSession(
                listOf(surface, mImageReader!!.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        // The camera is already closed
                        if (null == mCameraDevice) {
                            return
                        }
                        // When the session is ready, we start displaying the preview.
                        mCaptureSession = cameraCaptureSession
                        try {
                            // Auto focus should be continuous for camera preview.
                            mPreviewRequestBuilder?.set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )
                            mPreviewRequest = mPreviewRequestBuilder?.build()
                            mCaptureSession?.setRepeatingRequest(
                                mPreviewRequest!!,
                                mCaptureCallback, mBackgroundHandler
                            )
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onConfigureFailed(
                        cameraCaptureSession: CameraCaptureSession
                    ) {
                        //showToast("Failed")
                    }
                }, null
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    /**
     * Compares two `Size`s based on their areas.
     */
    internal class CompareSizesByArea : Comparator<Size> {
        override fun compare(lhs: Size, rhs: Size): Int {
            // We cast here to ensure the multiplications won't overflow
            return java.lang.Long.signum(
                lhs.width.toLong() * lhs.height -
                        rhs.width.toLong() * rhs.height
            )
        }
    }

    /**
     * Starts a background thread and its [Handler].
     */
    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("CameraBackground")
        mBackgroundThread?.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
    }

    /**
     * Stops the background thread and its [Handler].
     */
    private fun stopBackgroundThread() {
        mBackgroundThread?.quitSafely()
        try {
            mBackgroundThread?.join()
            mBackgroundThread = null
            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    /**
     * Given `choices` of `Size`s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values, and whose aspect
     * ratio matches with the specified value.
     *
     * @param choices     The list of sizes that the camera supports for the intended output class
     * @param width       The minimum desired width
     * @param height      The minimum desired height
     * @param aspectRatio The aspect ratio
     * @return The optimal `Size`, or an arbitrary one if none were big enough
     */
    private fun chooseOptimalSize(
        choices: Array<Size>,
        width: Int,
        height: Int,
        aspectRatio: Size
    ): Size {
        // Collect the supported resolutions that are at least as big as the preview Surface
        val bigEnough: MutableList<Size> = ArrayList()
        val w = aspectRatio.width
        val h = aspectRatio.height
        for (option in choices) {
            if (option.height == option.width * h / w && option.width >= width && option.height >= height) {
                bigEnough.add(option)
            }
        }

        // Pick the smallest of those, assuming we found any
        return if (bigEnough.size > 0) {
            Collections.min(bigEnough, CompareSizesByArea())
        } else {
            Log.e("AVX", "Couldn't find any suitable preview size")
            choices[0]
        }
    }
}