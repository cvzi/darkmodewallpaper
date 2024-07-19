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

import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Point
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.ext.SdkExtensions
import android.provider.MediaStore
import android.text.Html
import android.text.format.DateFormat
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.SeekBar
import android.widget.TextView
import android.widget.TimePicker
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.Companion.isPhotoPickerAvailable
import com.github.cvzi.darkmodewallpaper.databinding.DialogColorBinding
import com.google.android.renderscript.Toolkit
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.lang.ref.SoftReference
import java.time.LocalTime
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

const val UTILSTAG = "Utils.kt"

/**
 * The scale factor for a Drawable to fill a canvas and the resulting width and height
 */
data class DrawableScaleFactor(val scale: Float, val width: Int, val height: Int)

/**
 * Holds a Bitmap and whether the Bitmap fulfils the requested desired size
 */
data class ScaledBitmap(val bitmap: Bitmap, val isDesiredSize: Boolean)

/**
 * Holds the result of storing an image to disk and whether the image is an animation
 */
data class StoreFileResult(val success: Boolean, val isAnimated: Boolean)

/**
 * Save the drawable to image file
 */
fun storeFile(file: File, drawable: Drawable): Boolean {
    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth,
        drawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return storeFile(file, bitmap)
}

/**
 * Save the bitmap to image file
 */
fun storeFile(file: File, bitmap: Bitmap): Boolean {
    var outputStream: FileOutputStream? = null
    try {
        outputStream = FileOutputStream(file)
        bitmap.compress(
            if (file.extension.lowercase(Locale.ROOT) == "webp") {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSLESS
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
            } else {
                Bitmap.CompressFormat.PNG
            }, 100, outputStream
        )
        return true
    } catch (e: IOException) {
        Log.e(UTILSTAG, "$e")
    } finally {
        try {
            outputStream?.close()
        } catch (e: IOException) {
            Log.e(UTILSTAG, e.stackTraceToString())
        }
    }
    return false
}

/**
 * Save the stream content to file.
 * Optionally scale it down. Animated files are never scaled.
 */
fun storeFile(file: File, inputStream: InputStream, maximumSize: Int): StoreFileResult {
    var outputStream: FileOutputStream? = null
    val tmpFile = File(file.parent, "${file.name}.tmp")
    var success = false
    var isAnimated = false
    try {
        outputStream = FileOutputStream(tmpFile)
        inputStream.use { input ->
            outputStream.use { output ->
                val buffer = ByteArray(4 * 1024) // buffer size
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) {
                        break
                    }
                    output.write(buffer, 0, n)
                }
                output.flush()
            }
        }
        success = true
    } catch (e: IOException) {
        Log.e(UTILSTAG, "$e")
    } finally {
        try {
            outputStream?.close()
        } catch (e: IOException) {
            Log.e(UTILSTAG, e.stackTraceToString())
        }
        try {
            inputStream.close()
        } catch (e: IOException) {
            Log.e(UTILSTAG, e.stackTraceToString())
        }
    }

    if (success) {
        isAnimated = isAnimatedImage(tmpFile)
        if (!isAnimated && maximumSize > 0) {
            // Down sample the image
            val bitmap = loadImageFile(tmpFile, maximumSize, maximumSize)
            return if (bitmap != null) {
                storeFile(file, bitmap)
                tmpFile.delete()
                StoreFileResult(success = true, isAnimated = false)
            } else {
                StoreFileResult(success = false, isAnimated = false)
            }
        } else {
            file.delete()
            tmpFile.renameTo(file)
        }
    }
    return StoreFileResult(success = success, isAnimated = isAnimated)
}

/**
 * Check whether ImageDecoder can read a file as an animation or not
 */
fun isAnimatedImage(file: File): Boolean {
    return loadImageFile(file) is AnimatedImageDrawable
}

/**
 * Load an image into a Drawable
 */
fun loadImageFile(imageFile: File): Drawable? {
    return try {
        ImageDecoder.decodeDrawable(ImageDecoder.createSource(imageFile))
    } catch (e: IOException) {
        Log.e(UTILSTAG, "Failed to open image from file $imageFile", e)
        null
    } catch (e: IllegalStateException) {
        Log.e(UTILSTAG, "Failed to decode image from file $imageFile", e)
        null
    }
}


