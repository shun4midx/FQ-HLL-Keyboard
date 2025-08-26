package com.fqhll.keyboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.edit
import com.fqhll.keyboard.databinding.ActivityMainBinding
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import android.view.inputmethod.InputMethodManager;

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private lateinit var binding: ActivityMainBinding

    private var themes = arrayOf("Unselected", "Shun", "ShunV2", "Ducky", "DuckyV2", "Cabbage", "Sage", "ThisIsFine", "ThisIsFinePremium", "ThisIsFinePremium2", "AntiThisIsFine", "Black", "DarkBlue", "Hammerhead", "Stargaze", "CottonCandy", "Yellow", "Teal", "Purple", "Green", "Cyan")
    private var keyboardHeights = arrayOf("Unselected", "Short", "Medium", "Tall", "Custom")
    private var keyboardLayouts = arrayOf("Unselected", "QWERTY", "QWERTZ", "AZERTY", "Dvorak", "Colemak", "Zhuyin")
    private var emojiVariations = arrayOf("Unselected", "Masculine", "Feminine", "Neutral")
    private var keySound = arrayOf("Unselected", "click", "meow", "quack", "oiiai")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val repoLink: TextView = findViewById(R.id.repoLink)
        repoLink.movementMethod = LinkMovementMethod.getInstance()

        // Toggles

        val capsToggle: SwitchCompat = findViewById(R.id.capsToggle)
        val autocorToggle: SwitchCompat = findViewById(R.id.autocorToggle)
        val gridToggle: SwitchCompat = findViewById(R.id.gridToggle)
        val etenToggle: SwitchCompat = findViewById(R.id.etenToggle)
        val keySoundToggle: SwitchCompat = findViewById(R.id.keySoundToggle)
        val altSymbolToggle: SwitchCompat = findViewById(R.id.altSymbolToggle)

        val prefs = getSharedPreferences("keyboard_settings", Context.MODE_PRIVATE)

        if (!prefs.contains("capsToggle")) {
            prefs.edit().putBoolean("capsToggle", true).commit()
        }
        if (!prefs.contains("autocorToggle")) {
            prefs.edit().putBoolean("autocorToggle", true).commit()
        }
        if (!prefs.contains("gridToggle")) {
            prefs.edit().putBoolean("gridToggle", false).commit()
        }
        if (!prefs.contains("etenToggle")) {
            prefs.edit().putBoolean("etenToggle", false).commit()
        }
        if (!prefs.contains("keySoundToggle")) {
            prefs.edit().putBoolean("keySoundToggle", true).commit()
        }
        if (!prefs.contains("altSymbolToggle")) {
            prefs.edit().putBoolean("altSymbolToggle", false).commit()
        }

        // Load saved toggle state
        capsToggle.isChecked = prefs.getBoolean("capsToggle", true)
        autocorToggle.isChecked = prefs.getBoolean("autocorToggle", true)
        gridToggle.isChecked = prefs.getBoolean("gridToggle", false)
        etenToggle.isChecked = prefs.getBoolean("etenToggle", false)
        keySoundToggle.isChecked = prefs.getBoolean("keySoundToggle", true)
        altSymbolToggle.isChecked = prefs.getBoolean("altSymbolToggle", false)

        // Save toggle changes
        capsToggle.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit(commit = true) { putBoolean("capsToggle", isChecked) }
        }
        autocorToggle.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit(commit = true) { putBoolean("autocorToggle", isChecked) }
        }
        gridToggle.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit(commit = true) { putBoolean("gridToggle", isChecked) }
        }
        etenToggle.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit(commit = true) { putBoolean("etenToggle", isChecked) }
        }
        keySoundToggle.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit(commit = true) { putBoolean("keySoundToggle", isChecked) }
        }
        altSymbolToggle.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit(commit = true) { putBoolean("altSymbolToggle", isChecked) }
        }

        // Dropdowns

        val keyBackgroundColor: Spinner = findViewById(R.id.color_options)
        val savedColor = prefs.getString("key_color", "Shun")

        val keyboardHeight: Spinner = findViewById(R.id.height_options)
        val savedHeight = prefs.getString("keyboard_height", "Short")

        val keyboardLayout: Spinner = findViewById(R.id.layout_options)
        val savedLayout = prefs.getString("keyboard_layout", "qwerty")?.lowercase()

        val emojiVariation: Spinner = findViewById(R.id.emoji_options)
        val savedEmoji = prefs.getString("emoji_variation", "neutral")?.lowercase()

        val keySoundEffect: Spinner = findViewById(R.id.key_sound_options)
        val savedSoundEffect = prefs.getString("key_sound_effect", "click")


        prefs.edit { putString("key_color", savedColor) }
        prefs.edit { putString("keyboard_height", savedHeight) }
        prefs.edit { putString("keyboard_layout", savedLayout) }
        prefs.edit { putString("emoji_variation", savedEmoji) }
        prefs.edit { putString("key_sound_effect", savedSoundEffect) }


        val aa_color = ArrayAdapter(this, R.layout.spinner, themes)
        aa_color.setDropDownViewResource(R.layout.spinner)

        val aa_height = ArrayAdapter(this, R.layout.spinner, keyboardHeights)
        aa_height.setDropDownViewResource(R.layout.spinner)

        val aa_layout = ArrayAdapter(this, R.layout.spinner, keyboardLayouts)
        aa_layout.setDropDownViewResource(R.layout.spinner)

        val aa_emoji = ArrayAdapter(this, R.layout.spinner, emojiVariations)
        aa_emoji.setDropDownViewResource(R.layout.spinner)

        val aa_keySound = ArrayAdapter(this, R.layout.spinner, keySound)
        aa_keySound.setDropDownViewResource(R.layout.spinner)

        // the color dropdown
        with(keyBackgroundColor) {
            adapter = aa_color
            setSelection(0, false)
            onItemSelectedListener = this@MainActivity
            setPopupBackgroundResource(R.color.custom_fqhll_banner_blue)
        }

        // the height dropdown
        with(keyboardHeight) {
            adapter = aa_height
            setSelection(0, false)
            onItemSelectedListener = this@MainActivity
            setPopupBackgroundResource(R.color.shun_blue)
        }

        // the layout dropdown
        with(keyboardLayout) {
            adapter = aa_layout
            setSelection(0, false)
            onItemSelectedListener = this@MainActivity
            setPopupBackgroundResource(R.color.shun_blue)
        }

        // the emoji dropdown
        with(emojiVariation) {
            adapter = aa_emoji
            setSelection(0, false)
            onItemSelectedListener = this@MainActivity
            setPopupBackgroundResource(R.color.shun_blue)
        }

        // the key sound dropdown
        with(keySoundEffect) {
            adapter = aa_keySound
            setSelection(0, false)
            onItemSelectedListener = this@MainActivity
            setPopupBackgroundResource(R.color.shun_blue)
        }


        // edit dictionary stuff

        val editDictField: EditText = findViewById(R.id.edit_dict_input)
        val addDictButton: Button = findViewById(R.id.add_dict_btn)
        val removeDictButton: Button = findViewById(R.id.remove_dict_btn)
