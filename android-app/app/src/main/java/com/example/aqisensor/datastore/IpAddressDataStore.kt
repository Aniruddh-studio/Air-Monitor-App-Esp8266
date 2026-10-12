package com.example.aqisensor.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Extension property — creates a single DataStore instance per application context. */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aqi_prefs")

/**
 * Thin wrapper around DataStore that reads/writes the saved device IP address.
 *
 * The IP is stored under the key [KEY_DEVICE_IP].
 * Emits `null` when no IP has been saved yet (triggers the first-launch setup screen).
 */
class IpAddressDataStore(private val context: Context) {

    companion object {
        val KEY_DEVICE_IP = stringPreferencesKey("device_ip")
    }

    /** Flows the saved IP address, or `null` if not set. */
    val deviceIpFlow: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[KEY_DEVICE_IP] }

    /** Persist a new IP address. Replaces any previously stored value. */
    suspend fun saveIp(ip: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEVICE_IP] = ip.trim()
        }
    }

    /** Clear the saved IP (effectively triggers first-launch setup on next open). */
    suspend fun clearIp() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_DEVICE_IP)
        }
    }
}
