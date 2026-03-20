package com.emicollect.app.ui.home

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emicollect.app.data.drive.DriveServiceHelper
import com.emicollect.app.data.local.UserPreferencesRepository
import com.emicollect.app.data.model.CustomerWithDebtStatus
import com.emicollect.app.data.model.SortOption
import com.emicollect.app.data.repository.BackupRepository
import com.emicollect.app.data.repository.CollectionRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File
import javax.inject.Inject

// ─── UI Event sealed class (replaces Toast calls) ───────────────────────────
sealed class HomeUiEvent {
    data class ShowMessage(val message: String) : HomeUiEvent()
    object RestartRequired : HomeUiEvent()
}

data class HomeUiState(
    val customers: List<CustomerWithDebtStatus> = emptyList(),
    val isLoading: Boolean = true,
    val sortOption: SortOption = SortOption.URGENT,
    val totalCollectionToday: Double = 0.0,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    /** True when: local DB is empty + cloud backup found after sign-in */
    val showRestorePrompt: Boolean = false,
    /** True while "Today's Due" filter is active */
    val showTodaysDueOnly: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: CollectionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val backupRepository: BackupRepository,
    private val driveServiceHelper: DriveServiceHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<HomeUiEvent>()
    val uiEvent: SharedFlow<HomeUiEvent> = _uiEvent.asSharedFlow()

    val isWhatsAppEnabled = userPreferencesRepository.isWhatsAppEnabled

    // Separate flow for search query to handle debounce
    private val searchQuery = MutableStateFlow("")

    init {
        loadCollectionStats()
        setupCustomerFlow()
    }

    // ─── Backup / Restore ────────────────────────────────────────────────────

    fun performBackup(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val json = backupRepository.createBackupJson()
                saveBackupToDownloads(context, json)
                syncToCloud(context, json)
                _uiEvent.emit(HomeUiEvent.ShowMessage("Backup successfully saved to Downloads and Google Drive."))
            } catch (e: Exception) {
                _uiEvent.emit(HomeUiEvent.ShowMessage("Backup failed: ${e.message}"))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun syncToCloud(context: Context, json: String) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null) {
            try {
                driveServiceHelper.uploadDatabase(context, account)
            } catch (e: Exception) {
                // Cloud sync failure is non-fatal; local backup already succeeded.
            }
        }
    }

    fun restoreFromCloud(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val account = GoogleSignIn.getLastSignedInAccount(context)
                if (account == null) {
                    _uiEvent.emit(HomeUiEvent.ShowMessage("Google account not signed in. Please sign in via Settings."))
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }

                val backups = driveServiceHelper.listBackups(context, account)
                if (backups.isEmpty()) {
                    _uiEvent.emit(HomeUiEvent.ShowMessage("No backup found on Google Drive."))
                } else {
                    driveServiceHelper.downloadBackup(context, account, backups.first().id)
                    _uiEvent.emit(HomeUiEvent.RestartRequired)
                }
            } catch (e: Exception) {
                _uiEvent.emit(HomeUiEvent.ShowMessage("Restore failed: ${e.toString()}"))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun saveBackupToDownloads(context: Context, json: String) {
        val fileName = "emi_backup_${System.currentTimeMillis()}.json"
        withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { stream -> stream.write(json.toByteArray()) }
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                File(downloadsDir, fileName).writeText(json)
            }
        }
    }

    // ─── Restore-First Safety Protocol ───────────────────────────────────────

    /**
     * Called after Google Sign-In. Checks if local DB is empty and a cloud backup exists.
     * If so, prompts the user to restore instead of starting fresh.
     */
    fun checkRestoreFirstCondition(context: Context) {
        viewModelScope.launch {
            try {
                val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@launch
                val customerCount = repository.getCustomerCount()
                if (customerCount > 0) return@launch // Local data present — no need to prompt

                val backupExists = driveServiceHelper.checkForExistingBackup(context, account)
                if (backupExists) {
                    _uiState.update { it.copy(showRestorePrompt = true) }
                }
            } catch (e: Exception) {
                // Non-critical — silently ignore
            }
        }
    }

    fun dismissRestorePrompt() {
        _uiState.update { it.copy(showRestorePrompt = false) }
    }

    fun acceptRestoreFromCloud(context: Context) {
        _uiState.update { it.copy(showRestorePrompt = false) }
        restoreFromCloud(context)
    }

    // ─── Today's Due Filter ───────────────────────────────────────────────────

    fun toggleTodaysDueFilter() {
        _uiState.update { it.copy(showTodaysDueOnly = !it.showTodaysDueOnly) }
    }

    // ─── Settings ────────────────────────────────────────────────────────────

    fun setWhatsAppEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setWhatsAppEnabled(enabled)
        }
    }

    // ─── Customer Flow ────────────────────────────────────────────────────────

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun setupCustomerFlow() {
        viewModelScope.launch {
            combine(
                searchQuery.debounce(300),
                _uiState.map { it.sortOption }.distinctUntilChanged()
            ) { query, sortOption ->
                Pair(query, sortOption)
            }.flatMapLatest { (query, sortOption) ->
                _uiState.update { it.copy(isLoading = true) }
                if (query.isBlank()) {
                    repository.getCustomersSorted(sortOption)
                } else {
                    repository.searchCustomers(query)
                }
            }.collect { customers ->
                _uiState.update { it.copy(customers = customers, isLoading = false) }
            }
        }
    }

    private fun loadCollectionStats() {
        viewModelScope.launch {
            repository.getCollectedToday()
                .collect { amount ->
                    _uiState.update { it.copy(totalCollectionToday = amount) }
                }
        }
    }

    fun updateSortOption(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchQuery.value = query
    }

    fun toggleSearch() {
        _uiState.update {
            val newActive = !it.isSearchActive
            if (!newActive) onSearchQueryChange("")
            it.copy(isSearchActive = newActive)
        }
    }
}




