package com.hexin.screenlog.ui

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hexin.screenlog.R
import com.hexin.screenlog.api.ScreenLogLevel
import com.hexin.screenlog.api.ScreenLogManager
import com.hexin.screenlog.api.ScreenLogTouchMode
import com.hexin.screenlog.core.ScreenLogDispatcher
import com.hexin.screenlog.core.ScreenLogFormatter
import com.hexin.screenlog.databinding.ScreenLogPanelViewBinding
import com.hexin.screenlog.filter.ScreenLogFilter
import com.hexin.screenlog.filter.ScreenLogFilterMatcher

internal class ScreenLogPanelView(
    context: Context,
    private val state: ScreenLogPanelState
) : FrameLayout(context), ScreenLogDispatcher.Listener {

    private companion object {
        const val TAG = "ScreenLogPanelView"
    }

    private val adapter = ScreenLogAdapter(::onEntryClick, ::onEntryLongClick)
    private val binding = ScreenLogPanelViewBinding.inflate(LayoutInflater.from(context), this, true)
    private var filter = state.filter
    private var filteredEntries = emptyList<com.hexin.screenlog.core.ScreenLogEntry>()
    private var large = state.large
    private var refreshPosted = false
    private var touchMode = ScreenLogManager.getTouchMode()
    private var suppressFilterCallbacks = false
    private var suppressStateCallbacks = false
    private var restoreScrollPending = state.listPosition >= 0
    private var selectionMode = false
    private var autoScrollWasPaused = false

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onDoubleTap(e: MotionEvent): Boolean {
            Log.d(TAG, "onDoubleTap x=${e.x}, y=${e.y}, largeBefore=$large, touchMode=$touchMode")
            toggleHeight()
            return true
        }
    })

    init {
        isFocusableInTouchMode = true
        setOnKeyListener { _, keyCode, event ->
            if (selectionMode && keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                exitSelectionMode()
                true
            } else {
                false
            }
        }
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, panelHeight(large), Gravity.BOTTOM)
        binding.screenLogList.layoutManager = LinearLayoutManager(context)
        binding.screenLogList.adapter = adapter
        trace("init, defaultHeight=${panelHeight(false)}, root=${binding.root.javaClass.simpleName}")
        binding.root.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_UP) {
                trace("binding.root touch, action=${event.actionName()}, x=${event.x}, y=${event.y}")
            }
            false
        }
        binding.screenLogList.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, event: MotionEvent): Boolean {
                if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_UP) {
                    trace("screenLogList touch, action=${event.actionName()}, x=${event.x}, y=${event.y}")
                }
                return false
            }
        })
        binding.screenLogList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                saveListPosition()
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                saveListPosition()
            }
        })
        setupFilters()
        setupActions()
        restoreFilterControls()
        refreshNow()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        return if (touchMode == ScreenLogTouchMode.UNDERLYING_PAGE) {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                trace("dispatchTouchEvent pass through, action=${event.actionName()}, x=${event.x}, y=${event.y}")
            }
            false
        } else {
            if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_UP) {
                trace("dispatchTouchEvent handle, action=${event.actionName()}, x=${event.x}, y=${event.y}")
            }
            val handledByGesture = gestureDetector.onTouchEvent(event)
            val handledByChildren = super.dispatchTouchEvent(event)
            if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_UP) {
                trace("dispatchTouchEvent result, action=${event.actionName()}, gesture=$handledByGesture, children=$handledByChildren")
            }
            handledByChildren || handledByGesture
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_UP) {
            trace("onInterceptTouchEvent action=${event.actionName()}, x=${event.x}, y=${event.y}")
        }
        return super.onInterceptTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_UP) {
            trace("onTouchEvent action=${event.actionName()}, x=${event.x}, y=${event.y}")
        }
        return super.onTouchEvent(event)
    }

    fun setTouchMode(mode: ScreenLogTouchMode) {
        trace("setTouchMode from=$touchMode to=$mode")
        touchMode = mode
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        trace("onAttachedToWindow width=$width, height=$height, layoutHeight=${layoutParams?.height}, parent=${parent?.javaClass?.simpleName}")
        ScreenLogDispatcher.addListener(this)
        onAutoScrollChanged(ScreenLogDispatcher.isAutoScrollPaused())
        onRecordingChanged(ScreenLogManager.isRecordingEnabled())
        refreshNow()
    }

    override fun onDetachedFromWindow() {
        exitSelectionMode()
        saveListPosition()
        trace("onDetachedFromWindow")
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
        suppressStateCallbacks = true
        binding.screenLogAutoScroll.isChecked = !paused
        suppressStateCallbacks = false
    }

    override fun onRecordingChanged(enabled: Boolean) {
        suppressStateCallbacks = true
        binding.screenLogRecording.isChecked = enabled
        suppressStateCallbacks = false
    }

    private fun setupFilters() {
        val levels = listOf(context.getString(R.string.screen_log_level_all)) + ScreenLogLevel.values().map { it.name }
        binding.screenLogLevel.adapter = spinnerAdapter(levels)
        binding.screenLogLevel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (suppressFilterCallbacks) return
                filter = filter.copy(level = if (position == 0) null else ScreenLogLevel.valueOf(levels[position]))
                state.filter = filter
                refreshNow()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        binding.screenLogTag.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (suppressFilterCallbacks) return
                val selected = parent?.getItemAtPosition(position)?.toString().orEmpty()
                filter = filter.copy(tag = selected.takeIf { it != context.getString(R.string.screen_log_tag_all) })
                state.filter = filter
                refreshNow()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        binding.screenLogSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (suppressFilterCallbacks) return
                filter = filter.copy(keyword = s?.toString().orEmpty())
                state.filter = filter
                onLogsChanged()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun setupActions() {
        binding.screenLogRecording.setOnCheckedChangeListener { _, checked ->
            if (!suppressStateCallbacks) ScreenLogManager.setRecordingEnabled(checked)
        }
        binding.screenLogAutoScroll.setOnCheckedChangeListener { _, checked ->
            if (suppressStateCallbacks) return@setOnCheckedChangeListener
            if (checked) ScreenLogManager.resumeAutoScroll() else ScreenLogManager.pauseAutoScroll()
        }
        binding.screenLogClear.setOnClickListener { confirmClearLogs() }
        binding.screenLogCopyFiltered.setOnClickListener { copyFilteredEntries() }
        binding.screenLogSettings.setOnClickListener { ScreenLogManager.openSettings() }
        binding.screenLogSelectionClose.setOnClickListener { exitSelectionMode() }
        binding.screenLogSelectionAll.setOnClickListener { toggleSelectAll() }
        binding.screenLogSelectionCopy.setOnClickListener { copySelectedEntries() }
    }

    private fun refreshNow() {
        val entries = ScreenLogDispatcher.snapshot()
        updateTags(entries.map { it.tag }.distinct().sorted())
        filteredEntries = ScreenLogFilterMatcher.filter(entries, filter)
        adapter.submitList(filteredEntries) {
            if (selectionMode && selectedEntries().isEmpty()) {
                exitSelectionMode()
                return@submitList
            }
            if (selectionMode) updateSelectionToolbar()
            if (restoreScrollPending) {
                restoreListPosition()
            } else if (!ScreenLogDispatcher.isAutoScrollPaused() && filteredEntries.isNotEmpty()) {
                binding.screenLogList.scrollToPosition(filteredEntries.lastIndex)
            }
        }
    }

    private fun updateTags(tags: List<String>) {
        val values = listOf(context.getString(R.string.screen_log_tag_all)) + tags
        val selectedTag = filter.tag ?: context.getString(R.string.screen_log_tag_all)
        val existing = (binding.screenLogTag.adapter as? ArrayAdapter<*>)?.let { adapter ->
            (0 until adapter.count).map { adapter.getItem(it).toString() }
        }
        if (existing == values) {
            val index = values.indexOf(selectedTag).takeIf { it >= 0 } ?: 0
            if (binding.screenLogTag.selectedItemPosition != index) {
                suppressFilterCallbacks = true
                binding.screenLogTag.setSelection(index)
                suppressFilterCallbacks = false
            }
            return
        }
        binding.screenLogTag.adapter = spinnerAdapter(values)
        val index = values.indexOf(selectedTag).takeIf { it >= 0 } ?: 0
        suppressFilterCallbacks = true
        binding.screenLogTag.setSelection(index)
        suppressFilterCallbacks = false
    }

    private fun toggleHeight() {
        val oldHeight = layoutParams?.height ?: 0
        large = !large
        ScreenLogManager.setPanelExpanded(large)
        val newHeight = panelHeight(large)
        trace("toggleHeight large=$large, oldHeight=$oldHeight, newHeight=$newHeight, parent=${parent?.javaClass?.simpleName}")
        layoutParams = layoutParams.apply { height = newHeight }
        requestLayout()
    }

    private fun panelHeight(large: Boolean): Int {
        val screenHeight = resources.displayMetrics.heightPixels
        return if (large) screenHeight * 2 / 3 else screenHeight / 3
    }

    private fun MotionEvent.actionName(): String {
        return when (actionMasked) {
            MotionEvent.ACTION_DOWN -> "DOWN"
            MotionEvent.ACTION_UP -> "UP"
            MotionEvent.ACTION_MOVE -> "MOVE"
            MotionEvent.ACTION_CANCEL -> "CANCEL"
            else -> actionMasked.toString()
        }
    }

    private fun trace(message: String) {
        Log.i(TAG, message)
    }

    private fun restoreFilterControls() {
        suppressFilterCallbacks = true
        binding.screenLogSearch.setText(filter.keyword)
        val levelIndex = filter.level?.let { ScreenLogLevel.values().indexOf(it) + 1 } ?: 0
        binding.screenLogLevel.setSelection(levelIndex.coerceAtLeast(0))
        suppressFilterCallbacks = false
    }

    private fun saveListPosition() {
        val layoutManager = binding.screenLogList.layoutManager as? LinearLayoutManager ?: return
        val position = layoutManager.findFirstVisibleItemPosition()
        if (position < 0) return
        val top = layoutManager.findViewByPosition(position)?.top ?: 0
        state.listPosition = position
        state.listOffset = top
    }

    private fun restoreListPosition() {
        val itemCount = adapter.itemCount
        if (itemCount <= 0) {
            restoreScrollPending = false
            return
        }
        val position = state.listPosition.coerceIn(0, itemCount - 1)
        binding.screenLogList.post {
            (binding.screenLogList.layoutManager as? LinearLayoutManager)
                ?.scrollToPositionWithOffset(position, state.listOffset)
            restoreScrollPending = false
        }
    }

    private fun onEntryClick(entry: com.hexin.screenlog.core.ScreenLogEntry) {
        if (selectionMode) toggleEntrySelection(entry)
    }

    private fun spinnerAdapter(values: List<String>): ArrayAdapter<String> {
        return ArrayAdapter(
            context,
            R.layout.screen_log_spinner_item,
            android.R.id.text1,
            values
        ).apply {
            setDropDownViewResource(R.layout.screen_log_spinner_dropdown_item)
        }
    }

    private fun onEntryLongClick(entry: com.hexin.screenlog.core.ScreenLogEntry) {
        if (!selectionMode) enterSelectionMode()
        toggleEntrySelection(entry)
    }

    private fun enterSelectionMode() {
        selectionMode = true
        requestFocus()
        autoScrollWasPaused = ScreenLogDispatcher.isAutoScrollPaused()
        if (!autoScrollWasPaused) ScreenLogManager.setAutoScrollPausedTemporarily(true)
        adapter.setSelectionMode(true)
        setFilterControlsEnabled(false)
        updateSelectionToolbar()
    }

    private fun exitSelectionMode() {
        if (!selectionMode) return
        selectionMode = false
        adapter.setSelectionMode(false)
        setFilterControlsEnabled(true)
        binding.screenLogSelectionBar.visibility = View.GONE
        binding.screenLogNormalBar.visibility = View.VISIBLE
        if (!autoScrollWasPaused) ScreenLogManager.setAutoScrollPausedTemporarily(false)
    }

    private fun toggleEntrySelection(entry: com.hexin.screenlog.core.ScreenLogEntry) {
        adapter.setSelected(entry, !adapter.isSelected(entry))
        if (selectedEntries().isEmpty()) exitSelectionMode() else updateSelectionToolbar()
    }

    private fun toggleSelectAll() {
        val allSelected = filteredEntries.isNotEmpty() && filteredEntries.all(adapter::isSelected)
        filteredEntries.forEach { adapter.setSelected(it, !allSelected) }
        if (selectedEntries().isEmpty()) exitSelectionMode() else updateSelectionToolbar()
    }

    private fun selectedEntries(): List<com.hexin.screenlog.core.ScreenLogEntry> {
        return filteredEntries.filter(adapter::isSelected)
    }

    private fun updateSelectionToolbar() {
        val count = selectedEntries().size
        binding.screenLogNormalBar.visibility = View.INVISIBLE
        binding.screenLogSelectionBar.visibility = View.VISIBLE
        binding.screenLogSelectionCount.text = resources.getQuantityString(
            R.plurals.screen_log_selected_count,
            count,
            count
        )
        val allSelected = filteredEntries.isNotEmpty() && filteredEntries.all(adapter::isSelected)
        binding.screenLogSelectionAll.setText(
            if (allSelected) R.string.screen_log_deselect_all else R.string.screen_log_select_all
        )
    }

    private fun setFilterControlsEnabled(enabled: Boolean) {
        listOf(
            binding.screenLogSearch,
            binding.screenLogLevel,
            binding.screenLogTag,
            binding.screenLogAutoScroll
        ).forEach {
            it.isEnabled = enabled
            it.alpha = if (enabled) 1f else 0.5f
        }
    }

    private fun copySelectedEntries() {
        val entries = selectedEntries()
        if (entries.isEmpty()) return
        copyEntries(entries)
        exitSelectionMode()
    }

    private fun copyFilteredEntries() {
        // 复制时重新读取快照，避免关键字输入的延迟刷新导致复制到旧筛选结果。
        val entries = ScreenLogFilterMatcher.filter(ScreenLogDispatcher.snapshot(), filter)
        if (entries.isEmpty()) {
            Toast.makeText(context, R.string.screen_log_nothing_to_copy, Toast.LENGTH_SHORT).show()
            return
        }
        copyEntries(entries)
    }

    private fun copyEntries(entries: List<com.hexin.screenlog.core.ScreenLogEntry>) {
        val options = ScreenLogManager.copyOptions()
        val text = entries.joinToString(separator = "\n") {
            ScreenLogFormatter.formatForCopy(it, options)
        }
        copyText(text, copiedCount = entries.size)
    }

    private fun confirmClearLogs() {
        AlertDialog.Builder(context)
            .setTitle(R.string.screen_log_clear_confirm_title)
            .setMessage(R.string.screen_log_clear_confirm_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.screen_log_clear) { _, _ -> ScreenLogManager.clearLogs() }
            .show()
    }

    private fun copyText(text: String, copiedCount: Int) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("screen-log", text))
        val message = resources.getQuantityString(
            R.plurals.screen_log_copied_count,
            copiedCount,
            copiedCount
        )
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
