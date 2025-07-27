package com.fqhll.keyboard

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.fqhll.keyboard.databinding.ActivityMainBinding
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

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
    }

    external fun getSuggestion(input: String): String

    companion object {
        init {
            System.loadLibrary("keyboard")
        }
    }
}
