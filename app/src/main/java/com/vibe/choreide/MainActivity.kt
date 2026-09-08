package com.vibe.choreide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.topjohnwu.superuser.Shell
import com.vibe.choreide.ui.components.BottomNavBar
import com.vibe.choreide.ui.screens.BuildScreen
import com.vibe.choreide.ui.screens.EditorScreen
import com.vibe.choreide.ui.screens.MockupScreen
import com.vibe.choreide.ui.screens.ProjectScreen
import com.vibe.choreide.ui.screens.TerminalScreen
import com.vibe.choreide.ui.theme.ChoreIDETheme
import org.lsposed.hiddenapibypass.HiddenApiBypass

class MainActivity : ComponentActivity() {

    companion object {
        init {
            Shell.enableVerboseLogging = BuildConfig.DEBUG
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_MOUNT_MASTER)
                    .setTimeout(10)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            HiddenApiBypass.addHiddenApiExemptions("")
        } catch (t: Throwable) {
            // Non-fatal: hidden API bypass is best-effort
        }

        setContent {
            ChoreIDETheme {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route ?: "editor"

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        BottomNavBar(currentRoute = currentRoute) { route ->
                            navController.navigate(route) {
                                popUpTo("editor") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(modifier = Modifier.padding(innerPadding)) {
                        NavHost(
                            navController = navController,
                            startDestination = "editor"
                        ) {
                            composable("editor") { EditorScreen() }
                            composable("project") { ProjectScreen() }
                            composable("terminal") { TerminalScreen() }
                            composable("preview") { MockupScreen() }
                            composable("build") { BuildScreen() }
                        }
                    }
                }
            }
        }
    }
}
