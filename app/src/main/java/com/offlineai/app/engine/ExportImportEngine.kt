package com.offlineai.app.engine

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import com.offlineai.app.data.KnowledgeArticle
import com.offlineai.app.data.Message
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Robust on-device export, backup, and data import engine for Maphula.
 * Handles local device saving of artwork, chat transcripts, and knowledge datasets.
 */
class ExportImportEngine(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    /**
     * Saves generated bitmap to device Pictures directory.
     */
    fun exportBitmapToPictures(bitmap: Bitmap, title: String): File? {
        return try {
            val picturesDir = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "Maphula")
            if (!picturesDir.exists()) picturesDir.mkdirs()

            val fileName = "Maphula_${title.take(15).replace(" ", "_")}_${dateFormat.format(Date())}.png"
            val file = File(picturesDir, fileName)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
            file
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Exports chat messages to a clean Markdown transcript.
     */
    fun exportChatToMarkdown(messages: List<Message>): File? {
        return try {
            val docsDir = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS), "Maphula")
            if (!docsDir.exists()) docsDir.mkdirs()

            val fileName = "Maphula_Chat_${dateFormat.format(Date())}.md"
            val file = File(docsDir, fileName)

            val sb = StringBuilder()
            sb.append("# 🕉️ Maphula Offline AI Conversation Transcript\n")
            sb.append("**Exported at:** ${Date()}\n\n")
            sb.append("---\n\n")

            for (m in messages) {
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(m.timestamp))
                if (m.sender.name == "USER") {
                    sb.append("### 👤 User (${timeStr})\n")
                    sb.append("${m.text}\n\n")
                } else {
                    val personaName = m.persona.displayName
                    sb.append("### 🤖 ${personaName} AI (${timeStr})\n")
                    sb.append("${m.text}\n\n")
                }
            }

            file.writeText(sb.toString())
            file
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Backs up entire knowledge database to a JSON file.
     */
    fun exportKnowledgeToJson(articles: List<KnowledgeArticle>): File? {
        return try {
            val docsDir = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS), "Maphula")
            if (!docsDir.exists()) docsDir.mkdirs()

            val fileName = "Maphula_Knowledge_Backup_${dateFormat.format(Date())}.json"
            val file = File(docsDir, fileName)

            val array = JSONArray()
            for (art in articles) {
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
                array.put(obj)
            }

            file.writeText(array.toString(2))
            file
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Imports custom knowledge articles from JSON text.
     */
    fun importKnowledgeFromJson(jsonText: String): List<KnowledgeArticle> {
        val result = mutableListOf<KnowledgeArticle>()
        try {
            val array = JSONArray(jsonText)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val tagsList = mutableListOf<String>()
                val tagsArr = obj.optJSONArray("tags")
                if (tagsArr != null) {
                    for (j in 0 until tagsArr.length()) tagsList.add(tagsArr.getString(j))
                }

                val takeawaysList = mutableListOf<String>()
                val takeawaysArr = obj.optJSONArray("keyTakeaways")
                if (takeawaysArr != null) {
                    for (j in 0 until takeawaysArr.length()) takeawaysList.add(takeawaysArr.getString(j))
                }

                val article = KnowledgeArticle(
                    id = obj.optString("id", "custom_${System.currentTimeMillis()}_$i"),
                    title = obj.optString("title", "Imported Note"),
                    category = obj.optString("category", "Custom Notes"),
                    tags = tagsList,
                    summary = obj.optString("summary", ""),
                    content = obj.optString("content", ""),
                    keyTakeaways = takeawaysList,
                    isOnlineSynced = false
                )
                result.add(article)
            }
        } catch (_: Exception) {}
        return result
    }

    /**
     * Calculates storage footprint in megabytes/kilobytes.
     */
    fun getStorageFootprintInfo(articlesCount: Int, syncedCount: Int): StorageStats {
        var totalBytes = 0L
        val internalDir = context.filesDir
        internalDir.listFiles()?.forEach { totalBytes += it.length() }

        val cacheDir = context.cacheDir
        var cacheBytes = 0L
        cacheDir.listFiles()?.forEach { cacheBytes += it.length() }

        val picsDir = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "Maphula")
        var picsCount = 0
        if (picsDir.exists()) {
            picsDir.listFiles()?.forEach {
                totalBytes += it.length()
                picsCount++
            }
        }

        return StorageStats(
            totalArticlesCount = articlesCount,
            syncedArticlesCount = syncedCount,
            savedPicturesCount = picsCount,
            databaseSizeBytes = totalBytes,
            cacheSizeBytes = cacheBytes
        )
    }

    /**
     * Clears internal cache.
     */
    fun clearCache(): Boolean {
        return try {
            context.cacheDir.deleteRecursively()
            true
        } catch (_: Exception) {
            false
        }
    }
}

data class StorageStats(
    val totalArticlesCount: Int,
    val syncedArticlesCount: Int,
    val savedPicturesCount: Int,
    val databaseSizeBytes: Long,
    val cacheSizeBytes: Long
)
