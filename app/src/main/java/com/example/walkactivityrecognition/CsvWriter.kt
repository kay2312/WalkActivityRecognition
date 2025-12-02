package com.example.walkactivityrecognition

import android.content.Context
import java.io.File
import java.io.FileWriter

class CsvWriter(private val context: Context) {

    private var writer: FileWriter? = null
    private var file: File? = null

    fun startNewFile(): File {
        val fileName = "dataset_${System.currentTimeMillis()}.csv"
        val dir = context.getExternalFilesDir(null)

        file = File(dir, fileName)
        writer = FileWriter(file!!)

        writer!!.write("timestamp,type,x,y,z,class\n")
        writer!!.flush()

        return file!!
    }

    fun write(
        timestamp: Long,
        type: String,
        x: Float,
        y: Float,
        z: Float,
        classLabel: String
    ) {
        writer?.write("$timestamp,$type,$x,$y,$z,$classLabel\n")
        writer?.flush()
    }

    fun stop() {
        writer?.flush()
        writer?.close()
    }
}