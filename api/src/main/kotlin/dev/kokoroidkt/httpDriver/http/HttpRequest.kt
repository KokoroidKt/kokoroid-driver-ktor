// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
//
// SPDX-License-Identifier: LGPL-2.1

package dev.kokoroidkt.httpDriver.http

import kotlinx.serialization.json.JsonElement

/**
 * 表示一个传入的 HTTP 请求，主要用于 Webhook 或 反向连接等服务端场景。
 *
 * @property method HTTP 方法 (GET, POST 等)
 * @property path 请求路径
 * @property params 查询参数 (Query Parameters)
 * @property headers 请求头
 * @property json 解析后的 JSON 请求体；如果请求不是 JSON，或没有请求体，则为 `null`
 * @property bodyText 原始请求体文本；如果没有请求体，则为 `null`
 */
data class HttpRequest(
    val method: HttpMethod,
    val path: String,
    val params: Map<String, List<String>> = emptyMap(),
    val headers: Map<String, List<String>> = emptyMap(),
    val json: JsonElement? = null,
    val bodyText: String? = null,
)
