package com.hexin.screenlog.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Spinner
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hexin.screenlog.R
import com.hexin.screenlog.api.ScreenLogLevel
import com.hexin.screenlog.api.ScreenLogManager
import com.hexin.screenlog.core.ScreenLogDispatcher
import com.hexin.screenlog.core.ScreenLogFormatter
import com.hexin.screenlog.filter.ScreenLogFilter
import com.hexin.screenlog.filter.ScreenLogFilterMatcher

class ScreenLogPanelView(context: Context) : FrameLayout(context), ScreenLogDispatcher.Listener {

    private val adapter = ScreenLogAdapter(::copyEntry)
    private val list: RecyclerView
    private val search: EditText
    private val levelSpinner: Spinner
    private val tagSpinner: Spinner
    private val pauseButton: Button
    private val copyFilteredButton: Button
    private val clearButton: Button
    private var filter = ScreenLogFilter()
    private var filteredEntries = emptyList<com.hexin.screenlog.core.ScreenLogEntry>()
    private var large = false
    private var refreshPosted = false

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            toggleHeight()
            return true
        }
    })

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, panelHeight(false), Gravity.BOTTOM)
        LayoutInflater.from(context).inflate(R.layout.screen_log_panel_view, this, true)
        list = findViewById(R.id.screenLogList)
        search = findViewById(R.id.screenLogSearch)
        levelSpinner = findViewById(R.id.screenLogLevel)
        tagSpinner = findViewById(R.id.screenLogTag)
        pauseButton = findViewById(R.id.screenLogPause)
        copyFilteredButton = findViewById(R.id.screenLogCopyFiltered)
        clearButton = findViewById(R.id.screenLogClear)

        list.layoutManager = LinearLayoutManager(context)
        list.adapter = adapter
        setupFilters()
        setupActions()
        setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }
        refreshNow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ScreenLogDispatcher.addListener(this)
        onAutoScrollChanged(ScreenLogDispatcher.isAutoScrollPaused())
        refreshNow()
    }

    override fun onDetachedFromWindow() {
        ScreenLogDispatcher.removeListener(this)
        super.onDetachedFromWindow()
    }

    override fun onLogsChanged() {
        if (refreshPosted) return
        refreshPosted = true
        postDelayed({
            refreshPosted = false
            refreshNow()
        }, 180)
    }

    override fun onAutoScrollChanged(paused: Boolean) {
        pauseButton.text = context.getString(if (paused) R.string.screen_log_resume else R.string.screen_log_pause)
    }

    private fun setupFilters() {
        val levels = listOf(context.getString(R.string.screen_log_level_all)) + ScreenLogLevel.values().map { it.name }
        levelSpinner.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, levels)
        levelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filter = filter.copy(level = if (position == 0) null else ScreenLogLevel.valueOf(levels[position]))
                refreshNow()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        tagSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = parent?.getItemAtPosition(position)?.toString().orEmpty()
                filter = filter.copy(tag = selected.takeIf { it != context.getString(R.string.screen_log_tag_all) })
                refreshNow()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter = filter.copy(keyword = s?.toString().orEmpty())
                onLogsChanged()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun setupActions() {
        pauseButton.setOnClickListener {
            if (ScreenLogDispatcher.isAutoScrollPaused()) ScreenLogManager.resumeAutoScroll() else ScreenLogManager.pauseAutoScroll()
        }
        clearButton.setOnClickListener { ScreenLogManager.clearLogs() }
        copyFilteredButton.setOnClickListener {
            copyText(filteredEntries.joinToString(separator = "\n") { ScreenLogFormatter.display(it) })
        }
    }

    private fun refreshNow() {
        val entries = ScreenLogDispatcher.snapshot()
        updateTags(entries.map { it.tag }.distinct().sorted())
        filteredEntries = entries.filter { ScreenLogFilterMatcher.match(it, filter) }
        adapter.submitList(filteredEntries)
        if (!ScreenLogDispatcher.isAutoScrollPaused() && filteredEntries.isNotEmpty()) {
            list.scrollToPosition(filteredEntries.lastIndex)
        }
    }

    private fun updateTags(tags: List<String>) {
        val values = listOf(context.getString(R.string.screen_log_tag_all)) + tags
        val current = tagSpinner.selectedItem?.toString()
        val existing = (tagSpinner.adapter as? ArrayAdapter<*>)?.let { adapter ->
            (0 until adapter.count).map { adapter.getItem(it).toString() }
        }
        if (existing == values) return
        tagSpinner.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, values)
        val index = values.indexOf(current).takeIf { it >= 0 } ?: 0
        tagSpinner.setSelection(index)
    }

    private fun toggleHeight() {
        large = !large
        layoutParams = layoutParams.apply { height = panelHeight(large) }
    }

    private fun panelHeight(large: Boolean): Int {
        val screenHeight = resources.displayMetrics.heightPixels
        return screenHeight * if (large) 2 / 3 else 1 / 3
    }

    private fun copyEntry(entry: com.hexin.screenlog.core.ScreenLogEntry) {
        copyText(ScreenLogFormatter.display(entry))
    }

    private fun copyText(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("screen-log", text))
    }
}
