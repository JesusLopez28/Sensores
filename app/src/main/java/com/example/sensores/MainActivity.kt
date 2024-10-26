package com.example.sensores

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var activeSensor: Sensor? = null
    private var proximitySensor: Sensor? = null
    private var magneticSensor: Sensor? = null
    private var lightSensor: Sensor? = null
    private var tiltSensor: Sensor? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var imgChange: ImageView
    private lateinit var detalle: TextView
    private var isImageOne: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar SensorManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        tiltSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

        // Referencias de UI
        imgChange = findViewById(R.id.imageViewChange)
        detalle = findViewById(R.id.textView)

        // Configurar RecyclerView para la lista de sensores
        recyclerView = findViewById(R.id.recyclerViewSensors)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL).map { it.name }
        recyclerView.adapter = SensorAdapter(sensorList)

        // Configurar botones
        findViewById<Button>(R.id.btnMagneticSensor).setOnClickListener {
            activateSensor(magneticSensor, "Sensor magnético")
        }
        findViewById<Button>(R.id.btnProximitySensor).setOnClickListener {
            activateSensor(proximitySensor, "Sensor de proximidad")
        }
        findViewById<Button>(R.id.btnLightSensor).setOnClickListener {
            activateSensor(lightSensor, "Sensor de luz")
        }
        findViewById<Button>(R.id.btnTiltSensor).setOnClickListener {
            activateSensor(tiltSensor, "Sensor de inclinación")
        }
    }

    private fun activateSensor(sensor: Sensor?, sensorName: String) {
        // Desactivar sensor anterior
        sensorManager.unregisterListener(this)
        activeSensor = sensor

        // Registrar el sensor si está disponible
        sensor?.let {
            if (!sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)) {
                Toast.makeText(this, "$sensorName no se pudo registrar", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "$sensorName activado", Toast.LENGTH_SHORT).show()
            }
        } ?: Toast.makeText(this, "$sensorName no disponible", Toast.LENGTH_SHORT).show()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_PROXIMITY -> handleProximitySensor(it.values[0])
                Sensor.TYPE_MAGNETIC_FIELD -> handleMagneticSensor(it.values)
                Sensor.TYPE_LIGHT -> handleLightSensor(it.values[0])
                Sensor.TYPE_GAME_ROTATION_VECTOR -> handleTiltSensor(it.values)
            }
        }
    }

    private fun handleProximitySensor(value: Float) {
        // El sensor de proximidad devuelve si un objeto está cerca (< 1) o lejos
        if (value < 1.0) {
            // Objeto cerca
            detalle.textSize = 30f
            detalle.text = "CERCA: $value"
            imgChange.setImageResource(R.drawable.image_one)
        } else {
            // Objeto lejos
            detalle.text = "LEJOS: $value"
            imgChange.setImageResource(R.drawable.image_two)
        }
    }

    private fun handleMagneticSensor(values: FloatArray) {
        // Obtener la magnitud del campo magnético
        val magnitude = Math.sqrt(
            (values[0] * values[0] +
                    values[1] * values[1] +
                    values[2] * values[2]).toDouble()
        ).toFloat()

        // Cambiar el color y texto según la intensidad
        if (magnitude > 60) {
            detalle.setBackgroundColor(Color.RED)
            detalle.text = "Campo magnético FUERTE: $magnitude µT"
            imgChange.setImageResource(R.drawable.image_one)
        } else {
            detalle.setBackgroundColor(Color.GRAY)
            detalle.text = "Campo magnético normal: $magnitude µT"
            imgChange.setImageResource(R.drawable.image_two)
        }
    }

    private fun handleLightSensor(value: Float) {
        Log.d("LightSensor", "Intensidad de luz detectada, valor: $value")
        detalle.text = "Intensidad de luz: $value"
        if (value < 10) {
            imgChange.setImageResource(R.drawable.image_one)
        } else {
            imgChange.setImageResource(R.drawable.image_two)
        }
    }

    private fun handleTiltSensor(values: FloatArray) {
        val tilt = values[0] // Aquí tomamos solo el valor en el eje X para la inclinación
        imgChange.setImageResource(if (tilt > 0.5) R.drawable.image_one else R.drawable.image_two)
        Log.d("TiltSensor", "Inclinación detectada, valor: $tilt")
        detalle.text = "Inclinación detectada: $tilt"
    }

    private fun toggleImage() {
        imgChange.setImageResource(if (isImageOne) R.drawable.image_two else R.drawable.image_one)
        isImageOne = !isImageOne
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No es necesario implementar para esta lógica básica
    }

    override fun onPause() {
        super.onPause()
        // Desregistrar el sensor para ahorrar batería
        sensorManager.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        // Volver a registrar el sensor activo al reanudar la actividad
        activeSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    // Adaptador para el RecyclerView
    inner class SensorAdapter(private val sensorList: List<String>) :
        RecyclerView.Adapter<SensorAdapter.SensorViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SensorViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            return SensorViewHolder(view)
        }

        override fun onBindViewHolder(holder: SensorViewHolder, position: Int) {
            holder.sensorName.text = sensorList[position]
        }

        override fun getItemCount(): Int = sensorList.size

        inner class SensorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val sensorName: TextView = view.findViewById(android.R.id.text1)
        }
    }
}
