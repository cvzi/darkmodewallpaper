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
package com.github.cvzi.darkmodewallpaper.animation

import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.Drawable
import com.github.cvzi.darkmodewallpaper.BitmapOrDrawable
import com.github.cvzi.darkmodewallpaper.scaleDrawableToCanvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation
import androidx.core.graphics.withSave

class BlendImages(
    private val blendFromBitmapOrDrawable: BitmapOrDrawable?,
    private val blendFromColor: Int,
    private val blendFromOffsetXPixel: Float,
    private val blendFromOffsetYPixel: Float,
    blendAlphaStart: Int = 5,
    private val step: Int = 33
) {
    private var blendToBitmap: Bitmap? = null
    private var blendToDrawable: Drawable? = null
    private var overlayPaint = Paint()
    private var blendAlpha: Int = blendAlphaStart

    fun draw(
        canvas: Canvas,
        toBitmapOrDrawable: BitmapOrDrawable? = null,
        newColor: Int,
        offsetX: Float,
        offsetY: Float,
        reverseScroll: Boolean,
        width: Int,
        height: Int
    ): Boolean {
        if (blendToBitmap == null && toBitmapOrDrawable != null) {
            if (toBitmapOrDrawable.bitmap != null) {
                blendToBitmap = toBitmapOrDrawable.bitmap
            } else {
                blendToDrawable = toBitmapOrDrawable.drawable
            }
        }
        var blendFromBitmap = blendFromBitmapOrDrawable?.bitmap
        val blendFromDrawable = blendFromBitmapOrDrawable?.drawable
        if (blendFromBitmap == null && blendFromDrawable == null) {
            blendFromBitmap = createBitmap(1, 1)
        }

        var toBM = blendToBitmap
        val toDrawable = blendToDrawable
        if ((toBM == null || blendFromBitmap == toBM) && (toDrawable == null || blendFromDrawable == toDrawable)) {
            toBM = createBitmap(1, 1)
        }

        // Old Bitmap
        if (blendFromBitmap != null) {
            canvas.drawBitmap(
                blendFromBitmap,
                blendFromOffsetXPixel,
                blendFromOffsetYPixel,
                null
            )
        } else if (blendFromDrawable != null) {
            canvas.withTranslation(blendFromOffsetXPixel, blendFromOffsetYPixel) {
                // Apply offset/Scrolling
                // Enlarge small images
                val (scale, _, _) = scaleDrawableToCanvas(blendFromDrawable, width, height)
                if (scale != 1f) {
                    scale(scale, scale)
                }
                // Draw animation
                blendFromDrawable.draw(this)
            }
        }

        // Old color
        overlayPaint.color = blendFromColor
        if (overlayPaint.color != 0) {
            canvas.drawPaint(overlayPaint)
        }

        // New Bitmap
        val ox = if (reverseScroll) {
            -(1f - offsetX)
        } else {
            -offsetX
        }
        if (toBM != null) {
            val toPaint = Paint().apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
                alpha = blendAlpha
                isAntiAlias = false
            }
            canvas.drawBitmap(
                toBM,
                ox * (toBM.width - width),
                -offsetY * (toBM.height - height),
                toPaint
            )
        } else if (toDrawable != null) {
            toDrawable.alpha = blendAlpha
            toDrawable.setTintBlendMode(BlendMode.SRC_OVER)

            canvas.withSave {
                // Apply offset/Scrolling
                val (scale, imageWidth, imageHeight) = scaleDrawableToCanvas(
                    toDrawable,
                    width,
                    height
                )
                translate(ox * (imageWidth - width), -offsetY * (imageHeight - height))
                // Enlarge small images
                if (scale != 1f) {
                    scale(scale, scale)
                }
                // Draw animation
                toDrawable.draw(this)

            }
        }
        // New color
        overlayPaint.color = newColor
        if (overlayPaint.color != 0) {
            overlayPaint.alpha = if (blendAlpha < overlayPaint.alpha) {
                blendAlpha
            } else {
                overlayPaint.alpha
            }
            canvas.drawPaint(overlayPaint)
        }
        blendAlpha += step
        if (blendAlpha > 0xFF) {
            blendAlpha = 0xFF
            return false
        }

        return true
    }

}