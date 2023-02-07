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
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import com.github.cvzi.darkmodewallpaper.Preferences
import com.github.cvzi.darkmodewallpaper.R
import com.github.cvzi.darkmodewallpaper.databinding.ActivityMoreSettingsBinding

/**
 * Advanced/More settings
 */
class MoreSettingsActivity : AppCompatActivity() {
    private lateinit var preferencesGlobal: Preferences
    private lateinit var binding: ActivityMoreSettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferencesGlobal = Preferences(this, R.string.pref_file)

        binding = ActivityMoreSettingsBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.toggleButtonNotifyColorsImmediately.setOnCheckedChangeListener { _, isChecked ->
            preferencesGlobal.notifyColorsImmediatelyAfterUnlock = isChecked
        }
    }

    override fun onResume() {
        super.onResume()

        binding.toggleButtonNotifyColorsImmediately.isChecked =
            preferencesGlobal.notifyColorsImmediatelyAfterUnlock
    }
}