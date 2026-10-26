package com.example.vid2.domain.usecase

import com.example.vid2.domain.models.UserName
import com.example.vid2.domain.repository.UserRepository

class GetUserNameUseCase (private val userRepository: UserRepository){
    fun execute(): UserName {
        return userRepository.getName()

    }
}