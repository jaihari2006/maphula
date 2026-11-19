package com.offlineai.app.ui.arena

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import com.offlineai.app.engine.RavanaSkillEngine
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Animated Battle Arena Motion Graphics View for Ravana Adversarial Crucible.
 * Features:
 * - Animated Cyberpunk Holographic Arena Grid and Forcefield
 * - Red Attacker Bots vs Green/Cyan Defender Bots clashing with dynamic motion
 * - Energy Beam Discharges, Laser Sparks, Projectile Particle Physics
 * - Deflector Shield impact ripples and Shockwaves
 * - Real-time Bot Health Gauges, Floating Combat Numbers, Dynamic Head-to-Head Duels
 */
class RavanaArenaBattleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var isFighting: Boolean = false
    private var battleProgress: Float = 0f // 0f to 1f

    private var participatingBots: List<RavanaSkillEngine.RavanaBot> = emptyList()

    data class BotFighter(
        val name: String,
        val avatar: String,
        val isAttacker: Boolean,
        var x: Float = 0f,
        var y: Float = 0f,
        var targetX: Float = 0f,
        var targetY: Float = 0f,
        var baseRadius: Float = 36f,
        var health: Float = 100f,
        var maxHealth: Float = 100f,
        var recoilOffset: Float = 0f,
        var strikePhase: Float = 0f,
        var shieldGlow: Float = 0f,
        var laserBeamTarget: Pair<Float, Float>? = null,
        var primaryColor: Int = Color.parseColor("#EF4444")
    )

    data class CombatParticle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var life: Float, // 1f down to 0f
        var decay: Float,
        var color: Int,
        var radius: Float
    )

    data class FloatingCombatText(
        var text: String,
        var x: Float,
        var y: Float,
        var vy: Float = -2.5f,
        var alpha: Float = 1.0f,
        var color: Int = Color.parseColor("#F43F5E")
    )

    data class Shockwave(
        var cx: Float,
        var cy: Float,
        var currentRadius: Float = 10f,
        var maxRadius: Float = 90f,
        var alpha: Float = 1.0f,
        var color: Int = Color.parseColor("#06B6D4")
    )

    private val fighters = mutableListOf<BotFighter>()
    private val particles = mutableListOf<CombatParticle>()
    private val floatingTexts = mutableListOf<FloatingCombatText>()
    private val shockwaves = mutableListOf<Shockwave>()

    // Paints
    private val bgPaint = Paint().apply {
        color = Color.parseColor("#0B0F19")
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#152238")
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }

    private val centerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E293B")
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val energyBeamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    private val botBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val botRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 34f
    }

    private val healthBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#334155")
        style = Paint.Style.FILL
    }

    private val healthFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val shockwavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private var animator: ValueAnimator? = null
    private var globalTick: Long = 0L

    init {
        // Sample default bots if not yet configured
        val sampleRed = RavanaSkillEngine.RavanaBot(1, "Inquisitor Red", "Attacker", "🔴", RavanaSkillEngine.BotStance.ATTACKER, "", "")
        val sampleGreen = RavanaSkillEngine.RavanaBot(2, "Architect White", "Defender", "🟢", RavanaSkillEngine.BotStance.DEFENDER, "", "")
        setupFighters(listOf(sampleRed, sampleGreen))
    }

    fun setupFighters(bots: List<RavanaSkillEngine.RavanaBot>) {
        participatingBots = bots
        fighters.clear()
        particles.clear()
        floatingTexts.clear()
        shockwaves.clear()

        val attackers = bots.filter { it.stance == RavanaSkillEngine.BotStance.ATTACKER || it.stance == RavanaSkillEngine.BotStance.CHAOS || it.stance == RavanaSkillEngine.BotStance.AUDITOR }
        val defenders = bots.filter { !attackers.contains(it) }

        val actualAttackers = if (attackers.isEmpty() && bots.isNotEmpty()) listOf(bots.first()) else attackers
        val actualDefenders = if (defenders.isEmpty() && bots.size > 1) bots.drop(1) else defenders

        // Create Fighter representations
        actualAttackers.forEachIndexed { idx, bot ->
            fighters.add(
                BotFighter(
                    name = bot.name,
                    avatar = bot.avatar,
                    isAttacker = true,
                    primaryColor = Color.parseColor(if (idx % 2 == 0) "#EF4444" else "#F97316")
                )
            )
        }

        actualDefenders.forEachIndexed { idx, bot ->
            fighters.add(
                BotFighter(
                    name = bot.name,
                    avatar = bot.avatar,
                    isAttacker = false,
                    primaryColor = Color.parseColor(if (idx % 2 == 0) "#10B981" else "#06B6D4")
                )
            )
        }

        recalculateFighterPositions()
        invalidate()
    }

    private fun recalculateFighterPositions() {
        if (width == 0 || height == 0 || fighters.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()

        val attackers = fighters.filter { it.isAttacker }
        val defenders = fighters.filter { !it.isAttacker }

        val leftCenterX = w * 0.22f
        val rightCenterX = w * 0.78f

        // Position attackers on the left flank
        val leftSpacing = h / (attackers.size + 1)
        attackers.forEachIndexed { i, f ->
            f.targetX = leftCenterX
            f.targetY = leftSpacing * (i + 1)
            if (f.x == 0f && f.y == 0f) {
                f.x = f.targetX
                f.y = f.targetY
            }
        }

        // Position defenders on the right flank
        val rightSpacing = h / (defenders.size + 1)
        defenders.forEachIndexed { i, f ->
            f.targetX = rightCenterX
            f.targetY = rightSpacing * (i + 1)
            if (f.x == 0f && f.y == 0f) {
                f.x = f.targetX
                f.y = f.targetY
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recalculateFighterPositions()
    }

    fun startBattleAnimation(durationMs: Long = 4000L, onBattleEnd: (() -> Unit)? = null) {
        isFighting = true
        animator?.cancel()
        particles.clear()
        floatingTexts.clear()
        shockwaves.clear()

        // Reset fighters health
        fighters.forEach {
            it.health = 100f
            it.maxHealth = 100f
        }

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            interpolator = LinearInterpolator()
            addUpdateListener { va ->
                battleProgress = va.animatedValue as Float
                triggerBattleStep(battleProgress)
                invalidate()
            }
        }
        animator?.start()
    }

    fun stopBattleAnimation() {
        isFighting = false
        animator?.cancel()
        fighters.forEach {
            it.recoilOffset = 0f
            it.strikePhase = 0f
            it.laserBeamTarget = null
            it.shieldGlow = 0f
        }
        invalidate()
    }

    private fun triggerBattleStep(progress: Float) {
        globalTick++

        val attackers = fighters.filter { it.isAttacker }
        val defenders = fighters.filter { !it.isAttacker }

        if (attackers.isEmpty() || defenders.isEmpty()) return

        // Every few ticks, an attacker or defender launches an assault!
        if (globalTick % 5 == 0L && progress < 0.95f) {
            val isAttackerTurn = Random.nextBoolean()
            val striker = if (isAttackerTurn) attackers.random() else defenders.random()
            val victim = if (isAttackerTurn) defenders.random() else attackers.random()

            // Lunge forward towards target
            striker.strikePhase = 1f
            striker.laserBeamTarget = Pair(victim.x, victim.y)
            victim.recoilOffset = if (victim.isAttacker) -18f else 18f
            victim.shieldGlow = 1f

            val damage = Random.nextInt(12, 38)
            victim.health = max(15f, victim.health - damage)

            // Spawn sparks and combat text
            spawnClashParticles(victim.x, victim.y, striker.primaryColor)
            spawnFloatingCombatText(
                text = if (isAttackerTurn) "-$damage CRIT!" else "PARRIED -$damage",
                x = victim.x + Random.nextFloat() * 20f - 10f,
                y = victim.y - 30f,
                color = if (isAttackerTurn) Color.parseColor("#F43F5E") else Color.parseColor("#10B981")
            )

            // Spawn Shockwave
            shockwaves.add(
                Shockwave(
                    cx = (striker.x + victim.x) / 2f,
                    cy = (striker.y + victim.y) / 2f,
                    currentRadius = 10f,
                    color = striker.primaryColor
                )
            )
        }

        // Decay recoil and strike animations
        fighters.forEach { f ->
            f.strikePhase = max(0f, f.strikePhase - 0.12f)
            f.recoilOffset *= 0.82f
            f.shieldGlow = max(0f, f.shieldGlow - 0.08f)
            if (f.strikePhase == 0f) {
                f.laserBeamTarget = null
            }
        }

        // Update Particles
        val particleIterator = particles.iterator()
        while (particleIterator.hasNext()) {
            val p = particleIterator.next()
            p.x += p.vx
            p.y += p.vy
            p.life -= p.decay
            if (p.life <= 0f) {
                particleIterator.remove()
            }
        }

        // Update Floating Texts
        val textIterator = floatingTexts.iterator()
        while (textIterator.hasNext()) {
            val ft = textIterator.next()
            ft.y += ft.vy
            ft.alpha -= 0.04f
            if (ft.alpha <= 0f) {
                textIterator.remove()
            }
        }

        // Update Shockwaves
        val shockwaveIterator = shockwaves.iterator()
        while (shockwaveIterator.hasNext()) {
            val sw = shockwaveIterator.next()
            sw.currentRadius += 6f
            sw.alpha -= 0.06f
            if (sw.alpha <= 0f || sw.currentRadius >= sw.maxRadius) {
                shockwaveIterator.remove()
            }
        }
    }

    private fun spawnClashParticles(cx: Float, cy: Float, color: Int) {
        val count = Random.nextInt(8, 16)
        for (i in 0 until count) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 12f + 4f
            particles.add(
                CombatParticle(
                    x = cx,
                    y = cy,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    life = 1.0f,
                    decay = Random.nextFloat() * 0.05f + 0.04f,
                    color = color,
                    radius = Random.nextFloat() * 4f + 2f
                )
            )
        }
    }

    private fun spawnFloatingCombatText(text: String, x: Float, y: Float, color: Int) {
        floatingTexts.add(
            FloatingCombatText(
                text = text,
                x = x,
                y = y,
                color = color
            )
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // 1. Cyberpunk Arena Background
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Draw arena grid lines
        val step = 32f
        var gx = 0f
        while (gx <= w) {
            canvas.drawLine(gx, 0f, gx, h, gridPaint)
            gx += step
        }
        var gy = 0f
        while (gy <= h) {
            canvas.drawLine(0f, gy, w, gy, gridPaint)
            gy += step
        }

        // Center Arena Line / Octagon Ring
        val cx = w / 2f
        val cy = h / 2f
        canvas.drawCircle(cx, cy, min(w, h) * 0.35f, centerRingPaint)
        canvas.drawLine(cx, 0f, cx, h, centerRingPaint)

        // Draw "VS" Arena Insignia in center
        textPaint.textSize = 24f
        textPaint.color = Color.parseColor("#475569")
        canvas.drawText("⚡ VS ⚡", cx, cy + 8f, textPaint)

        // 2. Draw Energy Beams & Lasers
        fighters.forEach { f ->
            val beamTarget = f.laserBeamTarget
            if (beamTarget != null && f.strikePhase > 0f) {
                energyBeamPaint.color = f.primaryColor
                energyBeamPaint.alpha = (f.strikePhase * 255).toInt().coerceIn(0, 255)
                val curX = if (f.isAttacker) f.x + (beamTarget.first - f.x) * 0.35f * f.strikePhase else f.x - (f.x - beamTarget.first) * 0.35f * f.strikePhase
                canvas.drawLine(f.x, f.y, beamTarget.first, beamTarget.second, energyBeamPaint)

                // Laser discharge core
                energyBeamPaint.color = Color.WHITE
                energyBeamPaint.strokeWidth = 2f
                canvas.drawLine(f.x, f.y, beamTarget.first, beamTarget.second, energyBeamPaint)
                energyBeamPaint.strokeWidth = 5f
            }
        }

        // 3. Draw Shockwaves
        shockwaves.forEach { sw ->
            shockwavePaint.color = sw.color
            shockwavePaint.alpha = (sw.alpha * 255).toInt().coerceIn(0, 255)
            canvas.drawCircle(sw.cx, sw.cy, sw.currentRadius, shockwavePaint)
        }

        // 4. Draw Bot Fighters
        fighters.forEach { f ->
            // Compute real x with strike lunge and recoil
            val lungeDistance = if (f.isAttacker) 30f else -30f
            val drawX = f.x + (lungeDistance * f.strikePhase) + f.recoilOffset
            val drawY = f.y

            // Deflector Shield glow ripple
            if (f.shieldGlow > 0f) {
                botRingPaint.color = if (f.isAttacker) Color.parseColor("#F43F5E") else Color.parseColor("#06B6D4")
                botRingPaint.alpha = (f.shieldGlow * 200).toInt().coerceIn(0, 255)
                canvas.drawCircle(drawX, drawY, f.baseRadius + 14f * f.shieldGlow, botRingPaint)
            }

            // Bot Outer Ring
            botRingPaint.color = f.primaryColor
            botRingPaint.alpha = 255
            canvas.drawCircle(drawX, drawY, f.baseRadius, botRingPaint)

            // Bot Core Circle
            botBodyPaint.color = Color.parseColor("#1E293B")
            canvas.drawCircle(drawX, drawY, f.baseRadius - 2f, botBodyPaint)

            // Bot Avatar Emoji
            emojiPaint.textSize = 28f
            canvas.drawText(f.avatar, drawX, drawY + 10f, emojiPaint)

            // Bot Name label
            textPaint.textSize = 10f
            textPaint.color = Color.parseColor("#CBD5E1")
            canvas.drawText(f.name, drawX, drawY - f.baseRadius - 14f, textPaint)

            // Bot Health Bar
            val barW = 54f
            val barH = 5f
            val barLeft = drawX - (barW / 2f)
            val barTop = drawY - f.baseRadius - 8f

            canvas.drawRoundRect(barLeft, barTop, barLeft + barW, barTop + barH, 3f, 3f, healthBgPaint)
            val healthPercent = (f.health / f.maxHealth).coerceIn(0f, 1f)
            healthFillPaint.color = if (healthPercent > 0.5f) Color.parseColor("#10B981") else if (healthPercent > 0.25f) Color.parseColor("#F59E0B") else Color.parseColor("#EF4444")
            canvas.drawRoundRect(barLeft, barTop, barLeft + (barW * healthPercent), barTop + barH, 3f, 3f, healthFillPaint)
        }

        // 5. Draw Particles
        particles.forEach { p ->
            energyBeamPaint.color = p.color
            energyBeamPaint.alpha = (p.life * 255).toInt().coerceIn(0, 255)
            energyBeamPaint.style = Paint.Style.FILL
            canvas.drawCircle(p.x, p.y, p.radius * p.life, energyBeamPaint)
        }
        energyBeamPaint.style = Paint.Style.STROKE

        // 6. Draw Floating Combat Texts (CRIT, PARRIED, etc.)
        floatingTexts.forEach { ft ->
            textPaint.textSize = 12f
            textPaint.color = ft.color
            textPaint.alpha = (ft.alpha * 255).toInt().coerceIn(0, 255)
            canvas.drawText(ft.text, ft.x, ft.y, textPaint)
        }

        // Invalidate on animation if active
        if (isFighting) {
            postInvalidateOnAnimation()
        }
    }
}
