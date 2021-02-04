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

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class WaitAnimation(val width: Int, val height: Int, private val loadingMessage: String) {
    companion object {
        private val faces = arrayOf(
            "\uD83E\uDD37\uD83C\uDFFE\u200D\u2640",
            "\uD83D\uDE45\uD83C\uDFFE\u200D\u2640",
            "\uD83D\uDE46\uD83C\uDFFE\u200D\u2640",
            "\uD83D\uDC81\uD83C\uDFFE\u200D\u2640"
        )
    }

    private var animationColor = 0x0000
    private val animationColorEnd = 0x202020
    private var animationColorDir = 1
    private var animationIndex = 0
    private var textPositionX = width / 4f
    private var textPositionY = height / 2f
    private var paint = Paint().apply {
        color = Color.BLACK
        textSize = width / 12f
        isAntiAlias = true
    }


    fun draw(canvas: Canvas, errorStr: String?) {
        canvas.drawPaint(Paint().apply {
            style = Paint.Style.FILL
            color = 0xFF101010.toInt() + animationColor
            animationColor += 0x020202 * animationColorDir
            if (animationColor >= animationColorEnd) {
                animationColor = animationColorEnd
                animationColorDir = -animationColorDir
            } else if (animationColor <= 0) {
                animationColor = 0
                animationColorDir = -animationColorDir
            }
        })
        val text = if (errorStr.isNullOrEmpty()) {
            animationIndex = (animationIndex + 1) % (4 * faces.size)
            "${faces[animationIndex / 4]} $loadingMessage"
        } else {
            errorStr
        }
        canvas.drawText(
            text,
            textPositionX,
            textPositionY,
            paint
        )
    }
}

