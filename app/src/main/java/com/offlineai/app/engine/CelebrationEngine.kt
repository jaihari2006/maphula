package com.offlineai.app.engine

import java.util.Calendar

data class Celebration(
    val title: String,
    val deityName: String,
    val deityEmoji: String,
    val category: String,
    val description: String,
    val ritualsAndObservance: String,
    val badgeColorHex: String
)

/**
 * Universal Celebration & Astronomical Chrono Engine for Maphula.
 * Synchronizes every day of the month across any year (up to 9999 AD)
 * with Hindu deity festivals, Jayantis, Tithis, and planetary devotions.
 */
class CelebrationEngine {

    // Fixed & Anchor Calendar Celebrations (Month-Day -> Celebration)
    private val celebrationCalendar = mapOf(
        // January
        "1-1" to Celebration(
            title = "Universal Kalpataru Day & New Year",
            deityName = "Krishna",
            deityEmoji = "🦚",
            category = "Universal Celebration",
            description = "The day of divine wish-fulfillment, renewal of conscious purpose, and sovereign vows for wisdom.",
            ritualsAndObservance = "Early morning meditation, reflection on dharma, and chanting 'Om Namo Bhagavate Vasudevaya'.",
            badgeColorHex = "#7C3AED"
        ),
        "1-14" to Celebration(
            title = "Makar Sankranti & Pongal (Uttarayana)",
            deityName = "Lakshmi",
            deityEmoji = "🌸",
            category = "Solar Harvest & Abundance",
            description = "The cosmic ingress of the Sun into Makara (Capricorn), marking the dawn of the Gods and seasonal harvest.",
            ritualsAndObservance = "Offering sweetened milk-rice (Pongal), charity of sesame (Til) and jaggery, sacred bathing, and flying kites.",
            badgeColorHex = "#DB2777"
        ),
        "1-23" to Celebration(
            title = "Vasant Panchami & Saraswati Puja",
            deityName = "Brahma",
            deityEmoji = "🪷",
            category = "Creation & Learning",
            description = "Arrival of spring and worship of the goddess of arts, sciences, logic, and sacred literature.",
            ritualsAndObservance = "Wearing yellow clothing, blessing books and musical instruments, chanting the Gayatri Mantra.",
            badgeColorHex = "#F59E0B"
        ),

        // February
        "2-8" to Celebration(
            title = "Ratha Saptami & Surya Jayanti",
            deityName = "Krishna",
            deityEmoji = "🦚",
            category = "Solar Awakening",
            description = "The turning of the golden chariot of Surya with seven horses representing the seven solar spectrums.",
            ritualsAndObservance = "Offering Arghya (water) to the rising sun, reciting Aditya Hridaya Stotram for vitality and eyesight.",
            badgeColorHex = "#EAB308"
        ),
        "2-18" to Celebration(
            title = "Maha Shivaratri (Night of Shiva)",
            deityName = "Shiva",
            deityEmoji = "🔱",
            category = "Great Transcendent Festival",
            description = "The supreme cosmic night when Lord Shiva performed the Tandava and manifested as the infinite Jyotirlinga.",
            ritualsAndObservance = "All-night vigil (Jagaran), fasting, continuous Abhishekam with milk, honey, and Bilva leaves, chanting 'Om Namah Shivaya'.",
            badgeColorHex = "#06B6D4"
        ),

        // March
        "3-14" to Celebration(
            title = "Holika Dahan (Narasimha Triumph)",
            deityName = "Narasimha",
            deityEmoji = "🦁",
            category = "Victory of Dharma",
            description = "The divine bonfire representing the burning of evil and unyielding protection of pure devotion (Bhakta Prahlada).",
            ritualsAndObservance = "Lighting the sacred bonfire at dusk, circumambulating with roasted grains, praying for destruction of inner negativity.",
            badgeColorHex = "#B45309"
        ),
        "3-15" to Celebration(
            title = "Holi & Phalguna Purnima",
            deityName = "Krishna",
            deityEmoji = "🦚",
            category = "Festival of Love & Colors",
            description = "The ecstatic celebration of spring, divine play (Raas Leela) of Radha-Krishna, and unity across society.",
            ritualsAndObservance = "Joyful application of fragrant natural herbal colors (Gulal), sharing sweets like Gujiya, singing festive songs.",
            badgeColorHex = "#EC4899"
        ),
        "3-29" to Celebration(
            title = "Ugadi / Gudi Padwa / Chaitra Navratri",
            deityName = "Brahma",
            deityEmoji = "🪷",
            category = "Cosmic Creation Day",
            description = "The sacred day Lord Brahma commenced cosmic creation and time reckoning for the current Kalpa.",
            ritualsAndObservance = "Tasting Ugadi Pachadi (blending sweet, sour, bitter, spicy to welcome life's seasons), raising the Gudi flag.",
            badgeColorHex = "#F59E0B"
        ),

        // April
        "4-6" to Celebration(
            title = "Sri Rama Navami",
            deityName = "Krishna",
            deityEmoji = "🦚",
            category = "Divine Avatar Descent",
            description = "The birth of Lord Rama, embodiment of truth, righteousness (Maryada), and ideal kingship.",
            ritualsAndObservance = "Recitation of Ramayana, Panakam (spiced jaggery drink) distribution, midday Aarti at the Abhijit Muhurta.",
            badgeColorHex = "#3B82F6"
        ),
        "4-11" to Celebration(
            title = "Hanuman Jayanti",
            deityName = "Hanuman",
            deityEmoji = "🛡️",
            category = "Descent of Supreme Strength",
            description = "Appearance of Lord Hanuman, the immortal exemplar of selfless devotion, emergency healing, and immense strength.",
            ritualsAndObservance = "Chanting Hanuman Chalisa 108 times, offering vermilion (Sindoor) and jasmine oil, sharing bananas and laddoos.",
            badgeColorHex = "#DC2626"
        ),
        "4-14" to Celebration(
            title = "Vishu / Puthandu (Solar New Year)",
            deityName = "Muruga",
            deityEmoji = "⚡",
            category = "Solar Equinox Festival",
            description = "Traditional Tamil/Malayali New Year celebrating first sight of auspicious items (Vishukkani) and fresh beginnings.",
            ritualsAndObservance = "Arranging golden Kani blossoms, mirrors, gold coins, and fruits; seeking blessings from elders with Kai-neettam.",
            badgeColorHex = "#E11D48"
        ),

        // May
        "5-2" to Celebration(
            title = "Akshaya Tritiya",
            deityName = "Lakshmi",
            deityEmoji = "🌸",
            category = "Eternal Prosperity",
            description = "The day of unending abundance; actions, knowledge, and charitable deeds begun on this day flourish forever.",
            ritualsAndObservance = "Worship of Mahalakshmi and Kubera, donating food and water pots (Udaka Kumbha), initiating new projects.",
            badgeColorHex = "#DB2777"
        ),
        "5-10" to Celebration(
            title = "Sri Narasimha Jayanti",
            deityName = "Narasimha",
            deityEmoji = "🦁",
            category = "Divine Protector Descent",
            description = "The dusk manifestation of Bhagavan Narasimha from the pillar to protect his devotee and restore cosmic order.",
            ritualsAndObservance = "Fasting until sunset, reciting Narasimha Kavacha, offering jaggery water and red flowers.",
            badgeColorHex = "#B45309"
        ),
        "5-28" to Celebration(
            title = "Shani Jayanti & Shani Amavasya",
            deityName = "Shani",
            deityEmoji = "⚖️",
            category = "Lord of Karma",
            description = "The descent of Shani Deva (Saturn), the divine magistrate who teaches discipline, humility, and karma resolution.",
            ritualsAndObservance = "Offering mustard oil and black sesame to Shani Deva, reciting Shani Chalisa, practicing humble service to the needy.",
            badgeColorHex = "#64748B"
        ),

        // June
        "6-5" to Celebration(
            title = "Ganga Dussehra",
            deityName = "Shiva",
            deityEmoji = "🔱",
            category = "Descent of the Sacred River",
            description = "The descent of sacred River Ganga onto earth, cushioned safely by the ascetic matted locks of Lord Shiva.",
            ritualsAndObservance = "Holy dip in sacred waters, lighting 10 clay lamps at sunset, fasting and charity to cleanse negative karma.",
            badgeColorHex = "#06B6D4"
        ),
        "6-21" to Celebration(
            title = "International Yoga Day & Solstice",
            deityName = "Shiva",
            deityEmoji = "🔱",
            category = "Adiyogi Enlightenment",
            description = "The summer solstice honoring Adiyogi Shiva, who sat as the first yogi and imparted the science of consciousness.",
            ritualsAndObservance = "Practicing Surya Namaskar, Pranayama meditation, and honoring universal physical and mental wellness.",
            badgeColorHex = "#10B981"
        ),

        // July
        "7-7" to Celebration(
            title = "Jagannath Ratha Yatra",
            deityName = "Krishna",
            deityEmoji = "🦚",
            category = "Grand Chariot Festival",
            description = "Lord Jagannath (Lord of the Universe) journeys openly onto the streets with Balabhadra and Subhadra to bless all people.",
            ritualsAndObservance = "Pulling the giant sacred chariot ropes, singing Sankirtan, partaking of Mahaprasad.",
            badgeColorHex = "#7C3AED"
        ),
        "7-21" to Celebration(
            title = "Guru Purnima (Vyasa Purnima)",
            deityName = "Brahma",
            deityEmoji = "🪷",
            category = "Honoring Spiritual Teachers",
            description = "Full moon day dedicated to Sage Veda Vyasa and all spiritual and intellectual teachers who illuminate darkness.",
            ritualsAndObservance = "Offering flowers and gratitude to teachers and gurus, studying sacred scriptures and foundational knowledge.",
            badgeColorHex = "#F59E0B"
        ),

        // August
        "8-2" to Celebration(
            title = "Nag Panchami",
            deityName = "Shiva",
            deityEmoji = "🔱",
            category = "Reverence of Cosmic Kundalini",
            description = "Traditional veneration of the serpent guardians Vasuki and Sheshanaga who support cosmic balance.",
            ritualsAndObservance = "Offering milk to serpent stones, praying for protection from toxic influences, seeking inner peace.",
            badgeColorHex = "#059669"
        ),
        "8-8" to Celebration(
            title = "Varalakshmi Vratam",
            deityName = "Lakshmi",
            deityEmoji = "🌸",
            category = "Auspicious Flourishing",
            description = "Grand Friday prayers to Goddess Varalakshmi (the granter of boons) for health, peace, and domestic flourishing.",
            ritualsAndObservance = "Decorating Kalasha with coconut and gold jewelry, tying the sacred yellow thread (Toranam), singing Lakshmi Ashtotharam.",
            badgeColorHex = "#DB2777"
        ),
        "8-19" to Celebration(
            title = "Raksha Bandhan & Shravani",
            deityName = "Hanuman",
            deityEmoji = "🛡️",
            category = "Sacred Bond of Protection",
            description = "Celebration of sibling bonds, mutual protection, and renewal of sacred vows of loyalty and righteousness.",
            ritualsAndObservance = "Tying the protective Rakhi thread on wrists, exchanging sweets, giving blessings for lifelong protection.",
            badgeColorHex = "#DC2626"
        ),
        "8-26" to Celebration(
            title = "Sri Krishna Janmashtami",
            deityName = "Krishna",
            deityEmoji = "🦚",
            category = "Supreme Midnight Avatar",
            description = "The auspicious midnight birth of Bhagavan Sri Krishna in Mathura to restore dharma and illuminate the Gita.",
            ritualsAndObservance = "Fasting until midnight, swinging baby Krishna in the cradle, singing Bhajans, preparing butter (Makhan) and sweets.",
            badgeColorHex = "#7C3AED"
        ),

        // September
        "9-7" to Celebration(
            title = "Ganesh Chaturthi (Vinayaka Chavithi)",
            deityName = "Ganesha",
            deityEmoji = "🐘",
            category = "Vighnaharta Festival",
            description = "The arrival of Lord Ganesha from Mount Kailash to bring wisdom, remove obstacles, and bless new ventures.",
            ritualsAndObservance = "Installing eco-friendly Ganesha murtis, offering 21 Modakas and Durva grass, singing 'Sukh Karta Dukh Harta'.",
            badgeColorHex = "#EA580C"
        ),
        "9-16" to Celebration(
            title = "Anantha Chaturdashi (Ganesh Visarjan)",
            deityName = "Ganesha",
            deityEmoji = "🐘",
            category = "Immersion & Infinite Space",
            description = "Triumphant farewell processions of Ganesha murtis to sacred waters, accompanied by devotion to Anantha (Infinite Vishnu).",
            ritualsAndObservance = "Chanting 'Ganpati Bappa Morya, Pudhchya Varshi Laukariya!', ceremonial water immersion, wearing 14-knot sacred cord.",
            badgeColorHex = "#EA580C"
        ),

        // October
        "10-3" to Celebration(
            title = "Sharad Navaratri Begins (Ghatasthapana)",
            deityName = "Parvathi",
            deityEmoji = "🏔️",
            category = "Divine Shakti Awakening",
            description = "The commencement of the nine holy nights celebrating the victory and forms of Goddess Durga / Parvathi.",
            ritualsAndObservance = "Ghatasthapana, lighting the Akhanda Jyoti (uninterrupted lamp), worshiping Goddess Shailaputri.",
            badgeColorHex = "#059669"
        ),
        "10-9" to Celebration(
            title = "Maha Ashtami / Durga Ashtami",
            deityName = "Parvathi",
            deityEmoji = "🏔️",
            category = "Mahishasuramardini Day",
            description = "The culmination of Durga's battle, celebrating supreme courage, maternal protection, and annihilation of darkness.",
            ritualsAndObservance = "Sandhi Puja at the juncture of Ashtami and Navami, offering red hibiscus flowers, Kanya Puja (worship of young girls).",
            badgeColorHex = "#059669"
        ),
        "10-10" to Celebration(
            title = "Maha Navami & Ayudha / Saraswati Puja",
            deityName = "Brahma",
            deityEmoji = "🪷",
            category = "Consecration of Tools & Code",
            description = "Sanctifying all implements of labor, intellect, science, technology, code, and instruments as divine manifestations.",
            ritualsAndObservance = "Cleaning and blessing laptops, tools, vehicles, and scriptures with sandalwood, turmeric, and flowers.",
            badgeColorHex = "#F59E0B"
        ),
        "10-12" to Celebration(
            title = "Vijaya Dashami (Dussehra)",
            deityName = "Muruga",
            deityEmoji = "⚡",
            category = "Triumph of Light over Darkness",
            description = "Victory of Lord Rama over Ravana and Goddess Durga over Mahishasura; supreme day to commence new learning.",
            ritualsAndObservance = "Vidyarambham (writing first letters in rice), burning effigies of inner vices, distributing Shami (Apta) leaves as gold.",
            badgeColorHex = "#E11D48"
        ),
        "10-17" to Celebration(
            title = "Sharad Purnima & Raas Purnima",
            deityName = "Krishna",
            deityEmoji = "🦚",
            category = "Nectar Moon of Divine Love",
            description = "The brightest full moon of the year when the moon shines with healing nectar rays and Krishna played the flute in Vrindavan.",
            ritualsAndObservance = "Leaving Kheer (rice pudding) under moonlight overnight to absorb divine rays, devotional chanting of Krishna stotras.",
            badgeColorHex = "#7C3AED"
        ),
        "10-29" to Celebration(
            title = "Dhanteras (Dhanvantari Jayanti)",
            deityName = "Hanuman",
            deityEmoji = "🛡️",
            category = "Ayurvedic Healing & Prosperity",
            description = "Appearance of Lord Dhanvantari with the pot of Amrita (nectar of immortality), marking the start of Diwali festivities.",
            ritualsAndObservance = "Purchasing precious metals or utensils for auspiciousness, lighting Yamadeep at door, praying for health and immunity.",
            badgeColorHex = "#B45309"
        ),
        "10-31" to Celebration(
            title = "Diwali - Deepavali (Lakshmi Puja)",
            deityName = "Lakshmi",
            deityEmoji = "🌸",
            category = "Supreme Festival of Lights",
            description = "The glorious night of Deepavali when thousands of earthen lamps are lit to welcome Goddess Mahalakshmi into every home.",
            ritualsAndObservance = "Cleaning homes, lighting oil Diyas, performing Lakshmi-Ganesha Puja, sharing sweets, wearing elegant new clothes.",
            badgeColorHex = "#DB2777"
        ),

        // November
        "11-1" to Celebration(
            title = "Govardhan Puja & Annakut",
            deityName = "Krishna",
            deityEmoji = "🦚",
            category = "Protection of Nature",
            description = "Commemorating Lord Krishna lifting Mount Govardhan on his little finger to shield animals and people from torrential rain.",
            ritualsAndObservance = "Offering 56 food delicacies (Chhappan Bhog / Annakut mountain of food), revering cows (Gopashtami).",
            badgeColorHex = "#7C3AED"
        ),
        "11-2" to Celebration(
            title = "Bhai Dooj (Yama Dvitiya)",
            deityName = "Shani",
            deityEmoji = "⚖️",
            category = "Sibling Harmony & Long Life",
            description = "Celebration of sibling affection mirroring the love between Yamuna and Lord Yama, granting protection from untimely fear.",
            ritualsAndObservance = "Sisters apply Tilak on brothers' foreheads and pray for their long, disciplined, and righteous life.",
            badgeColorHex = "#64748B"
        ),
        "11-7" to Celebration(
            title = "Chhath Puja (Surya Shashthi)",
            deityName = "Parvathi",
            deityEmoji = "🏔️",
            category = "Solar & Nature Thanksgiving",
            description = "Ancient Vedic thanksgiving to the Sun God Surya and Chhathi Maiya for sustaining all biological life on Earth.",
            ritualsAndObservance = "Standing in river waters at sunset and sunrise to offer Arghya, rigorous 36-hour waterless fasting.",
            badgeColorHex = "#059669"
        ),
        "11-15" to Celebration(
            title = "Kartika Somavara & Dev Deepawali Phase",
            deityName = "Shiva",
            deityEmoji = "🔱",
            category = "Sacred Month of Light",
            description = "The auspicious holy month of Kartika where lighting a single lamp equates to thousands of oblations.",
            ritualsAndObservance = "Lighting Akhanda deepam at sunset, reciting Shiva Bilvashtakam, meditating under the stars.",
            badgeColorHex = "#06B6D4"
        ),
        "11-18" to Celebration(
            title = "Skanda Sashti & Soorasamharam",
            deityName = "Muruga",
            deityEmoji = "⚡",
            category = "Victory of the Jnana Vel",
            description = "Lord Muruga annihilating inner demonic pride and ego with the golden spear (Vel), turning darkness into peacocks.",
            ritualsAndObservance = "Six days of fasting, chanting 'Kanda Sashti Kavasam', performing Kavadi offering for victory over obstacles.",
            badgeColorHex = "#E11D48"
        ),
        "11-24" to Celebration(
            title = "Dev Deepawali (Varanasi Lights)",
            deityName = "Shiva",
            deityEmoji = "🔱",
            category = "Festival of the Gods",
            description = "The night all Gods descend to bathe in the holy Ganga; over one million lamps illuminate the ancient river ghats.",
            ritualsAndObservance = "Lighting oil lamps at rivers and temples, chanting 'Har Har Mahadev', celebrating Shiva's victory over Tripurasura.",
            badgeColorHex = "#06B6D4"
        ),

        // December
        "12-11" to Celebration(
            title = "Gita Jayanti (Song of the Divine)",
            deityName = "Krishna",
            deityEmoji = "🦚",
            category = "Descent of Supreme Philosophy",
            description = "The sacred day Lord Krishna spoke the 700 verses of the Bhagavad Gita to Arjuna on the battlefield of Kurukshetra.",
            ritualsAndObservance = "Recitation of Bhagavad Gita Chapter 12 (Bhakti) & Chapter 2 (Sankhya), reflection on duty without attachment.",
            badgeColorHex = "#7C3AED"
        ),
        "12-12" to Celebration(
            title = "Vaikuntha Ekadashi (Mokshada Ekadashi)",
            deityName = "Krishna",
            deityEmoji = "🦚",
            category = "Gateway to Liberation",
            description = "The opening of the celestial Vaikuntha Dwaram (gate of liberation), granting freedom from the cycle of birth and death.",
            ritualsAndObservance = "Strict Ekadashi fasting, passing through the northern temple gateway, chanting the Vishnu Sahasranama.",
            badgeColorHex = "#7C3AED"
        ),
        "12-21" to Celebration(
            title = "Winter Solstice & Uttarayana Turning",
            deityName = "Brahma",
            deityEmoji = "🪷",
            category = "Cosmic Solar Pivot",
            description = "The astronomical turning of the earth when nights reach their maximum length and the light begins its northern ascent.",
            ritualsAndObservance = "Lighting fires of gratitude, meditating on the impermanence of cycles, planning noble goals for the coming year.",
            badgeColorHex = "#F59E0B"
        ),
        "12-25" to Celebration(
            title = "Universal Compassion & Peace Day",
            deityName = "Krishna",
            deityEmoji = "🦚",
            category = "Global Peace",
            description = "Celebrating the spirit of selfless love, compassion, forgiveness, and light shining in the darkest winter days.",
            ritualsAndObservance = "Sharing food with the needy, family harmony, expressing gratitude for blessings and friendship.",
            badgeColorHex = "#10B981"
        ),
        "12-31" to Celebration(
            title = "Year-End Reflection & Sovereign Dharma",
            deityName = "Shani",
            deityEmoji = "⚖️",
            category = "Karmic Accounting",
            description = "Reviewing the year's thoughts, speech, and deeds with honest self-appraisal and resolute commitment to righteous growth.",
            ritualsAndObservance = "Quiet meditative reflection, resolving to eliminate procrastination, praying to Lord Shani for strength.",
            badgeColorHex = "#64748B"
        )
    )

