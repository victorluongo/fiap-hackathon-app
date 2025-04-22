package com.fiap.cutwatch.domain.state

sealed class RequestState<out T> {
    object Loading : RequestState<Nothing>()
    data class Success<T>(val data: T? = null) : RequestState<T>()
    data class Error(val throwable: Throwable) : RequestState<Nothing>()
    data class NewActivity(val isRecord: Boolean) : RequestState<Nothing>()
}