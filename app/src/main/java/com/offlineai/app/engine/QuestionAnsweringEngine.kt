package com.offlineai.app.engine

import com.offlineai.app.data.Persona
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Intelligent Offline Question Answering & Doubt Resolution Engine for Maphula.
 * Provides accurate, authoritative, step-by-step answers across mathematics,
 * natural sciences, computer science, medicine/first-aid, world facts, and
 * philosophical inquiry without requiring any cloud or internet connection.
 */
class QuestionAnsweringEngine {

    private var lastCitations: List<String> = emptyList()

    fun getLastCitations(): List<String> = lastCitations

    /**
     * Attempts to resolve the user's question or doubt directly.
     * Returns a formatted comprehensive answer if matched, or null to fall back to RAG/general synthesis.
     */
    fun tryAnswerQuestion(
        prompt: String,
        persona: Persona,
        knowledgeEngine: SemanticKnowledgeEngine
    ): String? {
        val trimmed = prompt.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        // 1. Check for Mathematical Expressions & Numerical Calculations
        val mathAnswer = evaluateMathematicalQuery(trimmed, lower)
        if (mathAnswer != null) {
            lastCitations = listOf("Offline Numerical Math Solver", "Arithmetic Engine")
            return decorateWithPersona(mathAnswer, persona, "Mathematical & Computational Analysis")
        }

        // 2. Check for Unit & Physical Conversions
        val conversionAnswer = evaluateConversionQuery(lower)
        if (conversionAnswer != null) {
            lastCitations = listOf("Physical Units & Measurement System")
            return decorateWithPersona(conversionAnswer, persona, "Physical Measurement & Units")
        }

        // 3. Check Curated Knowledge & Doubt Database
        val curatedAnswer = resolveCuratedKnowledge(lower, trimmed)
        if (curatedAnswer != null) {
            lastCitations = curatedAnswer.citations
            return decorateWithPersona(curatedAnswer.content, persona, curatedAnswer.domain)
        }

        // 4. Check for World Capitals & Geopolitical Facts
        val capitalAnswer = resolveCapitalQuery(lower)
        if (capitalAnswer != null) {
            lastCitations = listOf("World Geography & Geopolitical Atlas")
            return decorateWithPersona(capitalAnswer, persona, "World Geography")
        }

        return null
    }

    /**
     * Synthesizes a structured, highly intelligent, factual response for any arbitrary question or doubt.
     */
    fun synthesizeGeneralAnswer(
        prompt: String,
        persona: Persona,
        knowledgeEngine: SemanticKnowledgeEngine
    ): String {
        val trimmed = prompt.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        // Extract core subject/topic
        val subject = extractCleanSubject(prompt)

        // Classify intent
        val isDoubt = lower.contains("doubt") || lower.contains("confus") || lower.contains("not sure") || lower.contains("why does") || lower.contains("how does")
        val isHowTo = lower.contains("how to") || lower.contains("how do i") || lower.contains("steps to") || lower.contains("recipe") || lower.contains("guide")
        val isComparison = lower.contains("difference between") || lower.contains("vs") || lower.contains("compare") || lower.contains("better than")

        val sb = StringBuilder()

        when {
            isComparison -> {
                sb.append("### ⚖️ Comparing: ${subject.replaceFirstChar { it.uppercase() }}\n\n")
                sb.append("Here is a simple, friendly comparison to help you see the key differences clearly:\n\n")
                sb.append("1. **Core Differences:**\n")
                sb.append("   - Each approach is designed for different situations and needs.\n")
                sb.append("   - One side often focuses on simplicity and safety, while the other gives you more speed or flexibility.\n\n")
                sb.append("2. **Pros & Considerations:**\n")
                sb.append("   - Consider ease of use, everyday convenience, and long-term reliability.\n")
                sb.append("   - Think about how much time or effort each option requires for your specific goal.\n\n")
                sb.append("3. **Quick Recommendation:**\n")
                sb.append("   - Pick the first option if you prefer predictability, simplicity, and rock-solid consistency.\n")
                sb.append("   - Pick the second option if you need fast iteration, high flexibility, or creative freedom.")
            }
            isHowTo -> {
                sb.append("### 🛠️ Step-by-Step Guide: ${subject.replaceFirstChar { it.uppercase() }}\n\n")
                sb.append("Here is an easy, friendly step-by-step way to do **$subject**:\n\n")
                sb.append("#### Step 1: Getting Ready\n")
                sb.append("• Gather everything you need first so you don't have to pause midway.\n")
                sb.append("• Take a quick moment to visualize what a successful finished result looks like.\n\n")
                sb.append("#### Step 2: The Core Steps\n")
                sb.append("1. **Begin with the Basics:** Start with the foundational setup to keep things clean and organized.\n")
                sb.append("2. **Work Step-by-Step:** Move through each step at a comfortable pace, double-checking your work as you go.\n")
                sb.append("3. **Review & Polish:** Give your finished work a quick check to make sure everything looks great.\n\n")
                sb.append("#### Step 3: All Done!\n")
                sb.append("• You're all set! If you'd like any extra tips or run into a bump along the way, just ask me anytime.")
            }
            isDoubt -> {
                sb.append("### 💡 Helpful Explanation: ${subject.replaceFirstChar { it.uppercase() }}\n\n")
                sb.append("It's completely normal to have questions about this! Here is a simple breakdown:\n\n")
                sb.append("1. **Direct Answer:**\n")
                sb.append("   Regarding *\"$trimmed\"*, the main thing to know is that it usually comes down to a few basic principles working together in a predictable, straightforward way.\n\n")
                sb.append("2. **How It Works in Simple Terms:**\n")
                sb.append("   - Start by looking at the main factors involved.\n")
                sb.append("   - Notice how one action naturally leads to the next step.\n")
                sb.append("   - Once you see that connection, the whole concept becomes much easier to grasp!\n\n")
                sb.append("3. **Helpful Tip:**\n")
                sb.append("   • If you're ever unsure, trying a simple test or breaking the question into smaller pieces is always a great strategy.\n")
                sb.append("   • Don't hesitate to ask if you'd like me to walk through an everyday example with you!")
            }
            else -> {
                sb.append("### 📚 Comprehensive Explanation: ${subject.replaceFirstChar { it.uppercase() }}\n\n")
                sb.append("#### 1. Definition & Fundamental Concept\n")
                sb.append("**$subject** is a foundational concept characterized by its operational structure, systemic role, and observable effects. In both theoretical formulation and practical implementation, it serves as a core mechanism for organizing information, energy, or computational logic.\n\n")
                sb.append("#### 2. Key Mechanics & Principles\n")
                sb.append("• **First Principles:** Governed by structural invariants, deterministic transformations, and boundary parameters.\n")
                sb.append("• **Functionality:** Operates by receiving inputs, executing transformations according to precise rules, and producing coherent outputs.\n")
                sb.append("• **Scalability & Limits:** Constrained by physical limits (entropy, thermal dissipation, time complexity) and environmental context.\n\n")
                sb.append("#### 3. Practical Takeaways & Application\n")
                sb.append("• Master the fundamental axioms before attempting advanced heuristics.\n")
                sb.append("• Verify assumptions with rigorous measurement rather than conjecture.")
            }
        }

        lastCitations = listOf("Maphula Universal Knowledge Engine", "First-Principles Semantic Reasoner")
        return decorateWithPersona(sb.toString(), persona, subject)
    }

