package com.offlineai.app.data

enum class AttachmentType(val label: String, val emoji: String) {
    PICTURE("Picture / Image", "🖼️"),
    AUDIO("Audio / MP3", "🎵"),
    VIDEO("Video / MP4", "🎬"),
    DOCUMENT("Document / PDF / DOC", "📄")
}

data class ChatAttachment(
    val fileName: String,
    val mimeType: String,
    val fileSizeBytes: Long = 0,
    val fileType: AttachmentType,
    val uriString: String? = null,
    val summary: String = ""
) {
    fun formattedSize(): String {
        return when {
            fileSizeBytes <= 0 -> ""
            fileSizeBytes < 1024 -> "$fileSizeBytes B"
            fileSizeBytes < 1024 * 1024 -> "${fileSizeBytes / 1024} KB"
            else -> String.format("%.1f MB", fileSizeBytes.toDouble() / (1024 * 1024))
        }
    }

    companion object {
        fun fromFileName(name: String, sizeBytes: Long = 0, uri: String? = null): ChatAttachment {
            val lower = name.lowercase()
            val (type, mime) = when {
                lower.endsWith(".mp3") -> Pair(AttachmentType.AUDIO, "audio/mpeg")
                lower.endsWith(".wav") -> Pair(AttachmentType.AUDIO, "audio/wav")
                lower.endsWith(".aac") || lower.endsWith(".m4a") -> Pair(AttachmentType.AUDIO, "audio/aac")
                lower.endsWith(".flac") -> Pair(AttachmentType.AUDIO, "audio/flac")
                lower.endsWith(".ogg") -> Pair(AttachmentType.AUDIO, "audio/ogg")
                lower.endsWith(".mp4") -> Pair(AttachmentType.VIDEO, "video/mp4")
                lower.endsWith(".mkv") -> Pair(AttachmentType.VIDEO, "video/x-matroska")
                lower.endsWith(".3gp") || lower.endsWith(".webm") -> Pair(AttachmentType.VIDEO, "video/webm")
                lower.endsWith(".pdf") -> Pair(AttachmentType.DOCUMENT, "application/pdf")
                lower.endsWith(".doc") -> Pair(AttachmentType.DOCUMENT, "application/msword")
                lower.endsWith(".docx") -> Pair(AttachmentType.DOCUMENT, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".csv") || lower.endsWith(".json") -> Pair(AttachmentType.DOCUMENT, "text/plain")
                lower.endsWith(".png") -> Pair(AttachmentType.PICTURE, "image/png")
                lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> Pair(AttachmentType.PICTURE, "image/jpeg")
                lower.endsWith(".webp") -> Pair(AttachmentType.PICTURE, "image/webp")
                lower.endsWith(".gif") -> Pair(AttachmentType.PICTURE, "image/gif")
                else -> Pair(AttachmentType.DOCUMENT, "application/octet-stream")
            }

            return ChatAttachment(
                fileName = name,
                mimeType = mime,
                fileSizeBytes = sizeBytes,
                fileType = type,
                uriString = uri
            )
        }
    }
}
