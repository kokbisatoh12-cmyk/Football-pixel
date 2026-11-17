package com.example.game.engine

import com.example.data.Formation
import com.example.data.FormationTemplates
import com.example.data.MatchDifficulty
import com.example.data.PlayerFsmState
import com.example.data.Position
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

/**
 * Shared immutable context evaluated each simulation tick by a player's Finite State Machine.
 */
data class PlayerFsmContext(
    val player: SimPlayer,
    val allPlayers: List<SimPlayer>,
    val ball: SimBall,
    val formation: Formation,
    val difficulty: MatchDifficulty,
    val isPresser: Boolean,
    val hasBall: Boolean,
    val teamHasBall: Boolean,
    val rng: Random
) {
    val attackDir: Float get() = if (player.isHome) 1f else -1f
    val oppGoalX: Float get() = if (player.isHome) PitchConstants.PITCH_W else 0f
    val ownGoalX: Float get() = if (player.isHome) 0f else PitchConstants.PITCH_W
}

/**
 * Contract for all behavioral states inside [PlayerStateMachine].
 */
sealed interface PlayerState {
    val stateName: String
    val mappedEnum: PlayerFsmState

    fun onEnter(ctx: PlayerFsmContext) {}
    fun evaluateTransition(ctx: PlayerFsmContext): PlayerState?
    fun onExecute(ctx: PlayerFsmContext): AiDecision
    fun onExit(ctx: PlayerFsmContext) {}
}

/**
 * Per-player Finite State Machine controlling autonomous match AI behaviors
 * ('Chasing', 'Positioning', 'Dribbling', 'Shooting', 'Passing', 'Goalkeeping', 'Idle', 'Celebrating').
 */
class PlayerStateMachine(
    initialState: PlayerState = IdleState,
    private val onTransition: ((from: PlayerState, to: PlayerState) -> Unit)? = null
) {
    var currentState: PlayerState = initialState
        private set

    var previousState: PlayerState? = null
        private set

    var timeInState: Float = 0f
        private set

    var transitionCount: Int = 0
        private set

    fun update(ctx: PlayerFsmContext, dt: Float = 1f / 60f): AiDecision {
        val next = currentState.evaluateTransition(ctx)
        if (next != null && next::class != currentState::class) {
            transitionTo(next, ctx)
        }
        timeInState += dt
        return currentState.onExecute(ctx)
    }

    fun transitionTo(nextState: PlayerState, ctx: PlayerFsmContext) {
        val prev = currentState
        prev.onExit(ctx)
        previousState = prev
        currentState = nextState
        timeInState = 0f
        transitionCount++
        nextState.onEnter(ctx)
        onTransition?.invoke(prev, nextState)
    }
}

data object IdleState : PlayerState {
    override val stateName = "Idle"
    override val mappedEnum = PlayerFsmState.IDLE

    override fun evaluateTransition(ctx: PlayerFsmContext): PlayerState? {
        if (ctx.player.celebrateTimer > 0f) return CelebratingState
        if (ctx.player.isGk) return GoalkeepingState
        if (ctx.hasBall) return DribblingState
        if (ctx.isPresser && !ctx.teamHasBall) return ChasingState
        return PositioningState
    }

    override fun onExecute(ctx: PlayerFsmContext): AiDecision =
        AiDecision(fsmState = PlayerFsmState.IDLE)
}

data object CelebratingState : PlayerState {
    override val stateName = "Celebrating"
    override val mappedEnum = PlayerFsmState.CELEBRATE

    override fun evaluateTransition(ctx: PlayerFsmContext): PlayerState? {
        if (ctx.player.celebrateTimer <= 0f) {
            return if (ctx.player.isGk) GoalkeepingState else PositioningState
        }
        return null
    }

    override fun onExecute(ctx: PlayerFsmContext): AiDecision =
        AiDecision(fsmState = PlayerFsmState.CELEBRATE)
}

data object PositioningState : PlayerState {
    override val stateName = "Positioning"
    override val mappedEnum = PlayerFsmState.RECOVER_SHAPE

    override fun evaluateTransition(ctx: PlayerFsmContext): PlayerState? {
        if (ctx.player.celebrateTimer > 0f) return CelebratingState
        if (ctx.player.isGk) return GoalkeepingState
        if (ctx.hasBall) return DribblingState
        if (ctx.isPresser && !ctx.teamHasBall) return ChasingState
        return null
    }

