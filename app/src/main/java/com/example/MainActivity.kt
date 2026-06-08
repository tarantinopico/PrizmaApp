package com.example

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ui.navigation.*
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodels.BrowserViewModel
import com.example.ui.viewmodels.ProfileViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val appContainer = (application as PrizmaApp).container
            
            if (intent?.action == DownloadManager.ACTION_NOTIFICATION_CLICKED) {
                val ids = intent.getLongArrayExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS)
                if (ids != null && ids.isNotEmpty()) {
                    val downloadId = ids[0]
                    lifecycleScope.launch {
                        val repo = appContainer.browserRepository
                        val dl = repo.getDownloadByManagerId(downloadId)
                        if (dl != null && context != null) {
                            com.example.utils.FileOpenHelper.openDownloadedFile(context, dl)
                        }
                    }
                }
            } else if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id != -1L) {
                    lifecycleScope.launch {
                        // find matching download entity and update its status
                        // Since we don't have a direct query by managerId easily available, 
                        // we can fetch all downloads or let the repo handle it.
                        // For simplicity, we just update all active to done if manager confirms.
                        val q = DownloadManager.Query().setFilterById(id)
                        val dlManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        val cursor = dlManager.query(q)
                        if (cursor.moveToFirst()) {
                            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            if (statusIndex != -1) {
                                val status = cursor.getInt(statusIndex)
                                if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                                    val stringStatus = if (status == DownloadManager.STATUS_SUCCESSFUL) "dokončeno" else "selhalo"
                                    
                                    val repo = appContainer.browserRepository
                                    val dl = repo.getDownloadByManagerId(id)
                                    if (dl != null) {
                                        repo.updateDownload(dl.copy(status = stringStatus))
                                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                            // Vibration on download complete
                                            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                                            if (vibrator.hasVibrator()) {
                                                try {
                                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                                        vibrator.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 30, 80, 50), -1))
                                                    } else {
                                                        vibrator.vibrate(longArrayOf(0, 30, 80, 50), -1)
                                                    }
                                                } catch (e: Exception) {}
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        cursor.close()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Register download receiver
        val filter = IntentFilter().apply {
            addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED)
        }
        androidx.core.content.ContextCompat.registerReceiver(
            this,
            downloadReceiver,
            filter,
            androidx.core.content.ContextCompat.RECEIVER_EXPORTED
        )
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    
                    val appContainer = (application as PrizmaApp).container
                    
                    val profileViewModel: ProfileViewModel = viewModel(
                        factory = ProfileViewModel.provideFactory(
                            appContainer.browserRepository,
                            appContainer.settingsDataStore
                        )
                    )
                    
                    val browserViewModel: BrowserViewModel = viewModel(
                        factory = BrowserViewModel.provideFactory(appContainer.browserRepository, applicationContext, appContainer.settingsDataStore)
                    )

                    NavHost(
                        navController = navController,
                        startDestination = ProfileSelection
                    ) {
                        composable<ProfileSelection> {
                            ProfileSelectionScreen(
                                viewModel = profileViewModel,
                                onProfileSelected = { profileId ->
                                    navController.navigate(Browser(profileId)) {
                                        popUpTo(ProfileSelection) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                onNavigateToSettings = {
                                    navController.navigate(Settings(0L))
                                }
                            )
                        }
                        composable<Browser> { navBackStackEntry ->
                            val args = navBackStackEntry.toRoute<Browser>()
                            BrowserScreen(
                                profileId = args.profileId,
                                viewModel = browserViewModel,
                                onNavigateToTabs = { navController.navigate(TabSwitcher(args.profileId)) },
                                onNavigateToSettings = { navController.navigate(Settings(args.profileId)) },
                                onNavigateToDownloads = { navController.navigate(Downloads(args.profileId)) },
                                onSwitchProfile = { newProfileId ->
                                    profileViewModel.setActiveProfile(newProfileId)
                                    com.example.utils.ProfileUtils.setWebViewProfileOrRestart(applicationContext, newProfileId)
                                    navController.navigate(Browser(newProfileId)) {
                                        popUpTo(Browser(args.profileId)) { inclusive = true }
                                    }
                                },
                                onNavigateToProfileSelection = {
                                    navController.navigate(ProfileSelection) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable<TabSwitcher> { navBackStackEntry ->
                            val args = navBackStackEntry.toRoute<TabSwitcher>()
                            TabSwitcherScreen(
                                profileId = args.profileId,
                                viewModel = browserViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onSwitchProfile = {
                                    navController.navigate(ProfileSelection) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable<Settings> { navBackStackEntry ->
                            val args = navBackStackEntry.toRoute<Settings>()
                            SettingsScreen(
                                profileId = args.profileId,
                                viewModel = browserViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable<Downloads> { navBackStackEntry ->
                            val args = navBackStackEntry.toRoute<Downloads>()
                            DownloadsScreen(
                                profileId = args.profileId,
                                viewModel = browserViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(downloadReceiver)
    }
}
