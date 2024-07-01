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

const val MAX_BITMAP_BYTES = 100 * 1024 * 1024

const val DONATE_HTML =
    "&#x1f49c; <a href=\"https://cvzi.github.io/.github/\">Donate &amp; Support</a>"

const val DAY = false
const val NIGHT = true

typealias DayOrNight = Boolean
typealias StringRes = Int

open class WallpaperStatus
class WallpaperStatusLoading : WallpaperStatus()
open class WallpaperStatusLoaded : WallpaperStatus()
class WallpaperStatusLoadedImage : WallpaperStatusLoaded()
class WallpaperStatusLoadedSolid : WallpaperStatusLoaded()
class WallpaperStatusLoadedBlending : WallpaperStatusLoaded()

enum class NightModeTrigger {
    SYSTEM,
    TIMERANGE;

    companion object {
        fun valueOfOrFirst(value: String?) = value?.run {
            try {
                valueOf(value)
            } catch (e: IllegalArgumentException) {
                null
            }
        } ?: entries[0]
    }
}

enum class ScrollingMode {
    AUTOMATIC, ON, OFF, REVERSE
}