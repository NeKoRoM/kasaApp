package com.example.vid2.data.storage

import android.content.Context
import android.util.Log


private const val SHARED_PREFERENCES_NAME = "shared_pref_name"
private const val KEY_FIRST_NAME= "firstName"
private const val KEY_LAST_NAME= "lastName"

class SharedPrefUserStorage(context: Context) :UserStorage{
    private val sharedPreferences= context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
    override fun save(user: User): Boolean {
        sharedPreferences.edit().putString(KEY_FIRST_NAME,user.firstName).apply()
        sharedPreferences.edit().putString(KEY_LAST_NAME,user.firstName).apply()

        Log.d("MyLog","2"+ user.firstName)
        return true
    }

    override fun get(): User {
        val firstName = sharedPreferences.getString(KEY_FIRST_NAME,"") ?:""
        val lastName = sharedPreferences.getString(KEY_LAST_NAME,"default last name")?:"default last name"
        Log.d("MyLog",firstName)


        return User(firstName = firstName, lastName = lastName)
    }


}