package com.kruju.habits.ui

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kruju.habits.HabitViewModel
import kotlinx.coroutines.delay

private val Presets = listOf(1, 5, 10, 15, 20, 25, 30, 45, 60, 90)

@Composable
fun TimerScreen(vm: HabitViewModel, contentPadding: PaddingValues) {
    val timer by vm.timer.collectAsState()
    val haptics = LocalHapticFeedback.current
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

    LaunchedEffect(timer.running) {
        while (timer.running) {
            now = SystemClock.elapsedRealtime()
            delay(100)
        }
        now = SystemClock.elapsedRealtime()
    }

    val elapsed = timer.elapsed(now)
    val remaining = (timer.durationMs - elapsed).coerceAtLeast(0)
    val finished = timer.countdown && remaining == 0L && timer.running

    LaunchedEffect(finished) {
        if (finished) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            val tone = try {
                ToneGenerator(AudioManager.STREAM_ALARM, 90)
            } catch (e: RuntimeException) {
                null // No audio available; the vibration is enough.
            }
            tone?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1500)
            // Reset only after the tone: resetting changes `finished` and would cancel this effect.
            try {
                delay(1600)
            } finally {
                tone?.release()
            }
            vm.startPauseTimer()
            vm.resetTimer()
        }
    }

    val shownMs = if (timer.countdown) remaining else elapsed
    val progress = if (timer.countdown) {
        if (timer.durationMs == 0L) 0f else remaining.toFloat() / timer.durationMs
    } else {
        (elapsed % 60_000L) / 60_000f
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("Stopwatch", !timer.countdown, { vm.setTimerMode(false) })
            Pill("Countdown", timer.countdown, { vm.setTimerMode(true) })
        }
        Spacer(Modifier.height(24.dp))
        Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 10.dp.toPx()
                val inset = stroke / 2
                val arc = Size(size.width - stroke, size.height - stroke)
                drawArc(Palette.Card, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
                drawArc(
                    Palette.Accent, -90f, 360f * progress, false, Offset(inset, inset), arc,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(formatClock(shownMs, roundUp = timer.countdown), fontSize = 48.sp, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        timer.running -> "Running"
                        elapsed > 0 -> "Paused"
                        else -> if (timer.countdown) "Countdown" else "Stopwatch"
                    },
                    color = Palette.TextSoft,
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = { vm.resetTimer() }, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Filled.Replay, contentDescription = "Reset")
            }
            FilledIconButton(
                onClick = { vm.startPauseTimer() },
                modifier = Modifier.size(76.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Palette.Accent),
            ) {
                Icon(
                    if (timer.running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (timer.running) "Pause" else "Start",
                    modifier = Modifier.size(40.dp),
                )
            }
            Box(Modifier.size(56.dp))
        }
        if (timer.countdown) {
            Spacer(Modifier.height(32.dp))
            Text("Duration", style = MaterialTheme.typography.labelLarge, color = Palette.TextSoft)
            Spacer(Modifier.height(8.dp))
            LazyRow(
                Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(Presets) { minutes ->
                    Pill("$minutes min", timer.durationMs == minutes * 60_000L, { vm.setCountdownDuration(minutes * 60_000L) })
                }
            }
        }
    }
}

// A countdown rounds up so it shows 00:01 until it really ends; a stopwatch rounds down.
private fun formatClock(ms: Long, roundUp: Boolean): String {
    val totalSeconds = if (roundUp) (ms + 999) / 1000 else ms / 1000
    val hours = totalSeconds / 3600
    val minutes = totalSeconds / 60 % 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
}
