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
import com.github.cvzi.darkmodewallpaper.*

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        setHtmlText(R.id.textViewAboutLicense, R.string.about_license)

        val buttonAboutOpenSourceLicenses =
            findViewById<TextView>(R.id.buttonAboutOpenSourceLicenses)
        buttonAboutOpenSourceLicenses.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.about_open_source_licenses))
                .setView(WebView(this).apply {
                    loadUrl("file:///android_asset/open_source_licenses.html")
                })
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    dialog.safeDismiss()
                }
                .show()
        }

        setHtmlText(
            R.id.textViewAppVersion,
            R.string.about_version,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE,
            "${BuildConfig.BUILD_TYPE} (minSdkVersion 28 / Android 9)"
        )

        setHtmlText(R.id.textViewIssues, R.string.about_issues)

        setHtmlText(R.id.textViewTranslate, R.string.about_translate)
    }

    private fun setHtmlText(
        viewId: IdRes,
        stringRes: StringRes,
        vararg formatArgs: Any?
    ): TextView {
        return setHtmlText(viewId, getString(stringRes, *formatArgs))
    }

    private fun setHtmlText(viewId: IdRes, htmlString: String): TextView {
        return findViewById<TextView>(viewId).apply {
            movementMethod = LinkMovementMethod()
            text = Html.fromHtml(
                htmlString,
                Html.FROM_HTML_SEPARATOR_LINE_BREAK_DIV
            )
        }
    }
}