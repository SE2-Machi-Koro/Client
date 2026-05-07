package com.machikoro.client.domain.session

interface SessionStorage {
    suspend fun read(): Session?
    suspend fun write(session: Session)
    suspend fun clear()
}
