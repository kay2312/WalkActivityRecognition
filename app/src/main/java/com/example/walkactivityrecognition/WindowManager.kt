package com.example.walkactivityrecognition

class WindowManager(private val windowSize: Int = 25) {

    private val buffer = FloatArray(windowSize * 6)
    private var pointer = 0

    fun add(acc: FloatArray, gyro: FloatArray): FloatArray? {
        if (acc.size < 3 || gyro.size < 3) return null

        val index = pointer * 6

        buffer[index] = acc[0]
        buffer[index + 1] = acc[1]
        buffer[index + 2] = acc[2]

        buffer[index + 3] = gyro[0]
        buffer[index + 4] = gyro[1]
        buffer[index + 5] = gyro[2]

        pointer++

        return if (pointer == windowSize) {
            pointer = 0
            buffer.clone()
        } else null
    }

    fun reset() {
        pointer = 0
    }
}