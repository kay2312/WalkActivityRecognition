package com.example.walkactivityrecognition

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class SensorRecorder(
    private val sensorManager: SensorManager,
    private val onAcc: (FloatArray) -> Unit,
    private val onGyro: (FloatArray) -> Unit
) : SensorEventListener {

    private var accValues = FloatArray(3)
    private var gyroValues = FloatArray(3)

    fun start() {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                accValues = event.values.clone()
                onAcc(accValues)
            }
            Sensor.TYPE_GYROSCOPE -> {
                gyroValues = event.values.clone()
                onGyro(gyroValues)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}