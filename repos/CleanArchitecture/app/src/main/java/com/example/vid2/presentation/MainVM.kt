package com.example.vid2.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.vid2.data.repository.UserRepositoryImpl
import com.example.vid2.data.storage.SharedPrefUserStorage
import com.example.vid2.domain.models.SaveUserNameParam
import com.example.vid2.domain.usecase.GetUserNameUseCase
import com.example.vid2.domain.usecase.SaveUserNameUseCase

class MainVM(private val getUserNameUseCase : GetUserNameUseCase,
             private val saveUsernameUseCase:SaveUserNameUseCase):ViewModel() {


    private val resultLiveDataMutable= MutableLiveData<String>()
    val resultLiveData: LiveData<String> =resultLiveDataMutable


    fun save(text: String){
        val params = SaveUserNameParam(name = text)
        val result: Boolean = saveUsernameUseCase.execute(param = params)
        resultLiveDataMutable.value= result.toString()

    }

    fun load(){
        val userName = getUserNameUseCase.execute()
        resultLiveDataMutable.value = "${userName.firstName},${userName.lastName}"
    }

}