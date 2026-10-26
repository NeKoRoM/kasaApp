package nekorom.com

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DividerDefaults.color
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import database.Product
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * ====================================================================================================
 * UI-КОМПОНЕНТ КАРТКИ ТОВАРУ/ЗАПЧАСТИНИ (COMPOSE MULTIPLATFORM)
 * ====================================================================================================
 *
 * Відповідає за візуальне представлення окремої номенклатурної одиниці в інтерфейсі вибору товарів
 * для оформлення видачі, списання або переміщення на складі.
 *
 * @param item Доменна сутність товару [Product] із назвою, артикулом та залишками.
 * @param onClick Функція зворотного виклику (callback), що спрацьовує при натисканні на картку.
 */
@Composable
fun ItemCard(item: Product, onClick: () -> Unit) {
    // Інтерактивна кнопка-контейнер із заокругленими краями та нейтральним контрастним фоном
    Button(
        onClick = { onClick() },
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0)), // Світло-сірий фон
        modifier = Modifier
            .wrapContentSize()
            .border(0.5.dp, ButtonDefaults.buttonColors().containerColor)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Назва деталі або найменування товару
            Text(
                text = item.name,
                color = Color.Black,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            // Унікальний складський артикул/код
            Text(
                text = item.code,
                color = Color.DarkGray,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Прев'ю-функція для візуального тестування відображення картки товару в Compose Preview.
 */
@Preview
@Composable
fun ItemPreview() {
    ItemCard(Product("08157-04", "1", "Piła taśmowa 322 THx2500x16", "1")) {
        // Порожній обробник натискання для режиму попереднього перегляду
    }
}


