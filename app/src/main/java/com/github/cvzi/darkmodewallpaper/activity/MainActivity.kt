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

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.WallpaperManager
import android.content.ClipData
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.view.DragEvent.ACTION_DROP
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.children
import androidx.core.view.isVisible
import com.github.cvzi.darkmodewallpaper.*
import com.github.cvzi.darkmodewallpaper.view.PreviewView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.rarepebble.colorpicker.ColorPickerView
import java.io.File
import kotlin.math.exp
import kotlin.math.log
import kotlin.math.max

/**
 * Wallpaper settings
 */
open class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        var originalDesiredWidth = -1
        var originalDesiredHeight = -1
    }

    protected lateinit var preferences: Preferences
    protected var isLockScreenActivity = false
    protected var dayImageFile: File? = null
    protected var nightImageFile: File? = null

    private lateinit var previewViewDay: PreviewView
    private lateinit var previewViewNight: PreviewView
    private lateinit var imageButtonColorDay: ImageButton
    private lateinit var imageButtonColorNight: ImageButton
    private lateinit var switchColorDay: SwitchMaterial
    private lateinit var switchColorNight: SwitchMaterial
    private lateinit var buttonImportWallpaper: Button
    private lateinit var buttonApplyWallpaper: Button
    private lateinit var textStatusDayOrNight: TextView
    private var previewViewLayoutIndex = -1

    private lateinit var startForPickDayHomeScreenFile: ActivityResultLauncher<Intent>
    private lateinit var startForPickNightHomeScreenFile: ActivityResultLauncher<Intent>
    private lateinit var startForPickDayLockScreenFile: ActivityResultLauncher<Intent>
    private lateinit var startForPickNightLockScreenFile: ActivityResultLauncher<Intent>
    private lateinit var startForStoragePermission: ActivityResultLauncher<String>

    private var importFileThread: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startForPickDayHomeScreenFile = registerForActivityResult(dayFileLocation(false))
        startForPickNightHomeScreenFile = registerForActivityResult(nightFileLocation(false))
        startForPickDayLockScreenFile = registerForActivityResult(dayFileLocation(true))
        startForPickNightLockScreenFile = registerForActivityResult(nightFileLocation(true))
        startForStoragePermission = registerForActivityResult(
            RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                handleImportWallpaper()
            } else {
                Toast.makeText(
                    this,
                    R.string.wallpaper_import_permission_missing,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        preferences = Preferences(
            this,
            if (isLockScreenActivity) R.string.pref_file_lock_screen else R.string.pref_file
        )
        dayImageFile = dayFileLocation(isLockScreenActivity)
        nightImageFile = nightFileLocation(isLockScreenActivity)

        val wallpaperManager = WallpaperManager.getInstance(this)
        originalDesiredWidth = wallpaperManager.desiredMinimumWidth
        originalDesiredHeight = wallpaperManager.desiredMinimumHeight

        textStatusDayOrNight = findViewById(R.id.textStatusDayOrNight)
        previewViewDay = findViewById(R.id.viewColorDay)
        previewViewNight = findViewById(R.id.viewColorNight)
        buttonImportWallpaper = findViewById(R.id.buttonImportWallpaper)
        buttonApplyWallpaper = findViewById(R.id.buttonApplyWallpaper)
        val buttonSelectFileDay = findViewById<Button>(R.id.buttonSelectFileDay)
        val buttonSelectFileNight = findViewById<Button>(R.id.buttonSelectFileNight)
        val switchWallpaperReuseDay = findViewById<SwitchMaterial>(R.id.switchWallpaperReuseDay)
        switchColorDay = findViewById(R.id.switchColorDay)
        switchColorNight = findViewById(R.id.switchColorNight)
        val switchColorOnlyDay = findViewById<SwitchMaterial>(R.id.switchColorOnlyDay)
        val switchColorOnlyNight = findViewById<SwitchMaterial>(R.id.switchColorOnlyNight)
        imageButtonColorDay = findViewById(R.id.imageButtonColorDay)
        imageButtonColorNight = findViewById(R.id.imageButtonColorNight)

        makeCardViewReceiveDragAndDrop(
            findViewById(R.id.cardViewDay),
            dayFileLocation(isLockScreenActivity)
        )
        makeCardViewReceiveDragAndDrop(
            findViewById(R.id.cardViewNight),
            nightFileLocation(isLockScreenActivity)
        )

        textStatusDayOrNight.text =
            getString(
                if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                    R.string.status_darkmode_night
                } else {
                    R.string.status_darkmode_day
                }
            )

        findViewById<TextView>(R.id.textStatusDesiredDimensions).text = getString(
            R.string.resolution_width_x_height,
            wallpaperManager.desiredMinimumWidth,
            wallpaperManager.desiredMinimumHeight
        )


        val screenSize = getScreenSize()
        findViewById<TextView>(R.id.textStatusScreenDimensions).text = getString(
            R.string.resolution_width_x_height,
            screenSize.x,
            screenSize.y
        )

        previewViewDay.layoutParams = LinearLayout.LayoutParams(previewViewDay.layoutParams).apply {
            if (isLockScreenActivity) {
                width = screenSize.x / 5
                height = screenSize.y / 5
            } else {
                width = wallpaperManager.desiredMinimumWidth / 5
                height = wallpaperManager.desiredMinimumWidth / 5
            }
        }
        previewViewDay.scaledScreenWidth = screenSize.x / 5
        previewViewDay.scaledScreenHeight = screenSize.y / 5

        previewViewNight.layoutParams =
            LinearLayout.LayoutParams(previewViewNight.layoutParams).apply {
                if (isLockScreenActivity) {
                    width = screenSize.x / 5
                    height = screenSize.y / 5
                } else {
                    width = wallpaperManager.desiredMinimumWidth / 5
                    height = wallpaperManager.desiredMinimumWidth / 5
                }
            }
        previewViewNight.scaledScreenWidth = screenSize.x / 5
        previewViewNight.scaledScreenHeight = screenSize.y / 5

        findViewById<Button>(R.id.buttonLockScreenSettings).setOnClickListener {
            Handler(Looper.getMainLooper()).postDelayed({
                findViewById<View>(R.id.layoutRoot).visibility = View.INVISIBLE
            }, 200)

            startActivity(Intent(this, LockScreenActivity::class.java))
        }

        buttonImportWallpaper.setOnClickListener {
            askToImport()
        }

        buttonApplyWallpaper.setOnClickListener {
            var c = 1
            // Always preview Home Screen
            if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                c += 10
            }
            Preferences(this, R.string.pref_file).previewMode = c
            startActivity(Intent(
                WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER
            ).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@MainActivity, DarkWallpaperService::class.java)
                )
            })
        }

        findViewById<View>(R.id.imageButtonAbout).setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        switchColorDay.isChecked = preferences.useDayColor
        switchColorDay.setOnCheckedChangeListener { _, isChecked ->
            preferences.useDayColor = isChecked
            previewViewDay.color = if (preferences.useDayColor) preferences.colorDay else 0
            if (!isChecked) {
                preferences.useDayColorOnly = false
                switchColorOnlyDay.isChecked = false
            }
            DarkWallpaperService.invalidate()
        }

        switchColorNight.isChecked = preferences.useNightColor
        switchColorNight.setOnCheckedChangeListener { _, isChecked ->
            preferences.useNightColor = isChecked
            previewViewNight.color = if (preferences.useNightColor) preferences.colorNight else 0
            if (!isChecked) {
                preferences.useNightColorOnly = false
                switchColorOnlyNight.isChecked = false
            }
            DarkWallpaperService.invalidate()
        }

        switchColorOnlyDay.isChecked = preferences.useDayColorOnly
        switchColorOnlyDay.setOnCheckedChangeListener { _, isChecked ->
            preferences.useDayColorOnly = isChecked
            if (isChecked) {
                preferences.useDayColor = true
                switchColorDay.isChecked = true
            }
            previewViewDay.file = currentDayFile()
            previewViewDay.invalidate()
            DarkWallpaperService.invalidate()
        }

        switchColorOnlyNight.isChecked = preferences.useNightColorOnly
        switchColorOnlyNight.setOnCheckedChangeListener { _, isChecked ->
            preferences.useNightColorOnly = isChecked
            if (isChecked) {
                preferences.useNightColor = true
                switchColorNight.isChecked = true
            }
            previewViewNight.file = currentNightFile()
            previewViewNight.invalidate()
            DarkWallpaperService.invalidate()
        }

        buttonSelectFileDay.setOnClickListener {
            if (isLockScreenActivity) {
                startForPickDayLockScreenFile.launch(
                    imageChooserIntent(
                        getString(
                            R.string.wallpaper_file_chooser_title,
                            getString(R.string.wallpaper_file_chooser_day_time),
                            getString(R.string.wallpaper_file_chooser_lock_screen)
                        )
                    )
                )
            } else {
                startForPickDayHomeScreenFile.launch(
                    imageChooserIntent(
                        getString(
                            R.string.wallpaper_file_chooser_title,
                            getString(R.string.wallpaper_file_chooser_day_time),
                            getString(R.string.wallpaper_file_chooser_home_screen)
                        )
                    )
                )
            }
        }

        buttonSelectFileNight.isEnabled = preferences.useNightWallpaper
        buttonSelectFileNight.setOnClickListener {
            if (isLockScreenActivity) {
                startForPickNightLockScreenFile.launch(
                    imageChooserIntent(
                        getString(
                            R.string.wallpaper_file_chooser_title,
                            getString(R.string.wallpaper_file_chooser_night_time),
                            getString(R.string.wallpaper_file_chooser_lock_screen)
                        )
                    )
                )
            } else {
                startForPickNightHomeScreenFile.launch(
                    imageChooserIntent(
                        getString(
                            R.string.wallpaper_file_chooser_title,
                            getString(R.string.wallpaper_file_chooser_night_time),
                            getString(R.string.wallpaper_file_chooser_home_screen)
                        )
                    )
                )
            }
        }

        switchWallpaperReuseDay.isChecked = !preferences.useNightWallpaper
        switchWallpaperReuseDay.setOnCheckedChangeListener { _, isChecked ->
            preferences.useNightWallpaper = !isChecked
            previewViewNight.file = currentNightFile()
            buttonSelectFileNight.isEnabled = !isChecked
            DarkWallpaperService.invalidate()
        }


        imageButtonColorDay.setColorFilter(preferences.colorDay)
        imageButtonColorDay.setOnClickListener {
            colorChooserDialog(
                R.string.color_chooser_day, {
                    preferences.colorDay
                }, { color ->
                    preferences.colorDay = color
                    preferences.useDayColor = true
                    switchColorDay.isChecked = true
                    imageButtonColorDay.setColorFilter(color)
                    previewViewDay.color = color
                    imageButtonColorDay.setColorFilter(color)
                    DarkWallpaperService.invalidate()
                })
        }

        imageButtonColorNight.setColorFilter(preferences.colorNight)
        imageButtonColorNight.setOnClickListener {
            colorChooserDialog(
                R.string.color_chooser_night, {
                    preferences.colorNight
                }, { color ->
                    preferences.colorNight = color
                    preferences.useNightColor = true
                    switchColorNight.isChecked = true
                    imageButtonColorNight.setColorFilter(color)
                    previewViewNight.color = color
                    imageButtonColorNight.setColorFilter(color)
                    DarkWallpaperService.invalidate()
                })
        }


        previewViewDay.color = if (preferences.useDayColor) preferences.colorDay else 0
        previewViewDay.brightness = preferences.brightnessDay
        previewViewDay.contrast = preferences.contrastDay
        previewViewDay.file = currentDayFile()
        previewViewDay.setOnClickListener {
            openAdvancedDialog(DAY)
        }

        previewViewNight.color = if (preferences.useNightColor) preferences.colorNight else 0
        previewViewNight.brightness = preferences.brightnessNight
        previewViewNight.contrast = preferences.contrastNight
        previewViewNight.file = currentNightFile()
        previewViewNight.setOnClickListener {
            openAdvancedDialog(NIGHT)
        }

        if (intent != null
            && (intent.action == Intent.ACTION_SEND || intent.action == Intent.ACTION_ATTACH_DATA)
            && intent.type?.startsWith("image/") == true
        ) {
            // "Send to" / "Use as" from another app
            handleSendToAction(intent)
        } else if (dayImageFile?.exists() != true && !DarkWallpaperService.isRunning() && !isLockScreenActivity) {
            //If there is no file and the services are not running (i.e. usually a new install)
            askToImport()
        }

    }

    private fun makeCardViewReceiveDragAndDrop(cardViewDay: CardView, file: File) {
        val cardBackgroundColorOriginal = cardViewDay.cardBackgroundColor
        cardViewDay.setOnDragListener { v, event ->
            return@setOnDragListener when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    if (event.clipDescription.mimeTypeCount > 0 && event.clipDescription.getMimeType(
                            0
                        ).contains("image")
                    ) {
                        // Blue color
                        (v as? CardView)?.setCardBackgroundColor(
                            resources.getColor(
                                R.color.day_background,
                                null
                            )
                        )
                        true
                    } else {
                        false
                    }
                }
                DragEvent.ACTION_DRAG_ENTERED -> {
                    // Over the view -> green color
                    (v as? CardView)?.setCardBackgroundColor(
                        resources.getColor(
                            R.color.switch_green,
                            null
                        )
                    )
                    v.invalidate()
                    true
                }

                DragEvent.ACTION_DRAG_LOCATION -> {
                    // Ignore
                    true
                }
                DragEvent.ACTION_DRAG_EXITED -> {
                    // Blue color
                    (v as? CardView)?.setCardBackgroundColor(
                        resources.getColor(
                            R.color.day_background,
                            null
                        )
                    )
                    v.invalidate()
                    true
                }

                DragEvent.ACTION_DRAG_ENDED -> {
                    // Reset color
                    (v as? CardView)?.setCardBackgroundColor(cardBackgroundColorOriginal)
                    v.invalidate()
                    true
                }

                ACTION_DROP -> {
                    val imageItem: ClipData.Item = event.clipData.getItemAt(0)
                    val dropPermissions = requestDragAndDropPermissions(event)
                    saveFileFromUri(imageItem.uri, file) {
                        dropPermissions.release()
                    }
                    return@setOnDragListener true
                }
                else -> {
                    false
                }
            }
        }
    }

    private fun registerForActivityResult(file: File): ActivityResultLauncher<Intent> {
        return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                saveFileFromActivityResult(result, file)
            }
        }
    }

    private fun saveFileFromActivityResult(result: ActivityResult, file: File) {
        return saveFileFromUri(result.data?.data, file)
    }

    private fun saveFileFromUri(uri: Uri?, file: File, callback: ((success: Boolean) -> Unit)? = null) {
        val wallpaperManager = WallpaperManager.getInstance(this)
        val desiredMax =
            max(wallpaperManager.desiredMinimumWidth, wallpaperManager.desiredMinimumHeight)
        var alert: AlertDialog? = null
        var progressBar: ProgressBar? = null
        val appContext = applicationContext
        importFileThread = object : Thread("saveFileFromUri") {
            override fun run() {

                var success = false
                if (uri != null) {
                    contentResolver.openInputStream(uri)?.let { ifs ->
                        storeFile(file, ifs, desiredMax)
                        success = true
                        Log.v(
                            TAG,
                            "Stored ${file.nameWithoutExtension} wallpaper in $file"
                        )
                    }
                }
                runOnUiThread {
                    if (callback != null) {
                        callback(success)
                    }
                    if (success) {
                        alert?.dismiss()
                        Toast.makeText(
                            appContext,
                            getString(
                                R.string.wallpaper_file_chooser_success,
                                file.absolutePath
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                        previewViewDay.file = currentDayFile()
                        previewViewNight.file = currentNightFile()

                        DarkWallpaperService.invalidate(forceReload = true)
                    } else {
                        progressBar?.isVisible = false
                        alert?.getButton(AlertDialog.BUTTON_POSITIVE)?.isVisible = true
                        alert?.setMessage(getString(R.string.wallpaper_file_chooser_error))
                        Toast.makeText(
                            appContext,
                            R.string.wallpaper_file_chooser_error,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
        importFileThread?.start()
        progressBar = ProgressBar(this)
        alert = AlertDialog.Builder(this)
            .setTitle(getString(R.string.image_file_import_loading_title))
            .setMessage(getString(R.string.image_file_import_loading_message, uri?.toString(), file.absolutePath))
            .setView(progressBar)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                importFileThread?.join()
                dialog.dismiss()
                file.delete()
                File(file.parent, "${file.name}.tmp").delete()
                previewViewDay.file = currentDayFile()
                previewViewNight.file = currentNightFile()
                DarkWallpaperService.invalidate(forceReload = true)
            }
            .show()
        alert?.getButton(AlertDialog.BUTTON_POSITIVE)?.isVisible = false
    }

    private fun openAdvancedLayoutDay() {
        openAdvancedLayout(previewViewDay,
            preferences.colorDay,
            preferences.contrastDay,
            preferences.brightnessDay,
            { color ->
                preferences.colorDay = color
                preferences.useDayColor = true
                switchColorDay.isChecked = true
                previewViewDay.color = color
                DarkWallpaperService.invalidate()
            }, { contrast ->
                previewViewDay.contrast = contrast
                preferences.contrastDay = contrast
            }, { brightness ->
                previewViewDay.brightness = brightness
                preferences.brightnessDay = brightness
            })
    }

    private fun openAdvancedLayoutNight() {
        openAdvancedLayout(previewViewNight,
            preferences.colorNight,
            preferences.contrastNight,
            preferences.brightnessNight,
            { color ->
                preferences.colorNight = color
                preferences.useNightColor = true
                switchColorNight.isChecked = true
                previewViewNight.color = color
                DarkWallpaperService.invalidate()
            }, { contrast ->
                previewViewNight.contrast = contrast
                preferences.contrastNight = contrast
            }, { brightness ->
                previewViewNight.brightness = brightness
                preferences.brightnessNight = brightness
            })
    }

    override fun onBackPressed() {
        val layoutAdvanced = findViewById<ViewGroup?>(R.id.layoutAdvanced)
        if (previewViewLayoutIndex >= 0 && layoutAdvanced != null) {
            revertAdvancedLayout(layoutAdvanced)
            // Apply changes from advanced layout
            imageButtonColorDay.setColorFilter(preferences.colorDay)
            imageButtonColorNight.setColorFilter(preferences.colorNight)
            DarkWallpaperService.invalidate()
        } else {
            super.onBackPressed()
        }
    }

    private fun openAdvancedDialog(dayOrNight: DayOrNight) {
        val layoutAdvanced = findViewById<ViewGroup?>(R.id.layoutAdvanced)
        if (previewViewLayoutIndex >= 0 && layoutAdvanced != null) {
            // Do not show the dialog if the advanced view is already open
            return
        }

        val alert = AlertDialog.Builder(this).setTitle(R.string.dialog_advanced_title).create()
        val linearLayout =
            LayoutInflater.from(this).inflate(R.layout.dialog_advanced, alert.listView)
        alert.setView(linearLayout)
        linearLayout.findViewById<Button>(R.id.buttonPreview).setOnClickListener {
            alert.dismiss()

            var c = 1
            if (dayOrNight == NIGHT) {
                c += 10
            }
            if (isLockScreenActivity) {
                c += 1
            }
            Preferences(this, R.string.pref_file).previewMode = c

            startActivity(Intent(
                WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER
            ).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@MainActivity, DarkWallpaperService::class.java)
                )
            })

        }
        linearLayout.findViewById<Button>(R.id.buttonAdvanced).setOnClickListener {
            alert.dismiss()
            if (dayOrNight == NIGHT) {
                openAdvancedLayoutNight()
            } else {
                openAdvancedLayoutDay()
            }
        }
        linearLayout.findViewById<Button>(R.id.buttonNewImage).setOnClickListener {
            alert.dismiss()
            if (isLockScreenActivity && dayOrNight == DAY) {
                startForPickDayLockScreenFile.launch(imagePickIntent())
            } else if (isLockScreenActivity && dayOrNight == NIGHT) {
                startForPickNightLockScreenFile.launch(imagePickIntent())
            } else if (dayOrNight == DAY) {
                startForPickDayHomeScreenFile.launch(imagePickIntent())
            } else {
                startForPickNightHomeScreenFile.launch(imagePickIntent())
            }
        }
        alert.show()
    }

    private fun openAdvancedLayout(
        previewView: PreviewView,
        initColor: Int,
        initContrast: Float,
        initBrightness: Float,
        onColorPick: (color: Int) -> Unit,
        onContrastChanged: (value: Float) -> Unit,
        onBrightnessChanged: (value: Float) -> Unit
    ) {
        val layoutAdvanced = findViewById<ViewGroup?>(R.id.layoutAdvanced)
        if (previewViewLayoutIndex >= 0 && layoutAdvanced != null) {
            revertAdvancedLayout(layoutAdvanced)
        }
        enableFullScreen()

        val linearLayout = previewView.parent as LinearLayout
        linearLayout.orientation = 1 - linearLayout.orientation
        linearLayout.children.forEach { child ->
            if (child != previewView) {
                child.visibility = View.GONE
            }
        }
        if (isLockScreenActivity) {
            findViewById<View>(R.id.cardViewLockScreenSwitch).visibility = View.GONE
        }

        LayoutInflater.from(this).inflate(R.layout.layout_advanced, linearLayout)

        val tmp = linearLayout.findViewById<View>(R.id.placeHolderForPreviewView)
        val parent = tmp.parent as LinearLayout
        val index = parent.indexOfChild(tmp)
        parent.removeView(tmp)
        previewViewLayoutIndex = linearLayout.indexOfChild(previewView)
        linearLayout.removeView(previewView)
        parent.addView(previewView, index)
        previewView.tag = "previewView"


        val colorPickerView =
            linearLayout.findViewById<ColorPickerView>(R.id.colorPickerAdvanced)
        colorPickerView.color = initColor
        colorPickerView.showAlpha(true)
        colorPickerView.showHex(false)
        colorPickerView.addColorObserver {
            onColorPick(it.color)
        }

        val contrastSeekBar = findViewById<SeekBar>(R.id.seekBarContrast)
        contrastSeekBar.rotation = 0.5f
        contrastSeekBar.max = 1000
        contrastSeekBar.progress = (342.0 * (exp(initContrast - 0.1) - 1.0)).toInt()
        contrastSeekBar.setOnSeekBarChangeListener(OnSeekBarProgress { progress ->
            val v = 0.1f + log(1.0 + progress / 342.0, Math.E).toFloat()
            onContrastChanged(v)
        })

        val brightnessSeekBar = findViewById<SeekBar>(R.id.seekBarBrightness)
        brightnessSeekBar.rotation = 0.5f
        brightnessSeekBar.max = 1000
        brightnessSeekBar.progress = (1.97 * initBrightness + 500).toInt()
        brightnessSeekBar.setOnSeekBarChangeListener(OnSeekBarProgress { progress ->
            val v = (progress - 500f) / 1.97f
            onBrightnessChanged(v)
        })

        val buttonResetAdvanced = findViewById<Button>(R.id.buttonResetAdvanced)
        buttonResetAdvanced.setOnClickListener {
            brightnessSeekBar.progress = 500
            contrastSeekBar.progress = 500
        }
        val screenSize = getScreenSize()

        previewView.scaledScreenWidth = screenSize.x / 3
        previewView.scaledScreenHeight = screenSize.y / 3
        previewView.layoutParams = LinearLayout.LayoutParams(previewView.layoutParams).apply {
            if (isLockScreenActivity) {
                width = screenSize.x / 3
                height = screenSize.y / 3
            } else {
                val wallpaperManager = WallpaperManager.getInstance(this@MainActivity)
                width = wallpaperManager.desiredMinimumWidth / 3
                height = wallpaperManager.desiredMinimumWidth / 3
            }
        }

    }


    private fun revertAdvancedLayout(layoutAdvanced: ViewGroup) {
        // Remove advanced view
        val linearLayout = layoutAdvanced.parent as LinearLayout
        val previewView = linearLayout.findViewWithTag<PreviewView>("previewView")
        (previewView.parent as ViewGroup).removeView(previewView)
        linearLayout.removeView(layoutAdvanced)
        linearLayout.orientation = 1 - linearLayout.orientation
        linearLayout.addView(previewView, previewViewLayoutIndex)
        linearLayout.children.forEach { child ->
            child.visibility = View.VISIBLE
        }

        // Restore lock screen switch
        if (isLockScreenActivity) {
            findViewById<View>(R.id.cardViewLockScreenSwitch).visibility = View.VISIBLE
        }

        // Resize color view
        val wallpaperManager = WallpaperManager.getInstance(this)
        val screenSize = getScreenSize()
        previewView.scaledScreenWidth = screenSize.x / 5
        previewView.scaledScreenHeight = screenSize.y / 5
        previewView.layoutParams = LinearLayout.LayoutParams(previewView.layoutParams).apply {
            if (isLockScreenActivity) {
                width = screenSize.x / 5
                height = screenSize.y / 5
            } else {
                width = wallpaperManager.desiredMinimumWidth / 5
                height = wallpaperManager.desiredMinimumWidth / 5
            }
        }

        disableFullScreen()

        previewViewLayoutIndex = -1
    }

    override fun onResume() {
        super.onResume()

        findViewById<View>(R.id.layoutRoot).visibility = View.VISIBLE

        buttonImportWallpaper.isVisible = !DarkWallpaperService.isRunning()

        DarkWallpaperService.invalidate()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        when (newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> {
                textStatusDayOrNight.text = getString(R.string.status_darkmode_day)
            }
            Configuration.UI_MODE_NIGHT_YES -> {
                textStatusDayOrNight.text = getString(R.string.status_darkmode_night)
            }
        }
    }


    private fun currentDayFile(): File? {
        return if (preferences.useDayColorOnly) {
            null
        } else {
            dayImageFile
        }
    }

    private fun currentNightFile(): File? {
        return if (preferences.useNightColorOnly) {
            null
        } else if (preferences.useNightWallpaper && nightImageFile?.exists() == true) {
            nightImageFile
        } else {
            dayImageFile
        }

    }

    private fun askToImport() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.wallpaper_import_dialog_title)
        builder.setMessage(R.string.wallpaper_import_dialog_message)
        builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
            dialog.dismiss()
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                startForStoragePermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                handleImportWallpaper()
            }
        }
        builder.setNegativeButton(android.R.string.cancel, null)
        builder.show()
    }

    private fun handleImportWallpaper() {
        if (importWallpaper()) {
            Toast.makeText(
                this,
                getString(R.string.wallpaper_import_success, dayImageFile?.absolutePath),
                Toast.LENGTH_SHORT
            ).show()

            previewViewDay.file = currentDayFile()
            previewViewNight.file = currentNightFile()
            DarkWallpaperService.invalidate(forceReload = true)
        } else {
            Toast.makeText(
                this,
                R.string.wallpaper_import_failed,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun importWallpaper(): Boolean {
        val wallpaperManager = WallpaperManager.getInstance(this)
        wallpaperManager.drawable?.let {
            Log.v(TAG, "Storing in $dayImageFile")
            val r = storeFile(dayFileLocation(isLockScreenActivity), it)
            Log.v(TAG, "Stored $dayImageFile")
            return r
        }
        return false
    }

    private fun handleSendToAction(intent: Intent) {
        val dayOrNight = sendToActionIsNight()
        Log.v(TAG, "isNightSendToAction() = $dayOrNight")
        val file =
            if (dayOrNight == NIGHT) nightFileLocation(isLockScreenActivity) else dayFileLocation(
                isLockScreenActivity
            )
        val data = intent.data ?: intent.clipData?.getItemAt(0)?.uri
        data?.let { uri ->
            saveFileFromUri(uri, file)
        }
    }

    protected open fun sendToActionIsNight(): DayOrNight {
        preferences.useDayColorOnly = false
        return DAY
    }

    private fun colorChooserDialog(
        title: StringRes,
        getColor: (() -> Int),
        storeColor: ((color: Int) -> Unit)
    ) {
        val inflater = layoutInflater
        val dialogLayout: View = inflater.inflate(R.layout.dialog_color, null)
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setView(dialogLayout)
        builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
            val color = dialogLayout.findViewById<ColorPickerView>(R.id.colorPicker).color
            storeColor(color)
            dialog.dismiss()
        }
        builder.setNegativeButton(android.R.string.cancel, null)
        val dialog = builder.show()
        val colorPickerView = dialog.findViewById(R.id.colorPicker) as ColorPickerView
        colorPickerView.color = getColor()
        colorPickerView.showAlpha(true)
        colorPickerView.showHex(false)
    }

}
