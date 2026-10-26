import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment

/**
 * Допоміжні функції розширення для фрагментів у модулі BluetoothModule.
 * Шлях у системі: BluetoothModule/bt/src/main/java/com/nekorom/bt/Extentions.kt
 */

/**
 * Динамічна зміна кольору іконки ImageButton (наприклад, підсвічування зеленим при активному Bluetooth).
 */
fun Fragment.changeButtonColor(button: ImageButton, color: Int) {
    val drawable = button.drawable
    DrawableCompat.setTint(drawable, color)
    button.setImageDrawable(drawable)
}

/**
 * Перевірка наявності необхідних дозволів Bluetooth та геопозиції.
 * На Android 12+ (Build.VERSION_CODES.S) вимагає BLUETOOTH_CONNECT та ACCESS_FINE_LOCATION.
 * На старіших версіях достатньо ACCESS_FINE_LOCATION для сканування ефіру.
 */
fun Fragment.checkBtPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}