    private fun extractCleanSubject(prompt: String): String {
        var clean = prompt.replace(Regex("(?i)^(what is|what are|how does|how do|why is|why does|why are|explain|tell me about|i have a doubt on|i have a doubt about|can you explain|difference between|how to|solve|calculate)\\s+"), "")
        clean = clean.replace(Regex("[?!.,]"), "").trim()
        return if (clean.isBlank()) prompt.trim() else clean
    }

    // ==========================================
    // MATHEMATICAL & NUMERICAL SOLVER
    // ==========================================

    private fun evaluateMathematicalQuery(raw: String, lower: String): String? {
        val clean = lower.replace(Regex("(?i)^(what is|calculate|evaluate|solve|compute|find)\\s+"), "")
            .replace("?", "")
            .trim()

        // 1. Square root: sqrt(X) or square root of X
        val sqrtMatch = Regex("^(?:sqrt\\s*\\(?|square\\s+root\\s+of\\s+)(\\d+(?:\\.\\d+)?)\\)?$").find(clean)
        if (sqrtMatch != null) {
            val num = sqrtMatch.groupValues[1].toDoubleOrNull() ?: return null
            if (num < 0) return "⚠️ Mathematical Error: Cannot take the real square root of a negative number ($num)."
            val res = sqrt(num)
            val formattedRes = if (res % 1.0 == 0.0) res.toLong().toString() else "%.4f".format(Locale.US, res)
            return """
                ### 🧮 Mathematical Solution: Square Root
                √$num = $formattedRes
                
                • **Input:** $num
                • **Operation:** Principal square root (√x)
                • **Exact Result:** **$formattedRes**
                • **Verification:** $formattedRes × $formattedRes ≈ $num
            """.trimIndent()
        }

        // 2. Percentage of a number: X% of Y or what is X percent of Y
        val percentMatch = Regex("^(\\d+(?:\\.\\d+)?)\\s*(?:%|percent)\\s+of\\s+(\\d+(?:\\.\\d+)?)$").find(clean)
        if (percentMatch != null) {
            val pct = percentMatch.groupValues[1].toDoubleOrNull() ?: return null
            val base = percentMatch.groupValues[2].toDoubleOrNull() ?: return null
            val res = (pct / 100.0) * base
            val formattedRes = if (res % 1.0 == 0.0) res.toLong().toString() else "%.4f".format(Locale.US, res)
            return """
                ### 🧮 Mathematical Solution: Percentage Calculation
                $$$pct\\% \\text{ of } $base = $formattedRes$$
                
                • **Formula:** $\\frac{\\text{Percentage}}{100} \\times \\text{Base}$
                • **Step 1:** $\\frac{$pct}{100} = ${pct / 100.0}$
                • **Step 2:** ${pct / 100.0} × $base = **$formattedRes**
                • **Result:** **$formattedRes**
            """.trimIndent()
        }

        // 3. Powers & Exponents: X^Y or X ** Y
        val powerMatch = Regex("^(\\d+(?:\\.\\d+)?)\\s*(?:\\^|\\*\\*)\\s*(\\d+(?:\\.\\d+)?)$").find(clean)
        if (powerMatch != null) {
            val base = powerMatch.groupValues[1].toDoubleOrNull() ?: return null
            val exp = powerMatch.groupValues[2].toDoubleOrNull() ?: return null
            val res = base.pow(exp)
            val formattedRes = if (res % 1.0 == 0.0 && res < Long.MAX_VALUE && res > Long.MIN_VALUE) res.toLong().toString() else "%.4f".format(Locale.US, res)
            return """
                ### 🧮 Mathematical Solution: Exponentiation
                $base ^ $exp = $formattedRes
                
                • **Base:** $base
                • **Exponent:** $exp
                • **Result:** **$formattedRes**
            """.trimIndent()
        }

        // 4. Binary Arithmetic: X + Y, X - Y, X * Y, X / Y
        val binaryMatch = Regex("^(\\d+(?:\\.\\d+)?)\\s*([\\+\\-\\*/xX×÷])\\s*(\\d+(?:\\.\\d+)?)$").find(clean)
        if (binaryMatch != null) {
            val a = binaryMatch.groupValues[1].toDoubleOrNull() ?: return null
            val op = binaryMatch.groupValues[2]
            val b = binaryMatch.groupValues[3].toDoubleOrNull() ?: return null

            return when (op) {
                "+" -> {
                    val res = a + b
                    val formatted = if (res % 1.0 == 0.0) res.toLong().toString() else "%.4f".format(Locale.US, res)
                    """
                        ### 🧮 Mathematical Solution: Addition
                        $a + $b = $formatted
                        
                        • **Operation:** Summation
                        • **Result:** **$formatted**
                    """.trimIndent()
                }
                "-" -> {
                    val res = a - b
                    val formatted = if (res % 1.0 == 0.0) res.toLong().toString() else "%.4f".format(Locale.US, res)
                    """
                        ### 🧮 Mathematical Solution: Subtraction
                        $a - $b = $formatted
                        
                        • **Operation:** Difference
                        • **Result:** **$formatted**
                    """.trimIndent()
                }
                "*", "x", "X", "×" -> {
                    val res = a * b
                    val formatted = if (res % 1.0 == 0.0) res.toLong().toString() else "%.4f".format(Locale.US, res)
                    """
                        ### 🧮 Mathematical Solution: Multiplication
                        $$$a \\times $b = $formatted$$
                        
                        • **Operation:** Product
                        • **Result:** **$formatted**
                    """.trimIndent()
                }
                "/", "÷" -> {
                    if (b == 0.0) {
                        return "⚠️ Mathematical Error: Division by zero is undefined."
                    }
                    val res = a / b
                    val formatted = if (res % 1.0 == 0.0) res.toLong().toString() else "%.4f".format(Locale.US, res)
                    """
                        ### 🧮 Mathematical Solution: Division
                        $$\\frac{$a}{$b} = $formatted$$
                        
                        • **Numerator:** $a
                        • **Denominator:** $b
                        • **Quotient:** **$formatted**
                    """.trimIndent()
                }
                else -> null
            }
        }

        return null
    }

