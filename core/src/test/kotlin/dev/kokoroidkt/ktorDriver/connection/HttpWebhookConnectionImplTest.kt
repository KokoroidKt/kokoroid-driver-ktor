// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
// SPDX-FileContributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1

package dev.kokoroidkt.ktorDriver.connection

import dev.kokoroidkt.driverApi.transport.EventEmitter
import dev.kokoroidkt.httpDriver.http.HttpRequest
import dev.kokoroidkt.httpDriver.rule.ServerRule
import dev.kokoroidkt.ktorDriver.config.Config
import dev.kokoroidkt.transport.decoder.Decoder
import dev.kokoroidkt.transport.raw.Data
import dev.kokoroidkt.transport.raw.Raw
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.BeforeEach
import java.io.File
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpWebhookConnectionImplTest {
    private val baseUrl = URI("http://callback.com")
    private val endpoint = "/webhook/receive"
    private val header = mapOf("X-Webhook" to listOf("Active"))
    private lateinit var connection: HttpWebhookConnectionImpl
    private val dummyParams = JsonObject(emptyMap())
    private val dummyData = JsonPrimitive("webhook-data")

    @BeforeEach
    fun setup() {
        Config.setConfig(Config.createDefault())
        mockkObject(EventEmitter)
        coEvery { EventEmitter.emit(any()) } returns Unit
        connection =
            HttpWebhookConnectionImpl(
                baseUrl,
                endpoint,
                emptyList(),
                header,
                httpMethod = dev.kokoroidkt.httpDriver.http.HttpMethod.GET,
            )
    }

    @Test
    fun testWebhookConfig() {
        assertEquals(baseUrl, connection.baseUrl)
        assertEquals(endpoint, connection.endpoint)
        assertEquals(header, connection.header)
    }

    @Test
    fun testReceiveAndDecode() =
        runTest {
            val decoder = mockk<Decoder>(relaxed = true)
            val testJson = Json.parseToJsonElement("""{"event": "test"}""").jsonObject
            connection.registerDecoder(decoder)

            val raw = Raw(Data.Json(testJson), mapOf())
            every { decoder.invoke(any()) } returns mockk(relaxed = true)

            connection.decoders.forEach {
                val result = it.invoke(raw)
                EventEmitter.emit(result)
            }

            verify(exactly = 1) { decoder.invoke(any()) }
            coVerify(exactly = 1) { EventEmitter.emit(any()) }

            connection.close()
        }

    @Test
    fun testServerRuleFilter() =
        runTest {
            val rule = mockk<ServerRule>()
            val testJson = Json.parseToJsonElement("""{"event": "ignore"}""").jsonObject
            val request =
                HttpRequest(
                    method = dev.kokoroidkt.httpDriver.http.HttpMethod.POST,
                    path = endpoint,
                    json = testJson,
                )

            val webhookConn =
                HttpWebhookConnectionImpl(
                    baseUrl,
                    endpoint,
                    listOf(rule),
                    header,
                    httpMethod = dev.kokoroidkt.httpDriver.http.HttpMethod.GET,
                )
            coEvery { rule(any()) } returns false

            val decoder = mockk<Decoder>(relaxed = true)
            webhookConn.registerDecoder(decoder)

            // 模拟 run 内部的循环逻辑
            var shouldContinue = true
            webhookConn.rules.forEach {
                if (!it.invoke(request)) {
                    shouldContinue = false
                }
            }

            if (shouldContinue) {
                webhookConn.decoders.forEach {
                    it.invoke(Raw(Data.Json(testJson), emptyMap()))
                }
            }

            coVerify(exactly = 1) { rule(any()) }
            verify(exactly = 0) { decoder.invoke(any()) }
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
            val testConnection =
                HttpWebhookConnectionImpl(
                    baseUrl,
                    endpoint,
                    emptyList(),
                    header,
                    mockEngine,
                    httpMethod = dev.kokoroidkt.httpDriver.http.HttpMethod.GET,
                )

            testConnection.post("/api/reply", dummyParams, dummyData)

            val tempFile = File.createTempFile("webhook-upload", ".tmp")
            try {
                testConnection.putFile("/upload", JsonObject(emptyMap()), tempFile)
            } finally {
                tempFile.delete()
            }

            // Verify that the correct HTTP methods were used
            assertEquals(HttpMethod.Post, capturedMethods[0])
            assertEquals(HttpMethod.Put, capturedMethods[1])
        }
}
