package com.emicollect.app.ui.addcustomer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emicollect.app.data.local.entity.Customer
import com.emicollect.app.data.repository.CollectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddCustomerUiState(
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val frequency: String = "Weekly",
    val collectionDay: Int = 1, // 1=Sun, 2=Mon...
    val collectionWeek: Int = 1, // 1st, 2nd, 3rd, 4th
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false
)

@HiltViewModel
class AddCustomerViewModel @Inject constructor(
    private val repository: CollectionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddCustomerUiState())
    val uiState: StateFlow<AddCustomerUiState> = _uiState.asStateFlow()

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, error = null) }
    }

    fun onPhoneChange(phone: String) {
        _uiState.update { it.copy(phone = phone, error = null) }
    }

    fun onAddressChange(address: String) {
        _uiState.update { it.copy(address = address, error = null) }
    }

    fun onFrequencyChange(frequency: String) {
        _uiState.update { it.copy(frequency = frequency, error = null) }
    }

    fun onCollectionDayChange(day: Int) {
        _uiState.update { it.copy(collectionDay = day) }
    }

    fun onCollectionWeekChange(week: Int) {
        _uiState.update { it.copy(collectionWeek = week) }
    }

    fun saveCustomer() {
        val currentState = _uiState.value
        
        // Validation
        if (currentState.name.isBlank()) {
            _uiState.update { it.copy(error = "Name cannot be empty") }
            return
        }
        if (currentState.phone.isBlank()) {
            _uiState.update { it.copy(error = "Phone cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.addCustomer(
                    Customer(
                        name = currentState.name,
                        phone = currentState.phone,
                        address = currentState.address,
                        frequency = currentState.frequency,
                        collectionDay = currentState.collectionDay,
                        collectionWeek = currentState.collectionWeek
                    )
                )
                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                // Handle potential errors (e.g., duplicate phone if unique constraint exists)
                _uiState.update { 
                    it.copy(isLoading = false, error = "Failed to save customer: ${e.message}") 
                }
            }
        }
    }
}
