package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.ClubEntity
import com.example.data.MatchLaunchConfig
import com.example.data.TrainingDrillType
import com.example.ui.components.ClubCrestBadge
import com.example.ui.components.PixelButton
import com.example.ui.components.PixelPanel
import com.example.ui.theme.ArcadeEmerald
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.PanelBorder
import com.example.ui.theme.PlayerCyan
import com.example.ui.theme.StadiumMidnight
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun TrainingScreen(
    clubs: List<ClubEntity>,
    onStartDrill: (MatchLaunchConfig, TrainingDrillType) -> Unit,
    onBack: () -> Unit
) {
    if (clubs.isEmpty()) return
    var selectedClubIdx by remember { mutableIntStateOf(0) }
    val club = clubs[selectedClubIdx % clubs.size]
    val opp = clubs[(selectedClubIdx + 1) % clubs.size]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StadiumMidnight)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TacticalSurface)
                .border(2.dp, PanelBorder)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("train_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text("TRAINING GROUND", style = MaterialTheme.typography.headlineMedium, color = ArcadeEmerald)
            }
            Row(
                modifier = Modifier
                    .background(StadiumMidnight)
                    .border(1.dp, ChampionshipGold)
                    .clickable { selectedClubIdx = (selectedClubIdx + 1) % clubs.size }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ClubCrestBadge(club.shortCode, club.primaryColorHex, club.secondaryColorHex, size = 24.dp)
                Text("CLUB: ${club.name}", style = MaterialTheme.typography.bodyMedium, color = ChampionshipGold)
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TrainingDrillType.entries.forEach { drill ->
                val icon = when (drill) {
                    TrainingDrillType.PASSING -> Icons.Default.TrackChanges
                    TrainingDrillType.SHOOTING -> Icons.Default.SportsSoccer
                    TrainingDrillType.DRIBBLING -> Icons.Default.DirectionsRun
                }
                val accent = when (drill) {
                    TrainingDrillType.PASSING -> ArcadeEmerald
                    TrainingDrillType.SHOOTING -> ChampionshipGold
                    TrainingDrillType.DRIBBLING -> PlayerCyan
                }
                PixelPanel(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    borderColor = accent
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(StadiumMidnight)
                                    .border(2.dp, accent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = drill.title, tint = accent, modifier = Modifier.size(32.dp))
                            }
                            Text(drill.title.uppercase(), style = MaterialTheme.typography.titleMedium, color = accent)
                            Text(drill.subtitle, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                            Text("BOOST: ${drill.focusStat}", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                        }
                        PixelButton(
                            text = "START DRILL",
                            icon = Icons.Default.PlayArrow,
                            onClick = {
                                onStartDrill(
                                    MatchLaunchConfig(
                                        homeClubId = club.id,
                                        awayClubId = opp.id
                                    ),
                                    drill
                                )
                            },
                            containerColor = accent,
                            testTag = "train_start_${drill.name.lowercase()}",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
