package com.nyanthingy.mobileapp.modules.commons.extensions

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import kotlinx.coroutines.coroutineScope
import kotlin.math.hypot


/**
 * For converting pixels to dp
 */
@Composable
fun Float.pxToDp() = with(LocalDensity.current) { this@pxToDp.toDp() }

/**
 * Clears the local focus on region tap
 */
@Composable
fun Modifier.clearFocusOnTap(): Modifier
{
    val focusManager = LocalFocusManager.current
    return this.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(pass = PointerEventPass.Initial)
            val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
            if (upEvent != null) {
                focusManager.clearFocus()
            }
        }
    }
}

/**
 * Ignores parent padding by dp
 */
fun Modifier.ignoreParentPadding(horizontal: Dp = 0.dp, vertical: Dp = 0.dp): Modifier {
    return this.layout { measurable, constraints ->
        val overridenWidth = constraints.maxWidth + 2 * horizontal.roundToPx()
        val overridenHeight = constraints.maxHeight + 2 * vertical.roundToPx()

        val placeable = measurable.measure(
            constraints.copy(
                maxWidth = overridenWidth,
                maxHeight = overridenHeight
            )
        )
        layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }
}

/**
 * Shortcut for a dashed border
 */
fun Modifier.dashedBorder(strokeWidth: Dp, color: Color, cornerRadiusDp: Dp): Modifier
{
    return this.drawWithCache {
        onDrawBehind {
            val stroke = Stroke(
                width = strokeWidth.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )

            drawRoundRect(
                color = color,
                style = stroke,
                cornerRadius = CornerRadius(cornerRadiusDp.toPx())
            )
        }
    }
}


/**
 * Detect press gestures and pass the event to the callback rather than consuming it
 */
suspend fun PointerInputScope.detectPressGesturesUnconsumed(
    onLongPress: ((Offset, PointerEvent) -> Unit)? = null,
    onPressStart: ((Offset, PointerInputChange) -> Unit)? = null,
    onPressEnd: (() -> Unit)? = null
) = coroutineScope {

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)

        val downTime = System.currentTimeMillis()
        onPressStart?.invoke(down.position, down)

        val longPressTimeout = onLongPress?.let {
            viewConfiguration.longPressTimeoutMillis
        } ?: (Long.MAX_VALUE / 2)

        do {
            val event: PointerEvent = awaitPointerEvent()
            val currentTime = System.currentTimeMillis()

            if (currentTime - downTime >= longPressTimeout) {
                onLongPress?.invoke(event.changes.first().position, event)
            }

        } while (event.changes.fastAny { it.pressed })

        onPressEnd?.invoke()
    }
}

suspend fun PointerInputScope.detectZoomGesture(onZoom: (Float) -> Unit) {
    awaitPointerEventScope {
        var initialDistance: Float? = null
        while (true) {
            val event = awaitPointerEvent()
            val pointers = event.changes.filter { it.pressed }
            if (pointers.size == 2) {
                val currentDistance = calculateDistance(pointers[0].position.x, pointers[0].position.y, pointers[1].position.x, pointers[1].position.y)
                if (initialDistance == null) {
                    initialDistance = currentDistance
                } else {
                    val scale = currentDistance / initialDistance
                    onZoom(scale)
                    initialDistance = currentDistance
                }
            } else {
                initialDistance = null
            }
        }
    }
}

fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    return hypot(x2 - x1, y2 - y1)
}