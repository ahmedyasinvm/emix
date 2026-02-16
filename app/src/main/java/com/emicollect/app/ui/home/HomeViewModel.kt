package com.emicollect.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emicollect.app.data.repository.CollectionRepository
import com.emicollect.app.data.local.UserPreferencesRepository
import com.emicollect.app.data.model.CustomerWithDebtStatus
import com.emicollect.app.data.model.SortOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val customers: List<CustomerWithDebtStatus> = emptyList(),
    val isLoading: Boolean = true,
    val sortOption: SortOption = SortOption.URGENT,
    val totalCollectionToday: Double = 0.0,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: CollectionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val backupRepository: com.emicollect.app.data.repository.BackupRepository,
    private val googleDriveManager: com.emicollect.app.data.cloud.GoogleDriveManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    val isWhatsAppEnabled = userPreferencesRepository.isWhatsAppEnabled
    
    fun performBackup(context: android.content.Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val json = backupRepository.createBackupJson()
                saveBackupToFile(context, json)
                
                // Also Sync to Cloud
                syncToCloud(context, json)
                
                 android.widget.Toast.makeText(context, "Backup saved to Downloads & Cloud", android.widget.Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                 android.widget.Toast.makeText(context, "Backup failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    private suspend fun syncToCloud(context: android.content.Context, json: String) {
        val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
        if (account != null) {
            try {
                googleDriveManager.syncToCloud(context, account, json)
            } catch (e: Exception) {
                // Log or toast error silently for cloud sync
            }
        }
    }
    
    fun restoreFromCloud(context: android.content.Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
                if (account == null) {
                    android.widget.Toast.makeText(context, "Not signed in to Google", android.widget.Toast.LENGTH_LONG).show()
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }
                
                val json = googleDriveManager.fetchLatestCloudBackup(context, account)
                if (json != null) {
                    backupRepository.restoreBackup(json)
                    // Reload data
                    setupCustomerFlow()
                    loadCollectionStats()
                    android.widget.Toast.makeText(context, "Restored from Cloud successfully", android.widget.Toast.LENGTH_LONG).show()
                } else {
                    android.widget.Toast.makeText(context, "No backup found in Cloud", android.widget.Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Restore failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun saveBackupToFile(context: android.content.Context, json: String) {
        val fileName = "emi_backup_${System.currentTimeMillis()}.json"
        
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

    fun setWhatsAppEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setWhatsAppEnabled(enabled)
        }
    }

    // Separate flow for search query to handle debounce
    private val searchQuery = MutableStateFlow("")

    init {
        loadCollectionStats()
        setupCustomerFlow()
    }

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
            if (!newActive) {
                // Clear query when closing search
                onSearchQueryChange("")
            }
            it.copy(isSearchActive = newActive)
        }
    }
}