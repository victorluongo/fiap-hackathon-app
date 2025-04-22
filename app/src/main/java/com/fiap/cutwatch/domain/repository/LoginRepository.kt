package com.fiap.cutwatch.domain.repository

import com.fiap.cutwatch.domain.model.User
import com.fiap.cutwatch.domain.state.RequestState
import com.google.firebase.auth.FirebaseUser

interface LoginRepository {
    suspend fun createUser(user: User): RequestState<FirebaseUser>
    fun getUserLogged(): User
    suspend fun login(user: User): RequestState<FirebaseUser>
}