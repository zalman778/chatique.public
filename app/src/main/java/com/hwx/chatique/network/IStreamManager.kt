package com.hwx.chatique.network

import android.util.Log
import com.hwx.chatique.helpers.INetworkStatusListener
import com.hwx.chatique.helpers.INetworkStatusProvider
import com.hwx.chatique.helpers.ISnackbarManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext

interface IStreamManager {

    fun <T> launchBiDirectionalStream(
        streamId: StreamId,
        streamProvider: (Flow<T>) -> Flow<T>
    )

    fun listenStreamStates(streamId: StreamId)
    fun <T> getStreamInput(id: StreamId): Flow<T>
    fun getStreamState(id: StreamId): Flow<StreamState>
    fun <T : Any> sendMessageToStream(streamId: StreamId, message: T)
    fun onDestroy()

    sealed class StreamState {
        object NotConnected : StreamState()
        object Connected : StreamState()
        data class DelayingReconnect(val delayMs: Long, val attempt: Long) : StreamState()
        data class Reconnecting(val attempt: Long) : StreamState()
        data class Fail(val cause: Throwable?) : StreamState()

        fun isReconnecting() = this is Reconnecting || this is DelayingReconnect
    }

    enum class StreamId {
        MESSAGE_EVENT,
        META_EVENTS,
    }
}

class StreamManager(
    networkProvider: INetworkStatusProvider,
    private val snackbar: ISnackbarManager,
) : IStreamManager, CoroutineScope {

    private companion object {
        const val FORCED_RECONNECT_DELAY_MS = 3000L
        const val RECONNECT_DELAY_MS = 1500L
        const val MAX_RECONNECT_DELAY_MS = 10_000L
        const val MAX_RECONNECT_ATTEMPT_COUNT = 5

        const val ERROR_WAITING_DELAY = 2000L
    }

    data class BiDirectionalConnection<Input, Output>(
        val outputFlow: MutableSharedFlow<Output>,
        val inputFlow: Flow<Input>,
        val stateFlow: MutableStateFlow<IStreamManager.StreamState>,
        val retryFlow: MutableSharedFlow<Unit>,
    )

    private val biDirectionalMap =
        HashMap<IStreamManager.StreamId, BiDirectionalConnection<in Any, in Any>>()

    private val innerJob = SupervisorJob()
    override val coroutineContext: CoroutineContext = innerJob + Dispatchers.IO

    init {
        networkProvider.addListener(object : INetworkStatusListener {
            override fun onAvailable() {
                forceReconnect()
            }
        })
    }

    override fun <T> getStreamInput(id: IStreamManager.StreamId) =
        (biDirectionalMap[id]?.inputFlow as Flow<T>?)
            ?: emptyFlow()

    override fun getStreamState(id: IStreamManager.StreamId) =
        biDirectionalMap[id]?.stateFlow
            ?: flowOf(IStreamManager.StreamState.NotConnected)

    override fun <T> launchBiDirectionalStream(
        streamId: IStreamManager.StreamId,
        streamProvider: (Flow<T>) -> Flow<T>
    ) {
        val outputFlow = MutableSharedFlow<T>(1)
        val stateFlow =
            MutableStateFlow<IStreamManager.StreamState>(IStreamManager.StreamState.NotConnected)
        val retryFlow = MutableSharedFlow<Unit>(1)

        var oldWaitingJob: Job? = null

        //retry logics: slowly increase retry time until max retry time
        val retryCollector: suspend FlowCollector<*>.(cause: Throwable, attempt: Long) -> Boolean =
            { exception, attempt ->
                Log.w("AVX2", "flow.retryCollector() with exception = $exception")
                oldWaitingJob?.cancel()
                retryDelayed(attempt, stateFlow)
            }

        val job = Job()
        val inputFlow = streamProvider(outputFlow)
            .onStart {
                oldWaitingJob?.cancel()
                oldWaitingJob = this@StreamManager.launch {
                    delay(ERROR_WAITING_DELAY)
                    stateFlow.emit(IStreamManager.StreamState.Connected)
                }
            }
            .retryWhen(retryCollector)
            .catch {
                stateFlow.emit(IStreamManager.StreamState.Fail(it))
            }
            .shareIn(this + job, SharingStarted.Eagerly, replay = 1)

        val biDirectionalConnection =
            BiDirectionalConnection(outputFlow, inputFlow, stateFlow, retryFlow)
        biDirectionalMap[streamId] =
            biDirectionalConnection as BiDirectionalConnection<in Any, in Any>
    }

    override fun listenStreamStates(streamId: IStreamManager.StreamId) {
        val stateFlow = biDirectionalMap[streamId]?.stateFlow ?: return
        //stream watching logic..
        var prevStreamState: IStreamManager.StreamState? = null
        launch {
            stateFlow.collect { newState ->
                if (prevStreamState?.isReconnecting() == true && newState == IStreamManager.StreamState.Connected) {
                    snackbar.showMessage("Connected")
                }
                if ((prevStreamState == IStreamManager.StreamState.Connected ||
                            prevStreamState == null ||
                            prevStreamState == IStreamManager.StreamState.NotConnected
                            ) && newState.isReconnecting()
                ) {
                    snackbar.showMessage("Reconnecting")
                }
                prevStreamState = newState
            }
        }
    }

    override fun <T : Any> sendMessageToStream(streamId: IStreamManager.StreamId, message: T) {
        biDirectionalMap[streamId]?.outputFlow?.tryEmit(message)
    }

    override fun onDestroy() {
        innerJob.cancelChildren()
    }

    private suspend fun retryDelayed(
        attempt: Long,
        stateFlow: MutableStateFlow<IStreamManager.StreamState>
    ): Boolean {

        val delayTime = getDelayTime(attempt)
        stateFlow.emit(IStreamManager.StreamState.DelayingReconnect(delayTime, attempt))
        delay(delayTime)
        stateFlow.emit(IStreamManager.StreamState.Reconnecting(attempt))

        //todo - check this logic - kill retry job if forceReconnectCalled
        return true
    }


    private fun forceReconnect() {
        Log.w("AVX", "forceReconnect triggered")
        launch {
            // сети нужно время, иначе будут сыпаться исключения при реконнектах
            delay(FORCED_RECONNECT_DELAY_MS)
            biDirectionalMap.values.forEach { it.retryFlow.emit(Unit) }
        }
    }

    private fun getDelayTime(attempt: Long) = if (attempt <= MAX_RECONNECT_ATTEMPT_COUNT)
        attempt * RECONNECT_DELAY_MS
    else MAX_RECONNECT_DELAY_MS
}