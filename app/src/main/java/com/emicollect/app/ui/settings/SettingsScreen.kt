package com.emicollect.app.ui.settings

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.emicollect.app.ui.components.GlassCard
import com.emicollect.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Individual flows from SettingsViewModel
    val businessName by viewModel.businessName.collectAsState()
    val contactNumber by viewModel.contactNumber.collectAsState()
    val defaultAmount by viewModel.defaultAmount.collectAsState()
    val collectionSchedule by viewModel.collectionSchedule.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val isWhatsAppEnabled by viewModel.isWhatsAppEnabled.collectAsState()
    val currentUserEmail by viewModel.currentUserEmail.collectAsState()
    val isAutoBackupEnabled by viewModel.isAutoBackupEnabled.collectAsState()

    // Check signed in user on launch
    LaunchedEffect(Unit) {
        viewModel.checkSignedInUser(context)
    }

    // Collect snackbar events
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { msg ->
            if (msg == "RESTART_NEEDED") {
                Toast.makeText(context, "Data restored. Please restart the app.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Google sign-in launcher
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.handleSignInResult(result.data)
        }
    }

    // Local restore launcher
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.restoreDatabase(context, it) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = EmeraldLight,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "Settings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ═══ Profile ═══
            SettingsSection(icon = Icons.Default.Person, title = "Profile") {
                SettingsTextField(
                    label = "Business Name",
                    value = businessName,
                    onValueChange = { viewModel.updateBusinessName(it) },
                    icon = Icons.Default.Business
                )
                SettingsTextField(
                    label = "Contact Number",
                    value = contactNumber,
                    onValueChange = { viewModel.updateContactNumber(it) },
                    icon = Icons.Default.Phone,
                    keyboardType = KeyboardType.Phone
                )
            }

            // ═══ Collection Preferences ═══
            SettingsSection(icon = Icons.Default.Tune, title = "Collection Preferences") {
                SettingsTextField(
                    label = "Default Amount (₹)",
                    value = defaultAmount,
                    onValueChange = { viewModel.updateDefaultAmount(it) },
                    icon = Icons.Default.CurrencyRupee,
                    keyboardType = KeyboardType.Number
                )
            }

            // ═══ Appearance ═══
            SettingsSection(icon = Icons.Default.Palette, title = "Appearance") {
                SettingsToggle(
                    icon = Icons.Default.DarkMode,
                    label = "Dark Mode",
                    checked = isDarkMode,
                    onCheckedChange = { viewModel.toggleDarkMode(!isDarkMode) }
                )
            }

            // ═══ Security ═══
            SettingsSection(icon = Icons.Default.Lock, title = "Security") {
                SettingsToggle(
                    icon = Icons.Default.Fingerprint,
                    label = "App Lock (Biometric)",
                    checked = isBiometricEnabled,
                    onCheckedChange = { viewModel.setBiometricEnabled(!isBiometricEnabled) }
                )
            }

            // ═══ Notifications ═══
            SettingsSection(icon = Icons.Default.Notifications, title = "Notifications") {
                SettingsToggle(
                    icon = Icons.Default.Message,
                    label = "WhatsApp Receipt Sending",
                    checked = isWhatsAppEnabled,
                    onCheckedChange = { viewModel.setWhatsAppEnabled(!isWhatsAppEnabled) }
                )
            }

            // ═══ Cloud ═══
            SettingsSection(icon = Icons.Default.Cloud, title = "Cloud Backup") {
                if (currentUserEmail != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SuccessGreenTint)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Connected to Google Drive", style = MaterialTheme.typography.labelLarge, color = SuccessGreen, fontWeight = FontWeight.Bold)
                            Text(currentUserEmail ?: "", style = MaterialTheme.typography.bodySmall, color = SuccessGreen.copy(alpha = 0.7f))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsToggle(
                        icon = Icons.Default.Sync,
                        label = "Auto Daily Backup",
                        checked = isAutoBackupEnabled,
                        onCheckedChange = { viewModel.scheduleDailyBackup(context, !isAutoBackupEnabled) }
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SettingsActionButton(
                            icon = Icons.Default.CloudUpload,
                            label = "Backup",
                            color = EmeraldPrimary,
                            onClick = { viewModel.backupToDrive(context) },
                            modifier = Modifier.weight(1f)
                        )
                        SettingsActionButton(
                            icon = Icons.Default.CloudDownload,
                            label = "Restore",
                            color = GoldDeep,
                            onClick = { viewModel.restoreFromDrive(context) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = { viewModel.signOut(context) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Sign Out", color = ErrorRed.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    Button(
                        onClick = {
                            val intent = viewModel.getSignInIntent(context)
                            signInLauncher.launch(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Connect Google Drive", color = TextWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ═══ Data ═══
            SettingsSection(icon = Icons.Default.Storage, title = "Local Data") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SettingsActionButton(
                        icon = Icons.Default.Save,
                        label = "Local Backup",
                        color = EmeraldPrimary,
                        onClick = { viewModel.backupDatabase(context) },
                        modifier = Modifier.weight(1f)
                    )
                    SettingsActionButton(
                        icon = Icons.Default.Restore,
                        label = "Restore Local",
                        color = GoldDeep,
                        onClick = { restoreLauncher.launch(arrayOf("application/json")) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                SettingsActionButton(
                    icon = Icons.Default.FileDownload,
                    label = "Export to Excel",
                    color = InfoBlue,
                    onClick = { viewModel.exportToExcel(context) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ═══ Legal & Support ═══
            SettingsSection(icon = Icons.Default.VerifiedUser, title = "Legal & Support") {
                SettingsActionButton(
                    icon = Icons.Default.Policy,
                    label = "Privacy Policy",
                    color = InfoBlue,
                    onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://ahmedyasinvm.site/privacy.html"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ═══ About ═══
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "EMIX",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent
                    )
                    Text(
                        "EMI Collection Manager",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        "v4.6.5",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Text(
                        "By Ahmed Yasin",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldLight.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ─── Reusable Settings Components ──────────────────────────────────

@Composable
fun SettingsSection(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Brush.verticalGradient(listOf(GoldAccent, GoldDeep)))
            )
            Icon(icon, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = GoldAccent
            )
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

@Composable
fun SettingsToggle(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextWhite,
                checkedTrackColor = EmeraldPrimary,
                uncheckedThumbColor = SlateLight,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(18.dp)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedBorderColor = EmeraldPrimary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = EmeraldLight,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            cursorColor = GoldAccent
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun SettingsActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}