/**
 * Load a downscaled version of file into a bitmap
 */
fun loadImageFile(file: File, requestWidth: Int = 0, requestHeight: Int = 0): Bitmap? {
    return try {
        BitmapFactory.Options().run {
            if (requestWidth > 0 && requestHeight > 0) {
                inJustDecodeBounds = true
                BitmapFactory.decodeFile(file.absolutePath, this)
                inSampleSize = calculateInSampleSize(this, requestWidth, requestHeight)
                inJustDecodeBounds = false
            }
            BitmapFactory.decodeFile(file.absolutePath, this)
        }
    } catch (e: Exception) {
        Log.e(UTILSTAG, e.stackTraceToString())
        null
    }
}

/**
 * Calculate sample size necessary to fulfill requested dimensions
 *  https://developer.android.com/topic/performance/graphics/load-bitmap
 */
fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    // Raw height and width of image
    val (height: Int, width: Int) = options.run { outHeight to outWidth }
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {

        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize
}

/**
 * Let user choose an image file
 * If forceGetContent is true, a file picker will be used even on Android 13 Tiramisu
 */
fun imagePickIntent(context: Context, forceGetContent: Boolean = false): Intent {
    if (!forceGetContent && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && SdkExtensions.getExtensionVersion(
            Build.VERSION_CODES.R
        ) >= 2
    ) {
        val pickImagesIntent = Intent(MediaStore.ACTION_PICK_IMAGES).apply {
            setTypeAndNormalize("image/*")
        }
        val resolvedActivity = pickImagesIntent.resolveActivity(context.packageManager)
        if (resolvedActivity != null && isPhotoPickerAvailable(context)) {
            // Use the new Photo picker
            // https://developer.android.com/about/versions/13/features/photopicker
            return pickImagesIntent
        }
    }

    return Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        setDataAndTypeAndNormalize(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            "image/*"
        )
    }

}

/**
 * Show app chooser with a label for an intent
 * The given label is only shown until Android 11 R / SDK 30
 * For later Android versions there is no label at all.
 */
fun imageChooserIntent(context: Context, label: String): Intent {
    return Intent.createChooser(imagePickIntent(context, forceGetContent = true), label)
}


/**
 * Create color matrix to adjust brightness and contrast
 */
fun createColorMatrix(brightness: Float, contrast: Float): ColorMatrixColorFilter {
    val b = (brightness - contrast) / 2f
    return ColorMatrixColorFilter(
        floatArrayOf(
            contrast, 0f, 0f, 0f, b,
            0f, contrast, 0f, 0f, b,
            0f, 0f, contrast, 0f, b,
            0f, 0f, 0f, 1f, 0f
        )
    )
}


/**
 * Returns new bitmap that it blurred by radius pixels.
 * Uses RenderEffect.createBlurEffect() on Android S+ with
 * hardware acceleration. Otherwise a  modified version
 * of Renderscript.Toolkit.blur() is used.
 */
fun blur(mSrc: Bitmap, radius: Float): Bitmap {
    var r = max(1f, radius)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val canvas = Canvas()
        if (canvas.isHardwareAccelerated) {
            var src = mSrc
            if (r > 25f) {
                src = blur(mSrc, r - 25f)
                r = 25f
            }
            Log.d(UTILSTAG, "Using RenderEffect.createBlurEffect($r, $r)")
            val result =
                Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
            canvas.setBitmap(result)
            val bitmapEffect = RenderEffect.createBitmapEffect(src)
            val blurEffect =
                RenderEffect.createBlurEffect(r, r, bitmapEffect, Shader.TileMode.MIRROR)
            val renderNode = RenderNode(null)
            renderNode.setRenderEffect(blurEffect)
            try {
                canvas.drawRenderNode(renderNode)
                return result
            } catch (e: IllegalArgumentException) {
                Log.d(UTILSTAG, "RenderEffect failed:", e)
            }
        }
    }
    Log.v(UTILSTAG, "Using renderscript.Toolkit.blur($r)")
    return Toolkit.blurMulti(mSrc, radius.toInt())
}


