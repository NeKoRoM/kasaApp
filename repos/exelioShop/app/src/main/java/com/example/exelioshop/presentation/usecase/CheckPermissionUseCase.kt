package com.example.exelioshop.presentation.usecase

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * [CheckPermissionUseCase] — юзкейс перевірки системних дозволів рантайму Android.
 * 
 * Враховує відмінності між версіями ОС:
 * - Для Android 12 (API 31 S) і вище перевіряє:
 *   - [Manifest.permission.BLUETOOTH_CONNECT] (обов'язковий для обміну даними через Bluetooth з Android 12)
 *   - [Manifest.permission.ACCESS_FINE_LOCATION] (необхідний для сканування Bluetooth-пристроїв)
 *   - [Manifest.permission.CAMERA] (для оптичного сканування штрихкодів)
 * - Для старіших версій Android (до API 31) перевіряє:
 *   - [Manifest.permission.ACCESS_FINE_LOCATION]
 *   - [Manifest.permission.CAMERA]
 */
class CheckPermissionUseCase {
    /**
     * Перевірка наявності всіх обов'язкових дозволів для функціонування фіскального комплексу.
     * 
     * @param context контекст застосунку або Activity
     * @return `true` якщо всі необхідні дозволи надано, `false` в іншому випадку
     */
    fun checkPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
        }
    }
}