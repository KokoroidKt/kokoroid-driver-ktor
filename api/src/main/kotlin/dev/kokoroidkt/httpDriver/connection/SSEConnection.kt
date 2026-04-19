package dev.kokoroidkt.httpDriver.connection

import dev.kokoroid.transport.connection.Connection
import dev.kokoroidkt.httpDriver.client.HttpClient

/**
 * 使用SSE连接服务器
 * 这一般只能完成事件接收
 *
 * @constructor Create empty SSE connection
 */
abstract class SSEConnection(
    val sseEndpoint: String,
) : HttpClient,
    Connection
