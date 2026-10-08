package io.github.mohuddle.skerry.allowlist

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Off until the user turns it on. Shares the allowlist DataStore file. */
class ChargingPreference internal constructor(
    private val dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>,
) {
    val enabled: Flow<Boolean> = dataStore.data.map { prefs -> prefs[KEY] ?: false }

    suspend fun setEnabled(on: Boolean) {
        dataStore.edit { prefs -> prefs[KEY] = on }
    }

    suspend fun get(): Boolean = enabled.first()

    private companion object {
        val KEY = booleanPreferencesKey("charging_enabled")
    }
}

data class SkerryStores(
    val allowlist: AllowlistStore,
    val charging: ChargingPreference,
)

/** One DataStore per process. A second live store on this file throws. */
fun createSkerryStores(context: Context): SkerryStores {
    val dataStore = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    ) { File(context.filesDir, "allowlist.preferences_pb") }
    return SkerryStores(AllowlistStore(dataStore), ChargingPreference(dataStore))
}
