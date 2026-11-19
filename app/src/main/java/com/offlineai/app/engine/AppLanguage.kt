package com.offlineai.app.engine

import java.util.Locale

/**
 * Supported multi-lingual input and speech locales in Maphula.
 */
enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flagEmoji: String,
    val locale: Locale,
    val greeting: String,
    val promptPlaceholder: String
) {
    ENGLISH(
        code = "en",
        displayName = "English",
        nativeName = "English",
        flagEmoji = "🇺🇸",
        locale = Locale.US,
        greeting = "Namaste! How may I enlighten or assist your journey today?",
        promptPlaceholder = "iMessage • Ask Offline AI in English..."
    ),
    HINDI(
        code = "hi",
        displayName = "Hindi",
        nativeName = "हिन्दी",
        flagEmoji = "🇮🇳",
        locale = Locale("hi", "IN"),
        greeting = "नमस्ते! आज मैं आपकी किस प्रकार सहायता या मार्गदर्शन कर सकता हूँ?",
        promptPlaceholder = "संदेश लिखें • हिन्दी में पूछें..."
    ),
    TAMIL(
        code = "ta",
        displayName = "Tamil",
        nativeName = "தமிழ்",
        flagEmoji = "🇮🇳",
        locale = Locale("ta", "IN"),
        greeting = "வணக்கம்! இன்று உங்களுக்கு நான் எவ்வாறு உதவ முடியும்?",
        promptPlaceholder = "செய்தி எழுதவும் • தமிழில் கேளுங்கள்..."
    ),
    TELUGU(
        code = "te",
        displayName = "Telugu",
        nativeName = "తెలుగు",
        flagEmoji = "🇮🇳",
        locale = Locale("te", "IN"),
        greeting = "నమస్కారం! నేడు మీకు ఏ విధంగా సహాయపడగలను?",
        promptPlaceholder = "సందేశం పంపండి • తెలుగులో అడగండి..."
    ),
    SANSKRIT(
        code = "sa",
        displayName = "Sanskrit",
        nativeName = "संस्कृतम्",
        flagEmoji = "🕉️",
        locale = Locale("sa", "IN"),
        greeting = "नमो नमः! कथम् अहम् भवन्तं साहाय्यं कर्तुम् शक्नोमि?",
        promptPlaceholder = "संदेशं लिखतु • संस्कृतेन पृच्छतु..."
    ),
    SPANISH(
        code = "es",
        displayName = "Spanish",
        nativeName = "Español",
        flagEmoji = "🇪🇸",
        locale = Locale("es", "ES"),
        greeting = "¡Hola! ¿En qué puedo iluminarte o ayudarte hoy?",
        promptPlaceholder = "Escribe un mensaje en español..."
    ),
    FRENCH(
        code = "fr",
        displayName = "French",
        nativeName = "Français",
        flagEmoji = "🇫🇷",
        locale = Locale("fr", "FR"),
        greeting = "Bonjour ! Comment puis-je vous aider ou vous guider aujourd'hui ?",
        promptPlaceholder = "Écrivez un message en français..."
    ),
    GERMAN(
        code = "de",
        displayName = "German",
        nativeName = "Deutsch",
        flagEmoji = "🇩🇪",
        locale = Locale("de", "DE"),
        greeting = "Hallo! Wie kann ich Ihnen heute behilflich sein?",
        promptPlaceholder = "Nachricht auf Deutsch schreiben..."
    ),
    JAPANESE(
        code = "ja",
        displayName = "Japanese",
        nativeName = "日本語",
        flagEmoji = "🇯🇵",
        locale = Locale.JAPAN,
        greeting = "こんにちは！本日はどのようなお手伝いをいたしましょうか？",
        promptPlaceholder = "メッセージを入力 • 日本語で質問..."
    ),
    RUSSIAN(
        code = "ru",
        displayName = "Russian",
        nativeName = "Русский",
        flagEmoji = "🇷🇺",
        locale = Locale("ru", "RU"),
        greeting = "Здравствуйте! Чем я могу вам помочь сегодня?",
        promptPlaceholder = "Введите сообщение на русском..."
    ),
    ARABIC(
        code = "ar",
        displayName = "Arabic",
        nativeName = "العربية",
        flagEmoji = "🇸🇦",
        locale = Locale("ar", "SA"),
        greeting = "مرحباً! كيف يمكنني مساعدتك وإرشادك اليوم؟",
        promptPlaceholder = "اكتب رسالة بالعربية..."
    );

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return values().firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
        }

        /**
         * Detect language of input string based on script unicode blocks and keyword markers.
         */
        fun detectLanguage(input: String): AppLanguage {
            var devanagariCount = 0
            var tamilCount = 0
            var teluguCount = 0
            var arabicCount = 0
            var japaneseCount = 0
            var cyrillicCount = 0
            var latinCount = 0

            for (ch in input) {
                val block = Character.UnicodeBlock.of(ch)
                when (block) {
                    Character.UnicodeBlock.DEVANAGARI -> devanagariCount++
                    Character.UnicodeBlock.TAMIL -> tamilCount++
                    Character.UnicodeBlock.TELUGU -> teluguCount++
                    Character.UnicodeBlock.ARABIC -> arabicCount++
                    Character.UnicodeBlock.HIRAGANA,
                    Character.UnicodeBlock.KATAKANA,
                    Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS -> japaneseCount++
                    Character.UnicodeBlock.CYRILLIC -> cyrillicCount++
                    Character.UnicodeBlock.BASIC_LATIN,
                    Character.UnicodeBlock.LATIN_1_SUPPLEMENT -> latinCount++
                    else -> {}
                }
            }

            val maxNonLatin = maxOf(devanagariCount, tamilCount, teluguCount, arabicCount, japaneseCount, cyrillicCount)
            if (maxNonLatin > 0) {
                return when (maxNonLatin) {
                    devanagariCount -> {
                        // Check for Sanskrit markers
                        val lower = input.lowercase()
                        if (lower.contains("नमः") || lower.contains("अहम्") || lower.contains("भवन्")) SANSKRIT else HINDI
                    }
                    tamilCount -> TAMIL
                    teluguCount -> TELUGU
                    arabicCount -> ARABIC
                    japaneseCount -> JAPANESE
                    cyrillicCount -> RUSSIAN
                    else -> ENGLISH
                }
            }

            // Latin based language detection heuristic
            val lower = input.lowercase(Locale.ROOT)
            val spanishMarkers = listOf("que ", " por ", " para ", " como ", " hola", "¿", "¡", "gracias", "bueno", "amigo")
            val frenchMarkers = listOf(" c'est", " que ", " pour ", " avec ", " bonjour", "merci", "être", "avoir", "oui")
            val germanMarkers = listOf(" und ", " der ", " die ", " das ", " ist ", " nicht ", " hallo", "danke", "bitte")

            if (spanishMarkers.count { lower.contains(it) } >= 2) return SPANISH
            if (frenchMarkers.count { lower.contains(it) } >= 2) return FRENCH
            if (germanMarkers.count { lower.contains(it) } >= 2) return GERMAN

            return ENGLISH
        }
    }
}