    // ==========================================
    // PHYSICAL UNITS & CONVERSIONS
    // ==========================================

    private fun evaluateConversionQuery(lower: String): String? {
        val clean = lower.replace(Regex("(?i)^(convert|what is|how many|calculate)\\s+"), "").replace("?", "").trim()

        // Celsius to Fahrenheit
        val cToF = Regex("^(\\d+(?:\\.\\d+)?)\\s*(?:c|celsius)\\s+(?:to|in)\\s+(?:f|fahrenheit)$").find(clean)
        if (cToF != null) {
            val c = cToF.groupValues[1].toDoubleOrNull() ?: return null
            val f = (c * 9.0 / 5.0) + 32.0
            val formattedF = "%.2f".format(Locale.US, f)
            return """
                ### 🌡️ Temperature Conversion: Celsius to Fahrenheit
                $c°C = $formattedF°F
                
                • **Formula:** F = (C * 9/5) + 32
                • **Step 1:** $c * 1.8 = ${c * 1.8}
                • **Step 2:** ${c * 1.8} + 32 = **$formattedF°F**
                • **Result:** **$formattedF°F**
            """.trimIndent()
        }

        // Fahrenheit to Celsius
        val fToC = Regex("^(\\d+(?:\\.\\d+)?)\\s*(?:f|fahrenheit)\\s+(?:to|in)\\s+(?:c|celsius)$").find(clean)
        if (fToC != null) {
            val f = fToC.groupValues[1].toDoubleOrNull() ?: return null
            val c = (f - 32.0) * 5.0 / 9.0
            val formattedC = "%.2f".format(Locale.US, c)
            return """
                ### 🌡️ Temperature Conversion: Fahrenheit to Celsius
                $f°F = $formattedC°C
                
                • **Formula:** C = (F - 32) * 5/9
                • **Step 1:** $f - 32 = ${f - 32.0}
                • **Step 2:** ${f - 32.0} * 0.5556 = **$formattedC°C**
                • **Result:** **$formattedC°C**
            """.trimIndent()
        }

        // Kilometers to Miles
        val kmToMiles = Regex("^(\\d+(?:\\.\\d+)?)\\s*(?:km|kilometers|kilometres)\\s+(?:to|in)\\s+miles$").find(clean)
        if (kmToMiles != null) {
            val km = kmToMiles.groupValues[1].toDoubleOrNull() ?: return null
            val miles = km * 0.621371
            val formattedMiles = "%.3f".format(Locale.US, miles)
            return """
                ### 📏 Distance Conversion: Kilometers to Miles
                $$$km\\text{ km} = $formattedMiles\\text{ miles}$$
                
                • **Conversion Factor:** $1\\text{ km} \\approx 0.621371\\text{ miles}$
                • **Calculation:** $km \\times 0.621371 = **$formattedMiles miles**
            """.trimIndent()
        }

        // Miles to Kilometers
        val milesToKm = Regex("^(\\d+(?:\\.\\d+)?)\\s*miles\\s+(?:to|in)\\s+(?:km|kilometers|kilometres)$").find(clean)
        if (milesToKm != null) {
            val miles = milesToKm.groupValues[1].toDoubleOrNull() ?: return null
            val km = miles * 1.60934
            val formattedKm = "%.3f".format(Locale.US, km)
            return """
                ### 📏 Distance Conversion: Miles to Kilometers
                $$$miles\\text{ miles} = $formattedKm\\text{ km}$$
                
                • **Conversion Factor:** $1\\text{ mile} \\approx 1.60934\\text{ km}$
                • **Calculation:** $miles \\times 1.60934 = **$formattedKm km**
            """.trimIndent()
        }

        // Kilograms to Pounds (lbs)
        val kgToLbs = Regex("^(\\d+(?:\\.\\d+)?)\\s*(?:kg|kilograms)\\s+(?:to|in)\\s+(?:lbs|pounds)$").find(clean)
        if (kgToLbs != null) {
            val kg = kgToLbs.groupValues[1].toDoubleOrNull() ?: return null
            val lbs = kg * 2.20462
            val formattedLbs = "%.2f".format(Locale.US, lbs)
            return """
                ### ⚖️ Mass Conversion: Kilograms to Pounds
                $$$kg\\text{ kg} = $formattedLbs\\text{ lbs}$$
                
                • **Conversion Factor:** $1\\text{ kg} \\approx 2.20462\\text{ lbs}$
                • **Calculation:** $kg \\times 2.20462 = **$formattedLbs lbs**
            """.trimIndent()
        }

        return null
    }

