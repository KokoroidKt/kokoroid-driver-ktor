package dev.kokoroidkt.httpDriver.websocket

import kotlinx.serialization.json.JsonElement

fun interface WebsocketCallback {
    fun callback(data: JsonElement)
}
