package com.sexporsche.music_player

import android.content.Context
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import java.util.Locale

fun setLocale(context: Context, languageCode: String) {
    val locale = Locale(languageCode)
    Locale.setDefault(locale)

    val config =  context.resources.configuration
    config.setLocale(locale)
    context.resources.updateConfiguration(config,  context.resources.displayMetrics)

    // Сохраняем выбранный язык в SharedPreferences
    val editor =  context.getSharedPreferences("Settings", MODE_PRIVATE).edit()
    editor.putString("App_Language", languageCode)
    editor.apply()
}