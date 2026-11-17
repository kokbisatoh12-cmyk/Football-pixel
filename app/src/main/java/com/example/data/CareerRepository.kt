package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max
import kotlin.random.Random

class CareerRepository(private val db: AppDatabase) {
    private val clubDao = db.clubDao()
    private val playerDao = db.playerDao()
    private val careerDao = db.careerDao()
    private val seedMutex = Mutex()

    val standingsFlow: Flow<List<ClubEntity>> = clubDao.observeStandings()
    val allClubsFlow: Flow<List<ClubEntity>> = clubDao.observeAllClubs()
    val activeCareerFlow: Flow<CareerSaveEntity?> = careerDao.observeActiveCareer()
    val topScorersFlow: Flow<List<PlayerEntity>> = playerDao.observeTopScorers()
    val topAssistsFlow: Flow<List<PlayerEntity>> = playerDao.observeTopAssists()

    fun observePlayersForClub(clubId: Int): Flow<List<PlayerEntity>> =
        playerDao.observePlayersByClub(clubId)

    fun observeTransferMarket(excludeClubId: Int): Flow<List<PlayerEntity>> =
        playerDao.observeTransferMarketPlayers(excludeClubId)

    fun observeFixtures(careerId: Int): Flow<List<FixtureEntity>> =
        careerDao.observeFixtures(careerId)

    fun observeTrophies(careerId: Int): Flow<List<TrophyEntity>> =
        careerDao.observeTrophies(careerId)

    suspend fun ensureSeeded() {
        seedMutex.withLock {
            if (clubDao.getAllClubsSync().isNotEmpty()) return
            val initialClubs = SeedDataGenerator.buildInitialClubs()
            val insertedIds = clubDao.insertAll(initialClubs)
            val savedClubs = clubDao.getAllClubsSync()
            val allPlayers = mutableListOf<PlayerEntity>()
            savedClubs.forEachIndexed { idx, club ->
                val roster = SeedDataGenerator.buildRosterForClub(club, idx)
                allPlayers.addAll(roster)
                val wageSum = roster.sumOf { it.weeklyWage }
                clubDao.updateClub(club.copy(wageBill = wageSum))
            }
            if (insertedIds.isNotEmpty()) {
                playerDao.insertAll(allPlayers)
            }
        }
    }

    suspend fun getAllClubsSync(): List<ClubEntity> {
        ensureSeeded()
        return clubDao.getAllClubsSync()
    }

    suspend fun getClubWithPlayers(clubId: Int): Pair<ClubEntity?, List<PlayerEntity>> {
        ensureSeeded()
        return clubDao.getClubById(clubId) to playerDao.getPlayersByClubSync(clubId)
    }

    suspend fun startNewCareer(
        role: CareerRole,
        selectedClubId: Int,
        customPlayerName: String = "Bintang Garuda",
        customPosition: Position = Position.FWD
    ): CareerSaveEntity {
        ensureSeeded()
        // Reset league stats and players cleanly for fresh career
        clubDao.clearAll()
        playerDao.clearAll()
        careerDao.clearFixtures()
        careerDao.clearCareers()
        careerDao.clearTrophies()

        val initialClubs = SeedDataGenerator.buildInitialClubs()
        clubDao.insertAll(initialClubs)
        val freshClubs = clubDao.getAllClubsSync()
        val chosenIndex = (selectedClubId - 1).coerceIn(0, freshClubs.lastIndex)
        val targetClub = freshClubs[chosenIndex]

        val allPlayers = mutableListOf<PlayerEntity>()
        freshClubs.forEachIndexed { idx, club ->
            val isMine = club.id == targetClub.id
            val roster = SeedDataGenerator.buildRosterForClub(club, idx)
            allPlayers.addAll(roster)
            clubDao.updateClub(
                club.copy(
                    isPlayerClub = isMine,
                    wageBill = roster.sumOf { it.weeklyWage }
                )
            )
        }
        playerDao.insertAll(allPlayers)

        var userPlayerId: Int? = null
        if (role == CareerRole.PLAYER) {
            val pro = PlayerEntity(
                clubId = targetClub.id,
                name = customPlayerName.ifBlank { "Bintang Garuda" },
                age = 19,
                positionName = customPosition.name,
                ovr = 78,
                pace = 84,
                shooting = if (customPosition == Position.FWD) 83 else 75,
                passing = if (customPosition == Position.MID) 84 else 76,
                dribbling = 82,
                defending = if (customPosition == Position.DEF) 81 else 58,
                staminaMax = 88,
                weeklyWage = 45_000L,
                contractYears = 3,
                marketValue = 8_500_000L,
                isUserCareerPro = true,
                isStarter = true
            )
            userPlayerId = playerDao.insert(pro).toInt()
        }

        val career = CareerSaveEntity(
            careerRoleName = role.name,
            clubId = targetClub.id,
            userPlayerId = userPlayerId,
            seasonNumber = 1,
            currentMatchday = 1,
            totalMatchdays = freshClubs.size - 1,
            clubMorale = 78,
            trainingPoints = 3,
            isActive = true
        )
        val careerId = careerDao.insertCareer(career).toInt()
        val fixtures = SeedDataGenerator.generateRoundRobinFixtures(
            careerId = careerId,
            clubIds = freshClubs.map { it.id }
        )
        careerDao.insertFixtures(fixtures)
        return career.copy(id = careerId)
    }

