package com.fqhll.keyboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.edit
import com.fqhll.keyboard.databinding.ActivityMainBinding
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Paths

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private lateinit var binding: ActivityMainBinding

    private var themes = arrayOf("Unselected", "Shun", "ShunV2", "ShunV3", "ShunV4", "ShunV5", "ShunV6", "Ducky", "DuckyV2", "DuckyV3", "DuckyV4", "DuckyV5", "Cabbage", "Sage", "Jellyfish", "ThisIsFine", "ThisIsFinePremium", "ThisIsFinePremium2", "AntiThisIsFine", "AntiThisIsFinePremium", "AntiThisIsFinePremium2", "Black", "Stargaze", "StargazePremium", "Hammerhead", "CottonCandy", "DarkBlue", "Yellow", "Teal", "Purple", "Green", "Cyan", "ColorBlocks", "Nerdmortie")
    private var keyboardHeights = arrayOf("Unselected", "Short", "Medium", "Tall", "Custom")
    private var engKeyboardLayouts = arrayOf("Unselected", "QWERTY", "QWERTZ", "AZERTY", "Dvorak", "Colemak")
    private var chiKeyboardLayouts = arrayOf("Unselected", "Zhuyin", "ZhuyinEten", "Pinyin")
    private var emojiVariations = arrayOf("Unselected", "Masculine", "Feminine", "Neutral")
    private var keySound = arrayOf("Unselected", "click", "meow", "quack", "oiiai")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Ensure dictionary assets exist in internal storage early to avoid NoSuchFileException
        ensureDictionaryFilesExist()

        val repoLink: TextView = findViewById(R.id.repoLink)
        repoLink.movementMethod = LinkMovementMethod.getInstance()

        // Toggles

        val capsToggle: SwitchCompat = findViewById(R.id.capsToggle)
        val autocorToggle: SwitchCompat = findViewById(R.id.autocorToggle)
        val gridToggle: SwitchCompat = findViewById(R.id.gridToggle)
        val chiKeyboardDefaultToggle: SwitchCompat = findViewById(R.id.chiKeyboardDefaultToggle)
//        val etenToggle: SwitchCompat = findViewById(R.id.etenToggle)
        val keySoundToggle: SwitchCompat = findViewById(R.id.keySoundToggle)
        val altSymbolToggle: SwitchCompat = findViewById(R.id.altSymbolToggle)
        val fullStopCommentToggle: SwitchCompat = findViewById(R.id.fullStopCommentToggle)

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
        if (!prefs.contains("chiKeyboardDefaultToggle")) {
            prefs.edit().putBoolean("chiKeyboardDefaultToggle", false).commit()
        }
        if (!prefs.contains("keySoundToggle")) {
            prefs.edit().putBoolean("keySoundToggle", true).commit()
        }
        if (!prefs.contains("altSymbolToggle")) {
            prefs.edit().putBoolean("altSymbolToggle", false).commit()
        }
        if (!prefs.contains("fullStopCommentToggle")) {
            prefs.edit().putBoolean("fullStopCommentToggle", false).commit()
        }

        // Load saved toggle state
        capsToggle.isChecked = prefs.getBoolean("capsToggle", true)
        autocorToggle.isChecked = prefs.getBoolean("autocorToggle", true)
        gridToggle.isChecked = prefs.getBoolean("gridToggle", false)
        chiKeyboardDefaultToggle.isChecked = prefs.getBoolean("chiKeyboardDefaultToggle", false)
//        etenToggle.isChecked = prefs.getBoolean("etenToggle", false)
        keySoundToggle.isChecked = prefs.getBoolean("keySoundToggle", true)
        altSymbolToggle.isChecked = prefs.getBoolean("altSymbolToggle", false)
        fullStopCommentToggle.isChecked = prefs.getBoolean("fullStopCommentToggle", false)

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
        chiKeyboardDefaultToggle.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit(commit = true) { putBoolean("chiKeyboardDefaultToggle", isChecked) }
        }
