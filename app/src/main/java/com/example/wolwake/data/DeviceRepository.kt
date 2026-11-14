package com.example.wolwake.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

// DataStore 实例须为单例，委托声明在文件顶层保证全局唯一
private val Context.deviceStore by preferencesDataStore(name = "devices")

/** 设备列表唯一真实源：DataStore 中以 JSON 持久化，UI 一律经由 Flow 派生。 */
class DeviceRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    val devices: Flow<List<Device>> = context.deviceStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[KEY_DEVICES]?.let { decoded(it) } ?: emptyList() }

    suspend fun upsert(device: Device) {
        context.deviceStore.edit { prefs ->
            val current = prefs[KEY_DEVICES]?.let { decoded(it) } ?: emptyList()
            val updated: List<Device> = (current.filter { it.id != device.id } + device)
                .sortedBy { it.name }
            prefs[KEY_DEVICES] = json.encodeToString(updated)
        }
    }

    suspend fun delete(id: String) {
        context.deviceStore.edit { prefs ->
            val current = prefs[KEY_DEVICES]?.let { decoded(it) } ?: emptyList()
            prefs[KEY_DEVICES] = json.encodeToString(current.filter { it.id != id })
        }
    }

    private fun decoded(raw: String): List<Device> =
        runCatching { json.decodeFromString<List<Device>>(raw) }.getOrDefault(emptyList())

    private companion object {
        val KEY_DEVICES = stringPreferencesKey("devices_json")
    }
}
