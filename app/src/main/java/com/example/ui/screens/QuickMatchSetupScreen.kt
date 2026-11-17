package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.AppSettings
import com.example.data.ClubEntity
import com.example.data.Formation
import com.example.data.MatchLaunchConfig
import com.example.data.MatchSides
import com.example.data.TimeOfDay
import com.example.data.WeatherType
import com.example.ui.components.ClubCrestBadge
import com.example.ui.components.PixelPanel
import com.example.ui.theme.ArcadeEmerald
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.PanelBorder
import com.example.ui.theme.PitchGrassDark
import com.example.ui.theme.PitchGrassLight
import com.example.ui.theme.PlayerCyan
import com.example.ui.theme.StadiumMidnight
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun QuickMatchSetupScreen(
    clubs: List<ClubEntity>,
    settings: AppSettings,
    onStartMatch: (MatchLaunchConfig) -> Unit,
    onBack: () -> Unit
) {
    if (clubs.isEmpty()) return

    var homeIdx by remember { mutableIntStateOf(0) }
    var awayIdx by remember { mutableIntStateOf(if (clubs.size > 1) 1 else 0) }
    var sides by remember { mutableStateOf(if (settings.dualPadEnabled) MatchSides.P1_VS_P2 else MatchSides.P1_VS_AI) }
    var weather by remember { mutableStateOf(WeatherType.CLEAR) }
    var timeOfDay by remember { mutableStateOf(TimeOfDay.NIGHT) }
    var durationMin by remember { mutableIntStateOf(settings.defaultDurationMin) }
    var gameSpeed by remember { mutableFloatStateOf(settings.defaultSpeed) }
    var homeFormation by remember { mutableStateOf(clubs[0].formation) }

    val homeClub = clubs[homeIdx % clubs.size]
    val awayClub = clubs[awayIdx % clubs.size]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StadiumMidnight)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header (Inspired by Retro Friendly Match Screen)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(TacticalSurface)
                .border(2.dp, PanelBorder)
                .padding(vertical = 8.dp, horizontal = 12.dp)
        ) {
            Text(
                text = "FRIENDLY MATCH",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
            Text(
                text = "${homeClub.shortCode} vs ${awayClub.shortCode}",
                style = MaterialTheme.typography.titleMedium,
                color = ChampionshipGold,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        // Center 2.5D Mini Stadium Preview + Team Matchup
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home Team Card
            PixelPanel(
                modifier = Modifier.weight(0.75f),
                borderColor = PlayerCyan
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("HOME (P1)", style = MaterialTheme.typography.labelSmall, color = PlayerCyan)
                    ClubCrestBadge(homeClub.shortCode, homeClub.primaryColorHex, homeClub.secondaryColorHex, size = 48.dp)
                    Text(homeClub.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary, textAlign = TextAlign.Center)
                    Text("OVR ${homeClub.teamRating} • ${homeFormation.code}", style = MaterialTheme.typography.bodyMedium, color = ChampionshipGold)
                }
            }

            // Center 2.5D Pitch Preview
            Box(
                modifier = Modifier
                    .weight(1.4f)
                    .height(165.dp)
                    .border(2.dp, PanelBorder)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawRect(if (timeOfDay == TimeOfDay.NIGHT) Color(0xFF0A1422) else Color(0xFF285D3B))
                    val pitchPath = Path().apply {
                        moveTo(w * 0.16f, h * 0.16f)
                        lineTo(w * 0.84f, h * 0.16f)
                        lineTo(w * 0.94f, h * 0.86f)
                        lineTo(w * 0.06f, h * 0.86f)
                        close()
                    }
                    drawPath(pitchPath, PitchGrassDark)
                    for (i in 0..7 step 2) {
                        val x0 = w * (0.16f + i * 0.085f)
                        val x1 = w * (0.16f + (i + 1) * 0.085f)
                        val bx0 = w * (0.06f + i * 0.11f)
                        val bx1 = w * (0.06f + (i + 1) * 0.11f)
                        val stripe = Path().apply {
                            moveTo(x0, h * 0.16f); lineTo(x1, h * 0.16f)
                            lineTo(bx1, h * 0.86f); lineTo(bx0, h * 0.86f)
                            close()
                        }
                        drawPath(stripe, PitchGrassLight)
                    }
                    drawPath(pitchPath, Color.White, style = Stroke(width = 2.5f))
                    drawLine(Color.White, Offset(w * 0.5f, h * 0.16f), Offset(w * 0.5f, h * 0.86f), 2f)
                    drawCircle(Color.White, radius = h * 0.13f, center = Offset(w * 0.5f, h * 0.51f), style = Stroke(width = 2f))
                    drawCircle(Color.White, radius = 6f, center = Offset(w * 0.5f, h * 0.51f))

                    if (weather != WeatherType.CLEAR) {
                        val drops = if (weather == WeatherType.STORM) 45 else 24
                        for (i in 0 until drops) {
                            val rx = (i * 47f) % w
                            val ry = (i * 31f) % h
                            drawLine(Color(0x9993C5FD), Offset(rx, ry), Offset(rx - 4f, ry + 12f), 1.5f)
                        }
                    }
                }
                Text(
                    text = "${weather.label.uppercase()} • ${timeOfDay.label.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(Color(0xAA0D1520))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            // Away Team Card
            PixelPanel(
                modifier = Modifier.weight(0.75f),
                borderColor = ChampionshipGold
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (sides == MatchSides.P1_VS_P2) "AWAY (P2)" else "AWAY (AI)",
                        style = MaterialTheme.typography.labelSmall,
                        color = ChampionshipGold
                    )
                    ClubCrestBadge(awayClub.shortCode, awayClub.primaryColorHex, awayClub.secondaryColorHex, size = 48.dp)
                    Text(awayClub.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary, textAlign = TextAlign.Center)
                    Text("OVR ${awayClub.teamRating} • ${awayClub.formation.code}", style = MaterialTheme.typography.bodyMedium, color = ArcadeEmerald)
                }
            }
        }

        // Bottom Retro Card Strip (Inspired by Screenshot #3)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RetroSetupCard(
                title = "START\nMATCH!",
                subtitle = "KICK OFF",
                icon = Icons.Default.SportsSoccer,
                highlight = true,
                accentColor = ChampionshipGold,
                testTag = "qm_start_match",
                onClick = {
                    onStartMatch(
                        MatchLaunchConfig(
                            homeClubId = homeClub.id,
                            awayClubId = awayClub.id,
                            homeFormation = homeFormation,
                            awayFormation = awayClub.formation,
                            sides = sides,
                            weather = weather,
                            timeOfDay = timeOfDay,
                            durationMinutes = durationMin,
                            gameSpeed = gameSpeed,
                            difficulty = settings.difficulty
                        )
                    )
                }
            )
            RetroSetupCard(
                title = "TEAM 1",
                subtitle = homeClub.shortCode,
                icon = Icons.Default.SportsSoccer,
                accentColor = PlayerCyan,
                testTag = "qm_team1",
                onClick = { homeIdx = (homeIdx + 1) % clubs.size }
            )
            RetroSetupCard(
                title = "TEAM 2",
                subtitle = awayClub.shortCode,
                icon = Icons.Default.SportsSoccer,
                accentColor = ArcadeEmerald,
                testTag = "qm_team2",
                onClick = { awayIdx = (awayIdx + 1) % clubs.size }
            )
            RetroSetupCard(
                title = "SIDES",
                subtitle = sides.label,
                icon = Icons.Default.SportsEsports,
                testTag = "qm_sides",
                onClick = {
                    sides = if (sides == MatchSides.P1_VS_AI) MatchSides.P1_VS_P2 else MatchSides.P1_VS_AI
                }
            )
            RetroSetupCard(
                title = "FORMATION",
                subtitle = homeFormation.code,
                icon = Icons.Default.SportsSoccer,
                testTag = "qm_formation",
                onClick = {
                    val all = Formation.entries
                    homeFormation = all[(homeFormation.ordinal + 1) % all.size]
                }
            )
            RetroSetupCard(
                title = "TIME",
                subtitle = timeOfDay.label,
                icon = Icons.Default.WbSunny,
                testTag = "qm_time",
                onClick = {
                    timeOfDay = if (timeOfDay == TimeOfDay.NIGHT) TimeOfDay.DAY else TimeOfDay.NIGHT
                }
            )
            RetroSetupCard(
                title = "WEATHER",
                subtitle = weather.label,
                icon = Icons.Default.Cloud,
                testTag = "qm_weather",
                onClick = {
                    val all = WeatherType.entries
                    weather = all[(weather.ordinal + 1) % all.size]
                }
            )
            RetroSetupCard(
                title = "DURATION",
                subtitle = "${durationMin} MIN",
                icon = Icons.Default.Timer,
                testTag = "qm_duration",
                onClick = {
                    durationMin = when (durationMin) {
                        2 -> 3
                        3 -> 5
                        else -> 2
                    }
                }
            )
            RetroSetupCard(
                title = "SPEED",
                subtitle = "${gameSpeed}x",
                icon = Icons.Default.Speed,
                testTag = "qm_speed",
                onClick = {
                    gameSpeed = when {
                        gameSpeed < 0.9f -> 1.0f
                        gameSpeed < 1.15f -> 1.25f
                        else -> 0.85f
                    }
                }
            )
            RetroSetupCard(
                title = "BACK",
                subtitle = "MENU",
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                testTag = "qm_back",
                onClick = onBack
            )
        }
    }
}

@Composable
private fun RetroSetupCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    testTag: String,
    highlight: Boolean = false,
    accentColor: Color = TextPrimary,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(88.dp)
            .height(92.dp)
            .minimumInteractiveComponentSize()
            .testTag(testTag)
            .clickable(onClick = onClick),
        color = if (highlight) Color(0xFF283A52) else TacticalSurface,
        shape = RoundedCornerShape(3.dp),
        border = BorderStroke(2.dp, if (highlight) ChampionshipGold else PanelBorder)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(26.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (highlight) ChampionshipGold else accentColor,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
