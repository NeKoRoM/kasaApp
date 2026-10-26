// =================================================================================================
// МОДУЛЬ ВПРОВАДЖЕННЯ БІЗНЕС-СЦЕНАРІЇВ (DAGGER HILT DOMAIN MODULE)
//
// Архітектурний шар: Dependency Injection Layer (ViewModel Scope).
// Призначення:
// Забезпечує створення екземплярів доменних Use Case для впровадження у ViewModel.
// Життєвий цикл обмежений ViewModelComponent.
// =================================================================================================

package com.nekorom.kasaapp.di

import com.nekorom.kasaapp.domain.repository.TestRepository
import com.nekorom.kasaapp.domain.usecase.GetTestUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * Hilt-модуль постачання прецедентів використання (Use Cases) для рівня ViewModel.
 */
@Module
@InstallIn(ViewModelComponent::class)
class DomainModule {

    /**
     * Постачає екземпляр прецеденту [GetTestUseCase].
     *
     * @param testRepository Репозиторій [TestRepository], створений на рівні Singleton або DataModule.
     */
    @Provides
    fun provideGetUseCase(testRepository: TestRepository): GetTestUseCase {
        return GetTestUseCase(testRepository)
    }
}