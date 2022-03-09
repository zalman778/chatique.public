package com.hwx.chatique.di

import android.content.Context
import com.hwx.chatique.helpers.INetworkStatusProvider
import com.hwx.chatique.helpers.ISnackbarManager
import com.hwx.chatique.helpers.NetworkStatusProvider
import com.hwx.chatique.network.IStreamManager
import com.hwx.chatique.network.StreamManager
import com.hwx.chatique.network.grpc.*
import com.hwx.chatique.network.repo.AppRepo
import com.hwx.chatique.network.repo.IAppRepo
import com.hwx.chatique.network.repo.IStreamRepo
import com.hwx.chatique.network.repo.StreamRepo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.grpc.ClientInterceptor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {

    @Singleton
    @Provides
    fun provideInterceptors() = listOf(
        GrpcAuthHeadersInterceptor().authHeaders(),
        GrpcDeAuthInterceptor(),
    )

    @Singleton
    @Provides
    fun provideChannelHolder(
        @ApplicationContext context: Context,
        interceptors: @JvmSuppressWildcards List<ClientInterceptor>,
    ): IGrpcChannelHolder = GrpcChannelHolder(context, interceptors)

    @Singleton
    @Provides
    fun provideStubsHolder(
        holder: IGrpcChannelHolder,
    ): IGrpcStubsHolder = GrpcStubsHolder(holder.channel)

    @Singleton
    @Provides
    fun provideRepo(
        holder: IGrpcStubsHolder,
    ): IAppRepo = AppRepo(holder)

    @Singleton
    @Provides
    fun provideStreamRepo(
        holder: IGrpcStubsHolder,
    ): IStreamRepo = StreamRepo(holder)

    @Singleton
    @Provides
    fun provideStreamManager(
        networkProvider: INetworkStatusProvider,
        snackbar: ISnackbarManager,
    ): IStreamManager = StreamManager(networkProvider, snackbar)

    @Singleton
    @Provides
    fun provideINetworkStatusProvider(
        @ApplicationContext context: Context,
    ): INetworkStatusProvider = NetworkStatusProvider(context)
}