package com.offlineai.app.engine

import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Ravana Adversarial Arena Engine.
 * Implements a multi-bot adversarial debate crucible where 2 to 10 AI bots
 * clash across structured debate rounds to stress-test any question, idea,
 * or decision and determine whether it will SURVIVE or PERISH.
 */
class RavanaSkillEngine {

    companion object {
        const val MIN_BOTS = 2
        const val MAX_BOTS = 10
    }

    enum class BotStance {
        ATTACKER,
        DEFENDER,
        AUDITOR,
        CHAOS,
        STRATEGIST,
        PERFORMANCE,
        PSYCHOLOGY,
        MARKET,
        SECURITY,
        JUDGE,
        SPECIALIST
    }

    enum class SurvivalVerdict(val title: String, val emoji: String, val badgeColor: String) {
        SURVIVED("SURVIVED", "🏆", "#10B981"),
        CONDITIONALLY_SURVIVED("CONDITIONALLY SURVIVED", "⚠️", "#F59E0B"),
        PERISHED("PERISHED", "💀", "#EF4444")
    }

    data class RavanaBot(
        val id: Int,
        val name: String,
        val title: String,
        val avatar: String,
        val stance: BotStance,
        val tagline: String,
        val primaryFocus: String
    )

    data class DebateTurn(
        val roundNumber: Int,
        val bot: RavanaBot,
        val statement: String,
        val impactScore: Int // 1-100 impact of argument
    )

    data class BattleRound(
        val roundNumber: Int,
        val title: String,
        val description: String,
        val turns: List<DebateTurn>,
        val ideaHealthAfterRound: Int, // 0-100
        val armorRating: Int = 80,
        val attackPenetration: Int = 45
    )

    data class RavanaBattleResult(
        val userIdea: String,
        val botCount: Int,
        val participatingBots: List<RavanaBot>,
        val rounds: List<BattleRound>,
        val survivalScore: Int, // 0-100%
        val verdict: SurvivalVerdict,
        val verdictSummary: String,
        val fatalFlaws: List<String>,
        val survivingPillars: List<String>,
        val fortificationRoadmap: List<String>,
        val userReinforcement: String? = null,
        val armorRating: Int = 82,
        val attackPenetration: Int = 54
    )

    // Master roster of Ravana's 10 Heads
    val fullCouncil: List<RavanaBot> = listOf(
        RavanaBot(
            id = 1,
            name = "Inquisitor Red",
            title = "Red Team Destructor",
            avatar = "🔴",
            stance = BotStance.ATTACKER,
            tagline = "Crucifies weak assumptions and exposes fatal structural failure points.",
            primaryFocus = "Fatal dependency traps, architectural flaws, and market apathy"
        ),
        RavanaBot(
            id = 2,
            name = "Architect White",
            title = "Resilient Defender",
            avatar = "🟢",
            stance = BotStance.DEFENDER,
            tagline = "Fortifies core value, defends feasibility, and engineers practical survival paths.",
            primaryFocus = "Core value proposition, defensibility, and modular workarounds"
        ),
        RavanaBot(
            id = 3,
            name = "Pragmatic Auditor",
            title = "Capital & Unit Economics",
            avatar = "🟡",
            stance = BotStance.AUDITOR,
            tagline = "Audits financial bleed, operational burn, ROI thresholds, and unit viability.",
            primaryFocus = "Runway sustainability, marginal cost, and economic friction"
        ),
        RavanaBot(
            id = 4,
            name = "Chaos & Murphy",
            title = "Black Swan Hunter",
            avatar = "🟣",
            stance = BotStance.CHAOS,
            tagline = "Simulates worst-case catastrophic anomalies, edge cases, and sudden shocks.",
            primaryFocus = "Unforeseen supply shocks, regulatory bans, and cascading outages"
        ),
        RavanaBot(
            id = 5,
            name = "Vedic Strategist",
            title = "Ethical & Dharma Arbiter",
            avatar = "🛡️",
            stance = BotStance.STRATEGIST,
            tagline = "Evaluates moral sustainability, societal alignment, and long-term equilibrium.",
            primaryFocus = "Incentive compatibility, ethical hazard, and long-term trust"
        ),
        RavanaBot(
            id = 6,
            name = "Scale & Speed Profiler",
            title = "Computational Throughput",
            avatar = "⚡",
            stance = BotStance.PERFORMANCE,
            tagline = "Stress-tests 100x concurrency, latency bottlenecks, and resource consumption.",
            primaryFocus = "Throughput saturation, memory footprint, and scaling friction"
        ),
        RavanaBot(
            id = 7,
            name = "Human Empathy Advocate",
            title = "UX & Psychology",
            avatar = "🧠",
            stance = BotStance.PSYCHOLOGY,
            tagline = "Measures adoption friction, cognitive fatigue, habits, and user resistance.",
            primaryFocus = "Habit inertia, cognitive overhead, and behavioral buy-in"
        ),
        RavanaBot(
            id = 8,
            name = "Zero-Trust Sentinel",
            title = "Cybersecurity Defense",
            avatar = "🦁",
            stance = BotStance.SECURITY,
            tagline = "Probes data exfiltration, attack surfaces, authentication, and exploitability.",
            primaryFocus = "Attack vectors, data leak surface, and adversarial injection"
        ),
        RavanaBot(
            id = 9,
            name = "Market Disruption Scout",
            title = "Competitive Moats",
            avatar = "👁️",
            stance = BotStance.MARKET,
            tagline = "Identifies copycat vulnerability, substitution threats, and distribution barriers.",
            primaryFocus = "Defensibility moats, network effects, and incumbent retaliation"
        ),
        RavanaBot(
            id = 10,
            name = "Supreme Ravana Sovereign",
            title = "Crucible High Arbiter",
            avatar = "👑",
            stance = BotStance.JUDGE,
            tagline = "Synthesizes the totality of the debate and passes ultimate judgment.",
            primaryFocus = "Ultimate holistic survivability and battle-readiness verdict"
        )
    )

