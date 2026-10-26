package nekorom.com.repo

import database.Dao
import database.Department
import database.Product
import kotlinx.coroutines.flow.Flow

/**
 * ====================================================================================================
 * ДОМЕННИЙ РЕПОЗИТОРІЙ ПІДРОЗДІЛІВ/ВІДДІЛІВ (DEPARTMENT REPOSITORY)
 * ====================================================================================================
 *
 * Відповідає за надання реактивного доступу до списку виробничих цехів та підрозділів.
 * Інкапсулює взаємодію з об'єктом доступу до даних [Dao] бази даних Room.
 *
 * @property productDao Загальний DAO інтерфейс для доступу до таблиць SQLite/Room.
 */
class DepartmentRepository(private val productDao: Dao) {
    /**
     * Реактивний потік [Flow], що емітить оновлений список підрозділів [Department]
     * при будь-яких змінах у таблиці `departments` (додавання, синхронізація з хмарою, очищення).
     */
    val allDepartment: Flow<List<Department>> = productDao.getAllDepartments()
}