    suspend fun recordMatchAndAdvanceMatchday(
        fixtureId: Int,
        homeGoals: Int,
        awayGoals: Int,
        scorerPlayerIds: List<Int>,
        assistPlayerIds: List<Int>
    ) {
        val career = careerDao.getActiveCareerSync() ?: return
        val fixture = careerDao.getFixtureById(fixtureId) ?: return
        if (fixture.isPlayed) return

        // Update user match fixture & players
        applyFixtureOutcome(fixture, homeGoals, awayGoals, scorerPlayerIds, assistPlayerIds)

        // Simulate remaining AI fixtures in the same matchday
        val mdFixtures = careerDao.getFixturesByMatchday(career.id, fixture.matchday)
        val rng = Random(System.currentTimeMillis())
        for (other in mdFixtures) {
            if (other.id != fixture.id && !other.isPlayed) {
                val hClub = clubDao.getClubById(other.homeClubId)
                val aClub = clubDao.getClubById(other.awayClubId)
                val hDiff = ((hClub?.teamRating ?: 78) - (aClub?.teamRating ?: 78)) / 10f
                val hg = (rng.nextInt(0, 4) + if (hDiff > 0.3f) 1 else 0).coerceIn(0, 5)
                val ag = (rng.nextInt(0, 3) + if (hDiff < -0.3f) 1 else 0).coerceIn(0, 5)
                val hRoster = playerDao.getPlayersByClubSync(other.homeClubId).filter { it.isStarter }
                val aRoster = playerDao.getPlayersByClubSync(other.awayClubId).filter { it.isStarter }
                val scorers = mutableListOf<Int>()
                val assisters = mutableListOf<Int>()
                repeat(hg) {
                    pickAttacker(hRoster, rng)?.let { scorers.add(it.id) }
                    pickMidfielder(hRoster, rng)?.let { assisters.add(it.id) }
                }
                repeat(ag) {
                    pickAttacker(aRoster, rng)?.let { scorers.add(it.id) }
                    pickMidfielder(aRoster, rng)?.let { assisters.add(it.id) }
                }
                applyFixtureOutcome(other, hg, ag, scorers, assisters)
            }
        }

        // Advance matchday or finish season
        val nextMatchday = career.currentMatchday + 1
        if (nextMatchday > career.totalMatchdays) {
            concludeSeasonAndAwardTrophies(career)
        } else {
            careerDao.updateCareer(
                career.copy(
                    currentMatchday = nextMatchday,
                    trainingPoints = (career.trainingPoints + 2).coerceAtMost(8)
                )
            )
        }
    }

