/*  DarkModeLiveWallpaper github.com/cvzi/darkmodewallpaper
    Copyright © 2021 cuzi@openmail.cc

    This file is part of DarkModeLiveWallpaper.

    DarkModeLiveWallpaper is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DarkModeLiveWallpaper is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with DarkModeLiveWallpaper.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.github.cvzi.darkmodewallpaper

import android.content.Context
import android.os.Environment
import java.io.File

/**
 * All relevant properties of a Wallpaper
 *
 * @property brightness Value from ≈-255 to 0 to ≈255
 * @property contrast Value from ≈0.1 to 1 to ≈1.5
 * @property blur Value from 0.0 to 25.0
 */
data class WallpaperImage(
    val imageFile: File?,
    val color: Int,
    val brightness: Float,
    val contrast: Float,
    val blur: Float,
    val expiration: Int?
)


abstract class ImageProvider(val context: Context) {
    abstract fun get(
        dayOrNight: DayOrNight,
        isLockScreen: Boolean,
        callback: ((WallpaperImage) -> Unit)
    )

    abstract fun storeFileLocation(
        dayOrNight: DayOrNight,
        isLockScreen: Boolean
    ): File
}

class StaticDayAndNightProvider(context: Context) : ImageProvider(context) {
    private val preferencesLockScreen: Preferences =
        Preferences(context, R.string.pref_file_lock_screen)
    private val preferencesHomeScreen: Preferences = Preferences(context, R.string.pref_file)

    private fun dayFileName(isLockScreen: Boolean): String {
        return context.getString(if (isLockScreen) R.string.file_name_day_lock_wallpaper else R.string.file_name_day_wallpaper)
    }

    private fun nightFileName(isLockScreen: Boolean): String {
        return context.getString(if (isLockScreen) R.string.file_name_night_lock_wallpaper else R.string.file_name_night_wallpaper)
    }

