package com.example.helpinghand.work

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest

private fun SensorManager.lightSensorFlow(): Flow<Float> = callbackFlow {
    val sensor = getDefaultSensor(Sensor.TYPE_LIGHT)
    if (sensor == null) {
        // No sensor: close immediately
        close()
        return@callbackFlow
    }

    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val lux = event.values.firstOrNull() ?: return
            trySend(lux).isSuccess
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // no-op
        }
    }

    registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)

    awaitClose {
        unregisterListener(listener)
    }
}

/**
 * Returns "isDark" based on the ambient light sensor, using coroutines + Flow.
 * Sensor is only active when [dynamicEnabled] is true and the composable is in the composition.
 */
@Composable
fun rememberIsDarkFromSensor(
    dynamicEnabled: Boolean,
    luxThreshold: Float = 10f
): Pair<Boolean, Boolean> {
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    // Check once if sensor exists
    val hasLightSensor = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null
    }

    var isDark by remember { mutableStateOf(false) }

    LaunchedEffect(dynamicEnabled, hasLightSensor, sensorManager) {
        if (!dynamicEnabled || !hasLightSensor) {
            isDark = false
            return@LaunchedEffect
        }

        // Collect lux as long as we are in composition & dynamic is on
        sensorManager.lightSensorFlow().collectLatest { lux ->
            isDark = lux < luxThreshold
        }
    }

    // Pair: (hasLightSensor, isDarkIfDynamic)
    return hasLightSensor to (dynamicEnabled && hasLightSensor && isDark)
}
