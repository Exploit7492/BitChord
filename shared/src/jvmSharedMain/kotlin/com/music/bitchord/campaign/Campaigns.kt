package com.music.bitchord.campaign

import com.music.bitchord.data.DebugLog as Log
import com.music.bitchord.data.Http
import com.music.bitchord.data.model.Song
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

/**
 * The app's single campaign entry point, for the phone and the desktop alike.
 *
 * The backend address and key are per-app build configuration (the phone reads
 * them from `BuildConfig`), so they are handed in once at startup rather than
 * hardcoded here — this module has no `BuildConfig` of its own. Until [init]
 * runs, [nextForSlot] returns null and queues play without campaigns, which is
 * also exactly what a build with no backend configured does.
 */
object Campaigns {

    private const val TAG = "BitChord"

    @Volatile
    private var repository: CampaignRepository? = null

    /**
     * Enables campaign injection. Safe to call more than once; the last call
     * wins. A blank url or key disables campaigns rather than throwing.
     */
    fun init(supabaseUrl: String, anonKey: String) {
        if (supabaseUrl.isBlank() || anonKey.isBlank()) {
            Log.i(TAG, "campaigns disabled: no backend configured")
            repository = null
            return
        }
        repository = CampaignRepository(
            supabaseUrl = supabaseUrl.trimEnd('/'),
            anonKey = anonKey,
            client = HttpClient(OkHttp) {
                // The app's one OkHttp client, so campaigns share its
                // connection pool and dispatcher with every other request.
                engine { preconfigured = Http.client }
            },
        )
    }

    /** True when a backend is configured and campaign injection is live. */
    val enabled: Boolean get() = repository != null

    /**
     * The campaign for one queue slot, or null when there is none to place.
     *
     * @see CampaignRepository.fetchNextForQueueSlot
     */
    suspend fun nextForSlot(genre: String? = null): Song? = repository?.fetchNextForQueueSlot(genre)
}
