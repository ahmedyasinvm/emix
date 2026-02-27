package com.emicollect.app.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emicollect.app.data.local.UserPreferencesRepository
import com.emicollect.app.data.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val backupRepository: BackupRepository,
    private val googleAuthManager: com.emicollect.app.auth.GoogleAuthManager,
    private val driveServiceHelper: com.emicollect.app.data.drive.DriveServiceHelper
) : ViewModel() {

    // ── Snackbar event channel ──
    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    // ── Auth State ──
    private val _currentUserEmail = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val currentUserEmail: StateFlow<String?> = _currentUserEmail.asStateFlow()

    // Read from Preds
    val isAutoBackupEnabled: StateFlow<Boolean> = userPreferencesRepository.isAutoBackupEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val defaultAmount: StateFlow<String> = userPreferencesRepository.defaultCollectionAmount
        .map { it.toString() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val collectionSchedule: StateFlow<com.emicollect.app.data.local.CollectionSchedule> =
        userPreferencesRepository.collectionSchedule
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
                com.emicollect.app.data.local.CollectionSchedule(1, 1, 1))

    val businessName: StateFlow<String> = userPreferencesRepository.businessName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val contactNumber: StateFlow<String> = userPreferencesRepository.contactNumber
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val isBiometricEnabled: StateFlow<Boolean> = userPreferencesRepository.isBiometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isWhatsAppEnabled: StateFlow<Boolean> = userPreferencesRepository.isWhatsAppEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isDarkMode: StateFlow<Boolean> = userPreferencesRepository.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        checkAutoBackupStatus()
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setDarkMode(enabled) }
    }

    fun checkSignedInUser(context: android.content.Context) {
        val account = googleAuthManager.getLastSignedInAccount(context)
        _currentUserEmail.value = account?.email
    }

    fun updateDefaultAmount(amount: String) {
        val value = amount.toDoubleOrNull()
        if (value != null) {
            viewModelScope.launch { userPreferencesRepository.saveDefaultCollectionAmount(value) }
        }
    }

    fun updateWeeklyDay(day: Int) {
        viewModelScope.launch { userPreferencesRepository.saveWeeklyDay(day) }
    }

    fun updateMonthlySchedule(weekNum: Int, day: Int) {
        viewModelScope.launch { userPreferencesRepository.saveMonthlySchedule(weekNum, day) }
    }

    fun updateBusinessName(name: String) {
        viewModelScope.launch { userPreferencesRepository.saveBusinessName(name) }
    }

    fun updateContactNumber(number: String) {
        viewModelScope.launch { userPreferencesRepository.saveContactNumber(number) }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setBiometricEnabled(enabled) }
    }

    fun setWhatsAppEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setWhatsAppEnabled(enabled) }
    }

    // ═══════════════════════════════════════════
    // CLOUD BACKUP & AUTH
    // ═══════════════════════════════════════════

    fun getSignInIntent(context: android.content.Context) = googleAuthManager.getSignInIntent(context)

    fun handleSignInResult(intent: android.content.Intent?) {
        viewModelScope.launch {
            try {
                val account = googleAuthManager.handleSignInResult(intent)
                _currentUserEmail.value = account.email
                _snackbarMessage.emit("Signed in as ${account.email}.")
            } catch (e: com.google.android.gms.common.api.ApiException) {
                val code = e.statusCode
                android.util.Log.e("APP_IDENTITY", "Sign-In Failed Code: $code")
                when (code) {
                    10 -> _snackbarMessage.emit("Sign-in failed: SHA-1 fingerprint mismatch (code 10). Check Google Cloud configuration.")
                    12500 -> _snackbarMessage.emit("Sign-in unavailable. Check Play Services and network connection (code 12500).")
                    else -> _snackbarMessage.emit("Sign-in failed (code $code).")
                }
                e.printStackTrace()
            } catch (e: Exception) {
                _snackbarMessage.emit("Sign-in failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun signOut(context: android.content.Context) {
        googleAuthManager.signOut(context) {
            _currentUserEmail.value = null
            viewModelScope.launch { _snackbarMessage.emit("Signed out from Google Drive.")
            }
            scheduleDailyBackup(context, false)
        }
    }

    fun backupToDrive(context: android.content.Context) {
        viewModelScope.launch {
            val account = googleAuthManager.getLastSignedInAccount(context)
            if (account == null) {
                _snackbarMessage.emit("Please sign in with Google to enable cloud backup.")
                return@launch
            }

            try {
                _snackbarMessage.emit("Uploading backup to Google Drive...")
                val fileId = driveServiceHelper.uploadDatabase(context, account)
                if (fileId != null) {
                    _snackbarMessage.emit("Backup uploaded successfully.")
                } else {
                    _snackbarMessage.emit("Backup upload failed. Please try again.")
                }
            } catch (e: Exception) {
                _snackbarMessage.emit("Backup failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun restoreFromDrive(context: android.content.Context) {
        viewModelScope.launch {
            val account = googleAuthManager.getLastSignedInAccount(context)
            if (account == null) {
                _snackbarMessage.emit("Please sign in with Google to restore from cloud.")
                return@launch
            }

            try {
                _snackbarMessage.emit("Searching for cloud backups...")
                val backups = driveServiceHelper.listBackups(context, account)
                if (backups.isEmpty()) {
                    _snackbarMessage.emit("No backups found in the Emix_Backups folder.")
                    return@launch
                }

                val latest = backups.first()
                _snackbarMessage.emit("Restoring backup: ${latest.name}...")
                driveServiceHelper.downloadBackup(context, account, latest.id)
                _snackbarMessage.emit("RESTART_NEEDED")
            } catch (e: Exception) {
                _snackbarMessage.emit("Restore failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    // ═══════════════════════════════════════════
    // AUTO BACKUP (WORKMANAGER)
    // ═══════════════════════════════════════════

    fun scheduleDailyBackup(context: android.content.Context, enabled: Boolean) {
        // Save to Prefs
        viewModelScope.launch {
            userPreferencesRepository.setAutoBackupEnabled(enabled)
        }
        
        val workManager = androidx.work.WorkManager.getInstance(context)
        val workName = "DailyBackup"

        if (enabled) {
            val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
            
            val request = androidx.work.PeriodicWorkRequestBuilder<com.emicollect.app.workers.AutoBackupWorker>(
                24, java.util.concurrent.TimeUnit.HOURS
            )
            .setConstraints(constraints)
            .build()

            workManager.enqueueUniquePeriodicWork(
            workName,
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            request
            )
            viewModelScope.launch { _snackbarMessage.emit("Daily auto-backup scheduled.") }
        } else {
            workManager.cancelUniqueWork(workName)
            viewModelScope.launch { _snackbarMessage.emit("Daily auto-backup disabled.") }
        }
    }
    
    private fun checkAutoBackupStatus() {
        // Now handled by flow
    }

    // ═══════════════════════════════════════════
    // LOCAL DATA MANAGEMENT
    // ═══════════════════════════════════════════

    fun backupDatabase(context: android.content.Context) {
        viewModelScope.launch {
            try {
                val json = backupRepository.createBackupJson()
                val fileName = "emi_backup_${System.currentTimeMillis()}.json"

                withContext(Dispatchers.IO) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        val contentValues = android.content.ContentValues().apply {
                            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/json")
                            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                        }
                        val resolver = context.contentResolver
                        val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                        uri?.let {
                            resolver.openOutputStream(it)?.use { stream ->
                                stream.write(json.toByteArray())
                            }
                        }
                    } else {
                        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                        val file = java.io.File(downloadsDir, fileName)
                        file.writeText(json)
                    }
                }
                _snackbarMessage.emit("Backup saved to Downloads folder.")
            } catch (e: Exception) {
                _snackbarMessage.emit("Backup failed: ${e.message}")
            }
        }
    }

    fun restoreDatabase(context: android.content.Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                        ?: throw Exception("Could not read file")
                }
                backupRepository.restoreBackup(json)
                _snackbarMessage.emit("RESTART_NEEDED")
            } catch (e: Exception) {
                _snackbarMessage.emit("Restore failed: ${e.message}")
            }
        }
    }

    fun exportToExcel(context: android.content.Context) {
        viewModelScope.launch {
            try {
                _snackbarMessage.emit("Generating Excel file...")
                val json = backupRepository.createBackupJson()
                val backupData = com.google.gson.Gson().fromJson(json, com.emicollect.app.data.model.BackupData::class.java)

                val file = com.emicollect.app.utils.ExportUtils.generateExcelFile(context, backupData)

                if (file != null && file.exists()) {
                    withContext(Dispatchers.IO) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            val contentValues = android.content.ContentValues().apply {
                                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                            }
                            val resolver = context.contentResolver
                            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                            uri?.let {
                                resolver.openOutputStream(it)?.use { stream ->
                                    file.inputStream().copyTo(stream)
                                }
                            }
                        } else {
                            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                            val destFile = java.io.File(downloadsDir, file.name)
                            file.copyTo(destFile, overwrite = true)
                        }
                    }
                    _snackbarMessage.emit("Excel file exported to Downloads/${file.name}")
                } else {
                    _snackbarMessage.emit("Export failed: could not generate the file.")
                }
            } catch (e: Exception) {
                _snackbarMessage.emit("Export failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
