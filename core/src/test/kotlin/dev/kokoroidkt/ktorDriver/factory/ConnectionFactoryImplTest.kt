package dev.kokoroidkt.ktorDriver.factory

import dev.kokoroidkt.coreApi.utils.getExtensionLogger
import dev.kokoroidkt.driverApi.transport.GlobalLoopEmitter
import dev.kokoroidkt.httpDriver.http.HttpMethod
import dev.kokoroidkt.ktorDriver.config.Config
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConnectionFactoryImplTest {
    private lateinit var factory: ConnectionFactoryImpl

    companion object {
        @BeforeAll
        @JvmStatic
        fun setupAll() {
            mockkStatic("dev.kokoroidkt.coreApi.utils.LoggerKt")
            every { any<Any>().getExtensionLogger() } returns mockk(relaxed = true)
        }
    }

    @BeforeEach
    fun setup() {
        Config.setConfig(Config.createDefault())
        factory = ConnectionFactoryImpl()
        // 模拟 Koin 环境，因为某些 Connection 实现类可能会访问 Koin
        stopKoin()
        startKoin {
            modules(
                module {
                    single { mockk<GlobalLoopEmitter>(relaxed = true) }
                },
            )
        }
    }

    @Test
    fun testGetForwardWebsocketConn() {
        val url = URI("ws://localhost:8080")
        val conn = factory.getForwardWebsocketConn(url, emptyMap(), HttpMethod.GET, 30000L)
        assertEquals(url, conn.websocketUrl)
        assertTrue(factory.connections.any { it === conn })
    }

    @Test
    fun testGetReverseWebsocketConn() {
        val endpoint = "/ws/reverse"
        val conn = factory.getReverseWebsocketConn(endpoint, emptyList())
        assertEquals(endpoint, conn.wsEndPoint)
        assertTrue(factory.connections.any { it === conn })
    }

    @Test
    fun testGetHttpPollingConn() {
        val url = URI("http://localhost:8080")
        val conn =
            factory.getHttpPollingConn(
                url,
                "/poll",
                1000L,
                emptyMap(),
                JsonObject(emptyMap()),
                JsonPrimitive(""),
                emptyMap(),
                emptyMap(),
            )
        assertEquals(url, conn.baseUrl)
        assertEquals(1000L, conn.delayMillisecond)
        assertTrue(factory.connections.any { it === conn })
    }

    @Test
    fun testGetHttpWebhookConn() {
        val url = URI("http://localhost:8080")
        val conn = factory.getHttpWebhookConn("/webhook", url, emptyList(), emptyMap(), HttpMethod.POST)
        assertEquals(url, conn.baseUrl)
        assertEquals("/webhook", conn.endpoint)
        assertTrue(factory.connections.any { it === conn })
    }

    @Test
    fun testGetSSEConn() {
        val url = URI("http://localhost:8080")
        val conn = factory.getSSEConn(url, "/sse", emptyMap())
        assertEquals(url, conn.baseUrl)
        assertEquals("/sse", conn.sseEndpoint)
        assertTrue(factory.connections.any { it === conn })
    }
}
