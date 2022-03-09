package com.hwx.chatique.di

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.ExperimentalComposeUiApi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.hwx.chatique.flow.*
import com.hwx.chatique.flow.video.IVideoController
import com.hwx.chatique.flow.video.VideoController
import com.hwx.chatique.flow.voice.IVoiceController
import com.hwx.chatique.flow.voice.VoiceController
import com.hwx.chatique.helpers.*
import com.hwx.chatique.network.IStreamManager
import com.hwx.chatique.network.grpc.GrpcDeAuthInterceptor
import com.hwx.chatique.network.repo.IAppRepo
import com.hwx.chatique.network.repo.IStreamRepo
import com.hwx.chatique.p2p.E2eController
import com.hwx.chatique.p2p.IE2eController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@InstallIn(SingletonComponent::class)
@Module
class SingletonModule {

    private companion object {
        const val PREFS_NAME = "CHATIQUE_PREFS"
    }

    @Provides
    @Singleton
    fun provideActivityHolder() = ActivityHolder()

    @Provides
    @Singleton
    fun provideStartUpController(
        authInteractor: IAuthInteractor,
        streamManager: IStreamManager,
        streamRepo: IStreamRepo,
        dhCoordinator: IE2eController,
        commListener: ICommunicationListener,
    ): IStartUpController =
        StartUpController(authInteractor, streamManager, streamRepo, dhCoordinator, commListener)

    @Provides
    @Singleton
    fun provideAuthInteractor(
        profileHolder: IProfileHolder,
        repo: IAppRepo,
        snackbar: ISnackbarManager,
        @ApplicationContext context: Context,
        activityHolder: ActivityHolder,
    ): IAuthInteractor =
        AuthInteractor(profileHolder, repo, snackbar, context, activityHolder).also {
            GrpcDeAuthInterceptor.authInteractor = it
        }

    @Provides
    @Singleton
    fun provideProfileHolder(
        prefs: IPreferencesStore,
    ): IProfileHolder = ProfileHolder(prefs)

    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context,
    ): SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Provides
    @Singleton
    fun provideStorage(
        sharedPreferences: SharedPreferences,
        gson: Gson,
    ): IPreferencesStore = PreferencesStoreImpl(sharedPreferences, gson)

    @Provides
    @Singleton
    fun provideGson() = GsonBuilder()
        .setDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz")
        .create()

//        Gson().apply {
//
//        propertyNamingStrategy = PropertyNamingStrategy.LOWER_CAMEL_CASE
//       // dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
//        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
//    }

    @Provides
    @Singleton
    fun provideSnackbar(): ISnackbarManager = SnackbarManager()

    @Provides
    @Singleton
    fun provideNavigationHolder(): INavigationHolder = NavigationHolder()

    @Provides
    @Singleton
    fun provideDhCoordinator(
        streamManager: IStreamManager,
        prefs: IPreferencesStore,
        profileHolder: IProfileHolder,
    ): IE2eController = E2eController(streamManager, prefs, profileHolder)

    @Provides
    @Singleton
    fun provideVoiceController(
        activityHolder: ActivityHolder,
        streamManager: IStreamManager,
        profileHolder: IProfileHolder,
        toaster: IToaster,
    ): IVoiceController = VoiceController(activityHolder, streamManager, profileHolder, toaster)

    @Provides
    @Singleton
    fun provideVideoController(
        activityHolder: ActivityHolder,
        streamManager: IStreamManager,
        profileHolder: IProfileHolder,
    ): IVideoController = VideoController(activityHolder, profileHolder, streamManager)

    @Provides
    @Singleton
    fun provideCommunicationListener(
        streamManager: IStreamManager,
        navigationHolder: INavigationHolder,
    ): ICommunicationListener = CommunicationListener(streamManager, navigationHolder)

    @Provides
    @Singleton
    fun provideCommunicationServiceInteractor(
        @ApplicationContext context: Context,
    ): ICommunicationServiceInteractor = CommunicationServiceInteractor(context)

    @Provides
    @Singleton
    fun provideToaster(@ApplicationContext context: Context): IToaster = ToasterImpl(context)
}