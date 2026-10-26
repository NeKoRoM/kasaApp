package com.example.vid2.data.repository

import com.example.vid2.data.storage.User
import com.example.vid2.data.storage.UserStorage

import com.example.vid2.domain.models.SaveUserNameParam
import com.example.vid2.domain.models.UserName
import com.example.vid2.domain.repository.UserRepository


class UserRepositoryImpl(private val userStorage: UserStorage): UserRepository {

    override fun saveName(saveParam:SaveUserNameParam):Boolean{
        val user= User(firstName = saveParam.name, lastName = "")


        return userStorage.save(user)
    }
    override fun getName():UserName{
        val user= userStorage.get()
        val userName= UserName(firstName = user.firstName, lastName = user.lastName)
        return userName

    }
}