/**
 * Scale a bitmap so it fits destWidth X destHeight or desiredMinWidth X desiredMinHeight, choose
 * by comparing the image ratio to the given dimensions.
 * Adjust contrast and brightness if provided:
 * drawBitmap() with a ColorMatrix is slow, therefore it is done once before scaling the
 * bitmap instead of every time the bitmap is drawn.
 */
fun scaleAndAdjustBitmap(
    src: Bitmap,
    destWidth: Int,
    destHeight: Int,
    desiredMinWidth: Int,
    desiredMinHeight: Int,
    changeBrightness: Float?,
    changeContrast: Float?,
    blur: Float?
): ScaledBitmap {
    val brightness = changeBrightness ?: 0f
    val contrast = changeContrast ?: 1f
    val adjustedBitmap = if (abs(brightness) > 3f || abs(contrast - 1f) > 0.01f) {
        val paint = Paint().apply {
            colorFilter = createColorMatrix(brightness, contrast)
        }
        Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888).apply {
            val canvas = Canvas(this)
            canvas.drawBitmap(src, 0f, 0f, paint)
        }
    } else src
    var scaled =
        scaleBitmap(adjustedBitmap, destWidth, destHeight, desiredMinWidth, desiredMinHeight)
    if (blur != null && blur > 1.0f) {
        scaled = ScaledBitmap(blur(scaled.bitmap, blur), scaled.isDesiredSize)
    }
    return scaled
}

/**
 * Scale a bitmap so it fits destWidth X destHeight or desiredMinWidth X desiredMinHeight, choose
 * by comparing the image ratio to the given dimensions.
 */
fun scaleBitmap(
    src: Bitmap,
    destWidth: Int,
    destHeight: Int,
    desiredMinWidth: Int,
    desiredMinHeight: Int
): ScaledBitmap {
    Log.d(
        UTILSTAG,
        "scaleBitmap() From ${src.width}x${src.height} -> ${destWidth}x$destHeight or ${desiredMinWidth}x$desiredMinHeight"
    )
    if (src.width <= 0 && src.height <= 0) {
        return ScaledBitmap(src, false)
    }

    if (src.width == desiredMinWidth && src.height == desiredMinHeight) {
        return ScaledBitmap(src, true)
    }
    if (src.width == destWidth && src.height == destHeight) {
        return ScaledBitmap(src, false)
    }

    // Check which ratio is closer to image ratio
    val bitmapRatio = src.width.toFloat() / src.height
    val destRatio = destWidth.toFloat() / destHeight
    val desiredRatio = desiredMinWidth.toFloat() / desiredMinHeight
    val boundingWidth: Int
    val boundingHeight: Int

    var isDesired: Boolean


    Log.d(
        UTILSTAG,
        "scaleBitmap() abs(bitmapRatio - destRatio) > abs(bitmapRatio - desiredRatio) = abs($bitmapRatio - $destRatio) > abs($bitmapRatio - $desiredRatio)) = ${
            abs(bitmapRatio - destRatio)
        } > ${abs(bitmapRatio - desiredRatio)}"
    )
    if (abs(bitmapRatio - destRatio) >= abs(bitmapRatio - desiredRatio)) {
        isDesired = true
        boundingWidth = desiredMinWidth
        boundingHeight = desiredMinHeight
        Log.d(UTILSTAG, "scaleBitmap() Choosing desired width $desiredMinWidth x $desiredMinHeight")
    } else {
        isDesired = false
        boundingWidth = destWidth
        boundingHeight = destHeight
        Log.d(UTILSTAG, "scaleBitmap() Choosing destination width $destWidth x $destHeight")
    }

    val widthScale = boundingWidth.toFloat() / src.width
    val heightScale = boundingHeight.toFloat() / src.height

    var scale = max(widthScale, heightScale)

    Log.d(
        UTILSTAG,
        "scaleBitmap() New dim ${src.width}x${src.height} -> ${ceil(src.width * scale).toInt()} x ${
            ceil(
                src.height * scale
            ).toInt()
        }"
    )

    var newBm = Bitmap.createScaledBitmap(
        src,
        max(1, ceil(src.width * scale).toInt()),
        max(1, ceil(src.height * scale).toInt()),
        true
    )
    Log.d(
        UTILSTAG,
        "scaleBitmap() created newBm = ${newBm.width}x${newBm.height}@${newBm.byteCount}"
    )

    if (newBm.byteCount > MAX_BITMAP_BYTES) {
        Log.d(
            UTILSTAG,
            "scaleBitmap() Bitmap (isDesired=$isDesired) is too large"
        )

        if (isDesired && (destWidth < desiredMinWidth || destHeight < desiredMinHeight)) {
            isDesired = false
            Log.d(
                UTILSTAG,
                "scaleBitmap() Downsizing to destination dim: $destWidth x $destHeight"
            )
            newBm.recycle()
            newBm = scaleBitmap(
                src,
                destWidth,
                destHeight,
                destWidth,
                destHeight
            ).bitmap
        } else {
            isDesired = false
            while (newBm.byteCount > MAX_BITMAP_BYTES) {
                newBm.recycle()
                Log.d(
                    UTILSTAG,
                    "scaleBitmap() Downsizing $scale -> ${scale * 2f / 3f}"
                )
                scale *= 2f / 3f
                Log.d(
                    UTILSTAG,
                    "scaleBitmap() New size: ${ceil(src.width * scale).toInt()} x ${
                        ceil(
                            src.height * scale
                        ).toInt()
                    }"
                )
                newBm = Bitmap.createScaledBitmap(
                    src,
                    max(1, ceil(src.width * scale).toInt()),
                    max(1, ceil(src.height * scale).toInt()),
                    true
                )
            }
        }

    }
    return ScaledBitmap(
        newBm, isDesired
    )
}

