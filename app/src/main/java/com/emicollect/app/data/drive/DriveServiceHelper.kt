package com.emicollect.app.data.drive

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

data class DriveFile(
    val id: String,
    val name: String,
    val modifiedTime: com.google.api.client.util.DateTime?
)

@Singleton
class DriveServiceHelper @Inject constructor(
    private val database: com.emicollect.app.data.local.AppDatabase
) {

    private fun getDriveService(context: Context, account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, Collections.singleton(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account

        return Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("EMI Collect App")
            .build()
    }

    suspend fun createBackupFolder(context: Context, account: GoogleSignInAccount): String? = withContext(Dispatchers.IO) {
        val driveService = getDriveService(context, account)
        
        // Check if folder exists
        val query = "mimeType='application/vnd.google-apps.folder' and name='Emix_Backups' and trashed=false"
        val fileList = driveService.files().list().setQ(query).setSpaces("drive").execute()
        
        if (fileList.files.isNotEmpty()) {
            return@withContext fileList.files[0].id
        }
        
        // Create folder if not exists
        val folderMetadata = com.google.api.services.drive.model.File()
        folderMetadata.name = "Emix_Backups"
        folderMetadata.mimeType = "application/vnd.google-apps.folder"
        
        val folder = driveService.files().create(folderMetadata)
            .setFields("id")
            .execute()
            
        return@withContext folder.id
    }

    private fun getLocalDatabaseFile(context: Context): File {
        return context.getDatabasePath(com.emicollect.app.data.local.AppDatabase.DATABASE_NAME)
    }

    suspend fun uploadDatabase(context: Context, account: GoogleSignInAccount, dbFile: File? = null): String? = withContext(Dispatchers.IO) {
        // Use standard file if not provided
        val localFile = dbFile ?: getLocalDatabaseFile(context)

        // 1. Force Checkpoint (Aggressive Flush)
        // Ensure all WAL data is merged into the main .db file
        try {
            val supportDb = database.openHelper.writableDatabase
            supportDb.query("PRAGMA wal_checkpoint(FULL)").close()
            
            // Temporary disable WAL to force everything into the main file
            supportDb.disableWriteAheadLogging()
            
            // Re-enable it immediately after (optional, but good practice if app continues running)
            supportDb.enableWriteAheadLogging()
            
            // Short sleep to allow the OS to finish the file operation
            kotlinx.coroutines.delay(1000)
            
            val size = localFile.length()
            android.util.Log.d("BACKUP_DEBUG", "Database File size before upload: $size bytes")
            if (size <= 4096) {
                android.util.Log.w("BACKUP_DEBUG", "Warning: Database file seems too small ($size bytes). Backup might be empty.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("BACKUP_DEBUG", "Failed to flush WAL: ${e.message}")
        }

        // 2. Verify File
        if (!localFile.exists()) throw Exception("Database file does not exist locally")
        if (localFile.length() <= 0) throw Exception("Local Database is empty (0 bytes). Try checkpointing.")

        // 3. Prepare Filename
        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
        val fileName = "emix_backup_$timestamp.db"

        val folderId = createBackupFolder(context, account) ?: return@withContext null
        val driveService = getDriveService(context, account)
        
        // 4. Rotation Logic (Keep Max 5)
        val query = "'$folderId' in parents and name contains 'emix_backup_' and trashed=false"
        val existingFiles = driveService.files().list()
            .setQ(query)
            .setOrderBy("createdTime desc") // Newest first
            .setFields("files(id, name, createdTime)")
            .execute()
            .files

        // If we have 5 or more, delete the oldest ones until we have 4 (so we can add the 5th)
        if (existingFiles.size >= 5) {
            val toKeep = existingFiles.take(4) // Keep 4 newest
            val toDelete = existingFiles.drop(4) // Delete the rest
             
            for (file in toDelete) {
                 try {
                     driveService.files().delete(file.id).execute()
                     android.util.Log.d("BACKUP_ROTATION", "Deleted old backup: ${file.name}")
                 } catch (e: Exception) {
                     e.printStackTrace()
                 }
            }
        }

        // 5. Upload New File
        val fileMetadata = com.google.api.services.drive.model.File()
        fileMetadata.name = fileName
        fileMetadata.parents = Collections.singletonList(folderId)
        
        val inputStream = java.io.FileInputStream(localFile)
        val mediaContent = com.google.api.client.http.InputStreamContent("application/x-sqlite3", inputStream)
        mediaContent.length = localFile.length()

        val fileId = driveService.files().create(fileMetadata, mediaContent).setFields("id").execute().id
            
        inputStream.close()
        return@withContext fileId
    }

    suspend fun listBackups(context: Context, account: GoogleSignInAccount): List<DriveFile> = withContext(Dispatchers.IO) {
        val folderId = createBackupFolder(context, account) ?: return@withContext emptyList()
        val driveService = getDriveService(context, account)
        
        val query = "'$folderId' in parents and mimeType!='application/vnd.google-apps.folder' and trashed=false"
        val result = driveService.files().list()
            .setQ(query)
            .setOrderBy("modifiedTime desc")
            .setFields("files(id, name, modifiedTime)")
            .execute()
            
        return@withContext result.files.map { DriveFile(it.id, it.name, it.modifiedTime) }
    }

    suspend fun downloadBackup(context: Context, account: GoogleSignInAccount, fileId: String, targetFile: File? = null) = withContext(Dispatchers.IO) {
        val driveService = getDriveService(context, account)
        
        // 1. Download to Temp
        val tempFile = File(context.cacheDir, "restore_temp.db")
        if (tempFile.exists()) tempFile.delete()
        
        val outputStream = FileOutputStream(tempFile)
        driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
        outputStream.flush()
        outputStream.close()
        
        // 2. Verify Size
        if (tempFile.length() <= 0) {
            throw Exception("Downloaded backup is empty!")
        }
        
        // 3. Overwrite Target (Clean Slate)
        val finalTarget = targetFile ?: getLocalDatabaseFile(context)
        
        // Delete Main DB
        if (finalTarget.exists()) {
            finalTarget.delete()
        }

        // Delete WAL and SHM (Critical: Delete BEFORE copy to avoid mismatch)
        val walFile = File(finalTarget.parent, "${finalTarget.name}-wal")
        val shmFile = File(finalTarget.parent, "${finalTarget.name}-shm")
        if (walFile.exists()) walFile.delete()
        if (shmFile.exists()) shmFile.delete()

        // Copy new DB
        tempFile.copyTo(finalTarget, overwrite = true)
        tempFile.delete()
    }

    fun getDatabaseFile(context: Context): File? {
        // Deprecated: Use getLocalDatabaseFile internally
        return getLocalDatabaseFile(context)
    }

    /**
     * Returns true if at least one backup file exists in the user's Google Drive Emix_Backups folder.
     * Used by the Restore-First Safety Protocol.
     */
    suspend fun checkForExistingBackup(context: Context, account: GoogleSignInAccount): Boolean {
        return listBackups(context, account).isNotEmpty()
    }
}
