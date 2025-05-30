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

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.WallpaperColors
import android.app.wallpaper.WallpaperDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.Display
import android.view.SurfaceHolder
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import com.github.cvzi.darkmodewallpaper.activity.MainActivity
import com.github.cvzi.darkmodewallpaper.animation.BlendImages
import com.github.cvzi.darkmodewallpaper.animation.WaitAnimation
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.abs


class DarkWallpaperService : WallpaperService() {
    companion object {
        private const val TAG = "DarkWallpaperService"
        private var forceReload = false
        private val SERVICES: ArrayList<WeakReference<DarkWallpaperService>> = ArrayList()
        private val imageCache = ImageCache()
        private var loadFileThreadLock = ReentrantLock(false)
        private var loadFileThread = WeakReference<Thread>(null)

        val statusCanvasSize = Point(0, 0)
        val statusImageSize = Point(0, 0)
        val statusScaledImageSize = Point(0, 0)
        val statusRequestedSize = Point(0, 0)
        var statusScrolling = false
        var statusZoom = 0f
        var statusWallpaperColors: WallpaperColors? = null

        /**
         * Redraw or schedule redraw for all wallpapers that are currently running
         * forceReload=true reloads all images from disk
         */
        fun invalidate(forceReload: Boolean = false) {
            if (forceReload) {
                // Remove all images from memory
                imageCache.clear()
                this.forceReload = true
            }
            synchronized(SERVICES) {
                for (service in SERVICES) {
                    service.get()?.let {
                        synchronized(it.engines) {
                            for (engine in it.engines) {
                                engine.get()?.update()
                            }
                        }
                    }
                }
            }
        }

        /**
         * Event for the "separate lock screen settings" switch
         */
        fun lockScreenSettingsChanged() {
            synchronized(SERVICES) {
                for (service in SERVICES) {
                    service.get()?.let { mService ->
                        synchronized(mService.engines) {
                            for (engine in mService.engines) {
                                engine.get()?.lockScreenSettingsChanged()
                            }
                        }
                    }
                }
            }
        }

        /**
         * Event for the "custom wallpaper colors" switches in advanced settings
         */
        fun notifyColorsChanged() {
            synchronized(SERVICES) {
                for (service in SERVICES) {
                    service.get()?.let { mService ->
                        synchronized(mService.engines) {
                            for (engine in mService.engines) {
                                engine.get()?.resetWallpaperColors()
                                engine.get()?.notifyColorsChanged()
                            }
                        }
                    }
                }
            }
        }

        /**
         * Event for the night mode trigger settings
         */
        fun updateNightMode() {
            synchronized(SERVICES) {
                for (service in SERVICES) {
                    service.get()?.updateDayOrNightForAll()
                }
            }
        }

        /**
         * Returns true if any wallpaper is currently running, may be a real wallpaper or a preview.
         */
        fun isRunning(): Boolean {
            synchronized(SERVICES) {
                for (service in SERVICES) {
                    service.get()?.let {
                        synchronized(it.engines) {
                            for (engine in it.engines) {
                                if (engine.get() != null) {
                                    return true
                                }
                            }
                        }
                    }
                }
                return false
            }
        }

        /**
         * Switch between caching of images as hard or soft references
         */
        fun enableSoftReferencesCache(useSoftReferences: Boolean) =
            imageCache.enableSoftReferences(useSoftReferences)

        fun updateLuxThreshold() {
            synchronized(SERVICES) {
                for (service in SERVICES) {
                    service.get()?.updateLuxThreshold()
                }
            }
        }
    }

    private val self = WeakReference(this)
    private lateinit var preferencesGlobal: Preferences

    private var engines: ArrayList<WeakReference<WallpaperEngine>> =
        ArrayList()
    private lateinit var keyguardService: KeyguardManager

    private val overlayPaint = Paint().apply {
        isAntiAlias = false
    }

    // Keeps the last calculated value, so a new engine doesn't start with null
    private var lastWallpaperColors: WallpaperColors? = null
    private val lastWallpaperColorsMap: HashMap<String, WallpaperColors> = HashMap()

