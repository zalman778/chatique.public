package com.hwx.chatique.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RequestType {

    suspend fun <T> grpc(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.Success(block())
        } catch (e: Throwable) {
            Result.Fail(e)
        }
    }
}