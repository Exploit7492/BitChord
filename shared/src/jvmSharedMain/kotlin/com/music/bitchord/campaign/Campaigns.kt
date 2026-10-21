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
     * This install's listener identity, supplied at [init] by whichever
     * platform knows how to persist one (the phone's SharedPreferences, the
     * desktop's java.util.prefs). Shared here because the shared module has no
     * storage of its own, and the play reporter needs it to register a listener
     * before it can attribute a play. Defaults to a per-process UUID so an
     * un-wired platform still reports rather than crashing.
     */
    @Volatile
    private var deviceIdValue: String = java.util.UUID.randomUUID().toString()

    /**
     * The genre/mood term of the surface the current queue was started from, or
     * null for a queue with no genre context (search, album, playlist, radio,
     * autoplay). Set by the Explore mood/genre path, read at injection — see
     * [nextForSlotResolvingGenre].
     */
    @Volatile
    private var genreContext: String? = null

    /**
     * Enables campaign injection. Safe to call more than once; the last call
     * wins. A blank url or key disables campaigns rather than throwing.
     *
     * @param genreTileHost The mavins-web origin used to report an unresolved
     *   genre tile for review. Blank disables that reporting only; the tile
     *   still reaches the server as a raw term and fails closed there.
     * @param deviceId This install's persisted listener identity. Blank leaves
     *   the per-process default in place.
     */
    fun init(supabaseUrl: String, anonKey: String, genreTileHost: String = "", deviceId: String = "") {
        if (deviceId.isNotBlank()) deviceIdValue = deviceId
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
            genreTileHost = genreTileHost,
        )
    }

    /** True when a backend is configured and campaign injection is live. */
    val enabled: Boolean get() = repository != null

    /** This install's listener identity, for [ensureDeviceListener]. */
    fun deviceId(): String = deviceIdValue

    /** Records the genre context of the queue about to start; null clears it. */
    fun setGenreContext(term: String?) {
        genreContext = term?.takeIf { it.isNotBlank() }
    }

    /**
     * The campaign for one queue slot, or null when there is none to place.
     *
     * @see CampaignRepository.fetchNextForQueueSlot
     */
    suspend fun nextForSlot(genre: String? = null): Song? = repository?.fetchNextForQueueSlot(genre)

    /**
     * The campaign for one queue slot on the current queue, resolving the genre
     * context the Explore path recorded. A null context reaches the RPC as a
     * null genre and asks for any eligible campaign — injection is a
     * monetization slot, not a genre feature — while a concrete term confines
     * the pick to that genre and fails closed if it cannot be resolved.
     */
    suspend fun nextForSlotResolvingGenre(): Song? {
        val r = repository ?: return null
        val genre = genreContext?.let { GenreTileMappingCache.resolveTile(it, r) }
        return r.fetchNextForQueueSlot(genre)
    }

    /**
     * What a genre-tile tap should send: a resolved canonical genre, the raw
     * tile title when it is not yet reviewed, or null for a tile reviewed as a
     * mood. See [GenreTileMappingCache.resolveTile].
     */
    suspend fun resolveGenreTile(tileTitle: String): String? {
        val r = repository ?: return null
        return GenreTileMappingCache.resolveTile(tileTitle, r)
    }

    /** Registers this device as a listener; returns the listener id, or null. */
    suspend fun ensureDeviceListener(deviceId: String): String? = repository?.ensureDeviceListener(deviceId)

    /** Reports a qualified campaign play. Fire-and-forget. */
    suspend fun recordStream(
        campaignId: String,
        userId: String? = null,
        listenDurationSeconds: Int = 0,
        countryCode: String? = null,
        isFullListen: Boolean = false,
    ) = repository?.recordCampaignStream(campaignId, userId, listenDurationSeconds, countryCode, isFullListen)

    /** A campaign's current gathered total, or null on failure. */
    suspend fun campaignTotalStreams(campaignId: String): Long? = repository?.fetchCampaignTotalStreams(campaignId)

    /**
     * Every currently-live campaign, for the home banner. Empty when no backend
     * is configured, which is what a build without one shows.
     */
    suspend fun liveCampaignsForBanner(): List<CampaignCard> =
        repository?.fetchLiveCampaignsForBanner() ?: emptyList()
}
