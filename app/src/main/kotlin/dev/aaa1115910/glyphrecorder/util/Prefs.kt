package dev.aaa1115910.glyphrecorder.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.aaa1115910.glyphrecorder.App
import dev.aaa1115910.glyphrecorder.WorkingMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private const val PREFERENCES_NAME = "preferences"
val Context.dataStore by preferencesDataStore(name = PREFERENCES_NAME)

fun <T> DataStore<Preferences>.getPreferenceAsFlow(request: PreferenceRequest<T>) =
    data.map { it[request.key] ?: request.defaultValue }

@Composable
fun <T> DataStore<Preferences>.getPreferenceAsState(request: PreferenceRequest<T>) =
    getPreferenceAsFlow(request).collectAsState(initial = request.defaultValue)

suspend fun <T> DataStore<Preferences>.getPreference(preferenceEntry: PreferenceRequest<T>) =
    data.first()[preferenceEntry.key] ?: preferenceEntry.defaultValue

fun <T> DataStore<Preferences>.getPreferenceSync(preferenceEntry: PreferenceRequest<T>) =
    runBlocking { getPreference(preferenceEntry) }

suspend fun <T> DataStore<Preferences>.editPreference(
    preferenceEntry: PreferenceRequest<T>,
    value: T
) = edit { preferences -> preferences[preferenceEntry.key] = value }

fun <T> DataStore<Preferences>.editPreferenceSync(preferenceEntry: PreferenceRequest<T>, value: T) =
    runBlocking { editPreference(preferenceEntry, value) }

suspend fun <T> DataStore<Preferences>.editPreference(key: Preferences.Key<T>, value: T) =
    edit { preferences -> preferences[key] = value }

fun <T> DataStore<Preferences>.editPreferenceSync(key: Preferences.Key<T>, value: T) =
    runBlocking { editPreference(key, value) }


class PreferenceRequest<T>(
    val key: Preferences.Key<T>,
    val defaultValue: T
)

object PrefKeys {
    val workingKey = booleanPreferencesKey("working")
    val workingRequest = PreferenceRequest(workingKey, false)

    val workingModeKey = intPreferencesKey("working_mode")
    val workingModeRequest = PreferenceRequest(workingModeKey, WorkingMode.MediaProjection.ordinal)

    val circlesKey = stringPreferencesKey("circles")
    val circlesRequest = PreferenceRequest(circlesKey, "")

    val floatingWindowPositionXKey = intPreferencesKey("floating_window_position_x")
    val floatingWindowPositionYKey = intPreferencesKey("floating_window_position_y")
    val floatingWindowPositionXRequest = PreferenceRequest(floatingWindowPositionXKey, 0)
    val floatingWindowPositionYRequest = PreferenceRequest(floatingWindowPositionYKey, 0)

    val floatingWindowFirstTipShownKey = booleanPreferencesKey("floating_window_first_tip_shown")
    val floatingWindowFirstTipShownRequest =
        PreferenceRequest(floatingWindowFirstTipShownKey, false)
}

object Prefs {
    val dataStore = App.context.dataStore

    var working: Boolean
        get() = dataStore.getPreferenceSync(PrefKeys.workingRequest)
        set(value) {
            dataStore.editPreferenceSync(PrefKeys.workingKey, value)
        }

    var workingMode: WorkingMode
        get() = WorkingMode.entries[dataStore.getPreferenceSync(PrefKeys.workingModeRequest)]
        set(value) {
            dataStore.editPreferenceSync(PrefKeys.workingModeKey, value.ordinal)
        }

    var circles: List<Pair<Int, Int>>
        get() = dataStore.getPreferenceSync(PrefKeys.circlesRequest).split(",")
            .filter { it.isNotEmpty() }
            .map { it.split(":").let { pair -> pair[0].toInt() to pair[1].toInt() } }
        set(value) {
            dataStore.editPreferenceSync(
                PrefKeys.circlesKey,
                value
                    .take(11) //限制为11个
                    .joinToString(",") { "${it.first}:${it.second}" })
        }

    var floatingWindowPositionX: Int
        get() = dataStore.getPreferenceSync(PrefKeys.floatingWindowPositionXRequest)
        set(value) {
            dataStore.editPreferenceSync(PrefKeys.floatingWindowPositionXKey, value)
        }

    var floatingWindowPositionY: Int
        get() = dataStore.getPreferenceSync(PrefKeys.floatingWindowPositionYRequest)
        set(value) {
            dataStore.editPreferenceSync(PrefKeys.floatingWindowPositionYKey, value)
        }


    var floatingWindowFirstTipShown: Boolean
        get() = dataStore.getPreferenceSync(PrefKeys.floatingWindowFirstTipShownRequest)
        set(value) {
            dataStore.editPreferenceSync(PrefKeys.floatingWindowFirstTipShownKey, value)
        }
}