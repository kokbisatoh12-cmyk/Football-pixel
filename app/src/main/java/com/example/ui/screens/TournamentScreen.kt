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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
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
import com.example.data.AppSettings
import com.example.data.ClubEntity
import com.example.data.MatchLaunchConfig
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
import com.example.viewmodel.TournamentMatchNode
import com.example.viewmodel.TournamentState

@Composable
fun TournamentScreen(
    clubs: List<ClubEntity>,
    tournamentState: TournamentState,
    settings: AppSettings,
    onStartTournament: (Int) -> Unit,
    onSimulateAiMatches: () -> Unit,
    onPlayTournamentMatch: (MatchLaunchConfig) -> Unit,
    onBack: () -> Unit
) {
    var selectedClubId by remember(clubs) { mutableIntStateOf(clubs.firstOrNull()?.id ?: 1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StadiumMidnight)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Header
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
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("tourn_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text("PIXEL CUP TOURNAMENT", style = MaterialTheme.typography.headlineMedium, color = ChampionshipGold)
            }
            if (tournamentState.matches.isNotEmpty()) {
                PixelButton(
                    text = "NEW CUP",
                    onClick = { onStartTournament(selectedClubId) },
                    containerColor = TacticalSurface,
                    contentColor = TextPrimary,
                    testTag = "tourn_reset_btn"
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        if (tournamentState.matches.isEmpty()) {
            // Club picker to start 8-club knockout cup
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PixelPanel(modifier = Modifier.weight(0.9f).fillMaxHeight(), borderColor = ChampionshipGold) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = "Pixel Cup", tint = ChampionshipGold, modifier = Modifier.size(44.dp))
                            Text("8-CLUB KNOCKOUT CUP", style = MaterialTheme.typography.titleLarge, color = ChampionshipGold)
                            Text(
                                "Battle through Quarterfinals, Semifinals, and the Grand Final to lift the Pixel Cup Trophy!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                        PixelButton(
                            text = "ENTER TOURNAMENT",
                            icon = Icons.Default.PlayArrow,
                            onClick = { onStartTournament(selectedClubId) },
                            containerColor = ChampionshipGold,
                            testTag = "tourn_enter_btn",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                PixelPanel(modifier = Modifier.weight(1.1f).fillMaxHeight(), borderColor = PlayerCyan) {
                    Column {
                        Text("SELECT YOUR CLUB", style = MaterialTheme.typography.titleMedium, color = PlayerCyan)
                        Spacer(Modifier.height(6.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            items(clubs) { club ->
                                val sel = club.id == selectedClubId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (sel) PlayerCyan.copy(alpha = 0.22f) else StadiumMidnight)
                                        .border(2.dp, if (sel) PlayerCyan else PanelBorder)
                                        .clickable { selectedClubId = club.id }
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        ClubCrestBadge(club.shortCode, club.primaryColorHex, club.secondaryColorHex, size = 30.dp)
                                        Text(club.name, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                                    }
                                    Text("OVR ${club.teamRating}", style = MaterialTheme.typography.bodyMedium, color = ChampionshipGold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val userClub = tournamentState.userClub
            val nextUserMatch = tournamentState.matches.firstOrNull { m ->
                m.winnerClub == null && m.homeClub != null && m.awayClub != null &&
                    (m.homeClub.id == userClub?.id || m.awayClub.id == userClub?.id)
            }

            if (tournamentState.champion != null) {
                PixelPanel(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    borderColor = ChampionshipGold
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = "Champion", tint = ChampionshipGold, modifier = Modifier.size(34.dp))
                            Text(
                                "CHAMPION: ${tournamentState.champion.name.uppercase()}!",
                                style = MaterialTheme.typography.titleLarge,
                                color = ChampionshipGold
                            )
                        }
                        PixelButton(
                            text = "PLAY AGAIN",
                            onClick = { onStartTournament(userClub?.id ?: selectedClubId) },
                            containerColor = ArcadeEmerald
                        )
                    }
                }
            }

            // Bracket Grid: QF | SF | FINAL
            Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BracketColumn("QUARTERFINALS", tournamentState.matches.subList(0, 4), userClub?.id, Modifier.weight(1f))
                BracketColumn("SEMIFINALS", tournamentState.matches.subList(4, 6), userClub?.id, Modifier.weight(1f))
                Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    BracketColumn("GRAND FINAL", listOf(tournamentState.matches[6]), userClub?.id, Modifier.fillMaxWidth())

                    PixelPanel(borderColor = ArcadeEmerald) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (nextUserMatch != null && nextUserMatch.homeClub != null && nextUserMatch.awayClub != null) {
                                Text(
                                    "NEXT: ${nextUserMatch.homeClub.shortCode} vs ${nextUserMatch.awayClub.shortCode}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ChampionshipGold
                                )
                                PixelButton(
                                    text = "PLAY ${nextUserMatch.roundLabel}",
                                    icon = Icons.Default.PlayArrow,
                                    onClick = {
                                        onPlayTournamentMatch(
                                            MatchLaunchConfig(
                                                homeClubId = nextUserMatch.homeClub.id,
                                                awayClubId = nextUserMatch.awayClub.id,
                                                homeFormation = nextUserMatch.homeClub.formation,
                                                awayFormation = nextUserMatch.awayClub.formation,
                                                durationMinutes = settings.defaultDurationMin,
                                                gameSpeed = settings.defaultSpeed,
                                                difficulty = settings.difficulty,
                                                tournamentMatchId = nextUserMatch.id
                                            )
                                        )
                                    },
                                    containerColor = ChampionshipGold,
                                    testTag = "tourn_play_next",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                PixelButton(
                                    text = "SIMULATE AI ROUNDS",
                                    onClick = onSimulateAiMatches,
                                    containerColor = PlayerCyan,
                                    testTag = "tourn_sim_ai",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BracketColumn(
    title: String,
    nodes: List<TournamentMatchNode>,
    userClubId: Int?,
    modifier: Modifier = Modifier
) {
    PixelPanel(modifier = modifier, borderColor = PanelBorder) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = PlayerCyan)
            nodes.forEach { m ->
                val isUserInvolved = m.homeClub?.id == userClubId || m.awayClub?.id == userClubId
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StadiumMidnight)
                        .border(1.dp, if (isUserInvolved) ChampionshipGold else PanelBorder)
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(m.roundLabel, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(m.homeClub?.shortCode ?: "TBD", style = MaterialTheme.typography.bodyLarge, color = if (m.homeClub?.id == userClubId) ChampionshipGold else TextPrimary)
                        Text(m.homeScore?.toString() ?: "-", style = MaterialTheme.typography.bodyLarge, color = ArcadeEmerald)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(m.awayClub?.shortCode ?: "TBD", style = MaterialTheme.typography.bodyLarge, color = if (m.awayClub?.id == userClubId) ChampionshipGold else TextPrimary)
                        Text(m.awayScore?.toString() ?: "-", style = MaterialTheme.typography.bodyLarge, color = ArcadeEmerald)
                    }
                }
            }
        }
    }
}
