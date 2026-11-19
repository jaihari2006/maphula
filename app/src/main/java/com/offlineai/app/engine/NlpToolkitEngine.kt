package com.offlineai.app.engine

import com.offlineai.app.data.KeywordResult
import com.offlineai.app.data.NamedEntity
import com.offlineai.app.data.ReadabilityResult
import com.offlineai.app.data.SentimentResult
import com.offlineai.app.data.SummaryResult
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class NlpToolkitEngine {

    private val stopWords = setOf(
        "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are",
        "aren't", "as", "at", "be", "because", "been", "before", "being", "below", "between",
        "both", "but", "by", "can", "cannot", "could", "did", "do", "does", "doing", "down",
        "during", "each", "few", "for", "from", "further", "had", "has", "have", "having", "he",
        "her", "here", "hers", "herself", "him", "himself", "his", "how", "i", "if", "in", "into",
        "is", "it", "its", "itself", "me", "more", "most", "my", "myself", "no", "nor", "not",
        "of", "off", "on", "once", "only", "or", "other", "ought", "our", "ours", "ourselves",
        "out", "over", "own", "same", "she", "should", "so", "some", "such", "than", "that", "the",
        "their", "theirs", "them", "themselves", "then", "there", "these", "they", "this", "those",
        "through", "to", "too", "under", "until", "up", "very", "was", "we", "were", "what",
        "when", "where", "which", "while", "who", "whom", "why", "with", "would", "you", "your"
    )

    private val sentimentLexicon = mapOf(
        // High positive (+3)
        "excellent" to 3.0, "superb" to 3.0, "outstanding" to 3.0, "amazing" to 3.0,
        "brilliant" to 3.0, "masterpiece" to 3.0, "triumph" to 3.0, "fantastic" to 3.0,
        "love" to 3.0, "loved" to 3.0, "breakthrough" to 3.0, "revolutionary" to 3.0,
        // Moderate positive (+2)
        "good" to 2.0, "great" to 2.0, "helpful" to 2.0, "beneficial" to 2.0,
        "effective" to 2.0, "positive" to 2.0, "strong" to 2.0, "success" to 2.0,
        "successful" to 2.0, "clean" to 2.0, "smart" to 2.0, "insightful" to 2.0,
        "optimal" to 2.0, "robust" to 2.0, "peace" to 2.0, "happy" to 2.0,
        "delight" to 2.0, "inspire" to 2.0, "innovative" to 2.0, "reliable" to 2.0,
        // Mild positive (+1)
        "decent" to 1.0, "fine" to 1.0, "like" to 1.0, "liked" to 1.0, "neat" to 1.0,
        "adequate" to 1.0, "pleasant" to 1.0, "fair" to 1.0, "glad" to 1.0, "safe" to 1.0,

        // Mild negative (-1)
        "flaw" to -1.0, "flawed" to -1.0, "slow" to -1.0, "confusing" to -1.0,
        "annoying" to -1.0, "hard" to -1.0, "mediocre" to -1.0, "awkward" to -1.0,
        "questionable" to -1.0, "unclear" to -1.0, "fatigue" to -1.0, "stuck" to -1.0,

        // Moderate negative (-2)
        "bad" to -2.0, "poor" to -2.0, "danger" to -2.0, "dangerous" to -2.0,
        "error" to -2.0, "failure" to -2.0, "failed" to -2.0, "hate" to -2.0,
        "hated" to -2.0, "pain" to -2.0, "severe" to -2.0, "damage" to -2.0,
        "damaged" to -2.0, "harm" to -2.0, "harmful" to -2.0, "loss" to -2.0,
        "broken" to -2.0, "vulnerable" to -2.0, "struggle" to -2.0, "collapse" to -2.0,

        // High negative (-3)
        "terrible" to -3.0, "horrible" to -3.0, "awful" to -3.0, "catastrophic" to -3.0,
        "disaster" to -3.0, "disastrous" to -3.0, "fatal" to -3.0, "lethal" to -3.0,
        "worst" to -3.0, "ruined" to -3.0, "deadly" to -3.0, "tragic" to -3.0
    )

    private val negationWords = setOf("not", "no", "never", "without", "hardly", "barely", "scarcely", "cannot", "n't")
    private val intensifiers = mapOf("very" to 1.5, "extremely" to 2.0, "really" to 1.4, "incredibly" to 1.8, "deeply" to 1.5)

    // Emotion word seeds
    private val joyWords = setOf("happy", "love", "great", "excellent", "brilliant", "delight", "amazing", "wonderful", "smile", "success")
    private val cautionWords = setOf("danger", "risk", "hazard", "warning", "careful", "severe", "threat", "alert", "emergency", "burn")
    private val angerWords = setOf("hate", "furious", "annoying", "terrible", "rage", "ugly", "disgusting", "horrible", "infuriating")
    private val surpriseWords = setOf("shock", "astonished", "unexpected", "surprising", "extraordinary", "stunning", "unbelievable", "wonder")

    // --- 1. EXTRACTIVE SUMMARIZATION (TextRank) ---
    fun summarize(text: String, targetSentenceCount: Int = 3): SummaryResult {
        val rawSentences = splitSentences(text)
        if (rawSentences.size <= targetSentenceCount) {
            val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
            return SummaryResult(
                originalSentenceCount = rawSentences.size,
                summarySentenceCount = rawSentences.size,
                originalWordCount = words.size,
                summaryWordCount = words.size,
                compressionRatioPercent = 0,
                keySentences = rawSentences
            )
        }

        val tokenizedSentences = rawSentences.map { s ->
            s.lowercase()
                .replace(Regex("[^a-z0-9\\s]"), " ")
                .split(Regex("\\s+"))
                .filter { it.length > 1 && it !in stopWords }
        }

        val n = rawSentences.size
        val similarityMatrix = Array(n) { DoubleArray(n) }

        for (i in 0 until n) {
            val wordsI = tokenizedSentences[i].toSet()
            if (wordsI.isEmpty()) continue
            for (j in (i + 1) until n) {
                val wordsJ = tokenizedSentences[j].toSet()
                if (wordsJ.isEmpty()) continue
                val overlap = wordsI.intersect(wordsJ).size
                val denom = kotlin.math.ln(wordsI.size.toDouble() + 1.0) + kotlin.math.ln(wordsJ.size.toDouble() + 1.0)
                val sim = if (denom > 0) overlap / denom else 0.0
                similarityMatrix[i][j] = sim
                similarityMatrix[j][i] = sim
            }
        }

        // TextRank Power Iteration
        val scores = DoubleArray(n) { 1.0 }
        val damping = 0.85
        val iterations = 20

        for (iter in 0 until iterations) {
            val nextScores = DoubleArray(n)
            for (i in 0 until n) {
                var sum = 0.0
                for (j in 0 until n) {
                    if (i != j && similarityMatrix[i][j] > 0) {
                        val rowSum = similarityMatrix[j].sum()
                        if (rowSum > 0) {
                            sum += (similarityMatrix[i][j] / rowSum) * scores[j]
                        }
                    }
                }
                nextScores[i] = (1 - damping) + damping * sum
            }
            for (i in 0 until n) scores[i] = nextScores[i]
        }

        // Position boost: first and last sentences often contain key context
        scores[0] *= 1.3
        if (n > 1) scores[n - 1] *= 1.1

        // Pick top sentence indices
        val indexedScores = scores.indices.map { it to scores[it] }
            .sortedByDescending { it.second }
            .take(min(targetSentenceCount, n))
            .map { it.first }
            .sorted() // keep in original chronological order

        val summarySentences = indexedScores.map { rawSentences[it] }

        val origWords = text.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        val sumWords = summarySentences.joinToString(" ").split(Regex("\\s+")).filter { it.isNotBlank() }.size
        val compression = if (origWords > 0) ((origWords - sumWords).toDouble() / origWords * 100).toInt() else 0

        return SummaryResult(
            originalSentenceCount = rawSentences.size,
            summarySentenceCount = summarySentences.size,
            originalWordCount = origWords,
            summaryWordCount = sumWords,
            compressionRatioPercent = max(0, compression),
            keySentences = summarySentences
        )
    }

    // --- 2. SENTIMENT & EMOTIONAL TONE ANALYSIS ---
    fun analyzeSentiment(text: String): SentimentResult {
        val tokens = text.lowercase()
            .replace(Regex("[^a-z0-9'\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        var totalScore = 0.0
        var positiveCount = 0
        var negativeCount = 0
        var joyScore = 0
        var cautionScore = 0
        var angerScore = 0
        var surpriseScore = 0

        for (i in tokens.indices) {
            val token = tokens[i]

            if (token in joyWords) joyScore++
            if (token in cautionWords) cautionScore++
            if (token in angerWords) angerScore++
            if (token in surpriseWords) surpriseScore++

            val baseVal = sentimentLexicon[token] ?: continue

            var multiplier = 1.0

            // Check previous token for negation or intensifier
            if (i > 0) {
                val prev = tokens[i - 1]
                if (prev in negationWords || prev.endsWith("n't")) {
                    multiplier = -0.8
                } else if (prev in intensifiers) {
                    multiplier = intensifiers[prev] ?: 1.0
                }
            }

            val finalVal = baseVal * multiplier
            totalScore += finalVal
            if (finalVal > 0) positiveCount++ else if (finalVal < 0) negativeCount++
        }

        val totalMatches = positiveCount + negativeCount
        val normalizedScore = if (totalMatches > 0) {
            val raw = totalScore / (totalMatches * 2.5)
            max(-1.0, min(1.0, raw))
        } else 0.0

        val label = when {
            normalizedScore >= 0.25 -> "Positive"
            normalizedScore <= -0.25 -> "Negative"
            positiveCount > 0 && negativeCount > 0 -> "Mixed"
            else -> "Neutral"
        }

        val confidence = if (totalMatches > 0) {
            min(100, 50 + (abs(normalizedScore) * 50).toInt())
        } else 60

        val toneTotal = max(1, joyScore + cautionScore + angerScore + surpriseScore)
        val tones = mapOf(
            "Joy & Optimism" to ((joyScore.toDouble() / toneTotal) * 100).roundToInt(),
            "Caution & Risk" to ((cautionScore.toDouble() / toneTotal) * 100).roundToInt(),
            "Frustration" to ((angerScore.toDouble() / toneTotal) * 100).roundToInt(),
            "Surprise" to ((surpriseScore.toDouble() / toneTotal) * 100).roundToInt()
        )

        return SentimentResult(
            score = (normalizedScore * 100).roundToInt() / 100.0,
            label = label,
            confidence = confidence,
            positiveCount = positiveCount,
            negativeCount = negativeCount,
            tones = tones
        )
    }

    // --- 3. KEYWORD & ENTITY EXTRACTION ---
    fun extractKeywordsAndEntities(text: String, topK: Int = 8): KeywordResult {
        val words = text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 && it !in stopWords }

        val freq = mutableMapOf<String, Int>()
        for (w in words) freq[w] = (freq[w] ?: 0) + 1

        val total = max(1, words.size).toDouble()
        val scoredKeywords = freq.map { (w, count) ->
            val tf = count / total
            val lengthBonus = min(1.4, 0.9 + (w.length * 0.05))
            w to (tf * lengthBonus * 10.0)
        }.sortedByDescending { it.second }.take(topK)

        // Entity Detection via Regex
        val entities = mutableListOf<NamedEntity>()

        // 1. Emails
        val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        for (match in emailRegex.findAll(text)) {
            entities.add(NamedEntity(match.value, "Email"))
        }

        // 2. URLs
        val urlRegex = Regex("https?://[\\w\\d.-]+(?:\\.[a-zA-Z]{2,})+(?:/[^\\s]*)?")
        for (match in urlRegex.findAll(text)) {
            entities.add(NamedEntity(match.value, "URL"))
        }

        // 3. Numbers & Metrics
        val numberRegex = Regex("\\b\\d+(?:\\.\\d+)?(?:%|ms|s|m|kg|km|mph|bpm|C|F)?\\b")
        for (match in numberRegex.findAll(text)) {
            if (entities.none { it.text == match.value }) {
                entities.add(NamedEntity(match.value, "Metric / Number"))
            }
        }

        // 4. Capitalized Proper Nouns
        val capRegex = Regex("\\b[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*\\b")
        for (match in capRegex.findAll(text)) {
            val valStr = match.value
            if (valStr.lowercase() !in stopWords && entities.none { it.text == valStr } && valStr.length > 2) {
                entities.add(NamedEntity(valStr, "Entity / Concept"))
            }
        }

        return KeywordResult(
            topKeywords = scoredKeywords,
            entities = entities.take(12)
        )
    }

    // --- 4. READABILITY & TEXT COMPLEXITY ---
    fun analyzeReadability(text: String): ReadabilityResult {
        val sentences = splitSentences(text)
        val words = text.split(Regex("[^a-zA-Z0-9]+")).filter { it.isNotBlank() }

        val sentenceCount = max(1, sentences.size)
        val wordCount = max(1, words.size)

        var totalSyllables = 0
        for (w in words) {
            totalSyllables += countSyllables(w)
        }

        val avgWordsPerSentence = wordCount.toDouble() / sentenceCount
        val avgSyllablesPerWord = totalSyllables.toDouble() / wordCount

        // Flesch Reading Ease: 206.835 - 1.015 * (words/sentence) - 84.6 * (syllables/word)
        val rawFlesch = 206.835 - (1.015 * avgWordsPerSentence) - (84.6 * avgSyllablesPerWord)
        val fleschScore = max(0.0, min(100.0, rawFlesch))

        // Flesch-Kincaid Grade Level: 0.39 * (words/sentence) + 11.8 * (syllables/word) - 15.59
        val rawGrade = (0.39 * avgWordsPerSentence) + (11.8 * avgSyllablesPerWord) - 15.59
        val gradeNum = max(1, min(18, rawGrade.roundToInt()))

        val (gradeDesc, easeDesc) = when {
            fleschScore >= 80.0 -> Pair("Elementary (Grades 5-6)", "Very Easy to Read")
            fleschScore >= 70.0 -> Pair("Middle School (Grade 7)", "Fairly Easy to Read")
            fleschScore >= 60.0 -> Pair("High School (Grades 8-9)", "Standard Plain English")
            fleschScore >= 50.0 -> Pair("High School (Grades 10-12)", "Fairly Difficult")
            fleschScore >= 30.0 -> Pair("College Level", "Difficult")
            else -> Pair("Graduate / Professional", "Very Technical & Complex")
        }

        val readingTime = (wordCount.toDouble() / 200.0) // 200 wpm

        return ReadabilityResult(
            fleschScore = (fleschScore * 10).roundToInt() / 10.0,
            readingGrade = "$gradeDesc (Grade $gradeNum)",
            readingEaseDesc = easeDesc,
            wordCount = wordCount,
            sentenceCount = sentenceCount,
            syllableCount = totalSyllables,
            avgWordsPerSentence = (avgWordsPerSentence * 10).roundToInt() / 10.0,
            estimatedReadingTimeMinutes = (readingTime * 10).roundToInt() / 10.0
        )
    }

    private fun splitSentences(text: String): List<String> {
        val regex = Regex("(?<=[.!?])\\s+(?=[A-Z0-9])")
        return text.split(regex)
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length > 3 }
    }

    private fun countSyllables(word: String): Int {
        val w = word.lowercase()
        if (w.length <= 3) return 1
        var count = 0
        var prevIsVowel = false
        val vowels = "aeiouy"

        for (c in w) {
            val isVowel = c in vowels
            if (isVowel && !prevIsVowel) count++
            prevIsVowel = isVowel
        }

        // Silent 'e' at end
        if (w.endsWith("e") && !w.endsWith("le") && count > 1) {
            count--
        }

        return max(1, count)
    }
}
