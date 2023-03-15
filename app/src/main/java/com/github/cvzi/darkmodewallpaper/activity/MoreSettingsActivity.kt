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
package com.github.cvzi.darkmodewallpaper.activity

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.github.cvzi.darkmodewallpaper.*
import com.github.cvzi.darkmodewallpaper.databinding.ActivityMoreSettingsBinding
import com.github.cvzi.darkmodewallpaper.databinding.LayoutWallpaperColorsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Advanced/More settings
 */
class MoreSettingsActivity : AppCompatActivity() {
    private lateinit var preferencesGlobal: Preferences
    private lateinit var imageProvider: StaticDayAndNightProvider
    private lateinit var binding: ActivityMoreSettingsBinding
    private lateinit var checkeredBackground: BitmapDrawable
    private var defaultButtonTextColor: Int = Color.BLACK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferencesGlobal = Preferences(this, R.string.pref_file)
        imageProvider = StaticDayAndNightProvider(this)

        binding = ActivityMoreSettingsBinding.inflate(layoutInflater)

        setContentView(binding.root)

        checkeredBackground =
            BitmapDrawable(resources, checkeredBackground()).apply {
                setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                setTargetDensity(resources.displayMetrics.densityDpi * 2)
            }


        binding.apply {

            defaultButtonTextColor = binding.buttonNotifyColorsChanged.currentTextColor

            setHtmlText(
                binding.textViewLinkAndroidReference,
                "For more information see <a href=\"https://developer.android.com/reference/android/app/WallpaperColors\">https://developer.android.com/reference/android/app/WallpaperColors</a>"
            )

            toggleButtonNotifyColorsImmediately.setOnCheckedChangeListener { _, isChecked ->
                preferencesGlobal.notifyColorsImmediatelyAfterUnlock = isChecked
            }


            buttonInsertCurrent.setOnClickListener {
                CoroutineScope(Dispatchers.Default).launch {
                    val colors = WallpaperManager.getInstance(this@MoreSettingsActivity)
                        .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                    runOnUiThread {
                        if (colors == null) {
                            Toast.makeText(
                                this@MoreSettingsActivity,
                                "Could not retrieve current colors",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            askImportWhichColors(colors)
                        }
                    }
                }
            }

            @SuppressLint("SetTextI18n")
            homeDayColors.switchUseCustomColors.text = "Home/Day"
            @SuppressLint("SetTextI18n")
            homeNightColors.switchUseCustomColors.text = "Home/Night"
            @SuppressLint("SetTextI18n")
            lockDayColors.switchUseCustomColors.text = "Lock/Day"
            @SuppressLint("SetTextI18n")
            lockNightColors.switchUseCustomColors.text = "Lock/Night"

            homeDayColors.viewColorPrimary.setOnClickListener {
                openColorChooser(DAY, isLockScreen = false, "Home/Day/Primary", {
                    it?.primaryColor
                }, { color, editor ->
                    editor.setPrimaryColor(color)
                })
            }
            homeDayColors.viewColorSecondary.setOnClickListener {
                openColorChooser(DAY, isLockScreen = false, "Home/Day/Secondary", {
                    it?.secondaryColor
                }, { color, editor ->
                    editor.setSecondaryColor(color)
                })
            }
            homeDayColors.viewColorTertiary.setOnClickListener {
                openColorChooser(DAY, isLockScreen = false, "Home/Day/Tertiary", {
                    it?.tertiaryColor
                }, { color, editor ->
                    editor.setTertiaryColor(color)
                })
            }
            homeNightColors.viewColorPrimary.setOnClickListener {
                openColorChooser(NIGHT, isLockScreen = false, "Home/Night/Primary", {
                    it?.primaryColor
                }, { color, editor ->
                    editor.setPrimaryColor(color)
                })
            }
            homeNightColors.viewColorSecondary.setOnClickListener {
                openColorChooser(NIGHT, isLockScreen = false, "Home/Night/Secondary", {
                    it?.secondaryColor
                }, { color, editor ->
                    editor.setSecondaryColor(color)
                })
            }
            homeNightColors.viewColorTertiary.setOnClickListener {
                openColorChooser(NIGHT, isLockScreen = false, "Home/Night/Tertiary", {
                    it?.tertiaryColor
                }, { color, editor ->
                    editor.setTertiaryColor(color)
                })
            }

            lockDayColors.viewColorPrimary.setOnClickListener {
                openColorChooser(DAY, isLockScreen = true, "Lock/Day/Primary", {
                    it?.primaryColor
                }, { color, editor ->
                    editor.setPrimaryColor(color)
                })
            }
            lockDayColors.viewColorSecondary.setOnClickListener {
                openColorChooser(DAY, isLockScreen = true, "Lock/Day/Secondary", {
                    it?.secondaryColor
                }, { color, editor ->
                    editor.setSecondaryColor(color)
                })
            }
            lockDayColors.viewColorTertiary.setOnClickListener {
                openColorChooser(DAY, isLockScreen = true, "Lock/Day/Tertiary", {
                    it?.tertiaryColor
                }, { color, editor ->
                    editor.setTertiaryColor(color)
                })
            }

            lockNightColors.viewColorPrimary.setOnClickListener {
                openColorChooser(NIGHT, isLockScreen = true, "Lock/Night/Primary", {
                    it?.primaryColor
                }, { color, editor ->
                    editor.setPrimaryColor(color)
                })
            }
            lockNightColors.viewColorSecondary.setOnClickListener {
                openColorChooser(NIGHT, isLockScreen = true, "Lock/Night/Secondary", {
                    it?.secondaryColor
                }, { color, editor ->
                    editor.setSecondaryColor(color)
                })
            }
            lockNightColors.viewColorTertiary.setOnClickListener {
                openColorChooser(NIGHT, isLockScreen = true, "Lock/Night/Tertiary", {
                    it?.tertiaryColor
                }, { color, editor ->
                    editor.setTertiaryColor(color)
                })
            }

            toggleButtonChangeListener(
                homeDayColors.toggleButtonSupportsDarkTheme,
                DAY, isLockScreen = false
            ) { isChecked, editor ->
                editor.setDarkTheme(isChecked)
            }
            toggleButtonChangeListener(
                homeDayColors.toggleButtonSupportsDarkText,
                DAY, isLockScreen = false
            ) { isChecked, editor ->
                editor.setDarkText(isChecked)
            }
            toggleButtonChangeListener(
                homeNightColors.toggleButtonSupportsDarkTheme,
                NIGHT, isLockScreen = false
            ) { isChecked, editor ->
                editor.setDarkTheme(isChecked)
            }
            toggleButtonChangeListener(
                homeNightColors.toggleButtonSupportsDarkText,
                NIGHT, isLockScreen = false
            ) { isChecked, editor ->
                editor.setDarkText(isChecked)
            }
            toggleButtonChangeListener(
                lockDayColors.toggleButtonSupportsDarkTheme,
                DAY, isLockScreen = true
            ) { isChecked, editor ->
                editor.setDarkTheme(isChecked)
            }
            toggleButtonChangeListener(
                lockDayColors.toggleButtonSupportsDarkText,
                DAY, isLockScreen = true
            ) { isChecked, editor ->
                editor.setDarkText(isChecked)
            }
            toggleButtonChangeListener(
                lockNightColors.toggleButtonSupportsDarkTheme,
                NIGHT, isLockScreen = true
            ) { isChecked, editor ->
                editor.setDarkTheme(isChecked)
            }
            toggleButtonChangeListener(
                lockNightColors.toggleButtonSupportsDarkText,
                NIGHT, isLockScreen = true
            ) { isChecked, editor ->
                editor.setDarkText(isChecked)
            }

            homeDayColors.switchUseCustomColors.setOnCheckedChangeListener { _, isChecked ->
                imageProvider.setUseCustomWallpaperColors(DAY, isLockScreen = false, isChecked)
                buttonNotifyColorsChanged.setTextColor(defaultButtonTextColor)
            }
            homeNightColors.switchUseCustomColors.setOnCheckedChangeListener { _, isChecked ->
                imageProvider.setUseCustomWallpaperColors(NIGHT, isLockScreen = false, isChecked)
                buttonNotifyColorsChanged.setTextColor(defaultButtonTextColor)
            }
            lockDayColors.switchUseCustomColors.setOnCheckedChangeListener { _, isChecked ->
                imageProvider.setUseCustomWallpaperColors(DAY, isLockScreen = true, isChecked)
                buttonNotifyColorsChanged.setTextColor(defaultButtonTextColor)
            }
            lockNightColors.switchUseCustomColors.setOnCheckedChangeListener { _, isChecked ->
                imageProvider.setUseCustomWallpaperColors(NIGHT, isLockScreen = true, isChecked)
                buttonNotifyColorsChanged.setTextColor(defaultButtonTextColor)
            }

            buttonNotifyColorsChanged.setOnClickListener {
                DarkWallpaperService.notifyColorsChanged()
                buttonNotifyColorsChanged.setTextColor(Color.LTGRAY)
            }

        }

    }

