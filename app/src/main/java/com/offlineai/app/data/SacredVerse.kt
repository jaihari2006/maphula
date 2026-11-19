package com.offlineai.app.data

/**
 * Structured representation of a sacred scripture verse, philosophical maxim, or scientific principle.
 */
data class SacredVerse(
    val id: String,
    val scriptureName: String,
    val sectionOrChapter: String,
    val originalScript: String,       // Devanagari or Tamil
    val romanTransliteration: String,
    val englishTranslation: String,
    val deepPhilosophicalMeaning: String,
    val associatedDeity: Persona,
    val audioMelodyFrequencies: List<Double>,
    val tags: List<String>
)