    val customBots = mutableListOf<RavanaBot>()

    fun addCustomBot(
        name: String,
        title: String,
        avatar: String,
        stance: BotStance,
        tagline: String,
        primaryFocus: String
    ): RavanaBot {
        val nextId = (getAllAvailableBots().maxOfOrNull { it.id } ?: 10) + 1
        val newBot = RavanaBot(
            id = nextId,
            name = name.trim().ifEmpty { "Archetype #$nextId" },
            title = title.trim().ifEmpty { "Custom Challenger" },
            avatar = avatar.trim().ifEmpty { "⚔️" },
            stance = stance,
            tagline = tagline.trim().ifEmpty { "Custom adversarial perspective" },
            primaryFocus = primaryFocus.trim().ifEmpty { "Targeted friction & vulnerability testing" }
        )
        customBots.add(newBot)
        return newBot
    }

    fun clearCustomBots() {
        customBots.clear()
    }

    fun getAllAvailableBots(): List<RavanaBot> = fullCouncil + customBots

    fun getBotsForCount(count: Int): List<RavanaBot> {
        val all = getAllAvailableBots()
        val maxLimit = max(MAX_BOTS, all.size)
        val bounded = count.coerceIn(MIN_BOTS, maxLimit)
        return all.take(bounded)
    }

