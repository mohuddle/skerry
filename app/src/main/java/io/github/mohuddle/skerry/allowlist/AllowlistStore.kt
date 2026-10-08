package io.github.mohuddle.skerry.allowlist

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Package names the user has allowed. Missing key means an empty list. */
class AllowlistStore(
    private val dataStore: DataStore<Preferences>,
) {
    val packages: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[PACKAGES] ?: emptySet()
    }

    suspend fun list(): Set<String> = packages.first()

    suspend fun contains(packageName: String): Boolean = packageName in list()

    suspend fun add(packageName: String) {
        dataStore.edit { prefs ->
            prefs[PACKAGES] = (prefs[PACKAGES] ?: emptySet()) + packageName
        }
    }

    suspend fun remove(packageName: String) {
        dataStore.edit { prefs ->
            prefs[PACKAGES] = (prefs[PACKAGES] ?: emptySet()) - packageName
        }
    }

    private companion object {
        val PACKAGES = stringSetPreferencesKey("packages")
    }
}

private const val FILE_NAME = "allowlist.preferences_pb"

fun createAllowlistStore(filesDir: File, scope: CoroutineScope): AllowlistStore {
    val dataStore = PreferenceDataStoreFactory.create(scope = scope) {
        File(filesDir, FILE_NAME)
    }
    return AllowlistStore(dataStore)
}

/** One store per process. A second live DataStore on the same file throws. */
fun createAllowlistStore(context: Context): AllowlistStore {
    return createAllowlistStore(
        filesDir = context.filesDir,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    )
}
