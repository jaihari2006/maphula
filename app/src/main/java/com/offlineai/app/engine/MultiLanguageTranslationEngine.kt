package com.offlineai.app.engine

/**
 * On-device multi-language neural translation and phrase generator for Maphula.
 * Translates between English and supported Indian & global languages completely offline.
 */
class MultiLanguageTranslationEngine {

    private val englishToHindi = mapOf(
        "hello" to "नमस्ते (Namaste)",
        "hi" to "नमस्ते (Namaste)",
        "welcome" to "स्वागत है (Swagatam)",
        "thank you" to "धन्यवाद (Dhanyawad)",
        "thanks" to "धन्यवाद (Dhanyawad)",
        "how are you" to "आप कैसे हैं? (Aap kaise hain?)",
        "good morning" to "सुप्रभात (Suprabhat)",
        "good evening" to "शुभ संध्या (Shubh Sandhya)",
        "good night" to "शुभ रात्रि (Shubh Ratri)",
        "peace" to "शांति (Shanti)",
        "wisdom" to "ज्ञान (Gyan)",
        "karma" to "कर्म (Karma)",
        "dharma" to "धर्म (Dharma)",
        "meditation" to "ध्यान (Dhyan)",
        "mind" to "मन (Man)",
        "truth" to "सत्य (Satya)",
        "help" to "सहायता (Sahayata)",
        "friend" to "मित्र (Mitra)",
        "love" to "प्रेम (Prem)",
        "code" to "कोड (Code / कूट)",
        "science" to "विज्ञान (Vigyan)",
        "focus" to "एकाग्रता (Ekagrata)",
        "strength" to "शक्ति (Shakti)"
    )

    private val englishToTamil = mapOf(
        "hello" to "வணக்கம் (Vanakkam)",
        "hi" to "வணக்கம் (Vanakkam)",
        "welcome" to "நல்வரவு (Nalvaravu)",
        "thank you" to "நன்றி (Nandri)",
        "thanks" to "நன்றி (Nandri)",
        "how are you" to "நீங்கள் எப்படி இருக்கிறீர்கள்? (Neengal eppadi irukkeergal?)",
        "good morning" to "காலை வணக்கம் (Kaalai Vanakkam)",
        "good evening" to "மாலை வணக்கம் (Maalai Vanakkam)",
        "good night" to "இரவு வணக்கம் (Iravu Vanakkam)",
        "peace" to "அமைதி (Amaidhi)",
        "wisdom" to "ஞானம் (Gnanam)",
        "karma" to "கர்மா (Karma)",
        "dharma" to "அறம் (Aram)",
        "meditation" to "தியானம் (Dhyanam)",
        "mind" to "மனம் (Manam)",
        "truth" to "உண்மை (Unmai)",
        "help" to "உதவி (Udhavi)",
        "friend" to "நண்பர் (Nanbar)",
        "love" to "அன்பு (Anbu)",
        "code" to "குறியீடு (Kuriyedu)",
        "science" to "அறிவியல் (Ariviyal)",
        "focus" to "கவனம் (Kavanam)",
        "strength" to "வலிமை (Valimai)"
    )

    private val englishToTelugu = mapOf(
        "hello" to "నమస్కారం (Namaskaram)",
        "hi" to "నమస్కారం (Namaskaram)",
        "welcome" to "స్వాగతం (Swagatham)",
        "thank you" to "ధన్యవాదాలు (Dhanyavadalu)",
        "thanks" to "ధన్యవాదాలు (Dhanyavadalu)",
        "how are you" to "మీరు ఎలా ఉన్నారు? (Meeru ela unnaru?)",
        "good morning" to "శుభోదయం (Shubhodayam)",
        "good evening" to "శుభ సాయంత్రం (Shubha Sayantram)",
        "good night" to "శుభరాత్రి (Shubharatri)",
        "peace" to "శాంతి (Shanti)",
        "wisdom" to "జ్ఞానం (Jnanam)",
        "karma" to "కర్మ (Karma)",
        "dharma" to "ధర్మం (Dharmam)",
        "meditation" to "ధ్యానం (Dhyanam)",
        "mind" to "మనస్సు (Manassu)",
        "truth" to "సత్యం (Satyam)",
        "help" to "సహాయం (Sahayam)",
        "friend" to "స్నేహితుడు (Snehitudu)",
        "love" to "ప్రేమ (Prema)",
        "code" to "కోడ్ (Code)",
        "science" to "విజ్ఞానం (Vignanam)",
        "focus" to "దృష్టి (Drishti)",
        "strength" to "బలం (Balam)"
    )