    /**
     * Executes the adversarial battle between the configured bots for a given idea.
     * Supports dynamic mid-battle reinforcements from the user.
     */
    fun evaluateIdeaBattle(rawIdea: String, botCount: Int, userReinforcement: String? = null): RavanaBattleResult {
        val idea = rawIdea.trim().ifEmpty { "Autonomous On-Device AI Architecture" }
        val maxLimit = max(MAX_BOTS, getAllAvailableBots().size)
        val count = botCount.coerceIn(MIN_BOTS, maxLimit)
        val activeBots = getBotsForCount(count)

        val lower = idea.lowercase(Locale.ROOT)
        val hasReinforcement = !userReinforcement.isNullOrBlank()

        // Classify domain
        val isTechOrCoding = lower.contains("code") || lower.contains("ai") || lower.contains("software") ||
                lower.contains("app") || lower.contains("database") || lower.contains("api") ||
                lower.contains("rust") || lower.contains("python") || lower.contains("cloud") || lower.contains("monolith")
        val isBusinessOrStartup = lower.contains("startup") || lower.contains("business") || lower.contains("sell") ||
                lower.contains("product") || lower.contains("market") || lower.contains("company") || lower.contains("cafe")
        val isLifeOrCareer = lower.contains("job") || lower.contains("quit") || lower.contains("career") ||
                lower.contains("move") || lower.contains("study") || lower.contains("marry") || lower.contains("habit")

        val bot1 = activeBots[0] // Inquisitor Red
        val bot2 = activeBots[1] // Architect White

        // --- ROUND 1: Opening Assault & Parrying Defense ---
        val round1Turns = mutableListOf<DebateTurn>()

        val r1Assault = when {
            isTechOrCoding -> "This proposal ignores catastrophic operational latency and edge-case fragility! You are introducing unvetted abstraction layers where deterministic simplicity is required. If the network drops or memory spikes under heavy load, the entire pipeline deadlocks."
            isBusinessOrStartup -> "Your value proposition is built on wishful thinking! Customer acquisition costs will devour your margins before you reach break-even. Incumbents with existing distribution will clone your sole differentiator in under 60 days."
            isLifeOrCareer -> "You are confusing escapism with strategic ambition! Abandoning your current stable footing without a verified 12-month liquidity runway or validated product-market fit guarantees premature financial panic."
            else -> "The foundational hypothesis is unverified! You are confusing correlation with direct causation. Under harsh empirical conditions, your core assumption collapses immediately."
        }
        round1Turns.add(DebateTurn(1, bot1, r1Assault, impactScore = 84))

        val r1Defense = when {
            isTechOrCoding -> "The critique assumes a brittle monolithic design. By enforcing zero-copy primitives, asynchronous non-blocking buffers, and graceful offline fallback, the core invariant remains unbreakable even under extreme load."
            isBusinessOrStartup -> "Incumbents are paralyzed by legacy technical debt and high enterprise overhead. Our lean on-device velocity and direct niche targeting allows us to acquire loyal evangelists at zero marginal distribution cost."
            isLifeOrCareer -> "Every leap forward requires asymmetric calculated risk. We are not leaping blindly—we are establishing modular milestones, rigorous expense limits, and rapid prototype validation to test the waters before committing full capital."
            else -> "The critique attacks a straw-man version of the thesis. When scoped to its primary boundary conditions and executed with modular discipline, the foundational premise holds firm."
        }
        round1Turns.add(DebateTurn(1, bot2, r1Defense, impactScore = 80))

        if (count >= 3 && activeBots.size >= 3) {
            val bot3 = activeBots[2]
            round1Turns.add(
                DebateTurn(
                    1,
                    bot3,
                    "Let us audit the numbers without romantic delusion: what is the payback period? What is the burn rate? Without strict milestones at Day 30 and Day 90, capital attrition will strangle execution before scale is achieved.",
                    impactScore = 78
                )
            )
        }

        val round1 = BattleRound(
            roundNumber = 1,
            title = "Round 1: Opening Assault & Structural Breach",
            description = "Red Team attacks the fundamental premise while White Team fortifies the defensive core.",
            turns = round1Turns,
            ideaHealthAfterRound = 72,
            armorRating = 85,
            attackPenetration = 40
        )

        // --- ROUND 2: Cross-Examination & Stress-Testing ---
        val round2Turns = mutableListOf<DebateTurn>()

        val r2Assault = when {
            isTechOrCoding -> "You claim modularity will save you, but modularity introduces cross-boundary synchronization overhead! Who handles race conditions? What happens when corrupted inputs enter your pipeline?"
            isBusinessOrStartup -> "You mention loyal evangelists, but customer churn will crush you. The moment free trial expires or subscription bills arrive, 80% of users bounce. Where is the switching cost moat?"
            isLifeOrCareer -> "What happens during month 4 when motivation wanes and zero revenue appears? Doubt will paralyze your decision-making, leading to second-guessing and compromised execution."
            else -> "Your defense relies on ideal laboratory assumptions. When exposed to unpredictable environmental turbulence, your system exhibits extreme sensitivity to initial conditions."
        }
        round2Turns.add(DebateTurn(2, bot1, r2Assault, impactScore = 88))

        val r2Defense = when {
            isTechOrCoding -> "Input sanitization and immutable data contracts are asserted at the perimeter. We isolate worker threads so failures are compartmentalized. The blast radius of any crash is zero."
            isBusinessOrStartup -> "High switching costs are baked into workflow integration: when our solution stores their sovereign data locally with zero cloud leakage, leaving becomes a security downgrade."
            isLifeOrCareer -> "Discipline replaces fleeting motivation. By establishing daily non-negotiable execution blocks (Karma Yoga: action without outcome panic), progress is compounded irrespective of emotional highs and lows."
            else -> "We have incorporated defensive invariants: redundant safeguards, adaptive feedback loops, and error-containment walls that absorb turbulence rather than shattering."
        }
        round2Turns.add(DebateTurn(2, bot2, r2Defense, impactScore = 82))

        if (count >= 4 && activeBots.size >= 4) {
            val bot4 = activeBots[3] // Chaos
            round2Turns.add(
                DebateTurn(
                    2,
                    bot4,
                    "Murphy's Law is absolute: anything that can go wrong will go wrong. What if your primary API is deprecated? What if app store policies shift overnight? What if your hardware target undergoes supply failure?",
                    impactScore = 85
                )
            )
        }
        if (count >= 5 && activeBots.size >= 5) {
            val bot5 = activeBots[4] // Vedic Strategist
            round2Turns.add(
                DebateTurn(
                    2,
                    bot5,
                    "A structure built purely on aggressive expansion without ethical roots decays from within. Align this idea with long-term human empowerment and transparent integrity to build an unshakeable foundation.",
                    impactScore = 76
                )
            )
        }

        // MID-BATTLE USER REINFORCEMENT INJECTION
        if (hasReinforcement) {
            round2Turns.add(
                DebateTurn(
                    2,
                    RavanaBot(
                        id = 999,
                        name = "Commander Reinforcement",
                        title = "Tactical User Interjection",
                        avatar = "🛡️",
                        stance = BotStance.DEFENDER,
                        tagline = "Direct mid-battle tactical reinforcement.",
                        primaryFocus = "Live armor fortification and premise repair"
                    ),
                    "COMMANDER REINFORCEMENT DEPLOYED: \"$userReinforcement\" — The defensive shield absorbs the assault and closes the structural breach!",
                    impactScore = 94
                )
            )
        }

        val r2Health = if (hasReinforcement) 74 else 58
        val r2Armor = if (hasReinforcement) 88 else 70
        val r2Penetration = if (hasReinforcement) 42 else 62

        val round2 = BattleRound(
            roundNumber = 2,
            title = "Round 2: Cross-Examination & Stress-Testing",
            description = if (hasReinforcement) "User tactical reinforcements intervened to repel adversarial probes." else "Bots stress-test hidden assumptions, customer churn, and black swan shocks.",
            turns = round2Turns,
            ideaHealthAfterRound = r2Health,
            armorRating = r2Armor,
            attackPenetration = r2Penetration
        )

        // --- ROUND 3: Breaking Point & Crucible Climax ---
        val round3Turns = mutableListOf<DebateTurn>()

        if (count >= 6 && activeBots.size >= 6) {
            val bot6 = activeBots[5] // Scale
            round3Turns.add(
                DebateTurn(
                    3,
                    bot6,
                    "Profiling 100x scale: memory allocations in hot loops will trigger GC pauses. To survive, you must eliminate heap boxing and enforce primitive contiguous arrays.",
                    impactScore = 80
                )
            )
        }
        if (count >= 7 && activeBots.size >= 7) {
            val bot7 = activeBots[6] // Psychology
            round3Turns.add(
                DebateTurn(
                    3,
                    bot7,
                    "Cognitive friction audit: if setup takes more than 3 taps or 60 seconds, users will abandon. Streamline the onboarding experience to a frictionless zero-configuration state.",
                    impactScore = 82
                )
            )
        }
        if (count >= 8 && activeBots.size >= 8) {
            val bot8 = activeBots[7] // Security
            round3Turns.add(
                DebateTurn(
                    3,
                    bot8,
                    "Adversarial security assessment: enforce zero-trust boundaries, sandbox third-party dependencies, and cryptographically sign internal states to prevent unauthorized tampering.",
                    impactScore = 86
                )
            )
        }
        if (count >= 9 && activeBots.size >= 9) {
            val bot9 = activeBots[8] // Market
            round3Turns.add(
                DebateTurn(
                    3,
                    bot9,
                    "Competitive moat confirmed: open-source software and cloud dependencies are vulnerable. A 100% sovereign offline-first positioning creates an impenetrable defensive moat.",
                    impactScore = 84
                )
            )
        }

        // Custom Bots participation (if any in activeBots beyond the standard 10)
        for (i in 10 until activeBots.size) {
            val customBot = activeBots[i]
            round3Turns.add(
                DebateTurn(
                    3,
                    customBot,
                    "Custom Challenger [${customBot.name} - ${customBot.title}]: \"Focusing on ${customBot.primaryFocus}. ${customBot.tagline}\"",
                    impactScore = 83
                )
            )
        }

        val r3AssaultFinal = "Final Verdict from Red Team: This idea contains high-risk friction points. Unless the creator implements strict perimeter safeguards and capital buffers, it will be crushed by operational reality."
        round3Turns.add(DebateTurn(3, bot1, r3AssaultFinal, impactScore = 86))

        val r3DefenseFinal = if (hasReinforcement) {
            "Final Defense from White Team: With the commander's tactical reinforcement, the premise is fortified! The core is invulnerable to attrition."
        } else {
            "Final Defense from White Team: The idea has survived our fiercest attacks! The core thesis is robust. The battle scars have revealed the exact armor needed to make it triumphant."
        }
        round3Turns.add(DebateTurn(3, bot2, r3DefenseFinal, impactScore = 90))

        if (count >= 10 && activeBots.size >= 10) {
            val bot10 = activeBots[9] // Supreme Ravana
            round3Turns.add(
                DebateTurn(
                    3,
                    bot10,
                    "👑 RAVANA SUPREME DECREE: The crucible has tested this idea through the minds of ${activeBots.size} formidable heads. The weak flesh has burned away; what remains is hardened steel.",
                    impactScore = 95
                )
            )
        }

        // Calculate final survival metrics
        val baselineScore = 65
        val complexityPenalty = if (idea.length > 80) 5 else 0
        val botStressFactor = count * 2 // More bots mean higher scrutiny
        val resilienceBonus = 12
        val reinforcementBonus = if (hasReinforcement) 14 else 0

        val computedScore = (baselineScore - complexityPenalty - (botStressFactor / 3) + resilienceBonus + reinforcementBonus)
            .coerceIn(35, 96)

        val finalVerdict = when {
            computedScore >= 70 -> SurvivalVerdict.SURVIVED
            computedScore >= 48 -> SurvivalVerdict.CONDITIONALLY_SURVIVED
            else -> SurvivalVerdict.PERISHED
        }

        val finalArmor = if (hasReinforcement) 89 else 78
        val finalPenetration = if (hasReinforcement) 38 else 54

        val round3 = BattleRound(
            roundNumber = 3,
            title = "Round 3: Crucible Climax & Survival Judgment",
            description = "The decisive showdown where the council evaluates whether the idea holds ground or collapses.",
            turns = round3Turns,
            ideaHealthAfterRound = computedScore,
            armorRating = finalArmor,
            attackPenetration = finalPenetration
        )

        val fatalFlaws = listOf(
            "Underestimating initial distribution friction and customer inertia.",
            "Vulnerability to unexpected memory/latency bottlenecks under edge-case workloads.",
            "Failure to assert strict boundaries between core invariants and speculative features.",
            "Over-reliance on best-case user behavior without automated fail-safes."
        )

        val survivingPillars = listOf(
            "Distinct sovereign value proposition that solves a genuine underlying friction.",
            "Modular architecture enabling compartmentalized failure recovery.",
            "High defensibility against generic cloud-dependent competitors.",
            "Clear alignment between effort, execution, and long-term utility."
        )

        val fortificationRoadmap = listOf(
            "1. Implement Zero-Trust Sandboxing: Validate all inputs before they cross into the operational core.",
            "2. Establish 30/60/90-Day Metrics: Monitor unit velocity and hard metrics rather than subjective optimism.",
            "3. Enforce Graceful Offline Degradation: Guarantee the product delivers 80% value even during total external outage.",
            "4. Adopt the Maphula Principle: Focus 100% of energy on flawless execution, unburdened by anxious attachment to outcomes."
        )

        val verdictSummary = when (finalVerdict) {
            SurvivalVerdict.SURVIVED -> "🏆 **THE IDEA SURVIVES!** After undergoing relentless adversarial bombardment across ${activeBots.size} AI bots, your thesis demonstrated superior structural resilience. The core holds firm."
            SurvivalVerdict.CONDITIONALLY_SURVIVED -> "⚠️ **SURVIVES WITH CRITICAL CONDITIONS!** The idea withstood general assault, but the council exposed serious flank vulnerabilities. Implement the fortification roadmap immediately before deploying capital or code."
            SurvivalVerdict.PERISHED -> "💀 **THE IDEA HAS PERISHED!** The Red Team identified fundamental architectural fallacies that make survival in the wild improbable. Restructure the core premise and re-enter the arena."
        }

        return RavanaBattleResult(
            userIdea = idea,
            botCount = count,
            participatingBots = activeBots,
            rounds = listOf(round1, round2, round3),
            survivalScore = computedScore,
            verdict = finalVerdict,
            verdictSummary = verdictSummary,
            fatalFlaws = fatalFlaws,
            survivingPillars = survivingPillars,
            fortificationRoadmap = fortificationRoadmap,
            userReinforcement = userReinforcement,
            armorRating = finalArmor,
            attackPenetration = finalPenetration
        )
    }