/**
 * Real screen size of the device, roughly corresponds to lock screen wallpaper size
 */
fun Activity.getScreenSize(): Point {
    val screenSize = Point(0, 0)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && display != null) {
        windowManager.maximumWindowMetrics.bounds.run {
            screenSize.x = width()
            screenSize.y = height()
        }
    } else {
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealSize(screenSize)
    }
    return screenSize
}

/**
 * Capture event when a seek bar is moving
 */
class OnSeekBarProgress(val onProgress: (progress: Int) -> Unit) : SeekBar.OnSeekBarChangeListener {
    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        onProgress(progress)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {}

    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
}

/**
 * Make an activity go full screen
 */
fun Activity.enableFullScreen() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            @Suppress("DEPRECATION")
            window.setDecorFitsSystemWindows(false)
        }
        window.insetsController?.let {
            it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    } else {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }
}

/**
 * Revert Activity.enableFullScreen()
 */
fun Activity.disableFullScreen() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            @Suppress("DEPRECATION")
            window.setDecorFitsSystemWindows(true)
        }
        window.insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        window.insetsController?.systemBarsBehavior =
            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    } else {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }
}

/**
 * Call dismiss() on a Dialog and catch the IllegalArgumentException that is thrown if the context
 * of the dialog was already destroyed
 */
fun DialogInterface.safeDismiss() {
    try {
        this.dismiss()
    } catch (e: IllegalArgumentException) {
        // no-op
    }
}

fun createTimePicker(
    context: Context,
    load: (() -> String),
    save: ((String) -> Unit)
): TimePickerDialog {
    val listener = { _: TimePicker, hour: Int, minute: Int ->
        save("%02d:%02d".format(hour, minute))
    }
    val parts = load().split(":")
    val (hour, minute) = if (parts.size < 2) {
        // default to current time
        Calendar.getInstance().run {
            Pair(get(Calendar.HOUR_OF_DAY), get(Calendar.MINUTE))
        }
    } else {
        Pair(parts[0].toInt(), parts[1].toInt())
    }
    val is24HourView = DateFormat.is24HourFormat(context)
    return TimePickerDialog(context, listener, hour, minute, is24HourView)
}

fun timeIsInTimeRange(timeRangeStr: String, now: LocalTime = LocalTime.now()): Boolean {
    val (startStr, endStr) = timeRangeStr.split("-")
    val startTime = LocalTime.parse(startStr)
    val endTime = LocalTime.parse(endStr)
    return if (startTime.isAfter(endTime)) {
        !(now.isAfter(endTime) && now.isBefore(startTime))
    } else {
        now.isAfter(startTime) && now.isBefore(endTime)
    }
}

