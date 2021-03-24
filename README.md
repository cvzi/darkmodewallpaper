# darkmodewallpaper
A live wallpaper for Android that respects dark theme mode

The app automatically changes the wallpaper when the dark theme is enabled or disabled.
You can set a different image for dark theme or change the color/contrast/brightness of
your image.

[<img src="https://raw.githubusercontent.com/cvzi/ScreenshotTile/master/docs/imgs/get-it-on-f-droid.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/packages/com.github.cvzi.darkmodewallpaper/) 

This app requires Android 10. If you are using an earlier version of Android, you may be able to
achieve a comparable experience by using MacroDroid, Tasker or a similar automation tool.

## Permissions:
*   `READ_EXTERNAL_STORAGE` (read the contents of your internal storage/sd card)
    Required if you want to import your existing wallpaper, otherwise you may revoke it.

## Languages

To help translate this app, please visit [crowdin.com/project/darkmodewallpaper](https://crwd.in/darkmodewallpaper?utm_source=badge&utm_medium=referral&utm_campaign=badge-add-on), where the localizations are managed. If you like to add a new language, please open an issue or email me and I will add it.

<a href="https://crwd.in/darkmodewallpaper?utm_source=badge&utm_medium=referral&utm_campaign=badge-add-on" rel="nofollow"><img style="width:140;height:40px" src="https://badges.crowdin.net/badge/dark/crowdin-on-light.png" srcset="https://badges.crowdin.net/badge/dark/crowdin-on-light.png 1x,https://badges.crowdin.net/badge/crowdin-on-light@2x.png 2x"  alt="Crowdin | Agile localization for tech companies" /></a>

TODO:
*   Crop/move/zoom image
*   Customize animation duration

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

<img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/1_en-US.png" width="500" />
