package com.example.mediagrabber.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.mediagrabber.PlayerActivity
import com.example.mediagrabber.R
import com.example.mediagrabber.databinding.ItemMediaBinding
import com.example.mediagrabber.models.MediaModel
import java.io.File

class VideoAdapter(private val context: Context, private var videoList: ArrayList<MediaModel>): RecyclerView.Adapter<VideoAdapter.MyHolder>(){

    class MyHolder(binding: ItemMediaBinding): RecyclerView.ViewHolder(binding.root) {
        val title = binding.tvTitle
        val description = binding.tvDescription
        val duration = binding.tvDuration

        val img = binding.ivPlaceImage
        val root = binding.root
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(ItemMediaBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    @SuppressLint("CheckResult")
    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        holder.title.text = videoList[position].title
        holder.description.text = videoList[position].description
        holder.duration.text = DateUtils.formatElapsedTime(videoList[position].duration/1000)



        Glide.with(context)
            .asBitmap()
            .load(videoList[position].artUri)
            .apply(RequestOptions().placeholder(R.mipmap.ic_launcher).centerCrop())
            .into(holder.img)

        holder.root.setOnClickListener {
            val intent = Intent(context, PlayerActivity::class.java)
            intent.putExtra("position", position)
            intent.putExtra("video_title", videoList[position].title)

            val bundle = Bundle()
            bundle.putParcelableArrayList("videoArrayList", ArrayList(videoList))

            intent.putExtras(bundle)

            ContextCompat.startActivity(context, intent, null)
        }
//        holder.root.setOnLongClickListener {
//
//        }

    }


//old one works with getAllVideos() methid
    fun removeAt2(position: Int) {
        val fileId = videoList[position].id


        val contentResolver = context.contentResolver

        // Build the content URI using the file ID
        val contentUri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, fileId.toString())

        try {
            // Delete the file using ContentResolver
            val deletedRows = contentResolver.delete(contentUri, null, null)

            if (deletedRows > 0) {
                // File deletion successful
                Log.d("FileDeletion", "File deleted successfully")

                // Remove the item from the data source
                videoList.removeAt(position)

                // Notify the adapter about the removal
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, itemCount) // This line helps to avoid gaps in the list
            } else {
                // File deletion failed
                Log.e("FileDeletion", "Failed to delete the file")
            }
        } catch (e: SecurityException) {
            // Handle security exceptions
            Log.e("FileDeletion", "SecurityException: ${e.message}")
        } catch (e: Exception) {
            // Handle other exceptions
            Log.e("FileDeletion", "Error deleting file: ${e.message}")
        }
    }

    fun removeAt(position: Int) {
        try {
            val file = File(videoList[position].path)

            if (file.exists()) {
                // File exists, proceed with deletion
                if (file.delete()) {
                    // File deletion successful
                    Log.d("FileDeletion", "File deleted successfully")

                    // Remove the item from the data source
                    videoList.removeAt(position)

                    // Notify the adapter about the removal
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, itemCount) // This line helps to avoid gaps in the list
                } else {
                    // File deletion failed
                    Log.e("FileDeletion", "Failed to delete the file")
                }
            } else {
                // File not found
                Log.e("FileDeletion", "File not found: ${file.absolutePath}")
            }
        } catch (e: SecurityException) {
            // Handle security exceptions
            Log.e("FileDeletion", "SecurityException: ${e.message}")
        } catch (e: Exception) {
            // Handle other exceptions
            Log.e("FileDeletion", "Error deleting file: ${e.message}")
        }
    }

    //SHARE
    fun shareMedia(context: Context, position: Int) {
        val videoFile = File(videoList[position].path)

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "video/*"
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        // Use FileProvider to get a content URI
        val contentUri = FileProvider.getUriForFile(context, "com.example.mediagrabber.provider", videoFile)

        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
        Log.d("dddd", "${contentUri}")

        // Start the chooser
        ContextCompat.startActivity(context, Intent.createChooser(shareIntent, "Sharing Video File!!"), null)
    }


    override fun getItemCount(): Int {
        return videoList.size
    }

}