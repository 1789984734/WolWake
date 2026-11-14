package com.example.wolwake.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WolSenderTest {

    @Test
    fun `parseMac 容忍冒号 连字符 空格与无分隔符`() {
        val expected = byteArrayOf(
            0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte(),
        )
        assertArrayEquals(expected, WolSender.parseMac("AA:BB:CC:DD:EE:FF"))
        assertArrayEquals(expected, WolSender.parseMac("AA-BB-CC-DD-EE-FF"))
        assertArrayEquals(expected, WolSender.parseMac("aabbccddeeff"))
        assertArrayEquals(expected, WolSender.parseMac(" AA BB CC DD EE FF "))
        assertArrayEquals(expected, WolSender.parseMac("AA.BB.CC.DD.EE.FF"))
    }

    @Test
    fun `parseMac 拒绝非法输入`() {
        assertNull(WolSender.parseMac(""))
        assertNull(WolSender.parseMac("AA:BB:CC:DD:EE"))
        assertNull(WolSender.parseMac("AA:BB:CC:DD:EE:FF:00"))
        assertNull(WolSender.parseMac("AA:BB:CC:DD:EE:G1"))
        assertNull(WolSender.parseMac("这是一段中文"))
    }

    @Test
    fun `buildMagicPacket 生成标准 102 字节魔术包`() {
        val mac = WolSender.parseMac("AA:BB:CC:DD:EE:FF")!!
        val packet = WolSender.buildMagicPacket(mac)
        assertEquals(102, packet.size)
        assertTrue(packet.slice(0..5).all { it == 0xFF.toByte() })
        repeat(16) { i ->
            assertArrayEquals(mac, packet.slice(6 + i * 6 until 6 + (i + 1) * 6).toByteArray())
        }
    }

    @Test
    fun `formatMac 与 parseMac 互逆`() {
        val mac = WolSender.parseMac("aabbccddee01")!!
        assertEquals("AA:BB:CC:DD:EE:01", WolSender.formatMac(mac))
    }
}
