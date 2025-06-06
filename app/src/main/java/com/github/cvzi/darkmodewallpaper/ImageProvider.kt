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

import android.app.WallpaperColors
import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.IOException

/**
 * All relevant properties of a Wallpaper
 *
 * @property brightness Value from ≈-255 to 0 to ≈255
 * @property contrast Value from ≈0.1 to 1 to ≈1.5
 * @property blur Value from 0.0 to 100.0
 */
data class WallpaperImage(
    val imageFile: File?,
    val color: Int,
    val brightness: Float,
    val contrast: Float,
    val blur: Float,
    val scrollingMode: ScrollingMode,
    val expiration: Int?,
    val animated: Boolean,
    val customWallpaperColors: WallpaperColors?
)

const val IMAGEPROVIDERTAG = "ImageProvider"

abstract class ImageProvider {
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

class StaticDayAndNightProvider(val context: Context) : ImageProvider() {
    private val preferencesLockScreen =
        Preferences(context, R.string.pref_file_lock_screen)
    private val preferencesHomeScreen =
        Preferences(context, R.string.pref_file)

    private fun dayFileName(isLockScreen: Boolean): String {
        val currentPreferences = if (isLockScreen) preferencesLockScreen else preferencesHomeScreen
        return dayFileName(isLockScreen, currentPreferences.animatedFileDay)
    }

    private fun nightFileName(isLockScreen: Boolean): String {
        val currentPreferences = if (isLockScreen) preferencesLockScreen else preferencesHomeScreen
        return nightFileName(isLockScreen, currentPreferences.animatedFileNight)
    }

    private fun dayFileName(isLockScreen: Boolean, isAnimated: Boolean): String {
        return if (isAnimated) {
            context.getString(if (isLockScreen) R.string.file_name_gif_day_lock_wallpaper else R.string.file_name_gif_day_wallpaper)
        } else {
            context.getString(if (isLockScreen) R.string.file_name_day_lock_wallpaper else R.string.file_name_day_wallpaper)
        }
    }

    private fun nightFileName(isLockScreen: Boolean, isAnimated: Boolean): String {
        return if (isAnimated) {
            context.getString(if (isLockScreen) R.string.file_name_gif_night_lock_wallpaper else R.string.file_name_gif_night_wallpaper)
        } else {
            context.getString(if (isLockScreen) R.string.file_name_night_lock_wallpaper else R.string.file_name_night_wallpaper)
        }
    }

    private fun dayFileLocation(isLockScreen: Boolean, isAnimated: Boolean): File {
        val oldFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            dayFileName(isLockScreen, isAnimated)
        )
        val newFile = File(
            context.createDeviceProtectedStorageContext().filesDir,
            dayFileName(isLockScreen, isAnimated)
        )
        if (newFile.exists() || !oldFile.exists()) {
            return newFile
        }
        return oldFile
    }

