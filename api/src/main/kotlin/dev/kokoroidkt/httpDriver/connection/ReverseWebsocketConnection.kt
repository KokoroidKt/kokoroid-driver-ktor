// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
// SPDX-FileContributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1

package dev.kokoroidkt.httpDriver.connection

import dev.kokoroidkt.httpDriver.client.WebsocketServer
import dev.kokoroidkt.httpDriver.rule.ServerRule
import dev.kokoroidkt.transport.connection.Connection

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
