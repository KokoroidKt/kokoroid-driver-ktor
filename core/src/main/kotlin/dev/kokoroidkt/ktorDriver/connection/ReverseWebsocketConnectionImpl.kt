package dev.kokoroidkt.ktorDriver.connection

import dev.kokoroid.transport.connection.ConnectionState
import dev.kokoroid.transport.decoder.Decoder
import dev.kokoroid.transport.raw.Raw
import dev.kokoroidkt.coreApi.utils.getExtensionLogger
import dev.kokoroidkt.driverApi.transport.EventEmitter
import dev.kokoroidkt.httpDriver.client.WebsocketServer
import dev.kokoroidkt.httpDriver.connection.ReverseWebsocketConnection
import dev.kokoroidkt.httpDriver.rule.ServerRule
import dev.kokoroidkt.ktorDriver.client.WebsocketServerImpl
import dev.kokoroidkt.ktorDriver.config.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement

class ReverseWebsocketConnectionImpl(
    serverEndpoint: String,
    rules: List<ServerRule> = emptyList(),
) : ReverseWebsocketConnection(serverEndpoint, rules) {
    override val state get() = _state

    val decoders: MutableList<Decoder> = mutableListOf()

    val server: WebsocketServer = WebsocketServerImpl(serverEndpoint)

    /**
     * Receive channel
     * 当接口收到websocket请求，包装并发送到此Channel
     */
    val receiveChannel: Channel<Raw> = Channel(Config.config.websocketReceiveChannelSize)

    /**
     * Send channel
     * 要发送请求的时候，向此Channel写入，Websocket服务器会从这里取走JsonElement并发送
     */
    val sendChannel: Channel<JsonElement> = Channel(Config.config.websocketSendChannelSize)
    val logger = getExtensionLogger()

    override fun registerDecoder(decoder: Decoder) {
        decoders.add(decoder)
    }

    override suspend fun run() {
        _state = ConnectionState.RUNNING
        coroutineScope {
            launch(Dispatchers.IO) {
                whileLoop@ while (_state == ConnectionState.RUNNING) {
                    val item = receiveChannel.receive()
                    logger.debug { "Received request: $item" }
                    decoders.forEach {
                        val event = it.invoke(item)
                        EventEmitter.emit(event)
                    }
                }
            }
        }
    }

    override fun close() {
        _state = ConnectionState.CLOSING
    }

    private var _state: ConnectionState = ConnectionState.PREPAREING

    override suspend fun send(data: JsonElement) {
        sendChannel.send(data)
    }
}