    private fun nightFileLocation(isLockScreen: Boolean, isAnimated: Boolean): File {
        val oldFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            nightFileName(isLockScreen, isAnimated)
        )
        val newFile = File(
            context.createDeviceProtectedStorageContext().filesDir,
            nightFileName(isLockScreen, isAnimated)
        )
        if (newFile.exists() || !oldFile.exists()) {
            return newFile
        }
        return oldFile
    }

    private fun dayFileLocation(isLockScreen: Boolean): File {
        val oldFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            dayFileName(isLockScreen)
        )
        val newFile = File(
            context.createDeviceProtectedStorageContext().filesDir,
            dayFileName(isLockScreen)
        )
        if (newFile.exists() || !oldFile.exists()) {
            return newFile
        }
        return oldFile
    }

    private fun nightFileLocation(isLockScreen: Boolean): File {
        val oldFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            nightFileName(isLockScreen)
        )
        val newFile = File(
            context.createDeviceProtectedStorageContext().filesDir,
            nightFileName(isLockScreen)
        )
        if (newFile.exists() || !oldFile.exists()) {
            return newFile
        }
        return oldFile
    }

    fun moveFilesToDeviceProtectedStorage() {
        val deviceContext = context.createDeviceProtectedStorageContext()
        Log.d(IMAGEPROVIDERTAG, "Moving files to device protected storage")
        val oldHomeDayFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            dayFileName(isLockScreen = false, preferencesHomeScreen.animatedFileDay)
        )
        val newHomeDayFile = File(
            deviceContext.filesDir,
            dayFileName(isLockScreen = false, preferencesHomeScreen.animatedFileDay)
        )
        if (oldHomeDayFile.exists() && !newHomeDayFile.exists()) {
            oldHomeDayFile.copyTo(newHomeDayFile).let {
                if (it.exists() && it.canRead() && it.length() > 0L && it.length() == oldHomeDayFile.length()) {
                    Log.v(
                        IMAGEPROVIDERTAG,
                        "Successfully copied $oldHomeDayFile to $newHomeDayFile"
                    )
                    oldHomeDayFile.delete()
                } else {
                    Log.e(IMAGEPROVIDERTAG, "Could not copy $oldHomeDayFile to $newHomeDayFile")
                }
            }
        }

        val oldHomeNightFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            nightFileName(isLockScreen = false, preferencesHomeScreen.animatedFileDay)
        )
        val newHomeNightFile = File(
            deviceContext.filesDir,
            nightFileName(isLockScreen = false, preferencesHomeScreen.animatedFileDay)
        )
        if (oldHomeNightFile.exists() && !newHomeNightFile.exists()) {
            oldHomeNightFile.copyTo(newHomeNightFile).let {
                if (it.exists() && it.canRead() && it.length() > 0L && it.length() == oldHomeNightFile.length()) {
                    Log.v(
                        IMAGEPROVIDERTAG,
                        "Successfully copied $oldHomeNightFile to $newHomeNightFile"
                    )
                    oldHomeNightFile.delete()
                } else {
                    Log.e(IMAGEPROVIDERTAG, "Could not copy $oldHomeNightFile to $newHomeNightFile")
                }
            }
        }

        val oldLockDayFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            dayFileName(isLockScreen = true, preferencesLockScreen.animatedFileDay)
        )
        val newLockDayFile = File(
            deviceContext.filesDir,
            dayFileName(isLockScreen = true, preferencesLockScreen.animatedFileDay)
        )
        if (oldLockDayFile.exists() && !newLockDayFile.exists()) {
            oldLockDayFile.copyTo(newLockDayFile).let {
                if (it.exists() && it.canRead() && it.length() > 0L && it.length() == oldLockDayFile.length()) {
                    Log.v(
                        IMAGEPROVIDERTAG,
                        "Successfully copied $oldLockDayFile to $newLockDayFile"
                    )
                    oldLockDayFile.delete()
                } else {
                    Log.e(IMAGEPROVIDERTAG, "Could not copy $oldLockDayFile to $newLockDayFile")
                }
            }
        }

        val oldLockNightFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            nightFileName(isLockScreen = true, preferencesLockScreen.animatedFileDay)
        )
        val newLockNightFile = File(
            deviceContext.filesDir,
            nightFileName(isLockScreen = true, preferencesLockScreen.animatedFileDay)
        )
        if (oldLockNightFile.exists() && !newLockNightFile.exists()) {
            oldLockNightFile.copyTo(newLockNightFile).let {
                if (it.exists() && it.canRead() && it.length() > 0L && it.length() == oldLockNightFile.length()) {
                    Log.v(
                        IMAGEPROVIDERTAG,
                        "Successfully copied $oldLockNightFile to $newLockNightFile"
                    )
                    oldLockNightFile.delete()
                } else {
                    Log.e(IMAGEPROVIDERTAG, "Could not copy $oldLockNightFile to $newLockNightFile")
                }
            }
        }
    }


    fun setNewFile(dayOrNight: DayOrNight, isLockScreen: Boolean, isAnimated: Boolean, file: File) {
        val currentPreferences = if (isLockScreen) preferencesLockScreen else preferencesHomeScreen
        // Delete existing files
        storeFileLocation(dayOrNight, isLockScreen, isAnimated = false).run {
            if (exists()) {
                try {
                    delete()
                } catch (e: IOException) {
                    Log.e(IMAGEPROVIDERTAG, "Could not delete old static file: $this", e)
                }
            }
        }
        storeFileLocation(dayOrNight, isLockScreen, isAnimated = true).run {
            if (exists()) {
                try {
                    delete()
                } catch (e: IOException) {
                    Log.e(IMAGEPROVIDERTAG, "Could not delete animated static file: $this", e)
                }
            }
        }

        // Rename file to name depending on whether is is animated or not
        file.renameTo(storeFileLocation(dayOrNight, isLockScreen, isAnimated = isAnimated))

        if (dayOrNight == NIGHT) {
            currentPreferences.animatedFileNight = isAnimated
        } else {
            currentPreferences.animatedFileDay = isAnimated
        }
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

    fun setScrollingMode(dayOrNight: DayOrNight, scrollingMode: ScrollingMode) {
        if (dayOrNight == NIGHT) {
            preferencesHomeScreen.scrollingModeNight = scrollingMode
        } else {
            preferencesHomeScreen.scrollingModeDay = scrollingMode
        }
    }

    fun getScrollingMode(dayOrNight: DayOrNight): ScrollingMode {
        return if (dayOrNight == NIGHT) {
            preferencesHomeScreen.scrollingModeNight
        } else {
            preferencesHomeScreen.scrollingModeDay
        }
    }

    fun isAnimated(dayOrNight: DayOrNight, isLockScreen: Boolean): Boolean {
        val currentPreferences = if (isLockScreen) preferencesLockScreen else preferencesHomeScreen
        return if (dayOrNight == NIGHT && currentPreferences.useNightWallpaper) {
            currentPreferences.animatedFileNight
        } else {
            currentPreferences.animatedFileDay
        }
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

        val scrollingMode =
            if (dayOrNight == NIGHT) currentPreferences.scrollingModeNight else currentPreferences.scrollingModeDay

        val animated = isAnimated(dayOrNight, isLockScreen)

        val customWallpaperColors =
            if (dayOrNight == NIGHT && currentPreferences.customWallpaperColorsNight) {
                getWallpaperColors(NIGHT, isLockScreen)
            } else if (dayOrNight == DAY && currentPreferences.customWallpaperColorsDay) {
                getWallpaperColors(DAY, isLockScreen)
            } else {
                null
            }

        callback(
            WallpaperImage(
                imageFile,
                overlayColor,
                brightness,
                contrast,
                blur,
                scrollingMode,
                -1,
                animated,
                customWallpaperColors
            )
        )
    }

    fun storeFileLocation(
        dayOrNight: DayOrNight,
        isLockScreen: Boolean,
        isAnimated: Boolean
    ): File {
        return if (dayOrNight == NIGHT) nightFileLocation(
            isLockScreen,
            isAnimated
        ) else dayFileLocation(
            isLockScreen, isAnimated
        )
    }

    override fun storeFileLocation(
        dayOrNight: DayOrNight,
        isLockScreen: Boolean
    ): File {
        return if (dayOrNight == NIGHT) nightFileLocation(isLockScreen) else dayFileLocation(
            isLockScreen
        )
    }

    fun getWallpaperColors(dayOrNight: DayOrNight, isLockScreen: Boolean): WallpaperColors? {
        val currentPreferences = if (isLockScreen) preferencesLockScreen else preferencesHomeScreen
        return if (dayOrNight == NIGHT) {
            currentPreferences.getWallpaperColorsNight()

        } else {
            currentPreferences.getWallpaperColorsDay()
        }
    }

    fun setWallpaperColors(
        dayOrNight: DayOrNight,
        isLockScreen: Boolean,
        wallpaperColors: WallpaperColors?
    ) {
        val currentPreferences = if (isLockScreen) preferencesLockScreen else preferencesHomeScreen
        if (dayOrNight == NIGHT) {
            currentPreferences.setWallpaperColorsNight(wallpaperColors)
        } else {
            currentPreferences.setWallpaperColorsDay(wallpaperColors)
        }
    }


    fun setUseCustomWallpaperColors(
        dayOrNight: DayOrNight,
        isLockScreen: Boolean,
        enabled: Boolean
    ) {
        val currentPreferences = if (isLockScreen) preferencesLockScreen else preferencesHomeScreen
        if (dayOrNight == NIGHT) {
            currentPreferences.customWallpaperColorsNight = enabled
        } else {
            currentPreferences.customWallpaperColorsDay = enabled
        }
    }

    fun useCustomWallpaperColors(dayOrNight: DayOrNight, isLockScreen: Boolean): Boolean {
        val currentPreferences = if (isLockScreen) preferencesLockScreen else preferencesHomeScreen
        return if (dayOrNight == NIGHT) {
            currentPreferences.customWallpaperColorsNight
        } else {
            currentPreferences.customWallpaperColorsDay
        }
    }
}