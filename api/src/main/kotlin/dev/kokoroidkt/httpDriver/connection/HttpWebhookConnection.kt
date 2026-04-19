package dev.kokoroidkt.httpDriver.connection

import dev.kokoroid.transport.connection.Connection
import dev.kokoroidkt.httpDriver.client.HttpClient
import dev.kokoroidkt.httpDriver.http.HttpMethod
import dev.kokoroidkt.httpDriver.rule.ServerRule

/**
 * 用于使用Webhook方式连接服务端
 *

 * @property endpoint
 * @constructor Create an empty Http webhook connection
 */
abstract class HttpWebhookConnection(
    val endpoint: String?,
    val rules: List<ServerRule>,
    val httpMethod: HttpMethod = HttpMethod.POST,
) : Connection,
    HttpClient
