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
import android.text.Html
import android.text.method.LinkMovementMethod
import android.webkit.WebView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.github.cvzi.darkmodewallpaper.R

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val textViewAboutLicense = findViewById<TextView>(R.id.textViewAboutLicense)
        textViewAboutLicense.movementMethod = LinkMovementMethod()
        textViewAboutLicense.text = Html.fromHtml(
            getString(R.string.about_license),
            Html.FROM_HTML_SEPARATOR_LINE_BREAK_DIV
        )

        val buttonAboutOpenSourceLicenses =
            findViewById<TextView>(R.id.buttonAboutOpenSourceLicenses)
        buttonAboutOpenSourceLicenses.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.about_open_source_licenses))
                .setView(WebView(this).apply {
                    loadUrl("file:///android_asset/open_source_licenses.html")
                })
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }
}