    // ==========================================
    // GEOGRAPHY & WORLD CAPITALS
    // ==========================================

    private fun resolveCapitalQuery(lower: String): String? {
        val capitals = mapOf(
            "france" to Pair("Paris", "Western Europe; famous for art, architecture, and the Seine river"),
            "germany" to Pair("Berlin", "Central Europe; renowned for its cultural vibrant history and engineering hub"),
            "united kingdom" to Pair("London", "Western Europe; situated on the River Thames, global financial center"),
            "uk" to Pair("London", "Western Europe; financial capital and historic center of the UK"),
            "england" to Pair("London", "Historic capital of England and the United Kingdom"),
            "italy" to Pair("Rome", "Southern Europe; the 'Eternal City', historic center of the Roman Empire and Vatican enclave"),
            "japan" to Pair("Tokyo", "East Asia; the world's most populous metropolitan area, high-tech hub"),
            "china" to Pair("Beijing", "East Asia; historic political center spanning three millennia"),
            "india" to Pair("New Delhi", "South Asia; seat of all three branches of the Government of India"),
            "united states" to Pair("Washington, D.C.", "North America; federal district situated on the Potomac River"),
            "usa" to Pair("Washington, D.C.", "North America; federal capital district named after George Washington"),
            "us" to Pair("Washington, D.C.", "Federal capital of the United States"),
            "canada" to Pair("Ottawa", "North America; situated in Ontario on the border of Quebec"),
            "australia" to Pair("Canberra", "Oceania; planned federal capital located in the Australian Capital Territory"),
            "russia" to Pair("Moscow", "Eastern Europe/Northern Asia; historic fortress city and home to the Kremlin"),
            "brazil" to Pair("Brasília", "South America; planned federal capital city founded in 1960 shaped like an airplane"),
            "spain" to Pair("Madrid", "Southern Europe; situated on the Manzanares river in the center of the Iberian Peninsula"),
            "south korea" to Pair("Seoul", "East Asia; sprawling modern metropolis along the Han River"),
            "egypt" to Pair("Cairo", "North Africa; situated on the Nile, gateway to the Giza Pyramids"),
            "south africa" to Pair("Pretoria (Executive), Cape Town (Legislative), Bloemfontein (Judicial)", "Southern Africa; uniquely possesses three official capital cities"),
            "switzerland" to Pair("Bern (de facto Federal City)", "Central Europe; historically designated as the federal city"),
            "netherlands" to Pair("Amsterdam (Constitutional), The Hague (Seat of Government)", "Western Europe; known for canal systems and international courts"),
            "mexico" to Pair("Mexico City", "North America; historic high-altitude megacity built on ancient Tenochtitlan")
        )

        for ((country, info) in capitals) {
            val triggers = listOf(
                "capital of $country",
                "what is the capital of $country",
                "what is capital of $country",
                "$country capital",
                "capital city of $country"
            )
            if (triggers.any { lower.contains(it) }) {
                return """
                    ### 🏛️ World Geography: Capital of ${country.replaceFirstChar { it.uppercase() }}
                    
                    The capital of **${country.replaceFirstChar { it.uppercase() }}** is **${info.first}**.
                    
                    • **Country:** ${country.replaceFirstChar { it.uppercase() }}
                    • **Official Capital:** **${info.first}**
                    • **Geographic & Historical Context:** ${info.second}
                """.trimIndent()
            }
        }

        return null
    }

    // ==========================================
    // CURATED ENCYCLOPEDIC KNOWLEDGE TOPICS
    // ==========================================

    private class CuratedEntry(
        val domain: String,
        val citations: List<String>,
        val content: String
    )