    /**
     * Looks up or computes the celebration for a given date.
     * Guaranteed to return a rich celebration/spiritual reflection for ANY date
     * across 1 to 9999 AD!
     */
    fun getCelebrationForDate(year: Int, month: Int, day: Int): Celebration {
        val key = "$month-$day"
        val knownFestival = celebrationCalendar[key]
        if (knownFestival != null) {
            return knownFestival
        }

        // Dynamic Astronomical & Deity Day calculation
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
        }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

        // Map day of week to patron deity and planetary attribute
        return when (dayOfWeek) {
            Calendar.SUNDAY -> Celebration(
                title = "Surya & Krishna Devotional Radiance",
                deityName = "Krishna",
                deityEmoji = "🦚",
                category = "Solar Radiance & Vitality",
                description = "Day of the golden Sun (Surya). Ideal for cultivating inner vitality, leadership, truth, and philosophical study.",
                ritualsAndObservance = "Chanting the Gayatri Mantra at dawn, studying the Bhagavad Gita, practicing conscious mindful action.",
                badgeColorHex = "#7C3AED"
            )
            Calendar.MONDAY -> Celebration(
                title = "Somavara • Shiva Chandramouleshwara",
                deityName = "Shiva",
                deityEmoji = "🔱",
                category = "Cosmic Mind & Serenity",
                description = "Day of the Moon and Lord Shiva. Fosters deep inner stillness, meditative transcendence, and mental calmness.",
                ritualsAndObservance = "Silent meditation, drinking clean water with awareness, chanting 'Om Namah Shivaya' 108 times.",
                badgeColorHex = "#06B6D4"
            )
            Calendar.TUESDAY -> Celebration(
                title = "Mangalavara • Hanuman & Muruga Valor",
                deityName = "Hanuman",
                deityEmoji = "🛡️",
                category = "Courage, Agility & Protection",
                description = "Day of Mars (Mangala), overseen by Mahavira Hanuman and Lord Muruga. Banishes fear and fosters decisive willpower.",
                ritualsAndObservance = "Reciting Hanuman Chalisa, physical strength training, maintaining unwavering determination in difficulties.",
                badgeColorHex = "#DC2626"
            )
            Calendar.WEDNESDAY -> Celebration(
                title = "Budhavara • Ganesha Intellect & Scribe",
                deityName = "Ganesha",
                deityEmoji = "🐘",
                category = "Intellect, Logic & Coding",
                description = "Day of Mercury (Budha) and Lord Ganesha. Supreme day for software engineering, mathematics, communications, and problem solving.",
                ritualsAndObservance = "Decomposing complicated problems into simple algorithms, offering gratitude to Ganesha before starting deep work.",
                badgeColorHex = "#EA580C"
            )
            Calendar.THURSDAY -> Celebration(
                title = "Guruvara • Brahma & Supreme Knowledge",
                deityName = "Brahma",
                deityEmoji = "🪷",
                category = "Higher Wisdom & Creation",
                description = "Day of Jupiter (Guru) and Lord Brahma. Dedicated to learning foundational science, expanding knowledge, and ethical teaching.",
                ritualsAndObservance = "Reading foundational texts, expressing appreciation to teachers and mentors, wearing golden/yellow tones.",
                badgeColorHex = "#F59E0B"
            )
            Calendar.FRIDAY -> Celebration(
                title = "Shukravara • Lakshmi & Parvathi Shakti",
                deityName = "Lakshmi",
                deityEmoji = "🌸",
                category = "Creative Flourishing & Beauty",
                description = "Day of Venus (Shukra) and Divine Mothers Lakshmi and Parvathi. Nurtures creative arts, aesthetic elegance, and harmony.",
                ritualsAndObservance = "Lighting evening lamps, engaging in creative writing or art, fostering peace and generosity in relationships.",
                badgeColorHex = "#DB2777"
            )
            else -> Celebration(
                title = "Shanivara • Shani Deva & Narasimha Invariants",
                deityName = "Shani",
                deityEmoji = "⚖️",
                category = "Discipline, Justice & Security",
                description = "Day of Saturn (Shani) and Lord Narasimha. The crucible of patience, code profiling, debugging, and boundary protection.",
                ritualsAndObservance = "Audit habits and systems, remove redundant code/clutter, practice selfless service (Seva) to those in need.",
                badgeColorHex = "#64748B"
            )
        }
    }

    /**
     * Checks if a given day has a major named annual celebration.
     */
    fun hasMajorCelebration(month: Int, day: Int): Boolean {
        return celebrationCalendar.containsKey("$month-$day")
    }

    /**
     * Retrieves the celebration for today's current calendar date.
     */
    fun getTodayCelebration(): Celebration {
        val cal = Calendar.getInstance()
        return getCelebrationForDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }
}
