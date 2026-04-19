package dev.kokoroidkt.ktorDriver.connection

import dev.kokoroid.transport.connection.ConnectionState
import dev.kokoroid.transport.decoder.Decoder
import dev.kokoroid.transport.raw.Data
import dev.kokoroid.transport.raw.Raw
import dev.kokoroidkt.coreApi.utils.getExtensionLogger
import dev.kokoroidkt.driverApi.transport.EventEmitter
import dev.kokoroidkt.httpDriver.connection.SSEConnection
import dev.kokoroidkt.httpDriver.constants.AttrMagicKeys
import dev.kokoroidkt.httpDriver.http.HttpResponse
import dev.kokoroidkt.ktorDriver.client.HttpClientImpl
import io.ktor.client.plugins.sse.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.File
import java.net.URI

/**
 * 使用 SSE 连接服务器
 *
 * @property baseUrl 协议端的 baseUrl
 * @property sseEndpoint SSE 接收终结点
 * @property header 默认的请求头
 */
class SSEConnectionImpl(
    override val baseUrl: URI,
    sseEndpoint: String,
    override val header: Map<String, List<String>?>,
    engine: io.ktor.client.engine.HttpClientEngine? = null,
) : SSEConnection(sseEndpoint) {
    override val state get() = _state

    /**
     * 已注册的解码器列表
     */
    val decoders: MutableList<Decoder> = mutableListOf()
    val logger = getExtensionLogger()

    override fun registerDecoder(decoder: Decoder) {
        decoders.add(decoder)
    }

    override suspend fun run() {
        coroutineScope {
            launch(Dispatchers.IO) {
                logger.info { "Connected to SSE endpoint: ${baseUrl.toString() + sseEndpoint}" }
                client.ktorClient.sse(
                    host = baseUrl.host,
                    port = if (baseUrl.port == -1) (if (baseUrl.scheme == "https") 443 else 80) else baseUrl.port,
                    path = (baseUrl.path.removeSuffix("/") + "/" + sseEndpoint.removePrefix("/")),
                    scheme = baseUrl.scheme,
                ) {
                    incoming.collect { event ->
                        logger.debug { "Received SSE event: ${event.event} - ${event.data}" }
                        if (event.data == null) {
                            return@collect
                        }
                        decoders.forEach { decoder ->
                            logger.debug { "Invoke decoder: ${decoder.javaClass.simpleName}" }
                            val result =
                                event.data?.let {
                                    decoder.invoke(
                                        Raw(
                                            Data.Json(Json.parseToJsonElement(it)),
                                            mapOf(
                                                AttrMagicKeys.SSE_EVENT_TYPE.value to (event.event ?: "NULL"),
                                            ),
                                        ),
                                    )
                                }
                            if (result != null) {
                                logger.debug { "Decoder result: $result" }
                            }
                            EventEmitter.emit(result)
                        }
                    }
                }
            }
        }
    }

    override fun close() {
        _state = ConnectionState.CLOSING
    }

    private var _state: ConnectionState = ConnectionState.PREPAREING

    /**
     * 内部委托的 HttpClient
     */
    val client =
        HttpClientImpl(
            baseUrl,
            header,
            engine ?: io.ktor.client.engine.cio.CIO
                .create(),
        )

    override suspend fun get(
        endpoint: String,
        params: JsonElement,
        data: JsonElement,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse = client.get(endpoint, params, data, headers, cookies)

    override suspend fun head(
        endpoint: String,
        params: JsonElement,
        data: JsonElement,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse = client.head(endpoint, params, data, headers, cookies)

    override suspend fun post(
        endpoint: String,
        params: JsonElement,
        data: JsonElement,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse = client.post(endpoint, params, data, headers, cookies)

    override suspend fun put(
        endpoint: String,
        params: JsonElement,
        data: JsonElement,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse = client.put(endpoint, params, data, headers, cookies)

    override suspend fun delete(
        endpoint: String,
        params: JsonElement,
        data: JsonElement,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse = client.delete(endpoint, params, data, headers, cookies)

    override suspend fun connect(
        endpoint: String,
        params: JsonElement,
        data: JsonElement,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse = client.connect(endpoint, params, data, headers, cookies)

    override suspend fun options(
        endpoint: String,
        params: JsonElement,
        data: JsonElement,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse = client.options(endpoint, params, data, headers, cookies)

    override suspend fun trace(
        endpoint: String,
        params: JsonElement,
        data: JsonElement,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse = client.trace(endpoint, params, data, headers, cookies)

    override suspend fun patch(
        endpoint: String,
        params: JsonElement,
        data: JsonElement,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse = client.patch(endpoint, params, data, headers, cookies)

    override suspend fun postFile(
        endpoint: String,
        params: JsonElement,
        file: File,
        filePartName: String,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse = client.postFile(endpoint, params, file, filePartName, headers, cookies)

    override suspend fun putFile(
        endpoint: String,
        params: JsonElement,
        file: File,
        filePartName: String,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse = client.putFile(endpoint, params, file, filePartName, headers, cookies)

    override suspend fun patchFile(
        endpoint: String,
        params: JsonElement,
        file: File,
        filePartName: String,
        headers: Map<String, List<String>?>,
        cookies: Map<String, String>,
    ): HttpResponse = client.patchFile(endpoint, params, file, filePartName, headers, cookies)
}