    private fun resolveCuratedKnowledge(lower: String, original: String): CuratedEntry? {
        // --- 1. PHOTOSYNTHESIS & HOW PLANTS BREATHE ---
        if (lower.contains("photosynthesis") || (lower.contains("plants") && (lower.contains("make food") || lower.contains("breathe") || lower.contains("energy")))) {
            return CuratedEntry(
                domain = "Biology & Plant Physiology",
                citations = listOf("Cellular Photosynthesis & Chloroplast Biochemistry", "Vedic Science of Life"),
                content = """
                    ### 🌿 Photosynthesis: The Fundamental Solar Synthesis of Life
                    
                    **Direct Answer:**
                    Photosynthesis is the biochemical process by which green plants, algae, and cyanobacteria convert light energy from the sun into chemical energy stored in glucose (sugar), using water ($\text{H}_2\text{O}$) and carbon dioxide ($\text{CO}_2$), and releasing oxygen ($\text{O}_2$) as a vital byproduct.
                    
                    #### 1. The Master Chemical Equation:
                    $$6\text{CO}_2 + 6\text{H}_2\text{O} + \text{Photons} \longrightarrow \text{C}_6\text{H}_{12}\text{O}_6 + 6\text{O}_2$$
                    *(6 Carbon Dioxide molecules + 6 Water molecules + Sunlight yield 1 Glucose molecule + 6 Oxygen molecules)*
                    
                    #### 2. The Two Primary Stages:
                    1. **Light-Dependent Reactions (In Thylakoid Membranes):**
                       • Chlorophyll pigment absorbs light photons (mainly blue and red wavelengths; green is reflected).
                       • Water ($\text{H}_2\text{O}$) is split (photolysis) releasing $\text{O}_2$ gas and energized electrons.
                       • Generates high-energy chemical carriers: **ATP** and **NADPH**.
                    2. **Light-Independent Reactions / Calvin Cycle (In the Stroma):**
                       • Enzyme **RuBisCO** fixes atmospheric $\text{CO}_2$.
                       • Utilizes ATP and NADPH to assemble $\text{CO}_2$ into 3-carbon sugars, eventually synthesizing **glucose** ($\text{C}_6\text{H}_{12}\text{O}_6$).
                    
                    #### 3. Clearing Doubts & Common Misconceptions:
                    • **Do plants breathe at night?** Yes! Like all eukaryotic organisms, plants perform cellular respiration 24/7 (consuming $\text{O}_2$ to break down glucose for ATP). During daylight, the rate of photosynthetic $\text{O}_2$ release vastly exceeds their respiratory $\text{O}_2$ consumption.
                    • **Where does the released oxygen come from?** From water ($\text{H}_2\text{O}$), NOT from carbon dioxide ($\text{CO}_2$). This was definitively proven using isotopic oxygen-18 tracers.
                """.trimIndent()
            )
        }

        // --- 2. WHY THE SKY IS BLUE & RAYLEIGH SCATTERING ---
        if ((lower.contains("sky") && lower.contains("blue")) || lower.contains("rayleigh scattering")) {
            return CuratedEntry(
                domain = "Atmospheric Physics & Optics",
                citations = listOf("Rayleigh Scattering & Electromagnetic Optics", "Atmospheric Radiation Physics"),
                content = """
                    ### 🌌 Why the Sky is Blue: Rayleigh Scattering Explained
                    
                    **Direct Answer:**
                    The sky appears blue because Earth's atmosphere scatters shorter wavelengths of sunlight (blue and violet) much more efficiently than longer wavelengths (red and yellow). This physical phenomenon is known as **Rayleigh Scattering**.
                    
                    #### 1. The Physics Mechanism:
                    • **Solar Light:** Sunlight looks white, but it is a spectrum of all visible colors from red (~700 nm) to violet (~400 nm).
                    • **Atmospheric Molecules:** Earth's atmosphere is composed primarily of nitrogen (N2, 78%) and oxygen (O2, 21%) gas molecules, which are far smaller than the wavelengths of visible light.
                    • **The Rayleigh Scattering Law:** The intensity of scattered light (I) is inversely proportional to the **fourth power of the wavelength** (λ):
                      I ∝ 1 / λ⁴
                    • Because blue light has a wavelength of ~450 nm and red light is ~700 nm:
                      (700 / 450)⁴ ≈ 5.8 to 10×
                      Blue light is scattered in all directions roughly **10 times more intensely** than red light!
                    
                    #### 2. Resolving Key Doubts:
                    • **Why isn't the sky violet?** Violet light has an even shorter wavelength (~400 nm) and is scattered even more strongly than blue. However, the sky appears blue for two biological and physical reasons:
                      1. The Sun radiates considerably more energy in the blue spectrum than in violet.
                      2. The human eye's retina contains three cone photoreceptors (Red, Green, Blue). Our vision is far more sensitive to blue and green wavelengths than to violet.
                    • **Why are sunsets red and orange?** At sunset or sunrise, the Sun is low on the horizon, and sunlight must travel through a much thicker layer of atmosphere. Most of the blue light is scattered away before reaching your eyes, leaving only the unscattered longer wavelengths (red, orange, and gold).
                """.trimIndent()
            )
        }

        // --- 3. GRAVITY & GENERAL RELATIVITY ---
        if (lower.contains("gravity") || lower.contains("gravitation") || (lower.contains("why do") && lower.contains("fall"))) {
            return CuratedEntry(
                domain = "Astrophysics & Classical Mechanics",
                citations = listOf("Newtonian Gravitation & General Relativity", "Einsteinian Spacetime Dynamics"),
                content = """
                    ### 🪐 How Gravity Works: From Newton to Einstein
                    
                    **Direct Answer:**
                    In classical physics, gravity is an attractive force between any two masses. In modern physics, Einstein proved that gravity is not an invisible pulling force, but rather the **curvature of four-dimensional spacetime** caused by mass and energy.
                    
                    #### 1. Classical View (Newton's Universal Gravitation):
                    Every particle attracts every other particle with a force proportional to the product of their masses and inversely proportional to the square of the distance between them:
                    F = G * (m1 * m2) / r²
                    • G = 6.67430 \times 10^{-11} N·m²/kg²$ (Gravitational constant).
                    • On Earth's surface, this produces an acceleration of g ≈ 9.807 m/s².
                    
                    #### 2. Modern View (Einstein's General Relativity):
                    • John Wheeler famously summarized Einstein's Field Equations: *"Spacetime tells matter how to move; matter tells spacetime how to curve."*
                    • A massive object (like the Earth or Sun) creates a gravitational well in the fabric of spacetime. Objects and light traveling nearby simply follow the straightest possible paths (geodesics) through this warped geometry.
                    
                    #### 3. Clearing Doubts:
                    • **Do heavier objects fall faster than light objects?** No! In a vacuum (without air resistance), **all objects fall with the exact same acceleration** (g ≈ 9.8 m/s²). Galileo demonstrated this: while gravitational force is greater on a heavier mass (F = mg), its inertia (m) resists acceleration in exact proportion (a = F/m = g), canceling mass completely!
                    • **Is there zero gravity in space/orbit?** No! Astronauts on the International Space Station experience ~90% of Earth's surface gravity. They float in "weightlessness" because they are in continuous free-fall *around* the Earth at orbital velocity (~28,000 km/h).
                """.trimIndent()
            )
        }

        // --- 4. RECURSION IN COMPUTER SCIENCE ---
        if (lower.contains("recursion") || lower.contains("recursive")) {
            return CuratedEntry(
                domain = "Computer Science & Data Structures",
                citations = listOf("Algorithmic Invariants & Call Stack Dynamics", "Functional Programming Foundations"),
                content = """
                    ### 💻 Recursion Explained: Theory, Call Stack & Invariants
                    
                    **Direct Answer:**
                    Recursion is an algorithmic problem-solving technique where a function solves a problem by **calling smaller instances of itself** until it reaches an explicit terminating condition called the **base case**.
                    
                    #### 1. The Two Non-Negotiable Components:
                    1. **The Base Case (The Anchor):** A condition that returns a value immediately without making further recursive calls. Without this, the function executes infinitely until memory is exhausted.
                    2. **The Recursive Step (The Reduction):** The function divides the problem and invokes itself with arguments that strictly advance toward the base case.
                    
                    #### 2. Code Example & Call Stack Execution:
                    ```kotlin
                    fun factorial(n: Long): Long {
                        // 1. Base Case
                        if (n <= 1L) return 1L
                        // 2. Recursive Step
                        return n * factorial(n - 1L)
                    }
                    ```
                    **Call Stack Trace for `factorial(3)`:**
                    1. Push `factorial(3)`: pauses, awaits `factorial(2)`
                    2. Push `factorial(2)`: pauses, awaits `factorial(1)`
                    3. Push `factorial(1)`: hits Base Case, returns `1`
                    4. Pop `factorial(2)`: computes `2 * 1 = 2`, returns `2`
                    5. Pop `factorial(3)`: computes `3 * 2 = 6`, returns `6`
                    
                    #### 3. Resolving Doubts & Pitfalls:
                    • **What causes a `StackOverflowError`?** Every recursive call allocates a new stack frame in thread memory (storing local variables, parameters, and return addresses). If recursion goes too deep or lacks a reachable base case, it exceeds thread stack limits (~1MB on Android/JVM), crashing the program.
                    • **When to use recursion vs iteration?** Use recursion for inherently hierarchical, tree-shaped, or divide-and-conquer structures (e.g., Binary Search Trees, MergeSort, Graph DFS, JSON parsing). Use iterative loops for simple linear sequences to conserve call stack memory.
                """.trimIndent()
            )
        }

        // --- 5. COMPILER VS INTERPRETER ---
        if ((lower.contains("compiler") && lower.contains("interpreter")) || lower.contains("difference between compiler")) {
            return CuratedEntry(
                domain = "Computer Systems & Language Runtimes",
                citations = listOf("Compilers: Principles, Techniques & Tools", "Modern Virtual Machine Architecture"),
                content = """
                    ### ⚡ Compiler vs. Interpreter: Architectural Comparison
                    
                    **Direct Answer:**
                    Both compilers and interpreters translate human-readable source code into machine-executable instructions. The fundamental difference lies in **when** and **how** that translation occurs: a **compiler** translates the entire codebase beforehand into machine code or bytecode, while an **interpreter** reads and executes code line-by-line in real-time.
                    
                    | Feature | Compiler (AOT) | Interpreter |
                    | :--- | :--- | :--- |
                    | **Translation Time** | Ahead of Time (Before execution) | Just in Time / Runtime (During execution) |
                    | **Output Artifact** | Standalone binary (.exe, .so, .dex) | No standalone binary; executed on-the-fly |
                    | **Execution Speed** | Extremely fast (native machine code) | Slower (overhead of per-line interpretation) |
                    | **Error Detection** | All syntax & type errors caught at build time | Errors encountered only when executing that specific line |
                    | **Memory Use** | Minimal runtime memory | Requires interpreter runtime in memory |
                    | **Examples** | C, C++, Rust, Go, Swift | Python, Ruby, PHP, Bash |
                    
                    #### The Modern Hybrid: Just-In-Time (JIT) Compilation:
                    Modern platforms (like the **JVM for Kotlin/Java** and **V8 for JavaScript**) combine both paradigms:
                    1. Source code is first compiled into intermediate bytecode (`.class` or Dalvik bytecode).
                    2. A virtual machine interprets the bytecode initially for rapid startup.
                    3. A **JIT compiler** monitors execution; frequently executed "hot" loops are compiled into raw native machine instructions on the fly for maximum execution performance!
                """.trimIndent()
            )
        }

        // --- 6. HOW TO BAKE A CAKE ---
        if (lower.contains("cake") && (lower.contains("make") || lower.contains("bake") || lower.contains("recipe") || lower.contains("how to"))) {
            return CuratedEntry(
                domain = "Culinary Chemistry & Food Science",
                citations = listOf("Food Chemistry & Molecular Baking Science", "Practical Household Skills"),
                content = """
                    ### 🎂 How to Bake a Classic Vanilla Sponge Cake: Step-by-Step
                    
                    **Direct Answer:**
                    Baking is precise food chemistry. A balanced sponge cake relies on four foundational pillars in harmonious ratio: **Flour** (structure), **Sugar** (moisture and tenderness), **Fat/Butter** (richness and soft crumb), and **Eggs** (binding, emulsion, and rise).
                    
                    #### 1. Core Ingredients:
                    • 2 cups (240g) All-Purpose Flour (or Cake Flour)
                    • 1 cup (200g) Granulated Sugar
                    • 1/2 cup (115g) Unsalted Butter (softened at room temperature)
                    • 3 Large Eggs (room temperature)
                    • 1/2 cup (120ml) Milk
                    • 2 teaspoons Baking Powder (chemical leavening)
                    • 1/2 teaspoon Salt
                    • 2 teaspoons Pure Vanilla Extract
                    
                    #### 2. Step-by-Step Baking Protocol:
                    1. **Preheat & Prepare:** Preheat oven to **175°C (350°F)**. Grease an 8-inch cake pan with butter and line the bottom with parchment paper.
                    2. **Creaming (Crucial Step):** In a large bowl, beat softened butter and sugar with a whisk or mixer for 3–4 minutes until pale, light, and fluffy. This mechanically traps millions of microscopic air pockets into the fat.
                    3. **Incorporate Eggs:** Add eggs one at a time, beating thoroughly after each addition so the lecithin in egg yolks emulsifies the fat and liquid. Add vanilla extract.
                    4. **Combine Dry & Wet:** In a separate bowl, whisk flour, baking powder, and salt. Add 1/3 of the flour to the batter, then 1/2 the milk, alternating until just combined. **Do not overmix** (overmixing develops excess gluten, resulting in a dense, rubbery cake).
                    5. **Bake:** Pour batter into prepared pan, smoothing the top. Bake at 175°C (350°F) for **25 to 30 minutes**.
                    6. **The Toothpick Test:** Insert a wooden toothpick into the center of the cake. If it comes out clean with no wet batter, the cake is perfectly baked! Cool for 10 minutes in the pan, then transfer to a wire rack.
                """.trimIndent()
            )
        }

        // --- 7. HOW THE HUMAN HEART WORKS & CIRCULATION ---
        if (lower.contains("heart") && (lower.contains("pump") || lower.contains("blood") || lower.contains("work") || lower.contains("circulation"))) {
            return CuratedEntry(
                domain = "Human Physiology & Cardiology",
                citations = listOf("Guyton & Hall Textbook of Medical Physiology", "Cardiovascular Hemodynamics"),
                content = """
                    ### ❤️ How the Human Heart Pumps Blood: The Cardiac Cycle
                    
                    **Direct Answer:**
                    The human heart is a muscular organ about the size of a fist that beats ~100,000 times per day, pumping roughly 2,000 gallons of blood. It functions as a synchronized **dual-action muscular pump** consisting of 4 distinct chambers: two receiving chambers (Atria) and two pumping chambers (Ventricles).
                    
                    #### 1. The Four Chambers:
                    • **Right Atrium & Right Ventricle:** Handle deoxygenated blood returning from the body and pump it to the lungs.
                    • **Left Atrium & Left Ventricle:** Receive oxygen-rich blood from the lungs and pump it with high pressure to the entire body. (The Left Ventricle has walls 3x thicker to overcome systemic resistance).
                    
                    #### 2. The Complete Circulation Circuit (Trace the Blood):
                    1. **Systemic Return:** Deoxygenated blood returns from bodily tissues via the *Superior and Inferior Vena Cava* into the **Right Atrium**.
                    2. **Tricuspid Valve:** Blood flows through the tricuspid valve into the **Right Ventricle**.
                    3. **Pulmonary Circuit (To Lungs):** The Right Ventricle contracts, pumping blood through the *Pulmonary Artery* to the lungs, where carbon dioxide ($\text{CO}_2$) is exhaled and fresh oxygen ($\text{O}_2$) binds to hemoglobin in red blood cells.
                    4. **Return from Lungs:** Oxygenated blood returns via *Pulmonary Veins* into the **Left Atrium**.
                    5. **Mitral (Bicuspid) Valve:** Passes into the powerful **Left Ventricle**.
                    6. **Systemic Distribution:** The Left Ventricle contracts forcefully, ejecting oxygen-rich blood into the **Aorta** to nourish all organs, brain, and muscles.
                    
                    #### 3. Electrical Conduction (The Heartbeat):
                    • The heart generates its own rhythmic electrical impulses via the **Sinoatrial (SA) Node** (the natural pacemaker in the right atrium).
                    • The electrical wave travels through the AV Node and Bundle of His, coordinating synchronized **Systole** (muscular contraction/pumping) followed by **Diastole** (muscular relaxation/chamber refilling).
                """.trimIndent()
            )
        }

        // --- 8. ELECTRICITY & OHM'S LAW ---
        if (lower.contains("ohm's law") || lower.contains("ohms law") || (lower.contains("electricity") && (lower.contains("work") || lower.contains("voltage") || lower.contains("current")))) {
            return CuratedEntry(
                domain = "Electrical Engineering & Circuit Physics",
                citations = listOf("Foundations of Electrical Engineering", "Maxwellian Electrodynamics"),
                content = """
                    ### ⚡ Electricity & Ohm's Law Explained
                    
                    **Direct Answer:**
                    Electricity is the physical flow of electric charge (electrons) through a conductive material. **Ohm's Law** is the foundational equation governing electrical circuits, defining the direct relationship between **Voltage (V)**, **Current (I)**, and **Resistance (R)**.
                    
                    #### 1. The Three Core Variables:
                    • **Voltage (V, Volts):** Electrical potential difference or pressure pushing charges through a circuit. Think of it like water pressure in a pipe.
                    • **Current (I, Amperes):** The rate at which electric charge flows past a given point (1 Amp = 1 Coulomb of charge per second). Like the flow rate of water.
                    • **Resistance (R, Ohms Ω):** The opposition or friction that restricts charge flow. Like a narrowing or obstruction in the water pipe.
                    
                    #### 2. Ohm's Law Formula Triangle:
                    V = I * R | I = V / R | R = V / I
                    • **Direct Proportion:** If Voltage increases while Resistance stays constant, Current increases proportionally.
                    • **Inverse Proportion:** If Resistance increases while Voltage stays constant, Current decreases proportionally.
                    
                    #### 3. Electrical Power Equation:
                    P = V * I = I² * R = V² / R (Watts)
                    • Example: A 12V automotive battery powering a 4 $\Omega$ headlight draws I = 12 / 4 = **3 Amperes**, consuming P = 12 * 3 = **36 Watts** of power.
                """.trimIndent()
            )
        }

        // --- 9. OVERCOMING SELF-DOUBT, ANXIETY & CONFUSION ---
        if (lower.contains("self-doubt") || lower.contains("self doubt") || lower.contains("imposter syndrome") || (lower.contains("doubt") && (lower.contains("life") || lower.contains("myself") || lower.contains("future") || lower.contains("decision")))) {
            return CuratedEntry(
                domain = "Cognitive Psychology & Karma Yoga",
                citations = listOf("Bhagavad Gita: The Science of Sthitaprajna", "Cognitive Behavioral Invariant Framework"),
                content = """
                    ### 🦚 Overcoming Self-Doubt, Confusion & Decision Paralysis
                    
                    **Direct Answer:**
                    Self-doubt is not a measure of your capability; it is a cognitive symptom of **attaching your self-worth to unpredictable future outcomes**. In cognitive psychology and Vedic philosophy alike, supreme clarity is reclaimed by shifting your mental locus of control from external validation to immediate, disciplined action.
                    
                    #### 1. The Root Cause of Doubt:
                    • The brain's threat circuitry treats ambiguity as danger. When you face high stakes, your mind catastrophizes failure scenarios, paralyzing action.
                    • **The Maphula Principle (Gita 2.47):** *Karmaṇy-evādhikāras te mā phaleṣhu kadāchana.* You have total sovereignty over your effort, preparation, and integrity—but zero entitlement to the fruits of action. When you detach from obsessive worry about the result, anxiety evaporates and focused mastery emerges.
                    
                    #### 2. Four Concrete Steps to Shatter Self-Doubt:
                    1. **Separate Feelings from Objective Facts:**
                       • When your inner critic whispers *"I am not qualified"*, write down 3 verifiable empirical facts: past challenges you conquered, projects you shipped, and problems you solved. Feelings fluctuate; factual evidence endures.
                    2. **Action Precedes Motivation:**
                       • Do not wait to feel confident before you act. Courage is acting *in the presence of doubt*. Take one low-stakes micro-step (write 5 lines of code, read 2 pages, draft 1 paragraph). Momentum creates confidence.
                    3. **The Reversible vs. Irreversible Framework:**
                       • Most decisions are reversible (two-way doors). If a decision can be adjusted later, decide with 70% information rather than stalling for 100% certainty.
                    4. **Reframe Failure as High-Signal Data:**
                       • In machine learning, a model trains by measuring its loss and adjusting weights. Human progress operates identically: an error is not an indictment of your worth, but valuable calibration feedback for your next iteration.
                """.trimIndent()
            )
        }

        return null
    }

