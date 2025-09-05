package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.myapplication.ui.LoginScreen
import com.example.myapplication.ui.ProfileScreen
import com.example.myapplication.ui.RegisterScreen
import com.example.myapplication.ui.SplashScreen
import com.example.myapplication.ui.MainTabsScreen
import com.example.myapplication.SessionManager
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0.dp)
                ) { _ ->
                    val notifPermissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { }

                    LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT >= 33) {
                            val granted = ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!granted) {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    }
                    val navController = rememberNavController()
                    val session = remember { SessionManager(this@MainActivity) }
                    NavHost(
                        navController = navController,
                        startDestination = "splash"
                    ) {
                        composable("splash") {
                            SplashScreen(context = this@MainActivity) {
                                if (session.isLoggedIn()) {
                                    navController.navigate("main") { popUpTo("splash") { inclusive = true } }
                                } else {
                                    navController.navigate("login") { popUpTo("splash") { inclusive = true } }
                                }
                            }
                        }
                        composable("login") {
                            LoginScreen(
                                context = this@MainActivity,
                                onLoginSuccess = {
                                    // 记录签发时间，实现 30 天免登录
                                    session.tokenIssuedAtMs = System.currentTimeMillis()
                                    navController.navigate("main") { popUpTo("login") { inclusive = true } }
                                },
                                onNavigateRegister = {
                                    navController.navigate("register")
                                }
                            )
                        }
                        composable("main") {
                            MainTabsScreen(
                                context = this@MainActivity,
                                onLogout = {
                                    session.clear()
                                    navController.navigate("login") { popUpTo("main") { inclusive = true } }
                                }
                            )
                        }
                        composable("profile") {
                            ProfileScreen(context = this@MainActivity) {
                                navController.navigate("login") { popUpTo("profile") { inclusive = true } }
                            }
                        }
                        composable("register") {
                            RegisterScreen(
                                context = this@MainActivity,
                                onBackToLogin = { navController.popBackStack() },
                                onRegistered = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}