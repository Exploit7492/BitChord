package com.music.bitchord.campaign

import com.music.bitchord.data.model.QueueTier
import com.music.bitchord.data.model.Song
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CampaignQueueInjectionTest {

    private fun song(id: String) = Song(
        videoId = id,
        title = id,
        artist = "a",
        thumbnailUrl = null,
        queueTier = QueueTier.CONTEXT,
    )

    private fun campaign(id: String) = Song(
        videoId = id,
        title = id,
        artist = "a",
        thumbnailUrl = null,
    )

    private fun ids(songs: List<Song>) = songs.map { it.videoId }

    @Test
    fun `a campaign lands at every fifth position`() = runBlocking {
        val base = (0 until 12).map { song("s$it") }
        var n = 0

        val result = CampaignQueueInjection.inject(base) { campaign("c${n++}") }

        // Four songs, one campaign, four songs, one campaign, ...
        assertEquals(
            listOf("s0", "s1", "s2", "s3", "c0", "s4", "s5", "s6", "s7", "c1", "s8", "s9", "s10", "s11", "c2"),
            ids(result.items),
        )
    }

    @Test
    fun `a null slot is skipped and the pattern resumes at the next boundary`() = runBlocking {
        val base = (0 until 8).map { song("s$it") }
        var calls = 0

        // Only the second boundary (after base index 7) yields a campaign.
        val result = CampaignQueueInjection.inject(base) {
            if (calls++ == 1) campaign("c0") else null
        }

        assertEquals(listOf("s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7", "c0"), ids(result.items))
    }

    @Test
    fun `the tracked index follows the tapped song past injected campaigns`() = runBlocking {
        val base = (0 until 10).map { song("s$it") }

        val result = CampaignQueueInjection.inject(
            baseItems = base,
            targetOriginalIndex = 6,
        ) { campaign("c") }

        // One campaign after index 3, another after index 7 — the song tapped
        // at original index 6 is now behind the first campaign.
        assertEquals(
            listOf("s0", "s1", "s2", "s3", "c", "s4", "s5", "s6", "s7", "c", "s8", "s9"),
            ids(result.items),
        )
        assertEquals("s6", result.items[result.adjustedTargetIndex].videoId)
    }

    @Test
    fun `a skipped slot still tracks the tapped index correctly`() = runBlocking {
        val base = (0 until 10).map { song("s$it") }

        // Nothing injects anywhere, so no index moves.
        val result = CampaignQueueInjection.inject(
            baseItems = base,
            targetOriginalIndex = 5,
        ) { null }

        assertEquals(5, result.adjustedTargetIndex)
        assertEquals("s5", result.items[result.adjustedTargetIndex].videoId)
    }

    @Test
    fun `the run of four continues across chunks`() = runBlocking {
        val first = CampaignQueueInjection.inject(
            baseItems = (0 until 4).map { song("a$it") },
            offset = 0,
        ) { campaign("c0") }
        val second = CampaignQueueInjection.inject(
            baseItems = (0 until 4).map { song("b$it") },
            offset = 4,
        ) { campaign("c1") }

        assertEquals(listOf("a0", "a1", "a2", "a3", "c0"), ids(first.items))
        assertEquals(listOf("b0", "b1", "b2", "b3", "c1"), ids(second.items))
    }

    @Test
    fun `an injected campaign is a context-tier entry with its own identity`() = runBlocking {
        val result = CampaignQueueInjection.inject((0 until 4).map { song("s$it") }) { campaign("c") }

        val injected = result.items.last()
        assertTrue(injected.isCampaign)
        assertEquals(QueueTier.CONTEXT, injected.queueTier)
        assertTrue(injected.queueEntryId != null)
    }

    @Test
    fun `a chunk shorter than a run injects nothing`() = runBlocking {
        val result = CampaignQueueInjection.inject((0 until 3).map { song("s$it") }) { campaign("c") }

        assertEquals(listOf("s0", "s1", "s2"), ids(result.items))
    }
}
