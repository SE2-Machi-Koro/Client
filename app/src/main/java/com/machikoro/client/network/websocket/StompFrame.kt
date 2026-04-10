package com.machikoro.client.network.websocket

internal data class StompFrame(
    val command: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String = ""
)

internal fun StompFrame.serialize(): String {
    val builder = StringBuilder()
    builder.append(command).append('\n')
    headers.forEach { (key, value) ->
        builder.append(key).append(':').append(value).append('\n')
    }
    builder.append('\n')
    if (body.isNotEmpty()) {
        builder.append(body)
    }
    builder.append('\u0000')
    return builder.toString()
}

internal fun parseFrames(buffer: StringBuilder): List<StompFrame> {
    val frames = mutableListOf<StompFrame>()

    while (true) {
        val terminatorIndex = buffer.indexOf("\u0000")
        if (terminatorIndex < 0) {
            return frames
        }

        val rawFrame = buffer.substring(0, terminatorIndex)
        buffer.delete(0, terminatorIndex + 1)

        val normalized = rawFrame.trimStart('\n', '\r')
        if (normalized.isBlank()) {
            continue
        }

        val separatorIndex = normalized.indexOf("\n\n")
        val headerSection = if (separatorIndex >= 0) {
            normalized.substring(0, separatorIndex)
        } else {
            normalized
        }
        val bodySection = if (separatorIndex >= 0) {
            normalized.substring(separatorIndex + 2)
        } else {
            ""
        }

        val lines = headerSection.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            continue
        }

        val headers = lines
            .drop(1)
            .mapNotNull { line ->
                val delimiterIndex = line.indexOf(':')
                if (delimiterIndex <= 0) {
                    null
                } else {
                    line.substring(0, delimiterIndex) to line.substring(delimiterIndex + 1)
                }
            }
            .toMap()

        frames += StompFrame(
            command = lines.first(),
            headers = headers,
            body = bodySection
        )
    }
}
