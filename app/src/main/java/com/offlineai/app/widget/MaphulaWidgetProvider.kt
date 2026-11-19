package com.offlineai.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import com.offlineai.app.MainActivity
import com.offlineai.app.R
import com.offlineai.app.engine.CelebrationEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Mapula Offline AI Home Screen Widget Provider.
 * Provides on-home-screen offline AI status, daily inspiration & prompt suggestions,
 * and 1-tap quick actions for Ask AI, Voice dictation, Creative Studio, and cycling tips.
 */
class MaphulaWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_CYCLE_WISDOM = "com.offlineai.app.widget.ACTION_CYCLE_WISDOM"
        const val ACTION_CYCLE_TIP = "com.offlineai.app.widget.ACTION_CYCLE_TIP"
        const val ACTION_OPEN_MAIN = "com.offlineai.app.widget.ACTION_OPEN_MAIN"
        const val ACTION_OPEN_CHAT = "com.offlineai.app.widget.ACTION_OPEN_CHAT"
        const val ACTION_OPEN_VOICE = "com.offlineai.app.widget.ACTION_OPEN_VOICE"
        const val ACTION_OPEN_CREATIVE = "com.offlineai.app.widget.ACTION_OPEN_CREATIVE"
        const val ACTION_OPEN_PREFILL = "com.offlineai.app.widget.ACTION_OPEN_PREFILL"

        const val EXTRA_TARGET_TAB = "target_tab"
        const val EXTRA_OPEN_VOICE = "open_voice"
        const val EXTRA_PREFILL_PROMPT = "prefill_prompt"

        private const val PREFS_NAME = "maphula_widget_prefs"
        private const val PREF_INDEX = "wisdom_verse_index"

        data class WidgetPromptIdea(
            val category: String,
            val headline: String,
            val prompt: String,
            val badge: String
        )

        val AI_PROMPT_IDEAS = listOf(
            WidgetPromptIdea(
                category = "💡 Quick Prompt Idea",
                headline = "Explain Complex Concepts Simply",
                prompt = "Explain quantum computing and superposition like I am 10 years old.",
                badge = "Chat"
            ),
            WidgetPromptIdea(
                category = "⚡ Offline Python Studio",
                headline = "Generate Clean Code",
                prompt = "Write a Python script to parse a JSON file and count word occurrences.",
                badge = "Code"
            ),
            WidgetPromptIdea(
                category = "🎨 Creative Generative Studio",
                headline = "Offline Latent Diffusion",
                prompt = "A futuristic cyberpunk city with neon reflections in rainy puddles.",
                badge = "Art"
            ),
            WidgetPromptIdea(
                category = "🌐 Multilingual Offline AI",
                headline = "Regional Language Support",
                prompt = "नमस्ते! आप आज मेरी किस प्रकार सहायता कर सकते हैं?",
                badge = "Hindi"
            ),
            WidgetPromptIdea(
                category = "🎙️ Voice AI Assistant",
                headline = "Hands-free Dictation",
                prompt = "What are the three most fundamental laws of thermodynamics?",
                badge = "Voice"
            ),
            WidgetPromptIdea(
                category = "📚 Curated Knowledge Library",
                headline = "Emergency & Survival Guides",
                prompt = "Summarize the key first-aid steps for treating hypothermia.",
                badge = "Survival"
            ),
            WidgetPromptIdea(
                category = "🧠 Mindful Problem Solving",
                headline = "Focus & Productivity Strategy",
                prompt = "How can I structure my day to stay productive and avoid burnout?",
                badge = "Productivity"
            ),
            WidgetPromptIdea(
                category = "✨ Classical Philosophy",
                headline = "Action Without Attachment",
                prompt = "Focus on the quality of your craft; results follow dedication and perseverance.",
                badge = "Wisdom"
            ),
            WidgetPromptIdea(
                category = "🔬 Science & Astronomy",
                headline = "Offline Exploration",
                prompt = "Why is the sky blue, and how does Rayleigh scattering work in our atmosphere?",
                badge = "Science"
            ),
            WidgetPromptIdea(
                category = "🔒 100% Private & Sovereign",
                headline = "Zero Cloud Tracking",
                prompt = "All AI models, memory, and chats run 100% on-device offline.",
                badge = "Privacy"
            )
        )

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val celebrationEngine = CelebrationEngine()
            val celebration = celebrationEngine.getTodayCelebration()

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val tipIndex = (prefs.getInt(PREF_INDEX, 0) % AI_PROMPT_IDEAS.size).coerceAtLeast(0)
            val idea = AI_PROMPT_IDEAS[tipIndex]

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val formattedTime = timeFormat.format(Date())

            val views = RemoteViews(context.packageName, R.layout.widget_maphula)

            // Header & Status text
            views.setTextViewText(R.id.tvWidgetChrono, "⚡ $formattedTime")
            views.setTextViewText(R.id.tvWidgetCelebration, "✨ ${idea.badge.uppercase()} • ${celebration.title}")
            views.setTextViewText(R.id.tvWidgetScriptureOriginal, "${idea.category}: ${idea.headline}")
            views.setTextViewText(R.id.tvWidgetScriptureEnglish, "\"${idea.prompt}\"")

            // Base request code stride to guarantee unique request codes per widget
            val baseCode = (appWidgetId * 16).coerceAtLeast(0)

            // 1. Header Click -> Opens MainActivity
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_OPEN_MAIN
                data = Uri.parse("mapula-widget://$appWidgetId/main")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val mainPendingIntent = PendingIntent.getActivity(
                context,
                baseCode + 1,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.layoutWidgetHeader, mainPendingIntent)
            views.setOnClickPendingIntent(R.id.tvWidgetTitle, mainPendingIntent)
            views.setOnClickPendingIntent(R.id.tvWidgetChrono, mainPendingIntent)

            // 2. Click on Prompt Card -> Opens Chat with this prompt pre-filled
            val prefillIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_OPEN_PREFILL
                data = Uri.parse("mapula-widget://$appWidgetId/prefill")
                putExtra(EXTRA_TARGET_TAB, "chat")
                putExtra(EXTRA_PREFILL_PROMPT, idea.prompt)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val prefillPendingIntent = PendingIntent.getActivity(
                context,
                baseCode + 2,
                prefillIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.layoutWidgetPromptCard, prefillPendingIntent)
            views.setOnClickPendingIntent(R.id.tvWidgetCelebration, prefillPendingIntent)
            views.setOnClickPendingIntent(R.id.tvWidgetScriptureOriginal, prefillPendingIntent)
            views.setOnClickPendingIntent(R.id.tvWidgetScriptureEnglish, prefillPendingIntent)

            // 3. Quick Action: "Ask AI / Chat" -> Opens Chat
            val chatIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_OPEN_CHAT
                data = Uri.parse("mapula-widget://$appWidgetId/chat")
                putExtra(EXTRA_TARGET_TAB, "chat")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val chatPendingIntent = PendingIntent.getActivity(
                context,
                baseCode + 3,
                chatIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btnWidgetAskAi, chatPendingIntent)

            // 4. Quick Action: "Voice" -> Opens Chat & Starts Listening
            val voiceIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_OPEN_VOICE
                data = Uri.parse("mapula-widget://$appWidgetId/voice")
                putExtra(EXTRA_TARGET_TAB, "chat")
                putExtra(EXTRA_OPEN_VOICE, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val voicePendingIntent = PendingIntent.getActivity(
                context,
                baseCode + 4,
                voiceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btnWidgetVoice, voicePendingIntent)

            // 5. Quick Action: "Creative / Studio" -> Opens Creative Studio
            val creativeIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_OPEN_CREATIVE
                data = Uri.parse("mapula-widget://$appWidgetId/creative")
                putExtra(EXTRA_TARGET_TAB, "creative")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val creativePendingIntent = PendingIntent.getActivity(
                context,
                baseCode + 5,
                creativeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btnWidgetCreative, creativePendingIntent)

            // 6. Action: "Next Idea" -> Cycles tip without opening app
            val cycleIntent = Intent(context, MaphulaWidgetProvider::class.java).apply {
                action = ACTION_CYCLE_WISDOM
                data = Uri.parse("mapula-widget://$appWidgetId/cycle")
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val cyclePendingIntent = PendingIntent.getBroadcast(
                context,
                baseCode + 6,
                cycleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btnWidgetNextWisdom, cyclePendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, MaphulaWidgetProvider::class.java)
        try {
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                for (id in appWidgetIds) {
                    updateWidget(context, appWidgetManager, id)
                }
            }
        } catch (_: Exception) {}
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_CYCLE_WISDOM || intent.action == ACTION_CYCLE_TIP) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentIdx = prefs.getInt(PREF_INDEX, 0)
            prefs.edit().putInt(PREF_INDEX, (currentIdx + 1) % AI_PROMPT_IDEAS.size).commit()

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MaphulaWidgetProvider::class.java)
            val targetIds = mutableSetOf<Int>()
            try {
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds != null) {
                    for (id in appWidgetIds) targetIds.add(id)
                }
            } catch (_: Exception) {}
            val singleId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (singleId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                targetIds.add(singleId)
            }
            for (widgetId in targetIds) {
                updateWidget(context, appWidgetManager, widgetId)
            }
        }
    }
}
