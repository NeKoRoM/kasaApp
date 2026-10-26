package com.kasa.feature.pos

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Візуальний оверлей прицілу камери для швидкісного наведення на штрих-код:
 * - Затемнений напівпрозорий фон з прозорим вікном сканування по центру;
 * - Кутові маркери візира кольору Primary / Accent;
 * - Анімована горизонтальна лазерна смуга для тактильного відчуття активного сканування.
 */
@Composable
fun PosCameraViewfinderOverlay(
    modifier: Modifier = Modifier,
    statusText: String = "Наведіть камеру на штрих-код"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "laserScan")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserY"
    )

    val cornerColor = MaterialTheme.colorScheme.tertiary
    val laserColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
    val frameColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    val hudBg = MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f)

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            val boxW = canvasW * 0.82f
            val boxH = canvasH * 0.65f
            val left = (canvasW - boxW) / 2f
            val top = (canvasH - boxH) / 2f

            // Рамка сканування
            drawRoundRect(
                color = frameColor,
                topLeft = Offset(left, top),
                size = Size(boxW, boxH),
                cornerRadius = CornerRadius(12f, 12f),
                style = Stroke(width = 2f)
            )

            // Кутові акцентні скоби
            val cornerLen = 28f
            val strokeW = 6f

            // Верхній лівий
            drawLine(cornerColor, Offset(left, top), Offset(left + cornerLen, top), strokeW)
            drawLine(cornerColor, Offset(left, top), Offset(left, top + cornerLen), strokeW)

            // Верхній правий
            drawLine(cornerColor, Offset(left + boxW, top), Offset(left + boxW - cornerLen, top), strokeW)
            drawLine(cornerColor, Offset(left + boxW, top), Offset(left + boxW, top + cornerLen), strokeW)

            // Нижній лівий
            drawLine(cornerColor, Offset(left, top + boxH), Offset(left + cornerLen, top + boxH), strokeW)
            drawLine(cornerColor, Offset(left, top + boxH), Offset(left, top + boxH - cornerLen), strokeW)

            // Нижній правий
            drawLine(cornerColor, Offset(left + boxW, top + boxH), Offset(left + boxW - cornerLen, top + boxH), strokeW)
            drawLine(cornerColor, Offset(left + boxW, top + boxH), Offset(left + boxW, top + boxH - cornerLen), strokeW)

            // Анімована лазерна лінія
            val currentLaserY = top + (boxH * laserPosition)
            drawLine(
                color = laserColor,
                start = Offset(left + 8f, currentLaserY),
                end = Offset(left + boxW - 8f, currentLaserY),
                strokeWidth = 3f
            )
        }

        // Текстова підказка внизу прицілу
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .background(hudBg, shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = statusText,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