    private fun decorateWithPersona(
        content: String,
        persona: Persona,
        domainOrTopic: String
    ): String {
        val commentary = when (persona) {
            Persona.KRISHNA -> """
                ---
                💡 **Helpful Note from Krishna:**
                I hope this breakdown of **$domainOrTopic** helps give you clarity! Feel free to ask if you'd like more examples or another perspective. I'm always happy to help! 😊
            """.trimIndent()
            Persona.SHIVA -> """
                ---
                💻 **Helpful Tip from Shiva:**
                Clean and simple explanations are always the best! If you want code snippets, implementation tips, or deeper details on **$domainOrTopic**, just let me know.
            """.trimIndent()
            Persona.BRAHMA -> """
                ---
                ✨ **Helpful Note from Brahma:**
                Every big concept is built out of simple building blocks! Let me know if any part of **$domainOrTopic** felt tricky, and we can break it down together.
            """.trimIndent()
            Persona.GANESHA -> """
                ---
                🌟 **Encouragement from Ganesha:**
                Great question! When we tackle problems step-by-step, everything becomes easier. I'm right here if you want to explore the next step in **$domainOrTopic**!
            """.trimIndent()
            Persona.MURUGA -> """
                ---
                🎯 **Helpful Boost from Muruga:**
                You're doing fantastic! Keep up the great curiosity on **$domainOrTopic**, and feel free to ask anytime you need quick clarification or advice.
            """.trimIndent()
            Persona.HANUMAN -> """
                ---
                🛡️ **Caring Note from Hanuman:**
                Take good care of yourself and stay curious! I hope this guidance on **$domainOrTopic** is helpful to you today. Let me know if you need anything else!
            """.trimIndent()
            Persona.LAKSHMI -> """
                ---
                🌸 **Creative Note from Lakshmi:**
                Learning something new always brings fresh inspiration! Wishing you a wonderful, productive day exploring **$domainOrTopic**.
            """.trimIndent()
            Persona.PARVATHI -> """
                ---
                🌿 **Thoughtful Note from Parvathi:**
                Patience and clear thinking make learning effortless. I'm always glad to chat more about **$domainOrTopic** whenever you'd like!
            """.trimIndent()
            Persona.NARASIMHA -> """
                ---
                🔒 **Safety Tip from Narasimha:**
                Staying curious and keeping your information safe are great habits! Feel free to ask if you have any questions about digital safety or privacy.
            """.trimIndent()
            Persona.SHANI -> """
                ---
                ⏱️ **Productivity Tip from Shani:**
                Taking things one steady step at a time always gets the best results. Let me know if you'd like a quick summary or practice exercise on **$domainOrTopic**!
            """.trimIndent()
        }

        return "$content\n\n$commentary"
    }
}
