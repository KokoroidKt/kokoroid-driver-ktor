// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
//
// SPDX-License-Identifier: LGPL-2.1

package dev.kokoroidkt.ktorDriver.factory

import dev.kokoroidkt.httpDriver.connection.*
import dev.kokoroidkt.httpDriver.factory.ConnectionFactory
import dev.kokoroidkt.httpDriver.http.HttpMethod
import dev.kokoroidkt.httpDriver.rule.ServerRule
import dev.kokoroidkt.ktorDriver.connection.*
import io.ktor.client.engine.cio.*
import kotlinx.serialization.json.JsonElement
import java.net.URI

class ConnectionFactoryImpl : ConnectionFactory {
    private val _connections: MutableList<dev.kokoroidkt.transport.connection.Connection> = mutableListOf()
    val connections: List<dev.kokoroidkt.transport.connection.Connection> get() = _connections.toList()

    override fun getForwardWebsocketConn(
        websocketUrl: URI,
        headers: Map<String, List<String>>,
        httpMethod: HttpMethod,
        pingMillisecond: Long,
    ): ForwardWebsocketConnection {
        val impl = ForwardWebsocketConnectionImpl(websocketUrl, headers, CIO.create { }, pingMillisecond, httpMethod)
        _connections.add(impl)
        return impl
    }

    override fun getReverseWebsocketConn(
        websocketEndpoint: String,
        rules: List<ServerRule>,
    ): ReverseWebsocketConnection {
        val impl = ReverseWebsocketConnectionImpl(websocketEndpoint, rules)
        _connections.add(impl)
        return impl
    }

    override fun getHttpPollingConn(
        baseUrl: URI,
        pollingEndpoint: String,
        delayMillisecont: Long,
        headers: Map<String, List<String>>,
        pollingParams: JsonElement,
        pollingData: JsonElement,
        pollingHeader: Map<String, List<String>?>,
        pollingCookie: Map<String, String>,
    ): HttpPollingConnection {
        val impl =
            HttpPollingConnectionImpl(
                baseUrl = baseUrl,
                pollingEndpoint = pollingEndpoint,
                delayMillisecond = delayMillisecont,
                header = headers,
                pollingParams = pollingParams,
                pollingData = pollingData,
                pollingHeader = pollingHeader,
                pollingCookie = pollingCookie,
            )
        _connections.add(impl)
        return impl
    }

    override fun getHttpWebhookConn(
        webhookEndpoint: String,
        baseUrl: URI,
        rules: List<ServerRule>,
        headers: Map<String, List<String>>,
        httpMethod: HttpMethod,
    ): HttpWebhookConnection {
        val impl = HttpWebhookConnectionImpl(baseUrl, webhookEndpoint, rules, headers, CIO.create(), httpMethod)
        _connections.add(impl)
        return impl
    }

    override fun getSSEConn(
        baseUrl: URI,
        sseEndpoint: String,
        headers: Map<String, List<String>>,
    ): SSEConnection {
        val impl = SSEConnectionImpl(baseUrl, sseEndpoint, headers)
        _connections.add(impl)
        return impl
    }
}
