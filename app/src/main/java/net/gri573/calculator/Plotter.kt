package net.gri573.calculator

import android.annotation.SuppressLint
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Card
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

fun mapRange(x : Float, inLowerBound : Float = 0.0F, inUpperBound : Float = 1.0F, outLowerBound : Float = 0.0F, outUpperBound : Float = 1.0F) : Float {
    val inRange = inUpperBound - inLowerBound
    val outRange = outUpperBound - outLowerBound
    return (x - inLowerBound) / inRange * outRange + outLowerBound
}
fun clamp(x : Float, lower : Float, upper : Float) : Float {
    if (x < lower) {
        return lower
    }
    if (x > upper) {
        return upper
    }
    return x
}

@SuppressLint("DefaultLocale")
@Composable
fun PlotFunction(modifier : Modifier = Modifier, func : MathFunction, showPlot : MutableState<Boolean>) {
    val plotScale = remember {mutableFloatStateOf(2.0F)}
    val scrollCenter = remember {mutableStateOf(Offset.Zero)}
    Dialog(onDismissRequest = {showPlot.value = false}) {
        Card {
            Text(
                text = func.description,
                textAlign = TextAlign.Center
            )
            val textMeasurer = rememberTextMeasurer()
            Spacer(
                modifier = modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5F)
                    .transformable(rememberTransformableState { zoomChange, offsetChange, rotationChange ->
                        plotScale.floatValue /= zoomChange
                        scrollCenter.value += Offset(
                            plotScale.floatValue * offsetChange.x,
                            -plotScale.floatValue * offsetChange.y
                        )
                    })
                    .drawBehind {
                        val mappedSizeX = plotScale.floatValue
                        val mappedSizeY = -plotScale.floatValue * size.height / size.width
                        val plotCenterX = -scrollCenter.value.x / size.width
                        val plotCenterY = -scrollCenter.value.y / size.height
                        val plotBounds: List<List<Float>> = listOf(
                            listOf(
                                plotCenterX - 0.5F * mappedSizeX,
                                plotCenterX + 0.5F * mappedSizeX
                            ),
                            listOf(
                                plotCenterY - 0.5F * mappedSizeY,
                                plotCenterY + 0.5F * mappedSizeY
                            )
                        )
                        val tickDist = 10.0.pow(floor(log10(plotScale.floatValue) - 0.4F).toDouble()).toFloat()
                        val labelEvery = (
                            10.0.pow(floor(log10(plotScale.floatValue) - 0.1F).toDouble()).toFloat() /
                            tickDist
                        ).roundToInt()
                        val digits = max(0, -log10(tickDist * labelEvery).roundToInt())
                        var xTick = floor(plotBounds[0][0] / tickDist) * tickDist
                        var yTick = floor(plotBounds[1][1] / tickDist) * tickDist

                        val xValues: List<Float> = FloatArray(size.width.toInt() / 10 + 1) {
                            val mixFactor = it * 10.0F / size.width
                            mixFactor * (plotCenterX + 0.5F * mappedSizeX) +
                            (1.0F - mixFactor) * (plotCenterX - 0.5F * mappedSizeX)
                        }.toList()
                        var yValues : List<Float?>
                        try {
                            val yValues0: List<Double?> =
                                func.action(listOf(List<Double?>(xValues.size) { xValues[it].toDouble() }))
                            yValues =
                                List(xValues.size) { if(yValues0[it] != null && !(yValues0[it]!!.isNaN())) {yValues0[it]!!.toFloat()} else {null} }
                        } catch (e : NumberFormatException) {
                            yValues = List(xValues.size) {null}
                        }
                        /*val yValues: MutableList<Float?> = mutableListOf()
                        for (i in 0..size.width.toInt() / 10) {
                            try {
                                val thisY = func.action(listOf(DoubleArray(xValues[i].toDouble())).toFloat()
                                if (thisY != thisY || abs(thisY - plotCenterY) > 1e5F * plotScale.floatValue) {
                                    throw NumberFormatException("Don't want any NaNs or infs here!")
                                }
                                yValues.add(thisY)
                            } catch (e: Exception) {
                                yValues.add(null)
                            }
                        }*/

                        while (xTick < plotBounds[0][1]) {
                            val tickPos = Offset(
                                mapRange(xTick, plotBounds[0][0], plotBounds[0][1], 0.0F, size.width) - 1.dp.toPx(),
                                clamp(mapRange(0.0F, plotBounds[1][0], plotBounds[1][1], 0.0F, size.height) - 3.dp.toPx(), -3.dp.toPx(), size.height - 3.dp.toPx())
                            )
                            drawRect(
                                color = Color.Gray,
                                size = Size(2.dp.toPx(), 6.dp.toPx()),
                                topLeft = tickPos
                            )
                            if ((xTick / tickDist).roundToInt() % labelEvery == 0) {
                                val measuredText = textMeasurer.measure(AnnotatedString(String.format("%.${digits}f", xTick)))
                                drawText(
                                    measuredText,
                                    color = Color.LightGray,
                                    topLeft = tickPos + Offset(
                                        -measuredText.size.width * 0.5F,
                                        min(4.dp.toPx(), size.height - measuredText.size.height - tickPos.y)
                                    )
                                )
                            }
                            xTick += tickDist
                        }
                        while (yTick < plotBounds[1][0]) {
                            val tickPos = Offset(
                                clamp(mapRange(0.0F, plotBounds[0][0], plotBounds[0][1], 0.0F, size.width) - 3.dp.toPx(), -3.dp.toPx(), size.width - 3.dp.toPx()),
                                mapRange(yTick, plotBounds[1][0], plotBounds[1][1], 0.0F, size.height) - 1.dp.toPx()
                            )
                            drawRect(
                                color = Color.Gray,
                                size = Size(6.dp.toPx(), 2.dp.toPx()),
                                topLeft = tickPos
                            )
                            if ((yTick / tickDist).roundToInt() % labelEvery == 0) {
                                val measuredText = textMeasurer.measure(AnnotatedString(String.format("%.${digits}f", yTick)))
                                drawText(
                                    measuredText,
                                    color = Color.LightGray,
                                    topLeft = tickPos + Offset(
                                        min(4.dp.toPx(), size.width - measuredText.size.width - tickPos.x),
                                        -measuredText.size.height * 0.5F
                                    )
                                )
                            }
                            yTick += tickDist
                        }
                        drawRect(
                            color = Color.Gray,
                            size = Size(3.dp.toPx(), size.height + 30.dp.toPx()),
                            topLeft = Offset(
                                mapRange(0.0F, plotBounds[0][0], plotBounds[0][1], 0.0F, size.width) - 1.5.dp.toPx(),
                                -30.dp.toPx()
                            )
                        )
                        drawRect(
                            color = Color.Gray,
                            size = Size(size.width, 3.dp.toPx()),
                            topLeft = Offset(
                                0.0F,
                                mapRange(0.0F, plotBounds[1][0], plotBounds[1][1], 0.0F, size.height) - 1.5.dp.toPx()
                            )
                        )
                        val path = Path()
                        var prevValid = false
                        for (i in xValues.indices) {
                            if (yValues[i] != null) {
                                val newOffset = Offset(
                                    mapRange(
                                        xValues[i],
                                        plotBounds[0][0],
                                        plotBounds[0][1],
                                        0.0F,
                                        size.width
                                    ),
                                    mapRange(
                                        yValues[i]!!,
                                        plotBounds[1][0],
                                        plotBounds[1][1],
                                        0.0F,
                                        size.height
                                    )
                                )
                                if (prevValid) {
                                    path.lineTo(newOffset.x, newOffset.y)
                                } else {
                                    path.moveTo(newOffset.x, newOffset.y)
                                }
                                prevValid = true
                            } else {
                                prevValid = false
                            }
                        }
                        drawPath(path, color = Color.Magenta, style = Stroke(width = 2.dp.toPx()))
                    }
            )
        }
    }
}

@Composable
fun FunctionEntry (modifier : Modifier = Modifier, func : MathFunction) {
    val showPlot = remember { mutableStateOf(false) }
    if (showPlot.value) {
        PlotFunction(modifier, func, showPlot)
    }

    ClickableText(
        modifier = Modifier.padding(start = 8.dp, end = 8.dp),
        text = AnnotatedString(func.description),
        style = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            color = LocalContentColor.current
        ),
        onClick = {showPlot.value = true},
    )
}