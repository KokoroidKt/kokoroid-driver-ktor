// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
// SPDX-FileContributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1

package dev.kokoroidkt.httpDriver.connection

import dev.kokoroidkt.httpDriver.client.HttpClient
import dev.kokoroidkt.transport.connection.Connection

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
