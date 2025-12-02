package com.example.walkactivityrecognition

import android.graphics.Color
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var sensorManager: SensorManager

    private lateinit var recorder: SensorRecorder
    private lateinit var csvWriter: CsvWriter
    private lateinit var windowManager: WindowManager
    private lateinit var tfliteModel: TFLiteModel

    private var accelValues = FloatArray(3)
    private var gyroValues = FloatArray(3)

    private lateinit var statusText: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var currentValues: TextView
    private lateinit var predictionResult: TextView
    private lateinit var classSpinner: Spinner
    private lateinit var probContainer: LinearLayout

    private var isCollecting = false
    private var selectedClass = "stand"
    private var lastPredicted = "unknown"

    private val labelsUkr = mapOf(
        "slow_walk" to "Повільна хода",
        "normal_walk" to "Звичайна хода",
        "fast_walk" to "Швидка хода",
        "run" to "Біг",
        "stand" to "Стояння"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        tfliteModel = TFLiteModel(this)

        windowManager = WindowManager(25)
        csvWriter = CsvWriter(this)

        statusText = findViewById(R.id.statusText)
        btnStart = findViewById(R.id.startBtn)
        btnStop = findViewById(R.id.stopBtn)
        currentValues = findViewById(R.id.currentValues)
        predictionResult = findViewById(R.id.predictionResult)
        classSpinner = findViewById(R.id.classSpinner)
        probContainer = findViewById(R.id.probContainer)

        setupSpinner()
        setupRecorder()

        btnStart.setOnClickListener { startCollecting() }
        btnStop.setOnClickListener { stopCollecting() }
    }

    private fun setupSpinner() {

        val classNames = arrayOf(
            "Стояння", "Повільна хода", "Звичайна хода",
            "Швидка хода", "Біг", "Автоматичне розпізнавання"
        )

        val classCodes = arrayOf(
            "stand", "slow_walk", "normal_walk",
            "fast_walk", "run", "auto_predict"
        )

        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            classNames
        )
        adapter.setDropDownViewResource(
            R.layout.spinner_dropdown_item
        )

        classSpinner.adapter = adapter

        classSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                selectedClass = classCodes[pos]
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupRecorder() {
        recorder = SensorRecorder(
            sensorManager,
            onAcc = { acc ->
                accelValues = acc
                updateCurrentValues()
            },
            onGyro = { gyro ->
                gyroValues = gyro
                updateCurrentValues()

                if (isCollecting) {

                    val timestamp = System.currentTimeMillis()
                    val label = if (selectedClass == "auto_predict")
                        "predicted_$lastPredicted" else selectedClass

                    csvWriter.write(timestamp, "ACC",
                        accelValues[0], accelValues[1], accelValues[2], label)

                    csvWriter.write(timestamp, "GYRO",
                        gyroValues[0], gyroValues[1], gyroValues[2], label)

                    val window = windowManager.add(accelValues, gyroValues)
                    if (window != null) processPrediction(window)
                }
            }
        )
    }

    private fun updateCurrentValues() {
        currentValues.text = """
            Акселерометр:
            ${accelValues[0].f()} / ${accelValues[1].f()} / ${accelValues[2].f()}
            
            Гіроскоп:
            ${gyroValues[0].f()} / ${gyroValues[1].f()} / ${gyroValues[2].f()}
        """.trimIndent()

        currentValues.setTextColor(Color.parseColor("#4A70A9"))
        currentValues.textAlignment = View.TEXT_ALIGNMENT_CENTER
    }

    private fun startCollecting() {
        if (isCollecting) return
        isCollecting = true

        csvWriter.startNewFile()
        windowManager.reset()
        recorder.start()

        statusText.text = "Запис активний"
    }

    private fun stopCollecting() {
        isCollecting = false
        recorder.stop()
        csvWriter.stop()

        statusText.text = "Запис зупинено"
    }

    private fun processPrediction(window: FloatArray) {
        val (predicted, probs) = tfliteModel.predictWithProbabilities(window)
        lastPredicted = predicted

        predictionResult.text = "Результат: ${labelsUkr[predicted] ?: predicted}"

        showProbabilities(probs)
    }

    private fun showProbabilities(probs: FloatArray) {
        probContainer.removeAllViews()

        probs.forEachIndexed { index, value ->
            val label = labelsUkr[tfliteModel.classes[index]] ?: tfliteModel.classes[index]

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 8, 0, 8)
            }

            val title = TextView(this).apply {
                text = "$label: ${(value * 100).format(1)}%"
                textSize = 16f
                setTextColor(Color.parseColor("#4A70A9"))
            }

            val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100
                progress = (value * 100).toInt()
                progressDrawable = getDrawable(R.drawable.progress_blue)
            }

            row.addView(title)
            row.addView(bar)
            probContainer.addView(row)
        }
    }

    private fun Float.f(): String = String.format("%.2f", this)
    private fun Float.format(d: Int): String = "%.${d}f".format(this)
}