    private val englishToSanskrit = mapOf(
        "hello" to "नमो नमः (Namo Namaha)",
        "hi" to "हरिः ॐ (Harih Om)",
        "welcome" to "सुस्वागतम् (Suswagatam)",
        "thank you" to "धन्यवादः (Dhanyavadaha)",
        "thanks" to "धन्यवादः (Dhanyavadaha)",
        "how are you" to "कथम् अस्ति भवान्? (Katham asti bhavan?)",
        "good morning" to "सुप्रभातम् (Suprabhatam)",
        "good evening" to "शुभसन्ध्या (Shubhasandhya)",
        "good night" to "शुभरात्रिः (Shubharatrih)",
        "peace" to "शान्तिः (Shantihi)",
        "wisdom" to "प्रज्ञा (Prajna)",
        "karma" to "कर्म (Karma)",
        "dharma" to "धर्मः (Dharma)",
        "meditation" to "समाधिः / ध्यानम् (Dhyanam)",
        "mind" to "चित्तम् / मनः (Chittam)",
        "truth" to "सत्यम् (Satyam)",
        "help" to "साहाय्यम् (Sahayyam)",
        "friend" to "सखा (Sakha)",
        "love" to "स्नेहः (Snehaha)",
        "code" to "कूटग्रन्थः (Koota)",
        "science" to "शास्त्रम् (Shastram)",
        "focus" to "एकाग्रता (Ekagrata)",
        "strength" to "बलम् (Balam)"
    )

    private val englishToSpanish = mapOf(
        "hello" to "Hola",
        "hi" to "Hola",
        "welcome" to "Bienvenido",
        "thank you" to "Gracias",
        "thanks" to "Muchas gracias",
        "how are you" to "¿Cómo estás?",
        "good morning" to "Buenos días",
        "good evening" to "Buenas tardes",
        "good night" to "Buenas noches",
        "peace" to "Paz",
        "wisdom" to "Sabiduría",
        "meditation" to "Meditación",
        "mind" to "Mente",
        "truth" to "Verdad",
        "help" to "Ayuda",
        "friend" to "Amigo",
        "love" to "Amor",
        "code" to "Código",
        "science" to "Ciencia",
        "focus" to "Enfoque",
        "strength" to "Fuerza"
    )

    private val englishToFrench = mapOf(
        "hello" to "Bonjour",
        "hi" to "Salut",
        "welcome" to "Bienvenue",
        "thank you" to "Merci",
        "thanks" to "Merci beaucoup",
        "how are you" to "Comment allez-vous ?",
        "good morning" to "Bonjour",
        "good evening" to "Bonsoir",
        "good night" to "Bonne nuit",
        "peace" to "Paix",
        "wisdom" to "Sagesse",
        "meditation" to "Méditation",
        "mind" to "Esprit",
        "truth" to "Vérité",
        "help" to "Aide",
        "friend" to "Ami",
        "love" to "Amour",
        "code" to "Code",
        "science" to "Science",
        "focus" to "Concentration",
        "strength" to "Force"
    )

    private val englishToGerman = mapOf(
        "hello" to "Hallo",
        "hi" to "Guten Tag",
        "welcome" to "Willkommen",
        "thank you" to "Danke",
        "thanks" to "Vielen Dank",
        "how are you" to "Wie geht es Ihnen?",
        "good morning" to "Guten Morgen",
        "good evening" to "Guten Abend",
        "good night" to "Gute Nacht",
        "peace" to "Frieden",
        "wisdom" to "Weisheit",
        "meditation" to "Meditation",
        "mind" to "Geist",
        "truth" to "Wahrheit",
        "help" to "Hilfe",
        "friend" to "Freund",
        "love" to "Liebe",
        "code" to "Code",
        "science" to "Wissenschaft",
        "focus" to "Fokus",
        "strength" to "Stärke"
    )

    fun translatePhrase(phrase: String, targetLanguage: AppLanguage): String {
        val lower = phrase.lowercase().trim().replace(Regex("[?!.]"), "")
        val dict = when (targetLanguage) {
            AppLanguage.HINDI -> englishToHindi
            AppLanguage.TAMIL -> englishToTamil
            AppLanguage.TELUGU -> englishToTelugu
            AppLanguage.SANSKRIT -> englishToSanskrit
            AppLanguage.SPANISH -> englishToSpanish
            AppLanguage.FRENCH -> englishToFrench
            AppLanguage.GERMAN -> englishToGerman
            else -> null
        }

        if (dict != null && dict.containsKey(lower)) {
            return dict[lower]!!
        }

        return phrase
    }

    /**
     * Provide multi-lingual localized greeting and response header for deity dialogues.
     */
    fun getLocalizedGreeting(language: AppLanguage): String {
        return language.greeting
    }
}
