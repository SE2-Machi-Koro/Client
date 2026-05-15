package com.machikoro.client.domain.model.state

import org.junit.Assert.*
import org.junit.Test

class RegisterDialogStateTest {
    @Test
    fun testDefaultState() {
        val state = RegisterDialogState()
        assertFalse(state.canSubmit)
    }

    @Test
    fun testCanSubmitTrue() {
        val state = RegisterDialogState(username = "user", password = "pass")
        assertTrue(state.canSubmit)
    }

    @Test
    fun testCanSubmitFalseWhenSubmitting() {
        val state = RegisterDialogState(username = "user", password = "pass", submitting = true)
        assertFalse(state.canSubmit)
    }

    @Test
    fun testCanSubmitFalseWhenRegistered() {
        val state = RegisterDialogState(username = "user", password = "pass", registeredUsername = "user")
        assertFalse(state.canSubmit)
    }

    @Test
    fun testCanSubmitFalseWhenUsernameBlank() {
        val state = RegisterDialogState(username = "", password = "pass")
        assertFalse(state.canSubmit)
    }

    @Test
    fun testCanSubmitFalseWhenPasswordBlank() {
        val state = RegisterDialogState(username = "user", password = "")
        assertFalse(state.canSubmit)
    }
}
