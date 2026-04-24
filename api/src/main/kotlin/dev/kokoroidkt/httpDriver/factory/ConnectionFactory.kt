// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
// SPDX-FileContributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1

package dev.kokoroidkt.httpDriver.factory

import dev.kokoroidkt.httpDriver.connection.*
import dev.kokoroidkt.httpDriver.http.HttpMethod
import dev.kokoroidkt.httpDriver.rule.ServerRule
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import org.koin.java.KoinJavaComponent.getKoin
import java.net.URI

interface ConnectionFactory {
    fun getForwardWebsocketConn(
        websocketUrl: URI,
        headers: Map<String, List<String>> = mapOf(),
        httpMethod: HttpMethod = HttpMethod.GET,
        pingMillisecond: Long = 20_000L,
    ): ForwardWebsocketConnection

    fun getReverseWebsocketConn(
        websocketEndpoint: String,
        rules: List<ServerRule> = emptyList(),
    ): ReverseWebsocketConnection

    fun getHttpPollingConn(
        baseUrl: URI,
        pollingEndpoint: String,
        delayMillisecont: Long,
        headers: Map<String, List<String>> = mapOf(),
        pollingParams: JsonElement = buildJsonObject { },
        pollingData: JsonElement = buildJsonObject { },
        pollingHeader: Map<String, List<String>?> = emptyMap(),
        pollingCookie: Map<String, String> = emptyMap(),
    ): HttpPollingConnection

    fun getHttpWebhookConn(
        webhookEndpoint: String,
        baseUrl: URI,
        rules: List<ServerRule> = emptyList(),
        headers: Map<String, List<String>> = mapOf(),
        httpMethod: HttpMethod = HttpMethod.POST,
    ): HttpWebhookConnection

    fun getSSEConn(
        baseUrl: URI,
        sseEndpoint: String,
        headers: Map<String, List<String>> = mapOf(),
    ): SSEConnection

    companion object {
        fun create(): ConnectionFactory = getKoin().get<ConnectionFactory>()
    }
}
