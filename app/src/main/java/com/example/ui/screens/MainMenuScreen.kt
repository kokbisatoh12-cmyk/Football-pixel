package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CareerSaveEntity
import com.example.data.ClubEntity
import com.example.ui.components.ClubCrestBadge
import com.example.ui.components.PixelButton
import com.example.ui.components.PixelPanel
import com.example.ui.theme.ArcadeEmerald
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.FoulCrimson
import com.example.ui.theme.PanelHighlight
import com.example.ui.theme.PlayerCyan
import com.example.ui.theme.StadiumMidnight
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun MainMenuScreen(
    activeCareer: CareerSaveEntity?,
    careerClub: ClubEntity?,
    onPlayMatch: () -> Unit,
    onCareerMode: () -> Unit,
    onTournament: () -> Unit,
    onTraining: () -> Unit,
    onSettings: () -> Unit,
    onExitGame: () -> Unit
) {
    val pulseTransition = rememberInfiniteTransition(label = "floodlight_pulse")
    val shimmerAlpha by pulseTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 0.94f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "shimmer"
    )

    Box(modifier = Modifier.fillMaxSize().background(StadiumMidnight)) {
        // Animated pixel stadium background
        Image(
            painter = painterResource(id = R.drawable.img_stadium_menu_bg),
            contentDescription = "Retro Pixel Stadium Backdrop",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(shimmerAlpha)
        )

        // Contrast scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xD80D1520),
                            Color(0x990D1520),
                            Color(0xE80D1520)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Retro Marquee Logo & Career Status
            Column(
                modifier = Modifier.weight(1.1f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_pixel_kick_icon),
                            contentDescription = "Pixel Kick Crest",
                            modifier = Modifier
                                .size(64.dp)
                                .border(2.dp, ChampionshipGold)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "PIXEL KICK",
                                style = MaterialTheme.typography.displayLarge,
                                color = ChampionshipGold
                            )
                            Text(
                                text = "LEGACY 16-BIT EDITION",
                                style = MaterialTheme.typography.titleMedium,
                                color = ArcadeEmerald
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    PixelPanel(
                        borderColor = PanelHighlight,
                        backgroundColor = Color(0xCC182436)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "11v11 REAL-TIME ARCADE FOOTBALL",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "• 2.5D Pixel Pitch • FSM AI • Career Transfers • Dual Pad",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    }
                }

                if (activeCareer != null && careerClub != null) {
                    PixelPanel(
                        borderColor = ChampionshipGold,
                        backgroundColor = Color(0xDD182436)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ClubCrestBadge(
                                shortCode = careerClub.shortCode,
                                primaryColorHex = careerClub.primaryColorHex,
                                secondaryColorHex = careerClub.secondaryColorHex,
                                size = 40.dp
                            )
                            Column {
                                Text(
                                    text = "ACTIVE CAREER: ${careerClub.name.uppercase()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ChampionshipGold
                                )
                                Text(
                                    text = "Season ${activeCareer.seasonNumber} • Matchday ${activeCareer.currentMatchday}/${activeCareer.totalMatchdays} • ${activeCareer.role.label}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                } else {
                    PixelPanel(
                        borderColor = PanelHighlight,
                        backgroundColor = Color(0xBB182436)
                    ) {
                        Text(
                            text = "CONTROLS: Touch Joystick + Pass / Through / Shoot / Sprint • Supports 2P Dual Pad & Gamepad",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            }

            // Right Column: 6 Arcade Menu Actions
            Column(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
            ) {
                PixelButton(
                    text = stringResource(R.string.menu_play_match),
                    icon = Icons.Default.PlayArrow,
                    onClick = onPlayMatch,
                    containerColor = ChampionshipGold,
                    contentColor = StadiumMidnight,
                    testTag = "menu_play_match_btn",
                    modifier = Modifier.fillMaxWidth()
                )
                PixelButton(
                    text = stringResource(R.string.menu_career_mode),
                    icon = Icons.Default.WorkHistory,
                    onClick = onCareerMode,
                    containerColor = ArcadeEmerald,
                    contentColor = StadiumMidnight,
                    testTag = "menu_career_mode_btn",
                    modifier = Modifier.fillMaxWidth()
                )
                PixelButton(
                    text = stringResource(R.string.menu_tournament),
                    icon = Icons.Default.EmojiEvents,
                    onClick = onTournament,
                    containerColor = PlayerCyan,
                    contentColor = StadiumMidnight,
                    testTag = "menu_tournament_btn",
                    modifier = Modifier.fillMaxWidth()
                )
                PixelButton(
                    text = stringResource(R.string.menu_training),
                    icon = Icons.Default.FitnessCenter,
                    onClick = onTraining,
                    containerColor = TacticalSurface,
                    contentColor = TextPrimary,
                    testTag = "menu_training_btn",
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PixelButton(
                        text = stringResource(R.string.menu_settings),
                        icon = Icons.Default.Settings,
                        onClick = onSettings,
                        containerColor = TacticalSurface,
                        contentColor = TextPrimary,
                        testTag = "menu_settings_btn",
                        modifier = Modifier.weight(1f)
                    )
                    PixelButton(
                        text = stringResource(R.string.menu_exit),
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        onClick = onExitGame,
                        containerColor = FoulCrimson,
                        contentColor = TextPrimary,
                        testTag = "menu_exit_btn",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
