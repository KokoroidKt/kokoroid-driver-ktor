// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
//
// SPDX-License-Identifier: LGPL-2.1

package dev.kokoroidkt.ktorDriver.config

import dev.kokoroidkt.coreApi.annotation.WithComment
import kotlinx.serialization.Serializable

@Serializable
@WithComment("Ktor Driver Config", "基本只是些性能设置")
data class Config(
    @WithComment("服务器ip", "默认为0.0.0.0（公网）")
    val ip: String,
    @WithComment("服务器端口", "默认为6710")
    val port: Int,
    @WithComment("Websocket连接中，发送事件的缓冲区应该多大？", "默认为1024")
    val websocketSendChannelSize: Int,
    @WithComment("Websocket连接中，接收事件的缓冲区应该多大？", "默认为1024")
    val websocketReceiveChannelSize: Int,
    @WithComment("SSE最大重连次数", "Ktor Driver在SSE连接断开时只会最大尝试那么多次")
    val sseMaxReconnectTimes: Int,
    @WithComment("SSE重连间隔时间", "Ktor Driver在SSE连接断开时会每隔多少毫秒尝试重连")
    val sseReconnectInterval: Long,
    @WithComment("Http Webhook缓冲区大小", "默认1024")
    val httpWebhookChannelSize: Int,
    @WithComment("Websocket服务器连接超时时间", "默认为20秒")
    val websocketServerTimeoutMillisecond: Long,
    @WithComment("Websocket服务器ping间隔时间", "默认为10秒")
    val websocketServerPingInterval: Long,
    @WithComment("Websocket服务器最大帧大小", "默认为Long.MAX_VALUE")
    val websocketServerMaxFrameSize: Long,
    @WithComment("是否启用掩码？", "默认为false")
    val websocketServerEnableMask: Boolean,
) {
    companion object {
        private var _config: Config? = null

        val config: Config get() = _config ?: throw IllegalStateException("Config not initialized")

        fun setConfig(config: Config) {
            _config = config
        }

        fun createDefault() =
            Config(
                websocketSendChannelSize = 1024,
                websocketReceiveChannelSize = 1024,
                sseMaxReconnectTimes = 3,
                sseReconnectInterval = 1000L,
                httpWebhookChannelSize = 1024,
                ip = "0.0.0.0",
                port = 6710,
                websocketServerTimeoutMillisecond = 20000L,
                websocketServerPingInterval = 10000L,
                websocketServerMaxFrameSize = Long.MAX_VALUE,
                websocketServerEnableMask = false,
            )
    }
}
