# <img src="https://raw.githubusercontent.com/cvzi/darkmodewallpaper/main/app/src/main/ic_launcher-playstore.png" alt="Launcher icon" height="48"> DarkModeLiveWallpaper


[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3%20or%20later-a32d2a?style=for-the-badge&logo=GNU)](https://www.gnu.org/licenses/gpl-3.0)
[![F-Droid](https://img.shields.io/f-droid/v/com.github.cvzi.darkmodewallpaper.svg?style=for-the-badge&logo=f-droid)](https://f-droid.org/packages/com.github.cvzi.darkmodewallpaper/)
[![Download APK file](https://img.shields.io/github/release/cvzi/darkmodewallpaper.svg?style=for-the-badge&label=Download%20apk&logo=android&color=3d8)](https://github.com/cvzi/darkmodewallpaper/releases/latest)
[![F-Droid build status](https://img.shields.io/endpoint?logo=textpattern&logoColor=blue&style=for-the-badge&url=https%3A%2F%2Ff-droid-build.cuzi.workers.dev%2Fcom.github.cvzi.darkmodewallpaper)](https://monitor.f-droid.org/)
[![Gradle CI](https://img.shields.io/github/actions/workflow/status/cvzi/darkmodewallpaper/gradleCI.yml?branch=main&logo=github&style=for-the-badge)](https://github.com/cvzi/darkmodewallpaper/actions/workflows/gradleCI.yml)

A live wallpaper for Android that respects dark theme mode

[Changelog](CHANGELOG.md) • [View older releases](https://keybase.pub/cuzi/DarkModeWallpaper_bin/)

The app automatically changes the wallpaper when the dark theme is enabled or disabled.
You can set a different image for dark theme or change the color/contrast/brightness of
your image.

Also supports animated GIF and WebP animations.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/com.github.cvzi.darkmodewallpaper/)
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
     alt="Get it on Google Play"
     height="80">](https://play.google.com/store/apps/details?id=com.github.cvzi.darkmodewallpaper)

This app requires Android 10 or a later version. If you are using an earlier version of Android, you may be able to
achieve a comparable experience by using MacroDroid, Tasker or a similar automation tool.

## Selecting images

There are three ways to load an image as a wallpaper:
*  the "Select Image" button in the app opens the new [Android photo picker](https://developer.android.com/training/data-storage/shared/photopicker), its access is limited to the phone's gallery
*  tap on the preview image on the right and select the  "Select another image" to open the old photo picker
*  share the image from another app: open the image in the gallery or any file manager app and press "Share". There are four entries in the share menu: day / night / lockscreen day / lockscreen night

## Contributors

[![Contributors](https://contrib.rocks/image?repo=cvzi/darkmodewallpaper)](https://github.com/cvzi/darkmodewallpaper/graphs/contributors)

## Permissions

* `READ_EXTERNAL_STORAGE` (read the contents of your internal storage/sd card)
    Required if you want to import your existing wallpaper, otherwise you may revoke it.

## Translate

To help translate this app, please visit [crowdin.com/project/darkmodewallpaper]([https://crwd.in/darkmodewallpaper?utm_source=badge&utm_medium=referral&utm_campaign=badge-add-on](https://crowdin.com/project/darkmodewallpaper)), where the localizations are managed. If you like to add a new language, please open an issue or email me and I will add it.

[<image src="https://badges.crowdin.net/badge/dark/crowdin-on-light.png"
     alt="Crowdin | Agile localization for tech companies"
     height="40">](https://crowdin.com/project/darkmodewallpaper)

## Screenshots

| <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/1_en-US.png" alt="Screenshot-1" /> | <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/2_en-US.png" alt="Screenshot-2"/> | <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/3_en-US.png" alt="Screenshot-3"/> | <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/4_en-US.png" alt="Screenshot-4" /> |
| --- | ---- | ---- | --- |

----------------

## Notable libraries we use

* [martin-stone/hsv-alpha-color-picker-android](https://github.com/martin-stone/hsv-alpha-color-picker-android)
* [android/renderscript-intrinsics-replacement-toolkit](https://github.com/android/renderscript-intrinsics-replacement-toolkit) (We use a modified version that is limited to the blur-function)
* [jaredsburrows/gradle-license-plugin](https://github.com/jaredsburrows/gradle-license-plugin)

## License

This project is licensed under the **GNU General Public License version 3** or (at your option) any **later version**.
However individual files may have a different license. Refer to the license header of each file for detailed information.
In particular the files related to the `renderscript-intrinsics-replacement-toolkit` - including all C++ files - are licensed under the **Apache License, Version 2.0**.


[![Say Thanks!](https://img.shields.io/badge/say-thanks-ff69b4.svg?style=for-the-badge)](https://saythanks.io/to/cvzi)
