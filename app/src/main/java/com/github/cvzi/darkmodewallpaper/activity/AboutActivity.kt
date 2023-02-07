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
import com.github.cvzi.darkmodewallpaper.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAboutBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setHtmlText(binding.textViewAboutLicense, R.string.about_license)

        val buttonAboutOpenSourceLicenses =
            binding.buttonAboutOpenSourceLicenses
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
            binding.textViewAppVersion,
            R.string.about_version,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE,
            BuildConfig.BUILD_TYPE
        )

        setHtmlText(binding.textViewIssues, R.string.about_issues)

        setHtmlText(binding.textViewTranslate, R.string.about_translate)
    }

    private fun setHtmlText(
        textView: TextView,
        stringRes: StringRes,
        vararg formatArgs: Any?
    ): TextView {
        return setHtmlText(textView, getString(stringRes, *formatArgs))
    }

    private fun setHtmlText(textView: TextView, htmlString: String): TextView {
        return textView.apply {
            movementMethod = LinkMovementMethod()
            text = Html.fromHtml(
                htmlString,
                Html.FROM_HTML_SEPARATOR_LINE_BREAK_DIV
            )
        }
    }
}