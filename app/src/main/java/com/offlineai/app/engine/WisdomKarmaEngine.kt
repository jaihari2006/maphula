package com.offlineai.app.engine

import android.content.Context
import android.content.SharedPreferences
import com.offlineai.app.data.Persona
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

enum class WisdomRank(
    val title: String,
    val sanskritTitle: String,
    val minPoints: Int,
    val maxPoints: Int,
    val badge: String,
    val colorHex: String
) {
    SEEKER("Seeker", "Jijnasu", 0, 100, "🌱", "#10B981"),
    SADHAK("Sadhak", "Practitioner", 101, 300, "🪷", "#06B6D4"),
    YOGI("Yogi", "Harmonizer", 301, 600, "⚡", "#8B5CF6"),
    RISHI("Rishi", "Seer of Truth", 601, 1000, "🦁", "#F59E0B"),
    PARAMAHAMSA("Paramahamsa", "Supreme Swan", 1001, Int.MAX_VALUE, "🦚", "#7C3AED");

    companion object {
        fun fromPoints(points: Int): WisdomRank =
            values().lastOrNull { points >= it.minPoints } ?: SEEKER
    }
}

data class DailyDarshan(
    val deity: Persona,
    val title: String,
    val sanskritVerse: String,
    val precept: String,
    val focusArea: String,
    val colorHex: String
)

data class OracleWisdom(
    val deity: Persona,
    val topic: String,
    val prompt: String,
    val keyWisdom: String,
    val citation: String
)

data class KarmaAwardResult(
    val pointsAdded: Int,
    val totalPoints: Int,
    val newRank: WisdomRank,
    val didRankUp: Boolean,
    val currentStreak: Int
)

