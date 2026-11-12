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

object DocsReader {

    val extensions = setOf(
        "pdf", "txt", "rtf", "csv", "json", "xml", "html", "htm", "md", "log",
        "doc", "docx", "docm", "odt",
        "xls", "xlsx", "xlsm", "ods",
        "ppt", "pptx", "odp",
        "dwg", "dxf", "dwf", "dgn", "rvt", "skp", "stl", "step", "stp", "iges",
        "zip", "rar", "7z", "tar", "gz",
        "epub", "mobi", "vcf", "ics",
        "psd", "ai", "eps", "svg", "key", "pages", "numbers",
        "ttf", "otf", "bak", "db", "sql", "xps"
    )

    @Suppress("DEPRECATION")
    private fun canWalk(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 30) return Environment.isExternalStorageManager()
        if (Build.VERSION.SDK_INT >= 29) return false
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    @Suppress("DEPRECATION")
    private fun walk(): List<MediaItem> {
        val root = Environment.getExternalStorageDirectory()
        val rootPath = root.absolutePath
        val out = ArrayList<MediaItem>()
        val stack = ArrayDeque<File>()
        stack.addLast(root)

        while (stack.isNotEmpty() && out.size < 30000) {
            val dir = stack.removeLast()
            val children: Array<File>? = try {
                dir.listFiles()
            } catch (e: Exception) {
                null
            }
            if (children == null) continue

            for (f in children) {
                val name = f.name
                if (f.isDirectory) {
                    if (name.startsWith(".")) continue
                    if (dir == root && name.equals("Android", true)) {
                        val media = File(f, "media")
                        if (media.isDirectory) stack.addLast(media)
                        continue
                    }
                    stack.addLast(f)
                } else {
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (ext in extensions) {
                        val folder = (f.parent ?: "")
                            .removePrefix(rootPath)
                            .replace('\\', '/')
                            .trim('/')
                        out.add(
                            MediaItem(
                                Uri.fromFile(f),
                                name,
                                folder,
                                f.length(),
                                f.lastModified()
                            )
                        )
                    }
                }
            }
        }
        return out
    }

    @Suppress("DEPRECATION")
    private fun queryStore(context: Context): List<MediaItem> {
        val out = ArrayList<MediaItem>()
        val base = MediaStore.Files.getContentUri("external")
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
        try {
            context.contentResolver.query(
                base,
                projection,
                MediaStore.Files.FileColumns.MEDIA_TYPE + "=" +
                    MediaStore.Files.FileColumns.MEDIA_TYPE_NONE,
                null,
                null
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val modCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                val pathCol = c.getColumnIndexOrThrow(pathColumn)
                while (c.moveToNext()) {
                    val name = c.getString(nameCol) ?: continue
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (ext !in extensions) continue
                    val raw = c.getString(pathCol) ?: ""
                    val folder = if (Build.VERSION.SDK_INT >= 29) {
                        raw
                    } else {
                        (File(raw).parent ?: "").removePrefix(root)
                    }.replace('\\', '/').trim('/')
                    out.add(
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
        return out
    }

    fun readDocs(context: Context): List<MediaItem> {
        val list = try {
            if (canWalk(context)) walk() else queryStore(context)
        } catch (e: Exception) {
            emptyList()
        }
        return list.sortedByDescending { it.modified }
    }
}
