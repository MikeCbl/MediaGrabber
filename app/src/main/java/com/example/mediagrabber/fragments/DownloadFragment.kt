package com.example.mediagrabber.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.example.mediagrabber.R
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.io.File



class DownloadFragment : Fragment() {
    //zmienne
    private var downloading: Boolean = false
    private var updating: Boolean = false
    private var STORAGE_PERMISSION_CODE = 1

    //guziczki
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnUpdate: Button
    private lateinit var etUrl: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var pbLoading: ProgressBar
    private lateinit var textViewStatus: TextView
    private lateinit var cookieBtn: Switch

    //inne
    private val compositeDisposable = CompositeDisposable()
    private val processId = "MyDlProcess"
    private val callback: (Float, Long, String) -> Unit =
        { progress: Float, o2: Long, line: String ->
            requireActivity().runOnUiThread {
                progressBar.progress = progress.toInt()
                textViewStatus.text = line
            }
            Unit
        }

    //onCreate
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_downloads, container, false)

        initLibs(view)
        initViews(view)
        initListeners(view)

        return view
    }

    private fun initLibs(view: View) {
        YoutubeDL.getInstance().init(requireContext().applicationContext)
        FFmpeg.getInstance().init(requireContext().applicationContext)
        Aria2c.getInstance().init(requireContext().applicationContext)
    }

    private fun initViews(view: View) {
        btnStart = view.findViewById(R.id.btn_start_download)
        btnStop = view.findViewById(R.id.btn_stop_download)
        btnUpdate = view.findViewById(R.id.btn_update_ytdlp)
        etUrl = view.findViewById(R.id.editText_url)
        progressBar = view.findViewById(R.id.progressBar)
        pbLoading = view.findViewById(R.id.progressBar_status)
        textViewStatus = view.findViewById(R.id.text_status)
        cookieBtn = view.findViewById(R.id.use_cookie)
    }

    private fun initListeners(view: View) {
        btnStart.setOnClickListener {
            startDownload()
        }
        btnStop.setOnClickListener {
            try {
                YoutubeDL.getInstance().destroyProcessById(processId)
            } catch (e: Exception) {
                Log.e("DownloadingExampleActivity", e.toString())
            }
        }
        btnUpdate.setOnClickListener {
            val updateOptions = arrayOf("Stable Releases", "Nightly Releases")

            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Update Channel")
            builder.setItems(updateOptions) { _, which ->
                when (which) {
                    0 -> updateYoutubeDL(YoutubeDL.UpdateChannel._STABLE)
                    1 -> updateYoutubeDL(YoutubeDL.UpdateChannel._NIGHTLY)
                }
            }

            val dialog = builder.create()
            dialog.show()
        }
        cookieBtn.setOnClickListener {
//            generateCookiesFile()
        }
    }

    private fun updateYoutubeDL(updateChannel: YoutubeDL.UpdateChannel) {
        if (updating) {
            Toast.makeText(requireContext(), "Update is already in progress!", Toast.LENGTH_LONG)
                .show()
            return
        }
        updating = true
        progressBar.visibility = View.VISIBLE
        val disposable = Observable.fromCallable<YoutubeDL.UpdateStatus?> {
            YoutubeDL.getInstance().updateYoutubeDL(
                requireContext(),
                updateChannel
            )
        }
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ status: YoutubeDL.UpdateStatus? ->
                progressBar.visibility = View.GONE
                when (status) {
                    YoutubeDL.UpdateStatus.DONE -> Toast.makeText(
                        requireContext(),
                        "Update successful " + YoutubeDL.getInstance().versionName(requireContext()),
                        Toast.LENGTH_LONG
                    ).show()

                    YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE -> Toast.makeText(
                        requireContext(),
                        "Already up to date " + YoutubeDL.getInstance().versionName(requireContext()),
                        Toast.LENGTH_LONG
                    ).show()

                    else -> Toast.makeText(requireContext(), status.toString(), Toast.LENGTH_LONG)
                        .show()
                }
                updating = false
            }) { e: Throwable? ->
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "update failed", Toast.LENGTH_LONG).show()
                updating = false
            }
        compositeDisposable.add(disposable)
    }

    private fun startDownload() {
        Log.d("downloading", "startDownload() called")
        if (downloading) {
            Toast.makeText(
                requireContext(),
                "cannot start download. a download is already in progress",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (!isStoragePermissionGranted()) {
            Toast.makeText(requireContext(), "grant storage permission and retry", Toast.LENGTH_LONG).show()
            return
        }

        val url = etUrl.text.toString().trim { it <= ' ' }
//        val url = etUrl.text.toString().trim()
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(requireContext(), "Please enter a valid URL", Toast.LENGTH_SHORT).show()
            return
        }

//        val request = YoutubeDLRequest(emptyList())
        val request = YoutubeDLRequest(url)
        val youtubeDLDir: File = getDownloadLocation()
        val cookie = File(youtubeDLDir, "cookie.txt")

        if (cookieBtn.isActivated() && cookie.exists()) {
            request.addOption("--cookies", cookie.absolutePath)
        } else {
//            request.addOption("-f", "b")
//            request.addOption("--downloader", "libaria2c.so")
            request.addOption("--no-mtime")
//            request.addOption("--external-downloader-args", "aria2c:\"--summary-interval=1\"")
            request.addOption("-o", youtubeDLDir.absolutePath + "/%(uploader)s_%(id)s.%(ext)s")
        }
        showStart()

        downloading = true
        val disposable = Observable.fromCallable {
            YoutubeDL.getInstance().execute(request, processId, callback)
        }.subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ youtubeDLResponse ->
                pbLoading.visibility = View.GONE
                progressBar.progress = 100
                textViewStatus.visibility = View.VISIBLE
                textViewStatus.text = "Download Complete"
                // Log the command output for debugging
                Log.d("MyApp", "Command Output: ${youtubeDLResponse.out}")
                Toast.makeText(requireContext(), "Download successful", Toast.LENGTH_LONG).show()
                downloading = false
            }, { e ->
                pbLoading.visibility = View.GONE
                textViewStatus.visibility = View.VISIBLE
                textViewStatus.text = "Download Failed"
                // Log the error for debugging
                Log.e("MyApp", "Download Failed: ${e.message}", e)
                Toast.makeText(requireContext(), "Download failed. Check the logs for more details.", Toast.LENGTH_LONG).show()
                downloading = false
            })

        compositeDisposable.add(disposable)

    }

    private fun showStart() {
        textViewStatus.visibility = View.VISIBLE
        textViewStatus.text = "starting"
        progressBar.setProgress(0)
        pbLoading.visibility = View.VISIBLE
    }

    private fun getDownloadLocation(): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val youtubeDLDir = File(downloadsDir, "youtubedl-android/Videos")
        if (!youtubeDLDir.exists()) youtubeDLDir.mkdir()
        Log.d("LOCATOIn", "${youtubeDLDir}")
        return youtubeDLDir
    }

    private fun isStoragePermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //for api < 29 (android 10+)
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                return true
            } else {
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
                return false
            }
        } else {
            return true
        }
    }

    override fun onDestroy() {
        compositeDisposable.dispose()
        super.onDestroy()
    }
}