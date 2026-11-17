package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.MatchSides
import com.example.ui.screens.CareerHubScreen
import com.example.ui.screens.LiveMatchScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.screens.QuickMatchSetupScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TournamentScreen
import com.example.ui.screens.TrainingScreen
import com.example.ui.theme.PixelKickTheme
import com.example.viewmodel.MainViewModel

object Routes {
    const val MENU = "menu"
    const val QUICK_MATCH = "quick_match"
    const val CAREER = "career"
    const val TOURNAMENT = "tournament"
    const val TRAINING = "training"
    const val SETTINGS = "settings"
    const val LIVE_MATCH = "live_match"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Immersive fullscreen landscape experience for arcade football
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            PixelKickTheme {
                PixelKickApp(onExit = { finishAffinity() })
            }
        }
    }
}

@Composable
fun PixelKickApp(
    onExit: () -> Unit,
    vm: MainViewModel = viewModel()
) {
    val navController = rememberNavController()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val clubs by vm.allClubs.collectAsStateWithLifecycle()
    val standings by vm.standings.collectAsStateWithLifecycle()
    val activeCareer by vm.activeCareer.collectAsStateWithLifecycle()
    val squad by vm.careerClubPlayers.collectAsStateWithLifecycle()
    val market by vm.transferMarketPlayers.collectAsStateWithLifecycle()
    val fixtures by vm.careerFixtures.collectAsStateWithLifecycle()
    val trophies by vm.careerTrophies.collectAsStateWithLifecycle()
    val topScorers by vm.topScorers.collectAsStateWithLifecycle()
    val topAssists by vm.topAssists.collectAsStateWithLifecycle()
    val tournamentState by vm.tournamentState.collectAsStateWithLifecycle()
    val statusBanner by vm.statusBanner.collectAsStateWithLifecycle()
    val matchSnapshot by vm.matchSnapshot.collectAsStateWithLifecycle()
    val isPaused by vm.isPaused.collectAsStateWithLifecycle()

    var currentMatchSides by remember { mutableStateOf(MatchSides.P1_VS_AI) }

    NavHost(navController = navController, startDestination = Routes.MENU) {
        composable(Routes.MENU) {
            val careerClub = clubs.firstOrNull { it.id == activeCareer?.clubId }
            MainMenuScreen(
                activeCareer = activeCareer,
                careerClub = careerClub,
                onPlayMatch = { vm.audio.playMenuClick(); navController.navigate(Routes.QUICK_MATCH) },
                onCareerMode = { vm.audio.playMenuClick(); navController.navigate(Routes.CAREER) },
                onTournament = { vm.audio.playMenuClick(); navController.navigate(Routes.TOURNAMENT) },
                onTraining = { vm.audio.playMenuClick(); navController.navigate(Routes.TRAINING) },
                onSettings = { vm.audio.playMenuClick(); navController.navigate(Routes.SETTINGS) },
                onExitGame = onExit
            )
        }

        composable(Routes.QUICK_MATCH) {
            QuickMatchSetupScreen(
                clubs = clubs,
                settings = settings,
                onStartMatch = { cfg ->
                    vm.audio.playWhistle(false)
                    currentMatchSides = cfg.sides
                    vm.launchMatch(cfg) {
                        navController.navigate(Routes.LIVE_MATCH)
                    }
                },
                onBack = { vm.audio.playMenuClick(); navController.popBackStack() }
            )
        }

        composable(Routes.CAREER) {
            CareerHubScreen(
                career = activeCareer,
                clubs = clubs,
                standings = standings,
                squad = squad,
                transferMarket = market,
                fixtures = fixtures,
                trophies = trophies,
                topScorers = topScorers,
                topAssists = topAssists,
                settings = settings,
                statusBanner = statusBanner,
                onClearBanner = vm::clearBanner,
                onStartNewCareer = vm::startNewCareer,
                onPlayFixture = { cfg ->
                    vm.audio.playWhistle(false)
                    currentMatchSides = cfg.sides
                    vm.launchMatch(cfg) {
                        navController.navigate(Routes.LIVE_MATCH)
                    }
                },
                onBuyPlayer = vm::buyPlayer,
                onSellPlayer = vm::sellPlayer,
                onRenewContract = vm::renewContract,
                onTrainPlayer = vm::trainPlayer,
                onToggleStarter = vm::toggleStarter,
                onSetFormation = vm::setClubFormation,
                onBack = { vm.audio.playMenuClick(); navController.popBackStack() }
            )
        }

        composable(Routes.TOURNAMENT) {
            TournamentScreen(
                clubs = clubs,
                tournamentState = tournamentState,
                settings = settings,
                onStartTournament = vm::startTournament,
                onSimulateAiMatches = vm::simulateAiTournamentMatches,
                onPlayTournamentMatch = { cfg ->
                    vm.audio.playWhistle(false)
                    currentMatchSides = cfg.sides
                    vm.launchMatch(cfg) {
                        navController.navigate(Routes.LIVE_MATCH)
                    }
                },
                onBack = { vm.audio.playMenuClick(); navController.popBackStack() }
            )
        }

        composable(Routes.TRAINING) {
            TrainingScreen(
                clubs = clubs,
                onStartDrill = { cfg, drill ->
                    vm.audio.playWhistle(false)
                    currentMatchSides = MatchSides.P1_VS_AI
                    vm.launchMatch(cfg, trainingDrill = drill) {
                        navController.navigate(Routes.LIVE_MATCH)
                    }
                },
                onBack = { vm.audio.playMenuClick(); navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                settings = settings,
                onUpdateSettings = vm::updateSettings,
                onBack = { vm.audio.playMenuClick(); navController.popBackStack() }
            )
        }

        composable(Routes.LIVE_MATCH) {
            LiveMatchScreen(
                snapshot = matchSnapshot,
                sides = currentMatchSides,
                isPaused = isPaused,
                sfxEnabled = settings.sfxEnabled,
                p1Controls = vm.p1Controls,
                p2Controls = vm.p2Controls,
                keyBridge = vm.keyBridge,
                onTogglePause = vm::togglePause,
                onToggleSfx = { vm.updateSettings { it.copy(sfxEnabled = !it.sfxEnabled) } },
                onResumeHalftime = vm::resumeHalftime,
                onSubstitute = vm::substitutePlayer,
                onExitMatch = {
                    vm.exitMatch()
                    navController.popBackStack()
                }
            )
        }
    }
}
