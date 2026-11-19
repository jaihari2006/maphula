package com.offlineai.app.data

data class SentimentResult(
    val score: Double, // -1.0 (very negative) to +1.0 (very positive)
    val label: String, // Positive, Neutral, Negative, Mixed
    val confidence: Int, // 0 to 100%
    val positiveCount: Int,
    val negativeCount: Int,
    val tones: Map<String, Int> // e.g. "Joy" -> 80, "Caution" -> 20, etc.
)

data class SummaryResult(
    val originalSentenceCount: Int,
    val summarySentenceCount: Int,
    val originalWordCount: Int,
    val summaryWordCount: Int,
    val compressionRatioPercent: Int,
    val keySentences: List<String>
)

data class NamedEntity(
    val text: String,
    val type: String // e.g. NUMBER, EMAIL, URL, CAPITALIZED_ENTITY, DATE
)

data class KeywordResult(
    val topKeywords: List<Pair<String, Double>>,
    val entities: List<NamedEntity>
)

data class ReadabilityResult(
    val fleschScore: Double,
    val readingGrade: String,
    val readingEaseDesc: String,
    val wordCount: Int,
    val sentenceCount: Int,
    val syllableCount: Int,
    val avgWordsPerSentence: Double,
    val estimatedReadingTimeMinutes: Double
)
