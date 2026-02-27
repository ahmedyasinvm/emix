package com.emicollect.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class CollectionSchedule(
    val weeklyDay: Int,
    val monthlyWeekNum: Int,
    val monthlyDay: Int
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Existing keys
    private val DEFAULT_COLLECTION_AMOUNT = doublePreferencesKey("default_collection_amount")
    private val IS_WHATSAPP_ENABLED = booleanPreferencesKey("is_whatsapp_enabled")
    private val WEEKLY_COLLECTION_DAY = intPreferencesKey("weekly_collection_day")
    private val MONTHLY_WEEK_NUM = intPreferencesKey("monthly_week_num")
    private val MONTHLY_COLLECTION_DAY = intPreferencesKey("monthly_collection_day")

    // New keys
    private val BUSINESS_NAME = stringPreferencesKey("business_name")
    private val CONTACT_NUMBER = stringPreferencesKey("contact_number")
    private val IS_BIOMETRIC_ENABLED = booleanPreferencesKey("is_biometric_enabled")
    private val DEFAULT_COLLECTION_DAY_KEY = stringPreferencesKey("default_collection_day")

    // ═══ Existing Flows ═══
    val defaultCollectionAmount: Flow<Double> = context.dataStore.data
        .map { it[DEFAULT_COLLECTION_AMOUNT] ?: 500.0 }

    val isWhatsAppEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[IS_WHATSAPP_ENABLED] ?: true }

    val collectionSchedule: Flow<CollectionSchedule> = context.dataStore.data
        .map { prefs ->
            CollectionSchedule(
                weeklyDay = prefs[WEEKLY_COLLECTION_DAY] ?: 1,
                monthlyWeekNum = prefs[MONTHLY_WEEK_NUM] ?: 1,
                monthlyDay = prefs[MONTHLY_COLLECTION_DAY] ?: 1
            )
        }

    // ═══ New Flows ═══
    val businessName: Flow<String> = context.dataStore.data
        .map { it[BUSINESS_NAME] ?: "" }

    val contactNumber: Flow<String> = context.dataStore.data
        .map { it[CONTACT_NUMBER] ?: "" }

    val isBiometricEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[IS_BIOMETRIC_ENABLED] ?: false }

    val defaultCollectionDay: Flow<String> = context.dataStore.data
        .map { it[DEFAULT_COLLECTION_DAY_KEY] ?: "Monday" }

    // ═══ Existing Writers ═══
    suspend fun saveDefaultCollectionAmount(amount: Double) {
        context.dataStore.edit { it[DEFAULT_COLLECTION_AMOUNT] = amount }
    }

    suspend fun setWhatsAppEnabled(enabled: Boolean) {
        context.dataStore.edit { it[IS_WHATSAPP_ENABLED] = enabled }
    }

    suspend fun saveWeeklyDay(day: Int) {
        context.dataStore.edit { it[WEEKLY_COLLECTION_DAY] = day }
    }

    suspend fun saveMonthlySchedule(weekNum: Int, day: Int) {
        context.dataStore.edit {
            it[MONTHLY_WEEK_NUM] = weekNum
            it[MONTHLY_COLLECTION_DAY] = day
        }
    }

    // ═══ New Writers ═══
    suspend fun saveBusinessName(name: String) {
        context.dataStore.edit { it[BUSINESS_NAME] = name }
    }

    suspend fun saveContactNumber(number: String) {
        context.dataStore.edit { it[CONTACT_NUMBER] = number }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[IS_BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun saveDefaultCollectionDay(day: String) {
        context.dataStore.edit { it[DEFAULT_COLLECTION_DAY_KEY] = day }
    }

    // ═══ Sync Read (for ReceiptGenerator) ═══
    suspend fun getBusinessNameSync(): String {
        return context.dataStore.data.first()[BUSINESS_NAME] ?: ""
    }

    // ═══ Backup Preference ═══
    private val IS_AUTO_BACKUP_ENABLED = booleanPreferencesKey("is_auto_backup_enabled")
    private val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")

    val isAutoBackupEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { it[IS_AUTO_BACKUP_ENABLED] ?: false }

    val isDarkMode: Flow<Boolean> = context.dataStore.data
        .map { it[IS_DARK_MODE] ?: true } // Default to Dark Mode

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { it[IS_AUTO_BACKUP_ENABLED] = enabled }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[IS_DARK_MODE] = enabled }
    }
}