    override fun onExecute(ctx: PlayerFsmContext): AiDecision {
        val player = ctx.player
        val (anchorX, anchorY) = FsmTacticalMath.computeFormationTarget(
            player = player,
            formation = ctx.formation,
            ball = ctx.ball,
            teamHasBall = ctx.teamHasBall
        )

        if (ctx.teamHasBall) {
            val pushForward = if (player.role == Position.FWD || player.role == Position.MID) {
                95f * ctx.attackDir
            } else {
                35f * ctx.attackDir
            }
            val targetX = (anchorX + pushForward).coerceIn(80f, PitchConstants.PITCH_W - 80f)
            val dx = targetX - player.x
            val dy = anchorY - player.y
            val dist = hypot(dx, dy)
            val stateEnum = if (player.role == Position.FWD) {
                PlayerFsmState.MAKE_RUN
            } else {
                PlayerFsmState.SUPPORT_ATTACK
            }
            return if (dist > 14f) {
                AiDecision(
                    moveX = dx / dist,
                    moveY = dy / dist,
                    sprint = dist > 110f && player.role == Position.FWD && player.stamina > 45f,
                    fsmState = stateEnum
                )
            } else {
                AiDecision(fsmState = stateEnum)
            }
        }

        // Defending positioning: mark dangerous attacker or recover formation shape
        val dangerousAttacker = if (player.role == Position.DEF) {
            ctx.allPlayers
                .filter { it.isHome != player.isHome && !it.sentOff && !it.isGk }
                .minByOrNull { hypot(it.x - anchorX, it.y - anchorY) }
        } else {
            null
        }

        val targetX: Float
        val targetY: Float
        val stateEnum: PlayerFsmState
        if (dangerousAttacker != null && hypot(dangerousAttacker.x - anchorX, dangerousAttacker.y - anchorY) < 150f) {
            targetX = dangerousAttacker.x + (ctx.ownGoalX - dangerousAttacker.x).coerceIn(-35f, 35f)
            targetY = dangerousAttacker.y
            stateEnum = PlayerFsmState.MARK_OPPONENT
        } else {
            targetX = anchorX
            targetY = anchorY
            stateEnum = PlayerFsmState.RECOVER_SHAPE
        }

        val dx = targetX - player.x
        val dy = targetY - player.y
        val dist = hypot(dx, dy)
        return if (dist > 12f) {
            AiDecision(
                moveX = dx / dist,
                moveY = dy / dist,
                sprint = dist > 140f && player.stamina > 30f,
                fsmState = stateEnum
            )
        } else {
            AiDecision(fsmState = stateEnum)
        }
    }
}

data object ChasingState : PlayerState {
    override val stateName = "Chasing"
    override val mappedEnum = PlayerFsmState.PRESS_BALL

    override fun evaluateTransition(ctx: PlayerFsmContext): PlayerState? {
        if (ctx.player.celebrateTimer > 0f) return CelebratingState
        if (ctx.player.isGk) return GoalkeepingState
        if (ctx.hasBall) return DribblingState
        if (ctx.teamHasBall || !ctx.isPresser) return PositioningState
        return null
    }

    override fun onExecute(ctx: PlayerFsmContext): AiDecision {
        val player = ctx.player
        val ball = ctx.ball
        val targetX = ball.x + ball.vx * 0.08f
        val targetY = ball.y + ball.vy * 0.08f
        val dx = targetX - player.x
        val dy = targetY - player.y
        val dist = hypot(dx, dy).coerceAtLeast(1f)
        val ballCarrier = ball.ownerSimId?.let { id -> ctx.allPlayers.firstOrNull { it.simId == id } }
        val shouldSlide = ballCarrier != null &&
            dist in 24f..48f &&
            player.actionCooldown <= 0f &&
            ctx.rng.nextFloat() < 0.025f * ctx.difficulty.aiReactionScale

        return AiDecision(
            moveX = dx / dist,
            moveY = dy / dist,
            sprint = dist > 42f && player.stamina > 22f,
            fsmState = if (shouldSlide) PlayerFsmState.SLIDE_TACKLE else PlayerFsmState.PRESS_BALL,
            wantTackle = shouldSlide
        )
    }
}

data object DribblingState : PlayerState {
    override val stateName = "Dribbling"
    override val mappedEnum = PlayerFsmState.DRIBBLE

