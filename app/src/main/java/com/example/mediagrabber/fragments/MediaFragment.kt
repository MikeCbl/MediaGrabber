package com.example.mediagrabber.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mediagrabber.PlayerActivity
import com.example.mediagrabber.R
import com.example.mediagrabber.adapters.VideoAdapter
import com.example.mediagrabber.databinding.FragmentVideosBinding
import com.example.mediagrabber.models.MediaModel
import com.example.mediagrabber.utils.SwipeToDeleteCallback
import com.example.mediagrabber.utils.SwipeToShareCallback
import com.google.common.io.Files.getFileExtension
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaFragment : Fragment() {

    private lateinit var binding: FragmentVideosBinding
    private lateinit var videoList: ArrayList<MediaModel>
    private lateinit var adapter: VideoAdapter
    private lateinit var sharedPreferences: SharedPreferences
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =  inflater.inflate(R.layout.fragment_videos, container, false)
        val binding = FragmentVideosBinding.bind(view)

//        videoList = getAllVideos()


        // Specify the directory you want to use
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val youtubeDLDir = File(downloadsDir, "youtubedl-android/Videos")

//        sharedPreferences = requireActivity().getSharedPreferences("FolderPref", Activity.MODE_PRIVATE)
//        val selectedLocation = sharedPreferences.getString("selected_location", "")

        videoList = getVideosFromDirectory(youtubeDLDir)

        binding.rvVideo.setHasFixedSize(true)
        binding.rvVideo.setItemViewCacheSize(3) //Number of views to cache offscreen before returning them to the general recycled view pool
        binding.rvVideo.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVideo.adapter = VideoAdapter(requireContext(), videoList)


        //Update visibility no records? show text
        if(videoList.isEmpty()){
            binding.rvVideo.visibility = View.GONE
            binding.tvNoRecordsAvailable.visibility = View.VISIBLE
        }else{
            binding.rvVideo.visibility = View.VISIBLE
            binding.tvNoRecordsAvailable.visibility = View.GONE
        }



        //swipe to delete
        val deleteSwipeHandler = object : SwipeToDeleteCallback(requireContext()) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                askPermission()
                Log.d("log","message")
                val adapter = binding.rvVideo.adapter as VideoAdapter
                adapter.removeAt(viewHolder.adapterPosition)
            }
        }
        val deleteItemTouchHelper = ItemTouchHelper(deleteSwipeHandler)
        deleteItemTouchHelper.attachToRecyclerView(binding.rvVideo)


        //swipe to share
        var shareSwipeHandler = object : SwipeToShareCallback(requireContext()) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = binding.rvVideo.adapter as VideoAdapter
                Log.d("Adapter", "${adapter}")
                adapter.shareMedia(requireContext(),viewHolder.adapterPosition)
                adapter.notifyItemChanged(viewHolder.adapterPosition)

            }
        }

        val shareItemTouchHelper = ItemTouchHelper(shareSwipeHandler)
        shareItemTouchHelper.attachToRecyclerView(binding.rvVideo)

//        Log.d("getVid", "${getAllVideos()}")
        return view
    }


    private fun askPermission(){
        Dexter.withActivity(requireActivity()).withPermissions(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        ).withListener(object: MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport?)
            {
                if(report!!.areAllPermissionsGranted()){
                    Toast.makeText(requireContext(), "Posaida wszelkie uprawnienia", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>, token: PermissionToken)
            {
                showRationalDialogForPermisions()
            }
        }).onSameThread().check()
    }

    private fun showRationalDialogForPermisions(){
        AlertDialog.Builder(requireContext()).setMessage("WYPIERDALAJ PO UPRAWNIENIA")
            .setPositiveButton("G0 TO SETTINGS")
            { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", requireContext().packageName, null)
                    intent.data = uri
                    startActivity(intent)

                }catch (e: ActivityNotFoundException){
                    e.printStackTrace()
                }
            }.setNegativeButton("Cancel"){dialog, _ ->
                dialog.dismiss()
            }.show()
    }


    @SuppressLint("Range")
    private fun getAllVideos(): ArrayList<MediaModel> {
        val tempList = ArrayList<MediaModel>()

        val projection = arrayOf(
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.DESCRIPTION
        )
        val selection = "${MediaStore.Video.Media.BUCKET_DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf("youtubedl-android")

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val youtubeDLDir = File(downloadsDir, "youtubedl-android")

        context?.contentResolver?.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Video.Media.DATA} LIKE ?",
            arrayOf("${youtubeDLDir.path}%"),
            MediaStore.Video.Media.DATE_ADDED + " DESC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val titleC = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)) ?: "Unknown"
                val idC = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)) ?: "Unknown"
                val folderC = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)) ?: "Internal Storage"
                val sizeC = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE))
                val pathC = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)) ?: "Unknown"
                val durationC = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
                val bucketIdC = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID))
                val dateAddedC = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)) ?: "0"

                val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                    Date(dateAddedC.toLong() * 1000)
                )

                val fileExtension = getFileExtension(titleC)

                val descriptionText = "${formattedDate}, $fileExtension"

                try {
                    val file = File(pathC)
                    val artUriC = Uri.fromFile(file)

                    val video = MediaModel(
                        id = idC.toInt(),
                        title = titleC,
                        path = pathC,
                        description = descriptionText,
                        folderName = folderC,
                        duration = durationC,
                        size = sizeC.toString(),
                        artUri = artUriC,
                        bucketId = bucketIdC,
                        dateAdded = dateAddedC,
                        fileExtension = fileExtension
                    )

                    // Add all files to tempList first
                    tempList.add(video)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
                // Add additional logs
                Log.d("VideoDetails", "Title: $titleC, Path: $pathC, Size: $sizeC, Duration: $durationC")

            }
        }

        // Log the list of files obtained from getAllVideos()
        Log.d("getAllVideos", "List from getAllVideos(): $tempList")

        // Filter out non-existing files
