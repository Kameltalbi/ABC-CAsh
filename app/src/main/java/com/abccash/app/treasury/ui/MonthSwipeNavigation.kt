package com.abccash.app.treasury.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import java.time.YearMonth

/**
 * Swipe horizontally to change month: left = next month, right = previous month.
 * Order is always chronological (LTR), even on RTL devices.
 */
fun Modifier.monthSwipeNavigation(
    selectedMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    enabled: Boolean = true,
    minMonth: YearMonth? = null
): Modifier = if (!enabled) {
    this
} else {
    composed {
        val threshold = with(LocalDensity.current) { 72.dp.toPx() }
        var totalDrag by remember(selectedMonth) { mutableFloatStateOf(0f) }
        val layoutDirection = LocalLayoutDirection.current

        pointerInput(selectedMonth, layoutDirection, minMonth) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    val swipeLeft = when (layoutDirection) {
                        LayoutDirection.Rtl -> totalDrag > threshold
                        else -> totalDrag < -threshold
                    }
                    val swipeRight = when (layoutDirection) {
                        LayoutDirection.Rtl -> totalDrag < -threshold
                        else -> totalDrag > threshold
                    }
                    when {
                        swipeLeft -> onMonthChange(selectedMonth.plusMonths(1))
                        swipeRight -> {
                            val previous = selectedMonth.minusMonths(1)
                            if (minMonth == null || !previous.isBefore(minMonth)) {
                                onMonthChange(previous)
                            }
                        }
                    }
                    totalDrag = 0f
                },
                onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount }
            )
        }
    }
}
