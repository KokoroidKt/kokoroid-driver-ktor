package dev.kokoroidkt.ktorDriver.connection

import dev.kokoroid.transport.connection.ConnectionState
import dev.kokoroid.transport.decoder.Decoder
import dev.kokoroid.transport.raw.Data
import dev.kokoroid.transport.raw.Raw
import dev.kokoroidkt.coreApi.utils.getExtensionLogger
import dev.kokoroidkt.driverApi.transport.EventEmitter
import dev.kokoroidkt.httpDriver.connection.HttpPollingConnection
import dev.kokoroidkt.httpDriver.http.HttpResponse
import dev.kokoroidkt.ktorDriver.client.HttpClientImpl
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import java.io.File
import java.net.URI

/**
 * 使用 HTTP 轮询的方式连接服务端
 *
 * @property baseUrl 协议端的 baseUrl
 * @property pollingEndpoint 需要轮询的终结点
 * @property delayMillisecond 轮询间隔（毫秒）
 * @property header 默认的请求头
 * @param engine Ktor HttpClient 引擎
 */
class HttpPollingConnectionImpl(
    override val baseUrl: URI,
    pollingEndpoint: String,
    override val delayMillisecond: Long,
    override val header: Map<String, List<String>?>,
    engine: HttpClientEngine = CIO.create(),
    pollingParams: JsonElement,
    pollingData: JsonElement,
    pollingHeader: Map<String, List<String>?>,
    pollingCookie: Map<String, String>,
) : HttpPollingConnection(pollingEndpoint, pollingParams, pollingData, pollingHeader, pollingCookie) {
    override val state get() = _state
    val logger = getExtensionLogger()
    private val client = HttpClientImpl(baseUrl, header, engine)

    /**
     * 已注册的解码器列表
     */
    val decoders: MutableList<Decoder> = mutableListOf()

    override fun registerDecoder(decoder: Decoder) {
        decoders.add(decoder)
    }

    override suspend fun run() {
        _state = ConnectionState.RUNNING
    }

    override fun close() {
        _state = ConnectionState.CLOSING
    }

    private var _state: ConnectionState = ConnectionState.PREPAREING

    override suspend fun heartbeat() {
        coroutineScope {
            launch(Dispatchers.IO) {
                val result =
                    client.get(pollingEndpoint, params = pollingParams, data = pollingData, headers = pollingHeader)
                if (result.json == null) {
                    logger.debug { "Heartbeat failed for endpoint: $pollingEndpoint, cannot get json response, resp: $result" }
                    return@launch
                }

                decoders.forEach {
                    val event = it.invoke(Raw(Data.Json(result.json!!), mapOf()))
                    EventEmitter.emit(event)
                }
            }
        }
    }

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
