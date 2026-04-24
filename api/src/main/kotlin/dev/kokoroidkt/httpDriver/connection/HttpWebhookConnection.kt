// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
// SPDX-FileContributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1

package dev.kokoroidkt.httpDriver.connection

import dev.kokoroidkt.httpDriver.client.HttpClient
import dev.kokoroidkt.httpDriver.http.HttpMethod
import dev.kokoroidkt.httpDriver.rule.ServerRule
import dev.kokoroidkt.transport.connection.Connection

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
