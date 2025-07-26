// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}

applicationVariants.all {
    val variant = this
    variant.outputs
        .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
        .filter {
            val names = it.name.split("-")
            it.name.lowercase().contains(names[0], true) && it.name.lowercase().contains(names[1], true)
        }
        .forEach { output ->
            val outputFileName = "fqhll_keyboard.apk"
            output.outputFileName = outputFileName
        }
}