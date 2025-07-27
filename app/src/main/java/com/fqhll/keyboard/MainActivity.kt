package com.fqhll.keyboard

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.fqhll.keyboard.databinding.ActivityMainBinding
import androidx.core.content.edit

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private lateinit var binding: ActivityMainBinding

    var colors = arrayOf("black", "dark blue")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val capsToggle: SwitchCompat = findViewById(R.id.capsToggle)
        val prefs = getSharedPreferences("keyboard_settings", Context.MODE_PRIVATE)

        // Load saved toggle state
        capsToggle.isChecked = prefs.getBoolean("default_caps_enabled", false)

        // Save toggle changes
        capsToggle.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean("default_caps_enabled", isChecked) }
        }

        val keyBackgroundColor: Spinner = findViewById(R.id.spinner_options)
        val savedColor = prefs.getString("key_color", "black") // Default to "black"
        prefs.edit { putString("key_color", savedColor) }

        // the dropdown
        val aa = ArrayAdapter(this, R.layout.spinner, colors)
        aa.setDropDownViewResource(R.layout.spinner)

        with(keyBackgroundColor) {
            adapter = aa
            setSelection(0, false)
            onItemSelectedListener = this@MainActivity
            setPopupBackgroundResource(R.color.custom_cyan)
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val prefs = getSharedPreferences("keyboard_settings", Context.MODE_PRIVATE)
        val selectedColor = colors[position] // Get the selected color
        prefs.edit { putString("key_color", selectedColor) } // Save the selected color
        showToast(message = "Selected color: $selectedColor")
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        showToast(message = "Nothing selected")
    }

    private fun showToast(context: Context = applicationContext, message: String, duration: Int = Toast.LENGTH_LONG) {
        Toast.makeText(context, message, duration).show()
    }


    external fun getSuggestion(input: String): String

    companion object {
        init {
            System.loadLibrary("keyboard")
        }
    }
}
