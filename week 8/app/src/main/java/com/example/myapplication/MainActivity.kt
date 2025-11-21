package com.example.myapplication

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity(), SensorEventListener {
	private lateinit var sensorManager: SensorManager
    private lateinit var lsSensorList: ListView
    private var lightSensor: Sensor? = null
    private lateinit var txtSensorValues: TextView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_main)
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}


        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val deviceSensors: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_ALL)
        Log.v("Sensors", "Total sensors: ${deviceSensors.size}")

        val sensorNameList = ArrayList<String>()
        deviceSensors.forEach { sensor ->
            Log.v("Sensors", "Sensor names: ${sensor.name}")
            sensorNameList.add(sensor.name)
        }

        lsSensorList = findViewById(R.id.lsSensors)
        val adapter = ArrayAdapter(this, R.layout.template_layout, sensorNameList)
        lsSensorList.adapter = adapter

        txtSensorValues = findViewById(R.id.txtSensorValues)
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        if (lightSensor == null) {
            txtSensorValues.text = "Light sensor not available"
        }
	}

    override fun onResume() {
        super.onResume()
        lightSensor?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (event.sensor.type == Sensor.TYPE_LIGHT) {
            val lux = event.values[0]
            txtSensorValues.text = "Light: ${lux} lx"
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }
}









