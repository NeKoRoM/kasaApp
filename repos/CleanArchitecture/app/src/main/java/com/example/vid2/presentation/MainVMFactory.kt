package com.example.vid2.presentation

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.vid2.data.repository.UserRepositoryImpl
import com.example.vid2.data.storage.SharedPrefUserStorage
import com.example.vid2.domain.usecase.GetUserNameUseCase
import com.example.vid2.domain.usecase.SaveUserNameUseCase

class MainVMFactory(context: Context) : ViewModelProvider.Factory {
    private val userRepository by lazy(LazyThreadSafetyMode.NONE) {  UserRepositoryImpl(userStorage = SharedPrefUserStorage(context = context)) }
    private val getUserNameUseCase by lazy(LazyThreadSafetyMode.NONE) { GetUserNameUseCase(userRepository = userRepository) }
    private val saveUserNameUseCase by lazy(LazyThreadSafetyMode.NONE) { SaveUserNameUseCase(userRepository = userRepository) }



    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainVM(
            getUserNameUseCase=getUserNameUseCase, saveUsernameUseCase = saveUserNameUseCase
        ) as T
    }


}