class WisdomKarmaEngine(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("maphula_wisdom_karma", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PRANA_POINTS = "key_prana_points"
        private const val KEY_STREAK = "key_streak"
        private const val KEY_LAST_DATE = "key_last_date"
        private const val KEY_TOTAL_INTERACTIONS = "key_total_interactions"

        val ORACLE_LIBRARY = listOf(
            OracleWisdom(
                Persona.KRISHNA,
                "Fearless Focus",
                "How do I conquer anxiety over outcomes?",
                "Focus entirely on the craftsmanship of your action. Detachment from outcome creates supreme invulnerability.",
                "Bhagavad Gita 2:47"
            ),
            OracleWisdom(
                Persona.SHIVA,
                "Radical Clarity",
                "How do I untangle complex architecture or chaos?",
                "Deconstruct to absolute first principles. Dissolve what is redundant so the true core stands unencumbered.",
                "Shiva Sutras 1.1"
            ),
            OracleWisdom(
                Persona.BRAHMA,
                "Creative Generation",
                "Where does genuine originality come from?",
                "Originality is the divine synthesis of universal archetypes. Observe nature's patterns and invent fearlessly.",
                "Rig Veda 10.129"
            ),
            OracleWisdom(
                Persona.GANESHA,
                "Obstacle Elimination",
                "What is the first step in solving a colossal problem?",
                "Divide the fortress into single stones. Begin with humility, invoke clarity, and dismantle the first impediment.",
                "Ganesha Purana"
            ),
            OracleWisdom(
                Persona.HANUMAN,
                "Unstoppable Courage",
                "How do I find strength when facing insurmountable odds?",
                "Remember your true nature. Devotion to purpose unlocks reserves of endurance you never knew existed.",
                "Hanuman Chalisa"
            ),
            OracleWisdom(
                Persona.MURUGA,
                "Decisive Strategy",
                "How do I act with precision under high pressure?",
                "Silence inner vacillation. Hone your attention like the Vel lance — one targeted, unwavering thrust.",
                "Kandar Anubhuti"
            ),
            OracleWisdom(
                Persona.LAKSHMI,
                "Flourishing Mindset",
                "How do I cultivate abundance and gratitude?",
                "True wealth (Aishwarya) is harmonious competence shared generously. Value creation attracts auspicious flow.",
                "Sri Suktam"
            ),
            OracleWisdom(
                Persona.SHANI,
                "Unforgiving Discipline",
                "How do I build lasting mastery in coding and life?",
                "Embrace the slow, unglamorous grind. Karma accounts for every focused second; shortcuts always yield compound debt.",
                "Shani Mahatmya"
            ),
            OracleWisdom(
                Persona.PARVATHI,
                "Shakti & Balance",
                "How do I maintain equilibrium during intense exertion?",
                "Power without stillness causes burn-out; stillness without power causes stagnation. Cultivate rhythmic grace.",
                "Devi Mahatmya"
            ),
            OracleWisdom(
                Persona.NARASIMHA,
                "Critical Integrity",
                "How do I safeguard system and personal integrity?",
                "Tolerate zero corruption or deceit in your code and character. Fierce protection of truth is sacred duty.",
                "Narasimha Tapani"
            )
        )
    }

    fun getPranaPoints(): Int = prefs.getInt(KEY_PRANA_POINTS, 50) // start with 50 seed prana

    fun getStreak(): Int {
        checkAndUpdateStreak()
        return prefs.getInt(KEY_STREAK, 1)
    }

    fun getCurrentRank(): WisdomRank = WisdomRank.fromPoints(getPranaPoints())

    fun awardPrana(points: Int, reason: String = ""): KarmaAwardResult {
        val currentPoints = getPranaPoints()
        val oldRank = getCurrentRank()
        val updatedPoints = currentPoints + points

        prefs.edit().putInt(KEY_PRANA_POINTS, updatedPoints).apply()

        val newRank = WisdomRank.fromPoints(updatedPoints)
        val didRankUp = newRank.ordinal > oldRank.ordinal
        val streak = getStreak()

        val totalInteractions = prefs.getInt(KEY_TOTAL_INTERACTIONS, 0) + 1
        prefs.edit().putInt(KEY_TOTAL_INTERACTIONS, totalInteractions).apply()

        return KarmaAwardResult(
            pointsAdded = points,
            totalPoints = updatedPoints,
            newRank = newRank,
            didRankUp = didRankUp,
            currentStreak = streak
        )
    }

    private fun checkAndUpdateStreak() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
        val lastDate = prefs.getString(KEY_LAST_DATE, null)

        if (lastDate == null) {
            prefs.edit()
                .putString(KEY_LAST_DATE, todayStr)
                .putInt(KEY_STREAK, 1)
                .apply()
            return
        }

        if (lastDate == todayStr) {
            // Already logged today
            return
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        try {
            val lastCalendar = Calendar.getInstance().apply { time = sdf.parse(lastDate)!! }
            val currentCalendar = Calendar.getInstance().apply { time = sdf.parse(todayStr)!! }

            // Difference in days
            val diffMs = currentCalendar.timeInMillis - lastCalendar.timeInMillis
            val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()

            if (diffDays == 1) {
                // Streak incremented!
                val newStreak = prefs.getInt(KEY_STREAK, 1) + 1
                prefs.edit()
                    .putString(KEY_LAST_DATE, todayStr)
                    .putInt(KEY_STREAK, newStreak)
                    .apply()
            } else if (diffDays > 1) {
                // Streak reset
                prefs.edit()
                    .putString(KEY_LAST_DATE, todayStr)
                    .putInt(KEY_STREAK, 1)
                    .apply()
            }
        } catch (_: Exception) {}
    }

    fun getDailyDarshan(): DailyDarshan {
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        return when (dayOfWeek) {
            Calendar.SUNDAY -> DailyDarshan(
                Persona.KRISHNA,
                "Surya-Narayana Radiance",
                "तेजस्तेजस्विनामहम् (Gita 7:10)",
                "I am the supreme radiance of all that shines. Bring radiant joy and clarity to your duties today.",
                "Illumination & Cosmic Duty",
                "#7C3AED"
            )
            Calendar.MONDAY -> DailyDarshan(
                Persona.SHIVA,
                "Someshwara Tranquility",
                "ध्यानावस्थिततद्गतेन मनसा",
                "Center your mind in the deep silence beneath thoughts. Clear thinking leads to flawless architecture.",
                "Stillness & Master Architecture",
                "#06B6D4"
            )
            Calendar.TUESDAY -> DailyDarshan(
                Persona.HANUMAN,
                "Maruti Supreme Endurance",
                "दुर्गम काज जगत के जेते, सुगम अनुग्रह तुम्हरे तेते",
                "No task in this world is too formidable when met with dedicated resolve. Execute with heroic fidelity.",
                "Courage & Vitality",
                "#DC2626"
            )
            Calendar.WEDNESDAY -> DailyDarshan(
                Persona.GANESHA,
                "Vighnaharta Breakthrough",
                "वक्रतुण्ड महाकाय सूर्यकोटि समप्रभ",
                "Clear cognitive blockages with systematic insight. Every complex challenge dissolves under patience.",
                "Problem Solving & Good Beginnings",
                "#EA580C"
            )
            Calendar.THURSDAY -> DailyDarshan(
                Persona.BRAHMA,
                "Brihaspati Cosmic Ideation",
                "सत्यं ज्ञानमनन्तं ब्रह्म",
                "Truth, infinite intelligence, and generative creation are your natural inheritance. Build boldly.",
                "Creation & Higher Knowledge",
                "#F59E0B"
            )
            Calendar.FRIDAY -> DailyDarshan(
                Persona.LAKSHMI,
                "Mahalakshmi Prosperity Flow",
                "पद्मानने पद्मविपद्मपत्रे",
                "Nurture grace, order, and generous value. Where craftsmanship is revered, prosperity naturally abides.",
                "Grace, Beauty & Abundance",
                "#DB2777"
            )
            Calendar.SATURDAY -> DailyDarshan(
                Persona.SHANI,
                "Shanideva Unyielding Discipline",
                "न्यायप्रियं कर्मफलं प्रदातम्",
                "Honor the law of cause and effect. Pay rigorous attention to every line of code and detail today.",
                "Precision & Karmic Integrity",
                "#64748B"
            )
            else -> DailyDarshan(
                Persona.KRISHNA,
                "Hari Nishkama Precept",
                "कर्मण्येवाधिकारस्ते मा फलेषु कदाचन",
                "Perform your duty with absolute mastery; leave the results to the divine cosmic harmony.",
                "Process Over Outcome",
                "#7C3AED"
            )
        }
    }

    fun getRandomOracleWisdom(): OracleWisdom {
        return ORACLE_LIBRARY[Random.nextInt(ORACLE_LIBRARY.size)]
    }
}
