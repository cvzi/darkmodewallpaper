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
import com.github.cvzi.darkmodewallpaper.DAY
import com.github.cvzi.darkmodewallpaper.DarkWallpaperService
import com.github.cvzi.darkmodewallpaper.NIGHT
import com.github.cvzi.darkmodewallpaper.R
import com.google.android.material.switchmaterial.SwitchMaterial

/**
 * Separate settings for lock screen wallpaper
 */
open class LockScreenActivity : MainActivity() {
    init {
        isLockScreenActivity = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dayImageFile =
            imageProvider.storeFileLocation(dayOrNight = DAY, isLockScreen = isLockScreenActivity)
        nightImageFile =
            imageProvider.storeFileLocation(dayOrNight = NIGHT, isLockScreen = isLockScreenActivity)

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

        dayImageFile =
            imageProvider.storeFileLocation(dayOrNight = DAY, isLockScreen = isLockScreenActivity)
        nightImageFile =
            imageProvider.storeFileLocation(dayOrNight = NIGHT, isLockScreen = isLockScreenActivity)

        binding.tableRowButtonLockScreenSettings.removeAllViews()
        binding.tableRowButtonApplyWallpaper.removeAllViews()
        binding.textViewLockScreenDescription.visibility = View.VISIBLE
        binding.cardViewLockScreenSwitch.visibility = View.VISIBLE
        binding.imageViewLockSymbolDay.visibility = View.VISIBLE
        binding.imageViewLockSymbolNight.visibility = View.VISIBLE
        binding.tableRowSwitchZoom.visibility = View.GONE

        binding.switchAnimateFromLockScreen.isChecked =
            preferencesGlobal.animateFromLockScreen
        binding.switchSeparateLockScreen.isChecked =
            preferencesGlobal.separateLockScreen
    }

    override fun onPause() {
        super.onPause()

        Handler(Looper.getMainLooper()).postDelayed({
            binding.layoutRoot.visibility = View.INVISIBLE
        }, 500)
    }

}