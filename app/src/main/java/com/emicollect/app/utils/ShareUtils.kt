package com.emicollect.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

object ShareUtils {
    fun shareImage(context: Context, uri: Uri, subject: String = "App Receipt") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Receipt"))
    }
}
