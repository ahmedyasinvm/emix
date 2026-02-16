package com.emicollect.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val DEFAULT_COLLECTION_AMOUNT = doublePreferencesKey("default_collection_amount")
    private val IS_WHATSAPP_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey("is_whatsapp_enabled")

    val defaultCollectionAmount: Flow<Double> = context.dataStore.data
        .map { preferences ->
            preferences[DEFAULT_COLLECTION_AMOUNT] ?: 500.0
        }

    val isWhatsAppEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_WHATSAPP_ENABLED] ?: true
        }

    suspend fun saveDefaultCollectionAmount(amount: Double) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_COLLECTION_AMOUNT] = amount
        }
    }

    suspend fun setWhatsAppEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_WHATSAPP_ENABLED] = enabled
        }
    }
}
