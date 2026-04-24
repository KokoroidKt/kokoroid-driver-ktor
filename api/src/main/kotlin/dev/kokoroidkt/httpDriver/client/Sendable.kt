// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
// SPDX-FileContributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1

package dev.kokoroidkt.httpDriver.client

import kotlinx.serialization.json.JsonElement

interface Sendable {
    /**
     * 向Websocket连接的另一头发送一个消息
     *
     * @param data 要发送的数据
     */
    suspend fun send(data: JsonElement)
}
