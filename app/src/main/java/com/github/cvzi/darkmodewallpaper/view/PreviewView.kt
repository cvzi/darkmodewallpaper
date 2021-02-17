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
package com.github.cvzi.darkmodewallpaper.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.github.cvzi.darkmodewallpaper.loadImageFile
import com.github.cvzi.darkmodewallpaper.scaleBitmap
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


class PreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    companion object {
        private const val TAG = "PreviewView.kt"
        private val lock = ReentrantLock(false)
        private val bitmaps: ConcurrentHashMap<String, WeakReference<Bitmap>?> = ConcurrentHashMap()
    }

    private val overlayPaint = Paint()
    private val textPaint = Paint()
    private val bitmapPaint = Paint()
    private val colorMatrixArray = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )
    private val clippingPaint = Paint()
    private var bitmap: Bitmap? = null
    private var w = 0
    private var h = 0
    private var errorLoadingFile: String? = null
    var scaledScreenWidth: Int? = 0
    var scaledScreenHeight: Int? = 0
    private var shouldScroll = true

    init {
        overlayPaint.style = Paint.Style.FILL
        textPaint.isAntiAlias = true
        textPaint.color = Color.BLACK
        clippingPaint.color = 0x88777777.toInt()
        clippingPaint.strokeWidth = 3f
        clippingPaint.maskFilter
        clippingPaint.blendMode = BlendMode.PLUS
    }

    var color = 0x78FF0000
        set(value) {
            field = value
            invalidate()
        }

    var contrast = 1f
        set(value) {
            field = value
            updateColorMatrix()
            invalidate()
        }

    var brightness = 1f
        set(value) {
            field = value
            updateColorMatrix()
            invalidate()
        }

    var file: File? = null
        set(value) {
            field?.let {
                for (i in bitmaps) {
                    if (i.key.endsWith(it.absolutePath)) {
                        bitmaps.remove(i.key)
                    }
                }
                bitmap = null
            }
            field = value
            errorLoadingFile = null
            loadFile()
        }

    private fun updateColorMatrix() {
        /*  r  g  b  a  _
            c, 0, 0, 0, b,
            0, c, 0, 0, b,
            0, 0, c, 0, b,
            0, 0, 0, 1, 0

         */
        val b = (brightness - contrast) / 2f
        colorMatrixArray[0] = contrast
        colorMatrixArray[4] = b
        colorMatrixArray[6] = contrast
        colorMatrixArray[9] = b
        colorMatrixArray[12] = contrast
        colorMatrixArray[14] = b
        bitmapPaint.colorFilter = ColorMatrixColorFilter(colorMatrixArray)
    }

    private fun loadFile() {
        if (w <= 0 || h <= 0) {
            return
        }
        object : Thread("PreviewView.loadFile") {
            override fun run() {
                lock.withLock {
                    val fileToLoad = file
                    if (fileToLoad == null) {
                        errorLoadingFile = null
                        invalidate()
                        return
                    }
                    val key = "$w $h ${fileToLoad.absolutePath}"
                    bitmap = bitmaps.getOrDefault(key, null)?.get()
                    if (bitmap != null) {
                        invalidate()
                        return
                    }

                    bitmap = if (fileToLoad.exists() && fileToLoad.canRead() && w > 0 && h > 0) {
                        Log.v(TAG, "Loading bitmap from $file")
                        val original = loadImageFile(fileToLoad, w, h)
                        if (original != null) {
                            val (bm, isDesired) = scaleBitmap(
                                original,
                                scaledScreenWidth ?: width,
                                scaledScreenHeight ?: height,
                                width,
                                height
                            )
                            shouldScroll = isDesired
                            bm
                        } else {
                            errorLoadingFile = "Failed to load file"
                            Log.e(TAG, "Failed to load file $file")
                            null
                        }
                    } else if (w > 0 && h > 0) {
                        if (fileToLoad.exists()) {
                            errorLoadingFile = "Failed to read file"
                            Log.e(TAG, "Failed to read file $file")
                        } else {
                            errorLoadingFile = "No image set"
                        }
                        null
                    } else {
                        null
                    }

                    if (bitmap != null) {
                        bitmaps[key] = WeakReference(bitmap)
                    }
                    invalidate()
                }
            }
        }.apply {
            start()
        }
    }

    override fun onSizeChanged(width: Int, height: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(width, height, oldw, oldh)
        w = width
        h = height
        bitmap = null
    }

    override fun onDraw(canvas: Canvas?) {
        overlayPaint.color = color

        canvas?.apply {
            if (file == null) {
                bitmap = null
            } else if (bitmap == null && errorLoadingFile == null) {
                w = width
                h = height
                loadFile()
                drawText(
                    "Loading",
                    5f,
                    height / 2f,
                    textPaint.apply {
                        textSize = width / 10f
                    })
            } else if (errorLoadingFile != null) {
                drawText(
                    errorLoadingFile ?: "error",
                    5f,
                    height / 2f,
                    textPaint.apply {
                        textSize = width / 10f
                    })
            }
            bitmap?.let {
                canvas.drawBitmap(
                    it,
                    -0.5f * (it.width - width),
                    -0.5f * (it.height - height),
                    bitmapPaint
                )

            }
            if (bitmap == null && file == null) {
                overlayPaint.color = overlayPaint.color or 0xFF000000.toInt()
            }
            drawPaint(overlayPaint)

            // Draw screen boundaries
            if (!shouldScroll) {
                val screenWidth = scaledScreenWidth?.toFloat()
                val screenHeight = scaledScreenHeight?.toFloat()
                if (screenWidth != null && screenHeight != null && screenWidth > 0 && screenHeight > 0) {
                    canvas.drawLine(
                        width / 2f - screenWidth / 2f,
                        height / 2f - screenHeight / 2f,
                        width / 2f - screenWidth / 2f,
                        height / 2f + screenHeight / 2f,
                        clippingPaint
                    )
                    canvas.drawLine(
                        width / 2f + screenWidth / 2f,
                        height / 2f - screenHeight / 2f,
                        width / 2f + screenWidth / 2f,
                        height / 2f + screenHeight / 2f,
                        clippingPaint
                    )
                }
            }

        }
    }

}