package com.music.bitchord.campaign

import com.music.bitchord.data.DebugLog as Log
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.model.Song
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * Reads the campaign system this app is a consumer of.
 *
 * The campaigns themselves are published elsewhere — this app never creates,
 * edits or pauses one. Its whole job is to ask, per queue slot, which campaign
 * should play next, and to turn the answer into a playable [Song].
 *
 * The endpoint is the `get_next_campaign_for_queue_slot` RPC, which picks the
 * least-recently-served eligible campaign and marks it served in one
 * transaction. That atomicity is the reason the injection layer calls this once
 * per slot rather than fetching a batch: see
 * [CampaignQueueInjection] for why that distinction is load-bearing.
 *
 * @param supabaseUrl The project URL, without a trailing slash.
 * @param anonKey The public anon key. Blank in either disables campaigns
 *   entirely rather than failing — a build with no campaign backend configured
 *   plays a normal queue.
 */
class CampaignRepository(
    private val supabaseUrl: String,
    private val anonKey: String,
    private val client: HttpClient,
    private val ytMusic: YtMusicRepository = YtMusicRepository,
) {

    private val configured: Boolean
        get() = supabaseUrl.isNotBlank() && anonKey.isNotBlank()

    /**
     * The next campaign for one queue slot, or null when none is eligible.
     *
     * Null is an ordinary answer, not an error: the RPC's own eligibility
     * filter is fail-closed, so an empty result means "nothing to place right
     * now" and the slot is skipped. Every failure path returns null too — a
     * campaign that cannot be placed must never break the queue it rides on.
     *
     * @param genre The listener's current genre, when the queue was started
     *   from a genre surface. Null asks for a campaign without that
     *   constraint, which is what the RPC's own null branch handles.
     */
    suspend fun fetchNextForQueueSlot(genre: String? = null): Song? {
        if (!configured) return null
        return withContext(Dispatchers.IO) {
            try {
                val body = client.post("$supabaseUrl/rest/v1/rpc/get_next_campaign_for_queue_slot") {
                    header("apikey", anonKey)
                    header("Authorization", "Bearer $anonKey")
                    contentType(ContentType.Application.Json)
                    setBody(
                        buildString {
                            append("{\"p_genre\":")
                            if (genre == null) {
                                append("null")
                            } else {
                                append(JsonPrimitive(genre).toString())
                            }
                            append('}')
                        }
                    )
                }.bodyAsText()

                resolveRow(body)
            } catch (e: Exception) {
                Log.w(TAG, "fetchNextForQueueSlot failed for genre=$genre: ${e.message}")
                null
            }
        }
    }

    /**
     * Turns the RPC's JSON array into a playable song.
     *
     * The RPC returns an array (zero or one row) rather than an object, so an
     * empty array is the "no campaign" case. The row's `resolved_song_id` wins
     * when present; otherwise a video id is taken from the raw `source_url`.
     * Either way the id is resolved through YouTube Music, which is what
     * supplies title, artist and artwork — the campaign row itself carries only
     * the link.
     */
    private suspend fun resolveRow(body: String): Song? {
        val array = try {
            Json.parseToJsonElement(body) as? JsonArray
        } catch (e: Exception) {
            Log.w(TAG, "queue-slot response was not JSON: ${e.message}")
            return null
        }
        val row = array?.firstOrNull() as? JsonObject ?: return null

        val resolvedSongId = row["resolved_song_id"]?.jsonPrimitive?.content
            ?.takeIf { it.isNotBlank() }
        val videoId = resolvedSongId ?: extractVideoId(row["source_url"]?.jsonPrimitive?.content.orEmpty())
        if (videoId == null) {
            Log.w(TAG, "queue-slot row carried no usable video id")
            return null
        }

        return ytMusic.trackLinks(videoId).getOrNull()
    }

    private companion object {
        const val TAG = "BitChord"

        /**
         * Pulls the `v=` value out of a YouTube watch URL.
         *
         * Deliberately narrow: only the shapes a campaign submission actually
         * produces. A short link or a `youtu.be` form is not guessed at — the
         * RPC's own eligibility filter admits rows by their YouTube-URL shape,
         * and a row that reached here without a `resolved_song_id` is one whose
         * link was stored as a plain watch URL.
         */
        fun extractVideoId(url: String): String? {
            if (url.isBlank()) return null
            val marker = url.indexOf("v=")
            if (marker < 0) return null
            return url.substring(marker + 2)
                .takeWhile { it != '&' && it != '?' && it != '#' }
                .takeIf { it.isNotBlank() }
        }
    }
}
