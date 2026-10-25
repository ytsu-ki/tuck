package com.example.starbox.ui.theme

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.starbox.R

/** Light, dark, or whatever the phone is set to. */
enum class ThemeMode(@param:StringRes val label: Int, @param:DrawableRes val icon: Int) {
    System(R.string.theme_system, R.drawable.ic_theme_system),
    Light(R.string.theme_light, R.drawable.ic_theme_light),
    Dark(R.string.theme_dark, R.drawable.ic_theme_dark),
    ;

    fun isDark(systemIsDark: Boolean): Boolean = when (this) {
        System -> systemIsDark
        Light -> false
        Dark -> true
    }

    companion object {
        fun fromName(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: System
    }
}