/**
 * Open the wallpaper preview to set a live wallpaper
 */
fun applyLiveWallpaper(activity: Activity, c: Int, onError: () -> Unit) {
    Preferences(activity, R.string.pref_file).previewMode = c
    Intent(
        WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER
    ).apply {
        putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(activity, DarkWallpaperService::class.java)
        )
        if (resolveActivity(activity.packageManager) != null) {
            activity.startActivity(this)
        } else {
            onError()
        }
    }
}

/**
 * Decide whether scrolling should be enabled
 */
fun shouldScrollingBeEnabled(
    isDesiredSize: Boolean,
    scrollingMode: ScrollingMode? = null
): Boolean {
    return when (scrollingMode) {
        ScrollingMode.ON, ScrollingMode.REVERSE -> true
        ScrollingMode.OFF -> false
        else -> isDesiredSize
    }
}

/**
 * Turn Point into string "XxY"
 */
fun Point.toSizeString() = "${x}x${y}"

/**
 * Turn WallpaperColors to readable text
 */
fun Color?.toPrettyString() = this?.let {
    String.format("#%06X", 0xFFFFFF and toArgb())
} ?: ""

/**
 * Turn WallpaperColors to readable text
 */
fun WallpaperColors.toPrettyString() =
    "${primaryColor.toPrettyString()} ${secondaryColor.toPrettyString()} ${tertiaryColor.toPrettyString()} ${
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            "\n" + prettyColorHints()
        } else ""
    }"

/**
 * Turn WallpaperColors.colorHints to readable text
 */
fun WallpaperColors.prettyColorHints(): String {
    var s = "Dark text supported: "
    s += when (supportsDarkText) {
        true -> "yes"
        false -> "no"
        else -> "undefined"
    }
    s += "\nDark theme supported: "
    s += when (supportsDarkTheme) {
        true -> "yes"
        false -> "no"
        else -> "undefined"
    }
    return s
}

/**
 * Check if flag HINT_SUPPORTS_DARK_TEXT is set
 */
val WallpaperColors.supportsDarkText: Boolean?
    get() {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT != 0
        } else {
            null
        }
    }

/**
 * Check if flag HINT_SUPPORTS_DARK_THEME is set
 */
val WallpaperColors.supportsDarkTheme: Boolean?
    get() {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            colorHints and WallpaperColors.HINT_SUPPORTS_DARK_THEME != 0
        } else {
            null
        }
    }


/**
 * Keeps a reference to the bitmap or drawable to later generate WallpaperColors
 */
class WallpaperColorsHelper(
    var bitmapOrDrawable: BitmapOrDrawable,
    val key: String?,
    val file: File?,
    val customWallpaperColors: WallpaperColors?
) {
    private var isRecycled = false

    /**
     * Returns true the first time it is called and then false
     */
    fun use(): Boolean {
        val t = isRecycled
        isRecycled = true
        return !t
    }

    /**
     * Sets reference to bitmap and drawable to null
     */
    fun recycle() = bitmapOrDrawable.recycle()
}

/**
 * Holds a Bitmap or a Drawable or both null
 */
class BitmapOrDrawable(var bitmap: Bitmap? = null, var drawable: Drawable? = null) {
    fun isNull(): Boolean {
        return bitmap == null && drawable == null
    }

    fun isValid(): Boolean {
        return (drawable != null) || (bitmap != null && bitmap?.isRecycled == false)
    }

    fun recycle() {
        bitmap?.recycle()
        bitmap = null
        drawable = null
    }

    fun set(b: Bitmap?, d: Drawable?) {
        bitmap = b
        drawable = d
    }

    fun set(bitmapOrDrawable: BitmapOrDrawable?) {
        bitmap = bitmapOrDrawable?.bitmap
        drawable = bitmapOrDrawable?.drawable
    }

    fun set(scaledBitmapOrDrawable: ScaledBitmapOrDrawable?) {
        bitmap = scaledBitmapOrDrawable?.scaledBitmap?.bitmap
        drawable = scaledBitmapOrDrawable?.drawable
    }

