package com.offlineai.app.ui.knowledge

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.offlineai.app.R
import com.offlineai.app.data.KnowledgeArticle
import com.offlineai.app.ui.theme.ThemeManager
import com.offlineai.app.ui.motion.animateButteryEntrance
import com.offlineai.app.ui.motion.animateSpringBounce
import com.offlineai.app.ui.motion.applyButteryPress

class KnowledgeAdapter(
    private var items: List<Pair<KnowledgeArticle, Double>>,
    private val onReadClick: (KnowledgeArticle) -> Unit,
    private val onAskAiClick: (KnowledgeArticle) -> Unit
) : RecyclerView.Adapter<KnowledgeAdapter.ViewHolder>() {

    fun updateData(newItems: List<Pair<KnowledgeArticle, Double>>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_knowledge_article, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (article, score) = items[position]
        holder.bind(article, score)

        if (holder.itemView.getTag(R.id.tag_entrance_animated) == null) {
            holder.itemView.setTag(R.id.tag_entrance_animated, true)
            holder.itemView.animateButteryEntrance(position, startDelayMultiplier = 20L, duration = 280L)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategory: TextView = itemView.findViewById(R.id.tvArticleCategory)
        private val tvArticleSynced: TextView = itemView.findViewById(R.id.tvArticleSynced)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvArticleTitle)
        private val tvSummary: TextView = itemView.findViewById(R.id.tvArticleSummary)
        private val tvMatch: TextView = itemView.findViewById(R.id.tvArticleMatch)
        private val btnAskAi: MaterialButton = itemView.findViewById(R.id.btnAskAi)
        private val btnReadMore: MaterialButton = itemView.findViewById(R.id.btnReadMore)

        init {
            itemView.applyButteryPress(pressedScale = 0.97f)
            btnReadMore.applyButteryPress()
            btnAskAi.applyButteryPress()
        }

        fun bind(article: KnowledgeArticle, score: Double) {
            val curTheme = ThemeManager.currentTheme.value
            (itemView as? CardView)?.let { ThemeManager.applyCardStyle(it) }
            tvCategory.text = article.category
            tvTitle.text = article.title
            tvTitle.setTextColor(curTheme.textPrimary)
            tvSummary.text = article.summary
            tvSummary.setTextColor(curTheme.textSecondary)
            tvArticleSynced.visibility = if (article.isOnlineSynced) View.VISIBLE else View.GONE

            btnAskAi.setTextColor(curTheme.accentColor)
            btnReadMore.strokeColor = ColorStateList.valueOf(curTheme.cardStrokeColor)
            btnReadMore.setTextColor(curTheme.textPrimary)

            if (score > 1.05 || (score > 0.1 && score < 0.99)) {
                tvMatch.visibility = View.VISIBLE
                val matchPct = kotlin.math.min(99, (score * 50).toInt())
                tvMatch.text = "🎯 ${matchPct}% Match"
            } else {
                tvMatch.visibility = View.GONE
            }

            btnReadMore.setOnClickListener {
                btnReadMore.animateSpringBounce()
                onReadClick(article)
            }
            btnAskAi.setOnClickListener {
                btnAskAi.animateSpringBounce()
                onAskAiClick(article)
            }
            itemView.setOnClickListener { onReadClick(article) }
        }
    }
}
