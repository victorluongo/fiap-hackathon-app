package com.fiap.cutwatch.domain.state

sealed class RequestLoginState<out T> {
    object Success: RequestLoginState<Nothing>()
    object Fail: RequestLoginState<Nothing>()
}
