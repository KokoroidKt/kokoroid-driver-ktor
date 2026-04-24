// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
//
// SPDX-License-Identifier: LGPL-2.1

package dev.kokoroidkt.ktorDriver.client

import dev.kokoroidkt.ktorDriver.config.Config
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.BeforeEach
import java.io.File
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class HttpClientImplTest {
    private val baseUrl = URI("http://localhost:8080")
    private val defaultHeader = mapOf("X-Default" to listOf("Value"))
    private val dummyParams = JsonObject(mapOf("q" to JsonPrimitive("search")))
    private val dummyData = JsonPrimitive("test-value")
    private val endpoint = "/api/test"

    @BeforeEach
    fun setup() {
        Config.setConfig(Config.createDefault())
    }

    @Test
    fun testBaseUrlAndHeader() {
        val mockEngine = MockEngine { respondOk() }
        val client = HttpClientImpl(baseUrl, defaultHeader, mockEngine)

        assertEquals(baseUrl, client.baseUrl)
        assertEquals(defaultHeader, client.header)
    }

    /**
     * TDD: 测试 GET 请求
     */
    @Test
    fun testGetRequest() =
        runTest {
            var capturedMethod: HttpMethod? = null
            var capturedUrl: String? = null
            var capturedHeaders: Headers? = null

            val testEngine =
                MockEngine { request ->
                    capturedMethod = request.method
                    capturedUrl = request.url.toString()
                    capturedHeaders = request.headers
                    respondOk()
                }
            val testClient = HttpClientImpl(baseUrl, defaultHeader, testEngine)

            val reply =
                testClient.get(
                    endpoint = endpoint,
                    params = dummyParams,
                    data = JsonPrimitive(""),
                    headers = mapOf("X-Extra" to listOf("ExtraValue")),
                    cookies = mapOf("Session" to "12345"),
                )

            assertEquals(HttpMethod.Get, capturedMethod)
            assertEquals("http://localhost:8080/api/test?q=search", capturedUrl)
            assertNotNull(capturedHeaders)
            assertEquals("ExtraValue", capturedHeaders["X-Extra"])
            assertEquals("Session=12345", capturedHeaders["Cookie"])

            assertEquals(200, reply.statusCode)
            assertEquals("OK", reply.reasonPhrase)
            assertEquals(true, reply.isSuccess)
        }

    /**
     * TDD: 测试 HEAD 请求
     */
    @Test
    fun testHeadRequest() =
        runTest {
            var capturedMethod: HttpMethod? = null
            val testEngine =
                MockEngine { request ->
                    capturedMethod = request.method
                    respondOk()
                }
            val testClient = HttpClientImpl(baseUrl, defaultHeader, testEngine)

            testClient.head(endpoint, dummyParams, JsonPrimitive(""))
            assertEquals(HttpMethod.Head, capturedMethod)
        }

    /**
     * TDD: 测试 POST 请求
     */
    @Test
    fun testPostRequest() =
        runTest {
            var capturedMethod: HttpMethod? = null
            val testEngine =
                MockEngine { request ->
                    capturedMethod = request.method
                    respondOk()
                }
            val testClient = HttpClientImpl(baseUrl, defaultHeader, testEngine)

            testClient.post(endpoint, dummyParams, dummyData)
            assertEquals(HttpMethod.Post, capturedMethod)
        }

    /**
     * TDD: 测试 PUT 请求
     */
    @Test
    fun testPutRequest() =
        runTest {
            var capturedMethod: HttpMethod? = null
            val testEngine =
                MockEngine { request ->
                    capturedMethod = request.method
                    respondOk()
                }
            val testClient = HttpClientImpl(baseUrl, defaultHeader, testEngine)

            testClient.put(endpoint, dummyParams, dummyData)
            assertEquals(HttpMethod.Put, capturedMethod)
        }

    /**
     * TDD: 测试 DELETE 请求
     */
    @Test
    fun testDeleteRequest() =
        runTest {
            var capturedMethod: HttpMethod? = null
            val testEngine =
                MockEngine { request ->
                    capturedMethod = request.method
                    respondOk()
                }
            val testClient = HttpClientImpl(baseUrl, defaultHeader, testEngine)

            testClient.delete(endpoint, dummyParams, dummyData)
            assertEquals(HttpMethod.Delete, capturedMethod)
        }

    /**
     * TDD: 测试 OPTIONS 请求
     */
    @Test
    fun testOptionsRequest() =
        runTest {
            var capturedMethod: HttpMethod? = null
            val testEngine =
                MockEngine { request ->
                    capturedMethod = request.method
                    respondOk()
                }
            val testClient = HttpClientImpl(baseUrl, defaultHeader, testEngine)

            testClient.options(endpoint, dummyParams, JsonPrimitive(""))
            assertEquals(HttpMethod.Options, capturedMethod)
        }

    /**
     * TDD: 测试 TRACE 请求
     */
    @Test
    fun testTraceRequest() =
        runTest {
            var capturedMethod: HttpMethod? = null
            val testEngine =
                MockEngine { request ->
                    capturedMethod = request.method
                    respondOk()
                }
            val testClient = HttpClientImpl(baseUrl, defaultHeader, testEngine)

            testClient.trace(endpoint, dummyParams, JsonPrimitive(""))
            assertEquals(HttpMethod.Trace, capturedMethod)
        }

    /**
     * TDD: 测试 PATCH 请求
     */
    @Test
    fun testPatchRequest() =
        runTest {
            var capturedMethod: HttpMethod? = null
            val testEngine =
                MockEngine { request ->
                    capturedMethod = request.method
                    respondOk()
                }
            val testClient = HttpClientImpl(baseUrl, defaultHeader, testEngine)

            testClient.patch(endpoint, dummyParams, dummyData)
            assertEquals(HttpMethod.Patch, capturedMethod)
        }

    /**
     * TDD: 测试 POST 文件上传
     */
    @Test
    fun testPostFile() =
        runTest {
            var capturedMethod: HttpMethod? = null
            var capturedContentType: String? = null

            val testEngine =
                MockEngine { request ->
                    capturedMethod = request.method
                    // 在 Ktor MockEngine 中，ContentType 可能会在请求头中，也可能在 OutgoingContent 本身
                    capturedContentType = request.headers["Content-Type"] ?: request.body.contentType?.toString()
                    respondOk()
                }
            val testClient = HttpClientImpl(baseUrl, defaultHeader, testEngine)

            val tempFile = File.createTempFile("post-test", ".txt")
            tempFile.writeText("hello post")
            try {
                testClient.postFile(
                    endpoint = "/upload",
                    params = JsonObject(emptyMap()),
                    file = tempFile,
                    filePartName = "attachment",
                )

                assertEquals(HttpMethod.Post, capturedMethod)
                assertNotNull(capturedContentType, "ContentType should not be null")
                val isMultipart = capturedContentType?.contains("multipart/form-data") == true
                assertEquals(
                    true,
                    isMultipart,
                    "Content-Type should be multipart/form-data, but was $capturedContentType",
                )
            } finally {
                tempFile.delete()
            }
        }

    /**
     * TDD: 测试 PUT 文件上传
     */
    @Test
    fun testPutFile() =
        runTest {
            var capturedMethod: HttpMethod? = null
            val testEngine =
                MockEngine { request ->
                    capturedMethod = request.method
                    respondOk()
                }
            val testClient = HttpClientImpl(baseUrl, defaultHeader, testEngine)

            val tempFile = File.createTempFile("put-test", ".txt")
            tempFile.writeText("hello put")
            try {
                testClient.putFile(
                    endpoint = "/upload",
                    params = dummyParams,
                    file = tempFile,
                )
            } finally {
                tempFile.delete()
            }
            assertEquals(HttpMethod.Put, capturedMethod)
        }

    /**
     * TDD: 测试 PATCH 文件上传
     */
    @Test
    fun testPatchFile() =
        runTest {
            var capturedMethod: HttpMethod? = null
            val testEngine =
                MockEngine { request ->
                    capturedMethod = request.method
                    respondOk()
                }
            val testClient = HttpClientImpl(baseUrl, defaultHeader, testEngine)

            val tempFile = File.createTempFile("patch-test", ".txt")
            tempFile.writeText("hello patch")
            try {
                testClient.patchFile(
                    endpoint = "/upload",
                    params = dummyParams,
                    file = tempFile,
                )
            } finally {
                tempFile.delete()
            }
            assertEquals(HttpMethod.Patch, capturedMethod)
        }

    /**
     * TDD: 测试默认头信息
     */
    @Test
    fun testDefaultHeaders() =
        runTest {
            var capturedHeaders: Headers? = null
            val testEngine =
                MockEngine { request ->
                    capturedHeaders = request.headers
                    respondOk()
                }
            val testClient = HttpClientImpl(baseUrl, defaultHeader, testEngine)

            testClient.get(endpoint, JsonObject(emptyMap()), JsonPrimitive(""))

            assertNotNull(capturedHeaders)
            assertEquals("Value", capturedHeaders["X-Default"])
        }

    /**
     * TDD: 测试响应处理
     */
    @Test
    fun testResponseHandling() =
        runTest {
            val testEngine =
                MockEngine { request ->
                    respond(
                        content = "{\"message\":\"success\"}",
                        status = HttpStatusCode.OK,
                        headers =
                            headers {
                                append("Content-Type", "application/json")
                            },
                    )
                }
            val testClient = HttpClientImpl(baseUrl, defaultHeader, testEngine)

            val reply = testClient.get(endpoint, dummyParams, JsonPrimitive(""))

            assertEquals(200, reply.statusCode)
            assertEquals("OK", reply.reasonPhrase)
            assertEquals(true, reply.isSuccess)
            assertNotNull(reply.json)
            val jsonObj = reply.json as? JsonObject
            assertNotNull(jsonObj)
            val message = jsonObj["message"] as? JsonPrimitive
            assertNotNull(message)
            assertEquals("success", message.content)
            assertEquals("{\"message\":\"success\"}", reply.bodyText)
            assertEquals("application/json", reply.headers["Content-Type"]?.firstOrNull())
        }

    /**
     * TDD: 测试非JSON响应
     */
    @Test
    fun testNonJsonResponse() =
        runTest {
            val testEngine =
                MockEngine { request ->
                    respond(
                        content = "Plain text response",
                        status = HttpStatusCode.OK,
                        headers =
                            headers {
                                append("Content-Type", "text/plain")
                            },
                    )
                }
            val testClient = HttpClientImpl(baseUrl, defaultHeader, testEngine)

            val reply = testClient.get(endpoint, dummyParams, JsonPrimitive(""))

            assertEquals(200, reply.statusCode)
            assertEquals("OK", reply.reasonPhrase)
            assertEquals(true, reply.isSuccess)
            assertEquals(null, reply.json)
            assertEquals("Plain text response", reply.bodyText)
            assertEquals("text/plain", reply.headers["Content-Type"]?.firstOrNull())
        }

    /**
     * TDD: 测试错误响应
     */
    @Test
    fun testErrorResponse() =
        runTest {
            val testEngine =
                MockEngine { request ->
                    respond(
                        content = "{\"error\":\"Not Found\"}",
                        status = HttpStatusCode.NotFound,
                        headers =
                            headers {
                                append("Content-Type", "application/json")
                            },
                    )
                }
            val testClient = HttpClientImpl(baseUrl, defaultHeader, testEngine)

            val reply = testClient.get(endpoint, dummyParams, JsonPrimitive(""))

            assertEquals(404, reply.statusCode)
            assertEquals("Not Found", reply.reasonPhrase)
            assertEquals(false, reply.isSuccess)
            assertNotNull(reply.json)
            val jsonObj = reply.json as? JsonObject
            assertNotNull(jsonObj)
            val error = jsonObj["error"] as? JsonPrimitive
            assertNotNull(error)
            assertEquals("Not Found", error.content)
            assertEquals("{\"error\":\"Not Found\"}", reply.bodyText)
        }

    /**
     * TDD: 测试空响应体
     */
    @Test
    fun testEmptyResponse() =
        runTest {
            val testEngine =
                MockEngine { request ->
                    respond(
                        content = "",
                        status = HttpStatusCode.NoContent,
                        headers = headersOf(),
                    )
                }
            val testClient = HttpClientImpl(baseUrl, defaultHeader, testEngine)

            val reply = testClient.get(endpoint, dummyParams, JsonPrimitive(""))

            assertEquals(204, reply.statusCode)
            assertEquals("No Content", reply.reasonPhrase)
            assertEquals(true, reply.isSuccess)
            assertEquals(null, reply.json)
            assertEquals("", reply.bodyText)
        }

    /**
     * TDD: 测试无效JSON响应
     */
    @Test
    fun testInvalidJsonResponse() =
        runTest {
            val testEngine =
                MockEngine { request ->
                    respond(
                        content = "Invalid JSON {",
                        status = HttpStatusCode.OK,
                        headers =
                            headers {
                                append("Content-Type", "application/json")
                            },
                    )
                }
            val testClient = HttpClientImpl(baseUrl, defaultHeader, testEngine)

            val reply = testClient.get(endpoint, dummyParams, JsonPrimitive(""))

            assertEquals(200, reply.statusCode)
            assertEquals("OK", reply.reasonPhrase)
            assertEquals(true, reply.isSuccess)
            assertEquals(null, reply.json)
            assertEquals("Invalid JSON {", reply.bodyText)
        }
}
