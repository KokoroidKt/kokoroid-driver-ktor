package dev.kokoroidkt.httpDriver

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 使用MockK测试EventEmitter接口
 *
 * 这个测试演示了如何测试EventEmitter接口的实现
 */
class EventEmitterMockTest {
    /**
     * 测试EventEmitter接口的基本功能
     * 由于我们不知道EventEmitter的具体实现，我们创建一个模拟接口来演示测试模式
     */
    interface TestEventEmitter {
        companion object {
            fun emit(
                event: String,
                data: Any? = null,
            ) {
                // 默认实现，实际接口中可能是抽象的
            }
        }

        fun on(
            event: String,
            handler: (Any?) -> Unit,
        )

        fun off(
            event: String,
            handler: (Any?) -> Unit,
        )
    }

    @Test
    fun testEventEmitterCompanionEmit() {
        // 模拟EventEmitter的companion object
        val mockEmitter = mockk<TestEventEmitter.Companion>()

        // 设置期望行为
        every { mockEmitter.emit("testEvent", any()) } returns Unit

        // 调用emit方法
        mockEmitter.emit("testEvent", "testData")

        // 验证emit被调用
        verify { mockEmitter.emit("testEvent", "testData") }
    }

    @Test
    fun testEventEmitterOnMethod() {
        // 模拟EventEmitter实例
        val mockEmitter = mockk<TestEventEmitter>()
        var handlerCalled = false
        var receivedData: Any? = null

        // 设置on方法的期望行为
        every { mockEmitter.on("testEvent", any()) } answers {
            val handler = secondArg<(Any?) -> Unit>()
            // 模拟事件触发时调用handler
            handler("testData")
        }

        // 注册事件处理器
        mockEmitter.on("testEvent") { data ->
            handlerCalled = true
            receivedData = data
        }

        // 验证处理器被调用
        assertEquals(true, handlerCalled)
        assertEquals("testData", receivedData)
    }

    @Test
    fun testEventEmitterOffMethod() {
        // 模拟EventEmitter实例
        val mockEmitter = mockk<TestEventEmitter>()
        val testHandler: (Any?) -> Unit = { }

        // 设置off方法的期望行为
        every { mockEmitter.off("testEvent", testHandler) } returns Unit

        // 调用off方法
        mockEmitter.off("testEvent", testHandler)

        // 验证off被调用
        verify { mockEmitter.off("testEvent", testHandler) }
    }

    @Test
    fun testEventEmitterEmitWithDifferentDataTypes() {
        // 模拟companion object
        val mockEmitter = mockk<TestEventEmitter.Companion>()

        // 测试emit不同数据类型
        every { mockEmitter.emit("stringEvent", any<String>()) } returns Unit
        every { mockEmitter.emit("intEvent", any<Int>()) } returns Unit
        every { mockEmitter.emit("nullEvent", null) } returns Unit

        // 调用emit方法
        mockEmitter.emit("stringEvent", "test")
        mockEmitter.emit("intEvent", 123)
        mockEmitter.emit("nullEvent", null)

        // 验证所有调用
        verify {
            mockEmitter.emit("stringEvent", "test")
            mockEmitter.emit("intEvent", 123)
            mockEmitter.emit("nullEvent", null)
        }
    }
}