    private fun dayFileLocation(isLockScreen: Boolean): File {
        return File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            dayFileName(isLockScreen)
        )
    }

    private fun nightFileLocation(isLockScreen: Boolean): File {
        return File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            nightFileName(isLockScreen)
        )
    }

    fun setBrightness(dayOrNight: DayOrNight, isLockScreen: Boolean, brightness: Float) {
        val currentPreferences = if (isLockScreen) preferencesLockScreen else preferencesHomeScreen
        if (dayOrNight == NIGHT) {
            currentPreferences.brightnessNight = brightness
        } else {
            currentPreferences.brightnessDay = brightness
        }
    }

    fun getBrightness(dayOrNight: DayOrNight, isLockScreen: Boolean): Float {
        val currentPreferences = if (isLockScreen) preferencesLockScreen else preferencesHomeScreen
        return if (dayOrNight == NIGHT) {
            currentPreferences.brightnessNight
        } else {
            currentPreferences.brightnessDay
        }
    }

    fun setContrast(dayOrNight: DayOrNight, isLockScreen: Boolean, contrast: Float) {
        val currentPreferences = if (isLockScreen) preferencesLockScreen else preferencesHomeScreen
        if (dayOrNight == NIGHT) {
            currentPreferences.contrastNight = contrast
        } else {
            currentPreferences.contrastDay = contrast
        }
    }

    fun getContrast(dayOrNight: DayOrNight, isLockScreen: Boolean): Float {
        val currentPreferences = if (isLockScreen) preferencesLockScreen else preferencesHomeScreen
        return if (dayOrNight == NIGHT) {
            currentPreferences.contrastNight
        } else {
            currentPreferences.contrastDay
        }
    }

    fun setBlur(dayOrNight: DayOrNight, isLockScreen: Boolean, blur: Float) {
        val currentPreferences = if (isLockScreen) preferencesLockScreen else preferencesHomeScreen
        if (dayOrNight == NIGHT) {
            currentPreferences.blurNight = blur
        } else {
            currentPreferences.blurDay = blur
        }
    }

    fun getBlur(dayOrNight: DayOrNight, isLockScreen: Boolean): Float {
        val currentPreferences = if (isLockScreen) preferencesLockScreen else preferencesHomeScreen
        return if (dayOrNight == NIGHT) {
            currentPreferences.blurNight
        } else {
            currentPreferences.blurDay
        }
    }

    fun setColor(dayOrNight: DayOrNight, isLockScreen: Boolean, color: Int) {
        val currentPreferences = if (isLockScreen) preferencesLockScreen else preferencesHomeScreen
        if (dayOrNight == NIGHT) {
            currentPreferences.colorNight = color
        } else {
            currentPreferences.colorDay = color
        }
    }

    fun getColor(dayOrNight: DayOrNight, isLockScreen: Boolean): Int {
        val currentPreferences = if (isLockScreen) preferencesLockScreen else preferencesHomeScreen
        return if (dayOrNight == NIGHT) {
            currentPreferences.colorNight
        } else {
            currentPreferences.colorDay
        }
    }

    fun setUseColor(dayOrNight: DayOrNight, isLockScreen: Boolean, enabled: Boolean) {
        val currentPreferences = if (isLockScreen) preferencesLockScreen else preferencesHomeScreen
        if (dayOrNight == NIGHT) {
            currentPreferences.useNightColor = enabled
        } else {
            currentPreferences.useDayColor = enabled
        }
    }

    fun getUseColor(dayOrNight: DayOrNight, isLockScreen: Boolean): Boolean {
        val currentPreferences = if (isLockScreen) preferencesLockScreen else preferencesHomeScreen
        return if (dayOrNight == NIGHT) {
            currentPreferences.useNightColor
        } else {
            currentPreferences.useDayColor
        }
    }

    fun setUseColorOnly(dayOrNight: DayOrNight, isLockScreen: Boolean, enabled: Boolean) {
        val currentPreferences = if (isLockScreen) preferencesLockScreen else preferencesHomeScreen
        if (dayOrNight == NIGHT) {
            currentPreferences.useNightColorOnly = enabled
        } else {
            currentPreferences.useDayColorOnly = enabled
        }
    }

    fun getUseColorOnly(dayOrNight: DayOrNight, isLockScreen: Boolean): Boolean {
        val currentPreferences = if (isLockScreen) preferencesLockScreen else preferencesHomeScreen
        return if (dayOrNight == NIGHT) {
            currentPreferences.useNightColorOnly
        } else {
            currentPreferences.useDayColorOnly
        }
    }

    fun setUseNightWallpaper(isLockScreen: Boolean, enabled: Boolean) {
        val currentPreferences = if (isLockScreen) preferencesLockScreen else preferencesHomeScreen
        currentPreferences.useNightWallpaper = enabled
    }

    fun getUseNightWallpaper(isLockScreen: Boolean): Boolean {
        val currentPreferences = if (isLockScreen) preferencesLockScreen else preferencesHomeScreen
        return currentPreferences.useNightWallpaper
    }

    override fun get(
        dayOrNight: DayOrNight,
        isLockScreen: Boolean,
        callback: ((WallpaperImage) -> Unit)
    ) {
        val currentPreferences = if (isLockScreen) preferencesLockScreen else preferencesHomeScreen
        val imageFile =
            if ((dayOrNight == NIGHT && currentPreferences.useNightColorOnly) || (dayOrNight == DAY && currentPreferences.useDayColorOnly)) {
                null
            } else if (dayOrNight == NIGHT && currentPreferences.useNightWallpaper && nightFileLocation(
                    isLockScreen
                ).exists()
            ) {
                nightFileLocation(isLockScreen)
            } else {
                dayFileLocation(isLockScreen)
            }

        imageFile?.let {
            if (it.name.startsWith("lock_") && !it.exists()) {
                val old = File(it.parent, "L" + it.name.substring(1))
                if (old.exists()) {
                    old.renameTo(imageFile)
                }
            }
        }

        val overlayColor = if (dayOrNight == NIGHT && currentPreferences.useNightColor) {
            currentPreferences.colorNight
        } else if (dayOrNight == DAY && currentPreferences.useDayColor) {
            currentPreferences.colorDay
        } else {
            0
        }

        val brightness =
            if (dayOrNight == NIGHT) currentPreferences.brightnessNight else currentPreferences.brightnessDay

        val contrast =
            if (dayOrNight == NIGHT) currentPreferences.contrastNight else currentPreferences.contrastDay

        val blur =
            if (dayOrNight == NIGHT) currentPreferences.blurNight else currentPreferences.blurDay

        // TODO expiration via trigger

        callback(WallpaperImage(imageFile, overlayColor, brightness, contrast, blur, -1))
    }

    override fun storeFileLocation(
        dayOrNight: DayOrNight,
        isLockScreen: Boolean
    ): File {
        return if (dayOrNight == NIGHT) nightFileLocation(isLockScreen) else dayFileLocation(
            isLockScreen
        )
    }
}