//        val getDictButton: Button = findViewById(R.id.get_dict_btn)

        addDictButton.setOnClickListener {
            val inputWord = editDictField.text.toString()

            if (!inDictionary(inputWord) && inputWord != "") {
                showToast(message = "adding $inputWord to dictionary...")
                addToDictionary(inputWord)
            }

            else if (inputWord == "") {
                showToast(message = "word cannot be empty!")
            }

            else {
                showToast(message = "$inputWord is already in your dictionary!")
            }
        }

        removeDictButton.setOnClickListener {
            val inputWord = editDictField.text.toString()

            if (inDictionary(inputWord)) {
                showToast(message = "removing $inputWord from dictionary...")
                removeFromDictionary(inputWord)
            }

            else if (inputWord == "") {
                showToast(message = "word cannot be empty!")
            }

            else {
                showToast(message = "$inputWord is not in your dictionary!")
            }
        }

//        getDictButton.setOnClickListener {
//            saveFile()
//        }


        // launch keyboard stuff

        val openSettingsButton: Button = findViewById(R.id.open_settings_btn)
        val setKeyboardButton: Button = findViewById(R.id.set_keyboard_btn)

        openSettingsButton.setOnClickListener {
            openKeyboardSettings()
        }

        setKeyboardButton.setOnClickListener {
            switchKeyboard()
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val prefs = getSharedPreferences("keyboard_settings", Context.MODE_PRIVATE)
        if (parent != null) {
            when (parent.id) {

                R.id.color_options -> {
                    val selectedColor = themes[position] // Get the selected color
                    if (!selectedColor.equals("Unselected")) {
                        prefs.edit { putString("key_color", selectedColor) } // Save the selected color
                        showToast(message = "Selected theme: $selectedColor")
                    }
                }

                R.id.height_options -> {
                    val selectedHeight = keyboardHeights[position]
                    if (!selectedHeight.equals("Unselected")) {
                        prefs.edit { putString("keyboard_height", selectedHeight) }
                        showToast(message = "Selected height: $selectedHeight")
                    }
                }

                R.id.layout_options -> {
                    val selectedLayout = keyboardLayouts[position]
                    if (!selectedLayout.equals("Unselected")) {
                        prefs.edit { putString("keyboard_layout", selectedLayout) }
                        showToast(message = "Selected layout: $selectedLayout")
                    }
                }

                R.id.emoji_options -> {
                    val selectedEmoji = emojiVariations[position]
                    if (!selectedEmoji.equals("Unselected")) {
                        prefs.edit { putString("emoji_variation", selectedEmoji) }
                        showToast(message = "Selected: $selectedEmoji")
                    }
                }

                R.id.key_sound_options -> {
                    val selectedSound = keySound[position]
                    if (!selectedSound.equals("Unselected")) {
                        prefs.edit { putString("key_sound_effect", selectedSound) }
                        showToast(message = "Selected key sound: $selectedSound")
                    }
                }
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        showToast(message = "Nothing selected")
    }

    private fun showToast(context: Context = applicationContext, message: String, duration: Int = Toast.LENGTH_LONG) {
        Toast.makeText(context, message, duration).show()
    }


    external fun getSuggestion(input: String): String

    private fun addToDictionary(word: String) {
        val dictPath = filesDir.absolutePath + "/test_files/20k_texting.txt"
        val customWordsPath = filesDir.absolutePath + "/test_files/custom_words.txt"
        CustomKeyboardApp.nativeAddWord(word, dictPath)
        CustomKeyboardApp.nativeAddWord(word, customWordsPath)
    }

    private fun removeFromDictionary(word: String) {
        val dictPath = filesDir.absolutePath + "/test_files/20k_texting.txt"
        val customWordsPath = filesDir.absolutePath + "/test_files/custom_words.txt"
        CustomKeyboardApp.nativeRemoveWord(word, dictPath)
        CustomKeyboardApp.nativeRemoveWord(word, customWordsPath)
    }

    private fun inDictionary(word: String): Boolean {
        val path = Paths.get(filesDir.absolutePath + "/test_files/20k_texting.txt")

        val lines = Files.readAllLines(path)
        val wordSet: Set<String> = HashSet(lines)

        return wordSet.contains(word)
    }

    private fun getCustomWords(): Set<String> {
        val path = Paths.get(filesDir.absolutePath + "/test_files/custom_words.txt")

        val lines = Files.readAllLines(path)
        val wordSet: Set<String> = HashSet(lines)

        return wordSet
    }
    
    private val CREATE_FILE = 1

    private fun saveFile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "FQ-HLL_Keyboard_Custom_Dictionary_Export.txt")
        }

        val fileName = intent.getStringExtra(Intent.EXTRA_TITLE)
        val file = fileName?.let { File(this.filesDir, it) }

        showToast(message = fileName.toString())

        startActivityForResult(intent, CREATE_FILE)
        if (file != null) {
            alterDocument(file)
        }
    }

    private fun alterDocument(file: File) {
        FileOutputStream(file).use { fos -> fos.write("This is my custom file content.".toByteArray()) }
    }

    private fun openKeyboardSettings() {
        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
        startActivity(intent)
    }

    private fun switchKeyboard() {
        val inputMethodManager =
            applicationContext.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showInputMethodPicker()
    }

    companion object {
        init {
            System.loadLibrary("keyboard")
        }
    }
}
