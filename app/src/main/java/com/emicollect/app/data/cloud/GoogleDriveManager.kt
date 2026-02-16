package com.emicollect.app.data.cloud

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Collections
import javax.inject.Inject

class GoogleDriveManager @Inject constructor() {

    private fun getDriveService(context: Context, account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, Collections.singleton(DriveScopes.DRIVE_APPDATA)
        )
        credential.selectedAccount = account.account
        
        return Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("EMI Collect App").build()
    }

    suspend fun syncToCloud(context: Context, account: GoogleSignInAccount, json: String) {
        withContext(Dispatchers.IO) {
            val validJson = if (json.isBlank()) "{}" else json
            val driveService = getDriveService(context, account)
            
            // 1. Check if backup file exists
            val fileList = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = 'emi_backup.json' and trashed = false")
                .setFields("files(id)")
                .execute()
            
            val fileId = fileList.files.firstOrNull()?.id
            
            val fileMetadata = com.google.api.services.drive.model.File()
            fileMetadata.name = "emi_backup.json"
            fileMetadata.parents = Collections.singletonList("appDataFolder")
            
            val mediaContent = ByteArrayContent.fromString("application/json", validJson)
            
            if (fileId == null) {
                // Create new
                driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute()
            } else {
                // Update existing
                driveService.files().update(fileId, null, mediaContent)
                    .execute()
            }
        }
    }

    suspend fun fetchLatestCloudBackup(context: Context, account: GoogleSignInAccount): String? {
        return withContext(Dispatchers.IO) {
            val driveService = getDriveService(context, account)
            
            val fileList = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = 'emi_backup.json' and trashed = false")
                .setFields("files(id)")
                .execute()
            
            val fileId = fileList.files.firstOrNull()?.id ?: return@withContext null
            
            val inputStream = driveService.files().get(fileId).executeMediaAsInputStream()
            val reader = BufferedReader(InputStreamReader(inputStream))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line)
            }
            sb.toString()
        }
    }
}
