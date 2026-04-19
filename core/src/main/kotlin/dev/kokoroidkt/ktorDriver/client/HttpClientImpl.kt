package dev.kokoroidkt.ktorDriver.client

import dev.kokoroidkt.httpDriver.client.HttpClient
import dev.kokoroidkt.httpDriver.http.HttpResponse
import dev.kokoroidkt.ktorDriver.config.Config.Companion.config
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import java.net.URI
import kotlin.time.Duration.Companion.milliseconds

/**
 * HttpClient 的 Ktor 实现
 *
 * @property baseUrl 服务端的 baseUrl
 * @property header 默认的请求头
 */
class HttpClientImpl(
    override val baseUrl: URI,
    override val header: Map<String, List<String>?>,
    engine: HttpClientEngine = CIO.create(),
) : HttpClient {
    /**
     * 内部使用的 Ktor HttpClient 实例
     */
    val ktorClient =
        io.ktor.client.HttpClient(engine) {
            install(ContentNegotiation) {
                json()
            }
            install(SSE) {
                maxReconnectionAttempts = config.sseMaxReconnectTimes
                reconnectionTime = config.sseReconnectInterval.milliseconds
            }
            defaultRequest {
                url {
                    takeFrom(baseUrl)
                }
                this@HttpClientImpl.header.forEach { (key, value) ->
                    value?.forEach { item ->
                        headers.append(key, item)
                    }
                }
            }
        }

    /**
     * 构建请求的辅助方法，处理参数、头信息和 Cookie
     */
    private fun HttpRequestBuilder.prepareRequest(
        params: JsonElement,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ) {
        // 处理 URL 参数
        if (params is JsonObject) {
            params.forEach { (key, value) ->
                if (value is JsonPrimitive) {
                    url.parameters.append(key, value.content)
                }
            }
        }

        // 处理额外的请求头
        headers.forEach { (key, value) ->
            value?.forEach { item ->
                this.headers.append(key, item)
            }
        }

        // 处理 Cookie
        cookies.forEach { (key, value) ->
            cookie(key, value)
        }
    }

    private fun HttpRequestBuilder.prepareBody(data: JsonElement) {
        if (data !is JsonPrimitive || data.content.isNotEmpty()) {
            contentType(ContentType.Application.Json)
            setBody(data)
        }
    }

    private suspend fun io.ktor.client.statement.HttpResponse.toHttpReply(): dev.kokoroidkt.httpDriver.http.HttpResponse {
        val statusCode = this.status.value
        val reasonPhrase = this.status.description
        val headers =
            this.headers.entries().associate { entry ->
                entry.key to entry.value.toList()
            }

        val bodyText =
            try {
                this.bodyAsText()
            } catch (e: Exception) {
                null
            }

        val json =
            try {
                if (bodyText != null) {
                    kotlinx.serialization.json.Json
                        .parseToJsonElement(bodyText)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }

        return dev.kokoroidkt.httpDriver.http
            .HttpResponse(statusCode, reasonPhrase, json, headers, bodyText)
    }

    /**
     * 执行文件上传的内部通用方法
     */
    private suspend fun executeFileUpload(
        method: HttpMethod,
        endpoint: String,
        params: JsonElement,
        file: File,
        filePartName: String,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse =
        ktorClient
            .request(endpoint) {
                this.method = method
                prepareRequest(params, headers, cookies)
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                filePartName,
                                file.readBytes(),
                                Headers.build {
                                    append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                                },
                            )
                        },
                    ),
                )
            }.toHttpReply()

    override suspend fun get(
        endpoint: String,
        params: JsonElement,
        data: JsonElement,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse =
        ktorClient
            .get(endpoint) {
                prepareRequest(params, headers, cookies)
                prepareBody(data)
            }.toHttpReply()

    override suspend fun head(
        endpoint: String,
        params: JsonElement,
        data: JsonElement,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse =
        ktorClient
            .head(endpoint) {
                prepareRequest(params, headers, cookies)
                prepareBody(data)
            }.toHttpReply()

    override suspend fun post(
        endpoint: String,
        params: JsonElement,
        data: JsonElement,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse =
        ktorClient
            .post(endpoint) {
                prepareRequest(params, headers, cookies)
                prepareBody(data)
            }.toHttpReply()

    override suspend fun put(
        endpoint: String,
        params: JsonElement,
        data: JsonElement,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse =
        ktorClient
            .put(endpoint) {
                prepareRequest(params, headers, cookies)
                prepareBody(data)
            }.toHttpReply()

    override suspend fun delete(
        endpoint: String,
        params: JsonElement,
        data: JsonElement,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse =
        ktorClient
            .delete(endpoint) {
                prepareRequest(params, headers, cookies)
                prepareBody(data)
            }.toHttpReply()

    override suspend fun connect(
        endpoint: String,
        params: JsonElement,
        data: JsonElement,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse =
        ktorClient
            .request(endpoint) {
                method = HttpMethod.parse("CONNECT")
                prepareRequest(params, headers, cookies)
                prepareBody(data)
            }.toHttpReply()

    override suspend fun options(
        endpoint: String,
        params: JsonElement,
        data: JsonElement,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse =
        ktorClient
            .options(endpoint) {
                prepareRequest(params, headers, cookies)
                prepareBody(data)
            }.toHttpReply()

    override suspend fun trace(
        endpoint: String,
        params: JsonElement,
        data: JsonElement,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse =
        ktorClient
            .request(endpoint) {
                method = HttpMethod.parse("TRACE")
                prepareRequest(params, headers, cookies)
                prepareBody(data)
            }.toHttpReply()

    override suspend fun patch(
        endpoint: String,
        params: JsonElement,
        data: JsonElement,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse =
        ktorClient
            .patch(endpoint) {
                prepareRequest(params, headers, cookies)
                prepareBody(data)
            }.toHttpReply()

    override suspend fun postFile(
        endpoint: String,
        params: JsonElement,
        file: File,
        filePartName: String,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse = executeFileUpload(HttpMethod.Post, endpoint, params, file, filePartName, headers, cookies)

    override suspend fun putFile(
        endpoint: String,
        params: JsonElement,
        file: File,
        filePartName: String,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse = executeFileUpload(HttpMethod.Put, endpoint, params, file, filePartName, headers, cookies)

    override suspend fun patchFile(
        endpoint: String,
        params: JsonElement,
        file: File,
        filePartName: String,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse = executeFileUpload(HttpMethod.Patch, endpoint, params, file, filePartName, headers, cookies)
}
