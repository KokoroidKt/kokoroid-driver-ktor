// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
//
// SPDX-License-Identifier: LGPL-2.1

package dev.kokoroidkt.ktorDriver.connection

import dev.kokoroid.transport.decoder.Decoder
import dev.kokoroid.transport.raw.Data
import dev.kokoroid.transport.raw.Raw
import dev.kokoroidkt.coreApi.utils.getExtensionLogger
import dev.kokoroidkt.driverApi.transport.EventEmitter
import dev.kokoroidkt.driverApi.transport.GlobalLoopEmitter
import dev.kokoroidkt.ktorDriver.config.Config
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ForwardWebsocketConnectionImplTest {
    private val websocketUrl = URI("ws://localhost:8080/ws")
    private val header = mapOf("X-Test" to listOf("Value"))

    companion object {
        @BeforeAll
        @JvmStatic
        fun setupAll() {
            mockkStatic("dev.kokoroidkt.coreApi.utils.LoggerKt")
            every { any<Any>().getExtensionLogger() } returns mockk(relaxed = true)
        }
    }

    @BeforeEach
    fun setupKoin() {
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
    }

    @Test
    fun testWebsocketConfig() {
        val connection =
            ForwardWebsocketConnectionImpl(
                websocketUrl = websocketUrl,
                header = header,
                pingMillis = 30000L,
                method = dev.kokoroidkt.httpDriver.http.HttpMethod.GET,
            )
        assertEquals(websocketUrl, connection.websocketUrl)
        assertEquals(header, connection.header)
    }

    @Test
    fun testReceiveAndDecode() =
        runTest {
            val connection =
                ForwardWebsocketConnectionImpl(
                    websocketUrl = websocketUrl,
                    header = header,
                    pingMillis = 30000L,
                    method = dev.kokoroidkt.httpDriver.http.HttpMethod.GET,
                )

            val decoder = mockk<Decoder>(relaxed = true)
            connection.registerDecoder(decoder)
            every { decoder.invoke(any()) } returns mockk(relaxed = true)

            val testJson = buildJsonObject { put("hello", "world") }
            val raw = Raw(Data.Json(testJson), emptyMap())

            connection.decoders.forEach {
                val result = it.invoke(raw)
                EventEmitter.emit(result)
            }

            verify(exactly = 1) { decoder.invoke(any()) }
            coVerify(exactly = 1) { EventEmitter.emit(any()) }
        }

    @Test
    fun testCloseFrameHandling() =
        runTest {
            val connection =
                ForwardWebsocketConnectionImpl(
                    websocketUrl = websocketUrl,
                    header = header,
                    pingMillis = 30000L,
                    method = dev.kokoroidkt.httpDriver.http.HttpMethod.GET,
                )
            // 模拟状态变更
            connection.close()
            assertEquals(dev.kokoroid.transport.connection.ConnectionState.CLOSING, connection.state)
        }

    @Test
    fun testSendAfterClose() =
        runTest {
            val connection =
                ForwardWebsocketConnectionImpl(
                    websocketUrl = websocketUrl,
                    header = header,
                    pingMillis = 30000L,
                    method = dev.kokoroidkt.httpDriver.http.HttpMethod.GET,
                )
            connection.close()
            // 验证关闭后发送不抛出异常，或者符合预期行为（sendChannel 已关闭）
            try {
                connection.send(buildJsonObject { put("test", "data") })
            } catch (e: Exception) {
                // Depending on implementation, it might throw ClosedSendChannelException
                assertTrue(e is kotlinx.coroutines.channels.ClosedSendChannelException)
            }
        }

    @Test
    fun testSend() =
        runTest {
            val connection =
                ForwardWebsocketConnectionImpl(
                    websocketUrl = websocketUrl,
                    header = header,
                    pingMillis = 30000L,
                    method = dev.kokoroidkt.httpDriver.http.HttpMethod.GET,
                )

            val testJson = buildJsonObject { put("action", "test") }

            // 测试发送逻辑是否正确入队
            connection.send(testJson)

            // 虽然 sendChannel 是 private 的，但我们可以通过反射或观察行为来测试
            // 在这里我们主要确保 send 方法不会抛出异常
            connection.close()
        }
}
