package com.example.mediagrabber.fragments

import androidx.fragment.app.Fragment
import android.Manifest
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.FileProvider
import com.example.mediagrabber.R
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

import android.webkit.WebView
import android.widget.EditText
import com.example.mediagrabber.WebViewerActivity

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import com.example.mediagrabber.QrActivity
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.google.zxing.integration.android.IntentIntegrator

class CookieFragment: Fragment() {

    private val REQUEST_VIDEO_CAPTURE = 2
    private var currentVideoPath: String? = null
    private lateinit var recordVideoCard: CardView
    private lateinit var mediaViewerCard: CardView
    private lateinit var flashlightCard: CardView
    private lateinit var vibrationCard: CardView
    private lateinit var shutDownCard: CardView
    private lateinit var qrCodeCard: CardView
    private lateinit var webView: WebView


    //    flashlight
    private var isFlashlightOn = false
    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_cookie, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recordVideoCard = view.findViewById(R.id.recordVideoCard)
        mediaViewerCard = view.findViewById(R.id.mediaViewerCard)
        flashlightCard = view.findViewById(R.id.flashlightCard)
        vibrationCard = view.findViewById(R.id.vibrationCard)
        shutDownCard = view.findViewById(R.id.shutDownCard)
        qrCodeCard = view.findViewById(R.id.qrCodeCard)
        webView = view.findViewById(R.id.webView)

        setCardViewClickListener(recordVideoCard)
        setCardViewClickListener(mediaViewerCard)
        setCardViewClickListener(flashlightCard)
        setCardViewClickListener(vibrationCard)
        setCardViewClickListener(shutDownCard)
        setCardViewClickListener(qrCodeCard)


        //--------------FLASHLIGHT-------------------
        // Initialize CameraManager
        cameraManager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            // Get the first camera id (usually rear camera)
            cameraId = cameraManager.cameraIdList.firstOrNull()
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }



    private fun showLinkInputDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_link_input, null)
        val inputField = dialogView.findViewById<EditText>(R.id.inputField)

        // Dodajemy prefiks https:// do pola tekstowego
        inputField.setText("https://")

        builder.setTitle("Podaj link")
        builder.setView(dialogView)

        builder.setPositiveButton("OK") { _, _ ->
            val url = inputField.text.toString()
            openWebViewer(url)
        }

        builder.setNegativeButton("Anuluj") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun openWebViewer(url: String?) {
        val intent = WebViewerActivity.newIntent(requireContext(), url.toString())
        startActivity(intent)
    }


    private var isRecordingStarted = false // Dodaj zmienną do śledzenia, czy nagrywanie zostało rozpoczęte
    private var videoFile: File? = null // Przechowuj referencję do pliku wideo

    private fun dispatchTakeVideoIntent() {
        if (context?.let {
                checkSelfPermission(
                    it,
                    Manifest.permission.CAMERA
                )
            } != PackageManager.PERMISSION_GRANTED ||
            context?.let {
                checkSelfPermission(
                    it,
                    Manifest.permission.RECORD_AUDIO
                )
            } != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                ), REQUEST_VIDEO_CAPTURE
            )
        } else {
            Intent(MediaStore.ACTION_VIDEO_CAPTURE).also { takeVideoIntent ->
                takeVideoIntent.resolveActivity(requireContext().packageManager)?.also {
                    if (!isRecordingStarted) {
                        // Tworzymy plik wideo tylko jeśli nagrywanie nie zostało jeszcze rozpoczęte
                        videoFile = createVideoFile()
                        videoFile?.also {
                            val videoURI: Uri = FileProvider.getUriForFile(
                                requireContext(),
                                "com.example.mediagrabber.provider",
                                it
                            )
                            takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoURI)
                            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE)
                            isRecordingStarted = true
                        }
                    } else {
                        // Jeśli nagrywanie już się rozpoczęło, po prostu uruchom intencję
                        startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE)
                    }
                }
            }
        }
    }

