package com.music.bitchord.campaign

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CampaignUrlResolverTest {

    @Test
    fun `watch link with v first`() {
        assertEquals("dQw4w9WgXcQ", CampaignUrlResolver.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
    }

    @Test
    fun `watch link with v after other params`() {
        assertEquals(
            "dQw4w9WgXcQ",
            CampaignUrlResolver.extractVideoId("https://music.youtube.com/watch?list=RDAMVM&v=dQw4w9WgXcQ&t=30"),
        )
    }

    @Test
    fun `you tu be short link`() {
        assertEquals("dQw4w9WgXcQ", CampaignUrlResolver.extractVideoId("https://youtu.be/dQw4w9WgXcQ?t=5"))
    }

    @Test
    fun `shorts link`() {
        assertEquals("dQw4w9WgXcQ", CampaignUrlResolver.extractVideoId("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
    }

    @Test
    fun `live link`() {
        assertEquals("dQw4w9WgXcQ", CampaignUrlResolver.extractVideoId("https://youtube.com/live/dQw4w9WgXcQ"))
    }

    @Test
    fun `bare id`() {
        assertEquals("dQw4w9WgXcQ", CampaignUrlResolver.extractVideoId("  dQw4w9WgXcQ  "))
    }

    @Test
    fun `spotify link is not a youtube id`() {
        assertNull(CampaignUrlResolver.extractVideoId("https://open.spotify.com/track/4cOdK2wGLETKBW3PvgPWqT"))
    }

    @Test
    fun `blank yields null`() {
        assertNull(CampaignUrlResolver.extractVideoId("   "))
    }
}
