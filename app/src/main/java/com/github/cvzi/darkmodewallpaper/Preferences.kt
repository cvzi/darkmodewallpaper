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
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build

class Preferences(mContext: Context, private val prefFile: StringRes) {
    private val context = mContext.applicationContext
    private val pref: SharedPreferences =
        context.getSharedPreferences(context.getString(prefFile), Context.MODE_PRIVATE)
    var colorDay: Int
        get() = pref.getString(
            context.getString(R.string.pref_color_day_key),
            context.getString(R.string.pref_color_day_default)
        )?.toIntOrNull() ?: 0
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_color_day_key),
            value.toString()
        ).apply()

    var colorNight: Int
        get() = pref.getString(
            context.getString(R.string.pref_color_night_key),
            context.getString(R.string.pref_color_night_default)
        )?.toIntOrNull() ?: 0
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_color_night_key),
            value.toString()
        ).apply()

    /** Value ≈-255 to 0 to ≈255 */
    var brightnessDay: Float
        get() = pref.getString(
            context.getString(R.string.pref_brightness_day_key),
            context.getString(R.string.pref_brightness_day_default)
        )?.toFloatOrNull() ?: 0f
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_brightness_day_key),
            value.toString()
        ).apply()

    /** Value ≈-255 to 0 to ≈255 */
    var brightnessNight: Float
        get() = pref.getString(
            context.getString(R.string.pref_brightness_night_key),
            context.getString(R.string.pref_brightness_night_default)
        )?.toFloatOrNull() ?: 0f
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_brightness_night_key),
            value.toString()
        ).apply()

    /** Value ≈0.1 to 1 to ≈1.5 */
    var contrastDay: Float
        get() = pref.getString(
            context.getString(R.string.pref_contrast_day_key),
            context.getString(R.string.pref_contrast_day_default)
        )?.toFloatOrNull() ?: 1f
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_contrast_day_key),
            value.toString()
        ).apply()

    /** Value ≈0.1 to 1 to ≈1.5 */
    var contrastNight: Float
        get() = pref.getString(
            context.getString(R.string.pref_contrast_night_key),
            context.getString(R.string.pref_contrast_night_default)
        )?.toFloatOrNull() ?: 1f
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_contrast_night_key),
            value.toString()
        ).apply()

    /** Value 0.0 to 100.0 */
    var blurDay: Float
        get() = (pref.getString(
            context.getString(R.string.pref_blur_day_key),
            context.getString(R.string.pref_blur_day_default)
        )?.toFloatOrNull() ?: 0f).coerceIn(0f, 100f)
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_blur_day_key),
            value.toString()
        ).apply()

    /** Value 0.0 to 100.0 */
    var blurNight: Float
        get() = (pref.getString(
            context.getString(R.string.pref_blur_night_key),
            context.getString(R.string.pref_blur_night_default)
        )?.toFloatOrNull() ?: 0f).coerceIn(0f, 100f)
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_blur_night_key),
            value.toString()
        ).apply()
    var scrollingModeDay: ScrollingMode
        get() = try {
            ScrollingMode.valueOf(
                pref.getString(
                    context.getString(R.string.pref_scrolling_mode_day_key),
                    ScrollingMode.entries[0].name
                ) ?: ScrollingMode.entries[0].name
            )
        } catch (e: IllegalArgumentException) {
            ScrollingMode.entries[0]
        }
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_scrolling_mode_day_key),
            value.name
        ).apply()
    var scrollingModeNight: ScrollingMode
        get() = try {
            ScrollingMode.valueOf(
                pref.getString(
                    context.getString(R.string.pref_scrolling_mode_night_key),
                    ScrollingMode.entries[0].name
                ) ?: ScrollingMode.entries[0].name
            )
        } catch (e: IllegalArgumentException) {
            ScrollingMode.entries[0]
        }
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_scrolling_mode_night_key),
            value.name
        ).apply()
    var useNightWallpaper: Boolean
        get() = pref.getBoolean(
            context.getString(R.string.pref_use_night_wallpaper_key),
            context.resources.getBoolean(R.bool.pref_use_night_wallpaper_default)
        )
        set(value) = pref.edit().putBoolean(
            context.getString(R.string.pref_use_night_wallpaper_key),
            value
        ).apply()
    var useDayColor: Boolean
        get() = pref.getBoolean(
            context.getString(R.string.pref_use_day_color_key),
            context.resources.getBoolean(R.bool.pref_use_day_color_default)
        )
        set(value) = pref.edit().putBoolean(
            context.getString(R.string.pref_use_day_color_key),
            value
        ).apply()
    var useNightColor: Boolean
        get() = pref.getBoolean(
            context.getString(R.string.pref_use_night_color_key),
            context.resources.getBoolean(R.bool.pref_use_night_color_default)
        )
        set(value) = pref.edit().putBoolean(
            context.getString(R.string.pref_use_night_color_key),
            value
        ).apply()

    var useDayColorOnly: Boolean
        get() = pref.getBoolean(
            context.getString(R.string.pref_use_day_color_only_key),
            context.resources.getBoolean(R.bool.pref_use_day_color_only_default)
        )
        set(value) = pref.edit().putBoolean(
            context.getString(R.string.pref_use_day_color_only_key),
            value
        ).apply()

    var useNightColorOnly: Boolean
        get() = pref.getBoolean(
            context.getString(R.string.pref_use_night_color_only_key),
            context.resources.getBoolean(R.bool.pref_use_night_color_only_default)
        )
        set(value) = pref.edit().putBoolean(
            context.getString(R.string.pref_use_night_color_only_key),
            value
        ).apply()

    var separateLockScreen: Boolean
        get() = pref.getBoolean(
            context.getString(R.string.pref_separate_lock_screen_key),
            context.resources.getBoolean(R.bool.pref_separate_lock_screen_default)
        )
        set(value) = pref.edit().putBoolean(
            context.getString(R.string.pref_separate_lock_screen_key),
            value
        ).apply()

    var previewMode: Int
        get() = pref.getString(
            context.getString(R.string.pref_preview_mode_key),
            context.getString(R.string.pref_preview_mode_default)
        )?.toIntOrNull() ?: 0
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_preview_mode_key),
            value.toString()
        ).apply()

    var animateFromLockScreen: Boolean
        get() = pref.getBoolean(
            context.getString(R.string.pref_animate_from_lock_screen_key),
            context.resources.getBoolean(R.bool.pref_animate_from_lock_screen_default)
        )
        set(value) = pref.edit().putBoolean(
            context.getString(R.string.pref_animate_from_lock_screen_key),
            value
        ).apply()

    var nightModeTrigger: NightModeTrigger
        get() = NightModeTrigger.valueOfOrFirst(
            pref.getString(
                context.getString(R.string.pref_night_mode_trigger_key),
                context.getString(R.string.pref_night_mode_trigger_default)
            )
        )
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_night_mode_trigger_key),
            value.toString()
        ).apply()

    var nightModeTimeRange: String
        get() = pref.getString(
            context.getString(R.string.pref_night_mode_time_range_key),
            context.getString(R.string.pref_night_mode_time_range_default)
        ) ?: context.getString(R.string.pref_night_mode_time_range_default)
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_night_mode_time_range_key),
            value
        ).apply()

    var zoomEnabled: Boolean
        get() = pref.getBoolean(
            context.getString(R.string.pref_zoom_enabled_key),
            context.resources.getBoolean(R.bool.pref_zoom_enabled_default)
        )
        set(value) = pref.edit().putBoolean(
            context.getString(R.string.pref_zoom_enabled_key),
            value
        ).apply()

    var notifyColorsImmediatelyAfterUnlock: Boolean
        get() = pref.getBoolean(
            context.getString(R.string.pref_notify_colors_immediately_after_unlock_key),
            context.resources.getBoolean(R.bool.pref_notify_colors_immediately_after_unlock_default)
        )
        set(value) = pref.edit().putBoolean(
            context.getString(R.string.pref_notify_colors_immediately_after_unlock_key),
            value
        ).apply()

    var animatedFileDay: Boolean
        get() = pref.getBoolean(
            context.getString(R.string.pref_animated_file_day_key),
            context.resources.getBoolean(R.bool.pref_animated_file_day_default)
        )
        set(value) = pref.edit().putBoolean(
            context.getString(R.string.pref_animated_file_day_key),
            value
        ).apply()

    var animatedFileNight: Boolean
        get() = pref.getBoolean(
            context.getString(R.string.pref_animated_file_night_key),
            context.resources.getBoolean(R.bool.pref_animated_file_night_default)
        )
        set(value) = pref.edit().putBoolean(
            context.getString(R.string.pref_animated_file_night_key),
            value
        ).apply()

    fun getWallpaperColorsDay(): WallpaperColors? {
        val primary =
            pref.getString(context.getString(R.string.pref_wallpaper_colors_primary_day_key), null)
                ?.toIntOrNull()
                ?.takeUnless { it == Color.TRANSPARENT }
                ?: return null
        val secondary = pref.getString(
            context.getString(R.string.pref_wallpaper_colors_secondary_day_key),
            null
        )?.toIntOrNull()
        val tertiary =
            pref.getString(context.getString(R.string.pref_wallpaper_colors_tertiary_day_key), null)
                ?.toIntOrNull()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val supportsDarkText: Boolean = pref.getString(
                context.getString(R.string.pref_wallpaper_colors_hint_dark_text_day_key),
                null
            ).toBoolean()
            val supportsDarkTheme: Boolean = pref.getString(
                context.getString(R.string.pref_wallpaper_colors_hint_dark_theme_day_key),
                null
            ).toBoolean()
            var colorHints = 0
            if (supportsDarkText) {
                colorHints = colorHints or WallpaperColors.HINT_SUPPORTS_DARK_TEXT
            }
            if (supportsDarkTheme) {
                colorHints = colorHints or WallpaperColors.HINT_SUPPORTS_DARK_THEME
            }
            WallpaperColors(Color.valueOf(primary),
                secondary?.takeUnless { it == Color.TRANSPARENT }?.let { Color.valueOf(it) },
                tertiary?.takeUnless { it == Color.TRANSPARENT }?.let { Color.valueOf(it) },
                colorHints
            )
        } else {
            WallpaperColors(Color.valueOf(primary),
                secondary?.takeUnless { it == Color.TRANSPARENT }?.let { Color.valueOf(it) },
                tertiary?.takeUnless { it == Color.TRANSPARENT }?.let { Color.valueOf(it) })
        }
    }

    fun setWallpaperColorsDay(wColors: WallpaperColors?) {
        pref.edit()
            .putString(
                context.getString(R.string.pref_wallpaper_colors_primary_day_key),
                wColors?.primaryColor?.toArgb().toString()
            )
            .putString(
                context.getString(R.string.pref_wallpaper_colors_secondary_day_key),
                wColors?.secondaryColor?.toArgb().toString()
            )
            .putString(
                context.getString(R.string.pref_wallpaper_colors_tertiary_day_key),
                wColors?.tertiaryColor?.toArgb().toString()
            )
            .putString(
                context.getString(R.string.pref_wallpaper_colors_hint_dark_text_day_key),
                wColors?.supportsDarkText.toString()
            )
            .putString(
                context.getString(R.string.pref_wallpaper_colors_hint_dark_theme_day_key),
                wColors?.supportsDarkTheme.toString()
            )
            .apply()
    }

    fun getWallpaperColorsNight(): WallpaperColors? {
        val primary =
            pref.getString(
                context.getString(R.string.pref_wallpaper_colors_primary_night_key),
                null
            )
                ?.toIntOrNull()
                ?: return null
        val secondary = pref.getString(
            context.getString(R.string.pref_wallpaper_colors_secondary_night_key),
            null
        )?.toIntOrNull()
        val tertiary =
            pref.getString(
                context.getString(R.string.pref_wallpaper_colors_tertiary_night_key),
                null
            )
                ?.toIntOrNull()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val supportsDarkText: Boolean = pref.getString(
                context.getString(R.string.pref_wallpaper_colors_hint_dark_text_night_key),
                null
            ).toBoolean()
            val supportsDarkTheme: Boolean = pref.getString(
                context.getString(R.string.pref_wallpaper_colors_hint_dark_theme_night_key),
                null
            ).toBoolean()
            var colorHints = 0
            if (supportsDarkText) {
                colorHints = colorHints or WallpaperColors.HINT_SUPPORTS_DARK_TEXT
            }
            if (supportsDarkTheme) {
                colorHints = colorHints or WallpaperColors.HINT_SUPPORTS_DARK_THEME
            }
            WallpaperColors(
                Color.valueOf(primary),
                secondary?.let { Color.valueOf(it) },
                tertiary?.let { Color.valueOf(it) },
                colorHints
            )
        } else {
            WallpaperColors(Color.valueOf(primary),
                secondary?.let { Color.valueOf(it) }, tertiary?.let { Color.valueOf(it) })
        }
    }

    fun setWallpaperColorsNight(wColors: WallpaperColors?) {
        pref.edit()
            .putString(
                context.getString(R.string.pref_wallpaper_colors_primary_night_key),
                wColors?.primaryColor?.toArgb().toString()
            )
            .putString(
                context.getString(R.string.pref_wallpaper_colors_secondary_night_key),
                wColors?.secondaryColor?.toArgb().toString()
            )
            .putString(
                context.getString(R.string.pref_wallpaper_colors_tertiary_night_key),
                wColors?.tertiaryColor?.toArgb().toString()
            )
            .putString(
                context.getString(R.string.pref_wallpaper_colors_hint_dark_text_night_key),
                wColors?.supportsDarkText.toString()
            )
            .putString(
                context.getString(R.string.pref_wallpaper_colors_hint_dark_theme_night_key),
                wColors?.supportsDarkTheme.toString()
            )
            .apply()
    }

    var customWallpaperColorsDay: Boolean
        get() = pref.getBoolean(
            context.getString(R.string.pref_wallpaper_colors_custom_day_key),
            false
        )
        set(value) = pref.edit().putBoolean(
            context.getString(R.string.pref_wallpaper_colors_custom_day_key),
            value
        ).apply()

    var customWallpaperColorsNight: Boolean
        get() = pref.getBoolean(
            context.getString(R.string.pref_wallpaper_colors_custom_night_key),
            false
        )
        set(value) = pref.edit().putBoolean(
            context.getString(R.string.pref_wallpaper_colors_custom_night_key),
            value
        ).apply()

    override fun toString(): String {
        return super.toString() + "[${context.getString(prefFile)}]"
    }
}