//        etenToggle.setOnCheckedChangeListener { _, isChecked ->
//            prefs.edit(commit = true) { putBoolean("etenToggle", isChecked) }
//        }
        keySoundToggle.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit(commit = true) { putBoolean("keySoundToggle", isChecked) }
        }
        altSymbolToggle.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit(commit = true) { putBoolean("altSymbolToggle", isChecked) }
        }
        fullStopCommentToggle.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit(commit = true) { putBoolean("fullStopCommentToggle", isChecked) }
        }

        // Dropdowns

        val keyBackgroundColor: Spinner = findViewById(R.id.color_options)
        val savedColor = prefs.getString("key_color", "Shun")

        val keyboardHeight: Spinner = findViewById(R.id.height_options)
        val savedHeight = prefs.getString("keyboard_height", "Short")

        val engKeyboardLayout: Spinner = findViewById(R.id.eng_layout_options)
        val engSavedLayout = prefs.getString("eng_keyboard_layout", "qwerty")?.lowercase()

        val chiKeyboardLayout: Spinner = findViewById(R.id.chi_layout_options)
        val chiSavedLayout = prefs.getString("chi_keyboard_layout", "zhuyin")?.lowercase()

        val emojiVariation: Spinner = findViewById(R.id.emoji_options)
        val savedEmoji = prefs.getString("emoji_variation", "neutral")?.lowercase()

        val keySoundEffect: Spinner = findViewById(R.id.key_sound_options)
        val savedSoundEffect = prefs.getString("key_sound_effect", "click")


        prefs.edit { putString("key_color", savedColor) }
        prefs.edit { putString("keyboard_height", savedHeight) }
        prefs.edit { putString("eng_keyboard_layout", engSavedLayout) }
        prefs.edit { putString("chi_keyboard_layout", chiSavedLayout) }
        prefs.edit { putString("emoji_variation", savedEmoji) }
        prefs.edit { putString("key_sound_effect", savedSoundEffect) }


        val aa_color = ArrayAdapter(this, R.layout.spinner, themes)
        aa_color.setDropDownViewResource(R.layout.spinner)

        val aa_height = ArrayAdapter(this, R.layout.spinner, keyboardHeights)
        aa_height.setDropDownViewResource(R.layout.spinner)

        val aa_engLayout = ArrayAdapter(this, R.layout.spinner, engKeyboardLayouts)
        aa_engLayout.setDropDownViewResource(R.layout.spinner)

        val aa_chiLayout = ArrayAdapter(this, R.layout.spinner, chiKeyboardLayouts)
        aa_chiLayout.setDropDownViewResource(R.layout.spinner)

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

        // the eng layout dropdown
        with(engKeyboardLayout) {
            adapter = aa_engLayout
            setSelection(0, false)
            onItemSelectedListener = this@MainActivity
            setPopupBackgroundResource(R.color.shun_blue)
        }

        // the chi layout dropdown
        with(chiKeyboardLayout) {
            adapter = aa_chiLayout
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
        val getDictButton: Button = findViewById(R.id.get_dict_btn)
        val importDictButton: Button = findViewById(R.id.import_dict_btn)

        addDictButton.setOnClickListener {
            val inputWord = editDictField.text.toString()

            smartAddToDictionary(inputWord)
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

        getDictButton.setOnClickListener {
            saveFile()
        }

        importDictButton.setOnClickListener {
            openLocalFilePicker()
        }

        // Chinese keyboard
        val removeBtn = findViewById<Button>(R.id.btn_remove_chinese_dict)

        removeBtn.setOnClickListener {
            val file = File(filesDir.absolutePath + "/tsi_custom.json")

            if (file.exists()) {
                val deleted = file.delete()

                if (deleted) {
                    Toast.makeText(this, "Success! Reinstall the app to type Chinese.", Toast.LENGTH_LONG).show()

                    // IMPORTANT: prevent it from being copied back
                    getSharedPreferences("keyboard_settings", MODE_PRIVATE)
                        .edit()
                        .putBoolean("chinese_dict_removed", true)
                        .apply()

                } else {
                    Toast.makeText(this, "Failed to remove Chinese dictionary.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Chinese dictionary already removed.", Toast.LENGTH_SHORT).show()
            }
        }


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

    private fun ensureDictionaryFilesExist() {
        val baseDir = File(filesDir.absolutePath, "test_files")
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }

        val dictFile = File(baseDir, "20k_texting.txt")
        if (!dictFile.exists()) {
            try {
                assets.open("test_files/20k_texting.txt").use { input ->
                    FileOutputStream(dictFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: IOException) {
                try { dictFile.createNewFile() } catch (_: Exception) {}
            }
        }

        val customFile = File(baseDir, "custom_words.txt")
        if (!customFile.exists()) {
            try {
                customFile.createNewFile()
            } catch (_: Exception) {}
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

                R.id.eng_layout_options -> {
                    val selectedEngLayout = engKeyboardLayouts[position]
                    if (!selectedEngLayout.equals("Unselected")) {
                        prefs.edit { putString("eng_keyboard_layout", selectedEngLayout) }
                        showToast(message = "Selected layout: $selectedEngLayout")
                    }
                }

                R.id.chi_layout_options -> {
                    val selectedChiLayout = chiKeyboardLayouts[position]
                    if (!selectedChiLayout.equals("Unselected")) {
                        prefs.edit { putString("chi_keyboard_layout", selectedChiLayout) }
                        showToast(message = "Selected layout: $selectedChiLayout")
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
        ensureDictionaryFilesExist()

        val dictPath = filesDir.absolutePath + "/test_files/20k_texting.txt"
        val customWordsPath = filesDir.absolutePath + "/test_files/custom_words.txt"
        val contractionPath = filesDir.absolutePath + "/test_files/user_contractions.txt"
        CustomKeyboardApp.nativeAddWord(word, dictPath, contractionPath)
        CustomKeyboardApp.nativeAddWord(word, customWordsPath, contractionPath)
    }

    private fun removeFromDictionary(word: String) {
        ensureDictionaryFilesExist()

        val dictPath = filesDir.absolutePath + "/test_files/20k_texting.txt"
        val customWordsPath = filesDir.absolutePath + "/test_files/custom_words.txt"
        val contractionPath = filesDir.absolutePath + "/test_files/user_contractions.txt"
        CustomKeyboardApp.nativeRemoveWord(word, dictPath, contractionPath)
        CustomKeyboardApp.nativeRemoveWord(word, customWordsPath, contractionPath)
    }

    private fun inDictionary(word: String): Boolean {
        val path = Paths.get(filesDir.absolutePath + "/test_files/20k_texting.txt")

        val lines = Files.readAllLines(path)
        val wordSet: Set<String> = HashSet(lines)

        return wordSet.contains(word)
    }

    private fun smartAddToDictionary(word: String) {
        if (!inDictionary(word) && word != "") {
            showToast(message = "adding $word to dictionary...")
            addToDictionary(word)
        }

        else if (word == "") {
            showToast(message = "word cannot be empty!")
        }

        else {
            showToast(message = "$word is already in your dictionary!")
        }
    }

    private fun getCustomWords(): Set<String> {
        val path = Paths.get(filesDir.absolutePath + "/test_files/custom_words.txt")

        val lines = Files.readAllLines(path)
        val wordSet: Set<String> = HashSet(lines)
        return wordSet
    }

    private fun getCustomWordsString(): String {
        val customWordSet = getCustomWords()
        val outputString = customWordSet.joinToString(separator = "\n")
        return outputString
    }
    
    private val CREATE_FILE = 1

    private val PICK_FILE_REQUEST_CODE = 2

    private fun saveFile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "FQ-HLL_Keyboard_Custom_Dictionary_Export.txt")
        }

        // Launch the system file picker and wait for onActivityResult to get the URI.
        startActivityForResult(intent, CREATE_FILE)
    }

    private fun openLocalFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setType("text/plain")

        startActivityForResult(intent, PICK_FILE_REQUEST_CODE)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREATE_FILE && resultCode == Activity.RESULT_OK) {
            val uri: Uri? = data?.data
            if (uri != null) {
                writeToUri(uri)
            } else {
                showToast(message = "No file selected")
            }
        }
        else if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null && data.data != null) {
                val fileUri: Uri = data.data!!
                showToast(message = "Importing from file, please wait")
                readFileContent(fileUri)
            }
        }
    }

    private fun writeToUri(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri)?.use { out ->
//                out.write("This is my custom file content.".toByteArray())
                out.write(getCustomWordsString().toByteArray())
                out.flush()
            }
            showToast(message = "File saved")
        } catch (e: Exception) {
            showToast(message = "Save failed: ${e.message}")
        }
    }

    private fun readFileContent(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val stringBuilder = StringBuilder()
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
//                        stringBuilder.append(line).append("\n")
                        if (line != null) {
                            if (!inDictionary(line) && line != "") {
                                addToDictionary(line)
                            }
                        }
                    }

//                    val fileContent = stringBuilder.toString()
//                    showToast(message = "Save failed: $fileContent")
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

//    private fun importDictFromTxt(fileContent: String) {
//
//    }

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
