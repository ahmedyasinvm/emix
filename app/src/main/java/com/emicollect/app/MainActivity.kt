package com.emicollect.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.emicollect.app.ui.theme.EMICollectAppTheme
import com.emicollect.app.ui.theme.EmeraldPrimary
import com.emicollect.app.ui.theme.GoldAccent
import com.emicollect.app.ui.theme.TextWhite
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

        // Show a loading/splash screen immediately while we read the preference
        showLockScreen()



        lifecycleScope.launch {
            val isBiometricEnabled = userPreferencesRepository.isBiometricEnabled.first()

            if (!isBiometricEnabled) {
                // Biometric NOT enabled — go straight to app
                showAppContent()
            } else {
                // Biometric IS enabled — check hardware, then prompt
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
            // Hardware not available or no fingerprint enrolled — let them in
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
                    // User tapped "Cancel" or too many attempts → close app
                    Toast.makeText(this@MainActivity, "Authentication cancelled", Toast.LENGTH_SHORT).show()
                    finish()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    showAppContent()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Single bad attempt — prompt stays open, just toast
                    Toast.makeText(this@MainActivity, "Fingerprint not recognised", Toast.LENGTH_SHORT).show()
                }
            })

        biometricPrompt.authenticate(promptInfo)
    }

    /** Temporary lock/splash screen — shown until biometric check completes */
    private fun showLockScreen() {
        setContent {
            // Default to dark for splash
            EMICollectAppTheme(useDarkTheme = true) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "EMIX",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldAccent
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "EMI Collection Manager",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextWhite.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        CircularProgressIndicator(color = EmeraldPrimary, modifier = Modifier.size(32.dp))
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
