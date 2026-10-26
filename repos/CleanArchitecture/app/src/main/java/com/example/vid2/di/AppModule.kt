package com.example.vid2.di

import com.example.vid2.presentation.MainVM
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.scope.get
import org.koin.dsl.module

val appModule = module {
    viewModel<MainVM> {
        MainVM(
            getUserNameUseCase = get(),
            saveUsernameUseCase = get()
        )
    }
}