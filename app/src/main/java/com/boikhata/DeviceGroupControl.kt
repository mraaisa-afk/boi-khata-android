package com.boikhata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.core.database.dao.DeviceDao
import com.boikhata.core.database.entity.DeviceEntity
import com.boikhata.core.domain.p8.DeviceGroupPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DeviceGroupViewModel @Inject constructor(private val deviceDao: DeviceDao) : ViewModel() {
    var devices by mutableStateOf<List<DeviceEntity>>(emptyList())
        private set

    fun refresh(tenantId: String) { viewModelScope.launch { devices = deviceDao.getByTenant(tenantId) } }
    fun add(tenantId: String, label: String, isOwner: Boolean) {
        if (!isOwner || !DeviceGroupPolicy.canAddDevice(devices.count { it.isActive })) return
        viewModelScope.launch {
            deviceDao.insert(DeviceEntity(UUID.randomUUID().toString(), tenantId, label, false, true, System.currentTimeMillis()))
            refresh(tenantId)
        }
    }
    fun remove(deviceId: String, isOwner: Boolean) {
        if (!isOwner) return
        viewModelScope.launch { deviceDao.deactivateSecondary(deviceId); devices = devices.filterNot { it.id == deviceId } }
    }
}

@Composable
fun DeviceGroupCard(tenantId: String, isOwner: Boolean, viewModel: DeviceGroupViewModel = hiltViewModel()) {
    var label by remember { mutableStateOf("") }
    LaunchedEffect(tenantId) { viewModel.refresh(tenantId) }
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.device_group_title))
        Text(stringResource(R.string.device_group_usage, viewModel.devices.count { it.isActive }, DeviceGroupPolicy.LITE_DEVICE_LIMIT))
        OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text(stringResource(R.string.device_label)) }, enabled = isOwner)
        Button(onClick = { viewModel.add(tenantId, label, isOwner); label = "" }, enabled = isOwner && label.isNotBlank() && DeviceGroupPolicy.canAddDevice(viewModel.devices.count { it.isActive })) {
            Text(stringResource(R.string.add_device))
        }
        viewModel.devices.filterNot { it.isPrimary && it.isActive }.forEach { device ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(device.label)
                Button(onClick = { viewModel.remove(device.id, isOwner) }, enabled = isOwner && device.isActive) { Text(stringResource(R.string.remove_device)) }
            }
        }
    }
}
