package com.offlineai.app.data

data class KnowledgeArticle(
    val id: String,
    val title: String,
    val category: String,
    val tags: List<String>,
    val summary: String,
    val content: String,
    val keyTakeaways: List<String> = emptyList(),
    val isOnlineSynced: Boolean = false,
    val sourceUrl: String? = null,
    val syncedTimestamp: Long = 0L
)
