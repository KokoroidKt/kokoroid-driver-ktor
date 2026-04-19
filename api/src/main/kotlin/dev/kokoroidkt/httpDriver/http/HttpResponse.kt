package dev.kokoroidkt.httpDriver.http

import kotlinx.serialization.json.JsonElement

/**
 * 表示一次 HTTP 请求的响应结果，主要面向 JSON API 场景。
 *
 * 这个对象同时保留了解析后的 JSON 数据与原始文本内容，方便在以下场景使用：
 * - 响应体是 JSON 时，优先读取 [json]
 * - 响应体不是 JSON，或解析失败时，可通过 [bodyText] 获取原始文本
 * - 需要根据 HTTP 状态码判断请求是否成功时，可使用 [isSuccess]
 *
 * @property statusCode HTTP 状态码
 * @property reasonPhrase HTTP 状态描述
 * @property json 解析后的 JSON 响应体；如果响应不是 JSON，或没有响应体，则为 `null`
 * @property headers 响应头
 * @property bodyText 原始响应文本；如果没有响应体，则为 `null`
 */
data class HttpResponse(
    val statusCode: Int,
    val reasonPhrase: String,
    val json: JsonElement?,
    val headers: Map<String, List<String>> = emptyMap(),
    val bodyText: String? = null,
) {
    /**
     * 是否为成功响应。
     *
     * 当前按 HTTP 2xx 状态码范围判断。
     */
    val isSuccess = statusCode in 200..299
}
