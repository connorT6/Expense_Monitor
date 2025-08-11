package com.connort6.expensemonitor.ui.views

import android.content.res.Configuration
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.connort6.expensemonitor.R
import com.connort6.expensemonitor.ui.theme.ExpenseMonitorTheme
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class PieChartData(
    val label: String,
    val value: Double,
    val iconPainter: ImageBitmap,
    val color: Color,
    var selected: Boolean = false,
    var angle: Float = 0f,
    var startAngle: Float = 0f,
    var scale: Animatable<Float, AnimationVector1D> = Animatable(1f),
) {
    fun calcAngleTotalSweep(totalValue: Double, currentSweep: Float): Float {
        angle = (value / totalValue * 360).toFloat()
        startAngle = currentSweep
        return currentSweep + angle
    }
}


//TODO convert all measurements to dp
@Composable
fun PieChart(pies: List<PieChartData>) {
    val totalValue = pies.sumOf { it.value }
    var totalSweep = 0f
    var pieList by remember {
        mutableStateOf(pies.mapIndexed {index, it ->
            if (index == 0){
                totalSweep = 0f;
            }
            it.apply {
                totalSweep = calcAngleTotalSweep(totalValue, totalSweep)
            }
        })
    }

    val imageSize = Size(50f, 50f)

    val strokeWidth = 30.dp
    var startAngle = 0f
    val coroutineScope = rememberCoroutineScope()
    val iconOffset = 30.dp
    val selectedScale = 1.1f
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val center = calculateCenterOffset(size.width, size.height)
                    val radius = calculatePieChartRadius(size.width, size.height)
                    val xOffset = tapOffset.x - center.x
                    val yOffset = tapOffset.y - center.y
                    val distance = sqrt(
                        xOffset.pow(2) + yOffset.pow(2)
                    )

                    val strokeWidthPx = with(density) { strokeWidth.toPx() }

                    // outer circle taking as 1.2 which is clicked
                    if (distance !in (radius - (strokeWidthPx / 2 + 20f))..((radius * selectedScale) + strokeWidthPx / 2 + 20f)) {
                        return@detectTapGestures // if the radius not in range abort
                    }

                    // this will return angle according to mathematical way (CCW) but canvas draws Clockwise
                    var angle = atan2(yOffset, xOffset) * 180 / PI
//                    angle = angle * -1 // converting to clockwise angle
                    if (angle < 0) { // first angle will return [0,180]/[0,-180] need to convert (-) values to positive
                        angle += 360
                    }

                    pieList = pieList.map { pieData ->
                        val newVal: PieChartData
                        if (angle in pieData.startAngle..pieData.startAngle + pieData.angle) {
                            newVal = pieData.copy(selected = !pieData.selected)
                        } else {
                            newVal = pieData.copy(selected = false)
                        }
                        coroutineScope.launch {
                            if (pieData.selected && !newVal.selected) {
                                newVal.scale.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(durationMillis = 300)
                                )
                            }
                            if (!pieData.selected && newVal.selected) {
                                newVal.scale.animateTo(
                                    targetValue = selectedScale,
                                    animationSpec = tween(durationMillis = 300)
                                )
                            }
                        }
                        newVal
                    }

                }
            }
    ) {

        val center = calculateCenterOffset(size.width, size.height)
        val strokeWidthPx = with(density) { strokeWidth.toPx() }


        pieList.forEach { pieData ->

            // when clicked radius will increased
            val radius = calculatePieChartRadius(size.width, size.height) * pieData.scale.value

            drawArc(
                color = pieData.color,
                startAngle = startAngle,
                sweepAngle = pieData.angle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidthPx)
            )

            // icon & details will show on the center of slice
            val middleAngle: Double = (startAngle + pieData.angle / 2).toDouble()
            val radAngle = middleAngle / 180 * PI // converting to radians

            val iconOffsetPx = with(density) { iconOffset.toPx() }
            val xOffset = (radius + (strokeWidthPx / 2) + iconOffsetPx) * cos(radAngle)
            val yOffset = (radius + (strokeWidthPx / 2) + iconOffsetPx) * sin(radAngle)
            val imageOffset = Offset(
                (center.x + xOffset).toFloat() - (with(density) {
                    imageSize.width.dp.toPx().toInt()
                } / 2),
                (center.y + yOffset).toFloat() - (with(density) {
                    imageSize.height.dp.toPx().toInt()
                } / 2)
            )

            val offsetVal =
                sqrt(
                    (center.x + xOffset).toFloat().pow(2) + (center.y + yOffset).toFloat()
                        .pow(2)
                )

            Log.e(
                "TAG",
                "PieChart: label : ${pieData.label}, radius: $radius, offset: $offsetVal, diff : ${offsetVal - radius}, x : $xOffset, y : $yOffset"
            )

            val intOffset = IntOffset(imageOffset.x.toInt(), imageOffset.y.toInt())

            drawImage(
                pieData.iconPainter, dstOffset = intOffset,
                dstSize = IntSize(
                    with(density) { imageSize.width.dp.toPx().toInt() },
                    with(density) { imageSize.height.dp.toPx().toInt() }
                )
            )
            startAngle += pieData.angle
        }

    }
}

