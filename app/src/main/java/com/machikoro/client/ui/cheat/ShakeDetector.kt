package com.machikoro.client.ui.cheat

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.machikoro.client.domain.cheat.accelerationMagnitude
import com.machikoro.client.domain.cheat.isShake

private const val SHAKE_COOLDOWN_MS = 1_000L

/**
 * Registers an accelerometer listener while [enabled] and invokes [onShake] once
 * per deliberate shake, debounced by a 1 s cooldown so a single shake fires once.
 * Powers the hidden "Insider Trading" cheat (issue #203).
 *
 * The shake threshold itself lives in `domain/` ([isShake]) so it stays
 * unit-testable; this composable only wires the Android sensor lifecycle and is
 * excluded from coverage like the rest of `ui/`.
 */
@Composable
fun ShakeDetector(enabled: Boolean, onShake: () -> Unit) {
    val context = LocalContext.current
    val currentOnShake by rememberUpdatedState(onShake)

    DisposableEffect(enabled, context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (!enabled || sensorManager == null || accelerometer == null) {
            return@DisposableEffect onDispose { }
        }

        var lastShakeAt = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val magnitude = accelerationMagnitude(event.values[0], event.values[1], event.values[2])
                if (!isShake(magnitude)) return
                val now = SystemClock.elapsedRealtime()
                if (now - lastShakeAt < SHAKE_COOLDOWN_MS) return
                lastShakeAt = now
                currentOnShake()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager.unregisterListener(listener) }
    }
}
