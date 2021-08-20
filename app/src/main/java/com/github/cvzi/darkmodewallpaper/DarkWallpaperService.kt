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

import android.app.KeyguardManager
import android.app.WallpaperColors
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.Display
import android.view.SurfaceHolder
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import com.github.cvzi.darkmodewallpaper.activity.MainActivity
import com.github.cvzi.darkmodewallpaper.animation.BlendBitmaps
import com.github.cvzi.darkmodewallpaper.animation.WaitAnimation
import java.io.File
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.abs


class DarkWallpaperService : WallpaperService() {
    companion object {
        private const val TAG = "DarkWallpaperService"
        private var forceReload = false
        private val SERVICES: ArrayList<WeakReference<DarkWallpaperService>> = ArrayList()
        private val bitmaps: ConcurrentHashMap<String, SoftReference<ScaledBitmap>?> =
            ConcurrentHashMap()
        private var loadFileThreadLock = ReentrantLock(false)
        private var loadFileThread = WeakReference<Thread>(null)

        /**
         * Redraw or schedule redraw for all wallpapers that are currently running
         * forceReload=true reloads all bitmaps from disk
         */
        fun invalidate(forceReload: Boolean = false) {
            if (forceReload) {
                // Remove all bitmaps from memory
                bitmaps.clear()
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
    }

    private val self = WeakReference(this)
    private lateinit var preferencesGlobal: Preferences

    private var engines: ArrayList<WeakReference<DarkWallpaperService.WallpaperEngine>> =
        ArrayList()
    private lateinit var keyguardService: KeyguardManager

    private val overlayPaint = Paint()
    private val bitmapPaint = Paint()
    private val colorMatrixArray = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    // Keeps the last calculated value, so a new engine doesn't start with null
    private var lastWallpaperColors: WallpaperColors? = null

    init {
        overlayPaint.color = Color.argb(120, 0, 0, 0)
        overlayPaint.style = Paint.Style.FILL
    }

    override fun onCreate() {
        super.onCreate()
        synchronized(SERVICES) {
            SERVICES.add(self)
        }

        keyguardService = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        preferencesGlobal = Preferences(this, R.string.pref_file)
    }

    override fun onDestroy() {
        super.onDestroy()
        synchronized(SERVICES) {
            SERVICES.remove(self)
        }
    }

    override fun onCreateEngine(): Engine {
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
    }

    private fun updateDayOrNightForAll(newDayOrNight: Boolean? = null) {
        Log.v(TAG, "updateDayOrNightForAll()")
        synchronized(engines) {
            for (engine in engines) {
                engine.get()?.run {
                    if (!this.fixedConfig) {
                        this.dayOrNight = newDayOrNight ?: this.isDayOrNightMode()
                        this.update()
                    }
                }
            }
        }
    }

    private fun updateColorMatrix(brightness: Float, contrast: Float) {
        val b = (brightness - contrast) / 2f
        colorMatrixArray[0] = contrast
        colorMatrixArray[4] = b
        colorMatrixArray[6] = contrast
        colorMatrixArray[9] = b
        colorMatrixArray[12] = contrast
        colorMatrixArray[14] = b
        bitmapPaint.colorFilter = ColorMatrixColorFilter(colorMatrixArray)
    }

    private inner class WallpaperEngine : WallpaperService.Engine() {
        var dayOrNight: DayOrNight = DAY
        var hasSeparateLockScreenSettings = false
        var isLockScreen = false
        var fixedConfig = false

        private var invalid = true
        private var imageProvider: ImageProvider =
            StaticDayAndNightProvider(this@DarkWallpaperService)
        private val self = WeakReference(this)

        //private lateinit var dayImageLocation: File
        //private lateinit var nightImageLocation: File
        private var wallpaperImage: WallpaperImage? = null
        private var isSecondaryDisplay = false
        private var width = 0
        private var height = 0
        private var visible = true
        private var currentBitmapFile: File? = null
        private var zoom = 0f
        private var hasZoom = true
        private var offsetX = 0.5f
        private var offsetY = 0.5f
        private var shouldScroll = true
        private var offsetXBeforeLock = 0f
        private var offsetYBeforeLock = 0f
        private var lastBitmapPaint = Paint()
        private var waitAnimation: WaitAnimation? = null
        private var blendBitmaps: BlendBitmaps? = null
        private var blendFromOffsetXPixel = 0f
        private var blendFromOffsetYPixel = 0f
        private var errorLoadingFile: String? = null
        private var onUnLockBroadcastReceiver: OnUnLockBroadcastReceiver? = null
        private var wallpaperColors: WallpaperColors? = lastWallpaperColors
        private var calculateWallpaperColorsLastKey: String? = null
        private var calculateWallpaperColors = false
        private var calculateWallpaperColorsBitmap: Bitmap? = null
        private var calculateWallpaperColorsKey: String? = null
        private var calculateWallpaperColorsFile: File? = null
        private var calculateWallpaperColorsTime: Long = 0L
        fun invalidate() {
            invalid = true
        }

        fun lockScreenSettingsChanged() {
            hasSeparateLockScreenSettings = isSeparateLockScreenEnabled()
            isLockScreen = false
            invalidate()
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            invalid = true
            synchronized(engines) {
                engines.add(self)
            }

            hasSeparateLockScreenSettings = isSeparateLockScreenEnabled()

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
            //dayImageLocation = dayFileLocation(isLockScreen)
            //nightImageLocation = nightFileLocation(isLockScreen)
            imageProvider.get(dayOrNight, isLockScreen) {
                wallpaperImage = it
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            synchronized(engines) {
                engines.remove(self)
            }
            unRegisterOnUnLock()
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

        inner class OnUnLockBroadcastReceiver : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (!fixedConfig) {
                    isLockScreen = false
                    onLockScreenStatusChanged()
                }
            }

            fun register() {
                registerReceiver(this, IntentFilter().apply {
                    addAction(Intent.ACTION_USER_PRESENT)
                })
            }

            fun unregister() {
                try {
                    unregisterReceiver(this)
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, e.stackTraceToString())
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
                    blendBitmaps = null
                    if (isAnimateFromLockScreen()) {
                        currentBitmapFile?.let {
                            val (desiredWidth, desiredHeight) = desiredDimensions()
                            val key =
                                "$width $height $desiredWidth $desiredHeight ${it.absolutePath}"
                            val scaledBitmap = bitmaps.getOrDefault(key, null)?.get()
                            if (scaledBitmap != null) {
                                val (blendFromBitmap, _) = scaledBitmap
                                blendBitmaps = BlendBitmaps(
                                    blendFromBitmap,
                                    lastBitmapPaint,
                                    overlayPaint.color,
                                    blendFromOffsetXPixel,
                                    blendFromOffsetYPixel
                                )
                            }
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
        }


        fun desiredDimensions(): Point {
            val desiredWidth: Int
            val desiredHeight: Int
            if (isPreview && !isLockScreen && fixedConfig && MainActivity.originalDesiredWidth > 0) {
                // Home screen preview -> use original desired width because the preview screen has usually a wrong dimension
                desiredWidth = MainActivity.originalDesiredWidth
                desiredHeight = MainActivity.originalDesiredHeight
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
            // Scrolling detected -> redraw bitmap
            if (offsetX != xOffset || offsetY != yOffset) {
                offsetX = if (isLockScreen) 0.5f else xOffset
                offsetY = yOffset
                if (visible && shouldScroll) {
                    update()
                }
            }
        }

        override fun onComputeColors(): WallpaperColors? {
            // Log.v(TAG, "onComputeColors() $wallpaperColors")
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
        }

        private fun wallpaperColorsKey(key: String): String {
            return "$key ${overlayPaint.color} ${colorMatrixArray.contentToString()}"
        }

        private fun wallpaperColorsShouldCalculate(key: String): Boolean {
            return wallpaperColors == null || wallpaperColorsKey(key) != calculateWallpaperColorsLastKey
        }

        private fun computeWallpaperColors() {
            if (System.nanoTime() - calculateWallpaperColorsTime > 1000000000L) {
                // notifyColorsChanged() should only be called every 1 second
                // Log.v(TAG, "computeWallpaperColors() -> Calculating...")
                val bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bm)
                drawOnCanvas(canvas, calculateWallpaperColorsBitmap, calculateWallpaperColorsFile)
                wallpaperColors = WallpaperColors.fromBitmap(bm)
                bm.recycle()
                lastWallpaperColors = wallpaperColors
                calculateWallpaperColorsTime = System.nanoTime()
                calculateWallpaperColorsLastKey = calculateWallpaperColorsKey
                calculateWallpaperColors = false
                calculateWallpaperColorsBitmap = null
                calculateWallpaperColorsKey = null
                calculateWallpaperColorsFile = null
                // Log.v(TAG, "computeWallpaperColors() -> notifyColorsChanged(): $wallpaperColors")
                notifyColorsChanged()
            } else {
                Log.v(TAG, "computeWallpaperColors() deferred")
            }
        }

        private fun loadFile(imageFile: File, desiredWidth: Int, desiredHeight: Int) {
            if (loadFileThread.get()?.isAlive == true) {
                // A thread is already running, no use spawning another one
                return
            }
            object : Thread("loadFile") {
                override fun run() {
                    if (loadFileThreadLock.tryLock(50, TimeUnit.MILLISECONDS)) {
                        try {
                            errorLoadingFile = null
                            var currentBitmap: Bitmap? = null
                            val originalBitmap = loadImageFile(
                                imageFile,
                                requestWidth = maxOf(desiredWidth, width),
                                requestHeight = maxOf(desiredHeight, height)
                            )

                            if (originalBitmap != null) {
                                val (bm, isDesired) = scaleBitmap(
                                    originalBitmap,
                                    width,
                                    height,
                                    desiredWidth,
                                    desiredHeight
                                )
                                shouldScroll = isDesired
                                currentBitmap = bm
                                if (currentBitmap != originalBitmap) {
                                    originalBitmap.recycle()
                                }

                            } else {
                                Log.e(TAG, "Failed to read image from file $imageFile")
                            }
                            val key =
                                "$width $height $desiredWidth $desiredHeight ${imageFile.absolutePath}"
                            if (currentBitmap == null) {
                                bitmaps.remove(key)
                            } else {
                                bitmaps[key] =
                                    SoftReference(ScaledBitmap(currentBitmap, shouldScroll))
                            }

                            currentBitmapFile = imageFile

                            if (currentBitmap == null) {
                                errorLoadingFile = "Failed to load $imageFile"
                                invalid = false
                            }
                            update(currentBitmap)

                        } finally {
                            loadFileThreadLock.unlock()
                        }
                    }
                }
            }.apply {
                start()
                loadFileThread = WeakReference(this)
            }

        }


        /**
         * Update the wallpaper appearance.
         * Reloads all preferences and draws on the canvas
         */
        fun update(customBitmap: Bitmap? = null) {
            imageProvider.get(dayOrNight, isLockScreen) {
                wallpaperImage = it
                updateCanvas(customBitmap)
            }
        }

        /**
         * Draw on the canvas
         */
        fun updateCanvas(customBitmap: Bitmap? = null) {
            overlayPaint.color = wallpaperImage?.color ?: 0

            val imageFile = wallpaperImage?.imageFile

            val (desiredWidth, desiredHeight) = desiredDimensions()
            var currentBitmap: Bitmap? = null
            val key: String
            if (imageFile != null) {
                key =
                    "$width $height $desiredWidth $desiredHeight ${imageFile.absolutePath}"
                currentBitmap = customBitmap
                if (currentBitmap == null) {
                    val scaledBitmap = bitmaps.getOrDefault(key, null)?.get()
                    if (scaledBitmap != null) {
                        currentBitmap = scaledBitmap.bitmap
                        shouldScroll = scaledBitmap.isDesiredSize
                    }
                }

                if (forceReload || (customBitmap == null && (currentBitmap == null || currentBitmap.isRecycled))) {
                    if (forceReload) {
                        errorLoadingFile = null
                        forceReload = false
                    }
                    if (imageFile.exists() && imageFile.canRead()) {
                        if (!((currentBitmapFile?.name?.contains("lock") == true).xor("lock" in imageFile.name))) {
                            // Do not recycle unlocked/locked version of an image
                            currentBitmap?.recycle()
                        }
                        currentBitmap = null
                        if (loadFileThread.get()?.isAlive != true && errorLoadingFile.isNullOrEmpty()) {
                            loadFile(imageFile, desiredWidth, desiredHeight)
                        }
                    } else {
                        Log.e(TAG, "No image file")
                        errorLoadingFile = "\uD83D\uDC81\uD83C\uDFFE\u200D♀ no image file️"
                    }
                }
            } else {
                key = "solidColor"
                shouldScroll = false
            }
            // TODO shouldScroll -> call setOffsetNotificationsEnabled(boolean enabled)
            updateColorMatrix(
                wallpaperImage?.brightness ?: 1f,
                wallpaperImage?.contrast ?: 1f
            )

            // Draw on canvas
            var canvas: Canvas? = null
            var status: WallpaperStatus? = null
            try {
                canvas = surfaceHolder?.lockCanvas()
                if (canvas != null) {
                    status = drawOnCanvas(canvas, currentBitmap, imageFile)
                }
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "lockCanvas(): ${e.stackTraceToString()}")
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder?.unlockCanvasAndPost(canvas)
                    } catch (e: IllegalArgumentException) {
                        Log.e(TAG, "unlockCanvasAndPost(): ${e.stackTraceToString()}")
                    }
                }
            }

            // Calculate the wallpaper colors and notify Android system about new colors
            if (status is WallpaperStatusLoaded
                && status !is WallpaperStatusLoadedBlending
                && wallpaperColorsShouldCalculate(key)
            ) {
                calculateWallpaperColorsBitmap = currentBitmap
                calculateWallpaperColorsKey = wallpaperColorsKey(key)
                calculateWallpaperColorsFile = imageFile
                if (!calculateWallpaperColors) {
                    calculateWallpaperColors = true
                    Handler(Looper.getMainLooper()).postDelayed({
                        computeWallpaperColors()
                    }, 200)
                }
            }

        }

        private fun drawOnCanvas(
            canvas: Canvas,
            bm: Bitmap?,
            imageFile: File?
        ): WallpaperStatus {
            if (imageFile != null && bm != null && !bm.isRecycled) {
                waitAnimation = null
                if (blendBitmaps != null) {
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
                    if (blendBitmaps?.draw(
                            canvas,
                            bm,
                            bitmapPaint,
                            overlayPaint.color,
                            oX,
                            oY,
                            width,
                            height
                        ) != true
                    ) {
                        blendBitmaps = null
                    }
                    Handler(Looper.getMainLooper()).postDelayed({
                        update()
                    }, 50)
                    return WallpaperStatusLoadedBlending()
                } else {
                    // Draw bitmap
                    blendBitmaps = null
                    // Store current offset, in case of blending two bitmaps we will need the last offset
                    if (shouldScroll && !isSecondaryDisplay) {
                        blendFromOffsetXPixel = -offsetX * (bm.width - width)
                        blendFromOffsetYPixel = -offsetY * (bm.height - height)
                    } else {
                        blendFromOffsetXPixel = -0.5f * (bm.width - width)
                        blendFromOffsetYPixel = -0.5f * (bm.height - height)
                    }
                    if (isPreview) {
                        // Preview always set it offsetY=0.0
                        blendFromOffsetYPixel = -0.5f * (bm.height - height)
                    }
                    lastBitmapPaint = Paint(bitmapPaint)

                    if (hasZoom) {
                        canvas.save()
                        canvas.scale(1.0f + 0.05f * zoom, 1.0f + 0.05f * zoom)
                    }

                    canvas.drawBitmap(
                        bm,
                        blendFromOffsetXPixel,
                        blendFromOffsetYPixel,
                        bitmapPaint
                    )
                    if (hasZoom) {
                        canvas.restore()
                        if (zoom == 0f) {
                            hasZoom = false
                        }
                    }

                    //  and color
                    if (overlayPaint.color != 0) {
                        canvas.drawPaint(overlayPaint)
                    }
                    return WallpaperStatusLoadedImage()
                }
            } else if (imageFile == null) {
                // Color only
                overlayPaint.color =
                    overlayPaint.color or 0xFF000000.toInt() // make the color fully opaque
                canvas.drawPaint(overlayPaint)
                return WallpaperStatusLoadedSolid()
            } else {
                // No image or image is loading -> show animation
                waitAnimation = waitAnimation ?: WaitAnimation(
                    width,
                    height,
                    getString(R.string.wallpaper_is_loading)
                )
                waitAnimation?.draw(canvas, errorLoadingFile)
                Handler(Looper.getMainLooper()).postDelayed({
                    update()
                }, 200)
                return WallpaperStatusLoading()
            }
        }

    }
}