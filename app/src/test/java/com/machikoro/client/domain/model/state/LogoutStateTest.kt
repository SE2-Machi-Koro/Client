package com.machikoro.client.domain.model.state

import org.junit.Assert.assertEquals
import org.junit.Test

class LogoutStateTest {
    @Test
    fun testDefaultState() {
        val state = LogoutState()
        assertEquals(false, state.submitting)
    }

    @Test
    fun testSubmittingTrue() {
        val state = LogoutState(submitting = true)
        assertEquals(true, state.submitting)
    }
}
