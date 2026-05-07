package com.machikoro.client.domain.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

class DataStoreSessionStorage(
    private val dataStore: DataStore<Preferences>,
) : SessionStorage {

    constructor(context: Context) : this(context.applicationContext.sessionDataStore)

    override suspend fun read(): Session? {
        val prefs = dataStore.data.first()
        val token = prefs[KEY_TOKEN] ?: return null
        val username = prefs[KEY_USERNAME] ?: return null
        return Session(sessionToken = token, username = username)
    }

    override suspend fun write(session: Session) {
        dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = session.sessionToken
            prefs[KEY_USERNAME] = session.username
        }
    }

    override suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_TOKEN)
            prefs.remove(KEY_USERNAME)
        }
    }

    private companion object {
        val KEY_TOKEN = stringPreferencesKey("session_token")
        val KEY_USERNAME = stringPreferencesKey("session_username")
    }
}