    private suspend fun applyFixtureOutcome(
        fixture: FixtureEntity,
        homeGoals: Int,
        awayGoals: Int,
        scorerIds: List<Int>,
        assistIds: List<Int>
    ) {
        val homeClub = clubDao.getClubById(fixture.homeClubId) ?: return
        val awayClub = clubDao.getClubById(fixture.awayClubId) ?: return

        val homePts = when {
            homeGoals > awayGoals -> 3
            homeGoals == awayGoals -> 1
            else -> 0
        }
        val awayPts = when {
            awayGoals > homeGoals -> 3
            homeGoals == awayGoals -> 1
            else -> 0
        }

        val prizeBonusHome = if (homePts == 3) 450_000L else if (homePts == 1) 200_000L else 100_000L
        val prizeBonusAway = if (awayPts == 3) 450_000L else if (awayPts == 1) 200_000L else 100_000L

        clubDao.updateClub(
            homeClub.copy(
                played = homeClub.played + 1,
                won = homeClub.won + if (homePts == 3) 1 else 0,
                drawn = homeClub.drawn + if (homePts == 1) 1 else 0,
                lost = homeClub.lost + if (homePts == 0) 1 else 0,
                goalsFor = homeClub.goalsFor + homeGoals,
                goalsAgainst = homeClub.goalsAgainst + awayGoals,
                points = homeClub.points + homePts,
                budget = max(0L, homeClub.budget + prizeBonusHome - homeClub.wageBill / 4)
            )
        )
        clubDao.updateClub(
            awayClub.copy(
                played = awayClub.played + 1,
                won = awayClub.won + if (awayPts == 3) 1 else 0,
                drawn = awayClub.drawn + if (awayPts == 1) 1 else 0,
                lost = awayClub.lost + if (awayPts == 0) 1 else 0,
                goalsFor = awayClub.goalsFor + awayGoals,
                goalsAgainst = awayClub.goalsAgainst + homeGoals,
                points = awayClub.points + awayPts,
                budget = max(0L, awayClub.budget + prizeBonusAway - awayClub.wageBill / 4)
            )
        )

        val homePlayers = playerDao.getPlayersByClubSync(homeClub.id)
        val awayPlayers = playerDao.getPlayersByClubSync(awayClub.id)
        val allMatchPlayers = (homePlayers + awayPlayers).map { p ->
            val g = scorerIds.count { it == p.id }
            val a = assistIds.count { it == p.id }
            val app = if (p.isStarter) 1 else 0
            val xpGain = app * 15 + g * 25 + a * 18
            val newXp = p.xp + xpGain
            val levelUp = newXp >= 100
            p.copy(
                goals = p.goals + g,
                assists = p.assists + a,
                appearances = p.appearances + app,
                xp = if (levelUp) newXp - 100 else newXp,
                ovr = if (levelUp) (p.ovr + 1).coerceAtMost(96) else p.ovr,
                pace = if (levelUp) (p.pace + 1).coerceAtMost(97) else p.pace,
                shooting = if (levelUp) (p.shooting + 1).coerceAtMost(97) else p.shooting,
                passing = if (levelUp) (p.passing + 1).coerceAtMost(97) else p.passing
            )
        }
        playerDao.updatePlayers(allMatchPlayers)

        careerDao.updateFixture(
            fixture.copy(
                homeGoals = homeGoals,
                awayGoals = awayGoals,
                isPlayed = true,
                scorersSummary = "${homeClub.shortCode} $homeGoals - $awayGoals ${awayClub.shortCode}"
            )
        )
    }