    override fun evaluateTransition(ctx: PlayerFsmContext): PlayerState? {
        if (ctx.player.celebrateTimer > 0f) return CelebratingState
        if (ctx.player.isGk) return GoalkeepingState
        if (!ctx.hasBall) return if (ctx.isPresser && !ctx.teamHasBall) ChasingState else PositioningState

        if (FsmTacticalMath.shouldShootNow(ctx)) return ShootingState
        if (FsmTacticalMath.shouldPassNow(ctx)) return PassingState
        return null
    }

    override fun onExecute(ctx: PlayerFsmContext): AiDecision {
        val player = ctx.player
        val opponents = ctx.allPlayers.filter { it.isHome != player.isHome && !it.sentOff }
        val nearestOpp = opponents.minByOrNull { hypot(it.x - player.x, it.y - player.y) }
        val oppDist = nearestOpp?.let { hypot(it.x - player.x, it.y - player.y) } ?: 999f

        val dirX = ctx.oppGoalX - player.x
        var dirY = (PitchConstants.CENTER_Y - player.y) * 0.45f
        if (nearestOpp != null && oppDist < 85f) {
            dirY += (player.y - nearestOpp.y) * 1.4f
        }
        val len = hypot(dirX, dirY).coerceAtLeast(1f)
        return AiDecision(
            moveX = dirX / len,
            moveY = dirY / len,
            sprint = oppDist > 75f && player.stamina > 30f,
            fsmState = PlayerFsmState.DRIBBLE
        )
    }
}

data object ShootingState : PlayerState {
    override val stateName = "Shooting"
    override val mappedEnum = PlayerFsmState.SHOOT_DECISION

    override fun evaluateTransition(ctx: PlayerFsmContext): PlayerState? {
        if (ctx.player.celebrateTimer > 0f) return CelebratingState
        if (!ctx.hasBall) return if (ctx.isPresser && !ctx.teamHasBall) ChasingState else PositioningState
        if (!FsmTacticalMath.inShootingZone(ctx)) return DribblingState
        return null
    }

    override fun onExecute(ctx: PlayerFsmContext): AiDecision {
        val player = ctx.player
        val distToGoal = hypot(ctx.oppGoalX - player.x, PitchConstants.CENTER_Y - player.y)
        val aimTargetY = PitchConstants.CENTER_Y + (ctx.rng.nextFloat() - 0.5f) * (PitchConstants.GOAL_HALF_H * 1.4f)
        val dx = ctx.oppGoalX - player.x
        val dy = aimTargetY - player.y
        val len = hypot(dx, dy).coerceAtLeast(1f)
        return AiDecision(
            fsmState = PlayerFsmState.SHOOT_DECISION,
            wantShoot = true,
            shootPower = (0.58f + (1f - distToGoal / 320f) * 0.32f).coerceIn(0.5f, 0.96f),
            aimX = dx / len,
            aimY = dy / len
        )
    }
}

data object PassingState : PlayerState {
    override val stateName = "Passing"
    override val mappedEnum = PlayerFsmState.PASS_DECISION

    override fun evaluateTransition(ctx: PlayerFsmContext): PlayerState? {
        if (ctx.player.celebrateTimer > 0f) return CelebratingState
        if (!ctx.hasBall) return if (ctx.isPresser && !ctx.teamHasBall) ChasingState else PositioningState
        return null
    }

    override fun onExecute(ctx: PlayerFsmContext): AiDecision {
        val player = ctx.player
        val bestMate = FsmTacticalMath.findBestPassTarget(player, ctx.allPlayers, preferForward = true)
        if (bestMate == null) {
            return DribblingState.onExecute(ctx)
        }
        val dx = bestMate.x - player.x
        val dy = bestMate.y - player.y
        val len = hypot(dx, dy).coerceAtLeast(1f)
        val useThrough = (bestMate.role == Position.FWD || bestMate.role == Position.MID) &&
            hypot(dx, dy) > 135f && ctx.rng.nextFloat() < 0.45f
        return AiDecision(
            fsmState = PlayerFsmState.PASS_DECISION,
            wantPass = !useThrough,
            wantThrough = useThrough,
            aimX = dx / len,
            aimY = dy / len
        )
    }
}

data object GoalkeepingState : PlayerState {
    override val stateName = "Goalkeeping"
    override val mappedEnum = PlayerFsmState.GK_TRACK_ANGLE

