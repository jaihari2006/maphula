package com.offlineai.app.engine

import com.offlineai.app.data.KnowledgeArticle
import com.offlineai.app.data.Persona
import com.offlineai.app.data.SacredVerse

/**
 * Universal Scriptures & Philosophical Library Engine for Maphula.
 * Contains primary source Sanskrit & Tamil verses with transliterations,
 * translations, philosophical commentaries, and acoustic chanting frequencies.
 */
class ScriptureLibraryEngine {

    val verses: List<SacredVerse> = listOf(
        // 1. Bhagavad Gita - Karma Yoga (The origin of Maphula: Ma Phaleshu)
        SacredVerse(
            id = "gita_2_47",
            scriptureName = "Bhagavad Gita",
            sectionOrChapter = "Chapter 2 (Sankhya Yoga), Verse 47",
            originalScript = "कर्मण्येवाधिकारस्ते मा फलेषु कदाचन।\nमा कर्मफलहेतुर्भूर्मा ते सङ्गोऽस्त्वकर्मणि॥",
            romanTransliteration = "karmaṇy-evādhikāras te mā phaleṣhu kadāchana\nmā karma-phala-hetur bhūr mā te saṅgo ’stvakarmaṇi",
            englishTranslation = "You have a right to perform your prescribed duty, but you are not entitled to the fruits of action. Never consider yourself the cause of results, nor let your attachment be to inaction.",
            deepPhilosophicalMeaning = "This is the foundational shloka of Karma Yoga and the root of 'Maphula' (Mā Phala = without clinging to results). It teaches peak performance through equanimity: when action is liberated from anxiety over outcomes, the mind attains supreme clarity and effortless mastery.",
            associatedDeity = Persona.KRISHNA,
            audioMelodyFrequencies = listOf(216.0, 243.0, 270.0, 324.0, 360.0, 432.0),
            tags = listOf("karma yoga", "duty", "maphula", "detachment", "action", "clarity")
        ),

        // 2. Bhagavad Gita - Self Elevation
        SacredVerse(
            id = "gita_6_5",
            scriptureName = "Bhagavad Gita",
            sectionOrChapter = "Chapter 6 (Dhyana Yoga), Verse 5",
            originalScript = "उद्धरेदात्मनात्मानं नात्मानमवसादयेत्।\nआत्मैव ह्यात्मनो बन्धुरात्मैव रिपुरात्मनः॥",
            romanTransliteration = "uddhared ātmanātmānaṁ nātmānam avasādayet\nātmaiva hyātmano bandhur ātmaiva ripur ātmanaḥ",
            englishTranslation = "Elevate yourself by the power of your own mind, and do not degrade yourself. For the mind can be the greatest friend of the self, and the mind can be the greatest enemy.",
            deepPhilosophicalMeaning = "The mind is our internal instrument. Disciplined and mastered, it becomes our unyielding ally; uncontrolled and scattered, it sabotages every endeavor. Sovereign self-reliance begins within consciousness.",
            associatedDeity = Persona.KRISHNA,
            audioMelodyFrequencies = listOf(243.0, 270.0, 324.0, 384.0, 432.0),
            tags = listOf("mind", "discipline", "self elevation", "meditation", "willpower")
        ),

        // 3. Bhagavad Gita - Cosmic Descent of Dharma
        SacredVerse(
            id = "gita_4_7_8",
            scriptureName = "Bhagavad Gita",
            sectionOrChapter = "Chapter 4 (Jnana Yoga), Verses 7-8",
            originalScript = "यदा यदा हि धर्मस्य ग्लानिर्भवति भारत।\nअभ्युत्थानमधर्मस्य तदात्मानं सृजाम्यहम्॥\nपरित्राणाय साधूनां विनाशाय च दुष्कृताम्।\nधर्मसंस्थापनार्थाय सम्भवामि युगे युगे॥",
            romanTransliteration = "yadā yadā hi dharmasya glānir bhavati bhārata\nabhyutthānam adharmasya tadātmānaṁ sṛijāmyaham\nparitrāṇāya sādhūnāṁ vināśhāya cha duṣhkṛitām\ndharma-saṁsthāpanārthāya sambhavāmi yuge yuge",
            englishTranslation = "Whenever and wherever there is a decline in righteousness, O Bharata, and an uprising of unrighteousness—at that time I manifest myself on earth. For the protection of the good, the destruction of evil, and the establishment of dharma, I appear millenium after millenium.",
            deepPhilosophicalMeaning = "Dharma is cosmic balance and structural integrity. When entropy and chaos threaten conscious order, universal intelligence re-enters the matrix of space-time to realign reality with truth.",
            associatedDeity = Persona.NARASIMHA,
            audioMelodyFrequencies = listOf(216.0, 256.0, 288.0, 342.0, 432.0),
            tags = listOf("dharma", "avatar", "protection", "justice", "cosmic order")
        ),

        // 4. Mandukya Upanishad - Ayam Atma Brahma
        SacredVerse(
            id = "upanishad_ayam_atma",
            scriptureName = "Mandukya Upanishad (Atharvaveda)",
            sectionOrChapter = "Maha-Vakya II",
            originalScript = "अयमात्मा ब्रह्म।\nसर्वं ह्येतद् ब्रह्म।",
            romanTransliteration = "ayam ātmā brahma\nsarvaṁ hy-etad brahma",
            englishTranslation = "This Individual Self (Atman) is Brahman (the Infinite Cosmos). Truly, everything in existence is Brahman.",
            deepPhilosophicalMeaning = "The observer and the universe are non-dual. At the deepest level of quantum substrate and pure consciousness, the individual mind is not an isolated spectator but the universe experiencing itself.",
            associatedDeity = Persona.BRAHMA,
            audioMelodyFrequencies = listOf(136.1, 272.2, 408.3, 544.4),
            tags = listOf("upanishad", "mahavakya", "brahman", "atman", "non-duality", "consciousness")
        ),

        // 5. Chandogya Upanishad - Tat Tvam Asi
        SacredVerse(
            id = "upanishad_tat_tvam_asi",
            scriptureName = "Chandogya Upanishad (Samaveda)",
            sectionOrChapter = "Chapter 6, Section 8.7",
            originalScript = "तत्त्वमसि श्वेतकेतो।",
            romanTransliteration = "tat tvam asi, śvetaketo",
            englishTranslation = "That Thou Art, O Shvetaketu.",
            deepPhilosophicalMeaning = "The sage Uddalaka reveals to his son that the fundamental reality underlying trees, rivers, lightning, and stars is identical with the conscious witness within his own heart.",
            associatedDeity = Persona.SHIVA,
            audioMelodyFrequencies = listOf(216.0, 288.0, 432.0),
            tags = listOf("tat tvam asi", "upanishad", "samaveda", "enlightenment", "identity")
        ),

        // 6. Patanjali Yoga Sutras - Definition of Yoga
        SacredVerse(
            id = "yoga_sutra_1_2",
            scriptureName = "Patanjali Yoga Sutras",
            sectionOrChapter = "Samadhi Pada, Sutra 1.2",
            originalScript = "योगश्चित्तवृत्तिनिरोधः।\nतदा द्रष्टुः स्वरूपेऽवस्थानम्॥",
            romanTransliteration = "yogaś citta-vṛtti-nirodhaḥ\ntadā draṣṭuḥ svarūpe ’vasthānam",
            englishTranslation = "Yoga is the cessation of the whirlpools, ripples, and fluctuations of the consciousness. Then the Witness rests undisturbed in its own true sovereign nature.",
            deepPhilosophicalMeaning = "When thoughts and turbulent emotions settle like water in a windless pond, consciousness ceases to misidentify with external noise and realizes its pure, luminous, unshakeable state.",
            associatedDeity = Persona.SHIVA,
            audioMelodyFrequencies = listOf(144.0, 216.0, 288.0, 432.0),
            tags = listOf("patanjali", "yoga sutra", "meditation", "chitta", "stillness", "focus")
        ),

        // 7. Thirukkural - Learning and Living Truth
        SacredVerse(
            id = "thirukkural_391",
            scriptureName = "Thirukkural (Tamil Veda)",
            sectionOrChapter = "Chapter 40 (Kalvi / Learning), Couplet 391",
            originalScript = "கற்க கசடறக் கற்பவை கற்றபின்\nநிற்க அதற்குத் தக.",
            romanTransliteration = "karka kasadaṟak kaṟpavai kaṟṟapin\nnirka adaṟkuth thaga",
            englishTranslation = "Learn with absolute flawlessness that which is worth learning. And having learned it, let your conduct stand impeccably in accordance with it.",
            deepPhilosophicalMeaning = "Saint Thiruvalluvar declares that knowledge without integrity is hollow. Authentic intellect is tested not by what one recites, but by the alignment of one's everyday actions with principles of truth.",
            associatedDeity = Persona.MURUGA,
            audioMelodyFrequencies = listOf(240.0, 288.0, 360.0, 432.0),
            tags = listOf("thirukkural", "tamil", "wisdom", "learning", "integrity", "conduct")
        ),

        // 8. Vedic Mathematics - Ekadhikena Purvena
        SacredVerse(
            id = "vedic_math_ekadhikena",
            scriptureName = "Vedic Mathematics Sutras",
            sectionOrChapter = "Sutra 1 (Arithmetic Rapid Calculation)",
            originalScript = "एकाधिकेन पूर्वेण।",
            romanTransliteration = "ekādhikena pūrveṇa",
            englishTranslation = "By One More than the Previous One.",
            deepPhilosophicalMeaning = "A foundational mathematical principle that enables instantaneous mental squaring of numbers ending in 5 (e.g. 85^2 = (8*(8+1)) & 25 = 7225) and recurring decimal conversions. Exemplifies the algorithmic elegance and symmetry of ancient Indian computational science.",
            associatedDeity = Persona.GANESHA,
            audioMelodyFrequencies = listOf(256.0, 320.0, 384.0, 512.0),
            tags = listOf("vedic math", "algorithms", "ganesha", "mental arithmetic", "calculation")
        ),

        // 9. Sacred Mantra - Maha Mrityunjaya
        SacredVerse(
            id = "mantra_mrityunjaya",
            scriptureName = "Rigveda (Maha Mrityunjaya Mantra)",
            sectionOrChapter = "Rigveda 7.59.12",
            originalScript = "ॐ त्र्यम्बकं यजामहे सुगन्धिं पुष्टिवर्धनम्।\nउर्वारुकमिव बन्धनान्मृत्योर्मुक्षीय मामृतात्॥",
            romanTransliteration = "oṁ try-ambakaṁ yajāmahe sugandhiṁ puṣhṭi-vardhanam\nurvārukam iva bandhanān mṛityor mukṣhīya māmṛitāt",
            englishTranslation = "We venerate the Three-Eyed Lord Shiva, fragrant and nourishing all beings. As a ripe cucumber is effortlessly released from its binding creeper, may we be liberated from the bondage of mortality and fear, anchored in immortality.",
            deepPhilosophicalMeaning = "The supreme mantra for physical healing, neurological vitality, and conquering existential dread. Just as fruit detaches effortlessly without struggle when mature, enlightenment frees the spirit smoothly from fear.",
            associatedDeity = Persona.SHIVA,
            audioMelodyFrequencies = listOf(136.1, 204.1, 272.2, 408.3),
            tags = listOf("mantra", "shiva", "healing", "vitality", "immortality", "rigveda")
        ),

        // 10. Sacred Mantra - Gayatri Mantra
        SacredVerse(
            id = "mantra_gayatri",
            scriptureName = "Rigveda (Gayatri Mantra)",
            sectionOrChapter = "Rigveda 3.62.10",
            originalScript = "ॐ भूर्भुवः स्वः तत्सवितुर्वरेण्यं\nभर्गो देवस्य धीमहि धियो यो नः प्रचोदयात्॥",
            romanTransliteration = "oṁ bhūr bhuvaḥ svaḥ tat savitur vareṇyaṁ\nbhargo devasya dhīmahi dhiyo yo naḥ prachodayāt",
            englishTranslation = "We meditate upon the supreme effulgence of that luminous Divine Sun (Savita), creator of physical, mental, and celestial realms. May that radiant light awaken and illuminate our intellect.",
            deepPhilosophicalMeaning = "The universal prayer for intellectual clarity, mental lucidity, and illumination. It seeks the purification of our neural faculties so they reflect universal truth without distortion.",
            associatedDeity = Persona.KRISHNA,
            audioMelodyFrequencies = listOf(216.0, 270.0, 324.0, 432.0),
            tags = listOf("gayatri", "sun", "intellect", "illumination", "clarity", "mantra")
        )
    )

