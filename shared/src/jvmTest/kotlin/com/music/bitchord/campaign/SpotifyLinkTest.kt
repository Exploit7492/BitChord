package com.music.bitchord.campaign

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpotifyLinkTest {

    @Test
    fun `track link parses`() {
        val target = SpotifyLink.parse("https://open.spotify.com/track/4cOdK2wGLETKBW3PvgPWqT")
        assertEquals(SpotifyLink.Kind.TRACK, target?.kind)
        assertEquals("4cOdK2wGLETKBW3PvgPWqT", target?.id)
    }

    @Test
    fun `localised intl segment parses`() {
        val target = SpotifyLink.parse("https://open.spotify.com/intl-de/track/4cOdK2wGLETKBW3PvgPWqT?si=abc")
        assertEquals(SpotifyLink.Kind.TRACK, target?.kind)
    }

    @Test
    fun `embed iframe link parses`() {
        val target = SpotifyLink.parse("https://open.spotify.com/embed/album/1DFixLWuPkv3KT3TnV35m3")
        assertEquals(SpotifyLink.Kind.ALBUM, target?.kind)
    }

    @Test
    fun `uri form parses`() {
        val target = SpotifyLink.parse("spotify:artist:0OdUWJ0sBjDrqHygGUXeCF")
        assertEquals(SpotifyLink.Kind.ARTIST, target?.kind)
    }

    @Test
    fun `youtube url is not spotify`() {
        assertNull(SpotifyLink.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(!SpotifyLink.isSpotifyUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
    }

    @Test
    fun `embed url built from target`() {
        val target = SpotifyLink.Target(SpotifyLink.Kind.TRACK, "4cOdK2wGLETKBW3PvgPWqT")
        assertEquals("https://open.spotify.com/embed/track/4cOdK2wGLETKBW3PvgPWqT", SpotifyLink.embedUrl(target))
    }
}
