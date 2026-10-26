package com.example.vid2.di

import com.example.vid2.data.repository.UserRepositoryImpl
import com.example.vid2.data.storage.SharedPrefUserStorage
import com.example.vid2.data.storage.UserStorage
import com.example.vid2.domain.repository.UserRepository
import org.koin.dsl.module

val dataModule = module {

    single<UserStorage> {
        SharedPrefUserStorage(context = get())

    }

    single<UserRepository>  {
        UserRepositoryImpl(userStorage = get())
    }


}