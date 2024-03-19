package com.example.springbreakchooser

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.snackbar.Snackbar
import java.util.Locale
import java.util.Objects
import kotlin.math.sqrt
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var languageSpinner: Spinner
    private lateinit var language: String
    private lateinit var editVoiceText: EditText
    private lateinit var adapter: Adapter
    private var initialposition: Int = 0
    private val REQUEST_CODE_SPEECH_INPUT: Int = 1
    private lateinit var language_input: String
    private var sensorManager: SensorManager? = null
    private var acceleration = 0f
    private var currentAcceleration = 0f
    private var lastAcceleration = 0f
    private var textToSpeech: TextToSpeech? = null
    private val vacationSpotsSpain = listOf(
        VacationSpot("Barcelona", "geo:41.3851,2.1734"),
        VacationSpot("Madrid", "geo:40.416944,-3.703333"),
        VacationSpot("Ibiza", "geo:38.98,1.43")
    )
    private val vacationSpotsAmerica = listOf(
        VacationSpot("Seattle", "geo:47.609722,-122.333056"),
        VacationSpot("Boston", "geo:42.360278,-71.057778"),
        VacationSpot("Detroit", "geo:42.331389,-83.045833")
    )
    private val vacationSpotsCzech = listOf(
        VacationSpot("Prague", "geo:50.0875,14.421389"),
        VacationSpot("Brno", "geo:49.1925,16.608333"),
        VacationSpot("Ostrava", "geo:49.835556,18.2925")
    )

    private val startSpeechRecognition =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val res: ArrayList<String>? =
                    data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                res?.get(0)?.let { result ->
                    editVoiceText.setText(result)
                }
            } else {
                Toast.makeText(
                    this@MainActivity, "Speech recognition failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        languageSpinner = findViewById<Spinner>(R.id.languagespinner)
        editVoiceText = findViewById(R.id.editvoicetext)

        val items = resources.getStringArray(R.array.languages)

        adapter = object : ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, items) {

            override fun isEnabled(position: Int): Boolean {
                // Disable the first item from Spinner
                // First item will be used for hint
                return position != 0
            }

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val view: TextView = super.getDropDownView(position, convertView, parent) as TextView
                //set the color of first item in the drop down list to gray
                if(position == 0) {
                    view.setTextColor(Color.GRAY)
                } else {
                    //here it is possible to define color for other items by
                    //view.setTextColor(Color.RED)
                }
                return view
            }


        }
        languageSpinner.adapter = adapter as ArrayAdapter<String>
        initialposition = languageSpinner.selectedItemPosition
        languageSpinner.setSelection(initialposition, false)
        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position != 0) {
                    language = resources.getStringArray(R.array.languages)[position]
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        R.string.instruction_speak,
                        Toast.LENGTH_SHORT
                    ).show()

                    language_input = when (language) {
                        "English" -> "en-US"
                        "Spanish" -> "es-ES"
                        "Czech" -> "cs-CZ"
                        else -> ""
                    }
                    sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
                    Objects.requireNonNull(sensorManager)!!
                        .registerListener(
                            sensorListener,
                            sensorManager!!
                                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                            SensorManager.SENSOR_DELAY_NORMAL
                        )

                    acceleration = 10f
                    currentAcceleration = SensorManager.GRAVITY_EARTH
                    lastAcceleration = SensorManager.GRAVITY_EARTH

                    startSpeechRecognition.launch(createSpeechRecognitionIntent(language_input))
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
    }

    private var lastShakeTime: Long = 0
    private val shakeDelay: Long = 2000 // 2 seconds delay
    private val sensorListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {

            val currentTime = System.currentTimeMillis()

            if (currentTime - lastShakeTime >= shakeDelay) {

                // Fetching x,y,z values
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                lastAcceleration = currentAcceleration

                // Getting current accelerations
                // with the help of fetched x,y,z values
                currentAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                val delta: Float = currentAcceleration - lastAcceleration
                acceleration = acceleration * 0.9f + delta

                // Display a Toast message if
                // acceleration value is over 12
                if (acceleration > 15) {
                    onShakeSpeak(language)
                    onShakeGoogleMaps(language)
                    lastShakeTime = currentTime
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    private fun onShakeSpeak(lang: String) {
        if (textToSpeech == null) {
            textToSpeech = TextToSpeech(this) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    Log.i("TTS", "TextToSpeech engine initialized successfully")
                } else {
                    Log.e("TTS", "Initialization failed!")
                }
            }
        }

        // Set the language for TextToSpeech
        val result = textToSpeech?.setLanguage(getLocale(lang))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("TTS", "The Language is not supported!")
            return
        }
        val text = when(lang){
            "English" -> "Hello"
            "Spanish" -> "Hola"
            "Czech" -> "Ahoj"
            else -> ""
        }
        // Speak the provided text in the selected language
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun getLocale(lang: String): Locale {
        return when (lang) {
            "English" -> Locale("en", "US")
            "Spanish" -> Locale("es", "ES")
            "Czech" -> Locale("cs", "CZ")
            else -> Locale.getDefault()
        }
    }

    override fun onResume() {
        sensorManager?.registerListener(
            sensorListener, sensorManager!!.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER
            ), SensorManager.SENSOR_DELAY_NORMAL
        )
        super.onResume()
    }

    override fun onPause() {
        sensorManager!!.unregisterListener(sensorListener)
        super.onPause()
    }

    private fun onShakeGoogleMaps(lang: String) {
        val index = Random.nextInt(0, 2)
        val resultSpot = when (lang) {
            "English" -> vacationSpotsAmerica[index]
            "Spanish" -> vacationSpotsSpain[index]
            "Czech" -> vacationSpotsCzech[index]
            else -> null
        }
        if(resultSpot != null){
            val mapIntentUri = Uri.parse("${resultSpot.location}?q=${resultSpot.name}")
            val mapIntent = Intent(Intent.ACTION_VIEW, mapIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps") // Specify Google Maps package
            startActivity(mapIntent)
        }
        else{
            Toast.makeText(applicationContext, "Select a Language", Toast.LENGTH_SHORT)
                .show()
        }
    }

    // this part was written by ChatGPT
    private fun createSpeechRecognitionIntent(lang: String): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to text")
        }
    }


    override fun onDestroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onDestroy()
    }

}
data class VacationSpot(val name: String, val location: String)



