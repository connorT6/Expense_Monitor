package com.connort6.expensemonitor.ui.views

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
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
    fun toggleSelection(selected: Boolean) {
        this.selected = selected
    }

    fun isSelected(): Boolean {
        return selected
    }

    fun calcAngleTotalSweep(totalValue: Double, currentSweep: Float): Float {
        angle = (value / totalValue * 360).toFloat()
        startAngle = currentSweep
        return currentSweep + angle;
    }
}


//TODO convert all measurements to dp
@Composable
fun PieChart(pies: List<PieChartData>) {
    val totalValue = pies.sumOf { it.value }
    var pieList by remember {
        mutableStateOf(pies)
    }

    val imageSize = Size(40f, 40f)

    LaunchedEffect(pies) {
        var totalSweep = 0f
        pieList = pies.map {
            it.apply {
                totalSweep = calcAngleTotalSweep(totalValue, totalSweep)
            }
        }
    }

    val holeRadius: Dp = 40.dp // Define the radius of the hole in the center
    var startAngle = 0f
    val coroutineScope = rememberCoroutineScope()

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = min(size.width, size.height) / 4f
                    val xOffset = tapOffset.x - center.x
                    val yOffset = tapOffset.y - center.y
                    val distance = sqrt(
                        xOffset.pow(2) + yOffset.pow(2)
                    )

                    val strokeWidth = radius - holeRadius.toPx()
                    if (distance !in (radius - (strokeWidth / 2 + 20f))..(radius + strokeWidth / 2 + 20f)) {
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
                                    targetValue = 1.15f,
                                    animationSpec = tween(durationMillis = 300)
                                )
                            }
                        }
                        newVal
                    }

                }
            }
    ) {

        val center = Offset(size.width / 2f, size.height / 2f)


        pieList.forEach { pieData ->

            val radius = (size.minDimension / 4f) * pieData.scale.value

            val strokeWidth = radius - holeRadius.toPx()

            drawArc(
                color = pieData.color,
                startAngle = startAngle,
                sweepAngle = pieData.angle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth)
            )

            val middleAngle: Double = (startAngle + pieData.angle / 2).toDouble()
            val radAngle = middleAngle / 180 * PI
            val xOffset = (radius + 150f) * cos(radAngle)
            val yOffset = (radius + 150f) * sin(radAngle)
            val imageOffset = Offset(
                (center.x + xOffset).toFloat() - (with(density) { imageSize.width.dp.toPx().toInt() } / 2),
                (center.y + yOffset).toFloat() - (with(density) { imageSize.height.dp.toPx().toInt() } / 2)
            )

            val offsetVal =
                sqrt((center.x + xOffset).toFloat().pow(2) + (center.y + yOffset).toFloat().pow(2))

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

@Composable
fun SimplePieChart(
    data: List<PieChartData>,
    modifier: Modifier = Modifier,
    holeRadius: Dp = 40.dp
) {
    val totalValue = data.sumOf { it.value.toDouble() }.toFloat()
    var startAngle = 0f

    // We'll store the size of the box here
    var chartSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            // This is the key: we get the size of the Box when it's laid out
            .onSizeChanged {
                chartSize = it
            },
        contentAlignment = Alignment.Center
    ) {
        val minDimension = min(chartSize.width, chartSize.height).toFloat()

        if (minDimension > 0) {
            Canvas(modifier = Modifier.size(minDimension.dp)) {
                val pieRadius = size.minDimension / 2
                val strokeWidth = pieRadius - holeRadius.toPx()

                data.forEach { pieData ->
                    val sweepAngle: Float = ((pieData.value / totalValue) * 360f).toFloat()

                    drawArc(
                        color = pieData.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth)
                    )

                    startAngle += sweepAngle
                }
            }
        }
    }
}

@Preview
@Composable
fun PieChartPreview() {
    ExpenseMonitorTheme {
        PieChart(
            pies = listOf(
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
            )
        )
    }
}