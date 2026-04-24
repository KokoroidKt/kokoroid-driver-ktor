// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
// SPDX-FileContributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1

package dev.kokoroidkt.httpDriver.client

import dev.kokoroidkt.transport.client.Client

abstract class WebsocketServer(
    val wsEndPoint: String,
) : Client,
    Sendable
