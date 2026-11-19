package com.offlineai.app.ui.knowledge

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.offlineai.app.MainActivity
import com.offlineai.app.R
import com.offlineai.app.data.KnowledgeArticle
import com.offlineai.app.databinding.FragmentKnowledgeBinding
import com.offlineai.app.ui.theme.ResolvedTheme
import com.offlineai.app.ui.theme.ThemeManager
import com.offlineai.app.ui.motion.animateSpringBounce
import com.offlineai.app.ui.motion.applyButteryPress
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class KnowledgeFragment : Fragment() {

    private var _binding: FragmentKnowledgeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: KnowledgeAdapter
    private var selectedCategory: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKnowledgeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearch()
        setupCategoryChips()
        setupActionButtons()
        observeNetworkStatus()
        performSearch()
        applyTheme(ThemeManager.currentTheme.value)
    }

    override fun onResume() {
        super.onResume()
        applyTheme(ThemeManager.currentTheme.value)
    }

    fun applyTheme(theme: ResolvedTheme) {
        val ctx = context ?: return
        if (_binding == null) return

        binding.root.setBackgroundColor(theme.bgColor)
        binding.rvKnowledge.setBackgroundColor(theme.bgColor)
        binding.etSearchKnowledge.background = ThemeManager.inputBackground(ctx, theme)
        binding.etSearchKnowledge.setTextColor(theme.textPrimary)
        binding.etSearchKnowledge.setHintTextColor(theme.textMuted)

        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }
    }

    private fun setupRecyclerView() {
        adapter = KnowledgeAdapter(
            emptyList(),
            onReadClick = { article -> showArticleModal(article) },
            onAskAiClick = { article ->
                (activity as? MainActivity)?.askAiFromKnowledge(article.title)
            }
        )
        binding.rvKnowledge.layoutManager = LinearLayoutManager(requireContext())
        binding.rvKnowledge.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearchKnowledge.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                performSearch()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupCategoryChips() {
        binding.catAll.applyButteryPress()
        binding.catSynced.applyButteryPress()
        binding.catPersonal.applyButteryPress()
        binding.catEmergency.applyButteryPress()
        binding.catCS.applyButteryPress()
        binding.catScience.applyButteryPress()
        binding.catPhilosophy.applyButteryPress()
        binding.catScriptures.applyButteryPress()

        binding.chipGroupCategories.setOnCheckedChangeListener { _, checkedId ->
            selectedCategory = when (checkedId) {
                R.id.catSynced -> "ONLINE_SYNCED_FILTER"
                R.id.catPersonal -> "Personal Notes"
                R.id.catEmergency -> "Emergency & Survival"
                R.id.catCS -> "Computer Science"
                R.id.catScience -> "Science & Physics"
                R.id.catPhilosophy -> "Philosophy & Logic"
                R.id.catScriptures -> "Sacred Scriptures & Philosophy"
                else -> null
            }
            performSearch()
        }
    }

    private fun setupActionButtons() {
        binding.btnSyncOnline.applyButteryPress()
        binding.btnSyncOnline.setOnClickListener {
            binding.btnSyncOnline.animateSpringBounce()
            showSyncDialog()
        }

        binding.btnImportDoc.applyButteryPress()
        binding.btnImportDoc.setOnClickListener {
            binding.btnImportDoc.animateSpringBounce()
            showImportDocDialog()
        }

        binding.btnMindMap.applyButteryPress()
        binding.btnMindMap.setOnClickListener {
            binding.btnMindMap.animateSpringBounce()
            showMindMapDialog()
        }
    }

    private fun observeNetworkStatus() {
        val monitor = (activity as? MainActivity)?.networkMonitor ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            monitor.isOnlineFlow.collectLatest { isOnline ->
                updateNetworkStatusUI(isOnline)
            }
        }
    }

    private fun updateNetworkStatusUI(isOnline: Boolean) {
        if (_binding == null) return
        val count = (activity as? MainActivity)?.knowledgeEngine?.getSyncedCount() ?: 0
        if (isOnline) {
            binding.vKnowledgeDot.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.secondary)
            binding.tvKnowledgeSyncStatus.text = "Online: Ready to Sync ($count saved)"
            binding.tvKnowledgeSyncStatus.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.secondary)
            )
        } else {
            binding.vKnowledgeDot.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.accent_green)
            binding.tvKnowledgeSyncStatus.text = "Offline Mode: Local Database ($count synced)"
            binding.tvKnowledgeSyncStatus.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.text_secondary)
            )
        }
    }

    fun performSearch(queryText: String? = null) {
        val engine = (activity as? MainActivity)?.knowledgeEngine ?: return
        val query = queryText ?: binding.etSearchKnowledge.text.toString().trim()

        if (selectedCategory == "ONLINE_SYNCED_FILTER") {
            val synced = engine.getSyncedArticles()
            val filtered = if (query.isBlank()) {
                synced.map { Pair(it, 1.0) }
            } else {
                engine.search(query, topK = 15).filter { it.first.isOnlineSynced }
            }
            adapter.updateData(filtered)
        } else {
            val results = engine.search(query, topK = 15, categoryFilter = selectedCategory)
            adapter.updateData(results)
        }
    }

    fun setSearchQuery(query: String) {
        binding.etSearchKnowledge.setText(query)
        performSearch(query)
    }

    private fun showSyncDialog() {
        val mainActivity = activity as? MainActivity ?: return
        val onlineEngine = mainActivity.onlineSyncEngine
        val isOnline = mainActivity.networkMonitor.isOnline()

        val dialog = BottomSheetDialog(requireContext())
        com.offlineai.app.ui.theme.ThemeManager.setupSmoothBottomSheet(dialog)
        val view = layoutInflater.inflate(R.layout.dialog_sync_knowledge, null)
        dialog.setContentView(view)

        val vDialogDot = view.findViewById<View>(R.id.vDialogDot)
        val tvDialogStatus = view.findViewById<TextView>(R.id.tvDialogStatus)
        val btnAutoSwitch = view.findViewById<MaterialButton>(R.id.btnToggleAutoSwitch)
        val seekNetwork = view.findViewById<SeekBar>(R.id.seekNetworkMode)
        val tvSliderCurrentMode = view.findViewById<TextView>(R.id.tvSliderCurrentMode)

        val etOnlineTopic = view.findViewById<EditText>(R.id.etOnlineTopic)
        val btnDownloadTopic = view.findViewById<MaterialButton>(R.id.btnDownloadTopic)
        val btnSyncCurated = view.findViewById<MaterialButton>(R.id.btnSyncCurated)
        val pbSync = view.findViewById<ProgressBar>(R.id.pbSync)
        val tvSyncFeedback = view.findViewById<TextView>(R.id.tvSyncFeedback)

        fun refreshSyncDialogUI() {
            val isCurrentOnline = mainActivity.networkMonitor.isOnline()
            val isAuto = mainActivity.networkMonitor.isAutoSwitchEnabled

            btnAutoSwitch?.text = if (isAuto) "🤖 Auto Switch: ON" else "✋ Auto Switch: OFF"
            seekNetwork?.isEnabled = !isAuto
            seekNetwork?.progress = if (isCurrentOnline) 1 else 0

            if (isCurrentOnline) {
                vDialogDot?.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.secondary)
                tvDialogStatus?.text = "✓ Online Mode: Ready to retrieve and cache new knowledge"
                tvDialogStatus?.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary))
            } else {
                vDialogDot?.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.accent_green)
                tvDialogStatus?.text = "⚠️ Offline Mode: Internet is required to fetch new information"
                tvDialogStatus?.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_green))
            }

            tvSliderCurrentMode?.text = when {
                isAuto && isCurrentOnline -> "Current Mode: Auto-Switch (🟢 Hardware Internet Connected)"
                isAuto && !isCurrentOnline -> "Current Mode: Auto-Switch (⚪ No Hardware Network, 100% Local)"
                !isAuto && isCurrentOnline -> "Current Mode: Manual Override (🟢 Online Retrieval Enabled)"
                else -> "Current Mode: Manual Override (⚪ Strict Offline Air-Gap Forced)"
            }
        }

        refreshSyncDialogUI()

        btnAutoSwitch?.setOnClickListener {
            btnAutoSwitch.animateSpringBounce()
            mainActivity.networkMonitor.setAutoSwitch(!mainActivity.networkMonitor.isAutoSwitchEnabled)
            refreshSyncDialogUI()
        }

        seekNetwork?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val forceOnline = progress == 1
                    mainActivity.networkMonitor.setManualMode(forceOnline)
                    refreshSyncDialogUI()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnDownloadTopic.setOnClickListener {
            val topic = etOnlineTopic.text.toString().trim()
            if (topic.isBlank()) {
                etOnlineTopic.error = "Enter a topic to search online"
                return@setOnClickListener
            }

            if (!onlineEngine.isInternetAvailable()) {
                tvSyncFeedback.visibility = View.VISIBLE
                tvSyncFeedback.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_rose))
                tvSyncFeedback.text = "⚠️ Cannot fetch '$topic': No internet connection available."
                return@setOnClickListener
            }

            pbSync.visibility = View.VISIBLE
            btnDownloadTopic.isEnabled = false
            btnSyncCurated.isEnabled = false
            tvSyncFeedback.visibility = View.VISIBLE
            tvSyncFeedback.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary))
            tvSyncFeedback.text = "Fetching '$topic' from web..."

            lifecycleScope.launch {
                val result = onlineEngine.searchAndFetchArticle(topic)
                pbSync.visibility = View.GONE
                btnDownloadTopic.isEnabled = true
                btnSyncCurated.isEnabled = true

                if (result.isSuccess) {
                    val article = result.getOrNull()!!
                    tvSyncFeedback.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_green))
                    tvSyncFeedback.text = "✓ Successfully saved '${article.title}' to offline storage!"
                    performSearch()
                    Toast.makeText(requireContext(), "Saved '${article.title}' for offline use", Toast.LENGTH_SHORT).show()
                } else {
                    tvSyncFeedback.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_rose))
                    tvSyncFeedback.text = "Error: ${result.exceptionOrNull()?.message}"
                }
            }
        }

        btnSyncCurated.setOnClickListener {
            if (!onlineEngine.isInternetAvailable()) {
                tvSyncFeedback.visibility = View.VISIBLE
                tvSyncFeedback.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_rose))
                tvSyncFeedback.text = "⚠️ Internet is required to sync trending knowledge packs."
                return@setOnClickListener
            }

            pbSync.visibility = View.VISIBLE
            btnDownloadTopic.isEnabled = false
            btnSyncCurated.isEnabled = false
            tvSyncFeedback.visibility = View.VISIBLE
            tvSyncFeedback.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary))
            tvSyncFeedback.text = "Syncing curated knowledge packs from web..."

            lifecycleScope.launch {
                val result = onlineEngine.syncCuratedPacks()
                pbSync.visibility = View.GONE
                btnDownloadTopic.isEnabled = true
                btnSyncCurated.isEnabled = true

                if (result.isSuccess) {
                    val list = result.getOrNull() ?: emptyList()
                    tvSyncFeedback.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_green))
                    tvSyncFeedback.text = "✓ Synced ${list.size} knowledge packs into offline memory!"
                    performSearch()
                    Toast.makeText(requireContext(), "Synced ${list.size} articles to offline memory", Toast.LENGTH_SHORT).show()
                } else {
                    tvSyncFeedback.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_rose))
                    tvSyncFeedback.text = "Error: ${result.exceptionOrNull()?.message}"
                }
            }
        }

        dialog.show()
    }

    private fun showArticleModal(article: KnowledgeArticle) {
        val dialog = BottomSheetDialog(requireContext())
        com.offlineai.app.ui.theme.ThemeManager.setupSmoothBottomSheet(dialog)
        val view = layoutInflater.inflate(R.layout.dialog_article_detail, null)
        dialog.setContentView(view)

        val tvCategory = view.findViewById<TextView>(R.id.tvModalCategory)
        val tvTitle = view.findViewById<TextView>(R.id.tvModalTitle)
        val tvContent = view.findViewById<TextView>(R.id.tvModalContent)
        val tvTakeaways = view.findViewById<TextView>(R.id.tvModalTakeaways)
        val btnAskAi = view.findViewById<MaterialButton>(R.id.btnModalAskAi)

        val categoryText = if (article.isOnlineSynced) {
            "${article.category}  •  🌐 ONLINE SYNCED"
        } else {
            article.category
        }

        tvCategory.text = categoryText
        tvTitle.text = article.title
        tvContent.text = article.content

        if (article.keyTakeaways.isNotEmpty()) {
            tvTakeaways.text = article.keyTakeaways.joinToString("\n") { "• $it" }
        } else {
            tvTakeaways.visibility = View.GONE
            view.findViewById<View>(R.id.tvModalTakeawaysHeader).visibility = View.GONE
        }

        btnAskAi.setOnClickListener {
            dialog.dismiss()
            (activity as? MainActivity)?.askAiFromKnowledge(article.title)
        }

        dialog.show()
    }

    private fun showImportDocDialog() {
        val mainActivity = activity as? MainActivity ?: return
        val engine = mainActivity.knowledgeEngine

        val dialog = BottomSheetDialog(requireContext())
        com.offlineai.app.ui.theme.ThemeManager.setupSmoothBottomSheet(dialog)
        val view = layoutInflater.inflate(R.layout.dialog_import_document, null)
        dialog.setContentView(view)

        val etTitle = view.findViewById<EditText>(R.id.etDocTitle)
        val etCategory = view.findViewById<EditText>(R.id.etDocCategory)
        val etTags = view.findViewById<EditText>(R.id.etDocTags)
        val etContent = view.findViewById<EditText>(R.id.etDocContent)
        val btnConfirm = view.findViewById<android.widget.Button>(R.id.btnConfirmIngest)

        btnConfirm.applyButteryPress()
        btnConfirm.setOnClickListener {
            btnConfirm.animateSpringBounce()
            val title = etTitle.text.toString().trim()
            val category = etCategory.text.toString().trim().ifEmpty { "Personal Notes" }
            val tags = etTags.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val content = etContent.text.toString().trim()

            if (content.isEmpty()) {
                Toast.makeText(requireContext(), "Please provide document content.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val ingested = engine.ingestPersonalDocument(
                title = title,
                content = content,
                category = category,
                tags = tags
            )

            Toast.makeText(requireContext(), "✓ Ingested '${ingested.title}' into Vector RAG!", Toast.LENGTH_SHORT).show()
            performSearch()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showMindMapDialog() {
        val mainActivity = activity as? MainActivity ?: return
        val engine = mainActivity.knowledgeEngine

        val dialog = BottomSheetDialog(requireContext())
        com.offlineai.app.ui.theme.ThemeManager.setupSmoothBottomSheet(dialog)
        val view = layoutInflater.inflate(R.layout.dialog_concept_mindmap, null)
        dialog.setContentView(view)

        val btnClose = view.findViewById<android.widget.ImageButton>(R.id.btnCloseMindMap)
        val container = view.findViewById<android.widget.LinearLayout>(R.id.layoutClustersContainer)

        btnClose.applyButteryPress()
        btnClose.setOnClickListener { dialog.dismiss() }

        val clusters = engine.generateConceptGraph()
        container.removeAllViews()

        for (cluster in clusters) {
            val card = layoutInflater.inflate(R.layout.item_concept_cluster, container, false)
            val tvCategory = card.findViewById<TextView>(R.id.tvClusterCategory)
            val tvCount = card.findViewById<TextView>(R.id.tvClusterCount)
            val tvCentralTopic = card.findViewById<TextView>(R.id.tvCentralTopic)
            val layoutNodes = card.findViewById<android.widget.LinearLayout>(R.id.layoutNodesContainer)
            val tvRelated = card.findViewById<TextView>(R.id.tvRelatedDomains)

            tvCategory.text = cluster.category
            tvCount.text = "${cluster.articleCount} Articles"
            tvCentralTopic.text = "Central Hub: ${cluster.centralTopic}"
            tvRelated.text = if (cluster.relatedCategories.isNotEmpty()) {
                "🔗 Interconnected Domains: " + cluster.relatedCategories.joinToString(", ")
            } else {
                "🔗 Sovereign Local Domain Hub"
            }

            layoutNodes.removeAllViews()
            val hsv = android.widget.HorizontalScrollView(requireContext())
            hsv.isHorizontalScrollBarEnabled = false
            val nodesRow = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
            }

            for (node in cluster.nodes) {
                val chip = TextView(requireContext()).apply {
                    text = "• $node"
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    textSize = 10f
                    background = ContextCompat.getDrawable(context, R.drawable.bg_chip)
                    setPadding(20, 10, 20, 10)
                    val lp = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginEnd = 14
                    }
                    layoutParams = lp
                    applyButteryPress()
                    setOnClickListener {
                        dialog.dismiss()
                        setSearchQuery(node)
                    }
                }
                nodesRow.addView(chip)
            }
            hsv.addView(nodesRow)
            layoutNodes.addView(hsv)

            container.addView(card)
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
