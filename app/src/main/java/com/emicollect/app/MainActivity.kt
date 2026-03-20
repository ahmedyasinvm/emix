package com.emicollect.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.emicollect.app.data.local.UserPreferencesRepository
import com.emicollect.app.ui.addcustomer.AddCustomerScreen
import com.emicollect.app.ui.details.CustomerDetailScreen
import com.emicollect.app.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showLockScreen()

        lifecycleScope.launch {
            val isBiometricEnabled = userPreferencesRepository.isBiometricEnabled.first()

            if (!isBiometricEnabled) {
                showAppContent()
            } else {
                runOnUiThread { startBiometricAuth() }
            }
        }
    }

    private fun startBiometricAuth() {
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        )

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            showAppContent()
            return
        }

        val executor = ContextCompat.getMainExecutor(this)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Emix")
            .setSubtitle("Verify your identity to continue")
            .setNegativeButtonText("Cancel")
            .build()

        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@MainActivity, "Authentication cancelled", Toast.LENGTH_SHORT).show()
                    finish()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    showAppContent()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@MainActivity, "Fingerprint not recognised", Toast.LENGTH_SHORT).show()
                }
            })

        biometricPrompt.authenticate(promptInfo)
    }

    /** Animated premium lock/splash screen */
    private fun showLockScreen() {
        setContent {
            EMICollectAppTheme(useDarkTheme = true) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    GunmetalLight,
                                    GunmetalDark
                                ),
                                radius = 800f
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Pulsing ring
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val ringScale by infiniteTransition.animateFloat(
                        initialValue = 0.85f,
                        targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = EaseInOutCubic),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "ring_scale"
                    )
                    val ringAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 0.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = EaseInOutCubic),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "ring_alpha"
                    )

                    // Outer pulsing ring
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .scale(ringScale)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        EmeraldLight.copy(alpha = ringAlpha),
                                        EmeraldPrimary.copy(alpha = 0f)
                                    )
                                ),
                                CircleShape
                            )
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "EMIX",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 4.sp,
                                brush = Brush.linearGradient(
                                    colors = listOf(GoldAccent, GoldLight)
                                )
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "EMI Collection Manager",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextWhite.copy(alpha = 0.5f),
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
        }
    }

    /** Main app content — only shown after authentication passes */
    private fun showAppContent() {
        setContent {
            val isDarkMode by userPreferencesRepository.isDarkMode.collectAsState(initial = true)

            EMICollectAppTheme(useDarkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "main_screen") {
                        composable("main_screen") {
                            com.emicollect.app.ui.MainScreen(
                                onAddCustomerClick = { navController.navigate("add_customer") },
                                onCustomerClick = { customerId -> navController.navigate("customer_detail/$customerId") }
                            )
                        }
                        composable("add_customer") {
                            AddCustomerScreen(
                                onBackClick = { navController.popBackStack() },
                                onCustomerSaved = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = "customer_detail/{customerId}",
                            arguments = listOf(navArgument("customerId") { type = NavType.LongType })
                        ) {
                            CustomerDetailScreen(
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
