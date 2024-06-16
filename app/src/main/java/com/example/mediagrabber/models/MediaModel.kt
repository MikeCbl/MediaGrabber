package com.example.mediagrabber.models

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable


data class MediaModel(
    val id: Int,
    val title: String,
    val path: String,
    val description: String,
    val folderName: String,
    val duration: Long,
    val size: String,
    val artUri: Uri, //image
    val bucketId: String,
    val dateAdded: String, //date
    val fileExtension: String  // Added for file extension
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readLong(),
        parcel.readString()!!,
        parcel.readParcelable(Uri::class.java.classLoader)!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(title)
        parcel.writeString(path)
        parcel.writeString(description)
        parcel.writeString(folderName)
        parcel.writeLong(duration)
        parcel.writeString(size)
        parcel.writeParcelable(artUri, flags)
        parcel.writeString(bucketId)
        parcel.writeString(dateAdded)
        parcel.writeString(fileExtension)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MediaModel> {
        override fun createFromParcel(parcel: Parcel): MediaModel {
            return MediaModel(parcel)
        }

        override fun newArray(size: Int): Array<MediaModel?> {
            return arrayOfNulls(size)
        }
    }
}
