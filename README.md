# darkmodewallpaper
A live wallpaper for Android that respects dark theme mode

The app automatically changes the wallpaper when the dark theme is enabled or disabled.
You can set a different image for dark theme or change the color/contrast/brightness of
your image.

This app requires Android 10. If you are using an earlier version of Android, you may be able to
achieve a comparable experience by using MacroDroid, Tasker or a similar automation tool.

Permissions:
*   `READ_EXTERNAL_STORAGE` (read the contents of your internal storage/sd card)
    Required if you want to import you existing wallpaper, otherwise you may revoke it.

TODO:
*   Crop/move/zoom image
*   Customize animation duration
*   Run after boot (possible?)

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
