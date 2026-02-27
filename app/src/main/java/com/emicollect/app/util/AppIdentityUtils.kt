package com.emicollect.app.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

object AppIdentityUtils {
    fun getAppSignature(context: Context): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }
            
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (signatures == null || signatures.isEmpty()) return "Unknown"

            val md = MessageDigest.getInstance("SHA-1")
            md.update(signatures[0].toByteArray())
            val digest = md.digest()
            val hexString = StringBuilder()
            for (b in digest) {
                hexString.append(String.format("%02X:", b))
            }
            if (hexString.isNotEmpty()) {
                hexString.setLength(hexString.length - 1) // remove last colon
            }
            val sha1 = hexString.toString()
            android.util.Log.d("APP_IDENTITY", "SHA-1: $sha1")
            return sha1
        } catch (e: Exception) {
            e.printStackTrace()
            "Unknown"
        }
    }
}
