package com.example.wolwake.data

import kotlinx.serialization.Serializable

/** 可唤醒设备：MAC 归一化存储，host 为定向 IP 或子网广播地址。 */
@Serializable
data class Device(
    val id: String,
    val name: String,
    val mac: String,
    val host: String,
    val port: Int = 9,
)
