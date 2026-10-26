// =================================================================================================
// USE CASE: ОТРИМАННЯ ТЕСТОВИХ ДАНИХ (GET TEST USE CASE)
//
// Архітектурний шар: Domain Layer (Use Case).
// Призначення:
// Чистий сценарій використання бізнес-рівня для запиту списку тестових записів з репозиторію.
// Реалізує принцип інверсії залежностей (DIP) Clean Architecture.
// =================================================================================================

package com.nekorom.kasaapp.domain.usecase

import com.nekorom.kasaapp.domain.models.Test
import com.nekorom.kasaapp.domain.repository.TestRepository

/**
 * Прецедент отримання тестових об'єктів.
 *
 * @param testRepository Доменний репозиторій [TestRepository].
 */
class GetTestUseCase(private val testRepository: TestRepository) {
    /**
     * Виконання сценарію вибірки тестових записів.
     */
    fun execute(): List<Test> {
        return testRepository.getTest()
    }
}