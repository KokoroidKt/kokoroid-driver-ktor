package dev.kokoroidkt.httpDriver.client

import dev.kokoroid.transport.client.Client

abstract class WebsocketServer(
    val wsEndPoint: String,
) : Client,
    Sendable
