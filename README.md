# <img src="https://raw.githubusercontent.com/cvzi/darkmodewallpaper/main/app/src/main/ic_launcher-playstore.png" alt="Launcher icon" height="48"> DarkModeLiveWallpaper
[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3%20or%20later-a32d2a?style=for-the-badge&logo=GNU)](https://www.gnu.org/licenses/gpl-3.0)
[![F-Droid](https://img.shields.io/f-droid/v/com.github.cvzi.darkmodewallpaper.svg?style=for-the-badge&logo=f-droid)](https://f-droid.org/packages/com.github.cvzi.darkmodewallpaper/)
[![Download APK file](https://img.shields.io/github/release/cvzi/darkmodewallpaper.svg?style=for-the-badge&label=Download%20apk&logo=android&color=3d8)](https://github.com/cvzi/darkmodewallpaper/releases/latest)

A live wallpaper for Android that respects dark theme mode

[Changelog](CHANGELOG.md) • [View older releases](https://keybase.pub/cuzi/DarkModeWallpaper_bin/)

The app automatically changes the wallpaper when the dark theme is enabled or disabled.
You can set a different image for dark theme or change the color/contrast/brightness of
your image.

[<img src="https://raw.githubusercontent.com/cvzi/ScreenshotTile/master/docs/imgs/get-it-on-f-droid.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/packages/com.github.cvzi.darkmodewallpaper/) 

This app requires Android 10 or a later version. If you are using an earlier version of Android, you may be able to
achieve a comparable experience by using MacroDroid, Tasker or a similar automation tool.

## Contributors

[![Contributors](https://contrib.rocks/image?repo=cvzi/darkmodewallpaper)](https://github.com/cvzi/darkmodewallpaper/graphs/contributors)

## Permissions:
*   `READ_EXTERNAL_STORAGE` (read the contents of your internal storage/sd card)
    Required if you want to import your existing wallpaper, otherwise you may revoke it.

## Languages

To help translate this app, please visit [crowdin.com/project/darkmodewallpaper](https://crwd.in/darkmodewallpaper?utm_source=badge&utm_medium=referral&utm_campaign=badge-add-on), where the localizations are managed. If you like to add a new language, please open an issue or email me and I will add it.

<a href="https://crwd.in/darkmodewallpaper?utm_source=badge&utm_medium=referral&utm_campaign=badge-add-on" rel="nofollow"><img style="width:140;height:40px" src="https://badges.crowdin.net/badge/dark/crowdin-on-light.png" srcset="https://badges.crowdin.net/badge/dark/crowdin-on-light.png 1x,https://badges.crowdin.net/badge/crowdin-on-light@2x.png 2x"  alt="Crowdin | Agile localization for tech companies" /></a>


<img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/1_en-US.png" width="500" />


----------------


TODO:
*   Crop/move/zoom image
*   Customize animation duration
*   Support slideshow from folder instead of a single image

Implemented:
*   Import current wallpaper
*   Allow other apps to "share"/"use as" images to this app
*   Color overlay
*   Show a preview in the settings
*   Brightness/contrast settings
*   Show a "loading..." spinner when fetching an image from disk
*   Only move/scroll the wallpaper on page changes if the wallpaper is wide enough
*   Different wallpaper on lock-screen
*   Blend animation to turn lock-screen wallpaper into home-screen wallpaper
*   Show-case used open source software licenses
*   Preview each of the four wallpapers
*   Multi display support
*   Allow drag and drop images from other apps into MainActivity
*   onZoomChanged()
*   Delete image again


[![saythanks](https://img.shields.io/badge/say-thanks-ff69b4.svg?style=for-the-badge)](https://saythanks.io/to/cuzi%40openmail.cc)
