package dev.kokoroidkt.ktorDriver.client

import dev.kokoroidkt.httpDriver.client.WebsocketServer
import kotlinx.serialization.json.JsonElement

class WebsocketServerImpl(
    wsEndPoint: String,
) : WebsocketServer(wsEndPoint) {
    override suspend fun send(data: JsonElement) {
        TODO("向 Websocket 服务器的所有客户端或特定客户端发送数据")
    }
}
