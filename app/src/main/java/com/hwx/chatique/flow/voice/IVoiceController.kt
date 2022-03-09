package com.hwx.chatique.flow.voice

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.util.Log
import androidx.core.app.ActivityCompat
import com.hwx.chatique.arch.extensions.equalsToOneOf
import com.hwx.chatique.arch.extensions.zip
import com.hwx.chatique.flow.IProfileHolder
import com.hwx.chatique.helpers.ActivityHolder
import com.hwx.chatique.helpers.IToaster
import com.hwx.chatique.network.IStreamManager
import com.hwx.chatique.network.models.MessageMetaEvent
import com.hwx.chatique.network.models.MessageMetaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

interface IVoiceController {
    fun startVoicePlayback(userFromId: Long)
    fun startVoiceStreaming(chatId: String)
    fun stopVoicePlayback(userFromId: Long)
    fun stop()
    fun toggleMute()
    fun toggleSpeaker()
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    )
}

class VoiceController(
    private val activityHolder: ActivityHolder,
    private val streamManager: IStreamManager,
    private val profileHolder: IProfileHolder,
    private val toaster: IToaster,
) : IVoiceController {

    companion object {
        const val PERMISSION_REQUEST_VOICE = 1
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var targetChatId = ""
    private var recorder: AudioRecord? = null

    private val sampleRateHz = 16000
    private val bufferSizeInBytes = AudioRecord
        .getMinBufferSize(sampleRateHz, AudioFormat.CHANNEL_IN_MONO, AUDIO_FORMAT)

    private var recordingJob: Job? = null
    private val recordChunk = ByteArray(bufferSizeInBytes)
    private val emptyChunk = ByteArray(bufferSizeInBytes)
    private var playerJob: Job? = null
    private val currentPlayback = mutableSetOf<Long>()
    private val track = createTrack()
    private var isStreaming = false
    private var isMuted = false
    private var isUsingSpeaker = false
    private var oldAudioMode: Int? = null

    override fun startVoicePlayback(userFromId: Long) {
        currentPlayback.add(userFromId)
        rebuildPlayingJob()

        if (track.playState == AudioTrack.PLAYSTATE_STOPPED) {
            track.play()
        }
    }

    override fun startVoiceStreaming(chatId: String) {
        if (isStreaming) return
        targetChatId = chatId
        if (!checkPermissions()) return
        initializeRecorder()

        isStreaming = true
        recorder?.startRecording()
        recordingJob?.cancel()
        recordingJob = GlobalScope.launch(Dispatchers.IO) {
            while (isStreaming) {
                recorder?.read(recordChunk, 0, recordChunk.size)
                sendRecordedChunk()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_VOICE &&
            grantResults.getOrNull(0) == PackageManager.PERMISSION_GRANTED
        ) {
            startVoiceStreaming(targetChatId)
        }
    }

    override fun stop() {
        recordingJob?.cancel()
        recorder?.stop()
        playerJob?.cancel()
        track.stop()
        isStreaming = false
        currentPlayback.clear()
    }

    override fun toggleMute() {
        isMuted = !isMuted
    }

    override fun toggleSpeaker() {
        isUsingSpeaker = !isUsingSpeaker
        onTargetSpeakerUpdated()
    }

    private fun onTargetSpeakerUpdated() {
        val activity = activityHolder.activity ?: return
        val manager = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (isUsingSpeaker) {
            oldAudioMode = manager.mode
            manager.mode = AudioManager.MODE_NORMAL
            manager.isSpeakerphoneOn = true
        } else {
            oldAudioMode?.let {
                manager.mode = it
            }
            manager.isSpeakerphoneOn = false
        }
    }

    override fun stopVoicePlayback(userFromId: Long) {
        currentPlayback.remove(userFromId)
        rebuildPlayingJob()
    }

    private fun createTrack() = AudioTrack(
        AudioManager.STREAM_VOICE_CALL,
        sampleRateHz,
        AudioFormat.CHANNEL_OUT_MONO,
        AUDIO_FORMAT,
        bufferSizeInBytes,
        AudioTrack.MODE_STREAM,
    ).apply {
        play()
    }

    @SuppressLint("MissingPermission")
    private fun initializeRecorder() {
        if (recorder != null) return
        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRateHz,
            AudioFormat.CHANNEL_IN_MONO,
            AUDIO_FORMAT,
            bufferSizeInBytes,
        )
    }

    private fun checkPermissions(): Boolean {
        val activity = activityHolder.activity ?: return false
        val hasVoicePermission = ActivityCompat.checkSelfPermission(
            activity,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasVoicePermission) {
            ActivityCompat.requestPermissions(
                activity, arrayOf(
                    Manifest.permission.RECORD_AUDIO
                ), PERMISSION_REQUEST_VOICE
            )
            return false
        }
        return true
    }

    private fun sendRecordedChunk() {
        val payload = if (isMuted) emptyChunk else recordChunk
        val event = MessageMetaEvent(
            "",
            "",
            targetChatId,
            MessageMetaType.VOICE_CALL_PAYLOAD,
            "",
            -1,
            profileHolder.userId,
            payload,
        )
        streamManager.sendMessageToStream(IStreamManager.StreamId.META_EVENTS, event)
    }

    private fun rebuildPlayingJob() {
        toaster.longToast("rebuildPlayingJob: $currentPlayback")
        Log.w("AVX", "rebuildPlayingJob: $currentPlayback")
        playerJob?.cancel()
        if (currentPlayback.isEmpty()) return
        playerJob = GlobalScope.launch(Dispatchers.IO) {
            val flows = currentPlayback.map { userId ->
                streamManager.getStreamInput<MessageMetaEvent>(IStreamManager.StreamId.META_EVENTS)
                    .filter { it.type.equalsToOneOf(MessageMetaType.VOICE_CALL_PAYLOAD) && it.userFromId == userId }
            }
            zip(*flows.toTypedArray()) { payloads ->
                var initialBuffer = ByteArray(bufferSizeInBytes)
                var isInitialized = false
                payloads.forEach {
                    val payload = it.bytesPayload
                    if (!isInitialized) {
                        initialBuffer = payload
                        isInitialized = true
                    } else {
                        //https://stackoverflow.com/a/67834989
                        initialBuffer = SoundByteMixer.mix(initialBuffer, payload, false)
                    }
                }
                track.write(initialBuffer, 0, initialBuffer.size)
            }.collect()
        }
    }
}