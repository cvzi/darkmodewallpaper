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
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.ClipData
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.DragEvent
import android.view.DragEvent.ACTION_DROP
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.children
import androidx.core.view.isVisible
import com.github.cvzi.darkmodewallpaper.*
import com.github.cvzi.darkmodewallpaper.databinding.ActivityMainBinding
import com.github.cvzi.darkmodewallpaper.databinding.DialogAdvancedBinding
import com.github.cvzi.darkmodewallpaper.databinding.LayoutAdvancedBinding
import com.github.cvzi.darkmodewallpaper.view.PreviewView
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.lang.ref.WeakReference
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
        const val WALLPAPER_EXPORT_PACKAGE = "com.github.cvzi.wallpaperexport"
        const val WALLPAPER_EXPORT_FDROID =
            "https://f-droid.org/packages/com.github.cvzi.wallpaperexport/"
    }

    protected lateinit var preferencesGlobal: Preferences
    protected var isLockScreenActivity = false
    protected lateinit var imageProvider: StaticDayAndNightProvider

    protected lateinit var binding: ActivityMainBinding
    private lateinit var previewViewDay: PreviewView
    private lateinit var previewViewNight: PreviewView
    private lateinit var scrollingModeSpinnerDay: Spinner
    private lateinit var scrollingModeSpinnerNight: Spinner
    private lateinit var scrollingModeLayoutDay: LinearLayout
    private lateinit var scrollingModeLayoutNight: LinearLayout

    private var isPaused = false
    private var previewViewLayoutIndex = -1
    private var previewScale = 1f

    private lateinit var startForPickDayHomeScreenFile: ActivityResultLauncher<Intent>
    private lateinit var startForPickNightHomeScreenFile: ActivityResultLauncher<Intent>
    private lateinit var startForPickDayLockScreenFile: ActivityResultLauncher<Intent>
    private lateinit var startForPickNightLockScreenFile: ActivityResultLauncher<Intent>
    private lateinit var startForStoragePermission: ActivityResultLauncher<String>

    private var importFileThread: Thread? = null

    private val onBackInvokedCallback: OnBackInvokedCallback? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Handle back button for Android 13+
            OnBackInvokedCallback {
                val layoutAdvanced = findViewById<ViewGroup?>(R.id.layoutAdvanced)
                if (previewViewLayoutIndex >= 0 && layoutAdvanced != null) {
                    goBackFromAdvancedLayout()
                }
            }
        } else null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        imageProvider = StaticDayAndNightProvider(WeakReference(this))

        startForPickDayHomeScreenFile = registerForActivityResult(
            dayOrNight = DAY, isLockScreen = false
        )
        startForPickNightHomeScreenFile = registerForActivityResult(
            dayOrNight = NIGHT, isLockScreen = false
        )
        startForPickDayLockScreenFile = registerForActivityResult(
            dayOrNight = DAY, isLockScreen = true
        )
        startForPickNightLockScreenFile = registerForActivityResult(
            dayOrNight = NIGHT, isLockScreen = true
        )
        startForStoragePermission = registerForActivityResult(
            RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                askImportWhichWallpaper()
            } else {
                Toast.makeText(
                    this, R.string.wallpaper_import_permission_missing, Toast.LENGTH_LONG
                ).show()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    showWallpaperExportHint()
                }
            }
        }

        preferencesGlobal = Preferences(this, R.string.pref_file)

        val wallpaperManager = WallpaperManager.getInstance(this)
        originalDesiredWidth = wallpaperManager.desiredMinimumWidth
        originalDesiredHeight = wallpaperManager.desiredMinimumHeight

        previewViewDay = binding.viewColorDay
        previewViewNight = binding.viewColorNight
        scrollingModeSpinnerDay = binding.spinnerScrollingModeDay
        scrollingModeSpinnerNight = binding.spinnerScrollingModeNight
        scrollingModeLayoutDay = binding.linearLayoutScrollingModeDay
        scrollingModeLayoutNight = binding.linearLayoutScrollingModeNight

        makeCardViewReceiveDragAndDrop(
            binding.cardViewDay, dayOrNight = DAY, isLockScreen = isLockScreenActivity
        )
        makeCardViewReceiveDragAndDrop(
            binding.cardViewNight, dayOrNight = NIGHT, isLockScreen = isLockScreenActivity
        )


        setPreviewDimension(previewViewDay)
        setPreviewDimension(previewViewNight)

        binding.buttonLockScreenSettings.setOnClickListener {
            Handler(Looper.getMainLooper()).postDelayed({
                binding.layoutRoot.visibility = View.INVISIBLE
            }, 200)

            startActivity(Intent(this, LockScreenActivity::class.java))
        }

        binding.buttonImportWallpaper.setOnClickListener {
            askToImport()
        }

        binding.buttonMoreSettings.setOnClickListener {
            startActivity(Intent(this, MoreSettingsActivity::class.java))
        }

        binding.buttonApplyWallpaper.setOnClickListener {
            var c = 1
            // Always preview Home Screen
            if (isDayOrNightMode() == NIGHT) {
                c += 10
            }

            applyLiveWallpaper(this@MainActivity, c) {
                Toast.makeText(
                    this@MainActivity, R.string.apply_wallpaper_unavailable, Toast.LENGTH_LONG
                ).show()
                binding.buttonApplyWallpaper.isEnabled = false
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
                binding.buttonApplyWallpaper.isEnabled = false
            }
        }

        binding.imageButtonAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        binding.switchColorDay.isChecked = imageProvider.getUseColor(DAY, isLockScreenActivity)
        binding.switchColorDay.setOnCheckedChangeListener { _, isChecked ->
            imageProvider.setUseColor(DAY, isLockScreenActivity, isChecked)
            previewViewDay.color =
                if (isChecked) imageProvider.getColor(DAY, isLockScreenActivity) else 0
            if (!isChecked) {
                imageProvider.setUseColorOnly(DAY, isLockScreenActivity, false)
                binding.switchColorOnlyDay.isChecked = false
            }
            DarkWallpaperService.invalidate()
        }

        binding.switchColorNight.isChecked = imageProvider.getUseColor(NIGHT, isLockScreenActivity)
        binding.switchColorNight.setOnCheckedChangeListener { _, isChecked ->
            imageProvider.setUseColor(NIGHT, isLockScreenActivity, isChecked)
            previewViewNight.color =
                if (isChecked) imageProvider.getColor(NIGHT, isLockScreenActivity) else 0
            if (!isChecked) {
                imageProvider.setUseColorOnly(NIGHT, isLockScreenActivity, false)
                binding.switchColorOnlyNight.isChecked = false
            }
            DarkWallpaperService.invalidate()
        }

        binding.switchColorOnlyDay.isChecked =
            imageProvider.getUseColorOnly(DAY, isLockScreenActivity)
        binding.switchColorOnlyDay.setOnCheckedChangeListener { _, isChecked ->
            imageProvider.setUseColorOnly(DAY, isLockScreenActivity, isChecked)
            if (isChecked) {
                imageProvider.setUseColor(DAY, isLockScreenActivity, true)
                binding.switchColorDay.isChecked = true
            }
            previewViewDay.file = currentDayFile()
            previewViewDay.invalidate()
            DarkWallpaperService.invalidate()
        }

        binding.switchColorOnlyNight.isChecked =
            imageProvider.getUseColorOnly(NIGHT, isLockScreenActivity)
        binding.switchColorOnlyNight.setOnCheckedChangeListener { _, isChecked ->
            imageProvider.setUseColorOnly(NIGHT, isLockScreenActivity, isChecked)
            if (isChecked) {
                imageProvider.setUseColor(NIGHT, isLockScreenActivity, true)
                binding.switchColorNight.isChecked = true
            }
            previewViewNight.file = currentNightFile()
            previewViewNight.invalidate()
            DarkWallpaperService.invalidate()
        }

        binding.buttonSelectFileDay.setOnClickListener {
            if (isLockScreenActivity) {
                startForPickDayLockScreenFile.launch(
                    imagePickIntent()
                )
            } else {
                startForPickDayHomeScreenFile.launch(
                    imagePickIntent()
                )
            }
            binding.switchColorOnlyDay.isChecked = false
        }

        binding.buttonSelectFileNight.isEnabled =
            imageProvider.getUseNightWallpaper(isLockScreenActivity)
        binding.buttonSelectFileNight.setOnClickListener {
            if (isLockScreenActivity) {
                startForPickNightLockScreenFile.launch(
                    imagePickIntent()
                )
            } else {
                startForPickNightHomeScreenFile.launch(
                    imagePickIntent()
                )
            }
            binding.switchColorOnlyNight.isChecked = false
            binding.switchWallpaperReuseDay.isChecked = false
        }

        binding.switchWallpaperReuseDay.isChecked =
            !imageProvider.getUseNightWallpaper(isLockScreenActivity)
        binding.switchWallpaperReuseDay.setOnCheckedChangeListener { _, isChecked ->
            imageProvider.setUseNightWallpaper(isLockScreenActivity, !isChecked)
            previewViewNight.file = currentNightFile()
            binding.buttonSelectFileNight.isEnabled = !isChecked
            DarkWallpaperService.invalidate()
        }


        binding.imageButtonColorDay.setColorFilter(
            imageProvider.getColor(
                DAY, isLockScreenActivity
            )
        )
        binding.imageButtonColorDay.setOnClickListener {
            colorChooserDialog(R.string.color_chooser_day, {
                imageProvider.getColor(DAY, isLockScreenActivity)
            }, { color ->
                imageProvider.setColor(DAY, isLockScreenActivity, color)
                imageProvider.setUseColor(DAY, isLockScreenActivity, true)
                binding.switchColorDay.isChecked = true
                binding.imageButtonColorDay.setColorFilter(color)
                previewViewDay.color = color
                binding.imageButtonColorDay.setColorFilter(color)
                DarkWallpaperService.invalidate()
            })
        }

        binding.imageButtonColorNight.setColorFilter(
            imageProvider.getColor(
                NIGHT, isLockScreenActivity
            )
        )
        binding.imageButtonColorNight.setOnClickListener {
            colorChooserDialog(R.string.color_chooser_night, {
                imageProvider.getColor(NIGHT, isLockScreenActivity)
            }, { color ->
                imageProvider.setColor(NIGHT, isLockScreenActivity, color)
                imageProvider.setUseColor(NIGHT, isLockScreenActivity, true)
                binding.switchColorNight.isChecked = true
                binding.imageButtonColorNight.setColorFilter(color)
                previewViewNight.color = color
                binding.imageButtonColorNight.setColorFilter(color)
                DarkWallpaperService.invalidate()
            })
        }


        previewViewDay.apply {
            color =
                if (imageProvider.getUseColor(DAY, isLockScreenActivity)) imageProvider.getColor(
                    DAY, isLockScreenActivity
                ) else 0
            brightness = imageProvider.getBrightness(DAY, isLockScreenActivity)
            contrast = imageProvider.getContrast(DAY, isLockScreenActivity)
            blur = imageProvider.getBlur(DAY, isLockScreenActivity) / previewScale
            file = currentDayFile()
            setOnClickListener {
                openAdvancedDialog(DAY)
            }
        }
        previewViewNight.apply {
            color = if (imageProvider.getUseColor(
                    NIGHT, isLockScreenActivity
                )
            ) imageProvider.getColor(NIGHT, isLockScreenActivity) else 0
            brightness = imageProvider.getBrightness(NIGHT, isLockScreenActivity)
            contrast = imageProvider.getContrast(NIGHT, isLockScreenActivity)
            blur = imageProvider.getBlur(NIGHT, isLockScreenActivity) / previewScale
            file = currentNightFile()
            setOnClickListener {
                openAdvancedDialog(NIGHT)
            }
        }

        binding.switchTriggerSystem.isChecked =
            preferencesGlobal.nightModeTrigger == NightModeTrigger.SYSTEM
        binding.switchTriggerSystem.setOnCheckedChangeListener { _, isChecked ->
            onTriggerModeChanged(isChecked)
        }
        onTriggerModeChanged(binding.switchTriggerSystem.isChecked)

        binding.textViewStartTime.setOnClickListener {
            createTimePicker(this,
                load = { binding.textViewStartTime.text.toString() },
                save = { v ->
                    binding.textViewStartTime.text = v
                    saveTimeRange()
                }).show()
        }
        binding.textViewEndTime.setOnClickListener {
            createTimePicker(this, load = { binding.textViewEndTime.text.toString() }, save = { v ->
                binding.textViewEndTime.text = v
                saveTimeRange()
            }).show()
        }
        loadTimeRange()

        @SuppressLint("SetTextI18n")
        binding.textZoomEnabled.text = "${binding.textZoomEnabled.text} ❓"
        binding.textZoomEnabled.setOnClickListener {
            AlertDialog.Builder(this).create().apply {
                title = getString(R.string.zoom_effect)
                setMessage(getString(R.string.zoom_effect_description))
            }.show()
        }
        binding.switchZoomEnabled.isChecked = preferencesGlobal.zoomEnabled
        binding.switchZoomEnabled.setOnCheckedChangeListener { _, isChecked ->
            preferencesGlobal.zoomEnabled = isChecked
        }

        if (isLockScreenActivity) {
            scrollingModeLayoutDay.visibility = View.GONE
            scrollingModeLayoutNight.visibility = View.GONE
        } else {
            scrollingModeLayoutDay.visibility = View.VISIBLE
            scrollingModeLayoutNight.visibility = View.VISIBLE
            ArrayAdapter.createFromResource(
                this, R.array.scrolling_mode_values, android.R.layout.simple_spinner_item
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                scrollingModeSpinnerDay.adapter = adapter
                scrollingModeSpinnerNight.adapter = adapter
                scrollingModeSpinnerDay.onItemSelectedListener =
                    ScrollingModeOnItemSelectedListener(scrollingModeSpinnerDay, imageProvider, DAY)
                scrollingModeSpinnerNight.onItemSelectedListener =
                    ScrollingModeOnItemSelectedListener(
                        scrollingModeSpinnerNight, imageProvider, NIGHT
                    )
            }
        }

        setHtmlText(binding.textViewDonate, DONATE_HTML)

        if (intent != null && (intent.action == Intent.ACTION_SEND || intent.action == Intent.ACTION_ATTACH_DATA) && intent.type?.startsWith(
                "image/"
            ) == true
        ) {
            // "Send to" / "Use as" from another app
            handleSendToAction(intent)
        } else if (!imageProvider.storeFileLocation(
                dayOrNight = DAY, isLockScreen = isLockScreenActivity
            )
                .exists() && !DarkWallpaperService.isRunning() && !isLockScreenActivity && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
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
            binding.tableRowTimeRangeTrigger.visibility = View.GONE
            preferencesGlobal.nightModeTrigger = NightModeTrigger.SYSTEM
            binding.switchTriggerSystem.setText(R.string.night_mode_trigger_follow_system)
        } else {
            binding.tableRowTimeRangeTrigger.visibility = View.VISIBLE
            preferencesGlobal.nightModeTrigger = NightModeTrigger.TIMERANGE
            binding.switchTriggerSystem.setText(R.string.night_mode_trigger_time_range)
        }
        DarkWallpaperService.updateNightMode()
    }

    private fun saveTimeRange() {
        preferencesGlobal.nightModeTimeRange =
            "${binding.textViewStartTime.text}-${binding.textViewEndTime.text}"
        DarkWallpaperService.updateNightMode()
    }

    @SuppressLint("SetTextI18n")
    private fun loadTimeRange() {
        val parts = preferencesGlobal.nightModeTimeRange.split("-")
        if (parts.size > 1) {
            binding.textViewStartTime.text = parts[0]
            binding.textViewEndTime.text = parts[1]
        } else {
            binding.textViewStartTime.text = "20:00"
            binding.textViewEndTime.text = "08:00"
        }
    }

    private fun makeCardViewReceiveDragAndDrop(
        cardViewDay: CardView, dayOrNight: DayOrNight, isLockScreen: Boolean
    ) {
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
                                R.color.day_background, null
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
                            R.color.switch_green, null
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
                            R.color.day_background, null
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
                    saveFileFromUri(imageItem.uri, dayOrNight, isLockScreen) {
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

    private fun registerForActivityResult(
        dayOrNight: DayOrNight, isLockScreen: Boolean
    ): ActivityResultLauncher<Intent> {
        return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                saveFileFromActivityResult(result, dayOrNight, isLockScreen)
            }
        }
    }

    private fun saveFileFromActivityResult(
        result: ActivityResult, dayOrNight: DayOrNight, isLockScreen: Boolean
    ) {
        return saveFileFromUri(result.data?.data, dayOrNight, isLockScreen)
    }

    private fun saveFileFromUri(
        uri: Uri?,
        dayOrNight: DayOrNight,
        isLockScreen: Boolean,
        callback: ((success: Boolean) -> Unit)? = null
    ) {
        if (isLockScreen && isLockScreenActivity) {
            binding.switchSeparateLockScreen.isChecked = true
        }
        val wallpaperManager = WallpaperManager.getInstance(this)
        val desiredMax =
            max(wallpaperManager.desiredMinimumWidth, wallpaperManager.desiredMinimumHeight)
        var alert: AlertDialog? = null
        var progressBar: ProgressBar? = null
        // Store file with temporary extension
        val file =
            imageProvider.storeFileLocation(dayOrNight, isLockScreen, isAnimated = false).run {
                File(parent, "${nameWithoutExtension}.tmp")
            }
        Log.v(TAG, "file is ${file.name}")
        importFileThread = object : Thread("saveFileFromUri") {
            override fun run() {
                var result: StoreFileResult? = null
                var success = false
                if (uri != null) {
                    try {
                        contentResolver.openInputStream(uri)?.let { ifs ->
                            result = storeFile(file, ifs, desiredMax)
                            success = result?.success == true
                            Log.d(
                                TAG, "Stored ${file.nameWithoutExtension} wallpaper in $file"
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error storing file", e)
                    }
                }
                runOnUiThread {
                    if (callback != null) {
                        callback(success)
                    }
                    if (success) {
                        // Rename the file to .gif in case it is animated or .webp for static
                        Log.v(TAG, "File: ${file.name} is animated: ${result?.isAnimated}")
                        imageProvider.setNewFile(
                            dayOrNight, isLockScreen, result?.isAnimated == true, file
                        )
                        alert?.safeDismiss()
                        Toast.makeText(
                            this@MainActivity, getString(
                                R.string.image_file_import_success, file.absolutePath
                            ), Toast.LENGTH_SHORT
                        ).show()
                        previewViewDay.file = currentDayFile()
                        previewViewNight.file = currentNightFile()

                        DarkWallpaperService.invalidate(forceReload = true)
                    } else {
                        progressBar?.isVisible = false
                        alert?.getButton(AlertDialog.BUTTON_POSITIVE)?.isVisible = true
                        alert?.setMessage(getString(R.string.image_file_import_error))
                        Toast.makeText(
                            this@MainActivity, R.string.image_file_import_error, Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
        importFileThread?.start()
        progressBar = ProgressBar(this)
        alert =
            AlertDialog.Builder(this).setTitle(getString(R.string.image_file_import_loading_title))
                .setMessage(
                    getString(
                        R.string.image_file_import_loading_message,
                        uri?.toString(),
                        file.absolutePath
                    )
                ).setView(progressBar).setPositiveButton(android.R.string.ok) { dialog, _ ->
                    try {
                        importFileThread?.join()
                    } catch (e: InterruptedException) {
                        Log.e(TAG, "Error joining thread", e)
                    }
                    dialog.safeDismiss()
                    file.delete()
                    File(file.parent, "${file.name}.tmp").delete()
                    previewViewDay.file = currentDayFile()
                    previewViewNight.file = currentNightFile()
                    DarkWallpaperService.invalidate(forceReload = true)
                }.show()
        alert?.getButton(AlertDialog.BUTTON_POSITIVE)?.isVisible = false
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // This is no longer used on Android 13+/Tiramisu
        // See onBackInvokedCallback for Android 13+
        val layoutAdvanced = findViewById<ViewGroup?>(R.id.layoutAdvanced)
        if (previewViewLayoutIndex >= 0 && layoutAdvanced != null) {
            goBackFromAdvancedLayout()
        } else {
            @Suppress("DEPRECATION")
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
        val dialogBinding = DialogAdvancedBinding.inflate(this@MainActivity.layoutInflater)
        alert.setView(dialogBinding.root)

        dialogBinding.buttonPreview.setOnClickListener {
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
                        this@MainActivity, R.string.apply_wallpaper_unavailable, Toast.LENGTH_LONG
                    ).show()
                }
            }

        }
        dialogBinding.buttonAdvanced.setOnClickListener {
            alert.safeDismiss()
            if (dayOrNight == NIGHT) {
                openAdvancedLayoutNight()
            } else {
                openAdvancedLayoutDay()
            }
        }
        dialogBinding.buttonNewImage.setOnClickListener {
            alert.safeDismiss()
            if (isLockScreenActivity && dayOrNight == DAY) {
                startForPickDayLockScreenFile.launch(
                    imageChooserIntent(
                        getString(
                            R.string.wallpaper_file_chooser_title,
                            getString(R.string.wallpaper_file_chooser_day_time),
                            getString(R.string.wallpaper_file_chooser_lock_screen)
                        )
                    )
                )
                binding.switchColorOnlyDay.isChecked = false
            } else if (isLockScreenActivity && dayOrNight == NIGHT) {
                startForPickNightLockScreenFile.launch(
                    imageChooserIntent(
                        getString(
                            R.string.wallpaper_file_chooser_title,
                            getString(R.string.wallpaper_file_chooser_night_time),
                            getString(R.string.wallpaper_file_chooser_lock_screen)
                        )
                    )
                )
                binding.switchColorOnlyNight.isChecked = false
                binding.switchWallpaperReuseDay.isChecked = false
            } else if (dayOrNight == DAY) {
                startForPickDayHomeScreenFile.launch(
                    imageChooserIntent(
                        getString(
                            R.string.wallpaper_file_chooser_title,
                            getString(R.string.wallpaper_file_chooser_day_time),
                            getString(R.string.wallpaper_file_chooser_home_screen)
                        )
                    )
                )
                binding.switchColorOnlyDay.isChecked = false
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
                binding.switchColorOnlyNight.isChecked = false
                binding.switchWallpaperReuseDay.isChecked = false
            }
        }
        dialogBinding.buttonDeleteImage.setOnClickListener {
            alert.safeDismiss()
            imageProvider.storeFileLocation(
                dayOrNight = dayOrNight, isLockScreen = isLockScreenActivity
            ).delete()
            previewViewDay.file = currentDayFile()
            previewViewNight.file = currentNightFile()
            DarkWallpaperService.invalidate(forceReload = true)
        }

        alert.show()
    }

    private fun openAdvancedLayoutDayOrNight(
        previewView: PreviewView, switchColor: SwitchMaterial, isDayOrNight: DayOrNight
    ) {
        var shownHintBlurNotAvailable = false
        openAdvancedLayout(previewView,
            imageProvider.getColor(isDayOrNight, isLockScreenActivity),
            imageProvider.getContrast(isDayOrNight, isLockScreenActivity),
            imageProvider.getBrightness(isDayOrNight, isLockScreenActivity),
            imageProvider.getBlur(isDayOrNight, isLockScreenActivity),
            { color ->
                imageProvider.setColor(isDayOrNight, isLockScreenActivity, color)
                imageProvider.setUseColor(isDayOrNight, isLockScreenActivity, true)
                switchColor.isChecked = true
                previewView.color = color
                DarkWallpaperService.invalidate()
            },
            { contrast ->
                previewView.contrast = contrast
                imageProvider.setContrast(isDayOrNight, isLockScreenActivity, contrast)
            },
            { brightness ->
                previewView.brightness = brightness
                imageProvider.setBrightness(isDayOrNight, isLockScreenActivity, brightness)
            },
            { blur ->
                previewView.blur = blur / previewScale
                imageProvider.setBlur(isDayOrNight, isLockScreenActivity, blur)
                if (!shownHintBlurNotAvailable && imageProvider.isAnimated(
                        isDayOrNight, isLockScreenActivity
                    )
                ) {
                    shownHintBlurNotAvailable = true
                    Toast.makeText(
                        this, getString(R.string.blur_unavailable_in_animations), Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun openAdvancedLayoutDay() {
        openAdvancedLayoutDayOrNight(previewViewDay, binding.switchColorDay, DAY)
    }

    private fun openAdvancedLayoutNight() {
        openAdvancedLayoutDayOrNight(previewViewNight, binding.switchColorNight, NIGHT)
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
            binding.cardViewLockScreenSwitch.visibility = View.GONE
        }

        val layoutAdvancedBinding =
            LayoutAdvancedBinding.inflate(layoutInflater, linearLayout, true)

        val tmp = layoutAdvancedBinding.placeHolderForPreviewView
        val parent = tmp.parent as LinearLayout
        val index = parent.indexOfChild(tmp)
        parent.removeView(tmp)
        previewViewLayoutIndex = linearLayout.indexOfChild(previewView)
        linearLayout.removeView(previewView)
        parent.addView(previewView, index)
        previewView.tag = "previewView"

        layoutAdvancedBinding.colorPickerAdvanced.apply {
            color = initColor
            showAlpha(true)
            showHex(false)
            addColorObserver {
                onColorPick(it.color)
            }
        }

        layoutAdvancedBinding.seekBarContrast.apply {
            rotation = 0.5f
            max = 1000
            progress = (342.0 * (exp(initContrast - 0.1) - 1.0)).toInt()
            setOnSeekBarChangeListener(OnSeekBarProgress { progress ->
                val v = 0.1f + log(1.0 + progress / 342.0, Math.E).toFloat()
                onContrastChanged(v)
            })
        }

        layoutAdvancedBinding.seekBarBrightness.apply {
            rotation = 0.5f
            max = 1000
            progress = (1.97 * initBrightness + 500).toInt()
            setOnSeekBarChangeListener(OnSeekBarProgress { progress ->
                val v = (progress - 500f) / 1.97f
                onBrightnessChanged(v)
            })
        }


        layoutAdvancedBinding.seekBarBlur.rotation = 0.5f
        layoutAdvancedBinding.seekBarBlur.max = 1000
        // Seek bar map: 0-1 on bar maps to 0 and 1-101 on bar maps to 0-100
        // to make it easier to select 0 i.e. no blur
        layoutAdvancedBinding.seekBarBlur.progress = if (initBlur <= 1f) {
            0
        } else {
            (1000f / 106f * (initBlur.coerceIn(0f, 100f) + 5f)).toInt()
        }
        layoutAdvancedBinding.seekBarBlur.setOnSeekBarChangeListener(OnSeekBarProgress { progress ->
            val v = (progress * 106f / 1000f - 5f).coerceIn(0f, 100f)
            onBlurChanged(v)
            layoutAdvancedBinding.labelBlur.apply {
                @SuppressLint("SetTextI18n")
                text = "%.1f".format(v)
                width = (resources.displayMetrics.density * 40).toInt()
            }
        })

        layoutAdvancedBinding.buttonResetAdvanced.setOnClickListener {
            layoutAdvancedBinding.seekBarBrightness.progress = 500
            layoutAdvancedBinding.seekBarContrast.progress = 500
            layoutAdvancedBinding.seekBarBlur.progress = 0
        }

        setPreviewDimension(previewView, 3, 1, 2)

        // Handle back button on Android 13
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedCallback?.let {
                onBackInvokedDispatcher.registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, onBackInvokedCallback
                )
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
            binding.cardViewLockScreenSwitch.visibility = View.VISIBLE
        }

        // Resize color view
        setPreviewDimension(previewView)

        disableFullScreen()

        previewViewLayoutIndex = -1

        // Remove handler for back button on Android 13
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedCallback?.let {
                onBackInvokedDispatcher.unregisterOnBackInvokedCallback(onBackInvokedCallback)
            }
        }
    }

    private fun goBackFromAdvancedLayout() {
        val layoutAdvanced = findViewById<ViewGroup?>(R.id.layoutAdvanced)
        revertAdvancedLayout(layoutAdvanced)
        // Apply changes from advanced layout
        binding.imageButtonColorDay.setColorFilter(
            imageProvider.getColor(
                DAY, isLockScreenActivity
            )
        )
        binding.imageButtonColorNight.setColorFilter(
            imageProvider.getColor(
                NIGHT, isLockScreenActivity
            )
        )
        DarkWallpaperService.invalidate()
    }

    override fun onResume() {
        super.onResume()
        isPaused = false

        binding.layoutRoot.visibility = View.VISIBLE

        binding.buttonImportWallpaper.isVisible = !DarkWallpaperService.isRunning()

        binding.layoutRoot.visibility = View.VISIBLE

        DarkWallpaperService.invalidate()

        updateStatusValues()
    }

    override fun onPause() {
        super.onPause()
        isPaused = true

        Handler(Looper.getMainLooper()).postDelayed({
            binding.layoutRoot.visibility = View.INVISIBLE
        }, 500)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        when (newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> {
                binding.textStatusDayOrNight.text = getString(R.string.status_darkmode_day)
            }

            Configuration.UI_MODE_NIGHT_YES -> {
                binding.textStatusDayOrNight.text = getString(R.string.status_darkmode_night)
            }
        }
    }


    private fun updateStatusValues() {
        if (isDestroyed || isFinishing || isPaused) {
            return
        }
        val delayMillis = 1500L
        val colors = DarkWallpaperService.statusWallpaperColors
        if (colors == null) {
            // Try to load the system colors
            CoroutineScope(Dispatchers.Default).launch {
                val systemColors = WallpaperManager.getInstance(this@MainActivity)
                    .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                runOnUiThread {
                    updateStatusValues(systemColors)
                    Handler(Looper.getMainLooper()).postDelayed(
                        {
                            updateStatusValues()
                        }, delayMillis
                    )
                }
            }
        } else {
            updateStatusValues(colors)
            Handler(Looper.getMainLooper()).postDelayed(
                {
                    updateStatusValues()
                }, delayMillis
            )
        }
    }

    private fun updateStatusValues(colors: WallpaperColors?) {
        val wallpaperManager = WallpaperManager.getInstance(this)
        val screenSize = getScreenSize()

        binding.apply {

            textStatusDayOrNight.text = getString(
                if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                    R.string.status_darkmode_night
                } else {
                    R.string.status_darkmode_day
                }
            )

            textStatusDesiredDimensions.text = getString(
                R.string.resolution_width_x_height,
                wallpaperManager.desiredMinimumWidth,
                wallpaperManager.desiredMinimumHeight
            )

            textStatusScreenDimensions.text = getString(
                R.string.resolution_width_x_height, screenSize.x, screenSize.y
            )

            textStatusCanvasDimensions.text = DarkWallpaperService.statusCanvasSize.toSizeString()

            textStatusImageSize.text = if (DarkWallpaperService.statusScaledImageSize.x > 0) {
                "${DarkWallpaperService.statusImageSize.toSizeString()} scaled to ${DarkWallpaperService.statusScaledImageSize.toSizeString()}"
            } else {
                DarkWallpaperService.statusImageSize.toSizeString()
            }

            textStatusRequestedSize.text = DarkWallpaperService.statusRequestedSize.toSizeString()

            textStatusScrolling.text = DarkWallpaperService.statusScrolling.toString()

            textStatusZoom.text = DarkWallpaperService.statusZoom.toString()

            textWallpaperColors.text = colors?.toPrettyString() ?: "Not requested yet"

            colors?.let {
                viewColorPrimary.setBackgroundColor(colors.primaryColor.toArgb())
                viewColorSecondary.setBackgroundColor(
                    colors.secondaryColor?.toArgb() ?: Color.TRANSPARENT
                )
                viewColorTertiary.setBackgroundColor(
                    colors.tertiaryColor?.toArgb() ?: Color.TRANSPARENT
                )

            } ?: viewColorPrimary.setBackgroundColor(Color.TRANSPARENT)

        }
    }

    private fun currentDayFile(): File? {
        return if (imageProvider.getUseColorOnly(DAY, isLockScreenActivity)) {
            null
        } else {
            imageProvider.storeFileLocation(dayOrNight = DAY, isLockScreen = isLockScreenActivity)
        }
    }

    private fun currentNightFile(): File? {
        val dayImageFile =
            imageProvider.storeFileLocation(dayOrNight = DAY, isLockScreen = isLockScreenActivity)
        val nightImageFile =
            imageProvider.storeFileLocation(dayOrNight = NIGHT, isLockScreen = isLockScreenActivity)
        return if (imageProvider.getUseColorOnly(NIGHT, isLockScreenActivity)) {
            null
        } else if (imageProvider.getUseNightWallpaper(isLockScreenActivity) && nightImageFile.exists()) {
            nightImageFile
        } else {
            dayImageFile
        }
    }

    private fun askToImport() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.wallpaper_import_dialog_title)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            builder.setMessage(
                "(This function may be broken on Android 13 Tiramisu and higher and you may see a permission error)\n\n" + getString(
                    R.string.wallpaper_import_dialog_message
                )
            )
        } else {
            builder.setMessage(R.string.wallpaper_import_dialog_message)
        }
        builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
            dialog.safeDismiss()
            if (checkSelfPermission(READ_WALLPAPER_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
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
            choices, selection.toBooleanArray()
        ) { _: DialogInterface, which: Int, checked: Boolean ->
            selection[which] = checked
        }
        builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
            dialog.safeDismiss()
            if (checkSelfPermission(READ_WALLPAPER_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                startForStoragePermission.launch(READ_WALLPAPER_PERMISSION)
            } else {
                selection.forEachIndexed { index, checked ->
                    if (checked) {
                        val file = when (index) {
                            0 -> imageProvider.storeFileLocation(
                                dayOrNight = DAY, isLockScreen = false
                            )

                            1 -> imageProvider.storeFileLocation(
                                dayOrNight = NIGHT, isLockScreen = false
                            )

                            2 -> imageProvider.storeFileLocation(
                                dayOrNight = DAY, isLockScreen = true
                            )

                            else -> imageProvider.storeFileLocation(
                                dayOrNight = NIGHT, isLockScreen = true
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

    @SuppressLint("MissingPermission")
    private fun importWallpaper(file: File? = null) {
        val wallpaperManager = WallpaperManager.getInstance(this)
        val fileLocation = file ?: imageProvider.storeFileLocation(
            dayOrNight = DAY, isLockScreen = isLockScreenActivity
        )
        val alert: AlertDialog =
            AlertDialog.Builder(this).setTitle(getString(R.string.image_file_import_loading_title))
                .setMessage(
                    getString(
                        R.string.image_file_import_loading_message,
                        "WallpaperManager.getDrawable()",
                        fileLocation.toString()
                    )
                ).setView(ProgressBar(this)).show()
        object : Thread("saveFileFromUri") {
            override fun run() {
                var success = false
                if (checkSelfPermission(READ_WALLPAPER_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
                    wallpaperManager.drawable?.let {
                        success = storeFile(fileLocation, it)
                    }
                }
                runOnUiThread {
                    alert.safeDismiss()
                    if (success) {
                        Toast.makeText(
                            this@MainActivity, getString(
                                R.string.wallpaper_import_success, fileLocation.absolutePath
                            ), Toast.LENGTH_SHORT
                        ).show()
                        Log.d(TAG, "Wallpaper imported to $fileLocation")

                        previewViewDay.file = currentDayFile()
                        previewViewNight.file = currentNightFile()
                        DarkWallpaperService.invalidate(forceReload = true)
                    } else {
                        Toast.makeText(
                            this@MainActivity, R.string.wallpaper_import_failed, Toast.LENGTH_LONG
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
        val dayOrNight = sendToActionIsDayOrNight()
        Log.d(TAG, "isNightSendToAction() = $dayOrNight")
        val data = intent.data ?: intent.clipData?.getItemAt(0)?.uri
        data?.let { uri ->
            saveFileFromUri(uri, dayOrNight, isLockScreenActivity)
        }
    }

    @Suppress("SameReturnValue")
    protected open fun sendToActionIsDayOrNight(): DayOrNight {
        imageProvider.setUseColorOnly(DAY, isLockScreenActivity, false)
        return DAY
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


    private fun showWallpaperExportHint() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.wallpaper_import_dialog_title)
        builder.setMessage("Permission to import wallpaper was denied. If you did not see a permission dialog to allow the permission, Android 13+ has denied the permission automatically.\n\nHowever you can export the current wallpaper with a separate app and then import the image to this app.\n\nGo to separate app?\n\nhttps://f-droid.org/packages/com.github.cvzi.wallpaperexport/")
        builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
            dialog.safeDismiss()

            val intent = packageManager.getLaunchIntentForPackage(WALLPAPER_EXPORT_PACKAGE)
            if (intent?.resolveActivity(packageManager) != null) {
                intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } else {
                Intent(ACTION_VIEW, Uri.parse(WALLPAPER_EXPORT_FDROID)).apply {
                    if (resolveActivity(packageManager) != null) {
                        startActivity(Intent.createChooser(this, WALLPAPER_EXPORT_FDROID))
                    } else {
                        Log.e(TAG, "showWallpaperExportHint: No browser installed")
                    }
                }
            }
        }
        builder.setNegativeButton(android.R.string.cancel, null)
        builder.show()
    }
}

class ScrollingModeOnItemSelectedListener(
    spinner: Spinner,
    private val imageProvider: StaticDayAndNightProvider,
    private val isDayOrNight: DayOrNight
) : AdapterView.OnItemSelectedListener {
    init {
        spinner.setSelection(imageProvider.getScrollingMode(isDayOrNight).ordinal)
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        imageProvider.setScrollingMode(isDayOrNight, ScrollingMode.entries[pos])
        DarkWallpaperService.invalidate()
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        Log.v("Spinner", "onNothingSelected")
    }
}
