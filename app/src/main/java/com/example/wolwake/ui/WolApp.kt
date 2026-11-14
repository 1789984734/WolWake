package com.example.wolwake.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wolwake.R
import com.example.wolwake.data.Device

/** 应用根组合：装配列表、编辑弹层、删除确认与 Snackbar 反馈。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WolApp(viewModel: WolViewModel = viewModel(factory = WolViewModel.Factory)) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var sheetTarget by remember { mutableStateOf<Device?>(null) }
    var sheetVisible by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Device?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            snackbarHostState.showSnackbar(event.message, withDismissAction = true)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { sheetTarget = null; sheetVisible = true }) {
                Icon(Icons.Filled.Add, contentDescription = "添加设备")
            }
        },
    ) { padding ->
        DeviceListScreen(
            devices = state.devices,
            wakingId = state.wakingId,
            onWake = viewModel::wake,
            onEdit = { sheetTarget = it; sheetVisible = true },
            onDelete = { deleteTarget = it },
            modifier = Modifier.padding(padding),
        )
    }

    if (sheetVisible) {
        DeviceEditSheet(
            initial = sheetTarget,
            onDismiss = { sheetVisible = false },
            onSave = { viewModel.upsert(it); sheetVisible = false },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除设备") },
            text = { Text("确定要删除「${target.name}」吗？") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(target.id); deleteTarget = null }) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            },
        )
    }
}
