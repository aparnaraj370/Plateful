package com.example.plateful.presentation.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.PhoneAuthProvider
import com.example.plateful.domain.repository.UserDataRepository
import com.example.plateful.model.UserEntity
import com.example.plateful.presentation.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SignInDataViewModel: ViewModel() {

    private val userDataRepository = UserDataRepository()
    private val _user = MutableStateFlow(MainActivity.userEntity)
    // Expose an immutable StateFlow for observing
    val user: StateFlow<UserEntity?> get() = _user

    fun getUserData(uid: String) = viewModelScope.launch {
        _user.value = userDataRepository.getUserData(uid)
        MainActivity.userEntity = _user.value
        Log.i("SignInViewModel", "User data fetched: ${_user.value}")
    }

    fun setUserData(user: UserEntity) = viewModelScope.launch {
        userDataRepository.setUserData(user)
    }

}

class SignInDataViewModelFactory() : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SignInDataViewModel::class.java))
            return SignInDataViewModel() as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}