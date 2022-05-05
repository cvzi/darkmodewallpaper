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

import android.graphics.*

class BlendBitmaps(
    private val blendFromBitmap: Bitmap,
    private val blendFromColor: Int,
    private val blendFromOffsetXPixel: Float,
    private val blendFromOffsetYPixel: Float,
    blendAlphaStart: Int = 5,
    private val step: Int = 33
) {
    private var blendToBitmap: Bitmap? = null
    private var overlayPaint = Paint()
    private var blendAlpha: Int = blendAlphaStart

    fun draw(
        canvas: Canvas,
        toBitmap: Bitmap? = null,
        newColor: Int,
        offsetX: Float,
        offsetY: Float,
        width: Int,
        height: Int
    ): Boolean {
        if (blendToBitmap == null) {
            blendToBitmap = toBitmap
        }

        val toBM = blendToBitmap
        if (toBM == null || blendFromBitmap == toBM) {
            return false
        }

        // Old Bitmap
        canvas.drawBitmap(
            blendFromBitmap,
            blendFromOffsetXPixel,
            blendFromOffsetYPixel,
            null
        )

        // Old color
        overlayPaint.color = blendFromColor
        if (overlayPaint.color != 0) {
            canvas.drawPaint(overlayPaint)
        }

        // New Bitmap
        val toPaint = Paint()
        toPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        toPaint.alpha = blendAlpha
        toPaint.isAntiAlias = false
        canvas.drawBitmap(
            toBM,
            -offsetX * (toBM.width - width),
            -offsetY * (toBM.height - height),
            toPaint
        )
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