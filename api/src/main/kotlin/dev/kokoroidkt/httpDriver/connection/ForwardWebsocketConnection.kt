// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
// SPDX-FileContributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1

package dev.kokoroidkt.httpDriver.connection

import dev.kokoroid.transport.connection.Connection
import dev.kokoroidkt.httpDriver.client.WebsocketClient
import java.net.URI

/**
 * 用于正向连接Websocket，即：
 *  - 协议端实现Websocket服务器
 *  - Kokoroid作为客户端连接到协议端
 *
 * @constructor Create empty Forward websocket connection
 */
abstract class ForwardWebsocketConnection(
    val websocketUrl: URI,
) : WebsocketClient(),
    Connection