/**
 * Calculates the center offset for a given width and height.
 * width and height must be in pixels
 */
private fun calculateCenterOffset(width: Number, height: Number): Offset {
    return Offset(width.toInt() / 2f, height.toInt() / 2f)
}

private fun calculatePieChartRadius(width: Number, height: Number): Float {
    return min(width.toFloat(), height.toFloat()) / 4f
}

@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
fun PieChartPreview() {
    ExpenseMonitorTheme {
        val shuffled = materialPieColors.shuffled()
        val pies = listOf(
            PieChartData(
                "Label 1",
                10.0,
                ImageBitmap.imageResource(R.drawable.ic_fns),
                Color.Blue
            ),
            PieChartData(
                "Label 2",
                20.0,
                ImageBitmap.imageResource(R.drawable.ic_bank_card3),
                Color.Red
            ),
            PieChartData(
                "Label 3",
                30.0,
                ImageBitmap.imageResource(R.drawable.ic_accounts),
                Color.Green
            ),
            PieChartData(
                "Label 4",
                40.0,
                ImageBitmap.imageResource(R.drawable.ic_beaty),
                Color.Yellow
            )
        ).mapIndexed { index, data -> data.copy(color = shuffled[index]) }
        PieChart(
            pies = pies
        )
    }
}

val materialPieColors = listOf(
    Color(0xFFF44336),   // 1. Red
    Color(0xFFE91E63),   // 2. Pink
    Color(0xFFFF9800),   // 3. Orange
    Color(0xFFFFC107),   // 4. Amber
    Color(0xFF8BC34A),   // 5. Light Green
    Color(0xFF4CAF50),   // 6. Green
    Color(0xFF009688),   // 7. Teal
    Color(0xFF00BCD4),   // 8. Cyan
    Color(0xFF03A9F4),   // 9. Light Blue
    Color(0xFF2196F3),   // 10. Blue
    Color(0xFF3F51B5),   // 11. Indigo
    Color(0xFF673AB7),   // 12. Deep Purple
    Color(0xFF9C27B0),   // 13. Purple
    Color(0xFFEE77A2),   // 14. Custom Pink/Magenta
    Color(0xFF795548),   // 15. Brown
    Color(0xFFFFEB3B),   // 16. Yellow
    Color(0xFF607D8B),   // 17. Blue Grey
    Color(0xFFCDDC39),   // 18. Lime
    Color(0xFFFF5722),   // 19. Deep Orange
    Color(0xFF4DD0E1)    // 20. Cyan 300 (Lighter Cyan)
)