    override fun toString(): String {
        return "${super.toString()}[${bitmap ?: drawable ?: "null,null"}]"
    }
}

/**
 * Holds a ScaledBitmap or a Drawable or both null
 */
class ScaledBitmapOrDrawable(
    var scaledBitmap: ScaledBitmap? = null,
    var drawable: Drawable? = null
) {
    var isDesiredSize: Boolean = false
        get() = scaledBitmap?.isDesiredSize ?: field

    fun isValid(): Boolean {
        return (drawable != null) || (scaledBitmap?.bitmap != null && scaledBitmap?.bitmap?.isRecycled == false)
    }

    fun getBitmapOrDrawable() = BitmapOrDrawable(scaledBitmap?.bitmap, drawable)

    override fun toString(): String {
        return "${super.toString()}[${scaledBitmap ?: drawable ?: "null,null"}]"
    }

    fun allocationBytes(): Long = scaledBitmap?.bitmap?.allocationByteCount?.toLong()
        ?: ((drawable as? BitmapDrawable)?.bitmap?.allocationByteCount?.toLong() ?: 0L)

}

/**
 * Get a scale factor to scale the Drawable to fill the whole canvas
 * Can be used with canvas.scale(factor,factor).
 * returns the scale factor and the size of the image if the factor was applied
 */
fun scaleDrawableToCanvas(
    drawable: Drawable,
    canvasWidth: Int,
    canvasHeight: Int
): DrawableScaleFactor {
    var scale = 1f
    var drawableWidth = drawable.intrinsicWidth
    var drawableHeight = drawable.intrinsicHeight

    if (drawableWidth < canvasWidth || drawableHeight < canvasHeight || (drawableWidth > canvasWidth && drawableHeight > canvasHeight)) {
        scale = max(
            canvasWidth.toFloat() / drawable.intrinsicWidth,
            canvasHeight.toFloat() / drawable.intrinsicHeight
        )
        drawableHeight = (scale * drawableHeight).toInt()
        drawableWidth = (scale * drawableWidth).toInt()
    }

    return DrawableScaleFactor(scale, drawableWidth, drawableHeight)
}


/**
 * Generate the usual checkered background to highlight transparency
 */
fun checkeredBackground(): Bitmap {
    return Bitmap.createBitmap(19, 19, Bitmap.Config.ARGB_8888).apply {
        Canvas(this).apply {
            val dark = Paint().apply {
                color = 0xffCDCDCD.toInt()
            }
            val light = Paint().apply {
                color = 0xffF1F1F1.toInt()
            }
            drawRect(0f, 0f, 19f, 19f, dark)
            drawRect(9f, 0f, 19f, 9f, light)
            drawRect(0f, 9f, 9f, 19f, light)
        }
    }
}

fun Activity.colorChooserDialog(
    title: String,
    getColor: (() -> Int),
    storeColor: ((color: Int) -> Unit)
) {
    val dialogBinding = DialogColorBinding.inflate(layoutInflater)

    val builder = AlertDialog.Builder(this)
    builder.setTitle(title)
    builder.setView(dialogBinding.root)
    builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
        val color = dialogBinding.colorPicker.color
        storeColor(color)
        dialog.safeDismiss()
    }
    builder.setNegativeButton(android.R.string.cancel, null)
    builder.show()
    dialogBinding.colorPicker.apply {
        color = getColor()
        showAlpha(true)
        showHex(true)
    }
}

fun Activity.colorChooserDialog(
    title: StringRes,
    getColor: (() -> Int),
    storeColor: ((color: Int) -> Unit)
) = colorChooserDialog(getString(title), getColor, storeColor)

class WallpaperColorsEditor(wallpaperColors: WallpaperColors?) {
    private var primaryColor: Color = wallpaperColors?.primaryColor ?: Color.valueOf(Color.WHITE)
    private var secondaryColor = wallpaperColors?.secondaryColor
    private var tertiaryColor = wallpaperColors?.tertiaryColor
    private var darkText = wallpaperColors?.supportsDarkText
    private var darkTheme = wallpaperColors?.supportsDarkTheme
    fun setPrimaryColor(color: Int): WallpaperColorsEditor {
        primaryColor = Color.valueOf(color)
        return this
    }

