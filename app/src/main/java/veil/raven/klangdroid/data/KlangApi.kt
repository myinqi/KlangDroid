package veil.raven.klangdroid.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class KlangApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build(),
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    data class Bestand(
        val index: IndexAntwort,
        val eigen: Map<String, EigenKunst>,
        val karte: KarteAntwort? = null,
    )

    suspend fun laden(basis: String): Bestand = withContext(Dispatchers.IO) {
        val index = holen(basis, "api/index", IndexAntwort.serializer())
        val eigen = runCatching { holen(basis, "api/eigen-artwork", EigenAntwort.serializer()).songs }
            .getOrDefault(emptyMap())
        val karte = runCatching { holen(basis, "api/karte", KarteAntwort.serializer()) }.getOrNull()
        Bestand(index, eigen, karte)
    }

    suspend fun liedtext(basis: String, id: String): LiedtextAntwort = withContext(Dispatchers.IO) {
        holen(basis, "api/song/$id", LiedtextAntwort.serializer())
    }

    private fun <T> holen(basis: String, pfad: String, serializer: kotlinx.serialization.DeserializationStrategy<T>): T {
        val url = basis.trimEnd('/') + "/" + pfad
        val antwort = client.newCall(Request.Builder().url(url).build()).execute()
        antwort.use { res ->
            val koerper = res.body?.string().orEmpty()
            if (!res.isSuccessful) {
                val grund = when (res.code) {
                    503 -> "Der KlangTresor hat noch keinen Katalog."
                    else -> "Der KlangTresor antwortet mit ${res.code}."
                }
                throw IllegalStateException(grund)
            }
            return json.decodeFromString(serializer, koerper)
        }
    }
}
