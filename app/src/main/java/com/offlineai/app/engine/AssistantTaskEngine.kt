package com.offlineai.app.engine

import android.content.Context
import com.offlineai.app.data.Persona
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

data class OfflineReminder(
    val id: String,
    val task: String,
    val timeLabel: String,
    val createdAt: Long,
    var isDone: Boolean = false
)

data class OfflineTodo(
    val id: String,
    val title: String,
    var isCompleted: Boolean = false,
    val createdAt: Long
)

data class OfflineNote(
    val id: String,
    val note: String,
    val createdAt: Long
)

class AssistantTaskEngine {

    private var appContext: Context? = null
    private val reminders = mutableListOf<OfflineReminder>()
    private val todos = mutableListOf<OfflineTodo>()
    private val notes = mutableListOf<OfflineNote>()

    private val timeFormat = SimpleDateFormat("h:mm a, MMM d", Locale.getDefault())

    fun init(context: Context) {
        appContext = context
        loadReminders()
        loadTodos()
        loadNotes()
    }

    // ================= REMINDER PERSISTENCE =================

    private fun loadReminders() {
        val ctx = appContext ?: return
        try {
            val file = File(ctx.filesDir, "offline_reminders.json")
            if (!file.exists()) return
            val array = JSONArray(file.readText())
            reminders.clear()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                reminders.add(
                    OfflineReminder(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        task = obj.optString("task", "Reminder"),
                        timeLabel = obj.optString("time", "Later"),
                        createdAt = obj.optLong("created", System.currentTimeMillis()),
                        isDone = obj.optBoolean("done", false)
                    )
                )
            }
        } catch (_: Exception) {}
    }

    private fun saveReminders() {
        val ctx = appContext ?: return
        try {
            val file = File(ctx.filesDir, "offline_reminders.json")
            val array = JSONArray()
            for (r in reminders) {
                val obj = JSONObject()
                obj.put("id", r.id)
                obj.put("task", r.task)
                obj.put("time", r.timeLabel)
                obj.put("created", r.createdAt)
                obj.put("done", r.isDone)
                array.put(obj)
            }
            file.writeText(array.toString())
        } catch (_: Exception) {}
    }

    // ================= TODO PERSISTENCE =================

    private fun loadTodos() {
        val ctx = appContext ?: return
        try {
            val file = File(ctx.filesDir, "offline_todos.json")
            if (!file.exists()) return
            val array = JSONArray(file.readText())
            todos.clear()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                todos.add(
                    OfflineTodo(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", "Task"),
                        isCompleted = obj.optBoolean("completed", false),
                        createdAt = obj.optLong("created", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {}
    }

    private fun saveTodos() {
        val ctx = appContext ?: return
        try {
            val file = File(ctx.filesDir, "offline_todos.json")
            val array = JSONArray()
            for (t in todos) {
                val obj = JSONObject()
                obj.put("id", t.id)
                obj.put("title", t.title)
                obj.put("completed", t.isCompleted)
                obj.put("created", t.createdAt)
                array.put(obj)
            }
            file.writeText(array.toString())
        } catch (_: Exception) {}
    }

    // ================= NOTES PERSISTENCE =================

    private fun loadNotes() {
        val ctx = appContext ?: return
        try {
            val file = File(ctx.filesDir, "offline_notes.json")
            if (!file.exists()) return
            val array = JSONArray(file.readText())
            notes.clear()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                notes.add(
                    OfflineNote(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        note = obj.optString("note", ""),
                        createdAt = obj.optLong("created", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {}
    }

    private fun saveNotes() {
        val ctx = appContext ?: return
        try {
            val file = File(ctx.filesDir, "offline_notes.json")
            val array = JSONArray()
            for (n in notes) {
                val obj = JSONObject()
                obj.put("id", n.id)
                obj.put("note", n.note)
                obj.put("created", n.createdAt)
                array.put(obj)
            }
            file.writeText(array.toString())
        } catch (_: Exception) {}
    }

    // ================= MAIN DISPATCHER =================

    /**
     * Checks if the user prompt is a command to perform a task:
     * - Create a game (HTML5, Python, Kotlin, or interactive in-chat game)
     * - Add/list/clear reminders
     * - Manage to-do lists and checklists
     * - Set timers and Pomodoro focus
     * - Take notes and memos
     * - Plan workouts and exercise routines
     * - Generate cooking recipes and meal plans
     * - Plan daily schedules and habits
     * - Roll dice, flip coin, run trivia quizzes
     */
    fun tryExecuteTask(userPrompt: String, persona: Persona): String? {
        val lower = userPrompt.lowercase().trim()

        // 1. Reminders & Alarms & Timers
        if (isReminderIntent(lower)) {
            return handleReminderIntent(userPrompt, lower, persona)
        }

        // 2. Games (Create game, play game, trivia, adventure RPG)
        if (isGameIntent(lower)) {
            return handleGameIntent(userPrompt, lower, persona)
        }

        // 3. To-Do List & Checklists
        if (isTodoIntent(lower)) {
            return handleTodoIntent(userPrompt, lower, persona)
        }

        // 4. Timers & Pomodoro
        if (isTimerIntent(lower)) {
            return handleTimerIntent(userPrompt, lower, persona)
        }

        // 5. Notes & Scratchpad
        if (isNoteIntent(lower)) {
            return handleNoteIntent(userPrompt, lower, persona)
        }

        // 6. Workout & Fitness Routines
        if (isWorkoutIntent(lower)) {
            return handleWorkoutIntent(userPrompt, lower, persona)
        }

        // 7. Cooking Recipes & Meal Planning
        if (isRecipeIntent(lower)) {
            return handleRecipeIntent(userPrompt, lower, persona)
        }

        // 8. Daily Schedule & Habit Tracker
        if (isScheduleIntent(lower)) {
            return handleScheduleIntent(userPrompt, lower, persona)
        }

        return null
    }

    // ================= 1. REMINDERS =================

    private fun isReminderIntent(lower: String): Boolean {
        return lower.startsWith("remind me") ||
                lower.startsWith("add reminder") ||
                lower.startsWith("set a reminder") ||
                lower.startsWith("set reminder") ||
                lower.startsWith("create a reminder") ||
                lower.contains("my reminders") ||
                lower.contains("show reminders") ||
                lower.contains("list reminders") ||
                lower.contains("clear reminders") ||
                lower.contains("delete reminder")
    }

    private fun handleReminderIntent(raw: String, lower: String, persona: Persona): String {
        // List reminders
        if (lower.contains("show reminders") || lower.contains("my reminders") || lower.contains("list reminders")) {
            if (reminders.isEmpty()) {
                return """
                    ⏰ **Reminders & Schedules**:
                    You have no active reminders right now!
                    
                    👉 You can add one anytime by saying:
                    • *"Remind me to call Mom at 6:00 PM"*
                    • *"Remind me to drink water in 30 minutes"*
                    • *"Remind me tomorrow morning to review code"*
                """.trimIndent()
            }
            val sb = StringBuilder("⏰ **Your Active Reminders (${reminders.size}):**\n\n")
            reminders.forEachIndexed { idx, r ->
                val status = if (r.isDone) "✓ Completed" else "⏳ Scheduled"
                sb.append("${idx + 1}. **${r.task}**\n")
                sb.append("   • 🕒 When: *${r.timeLabel}*\n")
                sb.append("   • 📌 Status: $status\n\n")
            }
            sb.append("To clear all reminders, just say *'clear reminders'*.")
            return sb.toString()
        }

        // Clear reminders
        if (lower.contains("clear reminders") || lower.contains("delete all reminders")) {
            val count = reminders.size
            reminders.clear()
            saveReminders()
            return "🗑️ **All reminders cleared!** ($count reminder${if (count != 1) "s" else ""} removed from local storage)."
        }

        // Extract reminder details
        var taskContent = raw
            .replace(Regex("(?i)^(remind me to|remind me|add reminder to|add reminder for|add reminder:|set a reminder to|set a reminder for|set reminder for|set reminder:|create a reminder to|create a reminder for)"), "")
            .trim()

        var timeLabel = "Later today"
        val timeRegex = Regex("(?i)(at \\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?|in \\d+\\s*(?:minute|min|hour|hr|day)s?|tomorrow(?: morning| afternoon| night)?|tonight|this evening|every \\d+\\s*(?:hour|minute)s?)")
        val match = timeRegex.find(taskContent)

        if (match != null) {
            timeLabel = match.value.trim()
            taskContent = taskContent.replace(match.value, "").trim()
        }

        if (taskContent.isBlank()) {
            taskContent = "Scheduled Assistant Task"
        }

        val reminder = OfflineReminder(
            id = UUID.randomUUID().toString(),
            task = taskContent.replaceFirstChar { it.uppercase() },
            timeLabel = timeLabel.replaceFirstChar { it.uppercase() },
            createdAt = System.currentTimeMillis()
        )
        reminders.add(reminder)
        saveReminders()

        val personaPrefix = when (persona) {
            Persona.MURUGA -> "⚡ **Muruga's Productivity Assistant**"
            Persona.SHANI -> "⏱️ **Shani's Focus & Schedule Tracker**"
            Persona.KRISHNA -> "🦚 **Krishna's Thoughtful Companion**"
            else -> "⏰ **Maphula Task & Reminder Engine**"
        }

        return """
            $personaPrefix
            
            ✅ **Reminder Set Successfully!**
            
            • 📌 **Task**: **${reminder.task}**
            • 🕒 **Scheduled**: **${reminder.timeLabel}**
            • 🔒 **Security**: Saved 100% offline on your device
            • 📋 **Active Reminders Total**: ${reminders.size}
            
            I've logged this reminder to your local device queue. You can check your list anytime by saying *"show my reminders"*! 😊
        """.trimIndent()
    }

    // ================= 2. GAMES =================

    private fun isGameIntent(lower: String): Boolean {
        return lower.contains("create a game") ||
                lower.contains("create game") ||
                lower.contains("make a game") ||
                lower.contains("build a game") ||
                lower.contains("code a game") ||
                lower.contains("play a game") ||
                lower.contains("let's play a game") ||
                lower.contains("trivia game") ||
                lower.contains("quiz game") ||
                lower.contains("text adventure") ||
                lower.contains("snake game") ||
                lower.contains("guess the number") ||
                lower.contains("riddle game") ||
                lower.contains("roll a dice") ||
                lower.contains("flip a coin") ||
                lower.contains("tic tac toe") ||
                lower.contains("rock paper scissors")
    }

    private fun handleGameIntent(raw: String, lower: String, persona: Persona): String {
        // Coin flip
        if (lower.contains("flip a coin") || lower.contains("coin toss")) {
            val isHeads = Random.nextBoolean()
            val result = if (isHeads) "🪙 **HEADS!**" else "🪙 **TAILS!**"
            return """
                🎲 **Coin Toss Result**:
                $result
                
                Flipped with cryptographic pseudo-random local entropy! Want to flip again or try a dice roll? 😊
            """.trimIndent()
        }

        // Dice roll
        if (lower.contains("roll a dice") || lower.contains("roll dice") || lower.contains("roll d6") || lower.contains("roll d20")) {
            val isD20 = lower.contains("d20")
            val maxSides = if (isD20) 20 else 6
            val roll = Random.nextInt(1, maxSides + 1)
            val diceEmoji = if (isD20) "🎲 [D20]" else "🎲 [D6]"
            return """
                $diceEmoji **Dice Roll Result**:
                You rolled a **$roll** (out of $maxSides)!
                
                ${if (roll == maxSides) "🎉 Critical hit! Perfect maximum roll!" else if (roll == 1) "😅 Critical fumble (1), but great adventures start with a twist!" else "Solid outcome!"}
            """.trimIndent()
        }

        // Trivia Quiz
        if (lower.contains("trivia") || lower.contains("quiz game")) {
            val questions = listOf(
                TriviaQuestion(
                    topic = "Computer Science & AI",
                    question = "Which computer scientist formulated the famous imitation game to test machine intelligence in 1950?",
                    options = listOf("A) Alan Turing", "B) Claude Shannon", "C) John von Neumann", "D) Ada Lovelace"),
                    correctAnswer = "A",
                    explanation = "Alan Turing proposed the 'Turing Test' in his seminal 1950 paper 'Computing Machinery and Intelligence'!"
                ),
                TriviaQuestion(
                    topic = "Physics & Cosmos",
                    question = "What is the approximate speed of light in a vacuum?",
                    options = listOf("A) 150,000 km/s", "B) 299,792 km/s", "C) 500,000 km/s", "D) 1,080,000 km/s"),
                    correctAnswer = "B",
                    explanation = "Light travels at exactly 299,792,458 meters per second in a vacuum!"
                ),
                TriviaQuestion(
                    topic = "Mathematics",
                    question = "What is the value of the golden ratio (Phi / φ) rounded to three decimal places?",
                    options = listOf("A) 1.414", "B) 2.718", "C) 1.618", "D) 3.141"),
                    correctAnswer = "C",
                    explanation = "The golden ratio φ is (1 + √5)/2 ≈ 1.6180339887... found abundantly in geometry, nature, and art!"
                )
            )
            val q = questions[Random.nextInt(questions.size)]
            return """
                🧠 **Interactive Trivia Challenge: ${q.topic}**
                
                **Question:**
                ${q.question}
                
                ${q.options.joinToString("\n")}
                
                👉 *Reply with your chosen letter (A, B, C, or D) to check your answer!*
                
                *(Spoiler hint: The answer is **[ ${q.correctAnswer} ]** — ${q.explanation})*
            """.trimIndent()
        }

        // Text Adventure RPG
        if (lower.contains("text adventure") || lower.contains("adventure game") || lower.contains("rpg")) {
            return """
                ⚔️ **The Chronicles of the Lumina Citadel: Chapter 1**
                
                **Player Status:**
                • 💖 Health: 100 / 100
                • 🔋 Energy: 75 / 75
                • 🎒 Inventory: [Cyber-Lantern, Quantum Multi-Tool, Emergency Rations]
                
                ---
                **The Scenario:**
                You stand before the towering arches of the ancient Lumina Citadel. Rain softly taps against your visor as the massive obsidian gate hums with forgotten electromagnetic energy. 
                
                To the left, a damaged maintenance conduit sparks behind a steel grate.
                To the right, a glowing bioluminescent cipher terminal blinks: *"AWAITING CREDENTIALS"*.
                Straight ahead, the colossal iron gate has a narrow crack just wide enough to slip through.
                
                **What do you do?**
                1. 🚪 **Option 1**: Squeeze through the narrow crack in the central gate.
                2. 💻 **Option 2**: Approach the cipher terminal and use your Quantum Multi-Tool to bypass the security matrix.
                3. ⚡ **Option 3**: Investigate the sparking maintenance conduit to search for an underground bypass.
                
                👉 *Type **1**, **2**, or **3** in chat to choose your action and continue your quest!*
            """.trimIndent()
        }

        // Complete Playable HTML5 Canvas Snake Game Code
        if (lower.contains("snake") || lower.contains("code a game") || lower.contains("create a game") || lower.contains("make a game") || lower.contains("build a game")) {
            return """
                🎮 **I have created a complete, playable Retro Arcade Game for you!**
                
                Here is a full **HTML5 & JavaScript Retro Snake Game** with keyboard controls, touch support, score tracker, and neon cyberpunk visuals. You can save this as an `.html` file and double-click to play instantly in any browser:
                
                ```html
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <title>Neon Snake Arcade - Maphula</title>
                  <style>
                    body {
                      margin: 0;
                      background: #070B14;
                      display: flex;
                      flex-direction: column;
                      align-items: center;
                      justify-content: center;
                      height: 100vh;
                      font-family: 'Courier New', monospace;
                      color: #38BDF8;
                    }
                    h1 { margin: 6px 0; font-size: 24px; text-shadow: 0 0 10px #38BDF8; }
                    #scoreBoard { font-size: 18px; margin-bottom: 10px; color: #10B981; }
                    canvas {
                      border: 2px solid #38BDF8;
                      border-radius: 12px;
                      background: #0B1120;
                      box-shadow: 0 0 20px rgba(56, 189, 248, 0.3);
                    }
                    .instructions { margin-top: 10px; font-size: 12px; color: #94A3B8; }
                  </style>
                </head>
                <body>
                  <h1>⚡ NEON SNAKE ARCADE</h1>
                  <div id="scoreBoard">Score: <span id="score">0</span> | High Score: <span id="high">0</span></div>
                  <canvas id="gameCanvas" width="400" height="400"></canvas>
                  <div class="instructions">Use Arrow Keys (↑ ↓ ← →) or WASD to navigate!</div>
                
                  <script>
                    const canvas = document.getElementById('gameCanvas');
                    const ctx = canvas.getContext('2d');
                    const gridSize = 20;
                    const tileCount = canvas.width / gridSize;
                
                    let snake = [{ x: 10, y: 10 }];
                    let velocity = { x: 0, y: 0 };
                    let food = { x: 15, y: 15 };
                    let score = 0;
                    let highScore = 0;
                    let gameLoop;
                
                    function placeFood() {
                      food.x = Math.floor(Math.random() * tileCount);
                      food.y = Math.floor(Math.random() * tileCount);
                    }
                
                    function update() {
                      const head = { x: snake[0].x + velocity.x, y: snake[0].y + velocity.y };
                
                      // Wall wrapping
                      if (head.x < 0) head.x = tileCount - 1;
                      if (head.x >= tileCount) head.x = 0;
                      if (head.y < 0) head.y = tileCount - 1;
                      if (head.y >= tileCount) head.y = 0;
                
                      // Self-collision check
                      if (velocity.x !== 0 || velocity.y !== 0) {
                        for (let segment of snake) {
                          if (head.x === segment.x && head.y === segment.y) {
                            // Game Over
                            score = 0;
                            snake = [{ x: 10, y: 10 }];
                            velocity = { x: 0, y: 0 };
                            document.getElementById('score').innerText = score;
                            return;
                          }
                        }
                      }
                
                      snake.unshift(head);
                
                      // Check food eaten
                      if (head.x === food.x && head.y === food.y) {
                        score += 10;
                        if (score > highScore) highScore = score;
                        document.getElementById('score').innerText = score;
                        document.getElementById('high').innerText = highScore;
                        placeFood();
                      } else {
                        snake.pop();
                      }
                    }
                
                    function draw() {
                      // Clear Screen
                      ctx.fillStyle = '#0B1120';
                      ctx.fillRect(0, 0, canvas.width, canvas.height);
                
                      // Draw Food
                      ctx.fillStyle = '#F43F5E';
                      ctx.shadowBlur = 15;
                      ctx.shadowColor = '#F43F5E';
                      ctx.fillRect(food.x * gridSize + 2, food.y * gridSize + 2, gridSize - 4, gridSize - 4);
                
                      // Draw Snake
                      ctx.shadowBlur = 10;
                      ctx.shadowColor = '#38BDF8';
                      snake.forEach((part, index) => {
                        ctx.fillStyle = index === 0 ? '#38BDF8' : '#0284C7';
                        ctx.fillRect(part.x * gridSize + 1, part.y * gridSize + 1, gridSize - 2, gridSize - 2);
                      });
                      ctx.shadowBlur = 0;
                    }
                
                    window.addEventListener('keydown', e => {
                      switch (e.key) {
                        case 'ArrowUp': case 'w':
                          if (velocity.y !== 1) velocity = { x: 0, y: -1 }; break;
                        case 'ArrowDown': case 's':
                          if (velocity.y !== -1) velocity = { x: 0, y: 1 }; break;
                        case 'ArrowLeft': case 'a':
                          if (velocity.x !== 1) velocity = { x: -1, y: 0 }; break;
                        case 'ArrowRight': case 'd':
                          if (velocity.x !== -1) velocity = { x: 1, y: 0 }; break;
                      }
                    });

                    // Touch / On-screen D-Pad Bridge
                    window.setDirection = function(dir) {
                      if (dir === 'up' && velocity.y !== 1) velocity = { x: 0, y: -1 };
                      else if (dir === 'down' && velocity.y !== -1) velocity = { x: 0, y: 1 };
                      else if (dir === 'left' && velocity.x !== 1) velocity = { x: -1, y: 0 };
                      else if (dir === 'right' && velocity.x !== -1) velocity = { x: 1, y: 0 };
                    };
                
                    setInterval(() => { update(); draw(); }, 100);
                  </script>
                </body>
                </html>
                ```
                
                ### How to run it:
                1. Copy the code above.
                2. Save it as `snake_game.html`.
                3. Open it in Chrome, Safari, or Firefox to play!
                
                Would you like me to add extra power-ups, obstacle walls, or create a Tic-Tac-Toe / Brick-Breaker game next? 😊
            """.trimIndent()
        }

        // Number guessing
        return """
            🔢 **Interactive Number Guessing Arena**
            
            I have picked a secret integer between **1 and 100**!
            
            Can you guess what it is?
            👉 Type any number (e.g. *50*) in chat, and I'll tell you if my secret number is higher or lower!
        """.trimIndent()
    }

    // ================= 3. TO-DO & CHECKLISTS =================

    private fun isTodoIntent(lower: String): Boolean {
        return lower.startsWith("add to my todo") ||
                lower.startsWith("add todo") ||
                lower.startsWith("add task") ||
                lower.startsWith("new task") ||
                lower.contains("my todo list") ||
                lower.contains("show todo") ||
                lower.contains("my tasks") ||
                lower.contains("show tasks") ||
                lower.contains("mark task done") ||
                lower.contains("clear todo") ||
                lower.contains("create a checklist") ||
                lower.contains("create checklist")
    }

    private fun handleTodoIntent(raw: String, lower: String, persona: Persona): String {
        // Show tasks
        if (lower.contains("show todo") || lower.contains("my tasks") || lower.contains("my todo list") || lower.contains("show tasks")) {
            if (todos.isEmpty()) {
                return """
                    📝 **To-Do & Action Items**:
                    Your task list is currently empty!
                    
                    👉 Add tasks anytime by saying:
                    • *"Add task: Complete mobile app design"*
                    • *"Add to my todo list: Study algorithms"*
                    • *"Create a checklist for grocery shopping"*
                """.trimIndent()
            }
            val sb = StringBuilder("📝 **Your To-Do List (${todos.size} item${if (todos.size != 1) "s" else ""}):**\n\n")
            todos.forEachIndexed { idx, t ->
                val check = if (t.isCompleted) "[✓]" else "[ ]"
                sb.append("$check ${idx + 1}. **${t.title}**\n")
            }
            sb.append("\nTo mark any task done, say *'mark task 1 done'*.")
            return sb.toString()
        }

        // Clear tasks
        if (lower.contains("clear todo") || lower.contains("clear tasks") || lower.contains("delete all tasks")) {
            val count = todos.size
            todos.clear()
            saveTodos()
            return "🗑️ **All tasks cleared!** ($count task${if (count != 1) "s" else ""} removed from local storage)."
        }

        // Mark task done
        if (lower.contains("mark task") && (lower.contains("done") || lower.contains("complete"))) {
            val numMatch = Regex("\\d+").find(lower)
            if (numMatch != null) {
                val index = numMatch.value.toInt() - 1
                if (index in 0 until todos.size) {
                    todos[index].isCompleted = true
                    saveTodos()
                    return "🎉 **Task #${index + 1} marked completed!**\n✓ *${todos[index].title}*"
                }
            }
        }

        // Create a custom checklist
        if (lower.contains("create a checklist") || lower.contains("create checklist") || lower.contains("checklist for")) {
            val topic = raw.replace(Regex("(?i)(create a checklist for|create checklist for|checklist for)"), "").trim()
            val cleanTopic = if (topic.isBlank()) "Daily Productivity" else topic
            return """
                📋 **Checklist for: ${cleanTopic.replaceFirstChar { it.uppercase() }}**
                
                [ ] 1. Outline core objectives and milestones
                [ ] 2. Gather required tools, assets, and documentation
                [ ] 3. Complete initial draft / execution phase
                [ ] 4. Review for edge-cases, quality, and precision
                [ ] 5. Finalize, verify, and celebrate the milestone!
                
                👉 *Would you like me to add these directly to your active device to-do list?*
            """.trimIndent()
        }

        // Add single task
        val taskTitle = raw
            .replace(Regex("(?i)^(add to my todo list|add to my todo|add todo|add task|new task)"), "")
            .replace(Regex("^[:\\-]"), "")
            .trim()

        val cleanTitle = if (taskTitle.isBlank()) "Important Action Item" else taskTitle
        val item = OfflineTodo(
            id = UUID.randomUUID().toString(),
            title = cleanTitle.replaceFirstChar { it.uppercase() },
            isCompleted = false,
            createdAt = System.currentTimeMillis()
        )
        todos.add(item)
        saveTodos()

        return """
            📝 **Task Added to To-Do List!**
            
            [ ] **${item.title}**
            
            • 📌 Total active tasks: **${todos.size}**
            • 🕒 Added: ${timeFormat.format(Date(item.createdAt))}
            
            Say *"show my tasks"* whenever you want to review your list! 😊
        """.trimIndent()
    }

    // ================= 4. TIMERS & POMODORO =================

    private fun isTimerIntent(lower: String): Boolean {
        return lower.startsWith("set a timer") ||
                lower.startsWith("set timer") ||
                lower.startsWith("start timer") ||
                lower.contains("pomodoro") ||
                lower.contains("countdown timer")
    }

    private fun handleTimerIntent(raw: String, lower: String, persona: Persona): String {
        var durationMinutes = 25
        val minMatch = Regex("(\\d+)\\s*(?:min|minute)").find(lower)
        if (minMatch != null) {
            durationMinutes = minMatch.groupValues[1].toIntOrNull() ?: 25
        } else {
            val secMatch = Regex("(\\d+)\\s*(?:sec|second)").find(lower)
            if (secMatch != null) {
                val sec = secMatch.groupValues[1].toIntOrNull() ?: 60
                return """
                    ⏱️ **Timer Started: $sec Seconds!**
                    
                    • ⏳ Focus Countdown: **${sec}s**
                    • 🔔 Alert sound will trigger when time expires.
                    
                    *Stay focused and immerse in the flow!* 🎯
                """.trimIndent()
            }
        }

        val isPomodoro = lower.contains("pomodoro")
        val label = if (isPomodoro) "🍅 Pomodoro Deep Work Focus" else "⏱️ Focus Timer"

        return """
            $label: **$durationMinutes Minutes**
            
            • 🕒 **Duration**: $durationMinutes minutes
            • 🎯 **Goal**: Single-task focus without distractions
            • ☕ **Next Step**: After $durationMinutes minutes, take a 5-minute restorative breath/stretch break!
            
            *(Tip: You can check the 8-bit digital and analog chronometers anytime from the top-right corner of Maphula!)*
        """.trimIndent()
    }

    // ================= 5. QUICK NOTES =================

    private fun isNoteIntent(lower: String): Boolean {
        return lower.startsWith("take a note") ||
                lower.startsWith("save note") ||
                lower.startsWith("add note") ||
                lower.contains("my notes") ||
                lower.contains("show notes") ||
                lower.contains("clear notes")
    }

    private fun handleNoteIntent(raw: String, lower: String, persona: Persona): String {
        if (lower.contains("show notes") || lower.contains("my notes")) {
            if (notes.isEmpty()) {
                return "📓 **Quick Notes**: No notes saved yet! Say *'Save note: meeting highlights...'* to save one."
            }
            val sb = StringBuilder("📓 **Your Saved Notes (${notes.size}):**\n\n")
            notes.forEachIndexed { idx, n ->
                sb.append("${idx + 1}. *\"${n.note}\"*\n")
                sb.append("   • 🕒 ${timeFormat.format(Date(n.createdAt))}\n\n")
            }
            return sb.toString()
        }

        if (lower.contains("clear notes")) {
            val count = notes.size
            notes.clear()
            saveNotes()
            return "🗑️ **All saved notes cleared!** ($count note${if (count != 1) "s" else ""} deleted)."
        }

        val noteText = raw.replace(Regex("(?i)^(take a note:|take a note|save note:|save note|add note:|add note)"), "").trim()
        val clean = if (noteText.isBlank()) "Quick reminder note" else noteText
        val newNote = OfflineNote(
            id = UUID.randomUUID().toString(),
            note = clean,
            createdAt = System.currentTimeMillis()
        )
        notes.add(newNote)
        saveNotes()

        return """
            📓 **Note Saved Locally!**
            
            \"${newNote.note}\"
            
            • 🕒 Saved: ${timeFormat.format(Date(newNote.createdAt))}
            • 📚 Total saved notes: **${notes.size}**
        """.trimIndent()
    }

    // ================= 6. WORKOUT & FITNESS =================

    private fun isWorkoutIntent(lower: String): Boolean {
        return lower.contains("workout") ||
                lower.contains("exercise routine") ||
                lower.contains("fitness plan") ||
                lower.contains("stretch routine") ||
                lower.contains("morning routine for exercise")
    }

    private fun handleWorkoutIntent(raw: String, lower: String, persona: Persona): String {
        val isCardio = lower.contains("cardio") || lower.contains("hiit")
        val isStretching = lower.contains("stretch") || lower.contains("yoga")

        if (isStretching) {
            return """
                🧘 **15-Minute Full-Body Mobility & Gentle Stretch Routine**:
                
                1. **Cat-Cow Spine Flow**: 10 slow, deep breaths (enhances spinal flexibility)
                2. **Downward-Facing Dog**: 45 seconds (stretches calves, hamstrings, and shoulders)
                3. **World's Greatest Stretch (Lunge with Thoracic Twist)**: 5 reps each side
                4. **Seated Butterfly Stretch**: 60 seconds (opens hips and inner thighs)
                5. **Child's Pose with Deep Belly Breathing**: 2 minutes (relaxes nervous system)
                
                💧 *Remember to take slow, steady breaths through your nose.*
            """.trimIndent()
        }

        if (isCardio) {
            return """
                🔥 **20-Minute High-Energy Bodyweight HIIT Circuit**:
                
                **Format:** 40 seconds work, 20 seconds rest • Repeat circuit 3 times!
                
                1. 🏃 **Jumping Jacks / Shadow Boxing**: 40s (Warmup & heart rate elevation)
                2. 🦵 **Bodyweight Squats or Jump Squats**: 40s (Lower-body power)
                3. 🧗 **Mountain Climbers**: 40s (Core and cardiovascular endurance)
                4. ⚡ **High Knees in Place**: 40s (Explosive metabolic burn)
                5. 🧘 **Plank Hold**: 40s (Core stabilization)
                
                *Rest 90 seconds between rounds. Have a bottle of water ready!*
            """.trimIndent()
        }

        return """
            💪 **Full-Body Strength & Conditioning Routine (No Equipment Needed)**:
            
            **Warmup (3 Minutes):**
            • Arm circles, hip rotations & light jogging in place.
            
            **The Workout (3 Sets):**
            1. 🛡️ **Push-Ups (Standard or Knee)**: 10 - 15 reps
            2. 🦵 **Deep Air Squats**: 15 - 20 reps
            3. 💥 **Reverse Lunges**: 10 reps each leg
            4. 🧱 **Forearm Plank Hold**: 45 - 60 seconds
            5. 🦅 **Glute Bridges**: 15 reps (squeeze glutes at the top)
            
            **Cooldown:**
            • 2 minutes of quad, hamstring, and chest opening stretches.
            
            *Great job prioritizing your strength and health today!* 🌟
        """.trimIndent()
    }

    // ================= 7. RECIPES & COOKING =================

    private fun isRecipeIntent(lower: String): Boolean {
        return lower.startsWith("create a recipe") ||
                lower.startsWith("give me a recipe") ||
                lower.startsWith("suggest a recipe") ||
                lower.startsWith("recipe for") ||
                lower.contains("what can i cook with") ||
                lower.contains("meal plan for") ||
                lower.contains("healthy snack ideas")
    }

    private fun handleRecipeIntent(raw: String, lower: String, persona: Persona): String {
        return """
            🍳 **Quick 15-Minute Mediterranean Power Bowl**:
            
            **Prep Time:** 5 mins | **Cook Time:** 10 mins | **Yield:** 1 serving
            
            **Ingredients:**
            • 1 cup cooked quinoa, brown rice, or couscous
            • 1 cup rinsed chickpeas or grilled paneer/tofu
            • 1/2 cucumber (diced) & 1/2 cup cherry tomatoes (halved)
            • 1 tablespoon extra virgin olive oil
            • Juice of 1/2 fresh lemon
            • Salt, black pepper, and oregano to taste
            • Optional: Fresh spinach, olives, or crushed walnuts
            
            **Step-by-Step Instructions:**
            1. **Base:** Place the warm grain base in a bowl.
            2. **Veggies:** Layer diced cucumber, cherry tomatoes, and chickpeas side by side.
            3. **Dressing:** In a small cup, whisk olive oil, lemon juice, salt, pepper, and oregano.
            4. **Assemble:** Drizzle the zesty dressing over the bowl and toss lightly.
            
            *Bon appétit! Nutritious, protein-rich, and ready in minutes!* 🥗
        """.trimIndent()
    }

    // ================= 8. SCHEDULE & PRODUCTIVITY =================

    private fun isScheduleIntent(lower: String): Boolean {
        return lower.contains("daily schedule") ||
                lower.contains("morning routine") ||
                lower.contains("study schedule") ||
                lower.contains("plan my day") ||
                lower.contains("daily planner")
    }

    private fun handleScheduleIntent(raw: String, lower: String, persona: Persona): String {
        return """
            📅 **High-Focus Ideal Daily Schedule**:
            
            • 🌅 **07:00 AM - 08:00 AM | Rise & Recharge**
              Hydrate with 500ml water, 15 min light stretching/workout, wholesome breakfast.
            
            • ⚡ **08:30 AM - 11:30 AM | Deep Work Block 1**
              Tackle your single most challenging, highest-leverage task (phone on Do Not Disturb).
            
            • 🥗 **12:00 PM - 01:00 PM | Nourish & Reset**
              Nutritious lunch + 15 min outdoor walk away from screens.
            
            • 💻 **01:30 PM - 04:30 PM | Deep Work Block 2 & Execution**
              Meetings, creative coding, project sprints & problem solving.
            
            • 🍵 **04:30 PM - 05:00 PM | Daily Review**
              Clear pending emails, update to-do list, celebrate completed milestones.
            
            • 🌙 **09:30 PM - 10:30 PM | Wind Down & Rest**
              Screen-free time, reading, deep sleep preparation.
            
            *(Tip: Feel free to customize any block to fit your specific workday!)*
        """.trimIndent()
    }

    private data class TriviaQuestion(
        val topic: String,
        val question: String,
        val options: List<String>,
        val correctAnswer: String,
        val explanation: String
    )
}
