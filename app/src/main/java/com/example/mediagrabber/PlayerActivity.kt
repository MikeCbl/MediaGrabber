package com.example.mediagrabber

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore.Video
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.mediagrabber.databinding.ActivityPlayerBinding
import com.example.mediagrabber.fragments.MediaFragment
import com.example.mediagrabber.models.MediaModel

class PlayerActivity : AppCompatActivity() {

    lateinit var binding: ActivityPlayerBinding
    lateinit var player: ExoPlayer
    private lateinit var videoList: ArrayList<MediaModel>
    private var currentPosition: Int = 0

    @androidx.annotation.OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve video list from intent extras
        val videoArrayList: ArrayList<MediaModel>? = intent.getParcelableArrayListExtra("videoArrayList")
        videoList = videoArrayList ?: ArrayList()

        // Retrieve the selected position
        currentPosition = intent.getIntExtra("position", 0)

        if (videoList.isNotEmpty()) {
            player = ExoPlayer.Builder(this).build()
            binding.mediaPlayer.player = player

            val mediaItem = MediaItem.fromUri(videoList[currentPosition].artUri)

            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        } else {
            // Handle the case where the video list is empty or not initialized
            // You may want to show an error message or finish the activity
            // For now, let's just finish the activity
            finish()
        }


    }


    override fun onDestroy() {
        super.onDestroy()
        if (::player.isInitialized) {
            player.release()
        }
    }

}