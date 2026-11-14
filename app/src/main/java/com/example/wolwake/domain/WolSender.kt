package com.example.wolwake.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * WOL 核心：魔术包构造（纯函数）与 UDP 发送（IO 副作用）分离，便于单测与复用。
 * 魔术包 = 6 字节 0xFF 前缀 + 目标 MAC 重复 16 次，共 102 字节。
 */
object WolSender {

    private const val MAC_BYTES = 6
    private const val PREFIX_BYTES = 6
    private const val MAC_REPEATS = 16
    private const val HEX_DIGITS = "0123456789abcdef"

    /** 解析 MAC 地址，容忍 : - . 空格分隔与无分隔符写法；非法输入返回 null。 */
    fun parseMac(raw: String): ByteArray? {
        val cleaned = raw.trim().lowercase()
            .replace(":", "").replace("-", "").replace(".", "").replace(" ", "")
        if (cleaned.length != MAC_BYTES * 2 || cleaned.any { it !in HEX_DIGITS }) return null
        return ByteArray(MAC_BYTES) { i -> cleaned.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
    }

    /** 归一化为 AA:BB:CC:DD:EE:FF 大写展示格式。 */
    fun formatMac(mac: ByteArray): String =
        mac.joinToString(":") { (it.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase() }

    /** 构造魔术包；入参必须为 6 字节 MAC。 */
    fun buildMagicPacket(mac: ByteArray): ByteArray {
        require(mac.size == MAC_BYTES) { "MAC 地址必须为 6 字节" }
        val packet = ByteArray(PREFIX_BYTES + MAC_BYTES * MAC_REPEATS) { -1 }
        for (i in 0 until MAC_BYTES * MAC_REPEATS) {
            packet[PREFIX_BYTES + i] = mac[i % MAC_BYTES]
        }
        return packet
    }

    /**
     * 向 host:port 连续发送 [repeats] 个魔术包，提升对丢包的容忍度。
     * host 可为定向 IP（可跨网段）或子网广播地址（如 192.168.1.255）。
     */
    suspend fun wake(mac: ByteArray, host: String, port: Int = 9, repeats: Int = 3) {
        require(repeats >= 1) { "发送次数必须为正" }
        withContext(Dispatchers.IO) {
            val packet = buildMagicPacket(mac)
            val address = InetAddress.getByName(host)
            DatagramSocket().use { socket ->
                socket.broadcast = true
                val datagram = DatagramPacket(packet, packet.size, address, port)
                repeat(repeats) { index ->
                    socket.send(datagram)
                    if (index != repeats - 1) delay(50)
                }
            }
        }
    }
}
