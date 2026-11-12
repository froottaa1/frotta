package com.frotta

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import java.io.File

data class MediaItem(
    val uri: Uri,
    val name: String,
    val folder: String,
    val size: Long,
    val modified: Long
)

data class MediaFolder(
    val path: String,
    val label: String,
    val count: Int
)

object MediaReader {

    private fun collection(kind: String): Uri = when (kind) {
        "photos" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        "videos" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        else -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }

    fun hasAccess(context: Context, kind: String): Boolean {
        val perm = if (Build.VERSION.SDK_INT >= 33) {
            when (kind) {
                "photos" -> Manifest.permission.READ_MEDIA_IMAGES
                "videos" -> Manifest.permission.READ_MEDIA_VIDEO
                else -> Manifest.permission.READ_MEDIA_AUDIO
            }
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, perm) ==
            PackageManager.PERMISSION_GRANTED
    }

    @Suppress("DEPRECATION")
    fun readAll(context: Context, kind: String): List<MediaItem> {
        val base = collection(kind)
        val pathColumn = if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.MediaColumns.RELATIVE_PATH
        } else {
            MediaStore.MediaColumns.DATA
        }
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            pathColumn
        )
        val root = Environment.getExternalStorageDirectory().absolutePath
        val list = ArrayList<MediaItem>()
        try {
            context.contentResolver.query(base, projection, null, null, null)?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val modCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                val pathCol = c.getColumnIndexOrThrow(pathColumn)
                while (c.moveToNext()) {
                    val name = c.getString(nameCol) ?: continue
                    val raw = c.getString(pathCol) ?: ""
                    val folder = if (Build.VERSION.SDK_INT >= 29) {
                        raw
                    } else {
                        (File(raw).parent ?: "").removePrefix(root)
                    }.replace('\\', '/').trim('/')
                    list.add(
                        MediaItem(
                            ContentUris.withAppendedId(base, c.getLong(idCol)),
                            name,
                            folder,
                            c.getLong(sizeCol),
                            c.getLong(modCol) * 1000L
                        )
                    )
                }
            }
        } catch (e: Exception) {
        }
        return list
    }

    fun folders(items: List<MediaItem>): List<MediaFolder> {
        return items.groupBy { it.folder }
            .map { (path, list) ->
                MediaFolder(
                    path,
                    path.substringAfterLast('/').ifEmpty { "الرئيسي" },
                    list.size
                )
            }
            .sortedByDescending { it.count }
    }
}
