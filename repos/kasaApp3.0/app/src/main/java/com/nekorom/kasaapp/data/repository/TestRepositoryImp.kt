// =================================================================================================
// РЕАЛІЗАЦІЯ РЕПОЗИТОРІЮ ТЕСТОВИХ ДАНИХ (DATA MAPPING LAYER)
//
// Архітектурний шар: Data Layer (Repository Implementation).
// Призначення:
// Реалізує чистий доменний інтерфейс TestRepository, транслюючи сутності бази даних
// (TestEntity) у доменні бізнес-моделі (Test). Ізолює Domain Layer від деталей зберігання.
// =================================================================================================

package com.nekorom.kasaapp.data.repository

import com.nekorom.kasaapp.data.storage.TestStorage
import com.nekorom.kasaapp.domain.models.Test
import com.nekorom.kasaapp.domain.repository.TestRepository

/**
 * Імплементація інтерфейсу [TestRepository] із мапінгом сутностей БД у доменні моделі.
 *
 * @param testStorage Абстракція сховища даних [TestStorage].
 */
class TestRepositoryImp(private val testStorage: TestStorage):
    TestRepository {

    /**
     * Отримання списку сутностей зі сховища та їх трансформація в доменні моделі [Test].
     */
    override fun getTest(): List<Test> {
        val testEntityList = testStorage.get()
        val testList = mutableListOf<Test>()
        testEntityList.forEach{
            testList.add(Test(it.id))
        }
        return testList
    }

    /**
     * Збереження тестової моделі (зарезервовано для розширення функціоналу тестів).
     */
    override fun saveTest(): Boolean {
        TODO("Not yet implemented")
    }
}