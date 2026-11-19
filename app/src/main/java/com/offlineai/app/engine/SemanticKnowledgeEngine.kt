package com.offlineai.app.engine

import android.content.Context
import com.offlineai.app.data.KnowledgeArticle
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.ln
import kotlin.math.sqrt

class SemanticKnowledgeEngine {

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

    private val articles = mutableListOf<KnowledgeArticle>()
    private val docVectors = mutableListOf<Map<String, Double>>()
    private val idfMap = mutableMapOf<String, Double>()
    private var persistenceFile: File? = null

    init {
        loadKnowledgeBase()
        computeIdfAndDocVectors()
    }

    fun initPersistence(context: Context) {
        persistenceFile = File(context.filesDir, "synced_articles.json")
        loadPersistedArticles()
        computeIdfAndDocVectors()
    }

    private fun loadPersistedArticles() {
        val file = persistenceFile ?: return
        if (!file.exists()) return
        try {
            val json = file.readText()
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val article = articleFromJson(obj)
                val idx = articles.indexOfFirst { it.id == article.id }
                if (idx >= 0) {
                    articles[idx] = article
                } else {
                    articles.add(0, article)
                }
            }
        } catch (_: Exception) {}
    }

    @Synchronized
    fun addSyncedArticle(article: KnowledgeArticle) {
        val idx = articles.indexOfFirst { it.id == article.id || it.title.equals(article.title, ignoreCase = true) }
        if (idx >= 0) {
            articles[idx] = article
        } else {
            articles.add(0, article)
        }
        computeIdfAndDocVectors()
        savePersistedArticles()
    }

    private fun savePersistedArticles() {
        val file = persistenceFile ?: return
        try {
            val synced = articles.filter { it.isOnlineSynced }
            val array = JSONArray()
            for (art in synced) {
                array.put(articleToJson(art))
            }
            file.writeText(array.toString())
        } catch (_: Exception) {}
    }

    private fun articleToJson(art: KnowledgeArticle): JSONObject {
        val obj = JSONObject()
        obj.put("id", art.id)
        obj.put("title", art.title)
        obj.put("category", art.category)
        obj.put("tags", JSONArray(art.tags))
        obj.put("summary", art.summary)
        obj.put("content", art.content)
        obj.put("keyTakeaways", JSONArray(art.keyTakeaways))
        obj.put("isOnlineSynced", art.isOnlineSynced)
        obj.put("sourceUrl", art.sourceUrl ?: "")
        obj.put("syncedTimestamp", art.syncedTimestamp)
        return obj
    }

    private fun articleFromJson(obj: JSONObject): KnowledgeArticle {
        val tagsArr = obj.optJSONArray("tags") ?: JSONArray()
        val tags = (0 until tagsArr.length()).map { tagsArr.getString(it) }
        val takeArr = obj.optJSONArray("keyTakeaways") ?: JSONArray()
        val takeaways = (0 until takeArr.length()).map { takeArr.getString(it) }
        val sourceUrl = obj.optString("sourceUrl").takeIf { !it.isNullOrBlank() }
        return KnowledgeArticle(
            id = obj.getString("id"),
            title = obj.getString("title"),
            category = obj.getString("category"),
            tags = tags,
            summary = obj.getString("summary"),
            content = obj.getString("content"),
            keyTakeaways = takeaways,
            isOnlineSynced = obj.optBoolean("isOnlineSynced", false),
            sourceUrl = sourceUrl,
            syncedTimestamp = obj.optLong("syncedTimestamp", 0L)
        )
    }

    fun getSyncedArticles(): List<KnowledgeArticle> = articles.filter { it.isOnlineSynced }

    fun getSyncedCount(): Int = articles.count { it.isOnlineSynced }

    fun clearSyncedArticles() {
        articles.removeAll { it.isOnlineSynced }
        savePersistedArticles()
        computeIdfAndDocVectors()
    }

    data class ConceptCluster(
        val category: String,
        val centralTopic: String,
        val nodes: List<String>,
        val relatedCategories: List<String>,
        val articleCount: Int
    )

    /**
     * Ingests a personal note/document into the local vector knowledge base.
     * Generates semantic tags and key takeaways, updates TF-IDF vectors,
     * and persists the document offline.
     */
    @Synchronized
    fun ingestPersonalDocument(
        title: String,
        content: String,
        category: String = "Personal Notes",
        tags: List<String> = emptyList(),
        customSummary: String? = null,
        customTakeaways: List<String> = emptyList()
    ): KnowledgeArticle {
        val cleanTitle = title.trim().ifEmpty { "Personal Note #${System.currentTimeMillis() % 10000}" }
        val cleanContent = content.trim().ifEmpty { "No content provided." }
        val cleanCategory = category.trim().ifEmpty { "Personal Notes" }

        // Derive summary if not provided
        val summary = customSummary?.trim()?.takeIf { it.isNotEmpty() } ?: run {
            val firstSentence = cleanContent.split(Regex("[.!?]")).firstOrNull { it.isNotBlank() }?.trim()
            if (!firstSentence.isNullOrBlank()) {
                if (firstSentence.length > 180) firstSentence.take(177) + "..." else firstSentence
            } else {
                if (cleanContent.length > 180) cleanContent.take(177) + "..." else cleanContent
            }
        }

        // Derive tags from content and input
        val derivedTags = tags.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toMutableList()
        if (derivedTags.isEmpty()) {
            val contentTokens = tokenize(cleanContent).filter { it.length > 3 && !stopWords.contains(it) }
            val topTokens = contentTokens.groupingBy { it }.eachCount().entries
                .sortedByDescending { it.value }.take(4).map { it.key }
            derivedTags.addAll(topTokens)
        }

        // Derive takeaways if not provided
        val takeaways = if (customTakeaways.isNotEmpty()) {
            customTakeaways
        } else {
            val sentences = cleanContent.split(Regex("\n+|(?<=[.!?])\\s+"))
                .map { it.trim() }
                .filter { it.length in 15..200 }
            if (sentences.isNotEmpty()) {
                sentences.take(3)
            } else {
                listOf("Core concept: $cleanTitle", "Saved locally in sovereign personal knowledge base.")
            }
        }

        val articleId = "personal_${cleanTitle.lowercase().replace(Regex("[^a-z0-9]"), "_").take(30)}_${System.currentTimeMillis() % 100000}"

        val newArticle = KnowledgeArticle(
            id = articleId,
            title = cleanTitle,
            category = cleanCategory,
            tags = derivedTags.distinct(),
            summary = summary,
            content = cleanContent,
            keyTakeaways = takeaways,
            isOnlineSynced = true,
            sourceUrl = "local://personal_notes",
            syncedTimestamp = System.currentTimeMillis()
        )

        addSyncedArticle(newArticle)
        return newArticle
    }

    /**
     * Generates a concept graph linking categories, their prominent topics/nodes,
     * and relationships between domains for mind mapping.
     */
    fun generateConceptGraph(): List<ConceptCluster> {
        val grouped = articles.groupBy { it.category }
        val clusters = mutableListOf<ConceptCluster>()

        for ((category, catArticles) in grouped) {
            // Find most frequent tags and meaningful keywords in this category
            val tagCounts = catArticles.flatMap { it.tags }.groupingBy { it.lowercase() }.eachCount()
            val topTags = tagCounts.entries.sortedByDescending { it.value }.take(5).map { it.key }

            val contentWords = catArticles.flatMap { tokenize(it.title + " " + it.summary) }
                .filter { it.length > 3 && !stopWords.contains(it) }
                .groupingBy { it }
                .eachCount()
            val topWords = contentWords.entries.sortedByDescending { it.value }.take(6).map { it.key }

            val combinedNodes = (topTags + topWords).distinct().take(6)
            val centralTopic = catArticles.maxByOrNull { it.content.length }?.title ?: category

            // Find related categories based on overlapping tags
            val related = grouped.keys.filter { otherCat ->
                if (otherCat == category) return@filter false
                val otherArticles = grouped[otherCat] ?: return@filter false
                val otherTags = otherArticles.flatMap { it.tags }.map { it.lowercase() }.toSet()
                topTags.any { it in otherTags }
            }.take(3)

            clusters.add(
                ConceptCluster(
                    category = category,
                    centralTopic = centralTopic,
                    nodes = combinedNodes,
                    relatedCategories = related,
                    articleCount = catArticles.size
                )
            )
        }

        return clusters
    }

    fun getAllArticles(): List<KnowledgeArticle> = articles

    fun getArticleById(id: String): KnowledgeArticle? = articles.find { it.id == id }

    fun search(query: String, topK: Int = 5, categoryFilter: String? = null): List<Pair<KnowledgeArticle, Double>> {
        val queryTokens = tokenize(query)
        if (queryTokens.isEmpty()) {
            val list = if (categoryFilter != null && categoryFilter != "All") {
                articles.filter { it.category.equals(categoryFilter, ignoreCase = true) }
            } else articles
            return list.take(topK).map { Pair(it, 1.0) }
        }

        val queryVec = computeQueryVector(queryTokens)
        val queryNorm = computeNorm(queryVec)
        if (queryNorm == 0.0) return emptyList()

        val results = mutableListOf<Pair<KnowledgeArticle, Double>>()

        for (i in articles.indices) {
            val article = articles[i]
            if (categoryFilter != null && categoryFilter != "All" && !article.category.equals(categoryFilter, ignoreCase = true)) {
                continue
            }
            val docVec = docVectors[i]
            val docNorm = computeNorm(docVec)
            if (docNorm == 0.0) continue

            // Cosine similarity
            var dot = 0.0
            for ((term, qWeight) in queryVec) {
                val dWeight = docVec[term] ?: 0.0
                dot += qWeight * dWeight
            }

            // Keyword boost for exact title and tag matches
            var titleBoost = 0.0
            val lowerQuery = query.lowercase()
            if (article.title.lowercase().contains(lowerQuery)) titleBoost += 0.4
            for (tag in article.tags) {
                if (lowerQuery.contains(tag.lowercase())) titleBoost += 0.25
            }

            val similarity = (dot / (queryNorm * docNorm)) + titleBoost
            if (similarity > 0.05) {
                results.add(Pair(article, similarity))
            }
        }

        results.sortByDescending { it.second }
        return results.take(topK)
    }

    private fun tokenize(text: String): List<String> {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 1 && it !in stopWords }
    }

    private fun computeIdfAndDocVectors() {
        val nDocs = articles.size.toDouble()
        val docFreq = mutableMapOf<String, Int>()

        val tokenizedDocs = articles.map { article ->
            val fullText = "${article.title} ${article.tags.joinToString(" ")} ${article.summary} ${article.content}"
            tokenize(fullText)
        }

        for (tokens in tokenizedDocs) {
            val unique = tokens.toSet()
            for (term in unique) {
                docFreq[term] = (docFreq[term] ?: 0) + 1
            }
        }

        for ((term, count) in docFreq) {
            idfMap[term] = ln((nDocs + 1.0) / (count + 1.0)) + 1.0
        }

        docVectors.clear()
        for (tokens in tokenizedDocs) {
            val tf = mutableMapOf<String, Int>()
            for (t in tokens) tf[t] = (tf[t] ?: 0) + 1
            val vec = mutableMapOf<String, Double>()
            for ((t, count) in tf) {
                val idf = idfMap[t] ?: 1.0
                vec[t] = count.toDouble() * idf
            }
            docVectors.add(vec)
        }
    }

    private fun computeQueryVector(tokens: List<String>): Map<String, Double> {
        val tf = mutableMapOf<String, Int>()
        for (t in tokens) tf[t] = (tf[t] ?: 0) + 1
        val vec = mutableMapOf<String, Double>()
        for ((t, count) in tf) {
            val idf = idfMap[t] ?: 1.0
            vec[t] = count.toDouble() * idf
        }
        return vec
    }

    private fun computeNorm(vec: Map<String, Double>): Double {
        var sumSq = 0.0
        for (v in vec.values) sumSq += v * v
        return sqrt(sumSq)
    }

    private fun loadKnowledgeBase() {
        articles.add(
            KnowledgeArticle(
                id = "aid_cpr",
                title = "CPR & Choking Emergency Protocol",
                category = "Emergency & Survival",
                tags = listOf("cpr", "choking", "first aid", "emergency", "cardiac arrest"),
                summary = "Critical step-by-step protocol for performing chest compressions and Heimlich maneuver.",
                content = """
                    CARDIOPULMONARY RESUSCITATION (CPR):
                    1. Check Responsiveness: Tap shoulders firmly and shout 'Are you OK?'. Check for normal breathing for 5-10 seconds.
                    2. Hand Placement: Place the heel of one hand in the center of the chest (lower half of sternum), interlock the other hand on top.
                    3. Compression Depth & Rate: Push hard and fast: 2 to 2.4 inches (5-6 cm) deep at a rate of 100-120 beats per minute (e.g., to the rhythm of 'Stayin' Alive').
                    4. Full Recoil: Allow chest to completely rise between compressions. Do not lean on chest.
                    5. Rescue Breaths (if trained): 30 compressions followed by 2 gentle breaths (1 second each) watching for chest rise. If untrained, provide continuous Hands-Only CPR.
                    
                    CHOKING (HEIMLICH MANEUVER - ADULTS):
                    1. Stand behind victim and wrap arms around their waist.
                    2. Make a fist with one hand and place the thumb side just above the navel, below the ribcage.
                    3. Grasp your fist with your other hand.
                    4. Deliver quick, upward and inward thrusts until the airway blockage dislodges.
                """.trimIndent(),
                keyTakeaways = listOf("100-120 beats per minute rate", "2-2.4 inches compression depth", "Continuous compressions if untrained")
            )
        )

        articles.add(
            KnowledgeArticle(
                id = "aid_bleeding",
                title = "Severe Bleeding & Tourniquet Use",
                category = "Emergency & Survival",
                tags = listOf("bleeding", "tourniquet", "hemorrhage", "wound", "trauma"),
                summary = "Techniques for controlling life-threatening arterial bleeding using direct pressure, wound packing, and commercial or improvised tourniquets.",
                content = """
                    STEPS TO CONTROL SEVERE BLEEDING:
                    1. Direct Firm Pressure: Place a sterile dressing or clean cloth over the wound and apply firm, direct, continuous pressure with both hands.
                    2. Wound Packing: If bleeding is in the groin, shoulder, or neck (junctional zones where tourniquets cannot go), pack gauze or cloth tightly deep into the wound cavity directly against the bleeding vessel, then maintain hard pressure for at least 3 minutes.
                    3. Tourniquet Application (Extremity Bleeding):
                       - Apply 2 to 3 inches above the wound on the injured limb (never over a joint).
                       - Pull the strap as tight as possible before turning the windlass.
                       - Twist the windlass rod until bright red bleeding stops and distal pulse vanishes.
                       - Lock the windlass in the clip and note the exact time of application on the patient's forehead or tourniquet strap (T = HH:MM).
                       - Never loosen or remove a tourniquet once applied; only qualified surgical personnel should remove it.
                """.trimIndent(),
                keyTakeaways = listOf("Direct continuous pressure first", "Apply tourniquet 2-3 inches above wound", "Record exact application time")
            )
        )

        articles.add(
            KnowledgeArticle(
                id = "aid_burns",
                title = "Thermal Burn Classification & Care",
                category = "Emergency & Survival",
                tags = listOf("burns", "thermal", "scalds", "first aid", "skin"),
                summary = "Differentiating 1st, 2nd, and 3rd degree burns and emergency cooling protocols.",
                content = """
                    BURN CLASSIFICATION:
                    - 1st Degree (Superficial): Redness, mild pain, epidermis only. No blisters. (e.g., sunburn).
                    - 2nd Degree (Partial Thickness): Severe pain, deep redness, blistering, raw weeping skin.
                    - 3rd Degree (Full Thickness): Leathery, charred black or waxy white skin. Numbness due to destroyed nerve endings.
                    
                    EMERGENCY CARE PROTOCOL:
                    1. Cool the Burn: Run clean, cool water over the burn for 10-20 minutes.
                       WARNING: NEVER use ice, ice water, butter, oil, or toothpastes. Ice damages tissue further.
                    2. Remove Constrictive Items: Remove rings, watches, and tight clothing before swelling occurs.
                    3. Protect Blisters: Do NOT pop or drain blisters—they form a sterile barrier against infection.
                    4. Cover Loosely: Apply sterile non-stick dry dressing or clean plastic wrap loosely over the area.
                    5. Prevent Hypothermia: Keep the patient warm with blankets, especially with extensive second or third-degree burns.
                """.trimIndent(),
                keyTakeaways = listOf("Cool with clean running water 10-20 mins", "Never apply ice or butter", "Never pop blisters")
            )
        )

        articles.add(
            KnowledgeArticle(
                id = "cs_algorithms",
                title = "Sorting Algorithms & Big-O Complexity",
                category = "Computer Science",
                tags = listOf("algorithms", "sorting", "complexity", "big o", "quicksort", "mergesort"),
                summary = "Mathematical analysis and operational mechanics of fundamental sorting algorithms.",
                content = """
                    COMMON ALGORITHM COMPLEXITIES:
                    - O(1): Constant time (Hash map lookup)
                    - O(log N): Logarithmic time (Binary search)
                    - O(N): Linear time (Single loop search)
                    - O(N log N): Linearithmic time (Efficient comparisons: Mergesort, Heapsort, Quicksort average)
                    - O(N^2): Quadratic time (Bubble sort, Insertion sort, Selection sort)
                    
                    SORTING PARADIGMS:
                    1. Quicksort (Divide & Conquer):
                       - Select a pivot element. Partition array such that elements < pivot are left, > pivot are right.
                       - Recurse on sub-arrays. Average O(N log N), Worst O(N^2) if pivot is poorly chosen. In-place O(log N) space.
                    2. Mergesort:
                       - Recursively divide array into halves until size 1.
                       - Merge halves in sorted order. Guaranteed O(N log N) worst-case time, O(N) auxiliary space.
                    3. Binary Search:
                       - Requires sorted data. Inspect midpoint; eliminate half the search space each step. O(log N) comparisons.
                """.trimIndent(),
                keyTakeaways = listOf("Binary Search is O(log N)", "Mergesort guarantees O(N log N)", "Quicksort is in-place")
            )
        )

        articles.add(
            KnowledgeArticle(
                id = "cs_neural_nets",
                title = "Neural Networks & Backpropagation Explained",
                category = "Computer Science",
                tags = listOf("neural networks", "machine learning", "ai", "deep learning", "backpropagation"),
                summary = "Mathematical foundation of artificial neurons, activation functions, loss gradients, and stochastic gradient descent.",
                content = """
                    ARTIFICIAL NEURON ARCHITECTURE:
                    A neuron receives inputs x_i, multiplies them by learnable weights w_i, adds a bias b, and applies an activation function:
                    z = Σ (w_i * x_i) + b
                    a = σ(z)
                    
                    COMMON ACTIVATION FUNCTIONS:
                    - Sigmoid: σ(z) = 1 / (1 + e^(-z)). Maps values to (0, 1). Derivative: σ'(z) = σ(z) * (1 - σ(z)).
                    - ReLU: f(z) = max(0, z). Fast, prevents vanishing gradients in deep networks.
                    - Tanh: f(z) = (e^z - e^(-z)) / (e^z + e^(-z)). Zero-centered output between (-1, 1).
                    
                    BACKPROPAGATION & GRADIENT DESCENT:
                    1. Forward Pass: Compute layer activations sequentially from input to output.
                    2. Loss Calculation: Measure error between predicted output and ground truth (e.g., Mean Squared Error or Cross-Entropy).
                    3. Backward Pass: Use the chain rule of calculus to calculate partial derivatives ∂Loss/∂w for each weight.
                    4. Weight Update: Adjust weights opposite to gradient: w_new = w_old - (learning_rate * ∂Loss/∂w).
                """.trimIndent(),
                keyTakeaways = listOf("Forward pass produces predictions", "Chain rule propagates error backwards", "Weights update via gradient descent")
            )
        )

        articles.add(
            KnowledgeArticle(
                id = "sci_thermo",
                title = "Laws of Thermodynamics & Entropy",
                category = "Science & Physics",
                tags = listOf("thermodynamics", "physics", "entropy", "energy", "conservation"),
                summary = "The four fundamental laws governing energy, heat transfer, temperature, and universal entropy.",
                content = """
                    FOUR LAWS OF THERMODYNAMICS:
                    - Zeroth Law: If bodies A and B are in thermal equilibrium with C, then A and B are in equilibrium with each other. Establishes temperature measurement.
                    - First Law (Conservation of Energy): Energy cannot be created or destroyed, only transformed: ΔU = Q - W (Change in internal energy equals heat added minus work done).
                    - Second Law (Entropy): In any spontaneous cyclic process, the total entropy of an isolated system always increases over time: ΔS_total ≥ 0. Heat naturally flows only from hotter to colder bodies.
                    - Third Law (Absolute Zero): As temperature approaches absolute zero (0 Kelvin / -273.15°C), the entropy of a pure crystalline substance approaches a minimum constant value (zero).
                """.trimIndent(),
                keyTakeaways = listOf("Energy is conserved (1st Law)", "Entropy of isolated systems increases (2nd Law)", "Absolute zero is unreachable (3rd Law)")
            )
        )

        articles.add(
            KnowledgeArticle(
                id = "phil_fallacies",
                title = "Logical Fallacies & Critical Thinking",
                category = "Philosophy & Logic",
                tags = listOf("logic", "fallacies", "critical thinking", "reasoning", "argument"),
                summary = "Guide to identifying common errors in deductive and inductive reasoning.",
                content = """
                    COMMON LOGICAL FALLACIES:
                    1. Ad Hominem: Attacking the person making the argument rather than addressing their actual premise or evidence.
                    2. Straw Man: Distorting, exaggerating, or misrepresenting an opponent's argument to make it easier to knock down.
                    3. False Dilemma (Bifurcation): Presenting two alternative states as the only possibilities, when in fact more options exist.
                    4. Post Hoc Ergo Propter Hoc: Assuming that because event B occurred after event A, event A must have caused event B (correlation vs causation).
                    5. Appeal to Authority (Argumentum Ad Verecundiam): Insisting a claim is true solely because an authority figure asserted it, without supporting evidence.
                    6. Begging the Question (Circular Reasoning): An argument whose premises assume the truth of the conclusion they are supposed to prove.
                """.trimIndent(),
                keyTakeaways = listOf("Distinguish argument from speaker", "Correlation does not equal causation", "Avoid false either-or dichotomies")
            )
        )

        articles.add(
            KnowledgeArticle(
                id = "sci_quantum",
                title = "Quantum Mechanics: Superposition & Entanglement",
                category = "Science & Physics",
                tags = listOf("quantum", "physics", "superposition", "entanglement", "qubits"),
                summary = "Foundational concepts of wave-particle duality, state vectors, and non-local correlations.",
                content = """
                    KEY QUANTUM CONCEPTS:
                    1. Wave-Particle Duality: Matter and light exhibit properties of both waves and particles (demonstrated in Young's double-slit experiment).
                    2. Heisenberg Uncertainty Principle: One cannot simultaneously measure both position (x) and momentum (p) of a particle with arbitrary precision: Δx * Δp ≥ ħ / 2.
                    3. Quantum Superposition: A quantum system can exist in a linear combination of multiple states simultaneously until a measurement collapses the state vector into an eigenstate (basis state).
                    4. Quantum Entanglement: Two or more particles become correlated such that the quantum state of one particle cannot be described independently of the state of the other, regardless of distance.
                    5. Qubits: The quantum computing equivalent of classical bits, existing as |ψ⟩ = α|0⟩ + β|1⟩ where |α|^2 + |β|^2 = 1.
                """.trimIndent(),
                keyTakeaways = listOf("Particles have wave functions", "Measurement collapses superposition", "Entangled states share correlation across space")
            )
        )

        // Expanded Wilderness Medicine & Emergency Survival
        articles.add(
            KnowledgeArticle(
                id = "aid_hypothermia_heat",
                title = "Hypothermia & Heatstroke Emergency Protocols",
                category = "Emergency & Survival",
                tags = listOf("hypothermia", "heatstroke", "survival", "first aid", "temperature", "hanuman"),
                summary = "Critical environmental emergency intervention for severe core body temperature dysregulation.",
                content = """
                    HYPOTHERMIA MANAGEMENT:
                    - Signs: Mild (violent shivering, slurred speech); Moderate (loss of shivering, confusion); Severe (unconsciousness, bradycardia).
                    - Immediate Actions:
                      1. Remove wet clothing immediately; insulate body from frozen ground with sleeping pad or branches.
                      2. Wrap in vapor barrier / dry blankets covering head and neck.
                      3. Apply gentle warm packs to core areas only (groin, axillae/armpits, chest). NEVER rub frostbitten limbs or apply direct hot water to extremities (causes rewarming shock / core temperature drop).
                      4. Provide warm sweet fluids only if patient is alert and capable of swallowing.
                    
                    HEATSTROKE (HYPERTHERMIA) CRITICAL TRIAGE:
                    - Signs: Core body temp > 40°C (104°F), altered mental status, cessation of sweating, hot dry flushed skin.
                    - Immediate Actions:
                      1. Immediate aggressive cooling is lifesaving: immerse body up to neck in cold water if available.
                      2. If immersion unavailable: douse body with water and fan vigorously to maximize evaporative cooling.
                      3. Place ice/cold packs in armpits, neck, and groin.
                """.trimIndent(),
                keyTakeaways = listOf("Rewarm hypothermia from the core", "Never rub frostbite", "Rapid active cooling for heatstroke")
            )
        )

        // Expanded Zero-Trust & Cryptographic Architecture
        articles.add(
            KnowledgeArticle(
                id = "cs_zero_trust",
                title = "Zero Trust Security Architecture & Cryptographic Invariants",
                category = "Computer Science",
                tags = listOf("security", "zero trust", "cryptography", "sha256", "narasimha", "threats"),
                summary = "Principles of zero trust cybersecurity: continuous verification, least privilege, and cryptographic immutability.",
                content = """
                    CORE ZERO TRUST PRINCIPLES:
                    1. Never Trust, Always Verify: Do not assume internal network perimeter safety; verify every transaction, identity, and process explicitly.
                    2. Least Privilege Access: Grant access strictly scoped to the exact permissions and time window required for the operation.
                    3. Assume Breach: Design architectures with blast-radius containment, defense-in-depth, end-to-end encryption, and continuous telemetry auditing.
                    
                    CRYPTOGRAPHIC FOUNDATIONS:
                    - Cryptographic Hash Functions (SHA-256): Deterministic, one-way pre-image resistance, collision resistance, and avalanche effect (changing 1 bit flips ~50% of hash).
                    - Asymmetric Cryptography: Public/private key pairs (RSA, ECC, Ed25519) enabling digital signatures and key exchanges without shared secrets.
                    - Symmetric Ciphers (AES-256-GCM): Authenticated encryption providing both confidentiality and tamper-proof message integrity.
                """.trimIndent(),
                keyTakeaways = listOf("Never trust, always verify explicitly", "Enforce least privilege access", "Authenticated encryption prevents tampering")
            )
        )

        // Ancient Indian Computational Science & Binary Combinatorics
        articles.add(
            KnowledgeArticle(
                id = "hist_pingala_binary",
                title = "Pingala Chandaḥśāstra: Ancient Origins of Binary & Combinatorics",
                category = "Computer Science",
                tags = listOf("binary", "pingala", "vedic science", "combinatorics", "algorithms", "brahma"),
                summary = "Historical analysis of Acharya Pingala's binary numeral representation, Meru Prastara (Pascal's Triangle), and combinatorial algorithms.",
                content = """
                    PINGALA'S CHANDAḤŚĀSTRA (c. 3rd-2nd Century BCE):
                    - Guru & Laghu (Binary 1 and 0): Sanskrit poetic meters are composed of heavy/long (Guru = 1) and light/short (Laghu = 0) syllables.
                    - Prastāra (Exhaustive Permutations): An algorithmic method to systematically generate all 2^N binary arrangements of an N-syllable meter.
                    - Naṣṭam & Uddiṣṭam (Binary-to-Decimal and Decimal-to-Binary Conversion): Pingala established explicit conversion algorithms between binary syllabic sequences and decimal integers.
                    - Meru Prastāra (The Mount Meru Staircase): The triangle of binomial coefficients, documented centuries before Blaise Pascal, used to calculate combinations nCr.
                """.trimIndent(),
                keyTakeaways = listOf("First recorded binary system (Guru/Laghu)", "Algorithmic conversion between binary and decimal", "Meru Prastara anticipated Pascal's Triangle")
            )
        )

        articles.add(
            KnowledgeArticle(
                id = "sci_photosynthesis",
                title = "Photosynthesis & Solar Energy Conversion",
                category = "Science & Physics",
                tags = listOf("photosynthesis", "plants", "chlorophyll", "calvin cycle", "biology", "light", "oxygen"),
                summary = "Biochemical conversion of solar radiation, water, and carbon dioxide into glucose and atmospheric oxygen.",
                content = """
                    PHOTOSYNTHESIS BIOCHEMISTRY:
                    Equation: 6CO2 + 6H2O + Light -> C6H12O6 + 6O2
                    1. Light Reactions (Thylakoid Membranes): Chlorophyll absorbs photons, water is split (photolysis) producing O2, generating ATP and NADPH.
                    2. Calvin Cycle (Stroma): RuBisCO enzyme fixes carbon dioxide into 3-carbon sugars to synthesize glucose.
                    Plants perform cellular respiration 24/7 consuming O2; during daylight photosynthetic O2 output vastly exceeds respiration.
                """.trimIndent(),
                keyTakeaways = listOf("6CO2 + 6H2O + Light -> C6H12O6 + 6O2", "Water splitting releases O2", "Calvin cycle synthesizes glucose")
            )
        )

        articles.add(
            KnowledgeArticle(
                id = "sci_sky_blue",
                title = "Atmospheric Optics: Why the Sky is Blue & Rayleigh Scattering",
                category = "Science & Physics",
                tags = listOf("sky", "blue", "scattering", "rayleigh", "atmosphere", "optics", "light", "sunset"),
                summary = "Physical mechanics of electromagnetic wave scattering by atmospheric gas molecules.",
                content = """
                    RAYLEIGH SCATTERING LAW:
                    Scattering intensity is inversely proportional to the 4th power of wavelength: I ~ 1 / lambda^4.
                    Atmospheric N2 and O2 molecules are smaller than visible wavelengths. Shorter wavelengths (blue ~450nm) scatter ~10x more than longer red wavelengths (~700nm).
                    The sky is blue rather than violet because the Sun emits more blue photons and human cone receptors are far more sensitive to blue.
                    Sunsets are red because light traverses a longer atmospheric path, scattering away blue light and leaving longer red wavelengths.
                """.trimIndent(),
                keyTakeaways = listOf("Rayleigh scattering I ~ 1/lambda^4", "Blue light scatters ~10x more than red", "Sunsets are red due to longer path length")
            )
        )

        articles.add(
            KnowledgeArticle(
                id = "sci_gravity_relativity",
                title = "Gravitation & General Relativity: Spacetime Curvature",
                category = "Science & Physics",
                tags = listOf("gravity", "gravitation", "relativity", "einstein", "newton", "spacetime", "physics"),
                summary = "Evolution of gravitational theory from Newton's inverse-square law to Einstein's curved spacetime geodesics.",
                content = """
                    NEWTONIAN GRAVITATION & EINSTEINIAN RELATIVITY:
                    - Newton: F = G * (m1 * m2) / r^2. Acceleration at Earth surface g ~ 9.8 m/s^2.
                    - Galileo Equivalence Principle: All masses fall with identical acceleration in a vacuum because gravitational force scales with inertial mass.
                    - General Relativity: Spacetime is a 4-dimensional manifold curved by mass-energy; free-falling bodies follow geodesics.
                    - Orbital Free-fall: Astronauts in orbit experience apparent weightlessness because they are in continuous free-fall around Earth, with ~90% gravity present.
                """.trimIndent(),
                keyTakeaways = listOf("Newton: F = G*m1*m2/r^2", "Vacuum acceleration is mass-independent", "Einstein: Mass curves 4D spacetime")
            )
        )

        articles.add(
            KnowledgeArticle(
                id = "cs_compiler_interpreter",
                title = "Compilers vs Interpreters: Execution Mechanics & JIT",
                category = "Computer Science",
                tags = listOf("compiler", "interpreter", "jit", "bytecode", "programming", "code", "runtime"),
                summary = "Comparative analysis of ahead-of-time compilation, runtime interpretation, and hybrid Just-In-Time execution engines.",
                content = """
                    LANGUAGE RUNTIME PARADIGMS:
                    1. Compilers (AOT): Translate entire source code into native machine code or bytecode ahead of time (C, C++, Rust, Go, Kotlin). Fast runtime execution, build-time type verification.
                    2. Interpreters: Translate and execute code line-by-line in real-time (Python, Ruby, PHP). Fast development cycle, higher runtime interpretation overhead.
                    3. JIT Hybrid: JVM and V8 compile source to bytecode, then Just-In-Time compile high-frequency hot code into optimized native instructions during execution.
                """.trimIndent(),
                keyTakeaways = listOf("Compilers translate AOT into machine code", "Interpreters execute line-by-line", "JIT compiles hot loops dynamically")
            )
        )

        articles.add(
            KnowledgeArticle(
                id = "cs_recursion",
                title = "Recursive Algorithms & Call Stack Dynamics",
                category = "Computer Science",
                tags = listOf("recursion", "recursive", "base case", "call stack", "algorithms", "stack overflow"),
                summary = "Structural mechanics of self-referential functions, stack frame allocation, and termination invariants.",
                content = """
                    RECURSION ARCHITECTURE:
                    Every correct recursive function requires:
                    1. Base Case: Explicit terminating condition returning without recursive calls.
                    2. Recursive Step: Dividing problem state and invoking self with arguments strictly advancing toward base case.
                    Call Stack: Each invocation pushes a new activation frame (parameters, local variables, return address).
                    Missing or unreachable base cases cause StackOverflowError when thread stack limits are exceeded.
                """.trimIndent(),
                keyTakeaways = listOf("Must have base case and recursive step", "Pushes activation frames onto call stack", "Unbounded recursion causes StackOverflowError")
            )
        )

        articles.add(
            KnowledgeArticle(
                id = "sci_cardiovascular",
                title = "Human Cardiovascular System: Heart & Circulation",
                category = "Emergency & Survival",
                tags = listOf("heart", "circulation", "blood", "cardiovascular", "chambers", "cardiac cycle"),
                summary = "Anatomical mechanics of 4-chambered heart pumping, pulmonary and systemic circulatory loops, and cardiac conduction.",
                content = """
                    HEART ANATOMY & CARDIAC CIRCULATION:
                    4 Chambers: Right Atrium, Right Ventricle, Left Atrium, Left Ventricle.
                    1. Pulmonary Loop: Deoxygenated blood returns from body via Vena Cava -> Right Atrium -> Tricuspid Valve -> Right Ventricle -> Pulmonary Artery -> Lungs for gas exchange.
                    2. Systemic Loop: Oxygen-rich blood from lungs -> Pulmonary Veins -> Left Atrium -> Mitral Valve -> Left Ventricle -> Aorta -> Body organs and brain.
                    Conduction: Sinoatrial (SA) node pacemaker coordinates synchronized Systole (contraction) and Diastole (filling).
                """.trimIndent(),
                keyTakeaways = listOf("4 chambers: 2 atria and 2 ventricles", "Pulmonary loop oxygenates blood", "Left ventricle pumps to systemic aorta")
            )
        )

        articles.add(
            KnowledgeArticle(
                id = "sci_electricity_ohm",
                title = "Electricity, Circuits & Ohm's Law (V = I * R)",
                category = "Science & Physics",
                tags = listOf("electricity", "voltage", "current", "resistance", "ohms law", "circuits", "power"),
                summary = "Physical definitions and circuit relationships between electric potential, current flow, and resistance.",
                content = """
                    OHM'S LAW & CIRCUIT MECHANICS:
                    - Voltage (V, Volts): Electric potential difference driving charge flow.
                    - Current (I, Amperes): Rate of electric charge flow (1 A = 1 C/s).
                    - Resistance (R, Ohms): Material opposition to electric current.
                    Formula: V = I * R, I = V / R, R = V / I.
                    Electric Power: P = V * I = I^2 * R = V^2 / R (Watts).
                """.trimIndent(),
                keyTakeaways = listOf("V = I * R", "Current is proportional to Voltage", "Power P = V * I (Watts)")
            )
        )

        articles.add(
            KnowledgeArticle(
                id = "phil_overcoming_doubts",
                title = "Overcoming Self-Doubt, Decision Paralysis & Karma Yoga",
                category = "Philosophy & Logic",
                tags = listOf("doubt", "doubts", "decision", "self doubt", "karma", "clarity", "gita", "krishna"),
                summary = "Vedic psychological framework for dissolving decision paralysis and anchoring mind in purposeful action.",
                content = """
                    DISSOLVING SELF-DOUBT & DECISION PARALYSIS:
                    1. The Maphula Principle (Gita 2.47): 'Karmany-evadhikaras te ma phaleshu kadachana'. Direct 100% of focus onto excellence of action, releasing anxious attachment to outcomes.
                    2. Facts over Feelings: Distinguish emotional inner critic from verifiable empirical capabilities.
                    3. Action Precedes Motivation: Momentum is generated by taking low-stakes micro-actions rather than waiting for passive confidence.
                    4. Reversible vs Irreversible: Make reversible decisions with 70% data; treat unexpected results as calibration feedback.
                """.trimIndent(),
                keyTakeaways = listOf("Focus on action, release outcome anxiety", "Take low-stakes micro-actions to build momentum", "Reversible decisions need 70% data")
            )
        )

        // Inject all sacred verses from ScriptureLibraryEngine into knowledge base
        val scriptureArticles = ScriptureLibraryEngine().toKnowledgeArticles()
        articles.addAll(scriptureArticles)
    }

    @Synchronized
    fun addCustomArticles(customList: List<KnowledgeArticle>) {
        for (art in customList) {
            val idx = articles.indexOfFirst { it.id == art.id }
            if (idx >= 0) {
                articles[idx] = art
            } else {
                articles.add(0, art)
            }
        }
        computeIdfAndDocVectors()
        savePersistedArticles()
    }
}
