package com.emicollect.app.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.emicollect.app.auth.GoogleAuthManager
import com.emicollect.app.data.drive.DriveServiceHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// Using manual injection pattern since HiltWorker setup might be complex without specific gradle setup
// But given the environment, we'll try standard Hilt injection with EntryPoint if HiltWorker fails, 
// OR we can just use @EntryPoint.
// Let's use the EntryPoint approach to be safe and robust without extra dependencies.

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class AutoBackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AutoBackupEntryPoint {
        fun getDriveServiceHelper(): DriveServiceHelper
        fun getGoogleAuthManager(): GoogleAuthManager
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val appContext = applicationContext
        
        // Manual injection
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            AutoBackupEntryPoint::class.java
        )
        val driveServiceHelper = entryPoint.getDriveServiceHelper()
        val authManager = entryPoint.getGoogleAuthManager()

        // 1. Silent Check
        val account = GoogleSignIn.getLastSignedInAccount(appContext)
        if (account == null) {
            // Not signed in, retry check later or fail? 
            // If user logged out, we shouldn't keep retrying excessively, but for now FAIL is safer to stop this run.
            return@withContext Result.failure()
        }


        try {
            // 2. Upload
            // DriveServiceHelper handles file retrieval and checkpointing internally
            driveServiceHelper.uploadDatabase(appContext, account)

            // 3. Notify
            showNotification(appContext)
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // If network is flakey, RETRY
            if (runAttemptCount < 3) {
                 Result.retry()
            } else {
                 Result.failure()
            }
        }
    }

    private fun showNotification(context: Context) {
        val channelId = "backup_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Backup Status",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_upload) // Generic icon
            .setContentTitle("Daily Backup Successful")
            .setContentText("Your data has been safely backed up to Google Drive.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(1001, notification)
    }
}
