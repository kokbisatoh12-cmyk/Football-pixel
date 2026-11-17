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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.AppSettings
import com.example.data.CareerRole
import com.example.data.CareerSaveEntity
import com.example.data.ClubEntity
import com.example.data.FixtureEntity
import com.example.data.Formation
import com.example.data.MatchLaunchConfig
import com.example.data.PlayerEntity
import com.example.data.Position
import com.example.data.TrophyEntity
import com.example.ui.components.ClubCrestBadge
import com.example.ui.components.PixelButton
import com.example.ui.components.PixelPanel
import com.example.ui.components.PixelStatBar
import com.example.ui.theme.ArcadeEmerald
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.FoulCrimson
import com.example.ui.theme.PanelBorder
import com.example.ui.theme.PlayerCyan
import com.example.ui.theme.StadiumMidnight
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun CareerHubScreen(
    career: CareerSaveEntity?,
    clubs: List<ClubEntity>,
    standings: List<ClubEntity>,
    squad: List<PlayerEntity>,
    transferMarket: List<PlayerEntity>,
    fixtures: List<FixtureEntity>,
    trophies: List<TrophyEntity>,
    topScorers: List<PlayerEntity>,
    topAssists: List<PlayerEntity>,
    settings: AppSettings,
    statusBanner: String?,
    onClearBanner: () -> Unit,
    onStartNewCareer: (CareerRole, Int, String, Position) -> Unit,
    onPlayFixture: (MatchLaunchConfig) -> Unit,
    onBuyPlayer: (Int) -> Unit,
    onSellPlayer: (Int) -> Unit,
    onRenewContract: (Int) -> Unit,
    onTrainPlayer: (Int, String) -> Unit,
    onToggleStarter: (Int) -> Unit,
    onSetFormation: (Int, Formation) -> Unit,
    onBack: () -> Unit
) {
    var creatingNew by remember(career) { mutableStateOf(career == null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedPlayer by remember { mutableStateOf<PlayerEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StadiumMidnight)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // Top Header Bar
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
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("career_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text("CAREER MODE", style = MaterialTheme.typography.headlineMedium, color = ChampionshipGold)
            }
            if (career != null && !creatingNew) {
                val myClub = clubs.firstOrNull { it.id == career.clubId }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${myClub?.name ?: "Club"} • S${career.seasonNumber} MD ${career.currentMatchday}/${career.totalMatchdays} • $${(myClub?.budget ?: 0L) / 1_000_000}M",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ArcadeEmerald
                    )
                    PixelButton(
                        text = "NEW",
                        icon = Icons.Default.AddCircleOutline,
                        onClick = { creatingNew = true },
                        containerColor = TacticalSurfaceVariant,
                        contentColor = TextPrimary,
                        testTag = "career_new_btn"
                    )
                }
            }
        }

        if (statusBanner != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .background(ArcadeEmerald.copy(alpha = 0.2f))
                    .border(1.dp, ArcadeEmerald)
                    .clickable { onClearBanner() }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(statusBanner, style = MaterialTheme.typography.bodyMedium, color = ArcadeEmerald)
                Text("DISMISS", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }

        Spacer(Modifier.height(8.dp))

        if (creatingNew || career == null) {
            CareerOnboardingPane(
                clubs = clubs,
                canCancel = career != null,
                onCancel = { creatingNew = false },
                onConfirm = { role, clubId, name, pos ->
                    onStartNewCareer(role, clubId, name, pos)
                    creatingNew = false
                }
            )
            return
        }

        val myClub = clubs.firstOrNull { it.id == career.clubId }

        // Tab Strip
        val tabs = listOf("NEXT MATCH", "STANDINGS", "SQUAD", "TRANSFERS", "STATS & TROPHIES")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tabs.forEachIndexed { idx, label ->
                val active = selectedTab == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (active) ChampionshipGold else TacticalSurface)
                        .border(2.dp, if (active) ChampionshipGold else PanelBorder)
                        .clickable { selectedTab = idx }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) StadiumMidnight else TextPrimary
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                0 -> CareerOverviewTab(
                    career = career,
                    myClub = myClub,
                    clubs = clubs,
                    fixtures = fixtures,
                    settings = settings,
                    onPlayFixture = onPlayFixture
                )
                1 -> StandingsTab(standings = standings, myClubId = career.clubId)
                2 -> SquadTab(
                    myClub = myClub,
                    career = career,
                    squad = squad,
                    selectedPlayer = selectedPlayer,
                    onSelectPlayer = { selectedPlayer = it },
                    onToggleStarter = onToggleStarter,
                    onTrainPlayer = onTrainPlayer,
                    onRenewContract = onRenewContract,
                    onSellPlayer = onSellPlayer,
                    onSetFormation = onSetFormation
                )
                3 -> TransferMarketTab(
                    myClub = myClub,
                    clubs = clubs,
                    marketPlayers = transferMarket,
                    onBuyPlayer = onBuyPlayer
                )
                else -> StatsAndTrophiesTab(
                    topScorers = topScorers,
                    topAssists = topAssists,
                    trophies = trophies,
                    clubs = clubs
                )
            }
        }
    }
}

