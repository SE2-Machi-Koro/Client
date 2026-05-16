package com.machikoro.client.domain.model.state

import org.junit.Assert.*
import org.junit.Test

class LoginDialogStateTest {
    @Test
    fun testDefaultState() {
        val state = LoginDialogState()
        assertFalse(state.canSubmit)
    }

    @Test
    fun testCanSubmitTrue() {
        val state = LoginDialogState(username = "user", password = "pass")
        assertTrue(state.canSubmit)
    }

    @Test
    fun testCanSubmitFalseWhenSubmitting() {
        val state = LoginDialogState(username = "user", password = "pass", submitting = true)
        assertFalse(state.canSubmit)
    }

    @Test
    fun testCanSubmitFalseWhenLoggedIn() {
        val state = LoginDialogState(username = "user", password = "pass", loggedInAs = "user")
        assertFalse(state.canSubmit)
    }

    @Test
    fun testCanSubmitFalseWhenUsernameBlank() {
        val state = LoginDialogState(username = "", password = "pass")
        assertFalse(state.canSubmit)
    }

    @Test
    fun testCanSubmitFalseWhenPasswordBlank() {
        val state = LoginDialogState(username = "user", password = "")
        assertFalse(state.canSubmit)
    }
}
