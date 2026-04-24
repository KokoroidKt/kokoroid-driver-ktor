// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
// SPDX-FileContributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1

package dev.kokoroidkt.httpDriver.client

import dev.kokoroidkt.httpDriver.http.HttpResponse
import dev.kokoroidkt.transport.client.Client
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import java.io.File
import java.net.URI

/**
 * Http client 应该只用于与服务端连接
 *
 * @constructor Create empty Http client
 */
interface HttpClient : Client {
    /**
     * 服务端的 baseUrl
     */
    val baseUrl: URI

    /**
     * 默认的请求头
     */
    val header: Map<String, List<String>?>

    /**
     * 发送一个 GET 请求
     *
     * @param endpoint 动作终结点
     * @param params URL 参数
     * @param data 请求体数据
     * @param headers 额外的请求头
     * @param cookies 额外的 Cookie
     */
    suspend fun get(
        endpoint: String,
        params: JsonElement = buildJsonObject { },
        data: JsonElement = buildJsonObject { },
        headers: Map<String, List<String>?> = emptyMap(),
        cookies: Map<String, String> = emptyMap(),
    ): HttpResponse

    /**
     * 发送一个 HEAD 请求
     *
     * @param endpoint 动作终结点
     * @param params URL 参数
     * @param data 请求体数据
     * @param headers 额外的请求头
     * @param cookies 额外的 Cookie
     */
    suspend fun head(
        endpoint: String,
        params: JsonElement = buildJsonObject { },
        data: JsonElement = buildJsonObject { },
        headers: Map<String, List<String>?> = emptyMap(),
        cookies: Map<String, String> = emptyMap(),
    ): HttpResponse

    /**
     * 发送一个 POST 请求
     *
     * @param endpoint 动作终结点
     * @param params URL 参数
     * @param data 请求体数据
     * @param headers 额外的请求头
     * @param cookies 额外的 Cookie
     */
    suspend fun post(
        endpoint: String,
        params: JsonElement = buildJsonObject { },
        data: JsonElement = buildJsonObject { },
        headers: Map<String, List<String>?> = emptyMap(),
        cookies: Map<String, String> = emptyMap(),
    ): HttpResponse

    /**
     * 发送一个 PUT 请求
     *
     * @param endpoint 动作终结点
     * @param params URL 参数
     * @param data 请求体数据
     * @param headers 额外的请求头
     * @param cookies 额外的 Cookie
     */
    suspend fun put(
        endpoint: String,
        params: JsonElement = buildJsonObject { },
        data: JsonElement = buildJsonObject { },
        headers: Map<String, List<String>?> = emptyMap(),
        cookies: Map<String, String> = emptyMap(),
    ): HttpResponse

    /**
     * 发送一个 DELETE 请求
     *
     * @param endpoint 动作终结点
     * @param params URL 参数
     * @param data 请求体数据
     * @param headers 额外的请求头
     * @param cookies 额外的 Cookie
     */
    suspend fun delete(
        endpoint: String,
        params: JsonElement = buildJsonObject { },
        data: JsonElement = buildJsonObject { },
        headers: Map<String, List<String>?> = emptyMap(),
        cookies: Map<String, String> = emptyMap(),
    ): HttpResponse

    /**
     * 发送一个 CONNECT 请求
     *
     * @param endpoint 动作终结点
     * @param params URL 参数
     * @param data 请求体数据
     * @param headers 额外的请求头
     * @param cookies 额外的 Cookie
     */
    suspend fun connect(
        endpoint: String,
        params: JsonElement = buildJsonObject { },
        data: JsonElement = buildJsonObject { },
        headers: Map<String, List<String>?> = emptyMap(),
        cookies: Map<String, String> = emptyMap(),
    ): HttpResponse

    /**
     * 发送一个 OPTIONS 请求
     *
     * @param endpoint 动作终结点
     * @param params URL 参数
     * @param data 请求体数据
     * @param headers 额外的请求头
     * @param cookies 额外的 Cookie
     */
    suspend fun options(
        endpoint: String,
        params: JsonElement = buildJsonObject { },
        data: JsonElement = buildJsonObject { },
        headers: Map<String, List<String>?> = emptyMap(),
        cookies: Map<String, String> = emptyMap(),
    ): HttpResponse

    /**
     * 发送一个 TRACE 请求
     *
     * @param endpoint 动作终结点
     * @param params URL 参数
     * @param data 请求体数据
     * @param headers 额外的请求头
     * @param cookies 额外的 Cookie
     */
    suspend fun trace(
        endpoint: String,
        params: JsonElement = buildJsonObject { },
        data: JsonElement = buildJsonObject { },
        headers: Map<String, List<String>?> = emptyMap(),
        cookies: Map<String, String> = emptyMap(),
    ): HttpResponse

    /**
     * 发送一个 PATCH 请求
     *
     * @param endpoint 动作终结点
     * @param params URL 参数
     * @param data 请求体数据
     * @param headers 额外的请求头
     * @param cookies 额外的 Cookie
     */
    suspend fun patch(
        endpoint: String,
        params: JsonElement = buildJsonObject { },
        data: JsonElement = buildJsonObject { },
        headers: Map<String, List<String>?> = emptyMap(),
        cookies: Map<String, String> = emptyMap(),
    ): HttpResponse

    /**
     * 使用 POST 方法上传文件
     *
     * @param endpoint 动作终结点
     * @param params URL 参数
     * @param file 待上传的文件
     * @param filePartName 文件在 multipart 里的名称，默认为 "file"
     * @param headers 额外的请求头
     * @param cookies 额外的 Cookie
     */
    suspend fun postFile(
        endpoint: String,
        params: JsonElement = buildJsonObject { },
        file: File,
        filePartName: String = "file",
        headers: Map<String, List<String>?> = emptyMap(),
        cookies: Map<String, String> = emptyMap(),
    ): HttpResponse

    /**
     * 使用 PUT 方法上传文件
     *
     * @param endpoint 动作终结点
     * @param params URL 参数
     * @param file 待上传的文件
     * @param filePartName 文件在 multipart 里的名称，默认为 "file"
     * @param headers 额外的请求头
     * @param cookies 额外的 Cookie
     */
    suspend fun putFile(
        endpoint: String,
        params: JsonElement = buildJsonObject { },
        file: File,
        filePartName: String = "file",
        headers: Map<String, List<String>?> = emptyMap(),
        cookies: Map<String, String> = emptyMap(),
    ): HttpResponse

    /**
     * 使用 PATCH 方法上传文件
     *
     * @param endpoint 动作终结点
     * @param params URL 参数
     * @param file 待上传的文件
     * @param filePartName 文件在 multipart 里的名称，默认为 "file"
     * @param headers 额外的请求头
     * @param cookies 额外的 Cookie
     */
    suspend fun patchFile(
        endpoint: String,
        params: JsonElement = buildJsonObject { },
        file: File,
        filePartName: String = "file",
        headers: Map<String, List<String>?> = emptyMap(),
        cookies: Map<String, String> = emptyMap(),
    ): HttpResponse
}
