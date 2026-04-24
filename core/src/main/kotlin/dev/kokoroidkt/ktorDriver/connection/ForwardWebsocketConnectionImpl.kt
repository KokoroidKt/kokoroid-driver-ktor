// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
//
// SPDX-License-Identifier: LGPL-2.1

@file:Suppress("ktlint:standard:no-wildcard-imports")

package dev.kokoroidkt.ktorDriver.connection

import dev.kokoroidkt.coreApi.utils.getExtensionLogger
import dev.kokoroidkt.driverApi.transport.EventEmitter
import dev.kokoroidkt.httpDriver.connection.ForwardWebsocketConnection
import dev.kokoroidkt.httpDriver.http.HttpMethod
import dev.kokoroidkt.ktorDriver.client.WebsocketClientImpl
import dev.kokoroidkt.ktorDriver.config.Config
import dev.kokoroidkt.ktorDriver.util.toKtorHttpMethod
import dev.kokoroidkt.transport.connection.ConnectionState
import dev.kokoroidkt.transport.decoder.Decoder
import dev.kokoroidkt.transport.raw.Data
import dev.kokoroidkt.transport.raw.Raw
import io.ktor.client.engine.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.net.URI

class ForwardWebsocketConnectionImpl(
    websocketUrl: URI,
    override val header: Map<String, List<String>?>,
    engine: HttpClientEngine? = null,
    override val pingMillis: Long,
    override val method: HttpMethod,
) : ForwardWebsocketConnection(websocketUrl) {
    override val state get() = _state

    val client = WebsocketClientImpl(header, engine, pingMillis, method)
    val decoders: MutableList<Decoder> = mutableListOf()
    private val sendChannel = Channel<JsonElement>(Config.config.websocketSendChannelSize)
    val logger = getExtensionLogger()

    override fun registerDecoder(decoder: Decoder) {
        decoders.add(decoder)
    }

    override suspend fun run() {
        _state = ConnectionState.RUNNING
        CoroutineScope(Dispatchers.IO).launch {
            client.client.ws(
                method = method.toKtorHttpMethod(),
                host = websocketUrl.host,
                port = websocketUrl.port,
                path = websocketUrl.path,
            ) {
                launch(Dispatchers.IO) {
                    while (_state == ConnectionState.RUNNING) {
                        when (val item = incoming.receive()) {
                            is Frame.Text -> {
                                val element =
                                    runCatching { Json.parseToJsonElement(item.readText()) }.getOrNull() ?: continue
                                val raw = Raw(Data.Json(element), mapOf())
                                logger.debug { "Text received: $element" }
                                decoders.forEach {
                                    logger.debug { "Invoke decoder: ${it.javaClass.simpleName}" }
                                    val result = it.invoke(raw)
                                    if (result != null) {
                                        logger.debug { "Decoder result: $result" }
                                    }
                                    EventEmitter.emit(result)
                                }
                            }

                            is Frame.Binary -> {
                                val raw = Raw(Data.Binary(item.data), mapOf())
                                logger.debug { "Binary received: ${item.data.size} bytes" }
                                decoders.forEach {
                                    logger.debug { "Invoke decoder: ${it.javaClass.simpleName}" }
                                    val result = it.invoke(raw)
                                    if (result != null) {
                                        logger.debug { "Decoder result: $result" }
                                    }
                                    EventEmitter.emit(result)
                                }
                            }

                            is Frame.Close -> {
                                logger.debug { "Close received" }
                                logger.info { "Server Send Close Frame, closing connection..." }
                                _state = ConnectionState.CLOSING
                            }

                            is Frame.Ping -> {
                                logger.debug { "Ping received, replying with Pong" }
                                outgoing.send(Frame.Pong(item.data))
                            }

                            is Frame.Pong -> {
                                // DO NOTHING
                                logger.debug { "Pong received" }
                            }
                        }
                    }
                }
                launch(Dispatchers.IO) {
                    while (_state == ConnectionState.RUNNING) {
                        val message = sendChannel.receive()
                        outgoing.send(Frame.Text(Json.encodeToString(message)))
                        logger.debug { "Sent message: $message" }
                    }
                }
            }
        }
    }

    override fun close() {
        logger.info { "Closing connection" }
        _state = ConnectionState.CLOSING
        sendChannel.close()
        client.closeClient()
    }

    private var _state: ConnectionState = ConnectionState.PREPAREING

    override suspend fun send(data: JsonElement) {
        sendChannel.send(data)
    }
}
