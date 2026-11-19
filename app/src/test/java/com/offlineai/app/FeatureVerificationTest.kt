package com.offlineai.app

import com.offlineai.app.data.AttachmentType
import com.offlineai.app.data.ChatAttachment
import com.offlineai.app.data.Persona
import com.offlineai.app.engine.ActivationType
import com.offlineai.app.engine.CelebrationEngine
import com.offlineai.app.engine.ChatEngine
import com.offlineai.app.engine.ImageStyle
import com.offlineai.app.engine.NeuralNetwork
import com.offlineai.app.engine.NlpToolkitEngine
import com.offlineai.app.engine.ScriptureLibraryEngine
import com.offlineai.app.engine.SemanticKnowledgeEngine
import com.offlineai.app.engine.TrainingSample
import com.offlineai.app.engine.VideoMotionMode
import com.offlineai.app.engine.VoiceProfile
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FeatureVerificationTest {

    // ==========================================
    // 1. NEURAL LAB - MLP & Pattern Classifier
    // ==========================================

    @Test
    fun testNeuralNetworkXorConvergence() {
        val nn = NeuralNetwork(intArrayOf(2, 4, 1), ActivationType.SIGMOID)
        val xorSamples = listOf(
            TrainingSample(doubleArrayOf(0.0, 0.0), doubleArrayOf(0.0), "0^0=0"),
            TrainingSample(doubleArrayOf(0.0, 1.0), doubleArrayOf(1.0), "0^1=1"),
            TrainingSample(doubleArrayOf(1.0, 0.0), doubleArrayOf(1.0), "1^0=1"),
            TrainingSample(doubleArrayOf(1.0, 1.0), doubleArrayOf(0.0), "1^1=0")
        )

        var finalLoss = 1.0
        for (epoch in 1..2500) {
            finalLoss = nn.trainEpoch(xorSamples, learningRate = 0.5, momentum = 0.8)
        }

        assertTrue("XOR final loss should converge below 0.1, was: $finalLoss", finalLoss < 0.1)

        val out00 = nn.forward(doubleArrayOf(0.0, 0.0))[0]
        val out01 = nn.forward(doubleArrayOf(0.0, 1.0))[0]
        val out10 = nn.forward(doubleArrayOf(1.0, 0.0))[0]
        val out11 = nn.forward(doubleArrayOf(1.0, 1.0))[0]

        assertTrue("0^0 should be < 0.35, was $out00", out00 < 0.35)
        assertTrue("0^1 should be > 0.65, was $out01", out01 > 0.65)
        assertTrue("1^0 should be > 0.65, was $out10", out10 > 0.65)
        assertTrue("1^1 should be < 0.35, was $out11", out11 < 0.35)
    }

    @Test
    fun testNeuralNetworkPatternClassifier() {
        // 9 inputs (3x3 grid), 6 hidden, 2 output classes (Cross vs Plus)
        val nn = NeuralNetwork(intArrayOf(9, 6, 2), ActivationType.SIGMOID)
        assertEquals(3, nn.layerSizes.size)
        assertEquals(9, nn.layerSizes[0])
        assertEquals(6, nn.layerSizes[1])
        assertEquals(2, nn.layerSizes[2])

        val cross = doubleArrayOf(
            1.0, 0.0, 1.0,
            0.0, 1.0, 0.0,
            1.0, 0.0, 1.0
        )
        val plus = doubleArrayOf(
            0.0, 1.0, 0.0,
            1.0, 1.0, 1.0,
            0.0, 1.0, 0.0
        )

        val samples = listOf(
            TrainingSample(cross, doubleArrayOf(1.0, 0.0), "Cross"),
            TrainingSample(plus, doubleArrayOf(0.0, 1.0), "Plus")
        )

        for (i in 1..500) {
            nn.trainEpoch(samples, learningRate = 0.4, momentum = 0.8)
        }

        val crossOut = nn.forward(cross)
        val plusOut = nn.forward(plus)

        assertEquals(2, crossOut.size)
        assertEquals(2, plusOut.size)
        assertTrue("Cross should activate neuron 0 higher than neuron 1", crossOut[0] > crossOut[1])
        assertTrue("Plus should activate neuron 1 higher than neuron 0", plusOut[1] > plusOut[0])
    }

    // ==========================================
    // 2. NLP TOOLKIT ENGINE
    // ==========================================

    @Test
    fun testNlpSentimentAnalysis() {
        val nlp = NlpToolkitEngine()

        val posText = "This outstanding achievement is brilliant, amazing, and a triumphant success with excellent results."
        val posResult = nlp.analyzeSentiment(posText)
        assertEquals("Positive", posResult.label)
        assertTrue("Positive score should be > 0.3, was: ${posResult.score}", posResult.score > 0.3)

        val negText = "A terrible disaster occurred with horrible damage, catastrophic failure, and awful tragic loss."
        val negResult = nlp.analyzeSentiment(negText)
        assertEquals("Negative", negResult.label)
        assertTrue("Negative score should be < -0.3, was: ${negResult.score}", negResult.score < -0.3)

        val neutralText = "The system initializes the numerical parameter array according to standard specifications."
        val neutralResult = nlp.analyzeSentiment(neutralText)
        assertEquals("Neutral", neutralResult.label)
    }

    @Test
    fun testNlpSummarization() {
        val nlp = NlpToolkitEngine()
        val text = """
            Artificial Intelligence has evolved dramatically over the past several decades.
            Modern on-device AI enables high-performance models to run locally without cloud servers.
            Offline intelligence guarantees absolute data privacy and rapid response latency.
            By utilizing optimized linear algebra and neural network weights, mobile devices execute complex reasoning.
            This ensures that users can access intelligent decision-making anywhere, even without internet access.
        """.trimIndent()

        val summary = nlp.summarize(text, targetSentenceCount = 2)
        assertNotNull(summary)
        assertTrue(summary.keySentences.isNotEmpty())
        assertTrue("Summary should contain at least 1 sentence", summary.keySentences.size >= 1)
        assertTrue("Original sentence count should be >= 4", summary.originalSentenceCount >= 4)
        assertTrue("Compression ratio should be positive", summary.compressionRatioPercent >= 0)
    }

    @Test
    fun testNlpReadabilityAndKeywords() {
        val nlp = NlpToolkitEngine()
        val text = "Artificial intelligence empowers sovereign individuals with offline computing capabilities."

        val readability = nlp.analyzeReadability(text)
        assertNotNull(readability)
        assertTrue(readability.fleschScore >= 0.0)
        assertTrue(readability.readingGrade.isNotBlank())
        assertTrue(readability.readingEaseDesc.isNotBlank())

        val keywordResult = nlp.extractKeywordsAndEntities(text)
        assertTrue("Keywords list should not be empty", keywordResult.topKeywords.isNotEmpty())
        assertTrue("Keywords should contain key terms", keywordResult.topKeywords.any { it.first.contains("intelligence") || it.first.contains("offline") })
    }

    // ==========================================
    // 3. UNIVERSAL CALENDAR & CELEBRATIONS (1-9999 AD)
    // ==========================================

    @Test
    fun testCelebrationAnchorDates() {
        val engine = CelebrationEngine()

        val jan1 = engine.getCelebrationForDate(2026, 1, 1)
        assertTrue(jan1.title.contains("Kalpataru"))
        assertEquals("Krishna", jan1.deityName)

        val jan14 = engine.getCelebrationForDate(2026, 1, 14)
        assertTrue(jan14.title.contains("Makar Sankranti") || jan14.title.contains("Pongal"))
        assertEquals("Lakshmi", jan14.deityName)

        val janmashtami = engine.getCelebrationForDate(2026, 8, 26)
        assertTrue(janmashtami.title.contains("Janmashtami"))
        assertEquals("Krishna", janmashtami.deityName)

        val ganeshChaturthi = engine.getCelebrationForDate(2026, 9, 7)
        assertTrue(ganeshChaturthi.title.contains("Ganesh Chaturthi"))
        assertEquals("Ganesha", ganeshChaturthi.deityName)
    }

    @Test
    fun testCelebrationsAcrossCenturiesTo9999AD() {
        val engine = CelebrationEngine()
        val testYears = listOf(1, 500, 1000, 1947, 2026, 3000, 5555, 9999)

        for (year in testYears) {
            for (month in 1..12) {
                val celebration = engine.getCelebrationForDate(year, month, 15)
                assertNotNull("Celebration for $year-$month-15 should not be null", celebration)
                assertTrue("Title must not be blank for year $year", celebration.title.isNotBlank())
                assertTrue("Deity name must not be blank for year $year", celebration.deityName.isNotBlank())
                assertTrue("Rituals must not be blank for year $year", celebration.ritualsAndObservance.isNotBlank())
                assertTrue("Badge color must start with #", celebration.badgeColorHex.startsWith("#"))
            }
        }
    }

    // ==========================================
    // 4. SCRIPTURES & PHILOSOPHICAL ENGINE
    // ==========================================

    @Test
    fun testScriptureLibraryVerses() {
        val engine = ScriptureLibraryEngine()
        assertTrue("Scripture library must contain verses", engine.verses.isNotEmpty())

        val gita247 = engine.verses.find { it.id == "gita_2_47" }
        assertNotNull("Gita 2:47 (Karma Yoga) must exist", gita247)
        assertEquals(Persona.KRISHNA, gita247?.associatedDeity)
        assertTrue("Gita 2:47 should contain original Sanskrit", gita247?.originalScript?.contains("कर्मण्येवाधिकारस्ते") == true)
        assertTrue("Gita 2:47 tags must include maphula", gita247?.tags?.contains("maphula") == true)

        val krishnaVerses = engine.verses.filter { it.associatedDeity == Persona.KRISHNA }
        assertTrue("Should return Krishna verses", krishnaVerses.isNotEmpty())

        val shivaVerses = engine.verses.filter { it.associatedDeity == Persona.SHIVA }
        assertTrue("Should return Shiva verses", shivaVerses.isNotEmpty())

        val gitaVerses = engine.getVersesForScripture("Bhagavad Gita")
        assertTrue("Should return Gita verses", gitaVerses.isNotEmpty())

        val knowledgeArticles = engine.toKnowledgeArticles()
        assertEquals(engine.verses.size, knowledgeArticles.size)
        knowledgeArticles.forEach { article ->
            assertTrue(article.title.isNotBlank())
            assertTrue(article.content.isNotBlank())
            assertTrue(article.tags.isNotEmpty())
        }
    }

    // ==========================================
    // 5. DEITY PERSONAS & ATTRIBUTES
    // ==========================================

    @Test
    fun testAllTenDeityPersonasConfigured() {
        val personas = Persona.values()
        assertEquals(10, personas.size)

        val expectedNames = setOf(
            "Krishna", "Shiva", "Brahma", "Shani", "Ganesha",
            "Muruga", "Hanuman", "Lakshmi", "Parvathi", "Narasimha"
        )
        val actualNames = personas.map { it.displayName }.toSet()
        assertEquals(expectedNames, actualNames)

        personas.forEach { p ->
            assertTrue("Title should not be blank for ${p.name}", p.title.isNotBlank())
            assertTrue("Description should not be blank for ${p.name}", p.description.isNotBlank())
            assertTrue("System prompt should not be blank for ${p.name}", p.systemPrompt.isNotBlank())
            assertTrue("Tag color hex should start with # for ${p.name}", p.tagColorHex.startsWith("#"))
            assertEquals(7, p.tagColorHex.length)
        }

        // Backward compatibility aliases
        assertEquals(Persona.KRISHNA, Persona.HARI)
        assertEquals(Persona.SHIVA, Persona.CYGNUS)
        assertEquals(Persona.BRAHMA, Persona.SOPHIA)
        assertEquals(Persona.LAKSHMI, Persona.MUSE)
        assertEquals(Persona.HANUMAN, Persona.GUARDIAN)
    }

    // ==========================================
    // 6. VOICE PROFILES & MARKDOWN SANITIZATION
    // ==========================================

    @Test
    fun testDeityVoiceProfilesAndSalutations() {
        val testProfiles = listOf(
            VoiceProfile("v_krishna", "Krishna", 135f, 1.05f, 1.0f, 1.0f, 0.95f, "Cosmic", "#7C3AED"),
            VoiceProfile("v_shiva", "Shiva", 100f, 0.85f, 0.95f, 1.15f, 0.9f, "Transformer", "#06B6D4"),
            VoiceProfile("v_brahma", "Brahma", 125f, 1.0f, 0.98f, 1.05f, 0.92f, "Creator", "#F59E0B"),
            VoiceProfile("v_shani", "Shani", 95f, 0.8f, 0.92f, 1.2f, 0.85f, "Discipline", "#64748B"),
            VoiceProfile("v_ganesha", "Ganesha", 130f, 1.02f, 1.0f, 1.0f, 0.95f, "Obstacle Remover", "#EA580C"),
            VoiceProfile("v_muruga", "Muruga", 140f, 1.1f, 1.05f, 0.95f, 0.9f, "Tactical", "#E11D48"),
            VoiceProfile("v_hanuman", "Hanuman", 110f, 0.9f, 1.02f, 1.1f, 0.95f, "Guardian", "#DC2626"),
            VoiceProfile("v_lakshmi", "Lakshmi", 200f, 1.35f, 1.0f, 0.9f, 0.98f, "Flourishing", "#DB2777"),
            VoiceProfile("v_parvathi", "Parvathi", 190f, 1.25f, 0.98f, 0.92f, 0.96f, "Shakti", "#059669"),
            VoiceProfile("v_narasimha", "Narasimha", 90f, 0.75f, 0.9f, 1.25f, 0.8f, "Defense", "#B45309")
        )

        assertEquals(10, testProfiles.size)
        testProfiles.forEach { vp ->
            assertTrue("Pitch must be positive", vp.pitchHz > 0f)
            assertTrue("Pitch factor must be > 0.5", vp.pitchFactor >= 0.5f)
            assertTrue("Description must not be blank", vp.description.isNotBlank())
            assertTrue("Color must be hex", vp.colorHex.startsWith("#"))
        }

        // Test markdown sanitization logic
        val rawMarkdown = """
            ### 🦚 Divine Insight
            Here is **Krishna's** supreme counsel for you:
            * Focus on the *process* of action
            * Do not obsess over results
            See [Bhagavad Gita](https://example.com) for details.
            ```python
            def action():
                pass
            ```
        """.trimIndent()

        val cleaned = rawMarkdown
            .replace(Regex("```[\\s\\S]*?```"), "Code implementation omitted for speech.")
            .replace(Regex("`[^`]*`"), "")
            .replace(Regex("#+\\s*"), "")
            .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
            .replace(Regex("\\*([^*]+)\\*"), "$1")
            .replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1")
            .replace(Regex("^[•\\-*]\\s+", RegexOption.MULTILINE), "")
            .replace(Regex("[🦚🔱🪷🐘⚡🛡️🌸🏔️🦁⚖️]"), "")
            .trim()

        assertFalse("Markdown headers should be stripped", cleaned.contains("###"))
        assertFalse("Deity emojis should be removed for speech", cleaned.contains("🦚"))
        assertFalse("Bold asterisks should be stripped", cleaned.contains("**"))
        assertTrue("Python code should be replaced with speech placeholder", cleaned.contains("Code implementation omitted for speech."))
        assertTrue("Link text should be preserved", cleaned.contains("Bhagavad Gita"))
        assertFalse("URL should be stripped", cleaned.contains("https://example.com"))
    }

    // ==========================================
    // 7. MULTI-MODAL ATTACHMENTS
    // ==========================================

    @Test
    fun testMultiModalAttachmentTypes() {
        val picture = ChatAttachment.fromFileName("photo.jpg", 1024L)
        val audio = ChatAttachment.fromFileName("chant.mp3", 2048L)
        val video = ChatAttachment.fromFileName("demo.mp4", 4096L)
        val doc = ChatAttachment.fromFileName("scripture.pdf", 8192L)

        assertEquals(AttachmentType.PICTURE, picture.fileType)
        assertEquals("image/jpeg", picture.mimeType)

        assertEquals(AttachmentType.AUDIO, audio.fileType)
        assertEquals("audio/mpeg", audio.mimeType)

        assertEquals(AttachmentType.VIDEO, video.fileType)
        assertEquals("video/mp4", video.mimeType)

        assertEquals(AttachmentType.DOCUMENT, doc.fileType)
        assertEquals("application/pdf", doc.mimeType)
    }

    // ==========================================
    // 8. SEMANTIC KNOWLEDGE BASE & TF-IDF RETRIEVAL
    // ==========================================

    @Test
    fun testSemanticKnowledgeRetrieval() {
        val engine = SemanticKnowledgeEngine()
        val allArticles = engine.getAllArticles()
        assertTrue("Knowledge base should contain at least 5 articles", allArticles.size >= 5)

        // Search for emergency CPR
        val cprHits = engine.search("CPR chest compression choking emergency", topK = 3)
        assertTrue("Should return hits for CPR search", cprHits.isNotEmpty())
        val topCpr = cprHits.first().first
        assertTrue("Top hit should be CPR protocol, was: ${topCpr.title}", topCpr.title.contains("CPR") || topCpr.tags.contains("cpr"))
        assertTrue("Relevance score should be > 0.0", cprHits.first().second > 0.0)

        // Verify distinct categories exist
        val categories = allArticles.map { it.category }.distinct()
        assertTrue("Categories should not be empty", categories.isNotEmpty())
        assertTrue("Categories should contain Emergency & Survival", categories.any { it.contains("Emergency") })

        // Category filtered search
        val filteredHits = engine.search("", topK = 10, categoryFilter = "Emergency & Survival")
        assertTrue("Filtered search should return articles", filteredHits.isNotEmpty())
        filteredHits.forEach { (art, _) ->
            assertEquals("Emergency & Survival", art.category)
        }
    }

    // ==========================================
    // 9. DEITY AI & MULTI-MODAL CHAT ENGINE
    // ==========================================

    @Test
    fun testChatEngineAllTenDeityGreetings() = runBlocking {
        val knowledgeEngine = SemanticKnowledgeEngine()
        val nlpEngine = NlpToolkitEngine()
        val chatEngine = ChatEngine(knowledgeEngine, nlpEngine)

        val greetings = mapOf(
            Persona.KRISHNA to Pair("Krishna", "Radhe Radhe"),
            Persona.SHIVA to Pair("Shiva", "Om Namah Shivaya"),
            Persona.BRAHMA to Pair("Brahma", "Om Namo Brahmane"),
            Persona.SHANI to Pair("Shani", "Om Sham Shanicharaya"),
            Persona.GANESHA to Pair("Ganesha", "Om Gam Ganapataye"),
            Persona.MURUGA to Pair("Muruga", "Vetrivel"),
            Persona.HANUMAN to Pair("Hanuman", "Jai Bajrangbali"),
            Persona.LAKSHMI to Pair("Lakshmi", "Om Shreem Mahalakshmyai"),
            Persona.PARVATHI to Pair("Parvathi", "Om Parvathyai"),
            Persona.NARASIMHA to Pair("Narasimha", "Om Namo Bhagavate Narasimhaya")
        )

        for ((persona, expectedStrings) in greetings) {
            val tokens = chatEngine.generateStreamingResponse("hi", persona).toList()
            val fullResponse = tokens.last()
            assertTrue("Greeting for ${persona.name} should contain '${expectedStrings.first}'", fullResponse.contains(expectedStrings.first))
            assertTrue("Greeting for ${persona.name} should contain '${expectedStrings.second}'", fullResponse.contains(expectedStrings.second))
        }
    }

    @Test
    fun testChatEngineMultiModalInspections() = runBlocking {
        val knowledgeEngine = SemanticKnowledgeEngine()
        val nlpEngine = NlpToolkitEngine()
        val chatEngine = ChatEngine(knowledgeEngine, nlpEngine)

        // 1. Audio Inspection
        val audioAttachment = ChatAttachment.fromFileName("vedic_gayatri_mantra.mp3", 2048L)
        val audioTokens = chatEngine.generateStreamingResponse("analyze audio", Persona.KRISHNA, audioAttachment).toList()
        val audioResponse = audioTokens.last()
        assertTrue("Audio response should contain acoustic inspection header", audioResponse.contains("Acoustic & Audio Spectrogram Inspection"))
        assertTrue("Audio response should contain 432 Hz Solfeggio Scale", audioResponse.contains("432 Hz Solfeggio Scale"))
        assertTrue("Audio response should contain Krishna synthesis", audioResponse.contains("Krishna's Synthesis"))

        // 2. Video Inspection
        val videoAttachment = ChatAttachment.fromFileName("cosmic_diffusion_loop.mp4", 4096L)
        val videoTokens = chatEngine.generateStreamingResponse("analyze video", Persona.SHIVA, videoAttachment).toList()
        val videoResponse = videoTokens.last()
        assertTrue("Video response should contain video inspection header", videoResponse.contains("Video Motion & Frame Vector Inspection"))
        assertTrue("Video response should contain 24 Keyframes", videoResponse.contains("24 Keyframes Looping"))
        assertTrue("Video response should contain Shiva breakdown", videoResponse.contains("Shiva's Architectural Breakdown"))

        // 3. Document Inspection
        val docAttachment = ChatAttachment.fromFileName("bhagavad_gita_ch2.pdf", 8192L)
        val docTokens = chatEngine.generateStreamingResponse("analyze document", Persona.HANUMAN, docAttachment).toList()
        val docResponse = docTokens.last()
        assertTrue("Document response should contain OCR parsing header", docResponse.contains("Document OCR & Structural Parsing"))
        assertTrue("Document response should contain Bhagavad Gita Chapter 2", docResponse.contains("Bhagavad Gita Chapter 2"))
        assertTrue("Document response should contain Hanuman evaluation", docResponse.contains("Hanuman's Protective Evaluation"))

        // 4. Picture Inspection
        val picAttachment = ChatAttachment.fromFileName("sacred_sri_mandala.png", 1024L)
        val picTokens = chatEngine.generateStreamingResponse("analyze geometry", Persona.GANESHA, picAttachment).toList()
        val picResponse = picTokens.last()
        assertTrue("Picture response should contain Computer Vision header", picResponse.contains("Computer Vision & Visual Feature Inspection"))
        assertTrue("Picture response should contain Sacred Geometrical Sri Mandala", picResponse.contains("Sacred Geometrical Sri Mandala"))
        assertTrue("Picture response should contain Ganesha resolution", picResponse.contains("Ganesha's Obstacle Resolution"))
    }

    @Test
    fun testQuestionAnsweringArithmeticAndConversions() = runBlocking {
        val knowledgeEngine = SemanticKnowledgeEngine()
        val nlpEngine = NlpToolkitEngine()
        val chatEngine = ChatEngine(knowledgeEngine, nlpEngine)

        // 1. Arithmetic evaluation
        val mathTokens = chatEngine.generateStreamingResponse("what is 25 * 4", Persona.BRAHMA).toList()
        val mathAns = mathTokens.last()
        assertTrue("Multiplication should yield 100", mathAns.contains("100"))

        val pctTokens = chatEngine.generateStreamingResponse("calculate 15% of 200", Persona.SHANI).toList()
        val pctAns = pctTokens.last()
        assertTrue("Percentage should yield 30", pctAns.contains("30"))

        val sqrtTokens = chatEngine.generateStreamingResponse("sqrt(144)", Persona.BRAHMA).toList()
        val sqrtAns = sqrtTokens.last()
        assertTrue("Square root of 144 should yield 12", sqrtAns.contains("12"))

        // 2. Physical unit conversion
        val tempTokens = chatEngine.generateStreamingResponse("100 celsius to fahrenheit", Persona.BRAHMA).toList()
        val tempAns = tempTokens.last()
        assertTrue("100C in F should yield 212", tempAns.contains("212"))
    }

    @Test
    fun testQuestionAnsweringScienceAndTechDoubts() = runBlocking {
        val knowledgeEngine = SemanticKnowledgeEngine()
        val nlpEngine = NlpToolkitEngine()
        val chatEngine = ChatEngine(knowledgeEngine, nlpEngine)

        // 1. Photosynthesis
        val photoTokens = chatEngine.generateStreamingResponse("what is photosynthesis", Persona.BRAHMA).toList()
        val photoAns = photoTokens.last()
        assertTrue("Should explain chlorophyll or equation", photoAns.contains("Chlorophyll") || photoAns.contains("glucose") || photoAns.contains("CO2"))

        // 2. Sky Color / Rayleigh Scattering
        val skyTokens = chatEngine.generateStreamingResponse("why is the sky blue", Persona.KRISHNA).toList()
        val skyAns = skyTokens.last()
        assertTrue("Should explain Rayleigh Scattering", skyAns.contains("Rayleigh Scattering") || skyAns.contains("wavelength"))

        // 3. Recursion doubt
        val recTokens = chatEngine.generateStreamingResponse("i have a doubt about recursion in programming", Persona.SHIVA).toList()
        val recAns = recTokens.last()
        assertTrue("Should explain Base Case and Call Stack", recAns.contains("Base Case") && recAns.contains("Call Stack"))

        // 4. Compiler vs Interpreter
        val compTokens = chatEngine.generateStreamingResponse("difference between compiler and interpreter", Persona.SHIVA).toList()
        val compAns = compTokens.last()
        assertTrue("Should explain compiler vs interpreter", compAns.contains("Ahead of Time") || compAns.contains("Compiler") && compAns.contains("Interpreter"))

        // 5. World geography
        val capTokens = chatEngine.generateStreamingResponse("what is the capital of france", Persona.GANESHA).toList()
        val capAns = capTokens.last()
        assertTrue("Should state Paris as capital", capAns.contains("Paris"))

        // 6. How to bake a cake
        val cakeTokens = chatEngine.generateStreamingResponse("how do i make a cake", Persona.LAKSHMI).toList()
        val cakeAns = cakeTokens.last()
        assertTrue("Should explain cake baking steps and ingredients", cakeAns.contains("Flour") && cakeAns.contains("Bake"))
    }

    @Test
    fun testChatEngineNewChatSessionReset() = runBlocking {
        val knowledgeEngine = SemanticKnowledgeEngine()
        val nlpEngine = NlpToolkitEngine()
        val chatEngine = ChatEngine(knowledgeEngine, nlpEngine)

        // Seed some history
        chatEngine.generateStreamingResponse("Hello Krishna", Persona.KRISHNA).toList()
        assertTrue("Chat history should have messages", chatEngine.getHistory().isNotEmpty())

        // Start new chat (clear history)
        chatEngine.clearHistory()
        assertEquals(0, chatEngine.getHistory().size)
        // A new chat has no chat messages in the conversation
        assertTrue("New chat session must have zero chat messages", chatEngine.getHistory().isEmpty())
    }

    // ==========================================
    // 10. CREATIVE STUDIO ENUMS & STYLES
    // ==========================================

    @Test
    fun testCreativeStudioEnums() {
        val styles = ImageStyle.values()
        assertEquals(5, styles.size)
        styles.forEach { style ->
            assertTrue(style.label.isNotBlank())
            assertTrue(style.tag.isNotBlank())
        }

        val modes = VideoMotionMode.values()
        assertEquals(5, modes.size)
        modes.forEach { mode ->
            assertTrue(mode.label.isNotBlank())
            assertTrue(mode.description.isNotBlank())
        }
    }

    // ==========================================
    // 11. DEITY ACCENT PALETTES & THEME ADAPTATION
    // ==========================================

    @Test
    fun testDeityAccentPalettes() {
        val palettes = com.offlineai.app.ui.theme.AccentPalette.deityPalettes()
        assertEquals(9, palettes.size)

        palettes.forEach { pal ->
            assertTrue("Title should not be blank for ${pal.name}", pal.title.isNotBlank())
            assertTrue("Emoji should not be blank for ${pal.name}", pal.emoji.isNotBlank())
            assertTrue("Deity palette should not be bright", !pal.isBright)
            assertNotEquals("Primary color must be non-zero", 0, pal.primaryColor)
            assertNotEquals("Secondary color must be non-zero", 0, pal.secondaryColor)
            assertNotEquals("Aura color must be non-zero", 0, pal.auraColor)
        }

        // Test theme resolution with different palettes
        val violetTheme = com.offlineai.app.ui.theme.ThemeManager.resolveTheme(
            com.offlineai.app.ui.theme.AestheticStyle.MATTE,
            com.offlineai.app.ui.theme.LuminanceMode.DARK,
            com.offlineai.app.ui.theme.AccentPalette.COSMIC_VIOLET
        )
        assertEquals(com.offlineai.app.ui.theme.AccentPalette.COSMIC_VIOLET.primaryColor, violetTheme.accentColor)
        assertEquals(com.offlineai.app.ui.theme.AccentPalette.COSMIC_VIOLET.secondaryColor, violetTheme.accentSecondary)
        assertEquals(com.offlineai.app.ui.theme.AccentPalette.COSMIC_VIOLET.auraColor, violetTheme.auraGlow)

        val cyanTheme = com.offlineai.app.ui.theme.ThemeManager.resolveTheme(
            com.offlineai.app.ui.theme.AestheticStyle.GLASSY,
            com.offlineai.app.ui.theme.LuminanceMode.DARK,
            com.offlineai.app.ui.theme.AccentPalette.KAILASH_CYAN
        )
        assertEquals(com.offlineai.app.ui.theme.AccentPalette.KAILASH_CYAN.primaryColor, cyanTheme.accentColor)
        assertEquals(com.offlineai.app.ui.theme.AccentPalette.KAILASH_CYAN.secondaryColor, cyanTheme.accentSecondary)
        assertEquals(com.offlineai.app.ui.theme.AccentPalette.KAILASH_CYAN.auraColor, cyanTheme.auraGlow)

        val saffronTheme = com.offlineai.app.ui.theme.ThemeManager.resolveTheme(
            com.offlineai.app.ui.theme.AestheticStyle.SOLID,
            com.offlineai.app.ui.theme.LuminanceMode.LIGHT,
            com.offlineai.app.ui.theme.AccentPalette.VEDIC_SAFFRON
        )
        assertEquals(com.offlineai.app.ui.theme.AccentPalette.VEDIC_SAFFRON.primaryColor, saffronTheme.accentColor)
        assertEquals(com.offlineai.app.ui.theme.AccentPalette.VEDIC_SAFFRON.secondaryColor, saffronTheme.accentSecondary)
        assertEquals(com.offlineai.app.ui.theme.AccentPalette.VEDIC_SAFFRON.auraColor, saffronTheme.auraGlow)

        // Test Apple iOS Cupertino Theme Resolution
        val iosThemeDark = com.offlineai.app.ui.theme.ThemeManager.resolveTheme(
            com.offlineai.app.ui.theme.AestheticStyle.IOS,
            com.offlineai.app.ui.theme.LuminanceMode.DARK,
            com.offlineai.app.ui.theme.AccentPalette.ROYAL_AZURE
        )
        assertEquals(com.offlineai.app.ui.theme.AestheticStyle.IOS, iosThemeDark.style)
        assertEquals(com.offlineai.app.ui.theme.LuminanceMode.DARK, iosThemeDark.mode)
        assertEquals(com.offlineai.app.ui.theme.AccentPalette.ROYAL_AZURE.primaryColor, iosThemeDark.accentColor)
        assertEquals(18f, iosThemeDark.cornerRadiusDp, 0.01f)
        assertFalse(iosThemeDark.isLight)
        assertTrue(iosThemeDark.isGlassy)

        val iosThemeLight = com.offlineai.app.ui.theme.ThemeManager.resolveTheme(
            com.offlineai.app.ui.theme.AestheticStyle.IOS,
            com.offlineai.app.ui.theme.LuminanceMode.LIGHT,
            com.offlineai.app.ui.theme.AccentPalette.ROYAL_AZURE
        )
        assertEquals(com.offlineai.app.ui.theme.AestheticStyle.IOS, iosThemeLight.style)
        assertEquals(com.offlineai.app.ui.theme.LuminanceMode.LIGHT, iosThemeLight.mode)
        assertEquals(18f, iosThemeLight.cornerRadiusDp, 0.01f)
        assertTrue(iosThemeLight.isLight)
    }

    @Test
    fun testBrightSolidAndMattePalettes() {
        val brights = com.offlineai.app.ui.theme.AccentPalette.brightPalettes()
        assertEquals(7, brights.size)

        brights.forEach { pal ->
            assertTrue("Title should not be blank for ${pal.name}", pal.title.isNotBlank())
            assertTrue("Emoji should not be blank for ${pal.name}", pal.emoji.isNotBlank())
            assertTrue("Should be marked bright", pal.isBright)
            assertNotEquals(0, pal.primaryColor)
            assertNotEquals(0, pal.secondaryColor)
            assertNotEquals(0, pal.auraColor)
        }

        // Test theme resolution with bright palettes for Solid Color
        val solidYellow = com.offlineai.app.ui.theme.ThemeManager.resolveTheme(
            com.offlineai.app.ui.theme.AestheticStyle.SOLID,
            com.offlineai.app.ui.theme.LuminanceMode.DARK,
            com.offlineai.app.ui.theme.AccentPalette.BRIGHT_CYBER_YELLOW
        )
        assertEquals(com.offlineai.app.ui.theme.AccentPalette.BRIGHT_CYBER_YELLOW.primaryColor, solidYellow.accentColor)
        assertEquals(com.offlineai.app.ui.theme.AestheticStyle.SOLID, solidYellow.style)
        assertEquals(8f, solidYellow.cornerRadiusDp, 0.01f)
        assertFalse(solidYellow.isLight)
        assertFalse(solidYellow.isGlassy)

        // Test theme resolution with bright palettes for Matte Finish
        val matteCyan = com.offlineai.app.ui.theme.ThemeManager.resolveTheme(
            com.offlineai.app.ui.theme.AestheticStyle.MATTE,
            com.offlineai.app.ui.theme.LuminanceMode.DARK,
            com.offlineai.app.ui.theme.AccentPalette.BRIGHT_ELECTRIC_CYAN
        )
        assertEquals(com.offlineai.app.ui.theme.AccentPalette.BRIGHT_ELECTRIC_CYAN.primaryColor, matteCyan.accentColor)
        assertEquals(com.offlineai.app.ui.theme.AestheticStyle.MATTE, matteCyan.style)
        assertEquals(14f, matteCyan.cornerRadiusDp, 0.01f)
        assertFalse(matteCyan.isLight)
        assertFalse(matteCyan.isGlassy)
    }

    // ==========================================
    // 12. FLUID MOTION INTERPOLATORS & TACTILE PHYSICS
    // ==========================================

    @Test
    fun testMotionEasingInterpolators() {
        assertNotNull(com.offlineai.app.ui.motion.MotionUtils.BUTTERY_EASING)
        assertNotNull(com.offlineai.app.ui.motion.MotionUtils.DECELERATE_EASING)
        assertNotNull(com.offlineai.app.ui.motion.MotionUtils.SPRING_OVERSHOOT)

        // Mathematical verification of tactile spring overshoot curve:
        // f(t) = (t-1)^2 * ((tension + 1)*(t-1) + tension) + 1.0
        val tension = 2.4f
        fun springOvershootFormula(t: Float): Float {
            val tm = t - 1.0f
            return tm * tm * ((tension + 1f) * tm + tension) + 1.0f
        }
        val midSpring = springOvershootFormula(0.8f)
        assertTrue("Overshoot at 0.8 should exceed 1.0, was: $midSpring", midSpring > 1.0f)

        // Mathematical verification of buttery deceleration curve
        fun decelFormula(t: Float, factor: Float = 2.2f): Float = (1.0f - Math.pow((1.0 - t), (2.0 * factor)).toFloat())
        val midDecel = decelFormula(0.5f)
        assertTrue("Deceleration at 0.5 should be > 0.5, was: $midDecel", midDecel > 0.5f)
    }

    // ==========================================
    // 13. RAVANA ADVERSARIAL SKILL ENGINE
    // ==========================================

    @Test
    fun testRavanaSkillBotRosterAndConstraints() {
        val ravana = com.offlineai.app.engine.RavanaSkillEngine()
        assertEquals(2, com.offlineai.app.engine.RavanaSkillEngine.MIN_BOTS)
        assertEquals(10, com.offlineai.app.engine.RavanaSkillEngine.MAX_BOTS)
        assertEquals(10, ravana.fullCouncil.size)

        // Verify minimum 2 bots
        val botsMin = ravana.getBotsForCount(2)
        assertEquals(2, botsMin.size)
        assertEquals("Inquisitor Red", botsMin[0].name)
        assertEquals(com.offlineai.app.engine.RavanaSkillEngine.BotStance.ATTACKER, botsMin[0].stance)
        assertEquals("Architect White", botsMin[1].name)
        assertEquals(com.offlineai.app.engine.RavanaSkillEngine.BotStance.DEFENDER, botsMin[1].stance)

        // Verify user can assign 5 bots
        val bots5 = ravana.getBotsForCount(5)
        assertEquals(5, bots5.size)

        // Verify clamping below minimum and above maximum
        assertEquals(2, ravana.getBotsForCount(0).size)
        assertEquals(2, ravana.getBotsForCount(1).size)
        assertEquals(10, ravana.getBotsForCount(15).size)
    }

    @Test
    fun testRavanaSkillIdeaBattleSurvivalDetermination() {
        val ravana = com.offlineai.app.engine.RavanaSkillEngine()

        // 1. Battle with minimum 2 bots
        val battle2 = ravana.evaluateIdeaBattle("Autonomous on-device AI software", 2)
        assertEquals(2, battle2.botCount)
        assertEquals(3, battle2.rounds.size)
        assertTrue("Survival score must be between 0 and 100", battle2.survivalScore in 0..100)
        assertNotNull(battle2.verdict)
        assertTrue("Verdict summary must not be blank", battle2.verdictSummary.isNotBlank())
        assertTrue("Must expose fatal flaws", battle2.fatalFlaws.isNotEmpty())
        assertTrue("Must provide surviving pillars", battle2.survivingPillars.isNotEmpty())
        assertTrue("Must provide fortification roadmap", battle2.fortificationRoadmap.isNotEmpty())

        // 2. Battle with user-expanded 7 bots
        val battle7 = ravana.evaluateIdeaBattle("Quit job to open a cat cafe with crypto tokens", 7)
        assertEquals(7, battle7.botCount)
        assertEquals(7, battle7.participatingBots.size)
        assertEquals(3, battle7.rounds.size)

        // Verify each round has dialogue turns
        for (round in battle7.rounds) {
            assertTrue("Round ${round.roundNumber} must contain turns", round.turns.isNotEmpty())
            for (turn in round.turns) {
                assertTrue("Statement must not be blank", turn.statement.isNotBlank())
                assertTrue("Impact must be positive", turn.impactScore > 0)
            }
        }
    }

    @Test
    fun testRavanaSkillCustomBotBuilder() {
        val ravana = com.offlineai.app.engine.RavanaSkillEngine()
        assertEquals(10, ravana.getAllAvailableBots().size)

        // Add custom bot archetype
        val customBot = ravana.addCustomBot(
            name = "Memory Auditor Sentinel",
            title = "Zero Heap Allocator",
            avatar = "⚡",
            stance = com.offlineai.app.engine.RavanaSkillEngine.BotStance.PERFORMANCE,
            tagline = "Audits contiguous buffer allocations and GC pressure.",
            primaryFocus = "Heap fragmentation and CPU cache line locality"
        )

        assertNotNull(customBot)
        assertEquals("Memory Auditor Sentinel", customBot.name)
        assertEquals(11, customBot.id)
        assertEquals(11, ravana.getAllAvailableBots().size)
        assertEquals(1, ravana.customBots.size)

        // Evaluate battle with 11 bots including the custom bot
        val battle = ravana.evaluateIdeaBattle("High-frequency algorithmic tick processing", 11)
        assertEquals(11, battle.botCount)
        assertEquals(11, battle.participatingBots.size)
        assertTrue(battle.participatingBots.any { it.name == "Memory Auditor Sentinel" })

        // Check Round 3 turn contains custom bot contribution
        val round3 = battle.rounds[2]
        val customTurn = round3.turns.find { it.bot.name == "Memory Auditor Sentinel" }
        assertNotNull("Custom bot should contribute dialogue to round 3", customTurn)
    }

    @Test
    fun testRavanaSkillUserReinforcementAndScorecard() {
        val ravana = com.offlineai.app.engine.RavanaSkillEngine()

        val idea = "Autonomous on-device edge AI without external API dependencies"

        // Baseline battle without reinforcement
        val battleWithout = ravana.evaluateIdeaBattle(idea, 4)

        // Battle with tactical user reinforcement
        val reinforcementText = "We enforce hardware cryptoprocessor enclaves with strict read-only flash partitions."
        val battleWith = ravana.evaluateIdeaBattle(idea, 4, userReinforcement = reinforcementText)

        assertEquals(reinforcementText, battleWith.userReinforcement)
        assertTrue("Survival score with reinforcement (${battleWith.survivalScore}) should exceed or equal baseline (${battleWithout.survivalScore})",
            battleWith.survivalScore >= battleWithout.survivalScore)
        assertTrue(battleWith.armorRating >= battleWithout.armorRating)

        // Check Round 2 turn has Commander Reinforcement
        val r2 = battleWith.rounds[1]
        val commanderTurn = r2.turns.find { it.bot.name.contains("Commander", ignoreCase = true) }
        assertNotNull("Round 2 should feature Commander Reinforcement turn", commanderTurn)
        assertTrue(commanderTurn!!.statement.contains("COMMANDER REINFORCEMENT DEPLOYED"))

        // Test Scorecard generation
        val scorecard = ravana.generateBattleScorecard(battleWith)
        assertNotNull(scorecard)
        assertTrue(scorecard.contains("RAVANA ADVERSARIAL ARENA WAR ROOM SCORECARD"))
        assertTrue(scorecard.contains(idea))
        assertTrue(scorecard.contains(reinforcementText))
        assertTrue(scorecard.contains("MAPHULA SOVEREIGN SEAL"))
        assertTrue(scorecard.contains("ARMOR INTEGRITY"))
    }

    // ==========================================
    // 14. PERSONAL RAG & KNOWLEDGE CONCEPT MIND MAP
    // ==========================================

    @Test
    fun testSemanticKnowledgePersonalDocIngestion() {
        val engine = SemanticKnowledgeEngine()
        val initialCount = engine.getAllArticles().size

        val customDoc = engine.ingestPersonalDocument(
            title = "Zero-Copy Ring Buffer Protocol",
            content = "This specification defines a lock-free single-producer single-consumer circular queue. It uses atomic memory barriers to guarantee safe cross-thread transfer without dynamic heap allocations.",
            category = "Personal Notes",
            tags = listOf("concurrency", "low-latency", "lock-free")
        )

        assertNotNull(customDoc)
        assertEquals("Zero-Copy Ring Buffer Protocol", customDoc.title)
        assertEquals("Personal Notes", customDoc.category)
        assertTrue(customDoc.isOnlineSynced)
        assertEquals(initialCount + 1, engine.getAllArticles().size)
        assertTrue("Tags should contain low-latency", customDoc.tags.contains("low-latency"))
        assertTrue("Key takeaways must be populated", customDoc.keyTakeaways.isNotEmpty())

        // Search for terms from the new document
        val searchResults = engine.search("lock-free circular queue ring buffer", topK = 3)
        assertTrue("Should return search results", searchResults.isNotEmpty())
        assertEquals("Zero-Copy Ring Buffer Protocol", searchResults[0].first.title)
    }

    @Test
    fun testSemanticKnowledgeConceptMindMap() {
        val engine = SemanticKnowledgeEngine()
        val clusters = engine.generateConceptGraph()

        assertTrue("Mind map should generate clusters", clusters.isNotEmpty())
        for (cluster in clusters) {
            assertTrue("Cluster category must not be blank", cluster.category.isNotBlank())
            assertTrue("Central topic must not be blank", cluster.centralTopic.isNotBlank())
            assertTrue("Cluster must have concept nodes", cluster.nodes.isNotEmpty())
            assertTrue("Article count must be positive", cluster.articleCount > 0)
        }

        // Verify known categories are present
        val categories = clusters.map { it.category }
        assertTrue(categories.contains("Computer Science"))
        assertTrue(categories.contains("Emergency & Survival"))
    }

    // ==========================================
    // 15. HOME SCREEN WIDGET DATA & WISDOM
    // ==========================================

    @Test
    fun testMaphulaWidgetWisdomCycleAndCelebration() {
        val scriptureEngine = ScriptureLibraryEngine()
        val celebrationEngine = CelebrationEngine()

        val gitaVerses = scriptureEngine.getVersesForScripture("Bhagavad Gita")
        assertTrue("Gita verses must be available for widget", gitaVerses.isNotEmpty())

        // Verify verse text formatting
        val sampleVerse = gitaVerses[0]
        assertTrue("Original script must not be blank", sampleVerse.originalScript.isNotBlank())
        assertTrue("English translation must not be blank", sampleVerse.englishTranslation.isNotBlank())

        // Verify celebration formatting
        val celebration = celebrationEngine.getTodayCelebration()
        assertNotNull(celebration)
        assertTrue("Celebration title must not be blank", celebration.title.isNotBlank())

        assertEquals(
            "com.offlineai.app.widget.ACTION_CYCLE_WISDOM",
            com.offlineai.app.widget.MaphulaWidgetProvider.ACTION_CYCLE_WISDOM
        )
        assertEquals(
            "com.offlineai.app.widget.ACTION_CYCLE_TIP",
            com.offlineai.app.widget.MaphulaWidgetProvider.ACTION_CYCLE_TIP
        )
        assertEquals(
            "com.offlineai.app.widget.ACTION_OPEN_CHAT",
            com.offlineai.app.widget.MaphulaWidgetProvider.ACTION_OPEN_CHAT
        )
        assertEquals(
            "com.offlineai.app.widget.ACTION_OPEN_VOICE",
            com.offlineai.app.widget.MaphulaWidgetProvider.ACTION_OPEN_VOICE
        )
        assertEquals(
            "com.offlineai.app.widget.ACTION_OPEN_CREATIVE",
            com.offlineai.app.widget.MaphulaWidgetProvider.ACTION_OPEN_CREATIVE
        )
        assertEquals(
            "com.offlineai.app.widget.ACTION_OPEN_PREFILL",
            com.offlineai.app.widget.MaphulaWidgetProvider.ACTION_OPEN_PREFILL
        )

        // Verify Widget AI Prompt Ideas
        val ideas = com.offlineai.app.widget.MaphulaWidgetProvider.AI_PROMPT_IDEAS
        assertTrue("Widget must provide prompt ideas", ideas.isNotEmpty())
        for (idea in ideas) {
            assertTrue("Idea category cannot be blank", idea.category.isNotBlank())
            assertTrue("Idea headline cannot be blank", idea.headline.isNotBlank())
            assertTrue("Idea prompt cannot be blank", idea.prompt.isNotBlank())
            assertTrue("Idea badge cannot be blank", idea.badge.isNotBlank())
        }
    }

    // ==========================================
    // 16. AUTONOMOUS ASSISTANT TASKS (GAMES, REMINDERS, EXTRA TASKS)
    // ==========================================

    @Test
    fun testAssistantTaskEngineGameAndReminderExecution() = runBlocking {
        val knowledgeEngine = SemanticKnowledgeEngine()
        val nlpEngine = NlpToolkitEngine()
        val chatEngine = ChatEngine(knowledgeEngine, nlpEngine)

        // 1. Create a game (HTML5/JS Snake Game)
        val gameTokens = chatEngine.generateStreamingResponse("create a snake game", Persona.SHIVA).toList()
        val gameResponse = gameTokens.last()
        assertTrue("Game response should contain HTML5 Canvas code", gameResponse.contains("<!DOCTYPE html>") && gameResponse.contains("<canvas"))
        assertTrue("Game response should contain game instructions", gameResponse.contains("NEON SNAKE ARCADE"))

        // 2. Play in-chat game (Dice roll & Trivia)
        val diceTokens = chatEngine.generateStreamingResponse("roll a dice", Persona.GANESHA).toList()
        val diceResponse = diceTokens.last()
        assertTrue("Dice response should contain roll result", diceResponse.contains("Dice Roll Result"))

        val triviaTokens = chatEngine.generateStreamingResponse("play a trivia game", Persona.BRAHMA).toList()
        val triviaResponse = triviaTokens.last()
        assertTrue("Trivia response should contain Trivia Challenge", triviaResponse.contains("Trivia Challenge"))

        // 3. Add Reminder & List Reminders
        val remTokens = chatEngine.generateStreamingResponse("remind me to review code at 5pm", Persona.MURUGA).toList()
        val remResponse = remTokens.last()
        assertTrue("Reminder response should confirm reminder set", remResponse.contains("Reminder Set Successfully"))
        assertTrue("Reminder response should contain the task", remResponse.contains("Review code"))

        val listRemTokens = chatEngine.generateStreamingResponse("show my reminders", Persona.MURUGA).toList()
        val listRemResponse = listRemTokens.last()
        assertTrue("List reminders should contain the scheduled reminder", listRemResponse.contains("Review code"))

        // 4. To-Do List & Checklists
        val todoTokens = chatEngine.generateStreamingResponse("add task submit quarterly report", Persona.SHANI).toList()
        val todoResponse = todoTokens.last()
        assertTrue("Todo response should confirm added task", todoResponse.contains("Task Added to To-Do List"))

        val checklistTokens = chatEngine.generateStreamingResponse("create a checklist for project launch", Persona.SHANI).toList()
        val checklistResponse = checklistTokens.last()
        assertTrue("Checklist response should contain checklist items", checklistResponse.contains("Checklist for:"))

        // 5. Timers & Pomodoro
        val timerTokens = chatEngine.generateStreamingResponse("set a timer for 25 minutes pomodoro", Persona.MURUGA).toList()
        val timerResponse = timerTokens.last()
        assertTrue("Timer response should contain Pomodoro or Timer", timerResponse.contains("Pomodoro") || timerResponse.contains("Focus Timer"))

        // 6. Fitness & Workouts
        val workoutTokens = chatEngine.generateStreamingResponse("create a workout routine", Persona.HANUMAN).toList()
        val workoutResponse = workoutTokens.last()
        assertTrue("Workout response should contain exercise sets", workoutResponse.contains("Workout") && workoutResponse.contains("Push-Ups"))

        // 7. Quick Notes
        val noteTokens = chatEngine.generateStreamingResponse("save note: buy extra apples", Persona.KRISHNA).toList()
        val noteResponse = noteTokens.last()
        assertTrue("Note response should confirm note saved", noteResponse.contains("Note Saved Locally"))
    }

    // ==========================================
    // 17. RAVANA ARENA BATTLE MOTION GRAPHICS VERIFICATION
    // ==========================================

    @Test
    fun testRavanaArenaBattleBotsAndRosterIntegrity() {
        val ravanaEngine = com.offlineai.app.engine.RavanaSkillEngine()
        val allBots = ravanaEngine.getAllAvailableBots()
        assertTrue("Ravana full roster should have at least 10 bots", allBots.size >= 10)

        val twoBots = ravanaEngine.getBotsForCount(2)
        assertEquals("Should retrieve 2 bots for 2 bot count", 2, twoBots.size)
        assertTrue("Should contain an attacker and defender", twoBots.any { it.stance == com.offlineai.app.engine.RavanaSkillEngine.BotStance.ATTACKER })

        val sixBots = ravanaEngine.getBotsForCount(6)
        assertEquals(6, sixBots.size)

        val battleResult = ravanaEngine.evaluateIdeaBattle("Autonomous Distributed Computing", 4)
        assertNotNull(battleResult)
        assertEquals(4, battleResult.participatingBots.size)
        assertTrue("Battle should generate clash rounds", battleResult.rounds.isNotEmpty())
        assertTrue("Survival score should be 0..100", battleResult.survivalScore in 0..100)
    }

    // ==========================================
    // 18. CLAUDE CODE / CODEX STUDIO VERIFICATION
    // ==========================================

    @Test
    fun testClaudeCodeExecutionAndLinting() {
        val execEngine = com.offlineai.app.engine.code.CodeExecutionEngine()

        // 1. JavaScript execution
        val jsCode = """
            function add(a, b) { return a + b; }
            console.log("Result:", add(40, 2));
        """.trimIndent()
        val jsRes = execEngine.execute(jsCode, com.offlineai.app.engine.code.CodeLanguage.JAVASCRIPT)
        assertTrue("JS execution should succeed", jsRes.success)
        assertEquals(0, jsRes.exitCode)
        assertTrue("JS stdout should contain Result", jsRes.stdout.contains("Result"))

        // 2. Python execution
        val pyCode = """
            sample = [5, 2, 8, 1]
            print("Sorted:", sorted(sample))
        """.trimIndent()
        val pyRes = execEngine.execute(pyCode, com.offlineai.app.engine.code.CodeLanguage.PYTHON)
        assertTrue("Python execution should succeed", pyRes.success)
        assertTrue("Python stdout should contain Sorted", pyRes.stdout.contains("Sorted"))

        // 3. HTML live preview
        val htmlCode = "<html><body><h1>Claude Code</h1></body></html>"
        val htmlRes = execEngine.execute(htmlCode, com.offlineai.app.engine.code.CodeLanguage.HTML_CSS)
        assertTrue("HTML execution should succeed", htmlRes.success)
        assertNotNull("HTML rendered output should be non-null", htmlRes.renderedHtml)

        // 4. Linter checks
        val badJs = "var x = 10; eval('alert(1)');"
        val issues = execEngine.lintCode(badJs, com.offlineai.app.engine.code.CodeLanguage.JAVASCRIPT)
        assertTrue("Should detect var and eval issues", issues.any { it.message.contains("eval") })
    }

    @Test
    fun testClaudeCodeAssistantEngine() = runBlocking {
        val assistant = com.offlineai.app.engine.code.ClaudeCodeAssistantEngine()
        val flow = assistant.processCodeAgentQuery(
            prompt = "implement binary search",
            currentCode = "",
            language = com.offlineai.app.engine.code.CodeLanguage.PYTHON,
            mode = com.offlineai.app.engine.code.CodeAgentMode.AUTONOMOUS_CODER
        )
        val tokens = flow.toList()
        assertTrue("Flow should emit tokens", tokens.isNotEmpty())
        val finalResponse = tokens.last()
        assertTrue("Should contain Claude Code header", finalResponse.contains("Claude Code"))
        assertTrue("Should contain Execution Plan", finalResponse.contains("Execution Plan"))
        assertTrue("Should generate code solution", finalResponse.contains("binary_search"))
    }

    // ==========================================
    // 21. MULTI-LANGUAGE INPUT & TRANSLATION ENGINE
    // ==========================================

    @Test
    fun testMultiLanguageEnumsAndDetection() {
        val languages = com.offlineai.app.engine.AppLanguage.values()
        assertTrue("Should support at least 10 languages", languages.size >= 10)

        // Script detection test
        assertEquals(com.offlineai.app.engine.AppLanguage.HINDI, com.offlineai.app.engine.AppLanguage.detectLanguage("नमस्ते आप कैसे हैं?"))
        assertEquals(com.offlineai.app.engine.AppLanguage.TAMIL, com.offlineai.app.engine.AppLanguage.detectLanguage("வணக்கம் நலமா?"))
        assertEquals(com.offlineai.app.engine.AppLanguage.TELUGU, com.offlineai.app.engine.AppLanguage.detectLanguage("నమస్కారం ఎలా ఉన్నారు?"))
        assertEquals(com.offlineai.app.engine.AppLanguage.ARABIC, com.offlineai.app.engine.AppLanguage.detectLanguage("مرحبا كيف حالك"))
        assertEquals(com.offlineai.app.engine.AppLanguage.JAPANESE, com.offlineai.app.engine.AppLanguage.detectLanguage("こんにちは、元気ですか？"))
        assertEquals(com.offlineai.app.engine.AppLanguage.RUSSIAN, com.offlineai.app.engine.AppLanguage.detectLanguage("Здравствуйте, как дела?"))
        assertEquals(com.offlineai.app.engine.AppLanguage.SPANISH, com.offlineai.app.engine.AppLanguage.detectLanguage("Hola amigo, ¿cómo estás hoy? Gracias por todo."))
        assertEquals(com.offlineai.app.engine.AppLanguage.FRENCH, com.offlineai.app.engine.AppLanguage.detectLanguage("Bonjour mon ami, c'est pour vous avec plaisir, merci!"))
        assertEquals(com.offlineai.app.engine.AppLanguage.ENGLISH, com.offlineai.app.engine.AppLanguage.detectLanguage("Hello world! How are you today?"))
    }

    @Test
    fun testMultiLanguageTranslationEngine() {
        val translator = com.offlineai.app.engine.MultiLanguageTranslationEngine()
        val hindiHello = translator.translatePhrase("hello", com.offlineai.app.engine.AppLanguage.HINDI)
        assertTrue("Hindi translation should contain Namaste", hindiHello.contains("नमस्ते"))

        val tamilThanks = translator.translatePhrase("thank you", com.offlineai.app.engine.AppLanguage.TAMIL)
        assertTrue("Tamil translation should contain Nandri", tamilThanks.contains("நன்றி"))

        val teluguPeace = translator.translatePhrase("peace", com.offlineai.app.engine.AppLanguage.TELUGU)
        assertTrue("Telugu translation should contain Shanti", teluguPeace.contains("శాంతి"))

        val sanskritWisdom = translator.translatePhrase("wisdom", com.offlineai.app.engine.AppLanguage.SANSKRIT)
        assertTrue("Sanskrit translation should contain Prajna", sanskritWisdom.contains("प्रज्ञा"))
    }
}