@Composable
private fun CareerOnboardingPane(
    clubs: List<ClubEntity>,
    canCancel: Boolean,
    onCancel: () -> Unit,
    onConfirm: (CareerRole, Int, String, Position) -> Unit
) {
    var role by remember { mutableStateOf(CareerRole.MANAGER) }
    var selectedClubId by remember(clubs) { mutableIntStateOf(clubs.firstOrNull()?.id ?: 1) }
    var customName by remember { mutableStateOf("Rizky Pratama") }
    var customPos by remember { mutableStateOf(Position.FWD) }

    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // Left: Role & Player Creator
        PixelPanel(modifier = Modifier.weight(0.9f).fillMaxHeight(), borderColor = ChampionshipGold) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("1. SELECT CAREER PATH", style = MaterialTheme.typography.titleMedium, color = ChampionshipGold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CareerRole.entries.forEach { r ->
                        val active = role == r
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (active) ArcadeEmerald else StadiumMidnight)
                                .border(2.dp, if (active) ArcadeEmerald else PanelBorder)
                                .clickable { role = r }
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(r.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = if (active) StadiumMidnight else TextPrimary)
                        }
                    }
                }

                if (role == CareerRole.PLAYER) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Star Player Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(Position.FWD, Position.MID, Position.DEF).forEach { pos ->
                            val sel = customPos == pos
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (sel) PlayerCyan else StadiumMidnight)
                                    .border(1.dp, PanelBorder)
                                    .clickable { customPos = pos }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(pos.label, style = MaterialTheme.typography.labelSmall, color = if (sel) StadiumMidnight else TextPrimary)
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Manage tactics, contracts, player development, and transfers to win the Pixel League & trophies.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }

                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (canCancel) {
                        PixelButton(text = "CANCEL", onClick = onCancel, containerColor = TacticalSurfaceVariant, contentColor = TextPrimary, modifier = Modifier.weight(1f))
                    }
                    PixelButton(
                        text = "START CAREER",
                        onClick = { onConfirm(role, selectedClubId, customName, customPos) },
                        containerColor = ChampionshipGold,
                        testTag = "career_confirm_start",
                        modifier = Modifier.weight(1.4f)
                    )
                }
            }
        }

        // Right: Club Selector
        PixelPanel(modifier = Modifier.weight(1.1f).fillMaxHeight(), borderColor = PlayerCyan) {
            Column {
                Text("2. PICK YOUR CLUB", style = MaterialTheme.typography.titleMedium, color = PlayerCyan)
                Spacer(Modifier.height(6.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(clubs) { club ->
                        val sel = club.id == selectedClubId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (sel) PlayerCyan.copy(alpha = 0.2f) else StadiumMidnight)
                                .border(2.dp, if (sel) PlayerCyan else PanelBorder)
                                .clickable { selectedClubId = club.id }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ClubCrestBadge(club.shortCode, club.primaryColorHex, club.secondaryColorHex, size = 32.dp)
                                Column {
                                    Text(club.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text("Formation ${club.formation.code}", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("OVR ${club.teamRating}", style = MaterialTheme.typography.titleMedium, color = ChampionshipGold)
                                Text("$${club.budget / 1_000_000}M", style = MaterialTheme.typography.bodyMedium, color = ArcadeEmerald)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CareerOverviewTab(
    career: CareerSaveEntity,
    myClub: ClubEntity?,
    clubs: List<ClubEntity>,
    fixtures: List<FixtureEntity>,
    settings: AppSettings,
    onPlayFixture: (MatchLaunchConfig) -> Unit
) {
    val nextFixture = fixtures.firstOrNull {
        !it.isPlayed && it.matchday == career.currentMatchday &&
            (it.homeClubId == career.clubId || it.awayClubId == career.clubId)
    }
    val home = clubs.firstOrNull { it.id == nextFixture?.homeClubId }
    val away = clubs.firstOrNull { it.id == nextFixture?.awayClubId }

    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // Next Match Card
        PixelPanel(modifier = Modifier.weight(1f).fillMaxHeight(), borderColor = ChampionshipGold) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("MATCHDAY ${career.currentMatchday} FIXTURE", style = MaterialTheme.typography.titleMedium, color = ChampionshipGold)
                if (nextFixture != null && home != null && away != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ClubCrestBadge(home.shortCode, home.primaryColorHex, home.secondaryColorHex, size = 50.dp)
                            Spacer(Modifier.height(4.dp))
                            Text(home.name, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            Text("OVR ${home.teamRating}", style = MaterialTheme.typography.bodyMedium, color = PlayerCyan)
                        }
                        Text("VS", style = MaterialTheme.typography.headlineMedium, color = ChampionshipGold)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ClubCrestBadge(away.shortCode, away.primaryColorHex, away.secondaryColorHex, size = 50.dp)
                            Spacer(Modifier.height(4.dp))
                            Text(away.name, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            Text("OVR ${away.teamRating}", style = MaterialTheme.typography.bodyMedium, color = ArcadeEmerald)
                        }
                    }

                    PixelButton(
                        text = "PLAY MATCHDAY ${career.currentMatchday}",
                        icon = Icons.Default.PlayArrow,
                        onClick = {
                            onPlayFixture(
                                MatchLaunchConfig(
                                    homeClubId = home.id,
                                    awayClubId = away.id,
                                    homeFormation = home.formation,
                                    awayFormation = away.formation,
                                    durationMinutes = settings.defaultDurationMin,
                                    gameSpeed = settings.defaultSpeed,
                                    difficulty = settings.difficulty,
                                    careerFixtureId = nextFixture.id,
                                    lockedUserPlayerId = career.userPlayerId
                                )
                            )
                        },
                        containerColor = ChampionshipGold,
                        testTag = "career_play_next_match",
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text("All fixtures completed!", style = MaterialTheme.typography.titleMedium, color = ArcadeEmerald)
                }
            }
        }

        // Club Finances & Schedule
        PixelPanel(modifier = Modifier.weight(1f).fillMaxHeight(), borderColor = PanelBorder) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("CLUB FINANCES & SCHEDULE", style = MaterialTheme.typography.titleMedium, color = PlayerCyan)
                if (myClub != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Budget: $${myClub.budget / 1_000_000}M", style = MaterialTheme.typography.bodyLarge, color = ArcadeEmerald)
                        Text("Wages: $${myClub.wageBill / 1000}K/wk", style = MaterialTheme.typography.bodyLarge, color = ChampionshipGold)
                    }
                    Text("Training Points Available: ${career.trainingPoints}", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
                val myFixtures = fixtures.filter { it.homeClubId == career.clubId || it.awayClubId == career.clubId }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(myFixtures) { f ->
                        val h = clubs.firstOrNull { it.id == f.homeClubId }?.shortCode ?: "HOM"
                        val a = clubs.firstOrNull { it.id == f.awayClubId }?.shortCode ?: "AWY"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(StadiumMidnight)
                                .border(1.dp, PanelBorder)
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("MD ${f.matchday}: $h vs $a", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text(
                                text = if (f.isPlayed) "${f.homeGoals} - ${f.awayGoals}" else "UPCOMING",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (f.isPlayed) ChampionshipGold else TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StandingsTab(standings: List<ClubEntity>, myClubId: Int) {
    PixelPanel(modifier = Modifier.fillMaxSize(), borderColor = PanelBorder) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                Text("#", modifier = Modifier.width(28.dp), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text("CLUB", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text("P", modifier = Modifier.width(34.dp), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text("W", modifier = Modifier.width(34.dp), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text("D", modifier = Modifier.width(34.dp), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text("L", modifier = Modifier.width(34.dp), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text("GD", modifier = Modifier.width(42.dp), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text("PTS", modifier = Modifier.width(44.dp), style = MaterialTheme.typography.labelSmall, color = ChampionshipGold)
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                itemsIndexed(standings) { idx, club ->
                    val isMine = club.id == myClubId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isMine) ChampionshipGold.copy(alpha = 0.16f) else StadiumMidnight)
                            .border(1.dp, if (isMine) ChampionshipGold else PanelBorder)
                            .padding(vertical = 6.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${idx + 1}", modifier = Modifier.width(28.dp), style = MaterialTheme.typography.bodyMedium, color = if (idx == 0) ChampionshipGold else TextMuted)
                        Text(club.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = if (isMine) ChampionshipGold else TextPrimary)
                        Text("${club.played}", modifier = Modifier.width(34.dp), style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text("${club.won}", modifier = Modifier.width(34.dp), style = MaterialTheme.typography.bodyMedium, color = ArcadeEmerald)
                        Text("${club.drawn}", modifier = Modifier.width(34.dp), style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        Text("${club.lost}", modifier = Modifier.width(34.dp), style = MaterialTheme.typography.bodyMedium, color = FoulCrimson)
                        Text("${club.goalDifference}", modifier = Modifier.width(42.dp), style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text("${club.points}", modifier = Modifier.width(44.dp), style = MaterialTheme.typography.titleMedium, color = ChampionshipGold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SquadTab(
    myClub: ClubEntity?,
    career: CareerSaveEntity,
    squad: List<PlayerEntity>,
    selectedPlayer: PlayerEntity?,
    onSelectPlayer: (PlayerEntity) -> Unit,
    onToggleStarter: (Int) -> Unit,
    onTrainPlayer: (Int, String) -> Unit,
    onRenewContract: (Int) -> Unit,
    onSellPlayer: (Int) -> Unit,
    onSetFormation: (Int, Formation) -> Unit
) {
    val activePlayer = squad.firstOrNull { it.id == selectedPlayer?.id } ?: squad.firstOrNull()
    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // Left: Squad List + Formation Bar
        PixelPanel(modifier = Modifier.weight(1.1f).fillMaxHeight(), borderColor = PanelBorder) {
            Column {
                if (myClub != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("FORMATION:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Formation.entries.forEach { f ->
                            val sel = myClub.formation == f
                            Box(
                                modifier = Modifier
                                    .background(if (sel) PlayerCyan else StadiumMidnight)
                                    .border(1.dp, PanelBorder)
                                    .clickable { onSetFormation(myClub.id, f) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(f.code, style = MaterialTheme.typography.labelSmall, color = if (sel) StadiumMidnight else TextPrimary)
                            }
                        }
                    }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(squad) { p ->
                        val sel = activePlayer?.id == p.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (sel) PlayerCyan.copy(alpha = 0.2f) else StadiumMidnight)
                                .border(1.dp, if (sel) PlayerCyan else PanelBorder)
                                .clickable { onSelectPlayer(p) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (p.isStarter) "XI" else "SUB",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (p.isStarter) ArcadeEmerald else TextMuted
                                )
                                Text("${p.position.label} ${p.name}", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            }
                            Text("OVR ${p.ovr} • ${p.age}y", style = MaterialTheme.typography.bodyMedium, color = ChampionshipGold)
                        }
                    }
                }
            }
        }

        // Right: Selected Player Attributes, Contract, Training & Sale
        PixelPanel(modifier = Modifier.weight(0.9f).fillMaxHeight(), borderColor = ChampionshipGold) {
            if (activePlayer != null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(activePlayer.name, style = MaterialTheme.typography.titleLarge, color = ChampionshipGold)
                            Text(
                                "${activePlayer.position.label} • Age ${activePlayer.age} • Contract ${activePlayer.contractYears}y • $${activePlayer.weeklyWage / 1000}K/wk",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                        Text("OVR ${activePlayer.ovr}", style = MaterialTheme.typography.headlineMedium, color = ArcadeEmerald)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            PixelStatBar("PACE", activePlayer.pace)
                            PixelStatBar("SHOOT", activePlayer.shooting, barColor = ChampionshipGold)
                            PixelStatBar("PASS", activePlayer.passing, barColor = PlayerCyan)
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            PixelStatBar("DRIBBLE", activePlayer.dribbling)
                            PixelStatBar("DEFEND", activePlayer.defending, barColor = PlayerCyan)
                            PixelStatBar("STAMINA", activePlayer.staminaMax, barColor = ChampionshipGold)
                        }
                    }
                    Text(
                        "Season Stats: ${activePlayer.goals} Goals • ${activePlayer.assists} Assists • ${activePlayer.appearances} Apps",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        "TRAIN (+2 STAT, ${career.trainingPoints} PTS LEFT):",
                        style = MaterialTheme.typography.labelSmall,
                        color = ArcadeEmerald
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Pace", "Shooting", "Passing", "Dribbling").forEach { stat ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(TacticalSurfaceVariant)
                                    .border(1.dp, ArcadeEmerald)
                                    .clickable { onTrainPlayer(activePlayer.id, stat) }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(stat.take(4).uppercase(), style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PixelButton(
                            text = if (activePlayer.isStarter) "TO BENCH" else "TO XI",
                            onClick = { onToggleStarter(activePlayer.id) },
                            containerColor = PlayerCyan,
                            modifier = Modifier.weight(1f)
                        )
                        PixelButton(
                            text = "RENEW",
                            onClick = { onRenewContract(activePlayer.id) },
                            containerColor = ChampionshipGold,
                            modifier = Modifier.weight(1f)
                        )
                        PixelButton(
                            text = "SELL",
                            onClick = { onSellPlayer(activePlayer.id) },
                            containerColor = FoulCrimson,
                            contentColor = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransferMarketTab(
    myClub: ClubEntity?,
    clubs: List<ClubEntity>,
    marketPlayers: List<PlayerEntity>,
    onBuyPlayer: (Int) -> Unit
) {
    var posFilter by remember { mutableStateOf<Position?>(null) }
    val filtered = remember(marketPlayers, posFilter) {
        if (posFilter == null) marketPlayers.take(40)
        else marketPlayers.filter { it.position == posFilter }.take(40)
    }

    PixelPanel(modifier = Modifier.fillMaxSize(), borderColor = PlayerCyan) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val opts = listOf<Position?>(null, Position.GK, Position.DEF, Position.MID, Position.FWD)
                    opts.forEach { pos ->
                        val sel = posFilter == pos
                        Box(
                            modifier = Modifier
                                .background(if (sel) ChampionshipGold else StadiumMidnight)
                                .border(1.dp, PanelBorder)
                                .clickable { posFilter = pos }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(pos?.label ?: "ALL", style = MaterialTheme.typography.labelSmall, color = if (sel) StadiumMidnight else TextPrimary)
                        }
                    }
                }
                Text("BUDGET: $${(myClub?.budget ?: 0L) / 1_000_000}M", style = MaterialTheme.typography.titleMedium, color = ArcadeEmerald)
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(filtered) { p ->
                    val clubCode = clubs.firstOrNull { it.id == p.clubId }?.shortCode ?: "CLB"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StadiumMidnight)
                            .border(1.dp, PanelBorder)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${p.position.label} ${p.name} ($clubCode)", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            Text("OVR ${p.ovr} • Age ${p.age} • Wage $${p.weeklyWage / 1000}K", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("$${p.marketValue / 1_000_000}M", style = MaterialTheme.typography.titleMedium, color = ChampionshipGold)
                            PixelButton(
                                text = "SIGN",
                                onClick = { onBuyPlayer(p.id) },
                                containerColor = ArcadeEmerald
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsAndTrophiesTab(
    topScorers: List<PlayerEntity>,
    topAssists: List<PlayerEntity>,
    trophies: List<TrophyEntity>,
    clubs: List<ClubEntity>
) {
    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PixelPanel(modifier = Modifier.weight(1f).fillMaxHeight(), borderColor = ChampionshipGold) {
            Column {
                Text("GOLDEN BOOT (GOALS)", style = MaterialTheme.typography.titleMedium, color = ChampionshipGold)
                Spacer(Modifier.height(6.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(topScorers.take(10)) { p ->
                        val c = clubs.firstOrNull { it.id == p.clubId }?.shortCode ?: ""
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${p.name} ($c)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text("${p.goals} G", style = MaterialTheme.typography.bodyLarge, color = ChampionshipGold)
                        }
                    }
                }
            }
        }

        PixelPanel(modifier = Modifier.weight(1f).fillMaxHeight(), borderColor = PlayerCyan) {
            Column {
                Text("PLAYMAKERS (ASSISTS)", style = MaterialTheme.typography.titleMedium, color = PlayerCyan)
                Spacer(Modifier.height(6.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(topAssists.take(10)) { p ->
                        val c = clubs.firstOrNull { it.id == p.clubId }?.shortCode ?: ""
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${p.name} ($c)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text("${p.assists} A", style = MaterialTheme.typography.bodyLarge, color = PlayerCyan)
                        }
                    }
                }
            }
        }

        PixelPanel(modifier = Modifier.weight(1f).fillMaxHeight(), borderColor = ArcadeEmerald) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = "Trophy Room", tint = ChampionshipGold, modifier = Modifier.size(18.dp))
                    Text("TROPHY CABINET", style = MaterialTheme.typography.titleMedium, color = ArcadeEmerald)
                }
                Spacer(Modifier.height(6.dp))
                if (trophies.isEmpty()) {
                    Text("Complete a season to earn League, Golden Boot & Playmaker trophies!", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(trophies) { t ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(StadiumMidnight)
                                    .border(1.dp, ChampionshipGold)
                                    .padding(6.dp)
                            ) {
                                Text("S${t.seasonNumber} • ${t.title}", style = MaterialTheme.typography.labelSmall, color = ChampionshipGold)
                                Text(t.clubName, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                                Text(t.subtitle, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}
