package com.example.mediagrabber

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import java.io.File
import java.io.FileWriter
import java.io.IOException
import android.webkit.CookieManager


class WebViewerActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    lateinit var btnOk: Button


    companion object {
        private const val EXTRA_URL = "extra_url"

        fun newIntent(context: Context, url: String): Intent {
            return Intent(context, WebViewerActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_viewer)


        webView = findViewById(R.id.webView)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)




        val url = intent.getStringExtra(EXTRA_URL)
        setupWebView(url.toString())
    }

    private fun setupWebView(url: String) {
        webView.settings.javaScriptEnabled = true
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = WebViewClient()
        webView.loadUrl(url)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_ok -> {
                // Wygeneruj pliki cookie z sesji
//                val cookies = CookieManager.getInstance().getCookie(EXTRA_URL)

                // Zapisz pliki cookie do pliku txt
//                saveCookiesToFile(cookies)


                // Pobierz aktualny adres URL z WebView
                val url = webView.url

                // Pobierz pliki cookie dla aktualnego adresu URL
                val cookies = CookieManager.getInstance().getCookie(url)

                // Zapisz pliki cookie do pliku txt
                saveCookiesToFile(cookies)

                // Poinformuj użytkownika o zapisaniu plików cookie
                Toast.makeText(this, "Pliki cookie zostały zapisane do pliku.", Toast.LENGTH_SHORT).show()

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun saveCookiesToFile(cookies: String) {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val youtubeDLDir = File(downloadsDir, "youtubedl-android")

            // Sprawdzamy, czy katalog istnieje, jeśli nie, tworzymy go
            if (!youtubeDLDir.exists()) {
                youtubeDLDir.mkdirs()
            }

            val cookieFile = File(youtubeDLDir, "cookie.txt")
            FileWriter(cookieFile).use { writer ->
                writer.write(cookies)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("SaveCookieToFile", "Błąd podczas zapisu pliku cookie: ${e.message}")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_web_viewer, menu)
        return true
    }

}