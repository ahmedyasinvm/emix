package com.emicollect.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*

@Composable
fun SettingsDialog(
    isWhatsAppEnabled: Boolean,
    onToggleWhatsApp: (Boolean) -> Unit,
    onBackupData: () -> Unit,
    onRestoreFromCloud: () -> Unit,
    onDismiss: () -> Unit
) {
    var showRestoreWarning by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Auto-open WhatsApp",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = isWhatsAppEnabled,
                        onCheckedChange = onToggleWhatsApp
                    )
                }
                Text(
                    text = "Automatically open WhatsApp with a pre-filled receipt after recording a payment.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                
                Button(
                    onClick = onBackupData,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Backup Data to Downloads")
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { showRestoreWarning = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Restore from Cloud")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )

    if (showRestoreWarning) {
        AlertDialog(
            onDismissRequest = { showRestoreWarning = false },
            title = { Text("Restore from Cloud") },
            text = { Text("Warning: This will overwrite all local data. This action cannot be undone. Do you want to proceed?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreWarning = false
                        onRestoreFromCloud()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
