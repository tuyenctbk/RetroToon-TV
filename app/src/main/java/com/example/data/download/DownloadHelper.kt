package com.example.data.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.example.R
import com.example.data.model.CartoonItem

class DownloadHelper(private val context: Context) {

    fun downloadCartoon(cartoon: CartoonItem, streamUrl: String) {
        try {
            val cleanFilename = "RetroToon_${cartoon.id.replace(Regex("[^a-zA-Z0-9_]"), "_")}.mp4"
            val request = DownloadManager.Request(Uri.parse(streamUrl)).apply {
                setTitle("RetroToon: ${cartoon.title}")
                setDescription(context.getString(R.string.download_description))
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, cleanFilename)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(false)
            }
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            Toast.makeText(context, context.getString(R.string.downloading_toast, cartoon.title), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.download_failed_toast, e.localizedMessage ?: e.message.orEmpty()), Toast.LENGTH_LONG).show()
        }
    }
}