    private fun toggleButtonChangeListener(
        button: ToggleButton,
        dayOrNight: DayOrNight, isLockScreen: Boolean,
        editColor: (Boolean, WallpaperColorsEditor) -> WallpaperColorsEditor
    ) {
        button.setOnCheckedChangeListener { _, isChecked ->
            val wallpaperColors = editColor(
                isChecked, imageProvider
                    .getWallpaperColors(dayOrNight, isLockScreen)
                    .edit()
            )
                .build()
            imageProvider.setWallpaperColors(dayOrNight, isLockScreen, wallpaperColors)
            binding.buttonNotifyColorsChanged.setTextColor(defaultButtonTextColor)
        }
    }

    private fun openColorChooser(
        dayOrNight: DayOrNight, isLockScreen: Boolean, title: String,
        extractColor: ((WallpaperColors?) -> Color?),
        editColor: ((Int, WallpaperColorsEditor) -> WallpaperColorsEditor)
    ) {
        colorChooserDialog(
            title, {
                extractColor(
                    imageProvider.getWallpaperColors(
                        dayOrNight,
                        isLockScreen
                    )
                )?.toArgb() ?: Color.WHITE
            }, { color ->
                val wallpaperColors = editColor(
                    color, imageProvider
                        .getWallpaperColors(dayOrNight, isLockScreen)
                        .edit()
                )
                    .build()
                imageProvider.setWallpaperColors(dayOrNight, isLockScreen, wallpaperColors)
                displayWallpaperColors()
                binding.buttonNotifyColorsChanged.setTextColor(defaultButtonTextColor)
            })
    }