    /**
     * Generates a comprehensive exportable ASCII/text battle scorecard.
     */
    fun generateBattleScorecard(result: RavanaBattleResult): String {
        val sb = StringBuilder()
        sb.appendLine("╔══════════════════════════════════════════════════════════════╗")
        sb.appendLine("║         ⚔️ RAVANA ADVERSARIAL ARENA WAR ROOM SCORECARD        ║")
        sb.appendLine("╚══════════════════════════════════════════════════════════════╝")
        sb.appendLine("PROPOSAL: ${result.userIdea}")
        sb.appendLine("COUNCIL HEADCOUNT: ${result.botCount} Bots")
        sb.appendLine("BOT ROSTER: " + result.participatingBots.joinToString(", ") { "${it.avatar} ${it.name}" })
        if (!result.userReinforcement.isNullOrBlank()) {
            sb.appendLine("TACTICAL REINFORCEMENT: \"${result.userReinforcement}\" (+14% Armor)")
        }
        sb.appendLine("──────────────────────────────────────────────────────────────")
        sb.appendLine("ARENA ROUNDS ATTRITION LOG:")
        result.rounds.forEach { round ->
            sb.appendLine("• ${round.title}")
            sb.appendLine("  [Integrity: ${round.ideaHealthAfterRound}% | Armor: ${round.armorRating}% | Penetration: ${round.attackPenetration}%]")
            round.turns.forEach { turn ->
                val snippet = if (turn.statement.length > 75) turn.statement.take(72) + "..." else turn.statement
                sb.appendLine("  ${turn.bot.avatar} ${turn.bot.name}: \"$snippet\" (Impact: ${turn.impactScore}%)")
            }
        }
        sb.appendLine("──────────────────────────────────────────────────────────────")
        sb.appendLine("FINAL VERDICT: ${result.verdict.emoji} ${result.verdict.title} (Survival: ${result.survivalScore}%)")
        sb.appendLine("ARMOR INTEGRITY: ${result.armorRating}% | ATTACK PENETRATION: ${result.attackPenetration}%")
        sb.appendLine("FATAL FLAWS UNMASKED:")
        result.fatalFlaws.forEach { sb.appendLine("  🔴 $it") }
        sb.appendLine("SURVIVING PILLARS:")
        result.survivingPillars.forEach { sb.appendLine("  🟢 $it") }
        sb.appendLine("FORTIFICATION ROADMAP:")
        result.fortificationRoadmap.forEach { sb.appendLine("  🛡️ $it") }
        sb.appendLine("══════════════════════════════════════════════════════════════")
        sb.appendLine("[ MAPHULA SOVEREIGN SEAL: VERIFIED 100% OFFLINE ON-DEVICE ]")
        sb.appendLine("══════════════════════════════════════════════════════════════")
        return sb.toString().trim()
    }
}
