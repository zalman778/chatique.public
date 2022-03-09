package com.hwx.chatique.flow

import com.hwx.chatique.network.IStreamManager
import com.hwx.chatique.network.repo.IStreamRepo
import com.hwx.chatique.p2p.IE2eController

interface IStartUpController {
    fun onAppStarted()
    fun onAuthenticated()
    fun onDestroy()
}

class StartUpController(
    private val authInteractor: IAuthInteractor,
    private val streamManager: IStreamManager,
    private val streamRepo: IStreamRepo,
    private val e2eController: IE2eController,
    private val communicationListener: ICommunicationListener,
) : IStartUpController {

    init {
        authInteractor.onAuth = {
            onAuthenticated()
        }
    }

    override fun onAppStarted() {
        authInteractor.restoreAuth()
        e2eController.init()
    }

    override fun onAuthenticated() {
        streamManager.launchBiDirectionalStream(
            IStreamManager.StreamId.MESSAGE_EVENT,
            streamRepo::messageEvents
        )
        streamManager.launchBiDirectionalStream(
            IStreamManager.StreamId.META_EVENTS,
            streamRepo::metaEvents
        )
        e2eController.initServerPart()
        streamManager.listenStreamStates(IStreamManager.StreamId.MESSAGE_EVENT)
        communicationListener.onAuthenticated()
    }

    override fun onDestroy() {
        streamManager.onDestroy()
        communicationListener.onDestroy()
        e2eController.onDestroy()
    }
}