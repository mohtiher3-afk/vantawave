package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSettingsSheet(
    onDismiss: () -> Unit,
    onExport: (resolution: String, frameRate: String) -> Unit
) {
    var selectedRes by remember { mutableStateOf("1080p") }
    var selectedFps by remember { mutableStateOf("30fps") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = VantaSurface,
        contentColor = VantaTextPrimary
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Export Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            Text("Resolution", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("1080p", "4K").forEach { res ->
                    FilterChip(
                        selected = selectedRes == res,
                        onClick = { selectedRes = res },
                        label = { Text(res) }
                    )
                }
            }

            Text("Frame Rate", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("30fps", "60fps").forEach { fps ->
                    FilterChip(
                        selected = selectedFps == fps,
                        onClick = { selectedFps = fps },
                        label = { Text(fps) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onExport(selectedRes, selectedFps) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = VantaPrimary)
            ) {
                Text("Start Export")
            }
        }
    }
}