    override fun evaluateTransition(ctx: PlayerFsmContext): PlayerState? {
        if (ctx.player.celebrateTimer > 0f) return CelebratingState
        if (!ctx.player.isGk) return PositioningState
        return null
    }

    override fun onExecute(ctx: PlayerFsmContext): AiDecision {
        val player = ctx.player
        val ball = ctx.ball
        val ownGoalX = ctx.ownGoalX
        val attackDir = ctx.attackDir

        if (ctx.hasBall) {
            val bestMate = FsmTacticalMath.findBestPassTarget(player, ctx.allPlayers, preferForward = true)
            return if (bestMate != null) {
                val dx = bestMate.x - player.x
                val dy = bestMate.y - player.y
                val len = hypot(dx, dy).coerceAtLeast(1f)
                AiDecision(
                    fsmState = PlayerFsmState.PASS_DECISION,
                    wantPass = true,
                    aimX = dx / len,
                    aimY = dy / len
                )
            } else {
                AiDecision(
                    fsmState = PlayerFsmState.PASS_DECISION,
                    wantThrough = true,
                    aimX = attackDir,
                    aimY = (ctx.rng.nextFloat() - 0.5f) * 0.4f
                )
            }
        }

        val ballMovingToGoal = if (player.isHome) ball.vx < -140f else ball.vx > 140f
        val distToOwnGoal = abs(ball.x - ownGoalX)
        if (ballMovingToGoal && distToOwnGoal < 260f && ball.ownerSimId == null) {
            val t = ((ownGoalX + 26f * attackDir) - ball.x) / (ball.vx.takeIf { abs(it) > 1f } ?: 1f)
            val predY = (ball.y + ball.vy * t.coerceIn(0f, 1.2f))
                .coerceIn(PitchConstants.GOAL_TOP_Y + 6f, PitchConstants.GOAL_BOTTOM_Y - 6f)
            val dy = predY - player.y
            val dx = (ownGoalX + 28f * attackDir) - player.x
            val len = hypot(dx, dy).coerceAtLeast(1f)
            return AiDecision(
                moveX = dx / len,
                moveY = (dy / len) * ctx.difficulty.aiReactionScale,
                sprint = abs(dy) > 14f,
                fsmState = PlayerFsmState.GK_DIVE_SAVE,
                diveAnim = abs(dy) > 28f && distToOwnGoal < 150f
            )
        }

        if (ball.ownerSimId == null && distToOwnGoal < 140f && abs(ball.y - PitchConstants.CENTER_Y) < 130f) {
            val dx = ball.x - player.x
            val dy = ball.y - player.y
            val len = hypot(dx, dy).coerceAtLeast(1f)
            return AiDecision(
                moveX = dx / len,
                moveY = dy / len,
                sprint = true,
                fsmState = PlayerFsmState.PRESS_BALL
            )
        }

        val anchorX = ownGoalX + 32f * attackDir
        val anchorY = (PitchConstants.CENTER_Y + (ball.y - PitchConstants.CENTER_Y) * 0.28f)
            .coerceIn(PitchConstants.GOAL_TOP_Y + 12f, PitchConstants.GOAL_BOTTOM_Y - 12f)
        val dx = anchorX - player.x
        val dy = anchorY - player.y
        val dist = hypot(dx, dy)
        return if (dist > 6f) {
            AiDecision(
                moveX = dx / dist,
                moveY = dy / dist,
                fsmState = PlayerFsmState.GK_TRACK_ANGLE
            )
        } else {
            AiDecision(fsmState = PlayerFsmState.GK_TRACK_ANGLE)
        }
    }
}

