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
import com.github.cvzi.darkmodewallpaper.*
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

        dayImageFile = dayFileLocation(isLockScreenActivity)
        nightImageFile = nightFileLocation(isLockScreenActivity)

        findViewById<SwitchMaterial>(R.id.switchSeparateLockScreen).setOnCheckedChangeListener { _, isChecked ->
            setSeparateLockScreenEnabled(isChecked)
            DarkWallpaperService.lockScreenSettingsChanged()
        }

        findViewById<SwitchMaterial>(R.id.switchAnimateFromLockScreen).setOnCheckedChangeListener { _, isChecked ->
            setAnimateFromLockScreen(isChecked)
        }
    }

    override fun onResume() {
        super.onResume()

        dayImageFile = dayFileLocation(isLockScreenActivity)
        nightImageFile = nightFileLocation(isLockScreenActivity)

        findViewById<ViewGroup>(R.id.tableRowButtonLockScreenSettings).removeAllViews()
        findViewById<ViewGroup>(R.id.tableRowButtonApplyWallpaper).removeAllViews()
        findViewById<View>(R.id.textViewLockScreenDescription).visibility = View.VISIBLE
        findViewById<View>(R.id.cardViewLockScreenSwitch).visibility = View.VISIBLE
        findViewById<View>(R.id.imageViewLockSymbolDay).visibility = View.VISIBLE
        findViewById<View>(R.id.imageViewLockSymbolNight).visibility = View.VISIBLE

        findViewById<SwitchMaterial>(R.id.switchAnimateFromLockScreen).isChecked =
            isAnimateFromLockScreen()
        findViewById<SwitchMaterial>(R.id.switchSeparateLockScreen).isChecked =
            isSeparateLockScreenEnabled()
    }

    override fun onPause() {
        super.onPause()

        Handler(Looper.getMainLooper()).postDelayed({
            findViewById<View>(R.id.layoutRoot).visibility = View.INVISIBLE
        }, 200)
    }

}