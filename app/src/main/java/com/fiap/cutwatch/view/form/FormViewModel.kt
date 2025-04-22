package com.fiap.cutwatch.view.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiap.cutwatch.domain.model.User
import com.fiap.cutwatch.domain.state.RequestState
import com.fiap.cutwatch.domain.usecase.LoginUseCase
import com.google.android.gms.auth.api.identity.SignInPassword
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FormViewModel(private val useCase: LoginUseCase): ViewModel() {

    private val _createUserState = MutableStateFlow<RequestState<FirebaseUser>?>(null)
    val createUserState : StateFlow<RequestState<FirebaseUser>?> = _createUserState
    fun validUser(name: String, email: String, password: String, confirmPassword: String): Boolean {
        if (
            name.isNotEmpty()
            && email.isNotEmpty()
            && password.isNotEmpty()
            && confirmPassword.isNotEmpty()
            && password == confirmPassword
            ) {
            return true
        }
        return false
    }

    fun createUser(email: String, password: String) {
        viewModelScope.launch {
            _createUserState.value = RequestState.Loading
            val response = useCase.createUser(email, password)
            _createUserState.value = response
        }
    }
}