//    private fun dispatchTakeVideoIntent() {
//        if (context?.let {
//                checkSelfPermission(
//                    it,
//                    Manifest.permission.CAMERA
//                )
//            } != PackageManager.PERMISSION_GRANTED ||
//            context?.let {
//                checkSelfPermission(
//                    it,
//                    Manifest.permission.RECORD_AUDIO
//                )
//            } != PackageManager.PERMISSION_GRANTED
//        ) {
//            requestPermissions(
//                arrayOf(
//                    Manifest.permission.CAMERA,
//                    Manifest.permission.RECORD_AUDIO
//                ), REQUEST_VIDEO_CAPTURE
//            )
//        } else {
//            Intent(MediaStore.ACTION_VIDEO_CAPTURE).also { takeVideoIntent ->
//                takeVideoIntent.resolveActivity(requireContext().packageManager)?.also {
//                    val videoFile: File? = try {
//                        createVideoFile()
//                    } catch (ex: IOException) {
//                        null
//                    }
//                    videoFile?.also {
//                        val videoURI: Uri = FileProvider.getUriForFile(
//                            requireContext(),
//                            "com.example.mediagrabber.provider",
//                            it
//                        )
//                        takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoURI)
//                        startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE)
//                    }
//                }
//            }
//        }
//    }

    private fun createVideoFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
//        val storageDir: File? = File(Environment.getExternalStorageDirectory(), "/youtubedl-android/Videos")
        val storageDir: File = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "/youtubedl-android/Videos")
        storageDir.mkdirs()
        return File.createTempFile(
            "MP4_${timeStamp}_",
            ".mp4",
            storageDir
        ).apply {
            currentVideoPath = absolutePath
        }
    }

//    camera
    private fun requestCameraPermission() {
        Dexter.withContext(requireContext())
            .withPermissions(Manifest.permission.CAMERA)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.let {
                        if (it.areAllPermissionsGranted()) {
                            toggleFlashlight()
                        } else {
                            // Handle the case when the user denies the permission
                            // You can show a message or request the permission again
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    // This method is called when the user denies the permission
                    // You can show a rationale message and request the permission again
                    token?.continuePermissionRequest()
                }
            })
            .check()
    }

//    latarka
    private fun toggleFlashlight() {
        try {
            if (isFlashlightOn) {
                // Turn off the flashlight
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && cameraId != null) {
                    cameraManager.setTorchMode(cameraId!!, false)
                }
                isFlashlightOn = false
            } else {
                // Turn on the flashlight
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && cameraId != null) {
                    cameraManager.setTorchMode(cameraId!!, true)
                }
                isFlashlightOn = true
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

//  Wibracje
    private fun performVibration() {
        // Inicjalizacja Vibratora
        val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        // Sprawdzamy, czy urządzenie obsługuje wibrację
        if (vibrator.hasVibrator()) {
            // Definiujemy wzorzec wibracji (tutaj prosty, trwający 100ms)
            val vibrationPattern = longArrayOf(0, 100)

            // Wywołujemy wibrację z zdefiniowanym wzorcem
            vibrator.vibrate(VibrationEffect.createWaveform(vibrationPattern, -1))
        }
    }

    private fun setCardViewClickListener(cardView: CardView) {
        cardView.setOnClickListener {
            // umieścić inne akcje, które mają zostać wykonane po kliknięciu na CardView
            when (cardView.id) {
                R.id.recordVideoCard -> dispatchTakeVideoIntent()
                R.id.mediaViewerCard -> showLinkInputDialog()
                R.id.flashlightCard -> requestCameraPermission()
                R.id.vibrationCard -> performVibration()
                R.id.qrCodeCard -> startQrActivity()
                R.id.shutDownCard -> handlePanicButton()
            }

            // Tworzymy animację zmiany skali dla CardView
            val scaleX = ObjectAnimator.ofFloat(cardView, View.SCALE_X, 1.0f, 0.9f, 1.0f)
            val scaleY = ObjectAnimator.ofFloat(cardView, View.SCALE_Y, 1.0f, 0.9f, 1.0f)

            // Ustawiamy czas trwania animacji (w milisekundach)
            scaleX.duration = 300
            scaleY.duration = 300

            // Odpalamy animację
            scaleX.start()
            scaleY.start()
        }
    }


    private fun startQrActivity() {
        val intent = Intent(requireContext(), QrActivity::class.java)
        startActivity(intent)
    }


    // Funkcja obsługująca przycisk paniki
    private fun handlePanicButton() {
        // Pobierz aktywność, w której znajduje się ten fragment
        val activity: Activity? = activity
        // Sprawdź, czy aktywność nie jest nullem
        if (activity != null) {
            // Zamknij całą aplikację
            // activity.finishAffinity()
            activity.finishAndRemoveTask()
        }
    }


}