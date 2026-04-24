// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
//
// SPDX-License-Identifier: LGPL-2.1

package dev.kokoroidkt.ktorDriver.connection

import dev.kokoroidkt.coreApi.utils.getExtensionLogger
import dev.kokoroidkt.driverApi.transport.EventEmitter
import dev.kokoroidkt.httpDriver.connection.HttpWebhookConnection
import dev.kokoroidkt.httpDriver.http.HttpRequest
import dev.kokoroidkt.httpDriver.http.HttpResponse
import dev.kokoroidkt.httpDriver.rule.ServerRule
import dev.kokoroidkt.ktorDriver.client.HttpClientImpl
import dev.kokoroidkt.ktorDriver.config.Config
import dev.kokoroidkt.transport.connection.ConnectionState
import dev.kokoroidkt.transport.decoder.Decoder
import dev.kokoroidkt.transport.raw.Data
import dev.kokoroidkt.transport.raw.Raw
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import java.io.File
import java.net.URI

/**
 * 用于使用 Webhook 方式连接服务端
 *
 * @property baseUrl 协议端的 baseUrl
 * @property endpoint Webhook 接收终结点
 * @property header 默认的请求头
 * @param engine Ktor HttpClient 引擎
 */
class HttpWebhookConnectionImpl(
    override val baseUrl: URI,
    endpoint: String = "/ws",
    rule: List<ServerRule> = emptyList(),
    override val header: Map<String, List<String>?>,
    engine: HttpClientEngine = CIO.create(),
    httpMethod: dev.kokoroidkt.httpDriver.http.HttpMethod,
) : HttpWebhookConnection(endpoint, rule, httpMethod) {
    override val state get() = _state

    val logger = getExtensionLogger()
    val config = Config.config

    /**
     * 已注册的解码器列表
     */
    val decoders: MutableList<Decoder> = mutableListOf()

    /**
     * Receive channel
     * 当接口收到http请求，包装并发送到此Channel
     */
    val receiveChannel: Channel<HttpRequest> = Channel(config.httpWebhookChannelSize)

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
                    rules.forEach {
                        if (!it.invoke(item)) {
                            continue@whileLoop
                        }
                    }
                    decoders.forEach {
                        val event = item.json?.let { json -> it.invoke(Raw(Data.Json(json), mapOf())) }
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

    /**
     * 内部委托的 HttpClient
     */
    val client = HttpClientImpl(baseUrl, header, engine)

    override suspend fun get(
        endpoint: String,
        params: JsonElement,
        data: JsonElement,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse = client.get(endpoint, params, data, headers, cookies)

    override suspend fun head(
        endpoint: String,
        params: JsonElement,
        data: JsonElement,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse = client.head(endpoint, params, data, headers, cookies)

    override suspend fun post(
        endpoint: String,
        params: JsonElement,
        data: JsonElement,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse = client.post(endpoint, params, data, headers, cookies)

    override suspend fun put(
        endpoint: String,
        params: JsonElement,
        data: JsonElement,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse = client.put(endpoint, params, data, headers, cookies)

    override suspend fun delete(
        endpoint: String,
        params: JsonElement,
        data: JsonElement,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse = client.delete(endpoint, params, data, headers, cookies)

    override suspend fun connect(
        endpoint: String,
        params: JsonElement,
        data: JsonElement,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse = client.connect(endpoint, params, data, headers, cookies)

    override suspend fun options(
        endpoint: String,
        params: JsonElement,
        data: JsonElement,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse = client.options(endpoint, params, data, headers, cookies)

    override suspend fun trace(
        endpoint: String,
        params: JsonElement,
        data: JsonElement,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse = client.trace(endpoint, params, data, headers, cookies)

    override suspend fun patch(
        endpoint: String,
        params: JsonElement,
        data: JsonElement,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse = client.patch(endpoint, params, data, headers, cookies)

    override suspend fun postFile(
        endpoint: String,
        params: JsonElement,
        file: File,
        filePartName: String,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse = client.postFile(endpoint, params, file, filePartName, headers, cookies)

    override suspend fun putFile(
        endpoint: String,
        params: JsonElement,
        file: File,
        filePartName: String,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse = client.putFile(endpoint, params, file, filePartName, headers, cookies)

    override suspend fun patchFile(
        endpoint: String,
        params: JsonElement,
        file: File,
        filePartName: String,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse = client.patchFile(endpoint, params, file, filePartName, headers, cookies)
}
