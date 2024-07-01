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

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import com.github.cvzi.darkmodewallpaper.DarkWallpaperService

/**
 * Separate settings for lock screen wallpaper
 */
open class LockScreenActivity : MainActivity() {
    init {
        isLockScreenActivity = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.switchSeparateLockScreen.setOnCheckedChangeListener { _, isChecked ->
            preferencesGlobal.separateLockScreen = isChecked
            DarkWallpaperService.lockScreenSettingsChanged()
        }

        binding.switchAnimateFromLockScreen.setOnCheckedChangeListener { _, isChecked ->
            preferencesGlobal.animateFromLockScreen = isChecked
        }
    }

    override fun onResume() {
        super.onResume()

        binding.apply {
            tableRowButtonLockScreenSettings.removeAllViews()
            tableRowButtonApplyWallpaper.removeAllViews()
            textViewLockScreenDescription.visibility = View.VISIBLE
            cardViewLockScreenSwitch.visibility = View.VISIBLE
            cardViewDay.layoutParams = (cardViewDay.layoutParams as ViewGroup.MarginLayoutParams).apply {
                topMargin = 8
            }
            imageViewLockSymbolDay.visibility = View.VISIBLE
            imageViewLockSymbolNight.visibility = View.VISIBLE
            tableRowSwitchZoom.visibility = View.GONE

            switchAnimateFromLockScreen.isChecked =
                preferencesGlobal.animateFromLockScreen
            switchSeparateLockScreen.isChecked =
                preferencesGlobal.separateLockScreen
        }
    }

    override fun onPause() {
        super.onPause()

        Handler(Looper.getMainLooper()).postDelayed({
            binding.layoutRoot.visibility = View.INVISIBLE
        }, 500)
    }

}