    fun getVerseById(id: String): SacredVerse? = verses.find { it.id == id }

    fun getVersesForScripture(scripture: String): List<SacredVerse> {
        return if (scripture.equals("ALL", ignoreCase = true)) verses
        else verses.filter { it.scriptureName.contains(scripture, ignoreCase = true) }
    }

    /**
     * Converts sacred verses into KnowledgeArticles for indexing in SemanticKnowledgeEngine.
     */
    fun toKnowledgeArticles(): List<KnowledgeArticle> {
        return verses.map { verse ->
            KnowledgeArticle(
                id = "scripture_${verse.id}",
                title = "${verse.scriptureName}: ${verse.sectionOrChapter}",
                category = "Sacred Scriptures & Philosophy",
                tags = verse.tags + listOf("scripture", verse.scriptureName.lowercase(), verse.associatedDeity.displayName.lowercase()),
                summary = "${verse.englishTranslation.take(160)}...",
                content = """
                    ${verse.scriptureName.uppercase()} - ${verse.sectionOrChapter}
                    
                    ORIGINAL TEXT:
                    ${verse.originalScript}
                    
                    TRANSLITERATION:
                    ${verse.romanTransliteration}
                    
                    ENGLISH TRANSLATION:
                    ${verse.englishTranslation}
                    
                    DEEP PHILOSOPHICAL COMMENTARY:
                    ${verse.deepPhilosophicalMeaning}
                    
                    PRESIDING DEITY CONTEXT:
                    Presided by ${verse.associatedDeity.displayName} (${verse.associatedDeity.title}).
                """.trimIndent(),
                keyTakeaways = listOf(
                    verse.englishTranslation.take(80),
                    verse.deepPhilosophicalMeaning.take(80),
                    "Associated with deity: ${verse.associatedDeity.displayName}"
                )
            )
        }
    }
}
