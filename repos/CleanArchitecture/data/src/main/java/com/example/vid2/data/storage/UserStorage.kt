package com.example.vid2.data.storage

interface UserStorage {
fun save(user :User): Boolean

fun get():User
}