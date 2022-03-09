package com.hwx.chatique.network

sealed class Result<out T> {
    data class Success<out T>(val value: T) : Result<T>()
    data class Fail(val cause: Throwable) : Result<Nothing>()
}