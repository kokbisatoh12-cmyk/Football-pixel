package com.example

import com.example.audio.RetroAudioEngine
import com.example.data.Formation
import com.example.data.MatchDifficulty
import com.example.data.MatchLaunchConfig
import com.example.data.Position
import com.example.data.SeedDataGenerator
import com.example.data.TrainingDrillType
import com.example.game.engine.ChasingState
import com.example.game.engine.ControlInput
import com.example.game.engine.DribblingState
import com.example.game.engine.IdleState
import com.example.game.engine.MatchEngine
import com.example.game.engine.MatchPhase
import com.example.game.engine.PitchConstants
import com.example.game.engine.PlayerFsmContext
import com.example.game.engine.PlayerStateMachine
import com.example.game.engine.PositioningState
import com.example.game.engine.ShootingState
import com.example.game.engine.SimBall
import com.example.game.engine.SimPlayer
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchEngineAndCareerTest {

    @Test
    fun playerStateMachine_transitionsAcrossChasingPositioningDribblingAndShooting() {
        val transitions = mutableListOf<String>()
        val fsm = PlayerStateMachine(initialState = IdleState) { from, to ->
            transitions.add("${from.stateName}->${to.stateName}")
        }

        val striker = SimPlayer(
            simId = 10, entityId = 10, name = "Test Striker", shirtNumber = 9,
            isHome = true, isGk = false, slotIndex = 9, role = Position.FWD,
            ovr = 82, pace = 84, shooting = 85, passing = 78, dribbling = 83,
            defending = 45, staminaMax = 90, skinToneIndex = 0, hairStyleIndex = 0,
            primaryColor = 0xFFD92626, secondaryColor = 0xFFFFFFFF, shortsColor = 0xFFFFFFFF,
            x = PitchConstants.CENTER_X, y = PitchConstants.CENTER_Y
        )

        fun ctx(
            p: SimPlayer = striker,
            ball: SimBall = SimBall(),
            isPresser: Boolean = false,
            hasBall: Boolean = false,
            teamHasBall: Boolean = false
        ) = PlayerFsmContext(
            player = p,
            allPlayers = listOf(p),
            ball = ball,
            formation = Formation.F_442,
            difficulty = MatchDifficulty.PRO,
            isPresser = isPresser,
            hasBall = hasBall,
            teamHasBall = teamHasBall,
            rng = Random(1)
        )

        // 1. Idle -> Positioning when off-ball and not pressing
        fsm.update(ctx(), 0.05f)
        assertEquals(PositioningState, fsm.currentState)

        // 2. Positioning -> Chasing when designated presser while defending
        fsm.update(ctx(isPresser = true, teamHasBall = false), 0.05f)
        assertEquals(ChasingState, fsm.currentState)

        // 3. Chasing -> Dribbling when winning possession outside shooting box
        fsm.update(ctx(hasBall = true, teamHasBall = true), 0.05f)
        assertEquals(DribblingState, fsm.currentState)

        // 4. Dribbling -> Shooting when inside goal range
        val closeStriker = striker.copy(x = PitchConstants.PITCH_W - 140f, y = PitchConstants.CENTER_Y)
        val shootDecision = fsm.update(
            ctx(
                p = closeStriker,
                ball = SimBall(x = closeStriker.x + 10f, y = closeStriker.y, ownerSimId = closeStriker.simId),
                hasBall = true,
                teamHasBall = true
            ),
            0.05f
        )
        assertEquals(ShootingState, fsm.currentState)
        assertTrue(shootDecision.wantShoot)
        assertEquals(
            listOf("Idle->Positioning", "Positioning->Chasing", "Chasing->Dribbling", "Dribbling->Shooting"),
            transitions
        )
    }

    @Test
    fun seedData_builds12Clubs_15PlayersEach_and66RoundRobinFixtures() {
        val clubs = SeedDataGenerator.buildInitialClubs().mapIndexed { i, c -> c.copy(id = i + 1) }
        assertEquals(12, clubs.size)

        val allPlayers = clubs.flatMapIndexed { idx, club ->
            SeedDataGenerator.buildRosterForClub(club, idx)
        }
        assertEquals(180, allPlayers.size)

        val fixtures = SeedDataGenerator.generateRoundRobinFixtures(1, clubs.map { it.id })
        assertEquals(66, fixtures.size)
        clubs.forEach { c ->
            val count = fixtures.count { it.homeClubId == c.id || it.awayClubId == c.id }
            assertEquals(11, count)
        }
    }

    @Test
    fun matchEngine_runs11v11Simulation_andHandlesInputsAndSubs() {
        val clubs = SeedDataGenerator.buildInitialClubs().mapIndexed { i, c -> c.copy(id = i + 1) }
        val home = clubs[0]
        val away = clubs[1]
        val homeRoster = SeedDataGenerator.buildRosterForClub(home, 0).mapIndexed { i, p -> p.copy(id = 100 + i) }
        val awayRoster = SeedDataGenerator.buildRosterForClub(away, 1).mapIndexed { i, p -> p.copy(id = 200 + i) }

        val audio = RetroAudioEngine().apply { enabled = false }
        val engine = MatchEngine(
            config = MatchLaunchConfig(homeClubId = home.id, awayClubId = away.id, durationMinutes = 2),
            homeClub = home,
            awayClub = away,
            homeRoster = homeRoster,
            awayRoster = awayRoster,
            audio = audio
        )

        val initial = engine.snapshotFlow.value
        assertEquals(22, initial.players.size)
        assertEquals(4, initial.homeBench.size)

        // Step past kickoff countdown and simulate 3 seconds of 11v11 AI + user input
        repeat(90) {
            engine.updateInputs(
                ControlInput(moveX = 1f, moveY = 0.2f, sprint = true, passJustPressed = it == 80),
                ControlInput()
            )
            engine.step(1f / 30f)
        }

        val mid = engine.snapshotFlow.value
        assertEquals(MatchPhase.PLAYING, mid.phase)
        assertTrue(mid.matchSecondsElapsed > 0f)

        // Test live substitution
        val outSimId = mid.players.first { it.isHome && !it.isGk }.simId
        val subIn = mid.homeBench.first()
        assertTrue(engine.substitutePlayer(true, outSimId, subIn))
        assertEquals(3, engine.snapshotFlow.value.homeBench.size)
    }

    @Test
    fun matchEngine_trainingDrillMode_initializesTargets() {
        val clubs = SeedDataGenerator.buildInitialClubs().mapIndexed { i, c -> c.copy(id = i + 1) }
        val homeRoster = SeedDataGenerator.buildRosterForClub(clubs[0], 0).mapIndexed { i, p -> p.copy(id = 100 + i) }
        val awayRoster = SeedDataGenerator.buildRosterForClub(clubs[1], 1).mapIndexed { i, p -> p.copy(id = 200 + i) }
        val audio = RetroAudioEngine().apply { enabled = false }

        val engine = MatchEngine(
            config = MatchLaunchConfig(homeClubId = clubs[0].id, awayClubId = clubs[1].id),
            homeClub = clubs[0],
            awayClub = clubs[1],
            homeRoster = homeRoster,
            awayRoster = awayRoster,
            audio = audio,
            trainingDrill = TrainingDrillType.DRIBBLING
        )

        val snap = engine.snapshotFlow.value
        assertTrue(snap.isTrainingMode)
        assertEquals(6, snap.trainingTargets.size)
    }
}
