package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.RetroAudioEngine
import com.example.data.AppDatabase
import com.example.data.AppSettings
import com.example.data.CareerRepository
import com.example.data.CareerRole
import com.example.data.CareerSaveEntity
import com.example.data.ClubEntity
import com.example.data.FixtureEntity
import com.example.data.Formation
import com.example.data.MatchLaunchConfig
import com.example.data.PlayerEntity
import com.example.data.Position
import com.example.data.SettingsDataStore
import com.example.data.TrainingDrillType
import com.example.data.TrophyEntity
import com.example.game.engine.MatchEngine
import com.example.game.engine.MatchPhase
import com.example.game.engine.MatchSnapshot
import com.example.game.input.KeyboardGamepadBridge
import com.example.game.input.MutableControlState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

data class TournamentMatchNode(
    val id: Int,
    val roundLabel: String, // "QF", "SF", "FINAL"
    val homeClub: ClubEntity?,
    val awayClub: ClubEntity?,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val winnerClub: ClubEntity? = null
)

data class TournamentState(
    val userClub: ClubEntity? = null,
    val matches: List<TournamentMatchNode> = emptyList(),
    val champion: ClubEntity? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repository = CareerRepository(db)
    private val settingsStore = SettingsDataStore(application)
    val audio = RetroAudioEngine()

    val settings: StateFlow<AppSettings> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val allClubs: StateFlow<List<ClubEntity>> = repository.allClubsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val standings: StateFlow<List<ClubEntity>> = repository.standingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCareer: StateFlow<CareerSaveEntity?> = repository.activeCareerFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val careerClubPlayers: StateFlow<List<PlayerEntity>> = activeCareer
        .flatMapLatest { career ->
            if (career == null) flowOf(emptyList())
            else repository.observePlayersForClub(career.clubId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transferMarketPlayers: StateFlow<List<PlayerEntity>> = activeCareer
        .flatMapLatest { career ->
            if (career == null) flowOf(emptyList())
            else repository.observeTransferMarket(career.clubId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val careerFixtures: StateFlow<List<FixtureEntity>> = activeCareer
        .flatMapLatest { career ->
            if (career == null) flowOf(emptyList())
            else repository.observeFixtures(career.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val careerTrophies: StateFlow<List<TrophyEntity>> = activeCareer
        .flatMapLatest { career ->
            if (career == null) flowOf(emptyList())
            else repository.observeTrophies(career.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topScorers: StateFlow<List<PlayerEntity>> = repository.topScorersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topAssists: StateFlow<List<PlayerEntity>> = repository.topAssistsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _tournamentState = MutableStateFlow(TournamentState())
    val tournamentState: StateFlow<TournamentState> = _tournamentState.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    // Live match state
    private var matchEngine: MatchEngine? = null
    private var activeConfig: MatchLaunchConfig? = null
    private var matchLoopJob: Job? = null
    private var matchResultSaved = false

    private val _matchSnapshot = MutableStateFlow<MatchSnapshot?>(null)
    val matchSnapshot: StateFlow<MatchSnapshot?> = _matchSnapshot.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    val p1Controls = MutableControlState()
    val p2Controls = MutableControlState()
    val keyBridge = KeyboardGamepadBridge()

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
        viewModelScope.launch {
            settings.collect { s ->
                audio.enabled = s.sfxEnabled
                audio.volume = s.sfxVolume
            }
        }
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            settingsStore.updateSettings(transform)
        }
    }

    fun startNewCareer(
        role: CareerRole,
        clubId: Int,
        playerName: String,
        playerPos: Position
    ) {
        viewModelScope.launch {
            repository.startNewCareer(role, clubId, playerName, playerPos)
            audio.playWhistle(false)
            _statusBanner.value = "New ${role.label} career started!"
        }
    }

    fun buyPlayer(playerId: Int) {
        val career = activeCareer.value ?: return
        viewModelScope.launch {
            val res = repository.buyPlayer(playerId, career.clubId)
            _statusBanner.value = res.getOrElse { it.message ?: "Transfer failed" }
            if (res.isSuccess) audio.playGoalRoar()
        }
    }

    fun sellPlayer(playerId: Int) {
        val career = activeCareer.value ?: return
        viewModelScope.launch {
            val res = repository.sellPlayer(playerId, career.clubId)
            _statusBanner.value = res.getOrElse { it.message ?: "Sale failed" }
            if (res.isSuccess) audio.playMenuClick()
        }
    }

    fun renewContract(playerId: Int) {
        viewModelScope.launch {
            val res = repository.renewContract(playerId)
            _statusBanner.value = res.getOrElse { it.message ?: "Renewal failed" }
            if (res.isSuccess) audio.playMenuClick()
        }
    }

    fun trainPlayer(playerId: Int, stat: String) {
        viewModelScope.launch {
            val res = repository.trainPlayerAttribute(playerId, stat)
            _statusBanner.value = res.getOrElse { it.message ?: "Training failed" }
            if (res.isSuccess) audio.playMenuClick()
        }
    }

    fun toggleStarter(playerId: Int) {
        viewModelScope.launch {
            val res = repository.toggleStarter(playerId)
            _statusBanner.value = res.getOrElse { it.message ?: "Lineup update failed" }
        }
    }

    fun setClubFormation(clubId: Int, formation: Formation) {
        viewModelScope.launch {
            repository.updateClubFormation(clubId, formation)
            audio.playMenuClick()
        }
    }

    // --- Tournament Logic ---
    fun startTournament(userClubId: Int) {
        viewModelScope.launch {
            val clubs = repository.getAllClubsSync()
            val userClub = clubs.firstOrNull { it.id == userClubId } ?: clubs.first()
            val others = clubs.filter { it.id != userClub.id }.shuffled().take(7)
            val eight = (listOf(userClub) + others)
            val qfMatches = listOf(
                TournamentMatchNode(1, "QUARTERFINAL 1", eight[0], eight[1]),
                TournamentMatchNode(2, "QUARTERFINAL 2", eight[2], eight[3]),
                TournamentMatchNode(3, "QUARTERFINAL 3", eight[4], eight[5]),
                TournamentMatchNode(4, "QUARTERFINAL 4", eight[6], eight[7]),
                TournamentMatchNode(5, "SEMIFINAL 1", null, null),
                TournamentMatchNode(6, "SEMIFINAL 2", null, null),
                TournamentMatchNode(7, "FINAL", null, null)
            )
            _tournamentState.value = TournamentState(userClub = userClub, matches = qfMatches)
            audio.playWhistle(false)
        }
    }

    fun simulateAiTournamentMatches() {
        val state = _tournamentState.value
        val rng = Random(System.currentTimeMillis())
        val updated = state.matches.toMutableList()
        for (i in updated.indices) {
            val m = updated[i]
            if (m.winnerClub == null && m.homeClub != null && m.awayClub != null &&
                m.homeClub.id != state.userClub?.id && m.awayClub.id != state.userClub?.id
            ) {
                var hg = rng.nextInt(0, 4)
                var ag = rng.nextInt(0, 4)
                if (hg == ag) hg += 1
                val win = if (hg > ag) m.homeClub else m.awayClub
                updated[i] = m.copy(homeScore = hg, awayScore = ag, winnerClub = win)
            }
        }
        _tournamentState.value = advanceBracketWinners(state.copy(matches = updated))
    }

    private fun advanceBracketWinners(state: TournamentState): TournamentState {
        val list = state.matches.toMutableList()
        if (list.size < 7) return state
        // QF 1 & 2 -> SF 1 (index 4)
        val qf1 = list[0].winnerClub
        val qf2 = list[1].winnerClub
        if (qf1 != null && qf2 != null && list[4].homeClub == null) {
            list[4] = list[4].copy(homeClub = qf1, awayClub = qf2)
        }
        // QF 3 & 4 -> SF 2 (index 5)
        val qf3 = list[2].winnerClub
        val qf4 = list[3].winnerClub
        if (qf3 != null && qf4 != null && list[5].homeClub == null) {
            list[5] = list[5].copy(homeClub = qf3, awayClub = qf4)
        }
        // SF 1 & 2 -> FINAL (index 6)
        val sf1 = list[4].winnerClub
        val sf2 = list[5].winnerClub
        if (sf1 != null && sf2 != null && list[6].homeClub == null) {
            list[6] = list[6].copy(homeClub = sf1, awayClub = sf2)
        }
        val champ = list[6].winnerClub
        return state.copy(matches = list, champion = champ)
    }

    // --- Live Match Control ---
    fun launchMatch(config: MatchLaunchConfig, trainingDrill: TrainingDrillType? = null, onReady: () -> Unit) {
        viewModelScope.launch {
            val (hClub, hPlayers) = repository.getClubWithPlayers(config.homeClubId)
            val (aClub, aPlayers) = repository.getClubWithPlayers(config.awayClubId)
            if (hClub == null || aClub == null) return@launch

            matchLoopJob?.cancel()
            matchResultSaved = false
            _isPaused.value = false
            activeConfig = config

            val engine = MatchEngine(
                config = config,
                homeClub = hClub,
                awayClub = aClub,
                homeRoster = hPlayers,
                awayRoster = aPlayers,
                audio = audio,
                trainingDrill = trainingDrill
            )
            matchEngine = engine
            _matchSnapshot.value = engine.snapshotFlow.value

            onReady()
            startMatchLoop(engine)
        }
    }

    private fun startMatchLoop(engine: MatchEngine) {
        matchLoopJob = viewModelScope.launch(Dispatchers.Default) {
            var lastNs = System.nanoTime()
            while (isActive) {
                val now = System.nanoTime()
                val dt = ((now - lastNs) / 1_000_000_000f).coerceIn(0.005f, 0.04f)
                lastNs = now

                if (!_isPaused.value) {
                    val p1 = p1Controls.consumeSnapshot(
                        kbMoveX = keyBridge.p1MoveX(),
                        kbMoveY = keyBridge.p1MoveY(),
                        kbSprint = keyBridge.p1Sprint()
                    )
                    val p2 = p2Controls.consumeSnapshot(
                        kbMoveX = keyBridge.p2MoveX(),
                        kbMoveY = keyBridge.p2MoveY()
                    )
                    engine.updateInputs(p1, p2)
                    engine.step(dt)
                    val snap = engine.snapshotFlow.value
                    _matchSnapshot.value = snap

                    if (snap.phase == MatchPhase.FULLTIME && !matchResultSaved) {
                        matchResultSaved = true
                        onMatchCompleted(snap)
                    }
                }
                delay(16L)
            }
        }
    }

    private fun onMatchCompleted(snap: MatchSnapshot) {
        val cfg = activeConfig ?: return
        viewModelScope.launch {
            if (cfg.careerFixtureId != null) {
                repository.recordMatchAndAdvanceMatchday(
                    fixtureId = cfg.careerFixtureId,
                    homeGoals = snap.homeScore,
                    awayGoals = snap.awayScore,
                    scorerPlayerIds = snap.scorerEntityIds,
                    assistPlayerIds = snap.assistEntityIds
                )
            }
            if (cfg.tournamentMatchId != null) {
                val state = _tournamentState.value
                val updated = state.matches.map { m ->
                    if (m.id == cfg.tournamentMatchId) {
                        val winner = if (snap.homeScore >= snap.awayScore) m.homeClub else m.awayClub
                        m.copy(homeScore = snap.homeScore, awayScore = snap.awayScore, winnerClub = winner)
                    } else m
                }
                _tournamentState.value = advanceBracketWinners(state.copy(matches = updated))
                simulateAiTournamentMatches()
            }
            if (snap.isTrainingMode && snap.trainingScore > 0) {
                val career = activeCareer.value
                val targetId = career?.userPlayerId ?: careerClubPlayers.value.firstOrNull { !it.isStarter }?.id
                val stat = snap.trainingDrill?.focusStat ?: "Passing"
                if (career != null && targetId != null) {
                    repository.trainPlayerAttribute(targetId, stat)
                }
            }
        }
    }

    fun togglePause() {
        _isPaused.value = !_isPaused.value
        audio.playMenuClick()
    }

    fun resumeHalftime() {
        matchEngine?.resumeFromHalftime()
        _matchSnapshot.value = matchEngine?.snapshotFlow?.value
    }

    fun substitutePlayer(isHome: Boolean, outSimId: Int, inEntity: PlayerEntity) {
        if (matchEngine?.substitutePlayer(isHome, outSimId, inEntity) == true) {
            audio.playMenuClick()
            _matchSnapshot.value = matchEngine?.snapshotFlow?.value
        }
    }

    fun exitMatch() {
        matchLoopJob?.cancel()
        matchLoopJob = null
        matchEngine = null
        _matchSnapshot.value = null
        _isPaused.value = false
    }

    override fun onCleared() {
        super.onCleared()
        matchLoopJob?.cancel()
    }
}
