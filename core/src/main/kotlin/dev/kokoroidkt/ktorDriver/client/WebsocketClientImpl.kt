package dev.kokoroidkt.ktorDriver.client

import dev.kokoroidkt.httpDriver.client.WebsocketClient
import dev.kokoroidkt.httpDriver.http.HttpMethod
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.JsonElement

class WebsocketClientImpl(
    override val header: Map<String, List<String>?>,
    engine: HttpClientEngine? = null,
    override val pingMillis: Long,
    override val method: HttpMethod,
) : WebsocketClient() {
    val client =
        if (engine != null) {
            HttpClient(engine) {
                install(WebSockets) {
                    pingIntervalMillis = pingMillis
                }
                install(ContentNegotiation) {
                    json()
                }
            }
        } else {
            HttpClient(CIO) {
                install(WebSockets)
                install(ContentNegotiation) {
                    json()
                }
            }
        }

    /**
     * 由于Ktor的工作原理，不要用这个方法
     * 去使用对应Connection的send方法
     *
     * @param data
     */
    override suspend fun send(data: JsonElement): Unit = throw NotImplementedError("DO NOT USE WEBSOCKET CLIENT IMPL TO SEND MESSAGE")

    internal fun closeClient() {
        client.close()
    }
}
