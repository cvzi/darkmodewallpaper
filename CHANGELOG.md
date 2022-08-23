# Changelog

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
