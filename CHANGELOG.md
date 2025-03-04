# Changelog

## 1.8.0
*   Allow animation from lock-screen to home-screen without images i.ie "only color"-mode

## 1.7.2
*   Fix crash on OS with [16k page size](https://developer.android.com/guide/practices/page-sizes) and software rendering

## 1.7.1
*   Remove thumbnail from wallpaper to avoid a bug with the "Recent apps"-screen [#185](https://github.com/cvzi/darkmodewallpaper/issues/185)

## 1.7.0
*   Lockscreen wallpaper works after reboot
*   Option to disable notifying the launcher of the lockscreen colors
*   Option to disable automatic memory cleanup of images

## 1.6.11
*   SDK 35
*   Reduce memory usage

## 1.6.10
*   Fix a crash after applying the wallpaper

## 1.6.9
*   Reduce memory usage

## 1.6.8
*   Fix bug in Android 11 when trying to select an image from the gallery

## 1.6.7
*   Fix a memory leak

## 1.6.5
*   Dependency updates**

## 1.6.4
*   Translations updated
*   Dependency updates
*   Ndk 26.1

## 1.6.3
*   Swedish translation updated by [@opExe](https://github.com/opExe)
*   Sdk 34

## 1.6.2
*   Translations updated from crowdin
*   Don't ask to [import wallpaper on Android Tiramisu](https://issuetracker.google.com/issues/237124750#comment60)

## 1.6.1
*   Fix language list for Android 13

## 1.6.0
*   Translations updated from crowdin
*   Add donation links
*   Allow setting the colors that the launcher receives in the advanced settings
*   Include blur function directly instead of whole [Renderscript library](https://github.com/android/renderscript-intrinsics-replacement-toolkit) and optimize it

## 1.5.0
*   Support for animated GIF and WebP [#113](https://github.com/cvzi/darkmodewallpaper/issues/113) [#118](https://github.com/cvzi/darkmodewallpaper/pull/118)
*   Update to Gradle 8.0

## 1.4.14
*   Refactor wallpaper colors calculation

## 1.4.13
*   Add current wallpaper colors to debug information
*   Use viewBinding

## 1.4.12
*   Translations updated from crowdin
*   Fix link in copyright statement [#106](https://github.com/cvzi/darkmodewallpaper/issues/106)

## 1.4.11
*   Translations updated from crowdin

## 1.4.10
*   🇧🇾 Belarusian translation added by [@Atrafon](https://github.com/Atrafon)

## 1.4.9
*   Russian translation improved
*   Fix dropdown color in dark mode [#96](https://github.com/cvzi/darkmodewallpaper/issues/96)

## 1.4.8
*   Translations updated from crowdin
*   Offer to open WallpaperExport on Android 13 Tiramisu

## 1.4.7
*   Russian translation improved
*   Fix color of some buttons

## 1.4.6
*   Fix photo picker [#86](https://github.com/cvzi/darkmodewallpaper/issues/86)
*   Add more debug information

## 1.4.5
*   Fix: blurring night lockscreen does not work [#79](https://github.com/cvzi/darkmodewallpaper/issues/79)

## 1.4.4
*   Do not `notifyColorsChanged()` in Material you preview

## 1.4.3
*   🇹🇷 Turkish translation by [@metezd](https://github.com/metezd)

## 1.4.2
*   Translations updated from crowdin

## 1.4.1
*   Translations updated from crowdin

## 1.4.0
*   Utilize the new [photo picker](https://developer.android.com/about/versions/13/features/photopicker)
*   [Revoke permission](https://developer.android.com/about/versions/13/features?hl=en#developer-downgradable-permissions) after import of wallpaper
*   Add a monochrome launcher icon for ["Themed icons"](https://developer.android.com/about/versions/13/features?hl=en#themed-app-icons) in "Material You"
*   Include necessary files for [per-app language](https://developer.android.com/about/versions/13/features/app-languages)
*   Replace [onBackPressed()](https://developer.android.com/about/versions/13/features/predictive-back-gesture)
*   Allow higher blur values
*   Select scroll mode for home screen
*   Known issue: Importing wallpaper fails on Android 13 on a fresh install

## 1.3.1
*   Fix translations

## 1.3.0
*   Blur image in "Brightness & Contrast" menu [#64](https://github.com/cvzi/darkmodewallpaper/issues/64)

## 1.2.11
*   Immediately redraw after exiting micro screen [#63](https://github.com/cvzi/darkmodewallpaper/issues/63)

## 1.2.10
*   Allow importing to night/lockscreen [#62](https://github.com/cvzi/darkmodewallpaper/issues/62)

## 1.2.9
*   🇷🇺 Russian translation by [Ilyas Khaniev](https://github.com/TheOldBlood)

## 1.2.8
*   Improve drawing speed when contrast and brightness are set

## 1.2.7
*   Improve drawing speed when contrast and brightness are not set

## 1.2.6
*   Set `minifyEnabled true` in release [#42](https://github.com/cvzi/darkmodewallpaper/issues/42)

## 1.2.5
*   🇯🇵 Japanese translation by [@Npepperlinux](https://github.com/Npepperlinux)

## 1.2.4
*   Remove publish from build.gradle

## 1.2.3
*   🇯🇵 Japanese translation by [@Npepperlinux](https://github.com/Npepperlinux)

## 1.2.2
*   🇯🇵 Japanese translation by [@Npepperlinux](https://github.com/Npepperlinux)

## 1.2.1
*   Fix minor bugs

## 1.2.0
*   New advanced settings activity
*   First advanced setting: Delay color notification after unlock [#35](https://github.com/cvzi/darkmodewallpaper/issues/35)

## 1.1.10
*   Fix Material You preview size [#31](https://github.com/cvzi/darkmodewallpaper/issues/31)

## 1.1.9
*   🇪🇸 Spanish translation by [@sguinetti](https://github.com/sguinetti)

## 1.1.8
*   Fix small bugs

## 1.1.7
*   Improve color detection performance
*   Error in layout [#32](https://github.com/cvzi/darkmodewallpaper/issues/32) fixed in [#33](https://github.com/cvzi/darkmodewallpaper/pull/33) by [@yuhuitech](https://github.com/yuhuitech)

## 1.1.6
*   Detect colors from wallpaper instead of from the image file [#26](https://github.com/cvzi/darkmodewallpaper/issues/28), [#28](https://github.com/cvzi/darkmodewallpaper/issues/28), [#29](https://github.com/cvzi/darkmodewallpaper/issues/29)

## 1.1.5
*   Fix layout on Android 12 [#27](https://github.com/cvzi/darkmodewallpaper/issues/27)
*   Improve detection of wallpaper colors [#28](https://github.com/cvzi/darkmodewallpaper/issues/28)

## 1.1.4
*   New gradle version 7.0.0

## 1.1.3
*   Improve detection of wallpaper colors [#26](https://github.com/cvzi/darkmodewallpaper/issues/26)
*   Upgrade to SDK 30
*   Remove jcenter() and temporarily removed [gradle-license-plugin](https://github.com/jaredsburrows/gradle-license-plugin/issues/146)

## 1.1.2
*   Improve performance of zoom effect

## 1.1.1
*   Option to disable the zoom effect on Android 11+
*   Disabled zoom effect by default

## 1.1.0
*   Hex color code in color chooser [#12](https://github.com/cvzi/darkmodewallpaper/issues/12)
*   Activate night wallpaper according to time range or follow system theme [#10](https://github.com/cvzi/darkmodewallpaper/issues/10)
*   🇵🇱 Polish translation by [@gnu-ewm](https://github.com/gnu-ewm)

## 1.0.0
*   Fix: "Activity not found" when opening wallpaper preview
*   Minor design changes

## 1.0-alpha3
*   Fix: Unlock not detected [#7](https://github.com/cvzi/darkmodewallpaper/issues/7)
*   Fix: Safely dismiss() dialogs
*   Lock screen icons
*   Check for permission before trying to import wallpaper
*   Properly go into fullscreen mode with BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
*   Enable lock screen settings when image is selected
*   More info in about activity
*   Allow landscape orientation in activities

## 1.0-alpha2
*   French translation

## 1.0-alpha1
*   Texts improved
*   German translation
*   gradle wrapper fixed

## 1.0-alpha0
*   Initial version
