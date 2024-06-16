package com.example.mediagrabber

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.zxing.integration.android.IntentIntegrator

class QrActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sprawdź uprawnienia kamerowe
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        } else {
            startQrScanner()
        }
    }

    private fun startQrScanner() {
        IntentIntegrator(this).apply {
            setOrientationLocked(false)
            setPrompt("Zeskanuj kod QR")
            setBeepEnabled(true)
            initiateScan()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startQrScanner()
            } else {
                Toast.makeText(this, "Brak uprawnień do korzystania z kamery", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                // Zapisz odczytany tekst do schowka
                val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("QR Code", result.contents)
                clipboardManager.setPrimaryClip(clipData)

                Toast.makeText(this, "Tekst skopiowany do schowka", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Anulowano skanowanie", Toast.LENGTH_SHORT).show()
            }
        }

        // Zakończ QrActivity po zakończeniu skanowania
        finish()
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }
}