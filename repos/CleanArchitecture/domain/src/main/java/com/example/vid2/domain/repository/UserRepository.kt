package com.example.vid2.domain.repository

import com.example.vid2.domain.models.SaveUserNameParam
import com.example.vid2.domain.models.UserName

interface UserRepository {
    fun saveName(saveParam: SaveUserNameParam):Boolean
    fun getName(): UserName
}