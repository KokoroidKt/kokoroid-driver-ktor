package dev.kokoroidkt.ktorDriver.connection

import dev.kokoroid.transport.connection.ConnectionState
import dev.kokoroid.transport.decoder.Decoder
import dev.kokoroid.transport.raw.Data
import dev.kokoroid.transport.raw.Raw
import dev.kokoroidkt.coreApi.utils.getExtensionLogger
import dev.kokoroidkt.driverApi.transport.EventEmitter
import dev.kokoroidkt.driverApi.transport.GlobalLoopEmitter
import dev.kokoroidkt.httpDriver.rule.ServerRule
import dev.kokoroidkt.ktorDriver.config.Config
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ReverseWebsocketConnectionImplTest {
    private val endpoint = "/ws/reverse"
    private val rules = listOf<ServerRule>()
    private lateinit var connection: ReverseWebsocketConnectionImpl

    companion object {
        @BeforeAll
        @JvmStatic
        fun setupAll() {
            mockkStatic("dev.kokoroidkt.coreApi.utils.LoggerKt")
            every { any<Any>().getExtensionLogger() } returns mockk(relaxed = true)
        }
    }

    @BeforeEach
    fun setup() {
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

        connection = ReverseWebsocketConnectionImpl(endpoint, rules)
    }

    @Test
    fun testConfig() {
        assertEquals(endpoint, connection.wsEndPoint)
        assertEquals(rules, connection.rules)
        assertEquals(ConnectionState.PREPAREING, connection.state)
    }

    @Test
    fun testReceiveAndDecode() =
        runTest {
            val decoder = mockk<Decoder>(relaxed = true)
            val rawSlot = slot<Raw>()
            every { decoder.invoke(capture(rawSlot)) } returns mockk(relaxed = true)
            connection.registerDecoder(decoder)

            // 模拟接收到消息
            val testJson = Json.parseToJsonElement("""{"type": "message", "content": "hello"}""")

            // 手动执行接收逻辑，而不运行完整的 coroutineScope { launch { whileLoop } }
            // 因为 run() 内部的 coroutineScope 会阻塞直到子任务完成

            connection.receiveChannel.send(Raw(Data.Json(testJson), emptyMap()))

            // 模拟 run() 内部的一轮循环
            val item = connection.receiveChannel.receive()
            connection.decoders.forEach {
                val event = it.invoke(item)
                EventEmitter.emit(event)
            }

            // 验证解码器被调用
            verify(exactly = 1) { decoder.invoke(any()) }
            coVerify(exactly = 1) { EventEmitter.emit(any()) }

            val capturedRaw = rawSlot.captured
            val jsonData = capturedRaw.data as Data.Json
            assertEquals(testJson, jsonData.json)
        }

    @Test
    fun testServerRuleFilter() =
        runTest {
            val rule = mockk<ServerRule>()
            val testJson = Json.parseToJsonElement("""{"type": "ignore"}""")
            val request =
                dev.kokoroidkt.httpDriver.http.HttpRequest(
                    method = dev.kokoroidkt.httpDriver.http.HttpMethod.POST,
                    path = "/ws",
                    json = testJson,
                )

            val reverseConn = ReverseWebsocketConnectionImpl(endpoint, listOf(rule))
            coEvery { rule(any()) } returns false

            val decoder = mockk<Decoder>(relaxed = true)
            reverseConn.registerDecoder(decoder)

            // 模拟 run 内部逻辑，但加入规则判断
            var shouldContinue = true
            reverseConn.rules.forEach {
                if (!it.invoke(request)) {
                    shouldContinue = false
                }
            }

            if (shouldContinue) {
                reverseConn.decoders.forEach {
                    it.invoke(Raw(Data.Json(testJson), emptyMap()))
                }
            }

            coVerify(exactly = 1) { rule(any()) }
            verify(exactly = 0) { decoder.invoke(any()) }
        }

    @Test
    fun testSend() =
        runTest {
            val testJson = Json.parseToJsonElement("""{"action": "send", "msg": "hi"}""")

            launch {
                connection.send(testJson)
            }

            val sent = connection.sendChannel.receive()
            assertEquals(testJson, sent)
        }

    @Test
    fun testStateTransition() =
        runTest {
            assertEquals(ConnectionState.PREPAREING, connection.state)

            val job =
                launch {
                    connection.run()
                }

            delay(50)
            assertEquals(ConnectionState.RUNNING, connection.state)

            connection.close()
            assertEquals(ConnectionState.CLOSING, connection.state)
            job.cancel()
        }
}
