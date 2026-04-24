// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
//
// SPDX-License-Identifier: LGPL-2.1

package dev.kokoroidkt.httpDriver.websocket

import kotlinx.serialization.json.JsonElement

fun interface WebsocketCallback {
    fun callback(data: JsonElement)
}
