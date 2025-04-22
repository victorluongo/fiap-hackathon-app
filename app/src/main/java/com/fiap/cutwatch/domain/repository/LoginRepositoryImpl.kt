package com.fiap.cutwatch.domain.repository

import com.fiap.cutwatch.domain.model.User
import com.fiap.cutwatch.domain.state.RequestState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class LoginRepositoryImpl(private val auth: FirebaseAuth) : LoginRepository {
    override suspend fun createUser(user: User): RequestState<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(user.email!!, user.password!!).await()
            RequestState.Success(result.user!!)
        } catch (e: Exception) {
            RequestState.Error(e)
        }
    }

    override fun getUserLogged(): User {
        return User()
    }

    override suspend fun login(user: User): RequestState<FirebaseUser> {
        return try {
            val result =
                FirebaseAuth.getInstance().signInWithEmailAndPassword(user.email!!, user.password!!)
                    .await()
            RequestState.Success(result.user!!)
        } catch (e: Exception) {
            RequestState.Error(e)
        }
    }
}