package com.example.mediagrabber

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.KeyguardManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.biometrics.BiometricPrompt
import android.hardware.biometrics.BiometricManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CancellationSignal
import android.provider.Settings
import android.text.Editable
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import android.widget.EditText
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import android.os.Handler


class MainActivity : AppCompatActivity() {

    private var cancellationSignal: CancellationSignal? = null
    private var pinCode: String? = null
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var alertDialog: AlertDialog // Deklaracja globalnej zmiennej przechowującej AlertDialog


    private val authenticationCallback: BiometricPrompt.AuthenticationCallback
        get() =
            @RequiresApi(Build.VERSION_CODES.P)
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    super.onAuthenticationError(errorCode, errString)
                    notifyUser("Authentication error: $errString")
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    super.onAuthenticationSucceeded(result)
                    notifyUser("Authentication success!!!")
                    startActivity(Intent(this@MainActivity, SwitchActivity::class.java))
                    // Finish the MainActivity
                    finish()
                }
            }



    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        askPermission()

        sharedPreferences = getSharedPreferences("pin_code_pref", Context.MODE_PRIVATE)
        pinCode = sharedPreferences.getString("pin_code", null)

        if (pinCode.isNullOrEmpty()) {
            showSetPinDialog()
        }else{
            showChooseAuthenticationMethodDialog()
        }
    }


    private fun showSetPinDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_set_pin, null)

        val pinInput = dialogView.findViewById<EditText>(R.id.pin_input)
        val errorMessage = dialogView.findViewById<TextView>(R.id.error_message)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Set PIN Code")
        builder.setPositiveButton("Set", null)
            .setCancelable(false)

        alertDialog = builder.create()

        alertDialog.setView(dialogView)

        alertDialog.setOnShowListener { dialog ->
            alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setOnClickListener {
                val pinCodeEntered = pinInput.text.toString()
                if (pinCodeEntered.length in 4..8) {
                    sharedPreferences.edit().putString("pin_code", pinCodeEntered).apply()
                    pinCode = pinCodeEntered // Aktualizacja wartości pinCode
                    val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(pinInput.windowToken, 0)
                    alertDialog.dismiss()
                } else {
                    errorMessage.text = "PIN must be between 4 and 8 digits"
                }
            }
        }

        alertDialog.setOnDismissListener {
            Handler().postDelayed({
                pinCode = sharedPreferences.getString("pin_code", null) // Aktualizacja wartości pinCode
                if (!pinCode.isNullOrBlank()) {
                    showChooseAuthenticationMethodDialog()
                }
            }, 500)
        }

        alertDialog.show()
    }


    private fun showPinAuthentication() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_set_pin, null)

        val pinInput = dialogView.findViewById<EditText>(R.id.pin_input)
        val errorMessage = dialogView.findViewById<TextView>(R.id.error_message)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter PIN Code")
        builder.setPositiveButton("Authenticate", null)
            .setCancelable(false)

        alertDialog = builder.create()

        alertDialog.setView(dialogView)

        alertDialog.setOnShowListener { dialog ->
            alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setOnClickListener {
                val enteredPin = pinInput.text.toString()
                if (enteredPin == pinCode) {
                    startActivity(Intent(this@MainActivity, SwitchActivity::class.java))
                    finish()

                } else {
                    val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(pinInput.windowToken, 0)

                    errorMessage.text = "Invalid PIN"
                    Log.d("KURWAAA","Nie działa")
                }
            }
        }
        alertDialog.show()
    }

    private fun notifyUser(message: String){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun getCancelationSignal(): CancellationSignal {
        cancellationSignal = CancellationSignal()
        cancellationSignal?.setOnCancelListener {
            notifyUser("Authentication was cancelled by user")
        }

        return cancellationSignal as CancellationSignal
    }

    private fun showChooseAuthenticationMethodDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose Authentication Method")
        builder.setItems(arrayOf("PIN", "Fingerprint")) { dialog, which ->
            when (which) {
                0 -> {
                    showPinAuthentication()
                }
                1 -> {
                    showBiometricPrompt()
                }
            }
        }
        builder.setCancelable(false)
        builder.show()
    }

    private fun showBiometricPrompt() {
        val biometricPrompt = BiometricPrompt.Builder(this)
            .setTitle("Identify yourself")
            .setSubtitle("Authentication is required")
            .setDescription("This app uses fingerprint protection to keep your data secure")
            .setNegativeButton("Cancel", mainExecutor, DialogInterface.OnClickListener { dialog, which ->
//                notifyUser("Authentication failed")
            }).build()

        biometricPrompt.authenticate(getCancelationSignal(), mainExecutor, authenticationCallback)
    }

    //zeby nie wyskakiwalo po kazdym uruchomianu apki
    private var permissionsGrantedOnce = false

    private fun askPermission(){
        Dexter.withActivity(this).withPermissions(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.VIBRATE,
            Manifest.permission.USE_BIOMETRIC
        ).withListener(object: MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport?)
            {
                if(report!!.areAllPermissionsGranted() && !permissionsGrantedOnce){
                    permissionsGrantedOnce = true
                    Toast.makeText(this@MainActivity, "Posaida wszelkie uprawnienia", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>, token: PermissionToken)
            {
                showRationalDialogForPermisions()
            }
        }).onSameThread().check()
    }

    private fun showRationalDialogForPermisions(){
        AlertDialog.Builder(this).setMessage("Go to settings and add permissions")
            .setPositiveButton("Go to Settings") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)

                } catch (e: ActivityNotFoundException){
                    e.printStackTrace()
                }
            }.setNegativeButton("Cancel"){dialog, _ ->
                dialog.dismiss()
            }.show()
    }

}