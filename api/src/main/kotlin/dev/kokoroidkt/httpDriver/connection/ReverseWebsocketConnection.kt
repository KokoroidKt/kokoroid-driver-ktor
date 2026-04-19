package dev.kokoroidkt.httpDriver.connection

import dev.kokoroid.transport.connection.Connection
import dev.kokoroidkt.httpDriver.client.WebsocketServer
import dev.kokoroidkt.httpDriver.rule.ServerRule

/**
 * 用于反向连接Websocket， 即：
 *  - Kokoroid开启一个Websocket服务器
 *  - 协议端作为客户端连接到Kokoroid
 *
 * @property serverEndpoint
 * @constructor Create empty Reverse websocket connection
 */
abstract class ReverseWebsocketConnection(
    serverEndpoint: String,
    val rules: List<ServerRule> = emptyList(),
) : WebsocketServer(serverEndpoint),
    Connection
