package dev.kokoroidkt.ktorDriver

import dev.kokoroid.transport.connection.ConnectionState
import dev.kokoroid.transport.raw.Data
import dev.kokoroid.transport.raw.Raw
import dev.kokoroidkt.coreApi.utils.getExtensionLogger
import dev.kokoroidkt.driverApi.driver.Driver
import dev.kokoroidkt.driverApi.utils.loadConfigFromFile
import dev.kokoroidkt.driverApi.utils.metadata
import dev.kokoroidkt.httpDriver.factory.ConnectionFactory
import dev.kokoroidkt.ktorDriver.config.Config
import dev.kokoroidkt.ktorDriver.connection.*
import dev.kokoroidkt.ktorDriver.factory.ConnectionFactoryImpl
import dev.kokoroidkt.ktorDriver.util.toKokoroidRequest
import dev.kokoroidkt.ktorDriver.util.toKtorHttpMethod
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.getKoin
import kotlin.time.Duration.Companion.milliseconds

class KtorDriver : Driver() {
    private val logger = getExtensionLogger()

    val ktorServer by lazy {
        val config = Config.config
        embeddedServer(Netty, port = Config.config.port, host = "0.0.0.0") {
            install(WebSockets) {
                pingPeriod = config.websocketServerPingInterval.milliseconds
                timeout = config.websocketServerTimeoutMillisecond.milliseconds
                maxFrameSize = config.websocketServerMaxFrameSize
                masking = config.websocketServerEnableMask
            }
        }
    }

    override fun onLoad() {
        logger.info { "Ktor Driver Version ${metadata()?.version} Loading...." }
        Config.setConfig(loadConfigFromFile(defaultWhenNull = Config.createDefault()))
        loadKoinModules(
            module {
                single<ConnectionFactory> { ConnectionFactoryImpl() }
            },
        )
        logger.info { "Ktor Driver Load Finished" }
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStart() {
        logger.info { "Starting Ktor Connections" }
        val factory = getKoin().get<ConnectionFactory>() as ConnectionFactoryImpl
        factory.connections.forEach { connection ->
            appScope.launch {
                when (connection) {
                    is SSEConnectionImpl -> {
                        connection.run()
                    }

                    is ForwardWebsocketConnectionImpl -> {
                        connection.run()
                    }

                    is ReverseWebsocketConnectionImpl -> {
                        ktorServer.application.routing {
                            webSocket(connection.wsEndPoint) {
                                coroutineScope {
                                    launch(Dispatchers.IO) {
                                        while (connection.state == ConnectionState.RUNNING) {
                                            val item = incoming.receive()
                                            when (item) {
                                                is Frame.Binary -> {
                                                    Raw(Data.Binary(item.data), mapOf())
                                                }

                                                is Frame.Close -> {
                                                    connection.close()
                                                }

                                                is Frame.Ping -> {
                                                    outgoing.send(Frame.Pong(item.data))
                                                }

                                                is Frame.Pong -> {}

                                                is Frame.Text -> {
                                                    runCatching { Json.parseToJsonElement(item.readText()) }
                                                        .getOrNull()
                                                        ?.let {
                                                            Raw(Data.Json(it), emptyMap())
                                                        }
                                                }
                                            }
                                        }
                                    }
                                    launch(Dispatchers.IO) {
                                        while (connection.state == ConnectionState.RUNNING) {
                                            val item = connection.sendChannel.receive()
                                            outgoing.send(Frame.Text(Json.encodeToString(item)))
                                        }
                                    }
                                }
                            }
                        }
                        connection.run()
                    }

                    is HttpPollingConnectionImpl -> {
                        connection.run()
                    }

                    is HttpWebhookConnectionImpl -> {
                        ktorServer.application.routing {
                            connection.endpoint?.let { endpoint ->
                                route(endpoint, connection.httpMethod.toKtorHttpMethod()) {
                                    handle {
                                        connection.receiveChannel.send(call.request.toKokoroidRequest())
                                    }
                                }
                            }
                        }
                        connection.run()
                    }
                }
            }
        }
        ktorServer.start(wait = false)
        logger.info { "Ktor Server Started" }
        factory.connections.forEach {
            when (it) {
                is ForwardWebsocketConnectionImpl -> {
                    logger.info { " · Forward Websocket -> ${it.websocketUrl}" }
                }

                is HttpPollingConnectionImpl -> {
                    logger.info { " · Http Polling -> ${it.pollingEndpoint}" }
                }

                is HttpWebhookConnectionImpl -> {
                    logger.info { " · Http Webhook -> ${it.endpoint}" }
                }

                is ReverseWebsocketConnectionImpl -> {
                    logger.info { " · Reverse Websocket -> ${it.wsEndPoint}" }
                }

                is SSEConnectionImpl -> {
                    logger.info { " · SSE Connection -> ${it.sseEndpoint}" }
                }
            }
        }
    }

    override fun onStop() {
        ktorServer.stop(0, 0)
    }

    override fun onUnload() {
        logger.info { "Ktor Driver Stopping" }
        val factory = getKoin().get<ConnectionFactory>() as ConnectionFactoryImpl
        factory.connections.forEach { it.close() }
    }
}
