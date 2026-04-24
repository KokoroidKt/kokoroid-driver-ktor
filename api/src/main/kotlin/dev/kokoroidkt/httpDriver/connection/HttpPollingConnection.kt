// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
// SPDX-FileContributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1

package dev.kokoroidkt.httpDriver.connection

import dev.kokoroid.transport.connection.Connection
import dev.kokoroid.transport.connection.Heartbeatable
import dev.kokoroidkt.httpDriver.client.HttpClient
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject

/**
 * 使用HTTP轮询的方式连接服务端
 * 不应该用于发起除了连接协议端以外的HTTP请求
 * 应该只负责把请求向指定的地址发出去
 *
 * 实现此类需要实现Heartbeatable的heartbeat方法，用以轮询服务器
 *
 * @property baseUrl 协议端的baseUrl
 * @property pollingEndpoint 需要轮询的终结点
 * @constructor Create empty Http polling connection
 */
abstract class HttpPollingConnection(
    val pollingEndpoint: String,
    val pollingParams: JsonElement = buildJsonObject { },
    val pollingData: JsonElement = buildJsonObject { },
    val pollingHeader: Map<String, List<String>?> = emptyMap(),
    val pollingCookie: Map<String, String> = emptyMap(),
) : Connection,
    Heartbeatable,
    HttpClient
