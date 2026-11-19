package com.offlineai.app.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.offlineai.app.R
import com.offlineai.app.data.Message
import com.offlineai.app.data.SenderType
import com.offlineai.app.ui.theme.ThemeManager
import com.offlineai.app.ui.motion.animateButteryEntrance
import com.offlineai.app.ui.motion.animateSpringBounce
import com.offlineai.app.ui.motion.applyButteryPress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    private val messages: MutableList<Message>,
    private val onArticleClick: ((String) -> Unit)? = null,
    private val onSpeakClick: ((Message) -> Unit)? = null,
    private val onAttachmentClick: ((com.offlineai.app.data.ChatAttachment) -> Unit)? = null,
    private val onPlayGameClick: ((String) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_AI = 2
    }

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun getItemViewType(position: Int): Int {
        return when (messages[position].sender) {
            SenderType.USER -> TYPE_USER
            else -> TYPE_AI
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_USER) {
            val view = inflater.inflate(R.layout.item_message_user, parent, false)
            UserViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_message_ai, parent, false)
            AiViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is UserViewHolder) {
            holder.bind(message)
        } else if (holder is AiViewHolder) {
            holder.bind(message)
        }

        // Apply buttery entrance animation on first appearance
        if (holder.itemView.getTag(R.id.tag_entrance_animated) == null) {
            holder.itemView.setTag(R.id.tag_entrance_animated, true)
            holder.itemView.animateButteryEntrance(position, startDelayMultiplier = 20L, duration = 280L)
        }
    }

    override fun getItemCount(): Int = messages.size

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val layoutUserBubble: View = itemView.findViewById(R.id.layoutUserBubble)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvUserMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvUserTime)
        private val layoutAttachment: View? = itemView.findViewById(R.id.layoutUserAttachment)
        private val tvAttachmentEmoji: TextView? = itemView.findViewById(R.id.tvUserAttachmentEmoji)
        private val tvAttachmentName: TextView? = itemView.findViewById(R.id.tvUserAttachmentName)
        private val tvAttachmentMeta: TextView? = itemView.findViewById(R.id.tvUserAttachmentMeta)

        init {
            layoutUserBubble.applyButteryPress(pressedScale = 0.97f)
            layoutAttachment?.applyButteryPress()
        }

        fun bind(message: Message) {
            layoutUserBubble.background = ThemeManager.chatUserBackground(itemView.context)
            tvMessage.text = message.text
            tvTime.text = timeFormat.format(Date(message.timestamp))

            val att = message.attachment
            if (att != null) {
                layoutAttachment?.visibility = View.VISIBLE
                tvAttachmentEmoji?.text = att.fileType.emoji
                tvAttachmentName?.text = att.fileName
                tvAttachmentMeta?.text = "${att.fileType.label} • ${att.formattedSize()}"
                layoutAttachment?.setOnClickListener {
                    layoutAttachment.animateSpringBounce()
                    onAttachmentClick?.invoke(att)
                }
            } else {
                layoutAttachment?.visibility = View.GONE
                layoutAttachment?.setOnClickListener(null)
            }
        }
    }

    inner class AiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val layoutAiBubble: View = itemView.findViewById(R.id.layoutAiBubble)
        private val tvPersona: TextView = itemView.findViewById(R.id.tvAiPersona)
        private val tvCitation: TextView = itemView.findViewById(R.id.tvRagCitation)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvAiMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvAiTime)
        private val btnCopy: ImageButton = itemView.findViewById(R.id.btnCopyMessage)
        private val btnSpeak: ImageButton = itemView.findViewById(R.id.btnSpeakAiMessage)
        private val btnLaunchGame: View? = itemView.findViewById(R.id.btnLaunchGame)

        init {
            btnCopy.applyButteryPress()
            btnSpeak.applyButteryPress()
            btnLaunchGame?.applyButteryPress()
            tvCitation.applyButteryPress()
        }

        fun bind(message: Message) {
            val curTheme = ThemeManager.currentTheme.value
            layoutAiBubble.background = ThemeManager.chatAiBackground(itemView.context)
            tvMessage.setTextColor(curTheme.textPrimary)
            tvTime.setTextColor(curTheme.textMuted)
            tvPersona.text = message.persona.displayName
            try {
                tvPersona.setTextColor(Color.parseColor(message.persona.tagColorHex))
            } catch (_: Exception) {
                tvPersona.setTextColor(Color.CYAN)
            }

            if (message.relatedArticles.isNotEmpty()) {
                tvCitation.visibility = View.VISIBLE
                val articleTitle = message.relatedArticles.first()
                tvCitation.text = "📖 $articleTitle"
                tvCitation.setOnClickListener {
                    tvCitation.animateSpringBounce()
                    onArticleClick?.invoke(articleTitle)
                }
            } else {
                tvCitation.visibility = View.GONE
            }

            tvMessage.text = formatSimpleMarkdown(message.text)
            tvTime.text = timeFormat.format(Date(message.timestamp))

            // Inspect if this message contains a generated HTML/JS game
            val extractedHtml = extractGameHtml(message.text)
            if (extractedHtml != null) {
                btnLaunchGame?.visibility = View.VISIBLE
                btnLaunchGame?.setOnClickListener {
                    btnLaunchGame.animateSpringBounce()
                    onPlayGameClick?.invoke(extractedHtml)
                }
            } else {
                btnLaunchGame?.visibility = View.GONE
                btnLaunchGame?.setOnClickListener(null)
            }

            btnCopy.setOnClickListener {
                btnCopy.animateSpringBounce()
                val clipboard = itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Maphula Response", message.text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(itemView.context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }

            btnSpeak.setOnClickListener {
                btnSpeak.animateSpringBounce()
                onSpeakClick?.invoke(message)
            }
        }

        private fun extractGameHtml(text: String): String? {
            val htmlFenceRegex = Regex("```(?:html)?\\s*(<!DOCTYPE html>[\\s\\S]*?</html>)\\s*```", RegexOption.IGNORE_CASE)
            val match = htmlFenceRegex.find(text)
            if (match != null) {
                return match.groupValues[1]
            }
            if (text.contains("<canvas") && text.contains("<script>")) {
                val directMatch = Regex("(<!DOCTYPE html>[\\s\\S]*?</html>)", RegexOption.IGNORE_CASE).find(text)
                return directMatch?.groupValues?.get(1)
            }
            return null
        }

        private fun formatSimpleMarkdown(raw: String): CharSequence {
            val ssb = SpannableStringBuilder()
            val lines = raw.split("\n")

            for (i in lines.indices) {
                val line = lines[i]
                if (line.startsWith("### ") || line.startsWith("## ")) {
                    val headerText = line.replace(Regex("^#+\\s*"), "")
                    val start = ssb.length
                    ssb.append(headerText).append("\n")
                    ssb.setSpan(StyleSpan(Typeface.BOLD), start, ssb.length - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.setSpan(ForegroundColorSpan(Color.parseColor("#06B6D4")), start, ssb.length - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                } else if (line.startsWith("- ") || line.startsWith("• ")) {
                    val bulletText = "• " + line.substring(2)
                    ssb.append(formatInlineBold(bulletText)).append("\n")
                } else {
                    ssb.append(formatInlineBold(line))
                    if (i < lines.size - 1) ssb.append("\n")
                }
            }

            return ssb
        }

        private fun formatInlineBold(line: String): CharSequence {
            if (!line.contains("**")) return line
            val ssb = SpannableStringBuilder()
            val parts = line.split("**")
            for (j in parts.indices) {
                val start = ssb.length
                ssb.append(parts[j])
                if (j % 2 == 1) { // inside **
                    ssb.setSpan(StyleSpan(Typeface.BOLD), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            return ssb
        }
    }
}
