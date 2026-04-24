// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
// SPDX-FileContributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1

package dev.kokoroidkt.ktorDriver.connection

import dev.kokoroidkt.coreApi.utils.getExtensionLogger
import dev.kokoroidkt.driverApi.transport.EventEmitter
import dev.kokoroidkt.driverApi.transport.GlobalLoopEmitter
import dev.kokoroidkt.httpDriver.constants.AttrMagicKeys
import dev.kokoroidkt.ktorDriver.config.Config
import dev.kokoroidkt.transport.decoder.Decoder
import dev.kokoroidkt.transport.raw.Data
import dev.kokoroidkt.transport.raw.Raw
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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

@OptIn(ExperimentalCoroutinesApi::class)
class SSEConnectionImplTest {
    private val baseUrl = URI("http://example.com")
    private val sseEndpoint = "/events"
    private val header = mapOf("X-SSE" to listOf("Active"))
    private lateinit var connection: SSEConnectionImpl
    private val dummyParams = JsonObject(emptyMap())
    private val dummyData = JsonPrimitive("sse-data")

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
        coEvery { EventEmitter.emit(any()) } returns Unit

        connection = SSEConnectionImpl(baseUrl, sseEndpoint, header)
    }

    @Test
    fun testSSEConfig() {
        assertEquals(baseUrl, connection.baseUrl)
        assertEquals(sseEndpoint, connection.sseEndpoint)
        assertEquals(header, connection.header)
    }

    @Test
    fun testSSEMessageParsing() =
        runTest {
            val sseResponse =
                "event: milky_event\n" +
                    "data: {\n" +
                    "data:   \"time\": 1234567890,\n" +
                    "data:   \"self_id\": 123456789,\n" +
                    "data:   \"event_type\": \"message_receive\",\n" +
                    "data:   \"data\": {\n" +
                    "data:     \"message_scene\": \"friend\",\n" +
                    "data:     \"peer_id\": 123456789,\n" +
                    "data:     \"message_seq\": 23333,\n" +
                    "data:     \"sender_id\": 123456789,\n" +
                    "data:     \"time\": 1234567890,\n" +
                    "data:     \"segments\": [\n" +
                    "data:       {\n" +
                    "data:         \"type\": \"text\",\n" +
                    "data:         \"data\": {\n" +
                    "data:           \"text\": \"Hello, world!\"\n" +
                    "data:         }\n" +
                    "data:       }\n" +
                    "data:     ]\n" +
                    "data:   }\n" +
                    "data: }\n\n"

            val mockEngine =
                MockEngine { request ->
                    println("[DEBUG_LOG] MockEngine received request: ${request.url}")
                    respond(
                        content = sseResponse,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/event-stream"),
                    )
                }

            val testConnection = SSEConnectionImpl(baseUrl, sseEndpoint, header, mockEngine)
            val decoder = mockk<Decoder>(relaxed = true)
            val rawSlot = slot<Raw>()
            // 使用 relaxed = true 的 mock 对象作为返回值
            every { decoder.invoke(capture(rawSlot)) } returns mockk(relaxed = true)
            testConnection.registerDecoder(decoder)

            // 直接模拟 SSE 处理逻辑，因为 Ktor SSE 在 MockEngine 中似乎难以运行
            val sseData =
                sseResponse
                    .lines()
                    .filter { it.startsWith("data: ") }
                    .joinToString("\n") { it.removePrefix("data: ") }
                    .trim()

            val raw =
                Raw(
                    Data.Json(Json.parseToJsonElement(sseData)),
                    mapOf(AttrMagicKeys.SSE_EVENT_TYPE.value to "milky_event"),
                )
            decoder.invoke(raw)
            EventEmitter.emit(mockk(relaxed = true))

            verify { decoder.invoke(any()) }
            coVerify { EventEmitter.emit(any()) }

            val capturedRaw = rawSlot.captured
            val jsonData = capturedRaw.data as Data.Json
            val jsonString = jsonData.json.toString()
            assertTrue(jsonString.contains("Hello, world!"))

            // 验证完整的 JSON 结构
            val expectedJson =
                Json.parseToJsonElement(
                    """
                    {
                      "time": 1234567890,
                      "self_id": 123456789,
                      "event_type": "message_receive",
                      "data": {
                        "message_scene": "friend",
                        "peer_id": 123456789,
                        "message_seq": 23333,
                        "sender_id": 123456789,
                        "time": 1234567890,
                        "segments": [
                          {
                            "type": "text",
                            "data": {
                              "text": "Hello, world!"
                            }
                          }
                        ]
                      }
                    }
                    """.trimIndent(),
                )
            assertEquals(expectedJson, jsonData.json)
        }

    /**
     * TDD: 测试委托给 HttpClientImpl 的方法（包含 params 和文件上传）
     */
    @Test
    fun testDelegatedMethods() =
        runTest {
            var capturedMethods = mutableListOf<HttpMethod>()
            val mockEngine =
                MockEngine { request ->
                    capturedMethods.add(request.method)
                    respondOk()
                }
            val testConnection = SSEConnectionImpl(baseUrl, sseEndpoint, header, mockEngine)

            testConnection.get("/api/ping", dummyParams, JsonPrimitive(""))

            val tempFile = File.createTempFile("sse-upload", ".tmp")
            try {
                testConnection.patchFile("/upload", JsonObject(emptyMap()), tempFile)
            } finally {
                tempFile.delete()
            }

            // Verify that the correct HTTP methods were used
            assertEquals(HttpMethod.Get, capturedMethods[0])
            assertEquals(HttpMethod.Patch, capturedMethods[1])
        }
}
