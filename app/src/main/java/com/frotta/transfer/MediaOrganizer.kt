package com.frotta.transfer

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

data class FrottaMediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val albumName: String, // اسم المجلد الأصلي (Camera, WhatsApp, etc.)
    val dateAdded: Long     // للحفاظ على الترتيب الزمني
)

object MediaOrganizer {

    fun getDeviceMedia(context: Context): List<FrottaMediaItem> {
        val mediaList = mutableListOf<FrottaMediaItem>()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED
        )
        
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: "Unknown"
                val albumName = cursor.getString(bucketCol) ?: "Pictures"
                val dateAdded = cursor.getLong(dateCol)

                val uri = ContentUris.withAppendedId(collection, id)
                mediaList.add(FrottaMediaItem(id, uri, name, albumName, dateAdded))
            }
        }
        return mediaList
    }
}
