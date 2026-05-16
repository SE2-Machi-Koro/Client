package com.machikoro.client.network.websocket

import org.junit.Assert.assertEquals
import org.junit.Test

class StompFrameTest {
    @Test
    fun testSerialize() {
        val frame = StompFrame("SEND", mapOf("destination" to "/topic/test"), "body")
        val serialized = frame.serialize()
        assert(serialized.contains("SEND"))
        assert(serialized.contains("destination:/topic/test"))
        assert(serialized.contains("body"))
        assert(serialized.endsWith("\u0000"))
    }

    @Test
    fun testParseFrames() {
        val buffer = StringBuilder("SEND\ndestination:/topic/test\n\nbody\u0000")
        val frames = parseFrames(buffer)
        assertEquals(1, frames.size)
        assertEquals("SEND", frames[0].command)
        assertEquals("/topic/test", frames[0].headers["destination"])
        assertEquals("body", frames[0].body)
    }
}
