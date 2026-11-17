package com.example.game.engine

import com.example.audio.RetroAudioEngine
import com.example.data.ClubEntity
import com.example.data.FormationTemplates
import com.example.data.MatchLaunchConfig
import com.example.data.MatchSides
import com.example.data.PlayerAnim
import com.example.data.PlayerEntity
import com.example.data.PlayerFsmState
import com.example.data.Position
import com.example.data.SetPieceType
import com.example.data.TrainingDrillType
import com.example.data.WeatherType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class MatchEngine(
    private val config: MatchLaunchConfig,
    private val homeClub: ClubEntity,
    private val awayClub: ClubEntity,
    homeRoster: List<PlayerEntity>,
    awayRoster: List<PlayerEntity>,
    private val audio: RetroAudioEngine,
    private val trainingDrill: TrainingDrillType? = null
) {
    private val aiController = AiController(config.difficulty)
    private val rng = Random(System.currentTimeMillis())

    private var players = mutableListOf<SimPlayer>()
    private var homeBench = homeRoster.filter { !it.isStarter }.toMutableList()
    private var awayBench = awayRoster.filter { !it.isStarter }.toMutableList()
    private var ball = SimBall()

    private var homeScore = 0
    private var awayScore = 0
    private var half = 1
    private var matchSecondsElapsed = 0f
    private val totalMatchSeconds = (config.durationMinutes * 60f).coerceAtLeast(60f)
    private var phase = if (trainingDrill != null) MatchPhase.PLAYING else MatchPhase.KICKOFF
    private var setPieceType = if (trainingDrill != null) SetPieceType.NONE else SetPieceType.KICKOFF
    private var setPieceTeamHome = true
    private var phaseTimer = if (trainingDrill != null) 0f else 1.2f
    private var banner: MatchEventBanner? = null
    private var bannerTimer = 0f

    private var homePossessionTicks = 1
    private var awayPossessionTicks = 1
    private var homeShots = 0
    private var awayShots = 0
    private var homeFouls = 0
    private var awayFouls = 0
    private val scorerLogs = mutableListOf<String>()
    private val scorerEntityIds = mutableListOf<Int>()
    private val assistEntityIds = mutableListOf<Int>()

    private var p1ShootCharge = 0f
    private var p2ShootCharge = 0f
    private var camX = PitchConstants.CENTER_X
    private var camY = PitchConstants.CENTER_Y
    private var crowdExcitement = 0f
    private var tickCount = 0L

    // Training state
    private var trainingScore = 0
    private var trainingTimeLeft = 60f
    private var trainingTargets = mutableListOf<TrainingTarget>()

    private var p1Input = ControlInput()
    private var p2Input = ControlInput()

    private val _snapshotFlow: MutableStateFlow<MatchSnapshot>

    init {
        if (trainingDrill != null) {
            setupTrainingScenario(homeRoster, awayRoster, trainingDrill)
        } else {
            spawnFullTeams(homeRoster, awayRoster)
            resetToKickoff(homeKicksOff = true)
            showBanner("KICK OFF", "${homeClub.shortCode} vs ${awayClub.shortCode}", 0xFFF5B700)
        }
        _snapshotFlow = MutableStateFlow(buildSnapshot())
    }

    val snapshotFlow: StateFlow<MatchSnapshot> = _snapshotFlow.asStateFlow()

    fun updateInputs(p1: ControlInput, p2: ControlInput) {
        p1Input = p1
        p2Input = p2
    }

    fun resumeFromHalftime() {
        if (phase != MatchPhase.HALFTIME) return
        half = 2
        resetToKickoff(homeKicksOff = false)
        showBanner("2ND HALF", "Kick Off!", 0xFF00D084)
        audio.playWhistle(false)
        _snapshotFlow.value = buildSnapshot()
    }

    fun substitutePlayer(isHome: Boolean, outSimId: Int, inEntity: PlayerEntity): Boolean {
        val idx = players.indexOfFirst { it.simId == outSimId && it.isHome == isHome }
        if (idx == -1) return false
        val outP = players[idx]
        val bench = if (isHome) homeBench else awayBench
        val benchIdx = bench.indexOfFirst { it.id == inEntity.id }
        if (benchIdx == -1) return false

        bench.removeAt(benchIdx)
        val club = if (isHome) homeClub else awayClub
        players[idx] = outP.copy(
            entityId = inEntity.id,
            name = inEntity.name,
            ovr = inEntity.ovr,
            pace = inEntity.pace,
            shooting = inEntity.shooting,
            passing = inEntity.passing,
            dribbling = inEntity.dribbling,
            defending = inEntity.defending,
            staminaMax = inEntity.staminaMax,
            skinToneIndex = inEntity.skinToneIndex,
            hairStyleIndex = inEntity.hairStyleIndex,
            primaryColor = club.primaryColorHex,
            secondaryColor = club.secondaryColorHex,
            shortsColor = club.shortsColorHex,
            stamina = 100f,
            yellowCards = 0,
            sentOff = false
        )
        _snapshotFlow.value = buildSnapshot()
        return true
    }

    fun step(rawDt: Float) {
        val dt = (rawDt * config.gameSpeed).coerceIn(0.005f, 0.045f)
        tickCount++

        if (bannerTimer > 0f) {
            bannerTimer -= rawDt
            if (bannerTimer <= 0f) banner = null
        }
        crowdExcitement = (crowdExcitement - rawDt * 0.4f).coerceAtLeast(0f)

        if (trainingDrill != null) {
            stepTraining(dt)
            _snapshotFlow.value = buildSnapshot()
            return
        }

        if (phase == MatchPhase.HALFTIME || phase == MatchPhase.FULLTIME) {
            _snapshotFlow.value = buildSnapshot()
            return
        }

        // Countdown timers for kickoff / goal celebration / set piece
        if (phase == MatchPhase.KICKOFF || phase == MatchPhase.SET_PIECE_WAIT) {
            phaseTimer -= rawDt
            updatePlayersAnimationsOnly(dt)
            if (phaseTimer <= 0f) {
                phase = MatchPhase.PLAYING
                setPieceType = SetPieceType.NONE
                audio.playWhistle(false)
            }
            updateCamera(dt)
            _snapshotFlow.value = buildSnapshot()
            return
        }

        if (phase == MatchPhase.GOAL_CELEBRATION) {
            phaseTimer -= rawDt
            updatePlayersAnimationsOnly(dt)
            updateBallPhysics(dt, allowGoals = false)
            if (phaseTimer <= 0f) {
                val kickHome = !ball.lastTouchHome
                resetToKickoff(homeKicksOff = kickHome)
                showBanner("KICK OFF", if (kickHome) homeClub.shortCode else awayClub.shortCode, 0xFFF5B700)
            }
            updateCamera(dt)
            _snapshotFlow.value = buildSnapshot()
            return
        }

        // Active Match Clock
        matchSecondsElapsed += dt
        if (half == 1 && matchSecondsElapsed >= totalMatchSeconds * 0.5f) {
            matchSecondsElapsed = totalMatchSeconds * 0.5f
            phase = MatchPhase.HALFTIME
            audio.playWhistle(true)
            showBanner("HALF TIME", "${homeClub.shortCode} $homeScore - $awayScore ${awayClub.shortCode}", 0xFFF5B700)
            _snapshotFlow.value = buildSnapshot()
            return
        }
        if (half == 2 && matchSecondsElapsed >= totalMatchSeconds) {
            matchSecondsElapsed = totalMatchSeconds
            phase = MatchPhase.FULLTIME
            audio.playWhistle(true)
            showBanner("FULL TIME", "${homeClub.shortCode} $homeScore - $awayScore ${awayClub.shortCode}", 0xFF00D084)
            _snapshotFlow.value = buildSnapshot()
            return
        }

        // Possession stats
        val owner = ball.ownerSimId?.let { id -> players.firstOrNull { it.simId == id } }
        if (owner != null) {
            if (owner.isHome) homePossessionTicks++ else awayPossessionTicks++
        }

        // Ensure P1 (and P2 if local multi) control the right player
        updateActivePlayerAssignments(owner)

        // Identify closest defender on each team for pressing
        val homePresserId = findClosestPresser(isHome = true)
        val awayPresserId = findClosestPresser(isHome = false)

        // Update all 22 players
        for (i in players.indices) {
            val p = players[i]
            if (p.sentOff) continue

            val decision = when {
                p.isControlledByP1 -> buildHumanDecision(p, p1Input, isP1 = true, dt = dt)
                p.isControlledByP2 -> buildHumanDecision(p, p2Input, isP1 = false, dt = dt)
                else -> {
                    val formation = if (p.isHome) config.homeFormation else config.awayFormation
                    val teamHasBall = owner?.isHome == p.isHome
                    val isPresser = p.simId == (if (p.isHome) homePresserId else awayPresserId)
                    aiController.decide(p, players, ball, formation, isPresser, owner?.simId == p.simId, teamHasBall, dt)
                }
            }

            applyPlayerDecision(i, decision, dt)
        }

        // Update ball physics, interceptions, tackles, fouls, offsides, goals, set pieces
        updateBallPhysics(dt, allowGoals = true)
        resolveTacklesAndCollisions()
        updateCamera(dt)

        _snapshotFlow.value = buildSnapshot()
    }

    private fun buildHumanDecision(
        p: SimPlayer,
        input: ControlInput,
        isP1: Boolean,
        dt: Float
    ): AiDecision {
        val hasBall = ball.ownerSimId == p.simId
        val aimLen = hypot(input.moveX, input.moveY)
        val aimX = if (aimLen > 0.15f) input.moveX / aimLen else if (p.facingRight) 1f else -1f
        val aimY = if (aimLen > 0.15f) input.moveY / aimLen else 0f

        if (input.switchJustPressed && !hasBall) {
            switchControlledPlayer(isHome = p.isHome, isP1 = isP1)
        }

        var wantShoot = false
        var shootPower = 0.72f
        if (hasBall) {
            if (input.shootHolding) {
                val cur = if (isP1) p1ShootCharge else p2ShootCharge
                val next = (cur + dt * 1.45f).coerceAtMost(1f)
                if (isP1) p1ShootCharge = next else p2ShootCharge = next
            } else if (input.shootJustReleased || input.shootQuickTap) {
                val cur = if (isP1) p1ShootCharge else p2ShootCharge
                wantShoot = true
                shootPower = if (input.shootQuickTap && cur < 0.2f) 0.68f else cur.coerceIn(0.35f, 1f)
                if (isP1) p1ShootCharge = 0f else p2ShootCharge = 0f
            }
        } else {
            if (isP1) p1ShootCharge = 0f else p2ShootCharge = 0f
        }

        return AiDecision(
            moveX = input.moveX,
            moveY = input.moveY,
            sprint = input.sprint,
            fsmState = if (hasBall) PlayerFsmState.DRIBBLE else PlayerFsmState.PRESS_BALL,
            wantPass = hasBall && input.passJustPressed,
            wantThrough = hasBall && input.throughJustPressed,
            wantShoot = wantShoot,
            shootPower = shootPower,
            wantTackle = !hasBall && (input.tackleJustPressed || input.shootQuickTap || input.shootJustReleased),
            aimX = aimX,
            aimY = aimY
        )
    }

    private fun applyPlayerDecision(index: Int, d: AiDecision, dt: Float) {
        var p = players[index]
        val cooldown = (p.actionCooldown - dt).coerceAtLeast(0f)
        val slideT = (p.slideTimer - dt).coerceAtLeast(0f)
        val kickT = (p.kickTimer - dt).coerceAtLeast(0f)
        val celebT = (p.celebrateTimer - dt).coerceAtLeast(0f)

        // Slide tackle initiation
        if (d.wantTackle && cooldown <= 0f && slideT <= 0f && !p.isGk) {
            val dirLen = hypot(d.moveX, d.moveY)
            val tx = if (dirLen > 0.1f) d.moveX / dirLen else if (p.facingRight) 1f else -1f
            val ty = if (dirLen > 0.1f) d.moveY / dirLen else 0f
            audio.playTackle()
            p = p.copy(
                vx = tx * 310f,
                vy = ty * 310f,
                slideTimer = 0.34f,
                actionCooldown = 0.75f,
                anim = PlayerAnim.SLIDE,
                animTimer = p.animTimer + dt
            )
            players[index] = p
            return
        }

        // During slide animation keep momentum
        if (slideT > 0f) {
            val nx = (p.x + p.vx * dt).coerceIn(10f, PitchConstants.PITCH_W - 10f)
            val ny = (p.y + p.vy * dt).coerceIn(10f, PitchConstants.PITCH_H - 10f)
            players[index] = p.copy(
                x = nx,
                y = ny,
                vx = p.vx * 0.92f,
                vy = p.vy * 0.92f,
                slideTimer = slideT,
                actionCooldown = cooldown,
                anim = PlayerAnim.SLIDE,
                animTimer = p.animTimer + dt
            )
            return
        }

        // Kick / Pass / Through / Shoot actions when owning the ball
        if (ball.ownerSimId == p.simId && cooldown <= 0f) {
            if (d.wantShoot) {
                executeShot(p, d.aimX, d.aimY, d.shootPower)
                players[index] = p.copy(kickTimer = 0.24f, actionCooldown = 0.35f, anim = PlayerAnim.KICK)
                return
            }
            if (d.wantThrough) {
                executePass(p, d.aimX, d.aimY, isThrough = true)
                players[index] = p.copy(kickTimer = 0.22f, actionCooldown = 0.32f, anim = PlayerAnim.KICK)
                return
            }
            if (d.wantPass) {
                executePass(p, d.aimX, d.aimY, isThrough = false)
                players[index] = p.copy(kickTimer = 0.20f, actionCooldown = 0.28f, anim = PlayerAnim.KICK)
                return
            }
        }

        // Movement & Stamina calculation
        val mag = hypot(d.moveX, d.moveY).coerceAtMost(1f)
        val isSprinting = d.sprint && mag > 0.2f && p.stamina > 6f
        val basePaceSpeed = 128f + (p.pace - 50) * 1.15f
        val staminaPenalty = if (p.stamina < 25f) 0.78f else 1f
        val dribblePenalty = if (ball.ownerSimId == p.simId) 0.92f else 1f
        val speed = basePaceSpeed * (if (isSprinting) 1.36f else 1.0f) * staminaPenalty * dribblePenalty

        val targetVx = if (mag > 0.08f) (d.moveX / max(mag, 1f)) * speed else 0f
        val targetVy = if (mag > 0.08f) (d.moveY / max(mag, 1f)) * speed else 0f
        val newVx = p.vx + (targetVx - p.vx) * 0.35f
        val newVy = p.vy + (targetVy - p.vy) * 0.35f

        val nx = (p.x + newVx * dt).coerceIn(8f, PitchConstants.PITCH_W - 8f)
        val ny = (p.y + newVy * dt).coerceIn(10f, PitchConstants.PITCH_H - 10f)

        val newStamina = if (isSprinting) {
            (p.stamina - dt * (11f - (p.staminaMax - 60) * 0.1f)).coerceAtLeast(0f)
        } else {
            (p.stamina + dt * 5.2f).coerceAtMost(100f)
        }

        val speedMag = hypot(newVx, newVy)
        val facing = when {
            newVx > 8f -> true
            newVx < -8f -> false
            else -> p.facingRight
        }
        val anim = when {
            celebT > 0f -> PlayerAnim.CELEBRATE
            kickT > 0f -> PlayerAnim.KICK
            d.diveAnim -> PlayerAnim.GK_DIVE
            speedMag > 145f -> PlayerAnim.RUN
            speedMag > 20f -> PlayerAnim.WALK
            else -> PlayerAnim.IDLE
        }

        players[index] = p.copy(
            x = nx,
            y = ny,
            vx = newVx,
            vy = newVy,
            facingRight = facing,
            anim = anim,
            animTimer = p.animTimer + dt,
            fsmState = d.fsmState,
            stamina = newStamina,
            actionCooldown = cooldown,
            slideTimer = slideT,
            kickTimer = kickT,
            celebrateTimer = celebT
        )
    }

    private fun executePass(passer: SimPlayer, aimX: Float, aimY: Float, isThrough: Boolean) {
        val mates = players.filter { it.isHome == passer.isHome && it.simId != passer.simId && !it.sentOff }
        // Pick best teammate aligned with aim vector
        val target = mates.minByOrNull { m ->
            val dx = m.x - passer.x
            val dy = m.y - passer.y
            val dist = hypot(dx, dy).coerceAtLeast(1f)
            val dot = (dx / dist) * aimX + (dy / dist) * aimY
            val anglePenalty = (1f - dot) * 260f
            dist + anglePenalty
        }

        val passAccuracy = (passer.passing / 100f).coerceIn(0.6f, 0.98f)
        val jitter = (1f - passAccuracy) * 0.18f
        val targetX: Float
        val targetY: Float
        val speed: Float
        val vz: Float

        if (target != null && ((target.x - passer.x) * aimX + (target.y - passer.y) * aimY) > 0f) {
            val lead = if (isThrough) 0.55f else 0.18f
            targetX = target.x + target.vx * lead + (if (isThrough) (if (passer.isHome) 45f else -45f) else 0f)
            targetY = target.y + target.vy * lead
            val dist = hypot(targetX - passer.x, targetY - passer.y).coerceAtLeast(30f)
            speed = if (isThrough) (dist * 1.45f).coerceIn(330f, 540f) else (dist * 1.35f).coerceIn(260f, 440f)
            vz = if (isThrough && dist > 180f) 115f else 18f
        } else {
            targetX = passer.x + aimX * 220f
            targetY = passer.y + aimY * 220f
            speed = if (isThrough) 430f else 330f
            vz = if (isThrough) 90f else 15f
        }

        val angle = atan2(targetY - passer.y, targetX - passer.x) + (rng.nextFloat() - 0.5f) * jitter
        val attackDir = if (passer.isHome) 1f else -1f
        val inOppHalf = if (passer.isHome) passer.x > PitchConstants.CENTER_X else passer.x < PitchConstants.CENTER_X
        val kickedForward = (cos(angle) * attackDir) > 0.25f

        // Check offside condition at the moment of forward pass
        if (inOppHalf && kickedForward && target != null && !target.isGk) {
            if (isPlayerOffside(target, passer, allPlayers = players)) {
                triggerOffside(offsideByHome = passer.isHome, spotX = target.x, spotY = target.y)
                return
            }
        }

        audio.playPass()
        ball = ball.copy(
            vx = cos(angle) * speed,
            vy = sin(angle) * speed,
            vz = vz,
            ownerSimId = null,
            lastTouchHome = passer.isHome,
            prevTouchPlayerId = ball.lastTouchPlayerId,
            lastTouchPlayerId = passer.entityId,
            passPickupLockTimer = 0.16f,
            kickedForwardInOppHalf = kickedForward
        )
    }

    private fun executeShot(shooter: SimPlayer, aimX: Float, aimY: Float, power: Float) {
        val oppGoalX = if (shooter.isHome) PitchConstants.PITCH_W else 0f
        val dx = oppGoalX - shooter.x
        val goalAimY = PitchConstants.CENTER_Y + aimY * (PitchConstants.GOAL_HALF_H * 0.9f)
        val dy = goalAimY - shooter.y
        val baseAngle = atan2(dy, dx)
        val accuracyError = (1f - (shooter.shooting / 100f)) * 0.22f * (rng.nextFloat() - 0.5f)
        val finalAngle = baseAngle + accuracyError

        val shotSpeed = 420f + power * 280f + (shooter.shooting - 70) * 2.2f
        val liftZ = 45f + power * 145f

        if (shooter.isHome) homeShots++ else awayShots++
        crowdExcitement = 0.8f
        audio.playShoot()

        ball = ball.copy(
            vx = cos(finalAngle) * shotSpeed,
            vy = sin(finalAngle) * shotSpeed,
            vz = liftZ,
            ownerSimId = null,
            lastTouchHome = shooter.isHome,
            prevTouchPlayerId = ball.lastTouchPlayerId,
            lastTouchPlayerId = shooter.entityId,
            passPickupLockTimer = 0.18f
        )
    }

    private fun updateBallPhysics(dt: Float, allowGoals: Boolean) {
        val ownerId = ball.ownerSimId
        val lockTimer = (ball.passPickupLockTimer - dt).coerceAtLeast(0f)

        if (ownerId != null) {
            val owner = players.firstOrNull { it.simId == ownerId && !it.sentOff }
            if (owner != null) {
                val leadDist = 14f
                val facingVec = if (abs(owner.vx) > 8f || abs(owner.vy) > 8f) {
                    val len = hypot(owner.vx, owner.vy).coerceAtLeast(1f)
                    owner.vx / len to owner.vy / len
                } else {
                    (if (owner.facingRight) 1f else -1f) to 0f
                }
                ball = ball.copy(
                    x = owner.x + facingVec.first * leadDist,
                    y = owner.y + facingVec.second * leadDist,
                    z = 0f,
                    vx = owner.vx,
                    vy = owner.vy,
                    vz = 0f,
                    rotationAngle = ball.rotationAngle + hypot(owner.vx, owner.vy) * dt * 0.08f,
                    passPickupLockTimer = lockTimer
                )
                return
            } else {
                ball = ball.copy(ownerSimId = null)
            }
        }

        // Free-flight ball physics (2.5D X, Y, Z)
        val grassFriction = when (config.weather) {
            WeatherType.CLEAR -> 0.972f
            WeatherType.RAIN -> 0.980f
            WeatherType.STORM -> 0.984f
        }
        val newVz = ball.vz - 460f * dt
        var newZ = ball.z + newVz * dt
        var bounceVz = newVz
        if (newZ <= 0f) {
            newZ = 0f
            bounceVz = if (abs(newVz) > 55f) -newVz * 0.56f else 0f
        }

        val frictionFactor = if (newZ > 2f) 0.992f else grassFriction
        val newVx = ball.vx * frictionFactor
        val newVy = ball.vy * frictionFactor
        val newX = ball.x + newVx * dt
        val newY = ball.y + newVy * dt

        ball = ball.copy(
            x = newX,
            y = newY,
            z = newZ,
            vx = newVx,
            vy = newVy,
            vz = bounceVz,
            rotationAngle = ball.rotationAngle + hypot(newVx, newVy) * dt * 0.08f,
            passPickupLockTimer = lockTimer
        )

        if (!allowGoals) {
            ball = ball.copy(
                x = ball.x.coerceIn(-25f, PitchConstants.PITCH_W + 25f),
                y = ball.y.coerceIn(0f, PitchConstants.PITCH_H)
            )
            return
        }

        // Check Goalpost & Crossbar collisions or Goal Scored!
        val inGoalY = ball.y in PitchConstants.GOAL_TOP_Y..PitchConstants.GOAL_BOTTOM_Y
        val underBar = ball.z <= PitchConstants.GOAL_HEIGHT
        if (ball.x <= 0f) {
            if (inGoalY && underBar) {
                onGoalScored(scoredByHome = false)
                return
            } else if (inGoalY && !underBar && abs(ball.z - PitchConstants.GOAL_HEIGHT) < 10f) {
                audio.playPostClang()
                ball = ball.copy(x = 4f, vx = abs(ball.vx) * 0.6f)
                return
            } else {
                // Endline left -> Corner or Goal Kick
                if (ball.lastTouchHome) {
                    setupCornerKick(forHome = false, topCorner = ball.y < PitchConstants.CENTER_Y)
                } else {
                    setupGoalKick(forHome = true)
                }
                return
            }
        }
        if (ball.x >= PitchConstants.PITCH_W) {
            if (inGoalY && underBar) {
                onGoalScored(scoredByHome = true)
                return
            } else if (inGoalY && !underBar && abs(ball.z - PitchConstants.GOAL_HEIGHT) < 10f) {
                audio.playPostClang()
                ball = ball.copy(x = PitchConstants.PITCH_W - 4f, vx = -abs(ball.vx) * 0.6f)
                return
            } else {
                // Endline right -> Corner or Goal Kick
                if (!ball.lastTouchHome) {
                    setupCornerKick(forHome = true, topCorner = ball.y < PitchConstants.CENTER_Y)
                } else {
                    setupGoalKick(forHome = false)
                }
                return
            }
        }

        // Touchline out of bounds -> Throw-In
        if (ball.y <= 0f || ball.y >= PitchConstants.PITCH_H) {
            setupThrowIn(forHome = !ball.lastTouchHome, spotX = ball.x.coerceIn(30f, PitchConstants.PITCH_W - 30f), topLine = ball.y <= 0f)
            return
        }

        // Ball pickup / Goalkeeper save check
        if (lockTimer <= 0f && ball.z < 36f) {
            val catcher = players.filter { !it.sentOff && it.slideTimer <= 0f }
                .firstOrNull { p ->
                    val reach = if (p.isGk) 26f else 17f
                    hypot(p.x - ball.x, p.y - ball.y) < reach && (p.isGk || ball.z < 20f)
                }
            if (catcher != null) {
                ball = ball.copy(
                    ownerSimId = catcher.simId,
                    lastTouchHome = catcher.isHome,
                    prevTouchPlayerId = ball.lastTouchPlayerId,
                    lastTouchPlayerId = catcher.entityId
                )
            }
        }
    }

    private fun resolveTacklesAndCollisions() {
        val owner = ball.ownerSimId?.let { id -> players.firstOrNull { it.simId == id } } ?: return
        for (i in players.indices) {
            val tackler = players[i]
            if (tackler.sentOff || tackler.isHome == owner.isHome) continue
            val distToOwner = hypot(tackler.x - owner.x, tackler.y - owner.y)
            val distToBall = hypot(tackler.x - ball.x, tackler.y - ball.y)

            if (tackler.slideTimer > 0f) {
                if (distToBall < 20f) {
                    // Clean ball win!
                    ball = ball.copy(
                        ownerSimId = tackler.simId,
                        lastTouchHome = tackler.isHome,
                        prevTouchPlayerId = ball.lastTouchPlayerId,
                        lastTouchPlayerId = tackler.entityId,
                        passPickupLockTimer = 0.25f
                    )
                    return
                } else if (distToOwner < 18f) {
                    // Foul committed!
                    triggerFoul(foulerIndex = i, victim = owner)
                    return
                }
            } else if (distToBall < 14f && tackler.actionCooldown <= 0f && rng.nextFloat() < 0.22f) {
                // Standing interception
                ball = ball.copy(
                    ownerSimId = tackler.simId,
                    lastTouchHome = tackler.isHome,
                    prevTouchPlayerId = ball.lastTouchPlayerId,
                    lastTouchPlayerId = tackler.entityId,
                    passPickupLockTimer = 0.2f
                )
                return
            }
        }
    }

    private fun triggerFoul(foulerIndex: Int, victim: SimPlayer) {
        val fouler = players[foulerIndex]
        if (fouler.isHome) homeFouls++ else awayFouls++
        audio.playWhistle(true)

        val newYellows = fouler.yellowCards + 1
        val sentOff = newYellows >= 2
        players[foulerIndex] = fouler.copy(
            yellowCards = newYellows,
            sentOff = sentOff,
            slideTimer = 0f,
            vx = 0f,
            vy = 0f
        )

        val inVictimAttackingBox = if (victim.isHome) {
            victim.x > PitchConstants.PITCH_W - PitchConstants.PENALTY_BOX_W &&
                abs(victim.y - PitchConstants.CENTER_Y) < PitchConstants.PENALTY_BOX_HALF_H
        } else {
            victim.x < PitchConstants.PENALTY_BOX_W &&
                abs(victim.y - PitchConstants.CENTER_Y) < PitchConstants.PENALTY_BOX_HALF_H
        }

        val cardText = if (sentOff) "RED CARD! ${fouler.name}" else "YELLOW CARD! ${fouler.name}"
        if (inVictimAttackingBox) {
            setupPenaltyKick(forHome = victim.isHome, taker = victim)
            showBanner("PENALTY!", cardText, 0xFFFF4757)
        } else {
            setupFreeKick(forHome = victim.isHome, spotX = victim.x, spotY = victim.y)
            showBanner("FREE KICK", cardText, 0xFFFFD54F)
        }
    }

    private fun isPlayerOffside(receiver: SimPlayer, passer: SimPlayer, allPlayers: List<SimPlayer>): Boolean {
        val oppPlayers = allPlayers.filter { it.isHome != passer.isHome && !it.sentOff }
        return if (passer.isHome) {
            if (receiver.x <= PitchConstants.CENTER_X || receiver.x <= passer.x) return false
            val sortedOppX = oppPlayers.map { it.x }.sortedDescending()
            val secondLastDefX = sortedOppX.getOrElse(1) { PitchConstants.PITCH_W }
            receiver.x > secondLastDefX + 8f && receiver.x > ball.x + 8f
        } else {
            if (receiver.x >= PitchConstants.CENTER_X || receiver.x >= passer.x) return false
            val sortedOppX = oppPlayers.map { it.x }.sorted()
            val secondLastDefX = sortedOppX.getOrElse(1) { 0f }
            receiver.x < secondLastDefX - 8f && receiver.x < ball.x - 8f
        }
    }

    private fun triggerOffside(offsideByHome: Boolean, spotX: Float, spotY: Float) {
        audio.playWhistle(false)
        setupFreeKick(forHome = !offsideByHome, spotX = spotX, spotY = spotY)
        setPieceType = SetPieceType.OFFSIDE
        showBanner("OFFSIDE", "Free Kick ${if (!offsideByHome) homeClub.shortCode else awayClub.shortCode}", 0xFFFFD54F)
    }

    private fun onGoalScored(scoredByHome: Boolean) {
        if (scoredByHome) homeScore++ else awayScore++
        crowdExcitement = 1.0f
        audio.playGoalRoar()

        val scorer = players.firstOrNull { it.entityId == ball.lastTouchPlayerId && it.isHome == scoredByHome }
            ?: players.firstOrNull { it.isHome == scoredByHome && !it.isGk }
        if (scorer != null) {
            scorerEntityIds.add(scorer.entityId)
            scorerLogs.add("${displayMinuteLabel()} ${scorer.name} (${if (scoredByHome) homeClub.shortCode else awayClub.shortCode})")
        }
        val assisterId = ball.prevTouchPlayerId
        if (assisterId != null && assisterId != scorer?.entityId) {
            val assister = players.firstOrNull { it.entityId == assisterId && it.isHome == scoredByHome }
            if (assister != null) assistEntityIds.add(assister.entityId)
        }

        // Trigger celebration animation for scoring team!
        for (i in players.indices) {
            if (players[i].isHome == scoredByHome && !players[i].sentOff) {
                players[i] = players[i].copy(celebrateTimer = 2.4f, anim = PlayerAnim.CELEBRATE)
            }
        }

        phase = MatchPhase.GOAL_CELEBRATION
        phaseTimer = 2.4f
        ball = ball.copy(
            vx = ball.vx * 0.2f,
            vy = ball.vy * 0.2f,
            vz = 0f,
            ownerSimId = null,
            lastTouchHome = scoredByHome
        )
        showBanner("GOAL!!", scorer?.name ?: (if (scoredByHome) homeClub.name else awayClub.name), 0xFFF5B700)
    }

    private fun setupFreeKick(forHome: Boolean, spotX: Float, spotY: Float) {
        val sx = spotX.coerceIn(60f, PitchConstants.PITCH_W - 60f)
        val sy = spotY.coerceIn(40f, PitchConstants.PITCH_H - 40f)
        val taker = players.filter { it.isHome == forHome && !it.isGk && !it.sentOff }
            .minByOrNull { hypot(it.x - sx, it.y - sy) }
        placeSetPiece(SetPieceType.FREE_KICK, forHome, sx, sy, taker)
    }

    private fun setupCornerKick(forHome: Boolean, topCorner: Boolean) {
        val cx = if (forHome) PitchConstants.PITCH_W - 14f else 14f
        val cy = if (topCorner) 14f else PitchConstants.PITCH_H - 14f
        val taker = players.firstOrNull { it.isHome == forHome && !it.isGk && !it.sentOff }
        audio.playWhistle(false)
        placeSetPiece(SetPieceType.CORNER, forHome, cx, cy, taker)
        showBanner("CORNER KICK", if (forHome) homeClub.shortCode else awayClub.shortCode, 0xFF29B6F6)
    }

    private fun setupGoalKick(forHome: Boolean) {
        val gx = if (forHome) 52f else PitchConstants.PITCH_W - 52f
        val gy = PitchConstants.CENTER_Y
        val gk = players.firstOrNull { it.isHome == forHome && it.isGk }
        placeSetPiece(SetPieceType.GOAL_KICK, forHome, gx, gy, gk)
        showBanner("GOAL KICK", if (forHome) homeClub.shortCode else awayClub.shortCode, 0xFF94A8C0)
    }

    private fun setupThrowIn(forHome: Boolean, spotX: Float, topLine: Boolean) {
        val ty = if (topLine) 18f else PitchConstants.PITCH_H - 18f
        val taker = players.filter { it.isHome == forHome && !it.isGk && !it.sentOff }
            .minByOrNull { hypot(it.x - spotX, it.y - ty) }
        placeSetPiece(SetPieceType.THROW_IN, forHome, spotX, ty, taker)
        showBanner("THROW IN", if (forHome) homeClub.shortCode else awayClub.shortCode, 0xFF00D084)
    }

    private fun setupPenaltyKick(forHome: Boolean, taker: SimPlayer) {
        val px = if (forHome) PitchConstants.PITCH_W - PitchConstants.PENALTY_SPOT_OFFSET else PitchConstants.PENALTY_SPOT_OFFSET
        val py = PitchConstants.CENTER_Y
        placeSetPiece(SetPieceType.PENALTY, forHome, px, py, taker)
    }

    private fun placeSetPiece(type: SetPieceType, forHome: Boolean, x: Float, y: Float, taker: SimPlayer?) {
        phase = MatchPhase.SET_PIECE_WAIT
        setPieceType = type
        setPieceTeamHome = forHome
        phaseTimer = 0.85f
        if (taker != null) {
            val idx = players.indexOfFirst { it.simId == taker.simId }
            if (idx != -1) {
                players[idx] = players[idx].copy(x = x - (if (forHome) 12f else -12f), y = y, vx = 0f, vy = 0f)
            }
        }
        // Push opponents back slightly
        for (i in players.indices) {
            val p = players[i]
            if (p.isHome != forHome && !p.isGk) {
                val d = hypot(p.x - x, p.y - y)
                if (d < 90f) {
                    val pushX = if (forHome) (p.x - 55f).coerceIn(30f, PitchConstants.PITCH_W - 30f) else (p.x + 55f).coerceIn(30f, PitchConstants.PITCH_W - 30f)
                    players[i] = p.copy(x = pushX, vx = 0f, vy = 0f)
                }
            }
        }
        ball = ball.copy(
            x = x,
            y = y,
            z = 0f,
            vx = 0f,
            vy = 0f,
            vz = 0f,
            ownerSimId = taker?.simId,
            lastTouchHome = forHome
        )
    }

    private fun spawnFullTeams(homeRoster: List<PlayerEntity>, awayRoster: List<PlayerEntity>) {
        players.clear()
        val homeStarters = buildEleven(homeRoster)
        val awayStarters = buildEleven(awayRoster)

        homeStarters.forEachIndexed { slot, entity ->
            players.add(createSimPlayer(slot + 1, entity, isHome = true, slotIndex = slot, club = homeClub))
        }
        awayStarters.forEachIndexed { slot, entity ->
            players.add(createSimPlayer(100 + slot + 1, entity, isHome = false, slotIndex = slot, club = awayClub))
        }

        // Assign initial P1 (and P2 if 1P vs 2P)
        val p1Id = config.lockedUserPlayerId?.let { locked ->
            players.firstOrNull { it.entityId == locked && it.isHome }?.simId
        } ?: players.lastOrNull { it.isHome && !it.isGk }?.simId

        players.replaceAll { p ->
            when {
                p.simId == p1Id -> p.copy(isControlledByP1 = true)
                config.sides == MatchSides.P1_VS_P2 && p.simId == players.lastOrNull { !it.isHome && !it.isGk }?.simId ->
                    p.copy(isControlledByP2 = true)
                else -> p
            }
        }
    }

    private fun buildEleven(roster: List<PlayerEntity>): List<PlayerEntity> {
        val starters = roster.filter { it.isStarter }.toMutableList()
        val extras = roster.filter { !it.isStarter }
        while (starters.size < 11 && extras.isNotEmpty()) {
            starters.add(extras[starters.size % extras.size])
        }
        val gk = starters.firstOrNull { it.position == Position.GK } ?: starters.first()
        val outfield = starters.filter { it.id != gk.id }.take(10)
        return (listOf(gk) + outfield).take(11)
    }

    private fun createSimPlayer(
        simId: Int,
        e: PlayerEntity,
        isHome: Boolean,
        slotIndex: Int,
        club: ClubEntity
    ): SimPlayer {
        val formation = if (isHome) config.homeFormation else config.awayFormation
        val anchors = FormationTemplates.getAnchors(formation)
        val anchor = anchors.getOrElse(slotIndex) { anchors.last() }
        val ax = (if (isHome) anchor.normX * 0.88f else 1f - anchor.normX * 0.88f) * PitchConstants.PITCH_W
        val ay = anchor.normY * PitchConstants.PITCH_H
        return SimPlayer(
            simId = simId,
            entityId = e.id,
            name = e.name,
            shirtNumber = slotIndex + 1,
            isHome = isHome,
            isGk = slotIndex == 0,
            slotIndex = slotIndex,
            role = e.position,
            ovr = e.ovr,
            pace = e.pace,
            shooting = e.shooting,
            passing = e.passing,
            dribbling = e.dribbling,
            defending = e.defending,
            staminaMax = e.staminaMax,
            skinToneIndex = e.skinToneIndex,
            hairStyleIndex = e.hairStyleIndex,
            primaryColor = club.primaryColorHex,
            secondaryColor = club.secondaryColorHex,
            shortsColor = club.shortsColorHex,
            x = ax,
            y = ay,
            facingRight = isHome,
            homeAnchorX = ax,
            homeAnchorY = ay
        )
    }

    private fun resetToKickoff(homeKicksOff: Boolean) {
        phase = MatchPhase.KICKOFF
        setPieceType = SetPieceType.KICKOFF
        setPieceTeamHome = homeKicksOff
        phaseTimer = 1.1f

        val homeAnchors = FormationTemplates.getAnchors(config.homeFormation)
        val awayAnchors = FormationTemplates.getAnchors(config.awayFormation)
        for (i in players.indices) {
            val p = players[i]
            val anchors = if (p.isHome) homeAnchors else awayAnchors
            val anchor = anchors.getOrElse(p.slotIndex) { anchors.last() }
            val nx = if (p.isHome) {
                (anchor.normX * 0.56f) * PitchConstants.PITCH_W
            } else {
                (1f - anchor.normX * 0.56f) * PitchConstants.PITCH_W
            }
            val ny = anchor.normY * PitchConstants.PITCH_H
            players[i] = p.copy(
                x = nx,
                y = ny,
                vx = 0f,
                vy = 0f,
                facingRight = p.isHome,
                celebrateTimer = 0f,
                anim = PlayerAnim.IDLE
            )
        }
        val kicker = players.lastOrNull { it.isHome == homeKicksOff && !it.sentOff && !it.isGk }
        if (kicker != null) {
            val kIdx = players.indexOfFirst { it.simId == kicker.simId }
            players[kIdx] = players[kIdx].copy(
                x = PitchConstants.CENTER_X + (if (homeKicksOff) -12f else 12f),
                y = PitchConstants.CENTER_Y
            )
        }
        ball = SimBall(
            x = PitchConstants.CENTER_X,
            y = PitchConstants.CENTER_Y,
            ownerSimId = kicker?.simId,
            lastTouchHome = homeKicksOff
        )
    }

    private fun updateActivePlayerAssignments(owner: SimPlayer?) {
        if (config.lockedUserPlayerId != null) return
        // Auto-switch P1 if teammate gets possession
        if (owner != null && owner.isHome && !owner.isControlledByP1) {
            players.replaceAll { it.copy(isControlledByP1 = it.simId == owner.simId) }
        } else if (owner == null) {
            // Ensure at least one P1 active player
            if (players.none { it.isControlledByP1 && !it.sentOff }) {
                switchControlledPlayer(isHome = true, isP1 = true)
            }
        }

        if (config.sides == MatchSides.P1_VS_P2) {
            if (owner != null && !owner.isHome && !owner.isControlledByP2) {
                players.replaceAll { it.copy(isControlledByP2 = it.simId == owner.simId) }
            } else if (players.none { it.isControlledByP2 && !it.sentOff }) {
                switchControlledPlayer(isHome = false, isP1 = false)
            }
        }
    }

    private fun switchControlledPlayer(isHome: Boolean, isP1: Boolean) {
        val candidates = players.filter { it.isHome == isHome && !it.sentOff && !it.isGk }
        val currentId = players.firstOrNull { if (isP1) it.isControlledByP1 else it.isControlledByP2 }?.simId
        val next = candidates
            .filter { it.simId != currentId }
            .minByOrNull { hypot(it.x - ball.x, it.y - ball.y) }
            ?: candidates.firstOrNull()
            ?: return

        players.replaceAll { p ->
            if (isP1) p.copy(isControlledByP1 = p.simId == next.simId)
            else p.copy(isControlledByP2 = p.simId == next.simId)
        }
    }

    private fun findClosestPresser(isHome: Boolean): Int? {
        return players.filter { it.isHome == isHome && !it.isGk && !it.sentOff && !it.isControlledByP1 && !it.isControlledByP2 }
            .minByOrNull { hypot(it.x - ball.x, it.y - ball.y) }?.simId
    }

    private fun updatePlayersAnimationsOnly(dt: Float) {
        for (i in players.indices) {
            val p = players[i]
            val celebT = (p.celebrateTimer - dt).coerceAtLeast(0f)
            players[i] = p.copy(
                animTimer = p.animTimer + dt,
                celebrateTimer = celebT,
                anim = if (celebT > 0f) PlayerAnim.CELEBRATE else PlayerAnim.IDLE
            )
        }
    }

    private fun updateCamera(dt: Float) {
        val targetX = (ball.x + ball.vx * 0.14f).coerceIn(220f, PitchConstants.PITCH_W - 220f)
        val targetY = (ball.y + ball.vy * 0.10f).coerceIn(180f, PitchConstants.PITCH_H - 180f)
        val lerp = (dt * 6.5f).coerceIn(0f, 1f)
        camX += (targetX - camX) * lerp
        camY += (targetY - camY) * lerp
    }

    // --- Interactive Training Drill Engine ---
    private fun setupTrainingScenario(
        homeRoster: List<PlayerEntity>,
        awayRoster: List<PlayerEntity>,
        drill: TrainingDrillType
    ) {
        players.clear()
        val star = homeRoster.firstOrNull { it.position == Position.FWD } ?: homeRoster.first()
        val p1 = createSimPlayer(1, star, isHome = true, slotIndex = 9, club = homeClub).copy(
            x = PitchConstants.CENTER_X - 90f,
            y = PitchConstants.CENTER_Y,
            isControlledByP1 = true
        )
        players.add(p1)

        if (drill == TrainingDrillType.SHOOTING) {
            val gkEntity = awayRoster.firstOrNull { it.position == Position.GK } ?: awayRoster.first()
            val gk = createSimPlayer(101, gkEntity, isHome = false, slotIndex = 0, club = awayClub).copy(
                x = PitchConstants.PITCH_W - 34f,
                y = PitchConstants.CENTER_Y
            )
            players.add(gk)
            ball = SimBall(x = PitchConstants.PITCH_W - 240f, y = PitchConstants.CENTER_Y, ownerSimId = p1.simId, lastTouchHome = true)
            players[0] = p1.copy(x = PitchConstants.PITCH_W - 252f, y = PitchConstants.CENTER_Y)
            showBanner("SHOOTING DRILL", "Beat the Keeper!", 0xFFF5B700)
        } else if (drill == TrainingDrillType.PASSING) {
            spawnPassingTargets()
            ball = SimBall(x = p1.x + 12f, y = p1.y, ownerSimId = p1.simId, lastTouchHome = true)
            showBanner("PASSING DRILL", "Pass into Gold Targets!", 0xFF00D084)
        } else {
            // Slalom Dribbling cones
            trainingTargets = (0..5).map { idx ->
                TrainingTarget(
                    id = idx,
                    x = 340f + idx * 125f,
                    y = PitchConstants.CENTER_Y + if (idx % 2 == 0) -65f else 65f,
                    radius = 28f,
                    isCone = true
                )
            }.toMutableList()
            players[0] = p1.copy(x = 240f, y = PitchConstants.CENTER_Y)
            ball = SimBall(x = 252f, y = PitchConstants.CENTER_Y, ownerSimId = p1.simId, lastTouchHome = true)
            showBanner("DRIBBLING DRILL", "Weave past every cone!", 0xFF29B6F6)
        }
    }

    private fun spawnPassingTargets() {
        trainingTargets = listOf(
            TrainingTarget(1, PitchConstants.CENTER_X + 140f, PitchConstants.CENTER_Y - 140f, 34f),
            TrainingTarget(2, PitchConstants.CENTER_X + 240f, PitchConstants.CENTER_Y + 120f, 34f),
            TrainingTarget(3, PitchConstants.CENTER_X + 350f, PitchConstants.CENTER_Y - 40f, 34f)
        ).toMutableList()
    }

    private fun stepTraining(dt: Float) {
        trainingTimeLeft = (trainingTimeLeft - dt).coerceAtLeast(0f)
        if (trainingTimeLeft <= 0f) {
            phase = MatchPhase.FULLTIME
            showBanner("DRILL COMPLETE", "Score: $trainingScore PTS", 0xFF00D084)
            return
        }

        for (i in players.indices) {
            val p = players[i]
            val d = if (p.isControlledByP1) {
                buildHumanDecision(p, p1Input, isP1 = true, dt = dt)
            } else {
                aiController.decide(p, players, ball, config.awayFormation, false, ball.ownerSimId == p.simId, false, dt)
            }
            applyPlayerDecision(i, d, dt)
        }
        updateBallPhysics(dt, allowGoals = false)

        val p1 = players.first()
        when (trainingDrill) {
            TrainingDrillType.PASSING -> {
                trainingTargets.forEachIndexed { idx, t ->
                    if (!t.hit && hypot(ball.x - t.x, ball.y - t.y) < t.radius) {
                        trainingTargets[idx] = t.copy(hit = true)
                        trainingScore += 100
                        audio.playGoalRoar()
                        showBanner("TARGET HIT!", "+100 PTS", 0xFF00D084)
                        ball = ball.copy(ownerSimId = p1.simId, x = p1.x + 12f, y = p1.y, vx = 0f, vy = 0f, vz = 0f)
                    }
                }
                if (trainingTargets.all { it.hit }) spawnPassingTargets()
            }
            TrainingDrillType.SHOOTING -> {
                if (ball.x >= PitchConstants.PITCH_W - 6f && ball.y in PitchConstants.GOAL_TOP_Y..PitchConstants.GOAL_BOTTOM_Y) {
                    trainingScore += 150
                    audio.playGoalRoar()
                    showBanner("NICE FINISH!", "+150 PTS", 0xFFF5B700)
                    players[0] = p1.copy(x = PitchConstants.PITCH_W - 260f, y = PitchConstants.CENTER_Y + rng.nextInt(-130, 130))
                    ball = SimBall(x = players[0].x + 12f, y = players[0].y, ownerSimId = p1.simId, lastTouchHome = true)
                } else if (ball.x >= PitchConstants.PITCH_W || ball.x <= 10f || ball.y <= 5f || ball.y >= PitchConstants.PITCH_H - 5f ||
                    (ball.ownerSimId != null && ball.ownerSimId != p1.simId)
                ) {
                    players[0] = p1.copy(x = PitchConstants.PITCH_W - 260f, y = PitchConstants.CENTER_Y + rng.nextInt(-110, 110))
                    ball = SimBall(x = players[0].x + 12f, y = players[0].y, ownerSimId = p1.simId, lastTouchHome = true)
                }
            }
            TrainingDrillType.DRIBBLING -> {
                trainingTargets.forEachIndexed { idx, cone ->
                    if (!cone.hit && hypot(p1.x - cone.x, p1.y - cone.y) < 38f && ball.ownerSimId == p1.simId) {
                        trainingTargets[idx] = cone.copy(hit = true)
                        trainingScore += 50
                        audio.playMenuClick()
                    }
                }
                if (trainingTargets.all { it.hit }) {
                    trainingScore += 100
                    audio.playGoalRoar()
                    showBanner("CIRCUIT CLEAR!", "+100 BONUS", 0xFF29B6F6)
                    trainingTargets = trainingTargets.map { it.copy(hit = false) }.toMutableList()
                }
            }
            null -> Unit
        }
        updateCamera(dt)
    }

    private fun showBanner(title: String, subtitle: String, colorHex: Long) {
        banner = MatchEventBanner(title, subtitle, colorHex)
        bannerTimer = 2.0f
    }

    private fun displayMinuteLabel(): String {
        val m = ((matchSecondsElapsed / totalMatchSeconds) * 90f).toInt().coerceIn(1, 90)
        return "$m'"
    }

    private fun buildSnapshot(): MatchSnapshot {
        return MatchSnapshot(
            homeClub = homeClub,
            awayClub = awayClub,
            players = players.toList(),
            ball = ball,
            homeScore = homeScore,
            awayScore = awayScore,
            matchSecondsElapsed = matchSecondsElapsed,
            totalMatchSeconds = totalMatchSeconds,
            half = half,
            phase = phase,
            setPieceType = setPieceType,
            setPieceTeamHome = setPieceTeamHome,
            banner = banner,
            weather = config.weather,
            timeOfDay = config.timeOfDay,
            p1ShootCharge = p1ShootCharge,
            p2ShootCharge = p2ShootCharge,
            homePossessionTicks = homePossessionTicks,
            awayPossessionTicks = awayPossessionTicks,
            homeShots = homeShots,
            awayShots = awayShots,
            homeFouls = homeFouls,
            awayFouls = awayFouls,
            scorerLogs = scorerLogs.toList(),
            scorerEntityIds = scorerEntityIds.toList(),
            assistEntityIds = assistEntityIds.toList(),
            homeBench = homeBench.toList(),
            awayBench = awayBench.toList(),
            isTrainingMode = trainingDrill != null,
            trainingDrill = trainingDrill,
            trainingScore = trainingScore,
            trainingTimeLeft = trainingTimeLeft,
            trainingTargets = trainingTargets.toList(),
            crowdExcitement = crowdExcitement,
            cameraX = camX,
            cameraY = camY,
            tickCount = tickCount
        )
    }
}
