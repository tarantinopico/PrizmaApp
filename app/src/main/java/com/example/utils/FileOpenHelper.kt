package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.DownloadEntity
import java.io.File

object FileOpenHelper {
    fun openDownloadedFile(context: Context, download: DownloadEntity) {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), download.fileName)
        if (file.exists()) {
            try {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                val mimeType = download.mimeType.takeIf { it.isNotBlank() } ?: "*/*"
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                
                // Create chooser intent
                val chooser = Intent.createChooser(intent, "Otevřít pomocí")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                
                context.startActivity(chooser)
            } catch (e: Exception) {
                Toast.makeText(context, "Nelze najít aplikaci k otevření tohoto souboru.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Soubor již neexistuje.", Toast.LENGTH_SHORT).show()
        }
    }
}