    private suspend fun concludeSeasonAndAwardTrophies(career: CareerSaveEntity) {
        val allClubs = clubDao.getAllClubsSync().sortedWith(
            compareByDescending<ClubEntity> { it.points }
                .thenByDescending { it.goalDifference }
                .thenByDescending { it.goalsFor }
        )
        val champion = allClubs.firstOrNull()
        if (champion != null) {
            careerDao.insertTrophy(
                TrophyEntity(
                    careerId = career.id,
                    seasonNumber = career.seasonNumber,
                    title = "Pixel League Champion",
                    clubName = champion.name,
                    subtitle = "${champion.points} PTS • GD ${champion.goalDifference}"
                )
            )
        }
        val topScorer = playerDao.getAllPlayersSync().maxByOrNull { it.goals }
        if (topScorer != null && topScorer.goals > 0) {
            val scorerClub = clubDao.getClubById(topScorer.clubId)?.name ?: "League"
            careerDao.insertTrophy(
                TrophyEntity(
                    careerId = career.id,
                    seasonNumber = career.seasonNumber,
                    title = "Golden Boot Winner",
                    clubName = topScorer.name,
                    subtitle = "${topScorer.goals} Goals ($scorerClub)"
                )
            )
        }
        val topAssister = playerDao.getAllPlayersSync().maxByOrNull { it.assists }
        if (topAssister != null && topAssister.assists > 0) {
            careerDao.insertTrophy(
                TrophyEntity(
                    careerId = career.id,
                    seasonNumber = career.seasonNumber,
                    title = "Playmaker Trophy",
                    clubName = topAssister.name,
                    subtitle = "${topAssister.assists} Assists"
                )
            )
        }

        // Start new season cleanly
        allClubs.forEach { c ->
            clubDao.updateClub(
                c.copy(
                    played = 0, won = 0, drawn = 0, lost = 0,
                    goalsFor = 0, goalsAgainst = 0, points = 0,
                    budget = c.budget + 12_000_000L
                )
            )
        }
        val agedPlayers = playerDao.getAllPlayersSync().map { p ->
            p.copy(
                age = p.age + 1,
                contractYears = (p.contractYears - 1).coerceAtLeast(1),
                goals = 0,
                assists = 0,
                appearances = 0,
                yellowCards = 0
            )
        }
        playerDao.updatePlayers(agedPlayers)

        val nextSeason = career.seasonNumber + 1
        careerDao.updateCareer(
            career.copy(
                seasonNumber = nextSeason,
                currentMatchday = 1,
                trainingPoints = 5
            )
        )
        careerDao.clearFixtures()
        val nextFixtures = SeedDataGenerator.generateRoundRobinFixtures(
            careerId = career.id,
            clubIds = allClubs.map { it.id }
        )
        careerDao.insertFixtures(nextFixtures)
    }

    suspend fun buyPlayer(playerId: Int, buyerClubId: Int): Result<String> {
        val buyer = clubDao.getClubById(buyerClubId) ?: return Result.failure(Exception("Club not found"))
        val target = playerDao.getPlayerById(playerId) ?: return Result.failure(Exception("Player not found"))
        if (target.clubId == buyerClubId) return Result.failure(Exception("Already in your squad"))
        if (buyer.budget < target.marketValue) {
            return Result.failure(Exception("Insufficient transfer budget"))
        }
        val seller = clubDao.getClubById(target.clubId)

        clubDao.updateClub(
            buyer.copy(
                budget = buyer.budget - target.marketValue,
                wageBill = buyer.wageBill + target.weeklyWage
            )
        )
        if (seller != null) {
            clubDao.updateClub(
                seller.copy(
                    budget = seller.budget + target.marketValue,
                    wageBill = max(0L, seller.wageBill - target.weeklyWage)
                )
            )
        }
        playerDao.updatePlayer(
            target.copy(
                clubId = buyerClubId,
                contractYears = 3,
                isStarter = false
            )
        )
        return Result.success("Signed ${target.name}!")
    }

    suspend fun sellPlayer(playerId: Int, sellerClubId: Int): Result<String> {
        val seller = clubDao.getClubById(sellerClubId) ?: return Result.failure(Exception("Club not found"))
        val roster = playerDao.getPlayersByClubSync(sellerClubId)
        if (roster.size <= 12) return Result.failure(Exception("Minimum 12 players required in squad"))
        val player = roster.firstOrNull { it.id == playerId } ?: return Result.failure(Exception("Player not found"))
        val buyer = clubDao.getAllClubsSync().firstOrNull { it.id != sellerClubId } ?: return Result.failure(Exception("No buyer available"))

        clubDao.updateClub(
            seller.copy(
                budget = seller.budget + player.marketValue,
                wageBill = max(0L, seller.wageBill - player.weeklyWage)
            )
        )
        playerDao.updatePlayer(
            player.copy(
                clubId = buyer.id,
                isStarter = false
            )
        )
        return Result.success("Sold ${player.name} for $${player.marketValue / 1_000_000}M")
    }

