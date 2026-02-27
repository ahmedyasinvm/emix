package com.emicollect.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.rounded.DarkMode
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.emicollect.app.ui.components.GlassCard
import com.emicollect.app.ui.theme.EmeraldPrimary
import com.emicollect.app.ui.theme.GoldAccent
import com.emicollect.app.ui.theme.TextWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val businessName by viewModel.businessName.collectAsState()
    val contactNumber by viewModel.contactNumber.collectAsState()
    val defaultAmount by viewModel.defaultAmount.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val isWhatsAppEnabled by viewModel.isWhatsAppEnabled.collectAsState()
    val schedule by viewModel.collectionSchedule.collectAsState()

    var nameInput by remember(businessName) { mutableStateOf(businessName) }
    var numberInput by remember(contactNumber) { mutableStateOf(contactNumber) }
    var amountInput by remember(defaultAmount) { mutableStateOf(defaultAmount) }

    // File picker for restore
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.restoreDatabase(context, it) }
    }

    // Snackbar host state
    val snackbarHostState = remember { SnackbarHostState() }
    // Restart Dialog
    var showRestartDialog by remember { mutableStateOf(false) }
    
    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = {}, // Force user to click OK
            title = { Text("Restore Successful") },
            text = { Text("The app will now close to apply changes.") },
            confirmButton = {
                Button(
                    onClick = {
                        System.exit(0)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Restart Now", color = TextWhite)
                }
            },
            containerColor = Color(0xFF1E293B),
            titleContentColor = TextWhite,
            textContentColor = TextWhite.copy(alpha = 0.8f)
        )
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            if (message == "RESTART_NEEDED") {
                showRestartDialog = true
            } else {
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    // Fade-in animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { scaffoldPadding ->
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = androidx.compose.animation.core.tween(600)
        ) + slideInVertically(
            animationSpec = androidx.compose.animation.core.tween(600),
            initialOffsetY = { it / 8 }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            // ═══════════════════════════════════════════
            // SECTION 1: PROFILE
            // ═══════════════════════════════════════════
            SectionHeader("👤 Profile")

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = {
                            nameInput = it
                            viewModel.updateBusinessName(it)
                        },
                        label = { Text("Business Name") },
                        placeholder = { Text("E.g., Yasin Collections", color = TextWhite.copy(alpha = 0.3f)) },
                        leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, tint = EmeraldPrimary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = settingsFieldColors(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = numberInput,
                        onValueChange = {
                            numberInput = it
                            viewModel.updateContactNumber(it)
                        },
                        label = { Text("Contact Number") },
                        placeholder = { Text("+91 XXXXX XXXXX", color = TextWhite.copy(alpha = 0.3f)) },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = EmeraldPrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        colors = settingsFieldColors(),
                        singleLine = true
                    )
                }
            }

            // ═══════════════════════════════════════════
            // SECTION 2: COLLECTION PREFERENCES
            // ═══════════════════════════════════════════
            SectionHeader("💰 Collection Preferences")

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = {
                            amountInput = it
                            viewModel.updateDefaultAmount(it)
                        },
                        label = { Text("Default Weekly Amount (₹)") },
                        leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = GoldAccent) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = settingsFieldColors(),
                        singleLine = true
                    )
                    Text(
                        text = "Pre-filled when collecting payments",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextWhite.copy(alpha = 0.5f)
                    )

                    val days = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
                    val weeks = listOf("1st Week", "2nd Week", "3rd Week", "4th Week")

                    Text("Weekly Collection Day", style = MaterialTheme.typography.labelMedium, color = TextWhite.copy(alpha = 0.7f))
                    SettingsDropdown(
                        options = days,
                        selectedOption = days[(schedule.weeklyDay - 1).coerceIn(0, 6)],
                        onOptionSelected = { viewModel.updateWeeklyDay(days.indexOf(it) + 1) }
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Monthly Schedule", style = MaterialTheme.typography.labelMedium, color = TextWhite.copy(alpha = 0.7f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            SettingsDropdown(
                                options = weeks,
                                selectedOption = weeks[(schedule.monthlyWeekNum - 1).coerceIn(0, 3)],
                                onOptionSelected = { viewModel.updateMonthlySchedule(weeks.indexOf(it) + 1, schedule.monthlyDay) }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            SettingsDropdown(
                                options = days,
                                selectedOption = days[(schedule.monthlyDay - 1).coerceIn(0, 6)],
                                onOptionSelected = { viewModel.updateMonthlySchedule(schedule.monthlyWeekNum, days.indexOf(it) + 1) }
                            )
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════
            // SECTION 3: APPEARANCE
            // ═══════════════════════════════════════════
            SectionHeader("🎨 Appearance")
            
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsToggleRow(
                        icon = Icons.Rounded.DarkMode,
                        title = "Dark Mode",
                        subtitle = "Toggle dark/light theme",
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.toggleDarkMode(it) }
                    )
                }
            }

            // ═══════════════════════════════════════════
            // SECTION 4: SECURITY & NOTIFICATIONS
            // ═══════════════════════════════════════════
            SectionHeader("🔒 Security & Notifications")

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsToggleRow(
                        icon = Icons.Default.Fingerprint,
                        title = "App Lock (Biometric)",
                        subtitle = "Require fingerprint to open app",
                        checked = isBiometricEnabled,
                        onCheckedChange = { viewModel.setBiometricEnabled(it) }
                    )
                    Divider(color = TextWhite.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))
                    SettingsToggleRow(
                        icon = Icons.Default.Message,
                        title = "WhatsApp Receipts",
                        subtitle = "Auto-share receipts via WhatsApp",
                        checked = isWhatsAppEnabled,
                        onCheckedChange = { viewModel.setWhatsAppEnabled(it) }
                    )
                }
            }

            // ═══════════════════════════════════════════
            // SECTION 4: CLOUD BACKUP (GOOGLE DRIVE)
            // ═══════════════════════════════════════════
            SectionHeader("☁️ Cloud Backup")

            val currentUserEmail by viewModel.currentUserEmail.collectAsState()
            val isAutoBackupEnabled by viewModel.isAutoBackupEnabled.collectAsState()

            // Sign In Launcher
            val signInLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                viewModel.handleSignInResult(result.data)
            }

            // Permission Launcher for Notifications (Auto Backup)
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    viewModel.scheduleDailyBackup(context, true)
                } else {
                    // Permission denied, can't enable auto backup notification, but maybe still schedule?
                    // For now, just disable toggle
                    viewModel.scheduleDailyBackup(context, false)
                }
            }

            LaunchedEffect(Unit) {
                viewModel.checkSignedInUser(context)
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    
                    if (currentUserEmail == null) {
                        // Not Signed In
                        Text(
                            text = "Sign in to Google Drive to backup your data safely to the cloud.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextWhite.copy(alpha = 0.7f)
                        )
                        Button(
                            onClick = {
                                signInLauncher.launch(viewModel.getSignInIntent(context))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign in with Google", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Signed In
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Connected as", style = MaterialTheme.typography.bodySmall, color = TextWhite.copy(alpha = 0.7f))
                                Text(currentUserEmail!!, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.signOut(context) }) {
                                Text("Sign Out", color = Color(0xFFEF4444))
                            }
                        }

                        Divider(color = TextWhite.copy(alpha = 0.1f))

                        // Manual Actions
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Backup Now
                            Button(
                                onClick = { viewModel.backupToDrive(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Backup Now")
                            }

                            // Restore
                            OutlinedButton(
                                onClick = { viewModel.restoreFromDrive(context) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                                border = androidx.compose.foundation.BorderStroke(1.dp, TextWhite.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Restore")
                            }
                        }

                        // Auto-Backup Toggle
                        SettingsToggleRow(
                            icon = Icons.Default.Schedule,
                            title = "Daily Auto-Backup",
                            subtitle = "Backs up at night (requires Internet)",
                            checked = isAutoBackupEnabled,
                            onCheckedChange = { params ->
                                if (params) {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.scheduleDailyBackup(context, true)
                                    }
                                } else {
                                    viewModel.scheduleDailyBackup(context, false)
                                }
                            }
                        )


                    }
                }
            }

            // ═══════════════════════════════════════════
            // SECTION 5: LOCAL DATA MANAGEMENT
            // ═══════════════════════════════════════════
            SectionHeader("📁 Local Data")

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DataActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CloudUpload,
                    label = "Backup",
                    color = Color(0xFF059669)
                ) { viewModel.backupDatabase(context) }
                DataActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CloudDownload,
                    label = "Restore",
                    color = Color(0xFF2563EB)
                ) { restoreLauncher.launch(arrayOf("application/json")) }
                DataActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.TableChart,
                    label = "Export Excel",
                    color = Color(0xFFD97706)
                ) { viewModel.exportToExcel(context) }
            }

            // ═══════════════════════════════════════════
            // DEVELOPER CARD
            // ═══════════════════════════════════════════
            SectionHeader("👨‍💻 About the Developer")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, EmeraldPrimary, RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Developed by Ahmed Yasin",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Full Stack Android Architect",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GoldAccent
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Visit Official Site button
                    OutlinedButton(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ahmedyasinvm.site")))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(EmeraldPrimary, GoldAccent))
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Visit Official Site", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Social row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SocialIconButton(Icons.Default.Email, "Email") {
                            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:ahmedyasin.git@gmail.com")))
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                        SocialIconButton(Icons.Default.Code, "GitHub") {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ahmedyasinvm")))
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                        SocialIconButton(Icons.Default.Chat, "WhatsApp") {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/919961760986")))
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════
            // APP LINKS
            // ═══════════════════════════════════════════
            SectionHeader("📋 App Links")

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AppLinkItem(Icons.Default.PrivacyTip, "Privacy Policy") {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ahmedyasinvm.site/privacy.html")))
                }
                AppLinkItem(Icons.Default.Star, "Rate Us on Play Store") {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.emicollect.app")))
                    } catch (e: Exception) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.emicollect.app")))
                    }
                }
                AppLinkItem(Icons.Default.BugReport, "Report a Bug") {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:ahmedyasin.git@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, "Bug Report: Emix v2.0.4")
                    }
                    context.startActivity(intent)
                }
            }

            // ═══════════════════════════════════════════
            // FOOTER
            // ═══════════════════════════════════════════
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Made with ❤️ in India | v3.0.5",
                style = MaterialTheme.typography.bodySmall,
                color = TextWhite.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
    } // Scaffold
}

// ═══ Component: Section Header ═══
@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = EmeraldPrimary
    )
}

// ═══ Component: Toggle Row ═══
@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = TextWhite, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextWhite.copy(alpha = 0.5f))
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = EmeraldPrimary,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF334155)
            )
        )
    }
}

// ═══ Component: Data Action Button ═══
@Composable
private fun DataActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

// ═══ Component: Social Icon Button ═══
@Composable
private fun SocialIconButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
    ) {
        Icon(icon, contentDescription = label, tint = TextWhite, modifier = Modifier.size(22.dp))
    }
}

// ═══ Component: App Link Item ═══
@Composable
private fun AppLinkItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextWhite.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextWhite.copy(alpha = 0.8f))
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextWhite.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
    }
}

// ═══ Component: Settings Field Colors ═══
@Composable
private fun settingsFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextWhite,
    unfocusedTextColor = TextWhite,
    focusedBorderColor = EmeraldPrimary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = EmeraldPrimary,
    unfocusedLabelColor = TextWhite.copy(alpha = 0.6f),
    cursorColor = GoldAccent
)

// ═══ Dropdown (preserved from original) ═══
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDropdown(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            colors = settingsFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = TextWhite) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