internal object FsmTacticalMath {
    fun computeFormationTarget(
        player: SimPlayer,
        formation: Formation,
        ball: SimBall,
        teamHasBall: Boolean
    ): Pair<Float, Float> {
        val slots = FormationTemplates.getAnchors(formation)
        val slot = slots.getOrElse(player.slotIndex) { slots.last() }
        val attackDir = if (player.isHome) 1f else -1f

        val normX = if (player.isHome) slot.normX else 1f - slot.normX
        val normY = slot.normY

        val ballShiftX = ((ball.x / PitchConstants.PITCH_W) - 0.5f) * 260f
        val possessionPush = if (teamHasBall) 75f * attackDir else -55f * attackDir
        val ballShiftY = ((ball.y / PitchConstants.PITCH_H) - 0.5f) * 110f

        val targetX = if (player.isGk) {
            if (player.isHome) 34f else PitchConstants.PITCH_W - 34f
        } else {
            (normX * PitchConstants.PITCH_W + ballShiftX + possessionPush)
                .coerceIn(70f, PitchConstants.PITCH_W - 70f)
        }

        val targetY = if (player.isGk) {
            val trackY = PitchConstants.CENTER_Y + (ball.y - PitchConstants.CENTER_Y) * 0.25f
            trackY.coerceIn(PitchConstants.GOAL_TOP_Y + 12f, PitchConstants.GOAL_BOTTOM_Y - 12f)
        } else {
            (normY * PitchConstants.PITCH_H + ballShiftY)
                .coerceIn(38f, PitchConstants.PITCH_H - 38f)
        }

        return targetX to targetY
    }

    fun inShootingZone(ctx: PlayerFsmContext): Boolean {
        val distToGoal = hypot(ctx.oppGoalX - ctx.player.x, PitchConstants.CENTER_Y - ctx.player.y)
        return distToGoal < 300f && abs(ctx.player.y - PitchConstants.CENTER_Y) < 195f
    }

    fun shouldShootNow(ctx: PlayerFsmContext): Boolean {
        if (!inShootingZone(ctx)) return false
        val player = ctx.player
        val distToGoal = hypot(ctx.oppGoalX - player.x, PitchConstants.CENTER_Y - player.y)
        val oppDist = ctx.allPlayers
            .filter { it.isHome != player.isHome && !it.sentOff }
            .minOfOrNull { hypot(it.x - player.x, it.y - player.y) } ?: 999f
        return distToGoal < 190f || oppDist < 62f || ctx.rng.nextFloat() < 0.045f * ctx.difficulty.aiReactionScale
    }

    fun shouldPassNow(ctx: PlayerFsmContext): Boolean {
        val player = ctx.player
        val opponents = ctx.allPlayers.filter { it.isHome != player.isHome && !it.sentOff }
        val oppDist = opponents.minOfOrNull { hypot(it.x - player.x, it.y - player.y) } ?: 999f
        val bestMate = findBestPassTarget(player, ctx.allPlayers, preferForward = true) ?: return false
        val teammateAhead = (bestMate.x - player.x) * ctx.attackDir > 55f &&
            isPassingLaneClear(player, bestMate, opponents)
        val underPressure = oppDist < 54f
        return underPressure || (teammateAhead && ctx.rng.nextFloat() < 0.032f * ctx.difficulty.aiReactionScale)
    }

    fun findBestPassTarget(
        passer: SimPlayer,
        allPlayers: List<SimPlayer>,
        preferForward: Boolean
    ): SimPlayer? {
        val attackDir = if (passer.isHome) 1f else -1f
        val mates = allPlayers.filter {
            it.isHome == passer.isHome && it.simId != passer.simId && !it.sentOff && !it.isGk
        }
        val opponents = allPlayers.filter { it.isHome != passer.isHome && !it.sentOff }

        return mates.maxByOrNull { m ->
            val dx = (m.x - passer.x) * attackDir
            val dist = hypot(m.x - passer.x, m.y - passer.y)
            val distScore = if (dist in 70f..340f) 40f else -abs(dist - 180f) * 0.25f
            val forwardScore = if (preferForward) dx * 0.22f else 0f
            val laneClearBonus = if (isPassingLaneClear(passer, m, opponents)) 45f else -40f
            val nearestDef = opponents.minOfOrNull { hypot(it.x - m.x, it.y - m.y) } ?: 120f
            val spaceBonus = nearestDef.coerceAtMost(100f) * 0.4f
            distScore + forwardScore + laneClearBonus + spaceBonus
        }
    }

    fun isPassingLaneClear(
        from: SimPlayer,
        to: SimPlayer,
        opponents: List<SimPlayer>
    ): Boolean {
        val vx = to.x - from.x
        val vy = to.y - from.y
        val lenSq = vx * vx + vy * vy
        if (lenSq < 1f) return true
        for (opp in opponents) {
            val t = (((opp.x - from.x) * vx + (opp.y - from.y) * vy) / lenSq).coerceIn(0f, 1f)
            val px = from.x + t * vx
            val py = from.y + t * vy
            if (hypot(opp.x - px, opp.y - py) < 28f) return false
        }
        return true
    }
}
