package com.example.wolwake.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.wolwake.data.Device
import com.example.wolwake.domain.WolSender
import java.util.UUID

/** 添加/编辑设备表单：MAC 格式实时校验，保存前归一化。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceEditSheet(
    initial: Device?,
    onDismiss: () -> Unit,
    onSave: (Device) -> Unit,
) {
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var mac by remember(initial) { mutableStateOf(initial?.mac.orEmpty()) }
    var host by remember(initial) { mutableStateOf(initial?.host ?: "255.255.255.255") }
    var port by remember(initial) { mutableStateOf(initial?.port?.toString() ?: "9") }

    val macBytes = WolSender.parseMac(mac)
    val portValue = port.toIntOrNull()
    val portOk = portValue != null && portValue in 1..65535
    val valid = name.isNotBlank() && macBytes != null && host.isNotBlank() && portOk

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (initial == null) "添加设备" else "编辑设备",
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("名称") },
                isError = name.isNotEmpty() && name.isBlank(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = mac,
                onValueChange = { mac = it },
                label = { Text("MAC 地址") },
                placeholder = { Text("AA:BB:CC:DD:EE:FF") },
                isError = mac.isNotEmpty() && macBytes == null,
                supportingText = if (mac.isNotEmpty() && macBytes == null) {
                    { Text("格式不正确，示例：AA:BB:CC:DD:EE:FF") }
                } else {
                    null
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("目标地址") },
                supportingText = { Text("设备 IP 或子网广播地址，如 192.168.1.255") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                label = { Text("端口") },
                isError = port.isNotEmpty() && !portOk,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) { Text("取消") }
                Button(
                    onClick = {
                        onSave(
                            Device(
                                id = initial?.id ?: UUID.randomUUID().toString(),
                                name = name.trim(),
                                mac = WolSender.formatMac(requireNotNull(macBytes)),
                                host = host.trim(),
                                port = requireNotNull(portValue),
                            ),
                        )
                    },
                    enabled = valid,
                ) {
                    Text("保存")
                }
            }
        }
    }
}
