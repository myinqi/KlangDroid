package veil.raven.klangdroid.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.speicher by preferencesDataStore("klangdroid")

data class Werte(
    val adresse: String = STANDARD_ADRESSE,
    val thema: String = "rabe",
    val akzent: String = "",
    val ton: Tonquelle = Tonquelle.MP3,
)

class Einstellungen(private val context: Context) {
    private val adresseKey = stringPreferencesKey("adresse")
    private val themaKey = stringPreferencesKey("thema")
    private val akzentKey = stringPreferencesKey("akzent")
    private val tonKey = stringPreferencesKey("ton")

    val werte: Flow<Werte> = context.speicher.data.map { p ->
        Werte(
            adresse = normalisiereAdresse(p[adresseKey] ?: STANDARD_ADRESSE),
            thema = p[themaKey] ?: "rabe",
            akzent = p[akzentKey].orEmpty(),
            ton = Tonquelle.von(p[tonKey] ?: Tonquelle.MP3.id),
        )
    }

    suspend fun adresse(wert: String) = setzen(adresseKey, normalisiereAdresse(wert))
    suspend fun thema(id: String) = setzen(themaKey, id)
    suspend fun akzent(hex: String) = setzen(akzentKey, hex)
    suspend fun ton(quelle: Tonquelle) = setzen(tonKey, quelle.id)

    private suspend fun setzen(key: androidx.datastore.preferences.core.Preferences.Key<String>, wert: String) {
        context.speicher.edit { it[key] = wert }
    }
}
