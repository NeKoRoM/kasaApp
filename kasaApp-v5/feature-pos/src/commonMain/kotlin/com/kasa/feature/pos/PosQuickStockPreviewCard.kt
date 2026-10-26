package com.kasa.feature.pos

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasa.model.Product
import java.util.Locale

/**
 * Картка попереднього перегляду коригування залишку товару:
 * - Поточний залишок на складі в одиницях виміру;
 * - Очікуваний залишок після оприбуткування (+) або списання (-).
 */
@Composable
fun PosQuickStockPreviewCard(
    product: Product,
    predictedRemainder: Double,
    isNegative: Boolean,
    isNarrow: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = if (isNarrow) "Поточний:" else "Поточний залишок:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${String.format(Locale.US, "%.3f", product.remainder)} ${product.unitOfMeasurement}",
                    fontSize = if (isNarrow) 14.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(
                modifier = Modifier.weight(1f, fill = false),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = if (isNarrow) "Після зміни:" else "Буде після зміни:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${String.format(Locale.US, "%.3f", predictedRemainder)} ${product.unitOfMeasurement}",
                    fontSize = if (isNarrow) 14.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isNegative) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
