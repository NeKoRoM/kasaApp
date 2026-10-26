// =================================================================================================
// ХМАРНИЙ РЕПОЗИТОРІЙ FIREBASE: АВТОРИЗАЦІЯ, ЛІЦЕНЗУВАННЯ ТА СИНХРОНІЗАЦІЯ ПРОФІЛЮ
//
// Архітектурний шар: Data Layer (Firebase Implementation).
// Призначення:
// 1. Автентифікація касира/магазину через Firebase Authentication (Email/Password).
// 2. Безшовна автоматична реєстрація нового акаунта при першому вході.
// 3. Збереження та синхронізація профілю касира в Firebase Realtime Database (/users/{uid}).
// 4. Дистанційний контроль доступу (Realtime Kill-Switch): спостереження за прапорцем "enabled".
//    Якщо адміністратор блокує магазин або закінчується ліцензія (enabled == false),
//    спрацьовує колбек onFail(), який блокує інтерфейс каси.
// =================================================================================================

package com.nekorom.kasaapp.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.nekorom.kasaapp.data.roomDB.entity.UserEntity
import com.nekorom.kasaapp.domain.repository.NetworkDataRepository
import com.nekorom.kasaapp.utils.LOGIN
import com.nekorom.kasaapp.utils.PASSWORD
import com.nekorom.kasaapp.utils.USER

/**
 * Реалізація репозиторію хмарних даних на базі Google Firebase.
 */
class NetworkDataRepositoryImp : NetworkDataRepository {

    // Локальний кеш списку користувачів
    private val userList = mutableListOf<UserEntity>()

    // Посилання на корінь вузла користувачів у Firebase Realtime Database
    private val database = Firebase.database.reference.child("users")

    /**
     * Збереження або оновлення профілю касира [USER] у Realtime Database.
     * Ключем вузла виступає унікальний ідентифікатор Firebase Auth UID.
     */
    fun writeNewUser() {
        Firebase.auth.currentUser?.uid?.replace(".", "")?.let {
            database.child(it).setValue(USER?.copy(firebaseId = it))
        }
    }

    /**
     * Постійний моніторинг ліцензійного статусу або блокування терміналу в реальному часі.
     * Слухає зміну поля `/users/{uid}/enabled`.
     *
     * @param onFail Викликається, коли значення поля дорівнює `false` (термінал деактивовано).
     */
    fun checkEnabled(onFail: () -> Unit) {
        val enabled = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val post = dataSnapshot.getValue<Boolean>()
                if (post == false) {
                    onFail()
                }
                Log.d("FirebaseR", post.toString())
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Обробка скасування або помилки доступу
            }
        }
        Firebase.auth.currentUser?.uid?.let {
            database.child(it).child("enabled").addValueEventListener(enabled)
        }
    }

    // Екземпляр сервісу аутентифікації Firebase
    private val mAuth = FirebaseAuth.getInstance()

    /**
     * Вихід з облікового запису Firebase.
     */
    override fun signOut() {
        mAuth.signOut()
    }

    /**
     * Підключення до бази даних:
     * Спочатку намагається увійти з вказаними [LOGIN] та [PASSWORD].
     * Якщо акаунт не знайдено — автоматично створює новий обліковий запис у Firebase Auth.
     *
     * @param onSuccess Callback успішного входу або реєстрації.
     * @param onFail Callback у разі фатальної помилки автентифікації (наприклад, невірний пароль).
     */
    override fun connectToDatabase(onSuccess: () -> Unit, onFail: (String) -> Unit) {
        LOGIN?.let { login ->
            PASSWORD?.let { pass ->
                mAuth.signInWithEmailAndPassword(login, pass)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener {
                        // Якщо вхід не вдався, пробуємо автоматично зареєструвати касира
                        mAuth.createUserWithEmailAndPassword(login, pass)
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { onFail(it.message.toString()) }
                    }
            }
        }
    }
}

