import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment

/**
 * Розширення (Extension Functions) для роботи з системними дозволами Android у фрагментах.
 * Шлях у системі: kasaApp3.0/bt/src/main/java/com/nekorom/bt/Extentions.kt
 *
 * Перевіряє права, критично важливі для функціонування POS-термінала:
 * 1. ACCESS_FINE_LOCATION: Необхідно для виявлення Bluetooth-пристроїв на Android < 12.
 * 2. BLUETOOTH_CONNECT: Обов'язковий дозвіл для Android 12+ (API 31, Build.VERSION_CODES.S) для з'єднання з РРО.
 * 3. CAMERA: Необхідно для вбудованого сканера штрихкодів (ZXing / MLKit).
 *
 * @return true, якщо всі обов'язкові дозволи надані користувачем, інакше false.
 */
fun Fragment.checkBtPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // На Android 12 (API 31) і вище потрібні BLUETOOTH_CONNECT, точна геолокація та доступ до камери
        ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        // На Android 11 і нижче достатньо дозволу на геолокацію та камеру для роботи з Bluetooth
        ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
}