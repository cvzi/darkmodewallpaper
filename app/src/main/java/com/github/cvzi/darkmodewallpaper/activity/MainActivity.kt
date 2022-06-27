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
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.WallpaperManager
import android.content.ClipData
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.DragEvent
import android.view.DragEvent.ACTION_DROP
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        const val READ_WALLPAPER_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE
    }

    protected lateinit var preferencesGlobal: Preferences
    protected var isLockScreenActivity = false
    protected lateinit var imageProvider: StaticDayAndNightProvider
    protected var dayImageFile: File? = null
    protected var nightImageFile: File? = null

    private lateinit var previewViewDay: PreviewView
    private lateinit var previewViewNight: PreviewView
    private lateinit var imageButtonColorDay: ImageButton
    private lateinit var imageButtonColorNight: ImageButton
    private lateinit var switchColorDay: SwitchMaterial
    private lateinit var switchColorNight: SwitchMaterial
    private lateinit var switchWallpaperReuseDay: SwitchMaterial
    private lateinit var switchColorOnlyNight: SwitchMaterial
    private lateinit var switchColorOnlyDay: SwitchMaterial
    private lateinit var buttonImportWallpaper: Button
    private lateinit var buttonMoreSettings: Button
    private lateinit var buttonApplyWallpaper: Button
    private lateinit var textStatusDayOrNight: TextView
    private lateinit var switchTriggerSystem: SwitchMaterial
    private lateinit var textViewStartTime: TextView
    private lateinit var textViewEndTime: TextView
    private lateinit var tableRowTimeRangeTrigger: TableRow
    private lateinit var switchZoomEnabled: SwitchMaterial
    private lateinit var textZoomEnabled: TextView
    private var previewViewLayoutIndex = -1
    private var previewScale = 1f

    private lateinit var startForPickDayHomeScreenFile: ActivityResultLauncher<Intent>
    private lateinit var startForPickNightHomeScreenFile: ActivityResultLauncher<Intent>
    private lateinit var startForPickDayLockScreenFile: ActivityResultLauncher<Intent>
    private lateinit var startForPickNightLockScreenFile: ActivityResultLauncher<Intent>
    private lateinit var startForStoragePermission: ActivityResultLauncher<String>

    private var importFileThread: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageProvider = StaticDayAndNightProvider(this)

        startForPickDayHomeScreenFile = registerForActivityResult(
            imageProvider.storeFileLocation(
                dayOrNight = DAY,
                isLockScreen = false
            )
        )
        startForPickNightHomeScreenFile = registerForActivityResult(
            imageProvider.storeFileLocation(
                dayOrNight = NIGHT,
                isLockScreen = false
            )
        )
        startForPickDayLockScreenFile = registerForActivityResult(
            imageProvider.storeFileLocation(
                dayOrNight = DAY,
                isLockScreen = true
            )
        )
        startForPickNightLockScreenFile = registerForActivityResult(
            imageProvider.storeFileLocation(
                dayOrNight = NIGHT,
                isLockScreen = true
            )
        )
        startForStoragePermission = registerForActivityResult(
            RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                askImportWhichWallpaper()
            } else {
                Toast.makeText(
                    this,
                    R.string.wallpaper_import_permission_missing,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        /*
        if (isLockScreenActivity) {
            preferences = Preferences(
                this,
                R.string.pref_file_lock_screen
            )
            preferencesGlobal = Preferences(this, R.string.pref_file)
        } else {
            preferences = Preferences(
                this,
                R.string.pref_file
            )
            preferencesGlobal = preferences
        }
        */
        preferencesGlobal = Preferences(this, R.string.pref_file)

        dayImageFile =
            imageProvider.storeFileLocation(dayOrNight = DAY, isLockScreen = isLockScreenActivity)
        nightImageFile =
            imageProvider.storeFileLocation(dayOrNight = NIGHT, isLockScreen = isLockScreenActivity)

        val wallpaperManager = WallpaperManager.getInstance(this)
        originalDesiredWidth = wallpaperManager.desiredMinimumWidth
        originalDesiredHeight = wallpaperManager.desiredMinimumHeight

        textStatusDayOrNight = findViewById(R.id.textStatusDayOrNight)
        previewViewDay = findViewById(R.id.viewColorDay)
        previewViewNight = findViewById(R.id.viewColorNight)
        buttonImportWallpaper = findViewById(R.id.buttonImportWallpaper)
        buttonMoreSettings = findViewById(R.id.buttonMoreSettings)
        buttonApplyWallpaper = findViewById(R.id.buttonApplyWallpaper)
        val buttonSelectFileDay = findViewById<Button>(R.id.buttonSelectFileDay)
        val buttonSelectFileNight = findViewById<Button>(R.id.buttonSelectFileNight)
        switchWallpaperReuseDay = findViewById(R.id.switchWallpaperReuseDay)
        switchColorDay = findViewById(R.id.switchColorDay)
        switchColorNight = findViewById(R.id.switchColorNight)
        switchColorOnlyDay = findViewById(R.id.switchColorOnlyDay)
        switchColorOnlyNight = findViewById(R.id.switchColorOnlyNight)
        imageButtonColorDay = findViewById(R.id.imageButtonColorDay)
        imageButtonColorNight = findViewById(R.id.imageButtonColorNight)
        switchTriggerSystem = findViewById(R.id.switchTriggerSystem)
        textViewStartTime = findViewById(R.id.textViewStartTime)
        textViewEndTime = findViewById(R.id.textViewEndTime)
        tableRowTimeRangeTrigger = findViewById(R.id.tableRowTimeRangeTrigger)
        switchZoomEnabled = findViewById(R.id.switchZoomEnabled)
        textZoomEnabled = findViewById(R.id.textZoomEnabled)

        makeCardViewReceiveDragAndDrop(
            findViewById(R.id.cardViewDay),
            imageProvider.storeFileLocation(dayOrNight = DAY, isLockScreen = isLockScreenActivity)
        )
        makeCardViewReceiveDragAndDrop(
            findViewById(R.id.cardViewNight),
            imageProvider.storeFileLocation(dayOrNight = NIGHT, isLockScreen = isLockScreenActivity)
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

        setPreviewDimension(previewViewDay)
        setPreviewDimension(previewViewNight)

        findViewById<Button>(R.id.buttonLockScreenSettings).setOnClickListener {
            Handler(Looper.getMainLooper()).postDelayed({
                findViewById<View>(R.id.layoutRoot).visibility = View.INVISIBLE
            }, 200)

            startActivity(Intent(this, LockScreenActivity::class.java))
        }

        buttonImportWallpaper.setOnClickListener {
            askToImport()
        }

        buttonMoreSettings.setOnClickListener {
            startActivity(Intent(this, MoreSettingsActivity::class.java))
        }

        buttonApplyWallpaper.setOnClickListener {
            var c = 1
            // Always preview Home Screen
            if (isDayOrNightMode() == NIGHT) {
                c += 10
            }

            applyLiveWallpaper(this@MainActivity, c) {
                Toast.makeText(
                    this@MainActivity,
                    R.string.apply_wallpaper_unavailable,
                    Toast.LENGTH_LONG
                ).show()
                buttonApplyWallpaper.isEnabled = false
            }
        }
        Intent(
            WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER
        ).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this@MainActivity, DarkWallpaperService::class.java)
            )
            if (resolveActivity(packageManager) == null) {
                buttonApplyWallpaper.isEnabled = false
            }
        }

        findViewById<View>(R.id.imageButtonAbout).setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        switchColorDay.isChecked = imageProvider.getUseColor(DAY, isLockScreenActivity)
        switchColorDay.setOnCheckedChangeListener { _, isChecked ->
            imageProvider.setUseColor(DAY, isLockScreenActivity, isChecked)
            previewViewDay.color =
                if (isChecked) imageProvider.getColor(DAY, isLockScreenActivity) else 0
            if (!isChecked) {
                imageProvider.setUseColorOnly(DAY, isLockScreenActivity, false)
                switchColorOnlyDay.isChecked = false
            }
            DarkWallpaperService.invalidate()
        }

        switchColorNight.isChecked = imageProvider.getUseColor(NIGHT, isLockScreenActivity)
        switchColorNight.setOnCheckedChangeListener { _, isChecked ->
            imageProvider.setUseColor(NIGHT, isLockScreenActivity, isChecked)
            previewViewNight.color =
                if (isChecked) imageProvider.getColor(NIGHT, isLockScreenActivity) else 0
            if (!isChecked) {
                imageProvider.setUseColorOnly(NIGHT, isLockScreenActivity, false)
                switchColorOnlyNight.isChecked = false
            }
            DarkWallpaperService.invalidate()
        }

        switchColorOnlyDay.isChecked = imageProvider.getUseColorOnly(DAY, isLockScreenActivity)
        switchColorOnlyDay.setOnCheckedChangeListener { _, isChecked ->
            imageProvider.setUseColorOnly(DAY, isLockScreenActivity, isChecked)
            if (isChecked) {
                imageProvider.setUseColor(DAY, isLockScreenActivity, true)
                switchColorDay.isChecked = true
            }
            previewViewDay.file = currentDayFile()
            previewViewDay.invalidate()
            DarkWallpaperService.invalidate()
        }

        switchColorOnlyNight.isChecked = imageProvider.getUseColorOnly(NIGHT, isLockScreenActivity)
        switchColorOnlyNight.setOnCheckedChangeListener { _, isChecked ->
            imageProvider.setUseColorOnly(NIGHT, isLockScreenActivity, isChecked)
            if (isChecked) {
                imageProvider.setUseColor(NIGHT, isLockScreenActivity, true)
                switchColorNight.isChecked = true
            }
            previewViewNight.file = currentNightFile()
            previewViewNight.invalidate()
            DarkWallpaperService.invalidate()
        }

        buttonSelectFileDay.setOnClickListener {
            if (isLockScreenActivity) {
                startForPickDayLockScreenFile.launch(
                    imagePickIntent()
                )
            } else {
                startForPickDayHomeScreenFile.launch(
                    imagePickIntent()
                )
            }
            switchColorOnlyDay.isChecked = false
        }

        buttonSelectFileNight.isEnabled = imageProvider.getUseNightWallpaper(isLockScreenActivity)
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
            switchColorOnlyNight.isChecked = false
            switchWallpaperReuseDay.isChecked = false
        }

        switchWallpaperReuseDay.isChecked =
            !imageProvider.getUseNightWallpaper(isLockScreenActivity)
        switchWallpaperReuseDay.setOnCheckedChangeListener { _, isChecked ->
            imageProvider.setUseNightWallpaper(isLockScreenActivity, !isChecked)
            previewViewNight.file = currentNightFile()
            buttonSelectFileNight.isEnabled = !isChecked
            DarkWallpaperService.invalidate()
        }


        imageButtonColorDay.setColorFilter(imageProvider.getColor(DAY, isLockScreenActivity))
        imageButtonColorDay.setOnClickListener {
            colorChooserDialog(
                R.string.color_chooser_day, {
                    imageProvider.getColor(DAY, isLockScreenActivity)
                }, { color ->
                    imageProvider.setColor(DAY, isLockScreenActivity, color)
                    imageProvider.setUseColor(DAY, isLockScreenActivity, true)
                    switchColorDay.isChecked = true
                    imageButtonColorDay.setColorFilter(color)
                    previewViewDay.color = color
                    imageButtonColorDay.setColorFilter(color)
                    DarkWallpaperService.invalidate()
                })
        }

        imageButtonColorNight.setColorFilter(imageProvider.getColor(NIGHT, isLockScreenActivity))
        imageButtonColorNight.setOnClickListener {
            colorChooserDialog(
                R.string.color_chooser_night, {
                    imageProvider.getColor(NIGHT, isLockScreenActivity)
                }, { color ->
                    imageProvider.setColor(NIGHT, isLockScreenActivity, color)
                    imageProvider.setUseColor(NIGHT, isLockScreenActivity, true)
                    switchColorNight.isChecked = true
                    imageButtonColorNight.setColorFilter(color)
                    previewViewNight.color = color
                    imageButtonColorNight.setColorFilter(color)
                    DarkWallpaperService.invalidate()
                })
        }


        previewViewDay.color =
            if (imageProvider.getUseColor(DAY, isLockScreenActivity)) imageProvider.getColor(
                DAY,
                isLockScreenActivity
            ) else 0
        previewViewDay.brightness = imageProvider.getBrightness(DAY, isLockScreenActivity)
        previewViewDay.contrast = imageProvider.getContrast(DAY, isLockScreenActivity)
        previewViewDay.blur = imageProvider.getBlur(DAY, isLockScreenActivity) / previewScale
        previewViewDay.file = currentDayFile()
        previewViewDay.setOnClickListener {
            openAdvancedDialog(DAY)
        }

        previewViewNight.color = if (imageProvider.getUseColor(
                NIGHT,
                isLockScreenActivity
            )
        ) imageProvider.getColor(NIGHT, isLockScreenActivity) else 0
        previewViewNight.brightness = imageProvider.getBrightness(NIGHT, isLockScreenActivity)
        previewViewNight.contrast = imageProvider.getContrast(NIGHT, isLockScreenActivity)
        previewViewNight.blur = imageProvider.getBlur(NIGHT, isLockScreenActivity) / previewScale
        previewViewNight.file = currentNightFile()
        previewViewNight.setOnClickListener {
            openAdvancedDialog(NIGHT)
        }


        switchTriggerSystem.isChecked =
            preferencesGlobal.nightModeTrigger == NightModeTrigger.SYSTEM
        switchTriggerSystem.setOnCheckedChangeListener { _, isChecked ->
            onTriggerModeChanged(isChecked)
        }
        onTriggerModeChanged(switchTriggerSystem.isChecked)

        textViewStartTime.setOnClickListener {
            createTimePicker(
                this,
                load = { textViewStartTime.text.toString() },
                save = { v ->
                    textViewStartTime.text = v
                    saveTimeRange()
                }).show()
        }
        textViewEndTime.setOnClickListener {
            createTimePicker(
                this,
                load = { textViewEndTime.text.toString() },
                save = { v ->
                    textViewEndTime.text = v
                    saveTimeRange()
                }).show()
        }
        loadTimeRange()

        @SuppressLint("SetTextI18n")
        textZoomEnabled.text = "${textZoomEnabled.text} ❓"
        textZoomEnabled.setOnClickListener {
            AlertDialog.Builder(this).create().apply {
                title = getString(R.string.zoom_effect)
                setMessage(getString(R.string.zoom_effect_description))
            }.show()
        }
        switchZoomEnabled.isChecked = preferencesGlobal.zoomEnabled
        switchZoomEnabled.setOnCheckedChangeListener { _, isChecked ->
            preferencesGlobal.zoomEnabled = isChecked
        }

        if (intent != null
            && (intent.action == Intent.ACTION_SEND || intent.action == Intent.ACTION_ATTACH_DATA)
            && intent.type?.startsWith("image/") == true
        ) {
            // "Send to" / "Use as" from another app
            handleSendToAction(intent)
        } else if (dayImageFile?.exists() != true && !DarkWallpaperService.isRunning() && !isLockScreenActivity) {
            // If there is no file and the services are not running (i.e. usually a new install)
            askToImport()
        }
    }

    private fun setPreviewDimension(
        previewView: PreviewView,
        divideBy: Int = 5,
        maxRatioToScreenWidth: Int = 2,
        maxRatioToScreenHeight: Int = 1
    ) {
        val screenSize = getScreenSize()
        previewView.layoutParams = LinearLayout.LayoutParams(previewView.layoutParams).apply {
            if (isLockScreenActivity) {
                width = screenSize.x / divideBy
                height = screenSize.y / divideBy
            } else {
                val wallpaperManager = WallpaperManager.getInstance(this@MainActivity)
                width = wallpaperManager.desiredMinimumWidth / divideBy
                height = if (divideBy != 5) {
                    wallpaperManager.desiredMinimumHeight
                } else {
                    wallpaperManager.desiredMinimumWidth
                } / divideBy
            }
            while (width > screenSize.x / maxRatioToScreenWidth || height > screenSize.y / maxRatioToScreenHeight) {
                width = width * 3 / 4
                height = height * 3 / 4
            }
        }
        previewView.scaledScreenWidth = screenSize.x / divideBy
        previewView.scaledScreenHeight = screenSize.y / divideBy

        val wallpaperManager = WallpaperManager.getInstance(this@MainActivity)
        previewScale = max(
            wallpaperManager.desiredMinimumWidth / (previewView.scaledScreenWidth?.toFloat() ?: 1f),
            wallpaperManager.desiredMinimumHeight / (previewView.scaledScreenHeight?.toFloat()
                ?: 1f)
        )
    }

    private fun onTriggerModeChanged(isChecked: Boolean) {
        if (isChecked) {
            tableRowTimeRangeTrigger.visibility = View.GONE
            preferencesGlobal.nightModeTrigger = NightModeTrigger.SYSTEM
            switchTriggerSystem.setText(R.string.night_mode_trigger_follow_system)
        } else {
            tableRowTimeRangeTrigger.visibility = View.VISIBLE
            preferencesGlobal.nightModeTrigger = NightModeTrigger.TIMERANGE
            switchTriggerSystem.setText(R.string.night_mode_trigger_time_range)
        }
        DarkWallpaperService.updateNightMode()
    }

    private fun saveTimeRange() {
        preferencesGlobal.nightModeTimeRange = "${textViewStartTime.text}-${textViewEndTime.text}"
        DarkWallpaperService.updateNightMode()
    }

    @SuppressLint("SetTextI18n")
    private fun loadTimeRange() {
        val parts = preferencesGlobal.nightModeTimeRange.split("-")
        if (parts.size > 1) {
            textViewStartTime.text = parts[0]
            textViewEndTime.text = parts[1]
        } else {
            textViewStartTime.text = "20:00"
            textViewEndTime.text = "08:00"
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

    private fun saveFileFromUri(
        uri: Uri?,
        file: File,
        callback: ((success: Boolean) -> Unit)? = null
    ) {
        if (isLockScreenActivity) {
            findViewById<SwitchMaterial>(R.id.switchSeparateLockScreen).isChecked = true
        }
        val wallpaperManager = WallpaperManager.getInstance(this)
        val desiredMax =
            max(wallpaperManager.desiredMinimumWidth, wallpaperManager.desiredMinimumHeight)
        var alert: AlertDialog? = null
        var progressBar: ProgressBar? = null
        importFileThread = object : Thread("saveFileFromUri") {
            override fun run() {

                var success = false
                if (uri != null) {
                    contentResolver.openInputStream(uri)?.let { ifs ->
                        storeFile(file, ifs, desiredMax)
                        success = true
                        Log.d(
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
                        alert?.safeDismiss()
                        Toast.makeText(
                            this@MainActivity,
                            getString(
                                R.string.image_file_import_success,
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
                        alert?.setMessage(getString(R.string.image_file_import_error))
                        Toast.makeText(
                            this@MainActivity,
                            R.string.image_file_import_error,
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
            .setMessage(
                getString(
                    R.string.image_file_import_loading_message,
                    uri?.toString(),
                    file.absolutePath
                )
            )
            .setView(progressBar)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                importFileThread?.join()
                dialog.safeDismiss()
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
            imageProvider.getColor(DAY, isLockScreenActivity),
            imageProvider.getContrast(DAY, isLockScreenActivity),
            imageProvider.getBrightness(DAY, isLockScreenActivity),
            imageProvider.getBlur(DAY, isLockScreenActivity),
            { color ->
                imageProvider.setColor(DAY, isLockScreenActivity, color)
                imageProvider.setUseColor(DAY, isLockScreenActivity, true)
                switchColorDay.isChecked = true
                previewViewDay.color = color
                DarkWallpaperService.invalidate()
            }, { contrast ->
                previewViewDay.contrast = contrast
                imageProvider.setContrast(DAY, isLockScreenActivity, contrast)
            }, { brightness ->
                previewViewDay.brightness = brightness
                imageProvider.setBrightness(DAY, isLockScreenActivity, brightness)
            }, { blur ->
                previewViewDay.blur = blur / previewScale
                imageProvider.setBlur(DAY, isLockScreenActivity, blur)
            })
    }

    private fun openAdvancedLayoutNight() {
        openAdvancedLayout(previewViewNight,
            imageProvider.getColor(NIGHT, isLockScreenActivity),
            imageProvider.getContrast(NIGHT, isLockScreenActivity),
            imageProvider.getBrightness(NIGHT, isLockScreenActivity),
            imageProvider.getBlur(DAY, isLockScreenActivity),
            { color ->
                imageProvider.setColor(NIGHT, isLockScreenActivity, color)
                imageProvider.setUseColor(NIGHT, isLockScreenActivity, true)
                switchColorNight.isChecked = true
                previewViewNight.color = color
                DarkWallpaperService.invalidate()
            }, { contrast ->
                previewViewNight.contrast = contrast
                imageProvider.setContrast(NIGHT, isLockScreenActivity, contrast)
            }, { brightness ->
                previewViewNight.brightness = brightness
                imageProvider.setBrightness(NIGHT, isLockScreenActivity, brightness)
            }, { blur ->
                previewViewNight.blur = blur / previewScale
                imageProvider.setBlur(DAY, isLockScreenActivity, blur)
            })
    }

    override fun onBackPressed() {
        val layoutAdvanced = findViewById<ViewGroup?>(R.id.layoutAdvanced)
        if (previewViewLayoutIndex >= 0 && layoutAdvanced != null) {
            revertAdvancedLayout(layoutAdvanced)
            // Apply changes from advanced layout
            imageButtonColorDay.setColorFilter(imageProvider.getColor(DAY, isLockScreenActivity))
            imageButtonColorNight.setColorFilter(
                imageProvider.getColor(
                    NIGHT,
                    isLockScreenActivity
                )
            )
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
            alert.safeDismiss()

            var c = 1
            if (dayOrNight == NIGHT) {
                c += 10
            }
            if (isLockScreenActivity) {
                c += 1
            }
            preferencesGlobal.previewMode = c

            Intent(
                WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER
            ).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@MainActivity, DarkWallpaperService::class.java)
                )
                if (resolveActivity(packageManager) != null) {
                    startActivity(this)
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        R.string.apply_wallpaper_unavailable,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        }
        linearLayout.findViewById<Button>(R.id.buttonAdvanced).setOnClickListener {
            alert.safeDismiss()
            if (dayOrNight == NIGHT) {
                openAdvancedLayoutNight()
            } else {
                openAdvancedLayoutDay()
            }
        }
        linearLayout.findViewById<Button>(R.id.buttonNewImage).setOnClickListener {
            alert.safeDismiss()
            if (isLockScreenActivity && dayOrNight == DAY) {
                startForPickDayLockScreenFile.launch(imageChooserIntent(                        getString(
                    R.string.wallpaper_file_chooser_title,
                    getString(R.string.wallpaper_file_chooser_day_time),
                    getString(R.string.wallpaper_file_chooser_lock_screen)
                )))
                switchColorOnlyDay.isChecked = false
            } else if (isLockScreenActivity && dayOrNight == NIGHT) {
                startForPickNightLockScreenFile.launch(imageChooserIntent(                        getString(
                    R.string.wallpaper_file_chooser_title,
                    getString(R.string.wallpaper_file_chooser_night_time),
                    getString(R.string.wallpaper_file_chooser_lock_screen)
                )))
                switchColorOnlyNight.isChecked = false
                switchWallpaperReuseDay.isChecked = false
            } else if (dayOrNight == DAY) {
                startForPickDayHomeScreenFile.launch(imageChooserIntent(                        getString(
                    R.string.wallpaper_file_chooser_title,
                    getString(R.string.wallpaper_file_chooser_day_time),
                    getString(R.string.wallpaper_file_chooser_home_screen)
                )))
                switchColorOnlyDay.isChecked = false
            } else {
                startForPickNightHomeScreenFile.launch(imageChooserIntent(                        getString(
                    R.string.wallpaper_file_chooser_title,
                    getString(R.string.wallpaper_file_chooser_night_time),
                    getString(R.string.wallpaper_file_chooser_home_screen)
                )))
                switchColorOnlyNight.isChecked = false
                switchWallpaperReuseDay.isChecked = false
            }
        }
        linearLayout.findViewById<Button>(R.id.buttonDeleteImage).setOnClickListener {
            alert.safeDismiss()
            imageProvider.storeFileLocation(
                dayOrNight = dayOrNight,
                isLockScreen = isLockScreenActivity
            ).delete()
            previewViewDay.file = currentDayFile()
            previewViewNight.file = currentNightFile()
            DarkWallpaperService.invalidate(forceReload = true)
        }

        alert.show()
    }

    private fun openAdvancedLayout(
        previewView: PreviewView,
        initColor: Int,
        initContrast: Float,
        initBrightness: Float,
        initBlur: Float,
        onColorPick: (color: Int) -> Unit,
        onContrastChanged: (value: Float) -> Unit,
        onBrightnessChanged: (value: Float) -> Unit,
        onBlurChanged: (value: Float) -> Unit,
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

        val blurSeekBar = findViewById<SeekBar>(R.id.seekBarBlur)
        blurSeekBar.rotation = 0.5f
        blurSeekBar.max = 1000
        // Seek bar map: 0-1 on bar maps to 0 and 1-26 on bar maps to 0-25
        // to make it easier to select 0 i.e. no blur
        blurSeekBar.progress =
            if (initBlur <= 1f) {
                0
            } else {
                (1000f / 26f * (initBlur.coerceIn(0f, 25f) + 1f)).toInt()
            }
        blurSeekBar.setOnSeekBarChangeListener(OnSeekBarProgress { progress ->
            val v = (progress * 26f / 1000f - 1f).coerceIn(0f, 25f)
            onBlurChanged(v)
        })

        val buttonResetAdvanced = findViewById<Button>(R.id.buttonResetAdvanced)
        buttonResetAdvanced.setOnClickListener {
            brightnessSeekBar.progress = 500
            contrastSeekBar.progress = 500
            blurSeekBar.progress = 0
        }

        setPreviewDimension(previewView, 3, 1, 2)
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
        setPreviewDimension(previewView)

        disableFullScreen()

        previewViewLayoutIndex = -1
    }

    override fun onResume() {
        super.onResume()

        findViewById<View>(R.id.layoutRoot).visibility = View.VISIBLE

        buttonImportWallpaper.isVisible = !DarkWallpaperService.isRunning()

        findViewById<View>(R.id.layoutRoot).visibility = View.VISIBLE

        DarkWallpaperService.invalidate()
    }

    override fun onPause() {
        super.onPause()

        Handler(Looper.getMainLooper()).postDelayed({
            findViewById<View>(R.id.layoutRoot).visibility = View.INVISIBLE
        }, 500)
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
        return if (imageProvider.getUseColorOnly(DAY, isLockScreenActivity)) {
            null
        } else {
            dayImageFile
        }
    }

    private fun currentNightFile(): File? {
        return if (imageProvider.getUseColorOnly(NIGHT, isLockScreenActivity)) {
            null
        } else if (imageProvider.getUseNightWallpaper(isLockScreenActivity) && nightImageFile?.exists() == true) {
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
            dialog.safeDismiss()
            if (checkSelfPermission(READ_WALLPAPER_PERMISSION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                startForStoragePermission.launch(READ_WALLPAPER_PERMISSION)
            } else {
                askImportWhichWallpaper()
            }
        }
        builder.setNegativeButton(android.R.string.cancel, null)
        builder.show()
    }

    private fun askImportWhichWallpaper() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.wallpaper_import_dialog_title)
        val choices = arrayOf(
            "${getString(R.string.wallpaper_file_chooser_day_time)} / ${getString(R.string.wallpaper_file_chooser_home_screen)}",
            "${getString(R.string.wallpaper_file_chooser_night_time)} / ${getString(R.string.wallpaper_file_chooser_home_screen)}",
            "${getString(R.string.wallpaper_file_chooser_day_time)} / ${getString(R.string.wallpaper_file_chooser_lock_screen)}",
            "${getString(R.string.wallpaper_file_chooser_night_time)} / ${getString(R.string.wallpaper_file_chooser_lock_screen)}"
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
            if (checkSelfPermission(READ_WALLPAPER_PERMISSION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                startForStoragePermission.launch(READ_WALLPAPER_PERMISSION)
            } else {
                selection.forEachIndexed { index, checked ->
                    if (checked) {
                        val file = when (index) {
                            0 -> imageProvider.storeFileLocation(
                                dayOrNight = DAY,
                                isLockScreen = false
                            )
                            1 -> imageProvider.storeFileLocation(
                                dayOrNight = NIGHT,
                                isLockScreen = false
                            )
                            2 -> imageProvider.storeFileLocation(
                                dayOrNight = DAY,
                                isLockScreen = true
                            )
                            else -> imageProvider.storeFileLocation(
                                dayOrNight = NIGHT,
                                isLockScreen = true
                            )
                        }
                        importWallpaper(file)
                    }
                }
            }
        }
        builder.setNegativeButton(android.R.string.cancel, null)
        builder.show()
    }

    private fun importWallpaper(file: File? = null) {
        val wallpaperManager = WallpaperManager.getInstance(this)
        val fileLocation = file ?: imageProvider.storeFileLocation(
            dayOrNight = DAY,
            isLockScreen = isLockScreenActivity
        )
        val alert: AlertDialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.image_file_import_loading_title))
            .setMessage(
                getString(
                    R.string.image_file_import_loading_message,
                    "WallpaperManager.getDrawable()",
                    fileLocation.toString()
                )
            )
            .setView(ProgressBar(this))
            .show()
        object : Thread("saveFileFromUri") {
            override fun run() {
                var success = false
                if (checkSelfPermission(READ_WALLPAPER_PERMISSION)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    wallpaperManager.drawable?.let {
                        success = storeFile(fileLocation, it)
                    }
                }
                runOnUiThread {
                    alert.safeDismiss()
                    if (success) {
                        Toast.makeText(
                            this@MainActivity,
                            getString(
                                R.string.wallpaper_import_success,
                                dayImageFile?.absolutePath
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d(TAG, "Wallpaper imported to $fileLocation")

                        previewViewDay.file = currentDayFile()
                        previewViewNight.file = currentNightFile()
                        DarkWallpaperService.invalidate(forceReload = true)
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            R.string.wallpaper_import_failed,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    if (Build.VERSION.SDK_INT >= 33) {
                        revokeSelfPermissionOnKill(READ_WALLPAPER_PERMISSION)
                    }
                }
            }
        }.start()
    }

    private fun handleSendToAction(intent: Intent) {
        val dayOrNight = sendToActionIsNight()
        Log.d(TAG, "isNightSendToAction() = $dayOrNight")
        val file = imageProvider.storeFileLocation(
            dayOrNight = dayOrNight,
            isLockScreen = isLockScreenActivity
        )
        val data = intent.data ?: intent.clipData?.getItemAt(0)?.uri
        data?.let { uri ->
            saveFileFromUri(uri, file)
        }
    }

    protected open fun sendToActionIsNight(): DayOrNight {
        imageProvider.setUseColorOnly(DAY, isLockScreenActivity, false)
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
            dialog.safeDismiss()
        }
        builder.setNegativeButton(android.R.string.cancel, null)
        val dialog = builder.show()
        val colorPickerView = dialog.findViewById(R.id.colorPicker) as ColorPickerView
        colorPickerView.color = getColor()
        colorPickerView.showAlpha(true)
        colorPickerView.showHex(true)
    }

    private fun isDayOrNightMode(): DayOrNight {
        return when (preferencesGlobal.nightModeTrigger) {
            NightModeTrigger.TIMERANGE -> {
                timeIsInTimeRange(preferencesGlobal.nightModeTimeRange)
            }
            else -> {
                resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

}
