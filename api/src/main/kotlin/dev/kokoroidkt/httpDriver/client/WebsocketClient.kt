// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
// SPDX-FileContributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1

package dev.kokoroidkt.httpDriver.client

import dev.kokoroidkt.httpDriver.http.HttpMethod
import dev.kokoroidkt.transport.client.Client
import kotlinx.serialization.json.JsonElement

abstract class WebsocketClient :
    Client,
    Sendable {
    typealias Callback = suspend (JsonElement) -> Unit

    abstract val header: Map<String, List<String>?>
    abstract val pingMillis: Long
    abstract val method: HttpMethod
}
