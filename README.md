# <img src="https://raw.githubusercontent.com/cvzi/darkmodewallpaper/main/app/src/main/ic_launcher-playstore.png" alt="Launcher icon" height="48"> DarkModeLiveWallpaper


[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3%20or%20later-a32d2a?style=for-the-badge&logo=GNU)](https://www.gnu.org/licenses/gpl-3.0)
[![F-Droid](https://img.shields.io/f-droid/v/com.github.cvzi.darkmodewallpaper.svg?style=for-the-badge&logo=f-droid)](https://f-droid.org/packages/com.github.cvzi.darkmodewallpaper/)
[![Download APK file](https://img.shields.io/github/release/cvzi/darkmodewallpaper.svg?style=for-the-badge&label=Download%20apk&logo=android&color=3d8)](https://github.com/cvzi/darkmodewallpaper/releases/latest)
[![FOSSA Status](https://img.shields.io/endpoint?style=for-the-badge&url=https%3A%2F%2Funtitled-1ieeta2z95od.runkit.sh%2F%3Furl%3Dhttps%253A%252F%252Fapp.fossa.com%252Fapi%252Fprojects%252Fgit%25252Bgithub.com%25252Fcvzi%25252Fdarkmodewallpaper.svg%253Ftype%253Dshield)](https://app.fossa.com/projects/git%2Bgithub.com%2Fcvzi%2Fdarkmodewallpaper?ref=badge_shield)

A live wallpaper for Android that respects dark theme mode

[Changelog](CHANGELOG.md) • [View older releases](https://keybase.pub/cuzi/DarkModeWallpaper_bin/)

The app automatically changes the wallpaper when the dark theme is enabled or disabled.
You can set a different image for dark theme or change the color/contrast/brightness of
your image.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/com.github.cvzi.darkmodewallpaper/)
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
     alt="Get it on Google Play"
     height="80">](https://play.google.com/store/apps/details?id=com.github.cvzi.darkmodewallpaper)

This app requires Android 10 or a later version. If you are using an earlier version of Android, you may be able to
achieve a comparable experience by using MacroDroid, Tasker or a similar automation tool.

## Contributors

[![Contributors](https://contrib.rocks/image?repo=cvzi/darkmodewallpaper)](https://github.com/cvzi/darkmodewallpaper/graphs/contributors)

## Permissions

* `READ_EXTERNAL_STORAGE` (read the contents of your internal storage/sd card)
    Required if you want to import your existing wallpaper, otherwise you may revoke it.

## Translate

To help translate this app, please visit [crowdin.com/project/darkmodewallpaper](https://crwd.in/darkmodewallpaper?utm_source=badge&utm_medium=referral&utm_campaign=badge-add-on), where the localizations are managed. If you like to add a new language, please open an issue or email me and I will add it.

[<image src="https://badges.crowdin.net/badge/dark/crowdin-on-light.png"
     alt="Crowdin | Agile localization for tech companies"
     height="40">](https://https://crwd.in/darkmodewallpaper?utm_source=badge&utm_medium=referral&utm_campaign=badge-add-on)

## Screenshots

[//]: # ([<img src="https://raw.githubusercontent.com/meanindra/darkmodewallpaper/main/fastlane/metadata/android/en-US/images/phoneScreenshots/1_en-US.png")

[//]: # (     alt="Screenshot 1")

[//]: # (     height="500">]&#40;https://raw.githubusercontent.com/meanindra/darkmodewallpaper/main/fastlane/metadata/android/en-US/images/phoneScreenshots/1_en-US.png&#41;)

[//]: # ([<img src="https://raw.githubusercontent.com/meanindra/darkmodewallpaper/main/fastlane/metadata/android/en-US/images/phoneScreenshots/2_en-US.png")

[//]: # (     alt="Screenshot 2")

[//]: # (     height="500">]&#40;https://raw.githubusercontent.com/meanindra/darkmodewallpaper/main/fastlane/metadata/android/en-US/images/phoneScreenshots/2_en-US.png&#41;)

[//]: # ([<img src="https://raw.githubusercontent.com/meanindra/darkmodewallpaper/main/fastlane/metadata/android/en-US/images/phoneScreenshots/3_en-US.png")

[//]: # (     alt="Screenshot 3")

[//]: # (     height="500">]&#40;https://raw.githubusercontent.com/meanindra/darkmodewallpaper/main/fastlane/metadata/android/en-US/images/phoneScreenshots/3_en-US.png&#41;)

[//]: # ([<img src="https://raw.githubusercontent.com/meanindra/darkmodewallpaper/main/fastlane/metadata/android/en-US/images/phoneScreenshots/4_en-US.png")

[//]: # (     alt="Screenshot 4")

[//]: # (     height="500">]&#40;https://raw.githubusercontent.com/meanindra/darkmodewallpaper/main/fastlane/metadata/android/en-US/images/phoneScreenshots/4_en-US.png&#41;)
| <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/1_en-US.png" alt="Screenshot-1" /> | <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/2_en-US.png" alt="Screenshot-2" /> | <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/3_en-US.png" alt="Screenshot-3" /> | <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/4_en-US.png" alt="Screenshot-4" /> |
| --- | --- | --- | --- |

----------------


TODO:

* [ ] Crop/move/zoom image
* [ ] Customize animation duration
* [ ] Support slideshow from folder instead of a single image

Implemented:

* [x] Import current wallpaper
* [x] Allow other apps to "share"/"use as" images to this app
* [x] Color overlay
* [x] Show a preview in the settings
* [x] Brightness/contrast settings
* [x] Show a "loading..." spinner when fetching an image from disk
* [x] Only move/scroll the wallpaper on page changes if the wallpaper is wide enough
* [x] Different wallpaper on lock-screen
* [x] Blend animation to turn lock-screen wallpaper into home-screen wallpaper
* [x] Show-case used open source software licenses
* [x] Preview each of the four wallpapers
* [x] Multi display support
* [x] Allow drag and drop images from other apps into MainActivity
* [x] onZoomChanged()
* [x] Delete image again

## License

[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fcvzi%2Fdarkmodewallpaper.svg?type=large)](https://app.fossa.com/projects/git%2Bgithub.com%2Fcvzi%2Fdarkmodewallpaper?ref=badge_large)
