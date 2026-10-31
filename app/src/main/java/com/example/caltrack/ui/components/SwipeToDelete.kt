package com.example.caltrack.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.caltrack.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/** Share of the row's width past which letting go deletes it. */
private const val DeleteThreshold = 0.4f

/** Space between the row and the delete pill behind it. */
private val PillGap = 8.dp
private val IconSize = 24.dp
private val FlingVelocity = 800.dp

/**
 * Gmail-style swipe-to-delete in both directions. A full-height pill grows behind the row on the
 * side it leaves from; past the threshold the pill turns to the error color and its icon pops.
 * Letting go past the threshold (or flinging) slides the row out, collapses it, and only then
 * calls [onDelete]. All per-frame values are read in the layout/draw phases only.
 *
 * @param cornerRadius the row's own corner radius at rest; it rounds up to a full pill as the
 *   drag nears the threshold.
 * @param expandOnEnter grows the row in vertically when it first appears, e.g. after an Undo.
 */
@Composable
fun SwipeToDeleteBox(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    expandOnEnter: Boolean = false,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val colors = MaterialTheme.colorScheme
    val motionScheme = MaterialTheme.motionScheme
    val currentOnDelete by rememberUpdatedState(onDelete)
    val trash = painterResource(R.drawable.ic_delete)

    val swipe = remember { SwipeState() }
    val visibility = remember { MutableTransitionState(!expandOnEnter).apply { targetState = true } }
    val flingVelocity = with(density) { FlingVelocity.toPx() }

    fun onDrag(delta: Float) {
        if (swipe.dismissing) return
        swipe.offset += delta
        val past = swipe.width > 0f && abs(swipe.offset) >= swipe.width * DeleteThreshold
        if (past == swipe.pastThreshold) return
        swipe.pastThreshold = past
        // Once per crossing, not for as long as the drag stays past it
        if (past) {
            haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
            scope.launch {
                swipe.iconScale.animateTo(1.3f, spring(stiffness = Spring.StiffnessHigh))
                swipe.iconScale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
            }
        }
        scope.launch { swipe.alert.animateTo(if (past) 1f else 0f, tween(150)) }
    }

    fun settle(velocity: Float) {
        if (swipe.dismissing) return
        val start = swipe.offset
        val flung = abs(velocity) > flingVelocity && start != 0f && sign(velocity) == sign(start)
        swipe.settleJob = scope.launch {
            if (swipe.pastThreshold || flung) {
                swipe.dismissing = true
                animate(start, sign(start) * swipe.width, animationSpec = tween(200, easing = FastOutLinearInEasing)) { value, _ ->
                    swipe.offset = value
                }
                visibility.targetState = false
                snapshotFlow { visibility.isIdle }.first { it }
                currentOnDelete()
            } else {
                animate(
                    initialValue = start,
                    targetValue = 0f,
                    initialVelocity = velocity,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
                ) { value, _ -> swipe.offset = value }
            }
        }
    }

    AnimatedVisibility(
        visibleState = visibility,
        modifier = modifier,
        enter = expandVertically(motionScheme.defaultSpatialSpec(), expandFrom = Alignment.Top) +
            fadeIn(motionScheme.defaultEffectsSpec()),
        exit = shrinkVertically(tween(250)) + fadeOut(tween(250)),
    ) {
        Box(
            Modifier
                .onSizeChanged { swipe.width = it.width.toFloat() }
                .drawBehind {
                    val offset = swipe.offset
                    val pillWidth = abs(offset) - PillGap.toPx()
                    if (pillWidth <= 0f) return@drawBehind
                    // Anchored on the side the row leaves from
                    val left = if (offset > 0f) 0f else size.width - pillWidth
                    val alert = swipe.alert.value
                    drawRoundRect(
                        color = lerp(colors.errorContainer, colors.error, alert),
                        topLeft = Offset(left, 0f),
                        size = Size(pillWidth, size.height),
                        cornerRadius = CornerRadius(size.height / 2f),
                    )
                    val iconPx = IconSize.toPx()
                    // Fades in while the pill grows from one to two icon widths
                    val iconAlpha = ((pillWidth - iconPx) / iconPx).coerceIn(0f, 1f)
                    if (iconAlpha > 0f) {
                        val scale = swipe.iconScale.value
                        clipRect(left, 0f, left + pillWidth, size.height) {
                            withTransform({
                                translate(left + (pillWidth - iconPx) / 2f, (size.height - iconPx) / 2f)
                                scale(scale, scale, pivot = Offset(iconPx / 2f, iconPx / 2f))
                            }) {
                                with(trash) {
                                    draw(
                                        size = Size(iconPx, iconPx),
                                        alpha = iconAlpha,
                                        colorFilter = ColorFilter.tint(lerp(colors.onErrorContainer, colors.onError, alert)),
                                    )
                                }
                            }
                        }
                    }
                },
        ) {
            Box(
                Modifier
                    .graphicsLayer {
                        val offset = swipe.offset
                        translationX = offset
                        val progress = if (swipe.width > 0f) {
                            (abs(offset) / (swipe.width * DeleteThreshold)).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        val rest = cornerRadius.toPx()
                        shape = RoundedCornerShape(rest + (size.height / 2f - rest).coerceAtLeast(0f) * progress)
                        clip = true
                    }
                    .draggable(
                        state = rememberDraggableState(::onDrag),
                        orientation = Orientation.Horizontal,
                        onDragStarted = { swipe.settleJob?.cancel() },
                        onDragStopped = { velocity -> settle(velocity) },
                    ),
            ) {
                content()
            }
        }
    }
}

/** Plain holder: the per-frame values are snapshot state read only while laying out and drawing. */
private class SwipeState {
    var offset by mutableFloatStateOf(0f)
    var width = 0f
    var pastThreshold = false
    var dismissing = false
    var settleJob: Job? = null

    /** 0 = errorContainer pill, 1 = error pill. */
    val alert = Animatable(0f)
    val iconScale = Animatable(1f)
}
