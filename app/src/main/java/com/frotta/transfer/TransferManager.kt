package com.frotta.transfer

import android.content.Context
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.Socket

object TransferManager {

    // دالة بسيطة لإرسال ملف (سواء صورة أو ملف JSON للرسائل والمكالمات) عبر Socket
    fun sendFileToPeer(ipAddress: String, port: Int, file: File, onProgress: (Long) -> Unit) {
        try {
            val socket = Socket(ipAddress, port)
            val outputStream = DataOutputStream(socket.getOutputStream())
            
            // إرسال اسم وحجم الملف أولاً
            outputStream.writeUTF(file.name)
            outputStream.writeLong(file.length())

            // إرسال محتوى الملف
            FileInputStream(file).use { fileInputStream ->
                val buffer = ByteArray(4096)
                var bytesRead: Int
                var totalSent: Long = 0

                while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalSent += bytesRead.toLong()
                    onProgress(totalSent)
                }
            }
            outputStream.flush()
            socket.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