    override fun onResume() {
        super.onResume()

        binding.apply {

            toggleButtonNotifyColorsImmediately.isChecked =
                preferencesGlobal.notifyColorsImmediatelyAfterUnlock

            homeDayColors.switchUseCustomColors.isChecked =
                imageProvider.useCustomWallpaperColors(DAY, isLockScreen = false)
            homeNightColors.switchUseCustomColors.isChecked =
                imageProvider.useCustomWallpaperColors(NIGHT, isLockScreen = false)
            lockDayColors.switchUseCustomColors.isChecked =
                imageProvider.useCustomWallpaperColors(DAY, isLockScreen = true)
            lockNightColors.switchUseCustomColors.isChecked =
                imageProvider.useCustomWallpaperColors(NIGHT, isLockScreen = true)

        }
        displayWallpaperColors()

        binding.buttonNotifyColorsChanged.setTextColor(Color.LTGRAY)

        binding.textViewAndroid31.isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    }

    private fun displayWallpaperColors() {
        displayWallpaperColors(
            binding.homeDayColors,
            imageProvider.getWallpaperColors(DAY, isLockScreen = false)
        )
        displayWallpaperColors(
            binding.homeNightColors,
            imageProvider.getWallpaperColors(NIGHT, isLockScreen = false)
        )
        displayWallpaperColors(
            binding.lockDayColors,
            imageProvider.getWallpaperColors(DAY, isLockScreen = true)
        )
        displayWallpaperColors(
            binding.lockNightColors,
            imageProvider.getWallpaperColors(NIGHT, isLockScreen = true)
        )
    }

