package com.example.vid2.domain.usecase;

import com.example.vid2.domain.models.SaveUserNameParam
import com.example.vid2.domain.repository.UserRepository

public class SaveUserNameUseCase(private val userRepository: UserRepository) {
    fun execute(param: SaveUserNameParam): Boolean {
        return userRepository.saveName(param)
    }
}
