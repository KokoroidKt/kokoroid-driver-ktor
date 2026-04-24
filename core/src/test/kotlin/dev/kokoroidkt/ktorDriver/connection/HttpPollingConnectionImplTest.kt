// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
// SPDX-FileContributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1

package dev.kokoroidkt.ktorDriver.connection

import dev.kokoroidkt.coreApi.utils.getExtensionLogger
import dev.kokoroidkt.driverApi.transport.EventEmitter
import dev.kokoroidkt.driverApi.transport.GlobalLoopEmitter
import dev.kokoroidkt.ktorDriver.config.Config
import io.ktor.client.engine.mock.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.File
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HttpPollingConnectionImplTest {
    private val baseUrl = URI("http://api.example.com")
    private val pollingEndpoint = "/v1/poll"
    private val delay: Long = 1000
    private val header = mapOf("X-Test" to listOf("Value"))
    private val pollingParams = JsonObject(emptyMap())
    private val pollingData = JsonPrimitive("")
    private val pollingHeader = mapOf<String, List<String>?>()
    private val pollingCookie = mapOf<String, String>()
    private lateinit var connection: HttpPollingConnectionImpl
    private val dummyParams = JsonObject(emptyMap())
    private val dummyData = JsonPrimitive("poll-data")

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
        stopKoin()
        startKoin {
            modules(
                module {
                    single { mockk<GlobalLoopEmitter>(relaxed = true) }
                },
            )
        }
        mockkObject(EventEmitter)
        // 模拟 EventEmitter.emit 为空操作，防止它尝试调用 Koin
        io.mockk.coEvery { EventEmitter.emit(any()) } returns Unit

        connection =
            HttpPollingConnectionImpl(
                baseUrl,
                pollingEndpoint,
                delay,
                header,
                pollingParams = pollingParams,
                pollingData = pollingData,
                pollingHeader = pollingHeader,
                pollingCookie = pollingCookie,
            )
    }

    @Test
    fun testPollingConfig() {
        assertEquals(baseUrl, connection.baseUrl)
        assertEquals(pollingEndpoint, connection.pollingEndpoint)
        assertEquals(delay, connection.delayMillisecond)
        assertEquals(header, connection.header)
        assertEquals(dev.kokoroid.transport.connection.ConnectionState.PREPAREING, connection.state)
    }

    @Test
    fun testRunAndClose() =
        runTest {
            connection.run()
            assertEquals(dev.kokoroid.transport.connection.ConnectionState.RUNNING, connection.state)

            connection.close()
            assertEquals(dev.kokoroid.transport.connection.ConnectionState.CLOSING, connection.state)
        }

    @Test
    fun testRegisterDecoder() {
        val decoder = mockk<dev.kokoroid.transport.decoder.Decoder>()
        connection.registerDecoder(decoder)
        assertTrue(connection.decoders.contains(decoder))
    }

    @Test
    fun testHeartbeatSuccess() =
        runTest {
            val responseJson = buildJsonObject { put("event", JsonPrimitive("test")) }
            val mockEngine =
                MockEngine { request ->
                    assertEquals(pollingEndpoint, request.url.encodedPath)
                    respond(
                        content = responseJson.toString(),
                        headers = io.ktor.http.headersOf(io.ktor.http.HttpHeaders.ContentType, "application/json"),
                    )
                }

            val testConnection =
                HttpPollingConnectionImpl(
                    baseUrl,
                    pollingEndpoint,
                    delay,
                    header,
                    mockEngine,
                    pollingParams = pollingParams,
                    pollingData = pollingData,
                    pollingHeader = pollingHeader,
                    pollingCookie = pollingCookie,
                )

            val decoder = mockk<dev.kokoroid.transport.decoder.Decoder>()
            every { decoder.invoke(any()) } returns null
            testConnection.registerDecoder(decoder)

            testConnection.heartbeat()

            verify(timeout = 2000) { decoder.invoke(any()) }
            io.mockk.coVerify(timeout = 2000) { EventEmitter.emit(any()) }
        }

    @Test
    fun testHeartbeatFailureNoJson() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    respondOk("Not a JSON")
                }

            val testConnection =
                HttpPollingConnectionImpl(
                    baseUrl,
                    pollingEndpoint,
                    delay,
                    header,
                    mockEngine,
                    pollingParams = pollingParams,
                    pollingData = pollingData,
                    pollingHeader = pollingHeader,
                    pollingCookie = pollingCookie,
                )

            val decoder = mockk<dev.kokoroid.transport.decoder.Decoder>()
            testConnection.registerDecoder(decoder)

            testConnection.heartbeat()

            verify(exactly = 0) { decoder.invoke(any()) }
        }

    @Test
    fun testRunIdempotency() =
        runTest {
            connection.run()
            assertEquals(dev.kokoroid.transport.connection.ConnectionState.RUNNING, connection.state)
            connection.run()
            assertEquals(dev.kokoroid.transport.connection.ConnectionState.RUNNING, connection.state)
        }

    /**
     * TDD: 测试委托给 HttpClientImpl 的方法（包含 params 和文件上传）
     */
    @Test
    fun testMockEngineIntegration() =
        runTest {
            var capturedMethods = mutableListOf<io.ktor.http.HttpMethod>()
            val mockEngine =
                MockEngine { request ->
                    capturedMethods.add(request.method)
                    respondOk()
                }
            val testConnection =
                HttpPollingConnectionImpl(
                    baseUrl,
                    pollingEndpoint,
                    delay,
                    header,
                    mockEngine,
                    pollingParams = pollingParams,
                    pollingData = pollingData,
                    pollingHeader = pollingHeader,
                    pollingCookie = pollingCookie,
                )

            testConnection.get(pollingEndpoint, dummyParams, JsonPrimitive(""))
            testConnection.post("/api/action", dummyParams, dummyData)

            val tempFile = File.createTempFile("poll-upload", ".tmp")
            try {
                testConnection.postFile("/upload", JsonObject(emptyMap()), tempFile)
            } finally {
                tempFile.delete()
            }

            // Verify that the correct HTTP methods were used
            assertEquals(io.ktor.http.HttpMethod.Get, capturedMethods[0])
            assertEquals(io.ktor.http.HttpMethod.Post, capturedMethods[1])
            assertEquals(io.ktor.http.HttpMethod.Post, capturedMethods[2])
        }
}
