package com.example.walkactivityrecognition

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TFLiteModel(context: Context) {

    private val interpreter: Interpreter

    private val WINDOW = 25
    private val FEATURES = 6

    val classes = arrayOf(
        "slow_walk",
        "normal_walk",
        "fast_walk",
        "run",
        "stand"
    )

    init {
        interpreter = Interpreter(loadModelFile(context))
    }

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd("walk_activity_model.tflite")
        val inputStream = fileDescriptor.createInputStream()
        val channel = inputStream.channel
        return channel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    fun predictWithProbabilities(inputData: FloatArray): Pair<String, FloatArray> {

        val inputBuffer = ByteBuffer.allocateDirect(4 * WINDOW * FEATURES)
        inputBuffer.order(ByteOrder.nativeOrder())
        inputData.forEach { inputBuffer.putFloat(it) }

        val outputBuffer = ByteBuffer.allocateDirect(4 * 5)
        outputBuffer.order(ByteOrder.nativeOrder())

        interpreter.run(inputBuffer, outputBuffer)

        outputBuffer.rewind()
        val outputs = FloatArray(5)
        for (i in outputs.indices) outputs[i] = outputBuffer.getFloat()

        val maxIndex = outputs.indices.maxByOrNull { outputs[it] } ?: 0

        return classes[maxIndex] to outputs
    }
}
