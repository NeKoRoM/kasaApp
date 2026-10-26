package com.example.vid2.di

import com.example.vid2.domain.usecase.GetUserNameUseCase
import com.example.vid2.domain.usecase.SaveUserNameUseCase
import org.koin.dsl.module


val domainModule = module {

    factory<GetUserNameUseCase> {
        GetUserNameUseCase(userRepository = get())

    }
    factory<SaveUserNameUseCase> {
        SaveUserNameUseCase(userRepository = get())

    }
}