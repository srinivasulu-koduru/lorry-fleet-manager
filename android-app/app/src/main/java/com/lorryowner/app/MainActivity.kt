package com.lorryowner.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorryowner.app.security.SessionManager
import com.lorryowner.app.ui.components.ShieldVectorIcon
import com.lorryowner.app.ui.driver.DriverPortalHostScreen
import com.lorryowner.app.ui.login.LoginScreen
import com.lorryowner.app.ui.login.LoginViewModel
import com.lorryowner.app.ui.login.UserRole
import com.lorryowner.app.ui.owner.OwnerDashboardScreen
import com.lorryowner.app.ui.theme.BhpCardWhite
import com.lorryowner.app.ui.theme.BhpDarkForestGreen
import com.lorryowner.app.ui.theme.BhpDeepTeal
import com.lorryowner.app.ui.theme.BhpInputBg
import com.lorryowner.app.ui.theme.BhpInputBorder
import com.lorryowner.app.ui.theme.BhpTextBlack
import com.lorryowner.app.ui.theme.BhpTextMuted
import com.lorryowner.app.ui.theme.LorryOwnerAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LorryOwnerAppTheme {
                MainAppContent()
            }
        }
    }
}

@Composable
fun MainAppContent() {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val loginViewModel = remember { LoginViewModel(sessionManager = sessionManager) }
    val uiState = loginViewModel.uiState

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isAuthenticated) {
                when (uiState.userRole) {
                    UserRole.OWNER -> OwnerDashboardScreen(
                        sessionManager = sessionManager,
                        onLogout = { message -> loginViewModel.logout(message) },
                        modifier = Modifier.fillMaxSize()
                    )
                    UserRole.DRIVER -> DriverPortalHostScreen(
                        sessionManager = sessionManager,
                        onLogout = { loginViewModel.logout() },
                        modifier = Modifier.fillMaxSize()
                    )
                    else -> LoginScreen(
                        viewModel = loginViewModel,
                        onLoginSuccess = { }
                    )
                }
            } else {
                LoginScreen(
                    viewModel = loginViewModel,
                    onLoginSuccess = { role ->
                        // Role-based navigation managed automatically via uiState
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainActivityPreview() {
    LorryOwnerAppTheme {
        LoginScreen(
            viewModel = LoginViewModel(),
            onLoginSuccess = {}
        )
    }
}