    suspend fun renewContract(playerId: Int): Result<String> {
        val player = playerDao.getPlayerById(playerId) ?: return Result.failure(Exception("Player not found"))
        val club = clubDao.getClubById(player.clubId) ?: return Result.failure(Exception("Club not found"))
        val signingFee = player.weeklyWage * 6
        if (club.budget < signingFee) return Result.failure(Exception("Budget too low for renewal bonus"))
        val newWage = (player.weeklyWage * 1.12).toLong()
        clubDao.updateClub(
            club.copy(
                budget = club.budget - signingFee,
                wageBill = club.wageBill + (newWage - player.weeklyWage)
            )
        )
        playerDao.updatePlayer(
            player.copy(
                contractYears = (player.contractYears + 2).coerceAtMost(5),
                weeklyWage = newWage
            )
        )
        return Result.success("Renewed ${player.name} (+2 yrs)")
    }

    suspend fun trainPlayerAttribute(playerId: Int, focusStat: String): Result<String> {
        val career = careerDao.getActiveCareerSync() ?: return Result.failure(Exception("No active career"))
        if (career.trainingPoints <= 0) return Result.failure(Exception("No training points left this week"))
        val player = playerDao.getPlayerById(playerId) ?: return Result.failure(Exception("Player not found"))

        val updated = when (focusStat) {
            "Pace" -> player.copy(pace = (player.pace + 2).coerceAtMost(98))
            "Shooting" -> player.copy(shooting = (player.shooting + 2).coerceAtMost(98))
            "Passing" -> player.copy(passing = (player.passing + 2).coerceAtMost(98))
            "Dribbling" -> player.copy(dribbling = (player.dribbling + 2).coerceAtMost(98))
            else -> player.copy(defending = (player.defending + 2).coerceAtMost(98))
        }
        val newOvr = ((updated.pace + updated.shooting + updated.passing + updated.dribbling + updated.defending) / 5)
            .coerceAtLeast(player.ovr)
            .coerceAtMost(96)

        playerDao.updatePlayer(updated.copy(ovr = newOvr))
        careerDao.updateCareer(career.copy(trainingPoints = career.trainingPoints - 1))
        return Result.success("${player.name} $focusStat boosted!")
    }

    suspend fun toggleStarter(playerId: Int): Result<String> {
        val p = playerDao.getPlayerById(playerId) ?: return Result.failure(Exception("Not found"))
        val roster = playerDao.getPlayersByClubSync(p.clubId)
        val starters = roster.count { it.isStarter }
        if (!p.isStarter && starters >= 11) {
            return Result.failure(Exception("Already 11 starters. Move someone to bench first."))
        }
        if (p.isStarter && starters <= 11 && roster.size <= 11) {
            return Result.failure(Exception("Need 11 starters"))
        }
        playerDao.updatePlayer(p.copy(isStarter = !p.isStarter))
        return Result.success(if (!p.isStarter) "${p.name} moved to Starting XI" else "${p.name} moved to Bench")
    }

    suspend fun updateClubFormation(clubId: Int, formation: Formation) {
        val club = clubDao.getClubById(clubId) ?: return
        clubDao.updateClub(club.copy(formationName = formation.name))
    }

    private fun pickAttacker(roster: List<PlayerEntity>, rng: Random): PlayerEntity? {
        val attackers = roster.filter { it.position == Position.FWD || it.position == Position.MID }
        return attackers.randomOrNull(rng) ?: roster.randomOrNull(rng)
    }

    private fun pickMidfielder(roster: List<PlayerEntity>, rng: Random): PlayerEntity? {
        val mids = roster.filter { it.position == Position.MID || it.position == Position.FWD }
        return mids.randomOrNull(rng) ?: roster.randomOrNull(rng)
    }
}
