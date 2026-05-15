package com.machikoro.client.network.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthDtosTest {
    @Test
    fun testRegisterRequest() {
        val req = RegisterRequest("user", "pass")
        assertEquals("user", req.username)
        assertEquals("pass", req.password)
    }
    @Test
    fun testRegisterResponse() {
        val res = RegisterResponse(1, "user")
        assertEquals(1, res.id)
        assertEquals("user", res.username)
    }
    @Test
    fun testLoginRequest() {
        val req = LoginRequest("user", "pass")
        assertEquals("user", req.username)
        assertEquals("pass", req.password)
    }
    @Test
    fun testLoginResponse() {
        val res = LoginResponse("token", "user", 42)
        assertEquals("token", res.sessionToken)
        assertEquals("user", res.username)
        assertEquals(42, res.userId)
    }
    @Test
    fun testLogoutRequest() {
        val req = LogoutRequest("token")
        assertEquals("token", req.sessionToken)
    }
}
