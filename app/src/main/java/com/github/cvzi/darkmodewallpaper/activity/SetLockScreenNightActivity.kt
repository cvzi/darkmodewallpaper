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

import com.github.cvzi.darkmodewallpaper.DayOrNight
import com.github.cvzi.darkmodewallpaper.NIGHT

/**
 * Activity to set night lock-screen wallpaper through "send to"/"share"/"use as" from other apps
 */
class SetLockScreenNightActivity : LockScreenActivity() {
    override fun sendToActionIsNight(): DayOrNight {
        imageProvider.setUseNightWallpaper(isLockScreenActivity, true)
        imageProvider.setUseColorOnly(NIGHT, isLockScreenActivity, false)
        return NIGHT
    }

}