    private fun displayWallpaperColors(
        layoutWallpaperColors: LayoutWallpaperColorsBinding,
        wColors: WallpaperColors?
    ) {
        layoutWallpaperColors.apply {
            linearLayoutColorViews.background = checkeredBackground

            viewColorPrimary.setBackgroundColor(Color.TRANSPARENT)
            viewColorSecondary.setBackgroundColor(Color.TRANSPARENT)
            viewColorTertiary.setBackgroundColor(Color.TRANSPARENT)

            wColors?.let {
                viewColorPrimary.setBackgroundColor(wColors.primaryColor.toArgb())
                wColors.secondaryColor?.toArgb()?.let {
                    viewColorSecondary.setBackgroundColor(it)
                }
                wColors.tertiaryColor?.toArgb()?.let {
                    viewColorTertiary.setBackgroundColor(it)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                toggleButtonSupportsDarkText.isVisible = true
                toggleButtonSupportsDarkText.isChecked = wColors?.supportsDarkText == true
                toggleButtonSupportsDarkTheme.isVisible = true
                toggleButtonSupportsDarkTheme.isChecked = wColors?.supportsDarkTheme == true
            } else {
                toggleButtonSupportsDarkText.isVisible = false
                toggleButtonSupportsDarkTheme.isVisible = false
            }
        }
    }

    private fun askImportWhichColors(colors: WallpaperColors) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Which colors should be replaced with current colors?")
        val choices = arrayOf(
            "${getString(R.string.wallpaper_file_chooser_day_time)}/${getString(R.string.wallpaper_file_chooser_home_screen)}",
            "${getString(R.string.wallpaper_file_chooser_night_time)}/${getString(R.string.wallpaper_file_chooser_home_screen)}",
            "${getString(R.string.wallpaper_file_chooser_day_time)}/${getString(R.string.wallpaper_file_chooser_lock_screen)}",
            "${getString(R.string.wallpaper_file_chooser_night_time)}/${getString(R.string.wallpaper_file_chooser_lock_screen)}"
        )
        val selection = arrayOf(true, false, false, false)
        builder.setMultiChoiceItems(
            choices,
            selection.toBooleanArray()
        ) { _: DialogInterface, which: Int, checked: Boolean ->
            selection[which] = checked
        }
        builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
            dialog.safeDismiss()
            selection.forEachIndexed { index, checked ->
                if (checked) {
                    when (index) {
                        0 -> imageProvider.setWallpaperColors(
                            dayOrNight = DAY,
                            isLockScreen = false,
                            colors
                        )
                        1 -> imageProvider.setWallpaperColors(
                            dayOrNight = NIGHT,
                            isLockScreen = false,
                            colors
                        )
                        2 -> imageProvider.setWallpaperColors(
                            dayOrNight = DAY,
                            isLockScreen = true,
                            colors
                        )
                        else -> imageProvider.setWallpaperColors(
                            dayOrNight = NIGHT,
                            isLockScreen = true,
                            colors
                        )
                    }
                }
            }
            displayWallpaperColors()
            DarkWallpaperService.notifyColorsChanged()
            binding.buttonNotifyColorsChanged.setTextColor(Color.LTGRAY)
        }
        builder.setNegativeButton(android.R.string.cancel, null)
        builder.show()
    }

}