//        val filteredList = tempList.filter { video ->
//            val file = File(video.path)
//            file.exists()
//        }

        // Log the filtered list
//        Log.d("getAllVideos", "Filtered list: $filteredList")

//        return ArrayList(filteredList)
        return  ArrayList(tempList)
    }




    private fun getVideosFromDirectory(directory: File): ArrayList<MediaModel> {
        val tempList = ArrayList<MediaModel>()

        val files = directory.listFiles()?.filter { it.isFile }
        files?.forEach { file ->
            val title = file.name
            val id = file.hashCode() // You can use a unique identifier based on your requirements
            val folderName = directory.name
            val size = file.length().toString()
            val path = file.absolutePath
            val duration = getVideoDuration(file)
            val dateAdded = file.lastModified().toString()
            val fileExtension = getFileExtension(title)


            val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(dateAdded.toLong()))

            val descriptionText = "${formattedDate}, $fileExtension"

            val artUri = Uri.fromFile(file)

            val video = MediaModel(
                id = id,
                title = title,
                path = path,
                description = descriptionText,
                folderName = folderName,
                duration = duration,
                size = size,
                artUri = artUri,
                bucketId = "", // Replace with the correct value from your model
                dateAdded = dateAdded,
                fileExtension = fileExtension
                // Include other fields based on your model
            )

            tempList.add(video)
        }

        return tempList
    }

    private fun getVideoDuration(file: File): Long {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)

            val durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationString?.toLong() ?: 0L

            retriever.release()

            return duration
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return 0L
    }

//    @SuppressLint("Recycle", "Range")
//    private fun getAllVideos(): ArrayList<MediaModel> {
//        val tempList = ArrayList<MediaModel>()
//
//        val projection = arrayOf(
//            MediaStore.Video.Media.DISPLAY_NAME,
//            MediaStore.Video.Media.SIZE,
//            MediaStore.Video.Media._ID,
//            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
//            MediaStore.Video.Media.DATA,
//            MediaStore.Video.Media.DATE_ADDED,
//            MediaStore.Video.Media.DURATION,
//            MediaStore.Video.Media.BUCKET_ID,
//            MediaStore.Video.Media.DESCRIPTION  // Include DESCRIPTION column if used
//        )
//        val selection = "${MediaStore.Video.Media.BUCKET_DISPLAY_NAME} = ?"
//        val selectionArgs = arrayOf("youtubedl-android")
//
//        val cursor = context?.contentResolver?.query(
//            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
//            projection,
//            selection,
//            selectionArgs,
//            MediaStore.Video.Media.DATE_ADDED + " DESC"
//        )
//
//
//
//        if (cursor != null) {
//            if (cursor.moveToNext()) {
//                do {
//                    val titleC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)) ?: "Unknown"
//                    val idC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media._ID)) ?: "Unknown"
//                    val folderC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)) ?: "Internal Storage"
//                    val sizeC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.SIZE)) ?: "0"
//                    val pathC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA)) ?: "Unknown"
//                    val durationC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DURATION))?.toLong() ?: 0L
//                    val bucketIdC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID))
//
//                    val dateAddedC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)) ?: "0"
//
////                    val dateTakenC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN)) ?: "0"  // Added for creation date
//
//
//                    // Combine date and file extension in the description
//                    val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
//                        Date(dateAddedC.toLong() * 1000) // Convert to milliseconds
//                    )
//
//                    val fileExtension = getFileExtension(titleC)  // Function to extract file extension
//
//                    val descriptionText = "${formattedDate}, $fileExtension"
//
//                    try {
//                        val file = File(pathC)
//                        val artUriC = Uri.fromFile(file)
//
//                        val video = MediaModel(
//                            id = idC.toInt(),
//                            title = titleC,
//                            path = pathC,
//                            description = descriptionText,
//                            folderName = folderC,
//                            duration = durationC,
//                            size = sizeC,
//                            artUri = artUriC,
//                            bucketId = bucketIdC,
//                            dateAdded = dateAddedC,
//                            fileExtension = fileExtension  // Set the file extension
//                        )
//
//                        if (file.exists()) tempList.add(video)
//
//                    } catch (e: Exception) {
//                        e.printStackTrace()
//                    }
//                } while (cursor.moveToNext())
//
//                cursor.close()
//            }
//        }
//        return tempList
//    }
}