    fun setSecondaryColor(color: Int): WallpaperColorsEditor {
        secondaryColor = Color.valueOf(color)
        return this
    }

    fun setTertiaryColor(color: Int): WallpaperColorsEditor {
        tertiaryColor = Color.valueOf(color)
        return this
    }

    fun setDarkText(enabled: Boolean): WallpaperColorsEditor {
        darkText = enabled
        return this
    }

    fun setDarkTheme(enabled: Boolean): WallpaperColorsEditor {
        darkTheme = enabled
        return this
    }

    /**
     * Creates new WallpaperColors, missing values are filled with default values
     */
    fun build(): WallpaperColors {
        val second: Color?
        val tertiary: Color?
        if (secondaryColor == null && tertiaryColor != null) {
            // tertiaryColor can't be specified when secondaryColor is null -> swap them
            second = tertiaryColor
            tertiary = null
        } else {
            second = secondaryColor
            tertiary = tertiaryColor
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            var colorHints = 0
            if (darkText == true) {
                colorHints = colorHints or WallpaperColors.HINT_SUPPORTS_DARK_TEXT
            }
            if (darkTheme == true) {
                colorHints = colorHints or WallpaperColors.HINT_SUPPORTS_DARK_THEME
            }
            WallpaperColors(primaryColor, second, tertiary, colorHints)
        } else {
            WallpaperColors(primaryColor, second, tertiary)
        }

    }

}

/**
 * Return a WallpaperColorsEditor to edit the colors
 * Use .build() to get a new WallpaperColors object
 */
fun WallpaperColors?.edit() = WallpaperColorsEditor(this)

fun Context.setHtmlText(
    textView: TextView,
    stringRes: StringRes,
    vararg formatArgs: Any?
): TextView {
    return setHtmlText(textView, getString(stringRes, *formatArgs))
}

fun setHtmlText(textView: TextView, htmlString: String): TextView {
    return textView.apply {
        movementMethod = LinkMovementMethod()
        text = Html.fromHtml(
            htmlString,
            Html.FROM_HTML_SEPARATOR_LINE_BREAK_DIV
        )
    }
}

class ImageCache {
    val size: Int
        get() = softImageCache.size + hardImageCache.size
    private var useSoftReferences = true


    fun clear() {
        softImageCache.clear()
        hardImageCache.clear()
    }

    private val softImageCache: ConcurrentHashMap<String, SoftReference<ScaledBitmapOrDrawable>?> =
        ConcurrentHashMap()

    private val hardImageCache: ConcurrentHashMap<String, ScaledBitmapOrDrawable?> =
        ConcurrentHashMap()

    fun getOrDefault(
        key: String,
        default: ScaledBitmapOrDrawable? = null
    ): ScaledBitmapOrDrawable? =
        hardImageCache.getOrElse(key) {
            return softImageCache.getOrDefault(key, null)?.get()
        }

    fun store(key: String, value: ScaledBitmapOrDrawable?) {
        if (useSoftReferences) {
            softImageCache[key] = SoftReference(value)
        } else {
            hardImageCache[key] = value
        }
    }

    fun enableSoftReferences(useSoftReferences: Boolean) {
        this.useSoftReferences = useSoftReferences
        if (useSoftReferences) {
            hardImageCache.clear()
        } else {
            softImageCache.forEach { (key, softRef) ->
                softRef?.get()?.let {
                    hardImageCache[key] = it
                }
            }
        }
    }

    fun remove(key: String) {
        softImageCache.remove(key)
        hardImageCache.remove(key)
    }

    fun memorySize(): String {
        val imageCache: HashSet<ScaledBitmapOrDrawable> = HashSet()

        softImageCache.values.forEach { value ->
            value?.get()?.let { scaledBitmapOrDrawable ->
                imageCache.add(scaledBitmapOrDrawable)
            }
        }
        hardImageCache.values.forEach { value ->
            value?.let {
                imageCache.add(it)
            }
        }
        val size: Long = imageCache.sumOf {
            it.allocationBytes()
        }

        return "Currently ${imageCache.size} images with a total of ${size / (1024 * 1024)} MB"
    }

}