    var luxThreshold = 10
    private var lastLux = -1f
    private var lightSensorIsRegistered = false
    private var proximityClose = false
    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_LIGHT && event.values.isNotEmpty()) {
                if (!proximityClose) {
                    onLuxChange(event.values[0])
                }
            } else if (event.sensor.type == Sensor.TYPE_PROXIMITY && event.values.isNotEmpty()) {
                proximityClose = event.values[0] < event.sensor.maximumRange
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // no-op
        }
    }

    fun updateLuxThreshold() {
        luxThreshold = preferencesGlobal.luxThreshold
        if (luxThreshold > 0 && isDayOrNightMode() == DAY) {
            registerLightSensor()
        } else {
            unRegisterLightSensor()
        }
    }

    fun onLuxChange(lux: Float) {
        lastLux = lux
        if (lux < luxThreshold) {
            updateDayOrNightForAll(NIGHT, false)
        } else {
            updateDayOrNightForAll(DAY, false)
        }
    }

    init {
        overlayPaint.color = Color.argb(120, 0, 0, 0)
        overlayPaint.style = Paint.Style.FILL
    }

    override fun onCreate() {
        super.onCreate()
        synchronized(SERVICES) {
            SERVICES.add(self)
        }

        keyguardService = getSystemService(KEYGUARD_SERVICE) as KeyguardManager

        preferencesGlobal = Preferences(this, R.string.pref_file)

        // Try moving preferences to device protected storage
        Preferences.movePreferencesToDeviceProtectedStorage(
            this,
            getString(R.string.pref_file)
        )
        Preferences.movePreferencesToDeviceProtectedStorage(
            this,
            getString(R.string.pref_file_lock_screen)
        )
        // Move lock screen images to device protected storage
        StaticDayAndNightProvider(this).moveFilesToDeviceProtectedStorage()

        enableSoftReferencesCache(preferencesGlobal.autoClearMemory)

        luxThreshold = preferencesGlobal.luxThreshold
        if (luxThreshold > 0 && isDayOrNightMode() == DAY) {
            registerLightSensor()
        }
    }

    override fun onDestroy() {
        synchronized(engines) {
            for (engine in engines) {
                engine.clear()
            }
            engines.clear()
        }
        synchronized(SERVICES) {
            SERVICES.remove(self)
            self.clear()
        }
        Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null)
        unRegisterLightSensor()
        super.onDestroy()
    }

    override fun onTrimMemory(level: Int) {
        Log.d(TAG, "onTrimMemory(level=$level)")
        if (level > TRIM_MEMORY_BACKGROUND && preferencesGlobal.autoClearMemory && imageCache.size > 4) {
            imageCache.clear()
        }
        super.onTrimMemory(level)
    }

    override fun onCreateEngine(): Engine {
        return WallpaperEngine()
    }

    override fun onCreateEngine(description: WallpaperDescription): Engine {
        return WallpaperEngine()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (preferencesGlobal.nightModeTrigger == NightModeTrigger.SYSTEM) {
            var newDayOrNight: DayOrNight? = null
            when (newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_NO -> {
                    newDayOrNight = DAY
                }

                Configuration.UI_MODE_NIGHT_YES -> {
                    newDayOrNight = NIGHT
                }
            }
            if (newDayOrNight != null) {
                updateDayOrNightForAll(newDayOrNight)
            }
        }
        // Update all wallpapers after 3 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            invalidate()
        }, 3000)
    }

    private fun updateDayOrNightForAll(newDayOrNight: Boolean? = null, update: Boolean = true) {
        synchronized(engines) {
            for (engine in engines) {
                engine.get()?.run {
                    if (!this.fixedConfig) {
                        if (newDayOrNight != null && newDayOrNight == this.dayOrNight) {
                            return@run
                        }
                        this.dayOrNight = newDayOrNight ?: isDayOrNightMode()
                        if (update) {
                            this.update()
                        } else {
                            if (this.isVisible) {
                                this.invalidate()
                            }
                        }
                    }
                }
            }
        }
    }

    fun registerLightSensor() {
        if (lightSensorIsRegistered) {
            return
        }
        luxThreshold = preferencesGlobal.luxThreshold

        lightSensorIsRegistered = true
        if (lastLux >= 0) {
            onLuxChange(lastLux)
        }
        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)?.let { lightSensor ->
            Log.d(TAG, "SensorManager.registerListener() for TYPE_LIGHT")
            sensorManager.registerListener(
                sensorListener,
                lightSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)?.let { proximitySensor ->
            Log.d(TAG, "SensorManager.registerListener() for TYPE_PROXIMITY")
            sensorManager.registerListener(
                sensorListener,
                proximitySensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    fun unRegisterLightSensor() {
        if (lightSensorIsRegistered) {
            val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
            Log.d(TAG, "SensorManager.unregisterListener()")
            sensorManager.unregisterListener(sensorListener)
            lightSensorIsRegistered = false
        }
    }

    fun isDayOrNightMode(): DayOrNight {
        return when (preferencesGlobal.nightModeTrigger) {
            NightModeTrigger.TIMERANGE -> {
                timeIsInTimeRange(preferencesGlobal.nightModeTimeRange)
            }

            else -> {
                resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    private inner class WallpaperEngine : Engine() {
        var dayOrNight: DayOrNight = DAY
        var hasSeparateLockScreenSettings = false
        var isLockScreen = false
        var fixedConfig = false

        private var invalid = true
        private val self = WeakReference(this)
        private var imageProvider: ImageProvider =
            StaticDayAndNightProvider(this@DarkWallpaperService)

        private var wallpaperImage: WallpaperImage? = null
        private var isSecondaryDisplay = false
        private var width = 0
        private var height = 0
        private var visible = true
        private var currentImageFile: File? = null
        private var zoom = 0f
        private var hasZoom = true
        private var offsetX = 0.5f
        private var offsetY = 0.5f
        private var shouldScroll = true
        private var reverseScroll = false
        private var offsetXBeforeLock = 0f
        private var offsetYBeforeLock = 0f
        private var waitAnimation: WaitAnimation? = null
        private var blendImages: BlendImages? = null
        private var blendFromOffsetXPixel = 0f
        private var blendFromOffsetYPixel = 0f
        private var errorLoadingFile: String? = null
        private var errorLoadingFileTime: Long = 0L
        private var onUnLockBroadcastReceiver: OnUnLockBroadcastReceiver? = null
        private var wallpaperColors: WallpaperColors? = lastWallpaperColors
        private var calculateWallpaperColorsLastKey: String? = null
        private var calculateWallpaperColorsLastTime: Long = 0L
        private var calculateWallpaperColorsHelper: WallpaperColorsHelper? = null
        private var notifyColorsOnVisibilityChange = false
        fun invalidate() {
            invalid = true
        }

        fun lockScreenSettingsChanged() {
            hasSeparateLockScreenSettings = preferencesGlobal.separateLockScreen
            isLockScreen = false
            invalidate()
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(false)
            invalid = true
            synchronized(engines) {
                engines.add(self)
            }

            hasSeparateLockScreenSettings = preferencesGlobal.separateLockScreen
            var c = preferencesGlobal.previewMode
            if (isPreview && c > 0) {
                preferencesGlobal.previewMode = 0
                dayOrNight = if (c > 10) {
                    c -= 10
                    true
                } else {
                    false
                }
                isLockScreen = c > 1
                fixedConfig = true
            } else {
                dayOrNight = isDayOrNightMode()
            }
            imageProvider.get(dayOrNight, isLockScreen) {
                wallpaperImage = it
            }
        }

        override fun onDestroy() {
            synchronized(engines) {
                engines.remove(self)
            }
            unRegisterOnUnLock()
            super.onDestroy()
        }

        inner class OnUnLockBroadcastReceiver : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (!fixedConfig) {
                    isLockScreen = false
                    onLockScreenStatusChanged()
                }
            }


            fun register() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(this, IntentFilter().apply {
                        addAction(Intent.ACTION_USER_PRESENT)
                    }, RECEIVER_NOT_EXPORTED)
                } else {
                    @SuppressLint("UnspecifiedRegisterReceiverFlag")
                    registerReceiver(this, IntentFilter().apply {
                        addAction(Intent.ACTION_USER_PRESENT)
                    })
                }
            }

            fun unregister() {
                try {
                    unregisterReceiver(this)
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "IllegalArgumentException 01: ${e.stackTraceToString()}")
                } catch (e: RuntimeException) {
                    Log.e(TAG, "RuntimeException 02: ${e.stackTraceToString()}")
                }
            }
        }

        private fun registerOnUnLock() {
            if (onUnLockBroadcastReceiver == null) {
                onUnLockBroadcastReceiver = OnUnLockBroadcastReceiver()
            }
            onUnLockBroadcastReceiver?.register()
            Handler(Looper.getMainLooper()).postDelayed({
                // Check if the device was already unlocked before the broadcast receiver was ready
                if (isLockScreen && !keyguardService.isDeviceLocked) {
                    // Device was already unlocked
                    isLockScreen = hasSeparateLockScreenSettings && keyguardService.isDeviceLocked
                    onLockScreenStatusChanged()
                }
            }, 150)
        }

        private fun unRegisterOnUnLock() {
            onUnLockBroadcastReceiver?.unregister()
            onUnLockBroadcastReceiver = null
        }

        private fun onLockScreenStatusChanged() {
            if (fixedConfig) return

            isLockScreen = hasSeparateLockScreenSettings && keyguardService.isDeviceLocked
            imageProvider.get(dayOrNight, isLockScreen) { newWallpaperImage ->
                if (isLockScreen != hasSeparateLockScreenSettings && keyguardService.isDeviceLocked) {
                    // lock screen status has changed in the meantime -> wrong image was loaded
                    return@get onLockScreenStatusChanged()
                }

                wallpaperImage = newWallpaperImage
                if (isLockScreen) {
                    // Store current offsets
                    offsetXBeforeLock = offsetX
                    offsetYBeforeLock = offsetY
                    // Center wallpaper on lock screen
                    offsetX = 0.5f
                    registerOnUnLock()
                } else {
                    unRegisterOnUnLock()
                    blendImages = null
                    if (preferencesGlobal.animateFromLockScreen) {
                        currentImageFile?.let {
                            val (desiredWidth, desiredHeight) = desiredDimensions()
                            val key = generateCacheKey(
                                width,
                                height,
                                desiredWidth,
                                desiredHeight,
                                wallpaperImage?.brightness,
                                wallpaperImage?.contrast,
                                wallpaperImage?.blur,
                                it.absolutePath
                            )
                            val scaledBitmapOrDrawable = imageCache[key]
                            if (scaledBitmapOrDrawable?.isValid() == true) {
                                // Blend from bitmap/drawable
                                blendImages = BlendImages(
                                    scaledBitmapOrDrawable.getBitmapOrDrawable(),
                                    overlayPaint.color,
                                    blendFromOffsetXPixel,
                                    blendFromOffsetYPixel
                                )
                            }
                        }

                        if (blendImages == null) {
                            // Blend from color-only
                            blendImages = BlendImages(
                                null,
                                overlayPaint.color,
                                blendFromOffsetXPixel,
                                blendFromOffsetYPixel
                            )
                        }

                    }
                    // Restore offsets from before lock
                    offsetX = offsetXBeforeLock
                    offsetY = offsetYBeforeLock
                }

                invalid = true

                update()
            }

        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (!fixedConfig && hasSeparateLockScreenSettings && isLockScreen != keyguardService.isDeviceLocked) {
                isLockScreen = keyguardService.isDeviceLocked
                return onLockScreenStatusChanged()
            }
            if (!fixedConfig && preferencesGlobal.nightModeTrigger == NightModeTrigger.TIMERANGE) {
                // Update night mode according to time range
                val newDayOrNight: DayOrNight = isDayOrNightMode()
                if (newDayOrNight != dayOrNight) {
                    updateDayOrNightForAll(newDayOrNight)
                }
            }

            if (visible && invalid) {
                update()
            }

            if (!visible && notifyColorsOnVisibilityChange) {
                notifyColorsOnVisibilityChange = false
                notifyColorsChanged()
            }

            if (luxThreshold > 0) {
                if (visible && isDayOrNightMode() == DAY) {
                    registerLightSensor()
                } else {
                    unRegisterLightSensor()
                }
            }
        }


        override fun notifyColorsChanged() {
            if (isPreview && !isLockScreen && fixedConfig && MainActivity.originalDesiredWidth > 0) {
                Log.d(TAG, "notifyColorsChanged() blocked because: In-app preview")
            } else if (isPreview && (desiredMinimumWidth < width || desiredMinimumHeight < height)) {
                Log.d(TAG, "notifyColorsChanged() blocked because: Material you preview")
            } else if (preferencesGlobal.notifyColors || !isLockScreen) {
                Log.v(TAG, "notifyColorsChanged()")
                super.notifyColorsChanged()
            }
        }

        fun desiredDimensions(): Point {
            val desiredWidth: Int
            val desiredHeight: Int
            if (isPreview && !isLockScreen && fixedConfig && MainActivity.originalDesiredWidth > 0) {
                // Home screen preview -> use original desired width because the preview screen has usually a wrong dimension
                desiredWidth = MainActivity.originalDesiredWidth
                desiredHeight = MainActivity.originalDesiredHeight
            } else if (isPreview && (desiredMinimumWidth < width || desiredMinimumHeight < height)) {
                // Material You preview in Android settings under "Wallpaper & style"
                desiredWidth = width
                desiredHeight = height
            } else if (isSecondaryDisplay) {
                desiredWidth = width
                desiredHeight = height
            } else {
                desiredWidth = desiredMinimumWidth
                desiredHeight = desiredMinimumHeight
            }
            return Point(desiredWidth, desiredHeight)
        }

        override fun onSurfaceRedrawNeeded(holder: SurfaceHolder?) {
            if (!fixedConfig && hasSeparateLockScreenSettings && isLockScreen != keyguardService.isDeviceLocked) {
                isLockScreen = keyguardService.isDeviceLocked
                return onLockScreenStatusChanged()
            }
            if (visible) {
                update()
            }
        }

        override fun onZoomChanged(newZoom: Float) {
            if (!preferencesGlobal.zoomEnabled) {
                zoom = 0f
                return
            }
            hasZoom = true
            if (visible && (abs(zoom - newZoom) > 0.04f || newZoom == 0f || newZoom == 1f)) {
                Handler(Looper.getMainLooper()).post {
                    updateCanvas()
                }
            }
            zoom = newZoom
        }

        override fun onOffsetsChanged(
            xOffset: Float,
            yOffset: Float,
            xOffsetStep: Float,
            yOffsetStep: Float,
            xPixelOffset: Int,
            yPixelOffset: Int
        ) {
            if (fixedConfig) return
            // Scrolling on lock screen is uncommon, check whether its really still locked
            if (isLockScreen && !keyguardService.isDeviceLocked) {
                // Device was unlocked
                isLockScreen = hasSeparateLockScreenSettings && keyguardService.isDeviceLocked
                offsetX = xOffset
                offsetY = yOffset
                return onLockScreenStatusChanged()
            }
            // Scrolling detected -> redraw image
            if (offsetX != xOffset || offsetY != yOffset) {
                offsetX = if (isLockScreen) 0.5f else xOffset
                offsetY = yOffset
                if (visible && shouldScroll) {
                    update()
                }
            }
        }

        override fun onComputeColors(): WallpaperColors? {
            if (wallpaperColors == null && wallpaperImage?.customWallpaperColors != null) {
                // Only use custom wallpaper colors if the image is not loaded yet
                // otherwise the colors will be calculated from the image and mixed with the
                // custom wallpaper colors
                wallpaperColors = wallpaperImage?.customWallpaperColors
            }
            statusWallpaperColors = wallpaperColors
            return wallpaperColors
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder?, format: Int,
            width: Int, height: Int
        ) {
            this.width = width
            this.height = height
            invalid = true

            isSecondaryDisplay =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && displayContext?.display?.displayId != Display.DEFAULT_DISPLAY
        }

        override fun onDesiredSizeChanged(desiredWidth: Int, desiredHeight: Int) {
            invalid = true
            if (visible) {
                update()
            }
        }

        private fun wallpaperColorsKey(key: String): String {
            return "$key $currentImageFile ${overlayPaint.color} ${wallpaperImage?.brightness} ${wallpaperImage?.contrast} ${wallpaperImage?.blur}"
        }

        private fun wallpaperColorsShouldCalculate(wallpaperColorsKey: String): Boolean {
            return wallpaperColors == null || wallpaperColorsKey != calculateWallpaperColorsLastKey
        }

        fun resetWallpaperColors() {
            lastWallpaperColorsMap.clear()
            lastWallpaperColors = null
            calculateWallpaperColorsLastTime = 0L
            calculateWallpaperColorsLastKey = null
            wallpaperColors = null
            calculateWallpaperColorsHelper = null
            imageCache.clear()
        }

        private val runnableComputeWallpaperColors = Runnable {
            computeWallpaperColors()
        }
        private val runnableUpdate = Runnable {
            update()
        }

        private fun computeWallpaperColors() {
            val helper = calculateWallpaperColorsHelper ?: return
            if (System.nanoTime() - calculateWallpaperColorsLastTime > 1000000000L) {
                // notifyColorsChanged() should only be called every 1 second
                if (!helper.use()) return
                val customWallpaperColors = calculateWallpaperColorsHelper?.customWallpaperColors
                if (customWallpaperColors != null &&
                    customWallpaperColors.primaryColor.toArgb() != Color.TRANSPARENT
                ) {
                    // Use custom wallpaper colors
                    wallpaperColors = calculateWallpaperColorsHelper?.customWallpaperColors
                    lastWallpaperColors = wallpaperColors
                } else {
                    // Calculate wallpaper colors by drawing a frame on a canvas
                    val bm = createBitmap(width, height)
                    val canvas = Canvas(bm)
                    drawOnCanvas(canvas, helper.bitmapOrDrawable, helper.file)
                    wallpaperColors = WallpaperColors.fromBitmap(bm)
                    bm.recycle()
                    if (customWallpaperColors != null) {
                        // First color of customWallpaperColors is transparent,
                        // mix custom wallpaper colors with calculated colors
                        // this will preserve the customs dark text/theme settings
                        wallpaperColors = mix(customWallpaperColors, wallpaperColors)
                    }

                }
                lastWallpaperColors = wallpaperColors
                helper.key?.let {
                    wallpaperColors?.let {
                        if (lastWallpaperColorsMap.size > 3) {
                            lastWallpaperColorsMap.clear()
                        }
                        lastWallpaperColorsMap[helper.key] = it
                    }
                }
                calculateWallpaperColorsLastTime = System.nanoTime()
                calculateWallpaperColorsLastKey = helper.key
                calculateWallpaperColorsHelper = null
                if (isLockScreen || preferencesGlobal.notifyColorsImmediatelyAfterUnlock) {
                    notifyColorsChanged()
                } else {
                    // Don't notify on home screen, it will cause a flicker after the blending
                    // animation. Instead wait for the next app to open which will
                    // trigger an OnVisibilityChanged event
                    notifyColorsOnVisibilityChange = true
                }
            } else {
                Log.d(TAG, "computeWallpaperColors() deferred")
                Handler(Looper.getMainLooper()).apply {
                    removeCallbacks(runnableUpdate)
                    postDelayed(runnableUpdate, 1000)
                }
            }
        }

        private fun loadFile(
            imageFile: File,
            desiredWidth: Int,
            desiredHeight: Int,
            loadAnimated: Boolean
        ) {
            if (loadFileThread.get()?.isAlive == true) {
                // A thread is already running, no use spawning another one
                return
            }
            object : Thread("loadFile") {
                override fun run() {
                    try {
                        if (loadFileThreadLock.tryLock(50, TimeUnit.MILLISECONDS)) {
                            try {
                                if (loadAnimated) {
                                    loadFileAnimated(imageFile, desiredWidth, desiredHeight)
                                } else {
                                    loadFileBitmap(imageFile, desiredWidth, desiredHeight)
                                }
                            } finally {
                                loadFileThreadLock.unlock()
                            }
                        }
                    } catch (e: InterruptedException) {
                        Log.e(TAG, "loadFile() interrupted", e)
                    }
                }
            }.apply {
                start()
                loadFileThread = WeakReference(this)
            }

        }

        private fun loadFileBitmap(imageFile: File, desiredWidth: Int, desiredHeight: Int) {
            errorLoadingFile = null
            var currentBitmap: Bitmap? = null
            var isDesired = false
            val requestWidth = maxOf(desiredWidth, width)
            val requestHeight = maxOf(desiredHeight, height)
            statusRequestedSize.set(requestWidth, requestHeight)
            val originalBitmap = loadImageFile(
                imageFile,
                requestWidth,
                requestHeight
            )

            if (originalBitmap != null) {
                val (bm, isDesiredSize) = scaleAndAdjustBitmap(
                    originalBitmap,
                    width,
                    height,
                    desiredWidth,
                    desiredHeight,
                    wallpaperImage?.brightness,
                    wallpaperImage?.contrast,
                    wallpaperImage?.blur
                )
                isDesired = isDesiredSize
                shouldScroll = shouldScrollingBeEnabled(
                    isDesired,
                    wallpaperImage?.scrollingMode
                )
                reverseScroll =
                    wallpaperImage?.scrollingMode == ScrollingMode.REVERSE
                currentBitmap = bm
                if (currentBitmap != originalBitmap) {
                    originalBitmap.recycle()
                }

            } else {
                Log.e(TAG, "Failed to read image from file $imageFile")
            }
            val key = generateCacheKey(
                width,
                height,
                desiredWidth,
                desiredHeight,
                wallpaperImage?.brightness,
                wallpaperImage?.contrast,
                wallpaperImage?.blur,
                imageFile.absolutePath
            )

            if (currentBitmap == null) {
                imageCache.remove(key)
            } else {
                imageCache[key] = ScaledBitmapOrDrawable(ScaledBitmap(currentBitmap, isDesired))
            }

            currentImageFile = imageFile

            if (currentBitmap == null) {
                errorLoadingFile = "Failed to load $imageFile"
                errorLoadingFileTime = System.currentTimeMillis()
                invalid = false
            }
            update(BitmapOrDrawable(currentBitmap), isDesired)
        }

        private fun loadFileAnimated(imageFile: File, desiredWidth: Int, desiredHeight: Int) {
            errorLoadingFile = null
            val requestWidth = maxOf(desiredWidth, width)
            val requestHeight = maxOf(desiredHeight, height)
            statusRequestedSize.set(requestWidth, requestHeight)
            val drawable = loadImageFile(imageFile)

            // Apply contrast and brightness
            val contrast = wallpaperImage?.contrast ?: 1f
            val brightness = wallpaperImage?.brightness ?: 0f
            if (abs(brightness) > 3f || abs(contrast - 1f) > 0.01f) {
                drawable?.colorFilter = createColorMatrix(brightness, contrast)
            } else {
                drawable?.clearColorFilter()
            }

            if (drawable is AnimatedImageDrawable) {
                drawable.start()
            }
            val key = generateCacheKey(
                width,
                height,
                desiredWidth,
                desiredHeight,
                wallpaperImage?.brightness,
                wallpaperImage?.contrast,
                wallpaperImage?.blur,
                imageFile.absolutePath
            )

            val isDesired = true
            shouldScroll = shouldScrollingBeEnabled(
                isDesired,
                wallpaperImage?.scrollingMode
            )
            reverseScroll =
                wallpaperImage?.scrollingMode == ScrollingMode.REVERSE

            if (drawable == null) {
                imageCache.remove(key)
            } else {
                val scaledBitmapOrDrawable = ScaledBitmapOrDrawable(drawable = drawable)
                scaledBitmapOrDrawable.isDesiredSize = isDesired
                imageCache[key] = scaledBitmapOrDrawable
            }

            currentImageFile = imageFile

            if (drawable == null) {
                errorLoadingFile = "Failed to load $imageFile"
                errorLoadingFileTime = System.currentTimeMillis()
                invalid = false
            }
            update(BitmapOrDrawable(drawable = drawable), isDesired)
        }


        /**
         * Update the wallpaper appearance.
         * Reloads all preferences and draws on the canvas
         */
        fun update(
            customBitmapOrDrawable: BitmapOrDrawable? = null,
            isDesiredSize: Boolean = false
        ) {
            imageProvider.get(dayOrNight, isLockScreen) {
                wallpaperImage = it
                updateCanvas(customBitmapOrDrawable, isDesiredSize)
            }
        }

        /**
         * Draw on the canvas
         */
        fun updateCanvas(
            customBitmapOrDrawable: BitmapOrDrawable? = null,
            mIsDesiredSize: Boolean = false
        ) {
            overlayPaint.color = wallpaperImage?.color ?: 0

            val imageFile = wallpaperImage?.imageFile

            val (desiredWidth, desiredHeight) = desiredDimensions()
            val currentBitmapOrDrawable = BitmapOrDrawable()
            var isDesiredSize = false
            val key: String
            if (imageFile != null) {
                key = generateCacheKey(
                    width,
                    height,
                    desiredWidth,
                    desiredHeight,
                    wallpaperImage?.brightness,
                    wallpaperImage?.contrast,
                    wallpaperImage?.blur,
                    imageFile.absolutePath
                )
                if (customBitmapOrDrawable?.isValid() == true) {
                    currentBitmapOrDrawable.set(customBitmapOrDrawable)
                }
                if (currentBitmapOrDrawable.isNull()) {
                    val scaledBitmapOrDrawable = imageCache[key]
                    if (scaledBitmapOrDrawable?.isValid() == true) {
                        currentBitmapOrDrawable.set(scaledBitmapOrDrawable)
                        isDesiredSize = scaledBitmapOrDrawable.isDesiredSize
                    }
                } else {
                    isDesiredSize = mIsDesiredSize
                }

                if (forceReload || !currentBitmapOrDrawable.isValid()) {
                    if (forceReload) {
                        errorLoadingFile = null
                        forceReload = false
                    }
                    if (imageFile.exists() && imageFile.canRead()) {
                        if (!((currentImageFile?.name?.contains("lock") == true).xor("lock" in imageFile.name))) {
                            // Do not recycle unlocked/locked version of an image
                            currentBitmapOrDrawable.recycle()
                        }
                        if (!errorLoadingFile.isNullOrEmpty() && System.currentTimeMillis() - errorLoadingFileTime > 5000L) {
                            // Try to reload image every 5 seconds. This happens after reboot, if the sd card is not accessible yet
                            errorLoadingFile = null
                        }
                        if (loadFileThread.get()?.isAlive != true && errorLoadingFile.isNullOrEmpty()) {
                            loadFile(
                                imageFile,
                                desiredWidth,
                                desiredHeight,
                                wallpaperImage?.animated == true
                            )
                        }
                    } else {
                        errorLoadingFile = "\uD83D\uDC81\uD83C\uDFFE\u200D♀ no image file️"
                    }
                }
                shouldScroll =
                    shouldScrollingBeEnabled(isDesiredSize, wallpaperImage?.scrollingMode)
                reverseScroll =
                    wallpaperImage?.scrollingMode == ScrollingMode.REVERSE
            } else {
                key = generateSolidColorKey()
                shouldScroll = false
                reverseScroll = false
            }

            // Draw on canvas
            var canvas: Canvas? = null
            var status: WallpaperStatus? = null
            try {
                if (surfaceHolder?.surface?.isValid == true) {
                    canvas = surfaceHolder?.lockHardwareCanvas()
                }
                if (canvas != null) {
                    statusCanvasSize.set(canvas.width, canvas.height)
                    status = drawOnCanvas(canvas, currentBitmapOrDrawable, imageFile)
                }
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "lockCanvas():", e)
            } catch (e: IllegalStateException) {
                Log.e(TAG, "lockCanvas():", e)
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder?.unlockCanvasAndPost(canvas)
                    } catch (e: IllegalArgumentException) {
                        Log.e(TAG, "unlockCanvasAndPost():", e)
                    } catch (e: IllegalStateException) {
                        Log.e(TAG, "unlockCanvasAndPost():", e)
                    }
                }
            }

            // Calculate the wallpaper colors and notify Android system about new colors
            val colorKey = wallpaperColorsKey(key)
            if (status is WallpaperStatusLoaded
                && status !is WallpaperStatusLoadedBlending
                && wallpaperColorsShouldCalculate(colorKey)
            ) {
                if (colorKey in lastWallpaperColorsMap) {
                    wallpaperColors = lastWallpaperColorsMap[colorKey]
                    if (isLockScreen || preferencesGlobal.notifyColorsImmediatelyAfterUnlock) {
                        notifyColorsChanged()
                    } else {
                        notifyColorsOnVisibilityChange = true
                    }
                } else {
                    calculateWallpaperColorsHelper =
                        WallpaperColorsHelper(
                            currentBitmapOrDrawable,
                            colorKey,
                            imageFile,
                            wallpaperImage?.customWallpaperColors
                        )
                    Handler(Looper.getMainLooper()).apply {
                        removeCallbacks(runnableComputeWallpaperColors)
                        postDelayed(runnableComputeWallpaperColors, 200)
                    }
                }
            }
        }

        private fun drawOnCanvas(
            canvas: Canvas,
            bitmapOrDrawable: BitmapOrDrawable,
            imageFile: File?
        ): WallpaperStatus {
            val bm = bitmapOrDrawable.bitmap
            val drawable = bitmapOrDrawable.drawable

            if (imageFile != null && bitmapOrDrawable.isValid()) {
                statusScrolling = shouldScroll
                waitAnimation = null

                if (blendImages != null) {
                    // Blend from lock screen to home screen
                    val oX: Float
                    val oY: Float
                    if (shouldScroll) {
                        oX = offsetX
                        oY = offsetY
                    } else {
                        oX = 0.5f
                        oY = 0.5f
                    }
                    if (blendImages?.draw(
                            canvas,
                            bitmapOrDrawable,
                            overlayPaint.color,
                            oX,
                            oY,
                            reverseScroll,
                            width,
                            height
                        ) != true
                    ) {
                        blendImages = null
                    }
                    Handler(Looper.getMainLooper()).apply {
                        removeCallbacks(runnableUpdate)
                        postDelayed(runnableUpdate, 50)
                    }
                    return WallpaperStatusLoadedBlending()
                } else if (bm != null) {
                    return drawOnCanvasBitmap(canvas, bm)
                } else if (drawable != null) {
                    return drawOnCanvasDrawable(canvas, drawable)
                }
            } else if (imageFile == null) {
                // Color only
                if (blendImages != null) {
                    // Blend from lock screen to home screen
                    val oX: Float
                    val oY: Float
                    if (shouldScroll) {
                        oX = offsetX
                        oY = offsetY
                    } else {
                        oX = 0.5f
                        oY = 0.5f
                    }
                    if (blendImages?.draw(
                            canvas,
                            null,
                            overlayPaint.color or 0xFF000000.toInt(), // make the color fully opaque
                            oX,
                            oY,
                            reverseScroll,
                            width,
                            height
                        ) != true
                    ) {
                        blendImages = null
                    }
                    Handler(Looper.getMainLooper()).apply {
                        removeCallbacks(runnableUpdate)
                        postDelayed(runnableUpdate, 50)
                    }
                    return WallpaperStatusLoadedBlending()
                } else {
                    // No animation, just color
                    overlayPaint.color =
                        overlayPaint.color or 0xFF000000.toInt() // make the color fully opaque
                    canvas.drawPaint(overlayPaint)
                    return WallpaperStatusLoadedSolid()
                }
            } else {
                // No image or image is loading -> show animation
                waitAnimation = waitAnimation ?: WaitAnimation(
                    width,
                    height,
                    getString(R.string.wallpaper_is_loading)
                )
                waitAnimation?.draw(canvas, errorLoadingFile)
                Handler(Looper.getMainLooper()).apply {
                    removeCallbacks(runnableUpdate)
                    postDelayed(runnableUpdate, 200)
                }
                return WallpaperStatusLoading()
            }
            return WallpaperStatusLoading()
        }

        private fun drawOnCanvasBitmap(
            canvas: Canvas,
            bitmap: Bitmap
        ): WallpaperStatus {
            statusImageSize.set(bitmap.width, bitmap.height)
            statusScaledImageSize.set(0, 0)
            blendImages = null
            // Store current offset, in case of blending two bitmaps we will need the last offset
            if (shouldScroll && !isSecondaryDisplay) {
                blendFromOffsetXPixel = if (reverseScroll) {
                    -(1f - offsetX) * (bitmap.width - width)
                } else {
                    -offsetX * (bitmap.width - width)
                }
                blendFromOffsetYPixel = -offsetY * (bitmap.height - height)
            } else {
                blendFromOffsetXPixel = -0.5f * (bitmap.width - width)
                blendFromOffsetYPixel = -0.5f * (bitmap.height - height)
            }
            if (isPreview) {
                // Preview always set it offsetY=0.0
                blendFromOffsetYPixel = -0.5f * (bitmap.height - height)
            }

            // Zoom in (e.g. in task view)
            statusZoom = if (hasZoom) {
                canvas.save()
                canvas.scale(
                    1.0f + 0.05f * zoom, 1.0f + 0.05f * zoom,
                    0.5f * width, 0.5f * height
                )
                zoom
            } else {
                0f
            }

            // Draw bitmap
            try {
                canvas.drawBitmap(
                    bitmap,
                    blendFromOffsetXPixel,
                    blendFromOffsetYPixel,
                    null
                )
            } catch (e: RuntimeException) {
                Log.e(
                    TAG,
                    "canvas.drawBitmap() Bitmap: ${bitmap.width}x${bitmap.height} ${bitmap.byteCount}bytes",
                    e
                )
            }

            //  and color
            if (overlayPaint.color != 0) {
                canvas.drawPaint(overlayPaint)
            }

            // Turn of zoom if it reached zero
            if (hasZoom) {
                canvas.restore()
                if (zoom == 0f) {
                    hasZoom = false
                }
            }
            return WallpaperStatusLoadedImage()
        }

        private fun drawOnCanvasDrawable(
            canvas: Canvas,
            drawable: Drawable
        ): WallpaperStatus {
            statusImageSize.set(drawable.intrinsicWidth, drawable.intrinsicHeight)
            blendImages = null

            val (scale, imageWidth, imageHeight) = scaleDrawableToCanvas(drawable, width, height)
            statusScaledImageSize.set(imageWidth, imageHeight)

            // Store current offset, in case of blending two images we will need the last offset
            if (shouldScroll && !isSecondaryDisplay) {
                blendFromOffsetXPixel = if (reverseScroll) {
                    -(1f - offsetX) * (imageWidth - width)
                } else {
                    -offsetX * (imageWidth - width)
                }
                blendFromOffsetYPixel = -offsetY * (imageHeight - height)
            } else {
                blendFromOffsetXPixel = -0.5f * (imageWidth - width)
                blendFromOffsetYPixel = -0.5f * (imageHeight - height)
            }
            if (isPreview) {
                // Preview always set it offsetY=0.0
                blendFromOffsetYPixel = -0.5f * (imageHeight - height)
            }

            canvas.withSave {

                // Zoom in (e.g. in task view)
                statusZoom = if (hasZoom) {
                    scale(
                        1.0f + 0.05f * zoom, 1.0f + 0.05f * zoom,
                        0.5f * width, 0.5f * height
                    )
                    zoom
                } else {
                    0f
                }

                // Apply offset/Scrolling
                translate(blendFromOffsetXPixel, blendFromOffsetYPixel)

                // Enlarge small images
                if (scale != 1f) {
                    scale(scale, scale)
                }

                // Draw animation
                drawable.draw(this)

                //  and color
                if (overlayPaint.color != 0) {
                    drawPaint(overlayPaint)
                }

            }

            // Turn of zoom if it reached zero
            if (hasZoom) {
                if (zoom == 0f) {
                    hasZoom = false
                }
            }
            return WallpaperStatusLoadedImage()
        }
    }

    @Suppress("SameReturnValue")
    private fun generateSolidColorKey(): String {
        return "solidColor"
    }

    private fun generateCacheKey(
        width: Int,
        height: Int,
        desiredWidth: Int,
        desiredHeight: Int,
        brightness: Float?,
        contrast: Float?,
        blur: Float?,
        absolutePath: String
    ): String {
        return "${resources.configuration.orientation} $width $height $desiredWidth $desiredHeight $brightness $contrast $blur $absolutePath"
    }
}
