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
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.view.doOnNextLayout
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

    override fun setFitsSystemWindows(fitSystemWindows: Boolean) {
        // 宿主可能给 content 下所有 ViewGroup 强制开启该标志；浮层坐标已基于 content，不能再次扣除状态栏。
        super.setFitsSystemWindows(false)
    }

    private companion object {
        const val TAG = "ScreenLogPanelView"
    }

    private val adapter = ScreenLogAdapter(::onEntryClick, ::onEntryLongClick)
    private val binding = ScreenLogPanelViewBinding.inflate(LayoutInflater.from(context), this, true)
    private val commonTags = ScreenLogManager.commonTags()
    private var filter = state.filter
    private var filteredEntries = emptyList<com.hexin.screenlog.core.ScreenLogEntry>()
    private var refreshPosted = false
    private var touchMode = ScreenLogManager.getTouchMode()
    private var suppressFilterCallbacks = false
    private var restoreScrollPending = state.listPosition >= 0
    private var selectionMode = false
    private var autoScrollWasPaused = false
    private var userScrollGeneration = 0L
    private var touchTargetGesture = false
    private var panelDragDownY = 0f
    private var panelDragStartOffset = 0f
    private var panelDragging = false
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (!isTouchTarget(e)) return false
            ScreenLogManager.toggleTouchMode()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            // 顶部操作按钮的连续点击不参与双击切换面板高度。
            if (e.y <= binding.screenLogNormalBar.bottom && !isTouchTarget(e)) return false
            Log.d(TAG, "onDoubleTap x=${e.x}, y=${e.y}, largeBefore=${state.large}, touchMode=$touchMode")
            if (state.fullscreen) toggleFullscreen() else toggleHeight()
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
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, panelHeight(), Gravity.BOTTOM)
        binding.screenLogList.layoutManager = LinearLayoutManager(context)
        binding.screenLogList.adapter = adapter
        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> constrainPanelPosition() }
        trace("init, defaultHeight=${panelHeight()}, root=${binding.root.javaClass.simpleName}")
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
                updateJumpButtonsVisibility()
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) userScrollGeneration++
                saveListPosition()
                updateJumpButtonsVisibility()
            }
        })
        setupFilters()
        setupActions()
        updateTouchTargetText()
        restoreFilterControls()
        refreshNow()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            touchTargetGesture = isTouchTarget(event)
            if (touchTargetGesture) {
                panelDragDownY = event.rawY
                panelDragStartOffset = state.panelOffsetY
                panelDragging = false
            }
        }
        if (touchTargetGesture && handlePanelDrag(event)) return true
        if (touchMode == ScreenLogTouchMode.UNDERLYING_PAGE) {
            if (touchTargetGesture) gestureDetector.onTouchEvent(event)
            if (!touchTargetGesture && event.actionMasked == MotionEvent.ACTION_DOWN) {
                trace("dispatchTouchEvent pass through, action=${event.actionName()}, x=${event.x}, y=${event.y}")
            }
            val handled = touchTargetGesture
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                touchTargetGesture = false
            }
            return handled
        }
        if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_UP) {
            trace("dispatchTouchEvent handle, action=${event.actionName()}, x=${event.x}, y=${event.y}")
        }
        val handledByGesture = gestureDetector.onTouchEvent(event)
        val handledByChildren = super.dispatchTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_UP) {
            trace("dispatchTouchEvent result, action=${event.actionName()}, gesture=$handledByGesture, children=$handledByChildren")
        }
        return handledByChildren || handledByGesture
    }

    private fun handlePanelDrag(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (!state.fullscreen && !panelDragging &&
                    kotlin.math.abs(event.rawY - panelDragDownY) > touchSlop
                ) {
                    panelDragging = true
                    val cancel = MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
                    gestureDetector.onTouchEvent(cancel)
                    if (touchMode == ScreenLogTouchMode.LOG_PANEL) super.dispatchTouchEvent(cancel)
                    cancel.recycle()
                }
                if (panelDragging) {
                    state.panelOffsetY = panelDragStartOffset + event.rawY - panelDragDownY
                    constrainPanelPosition()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (panelDragging) {
                    panelDragging = false
                    touchTargetGesture = false
                    return true
                }
            }
        }
        return false
    }

    private fun constrainPanelPosition() {
        val host = parent as? ViewGroup ?: return
        if (host.height == 0 || height == 0) return
        if (state.fullscreen) {
            translationY = 0f
            return
        }
        val topInset = ScreenLogPanelInsets.topMargin(host)
        val bottom = host.height - (layoutParams as LayoutParams).bottomMargin
        val defaultTop = bottom - height
        state.panelOffsetY = state.panelOffsetY.coerceIn(
            (topInset - defaultTop).coerceAtMost(0).toFloat(), 0f
        )
        translationY = state.panelOffsetY
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
        updateTouchTargetText()
    }

    private fun isTouchTarget(event: MotionEvent): Boolean {
        val bar = binding.screenLogNormalBar
        val title = binding.screenLogTitle
        return bar.visibility == View.VISIBLE &&
            event.x >= bar.left + title.left && event.x < bar.left + title.right &&
            event.y >= bar.top + title.top && event.y < bar.top + title.bottom
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        trace("onAttachedToWindow width=$width, height=$height, layoutHeight=${layoutParams?.height}, parent=${parent?.javaClass?.simpleName}")
        ScreenLogDispatcher.addListener(this)
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

    override fun onAutoScrollChanged(paused: Boolean) = Unit

    override fun onRecordingChanged(enabled: Boolean) = Unit

    private fun setupFilters() {
        val levels = listOf(context.getString(R.string.screen_log_level_all)) + ScreenLogLevel.values().map { it.name }
        binding.screenLogLevel.adapter = spinnerAdapter(levels)
        binding.screenLogCommonTag.adapter = spinnerAdapter(
            listOf(context.getString(R.string.screen_log_common_tag_all)) + commonTags
        )
        binding.screenLogCommonTag.setSelection(
            if (state.commonTagSelected) commonTags.indexOf(filter.tag) + 1 else 0
        )
        binding.screenLogLevel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (suppressFilterCallbacks) return
                filter = filter.copy(level = if (position == 0) null else ScreenLogLevel.valueOf(levels[position]))
                state.filter = filter
                refreshNow()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        binding.screenLogCommonTag.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (suppressFilterCallbacks || binding.screenLogCommonTag.selectedItemPosition != position ||
                    (position == 0 && !state.commonTagSelected)
                ) return
                selectTag(commonTags.getOrNull(position - 1), fromCommonTags = true)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        binding.screenLogTag.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (suppressFilterCallbacks || binding.screenLogTag.selectedItemPosition != position ||
                    (position == 0 && state.commonTagSelected)
                ) return
                selectTag(if (position == 0) null else parent?.getItemAtPosition(position)?.toString())
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
        updateFullscreenAction()
        binding.screenLogFullscreen.setOnClickListener { toggleFullscreen() }
        binding.screenLogClear.setOnClickListener { confirmClearLogs() }
        binding.screenLogCopyFiltered.setOnClickListener { copyFilteredEntries() }
        binding.screenLogSettings.setOnClickListener { ScreenLogManager.openSettings() }
        binding.screenLogSelectionClose.setOnClickListener { exitSelectionMode() }
        binding.screenLogSelectionAll.setOnClickListener { toggleSelectAll() }
        binding.screenLogSelectionCopy.setOnClickListener { copySelectedEntries() }
        binding.screenLogJumpToTop.setOnClickListener {
            binding.screenLogList.stopScroll()
            userScrollGeneration++
            (binding.screenLogList.layoutManager as? LinearLayoutManager)
                ?.scrollToPositionWithOffset(0, 0)
            binding.screenLogList.post { updateJumpButtonsVisibility() }
        }
        binding.screenLogJumpToLatest.setOnClickListener {
            binding.screenLogList.stopScroll()
            userScrollGeneration++
            scrollToBottom()
        }
    }

    private fun refreshNow() {
        val wasAtBottom = !binding.screenLogList.canScrollVertically(1)
        val scrollGeneration = userScrollGeneration
        val entries = ScreenLogDispatcher.snapshot()
        updateTags(entries.map { it.tag }.distinct().sorted())
        filteredEntries = ScreenLogFilterMatcher.filter(entries, filter)
        adapter.setSearchKeyword(filter.keyword.trim())
        adapter.submitList(filteredEntries) {
            if (selectionMode && selectedEntries().isEmpty()) {
                exitSelectionMode()
                return@submitList
            }
            if (selectionMode) updateSelectionToolbar()
            if (restoreScrollPending) {
                restoreListPosition()
            } else if (filteredEntries.isNotEmpty() && ScreenLogAutoScrollPolicy.shouldFollow(
                    paused = ScreenLogDispatcher.isAutoScrollPaused(),
                    atBottom = wasAtBottom,
                    userScrolling = binding.screenLogList.scrollState != RecyclerView.SCROLL_STATE_IDLE ||
                        userScrollGeneration != scrollGeneration
                )) {
                scrollToBottom(scrollGeneration)
            }
            binding.screenLogList.post { updateJumpButtonsVisibility() }
        }
    }

    private fun updateJumpButtonsVisibility() {
        val hasEntries = adapter.itemCount > 0
        binding.screenLogJumpToTop.visibility = if (
            hasEntries && binding.screenLogList.canScrollVertically(-1)
        ) View.VISIBLE else View.INVISIBLE
        binding.screenLogJumpToLatest.visibility = if (
            hasEntries && binding.screenLogList.canScrollVertically(1)
        ) View.VISIBLE else View.INVISIBLE
    }

    private fun scrollToBottom(expectedUserScrollGeneration: Long = userScrollGeneration) {
        val list = binding.screenLogList
        val layoutManager = list.layoutManager as? LinearLayoutManager ?: return
        val lastPosition = adapter.itemCount - 1
        if (lastPosition < 0) return

        list.doOnNextLayout {
            list.post {
                if (userScrollGeneration != expectedUserScrollGeneration ||
                    list.scrollState != RecyclerView.SCROLL_STATE_IDLE
                ) return@post

                val lastView = layoutManager.findViewByPosition(lastPosition)
                if (lastView != null) {
                    val viewportBottom = list.height - list.paddingBottom
                    list.scrollBy(0, (lastView.bottom - viewportBottom).coerceAtLeast(0))
                }
                updateJumpButtonsVisibility()
            }
        }
        layoutManager.scrollToPositionWithOffset(lastPosition, 0)
    }

    private fun updateTags(tags: List<String>) {
        if (!state.commonTagSelected && filter.tag != null && filter.tag !in tags) {
            filter = filter.copy(tag = null)
            state.filter = filter
        }
        val values = listOf(context.getString(R.string.screen_log_tag_all)) + tags
        val selectedTag = filter.tag.takeUnless { state.commonTagSelected }
            ?: context.getString(R.string.screen_log_tag_all)
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

    private fun selectTag(tag: String?, fromCommonTags: Boolean = false) {
        val commonSelected = tag != null && fromCommonTags
        if (filter.tag == tag && state.commonTagSelected == commonSelected) return
        filter = filter.copy(tag = tag)
        state.filter = filter
        state.commonTagSelected = commonSelected
        updateCommonTagSelection()
        refreshNow()
    }

    private fun updateCommonTagSelection() {
        val selectedIndex = if (state.commonTagSelected) commonTags.indexOf(filter.tag) + 1 else 0
        if (binding.screenLogCommonTag.selectedItemPosition == selectedIndex) return
        suppressFilterCallbacks = true
        binding.screenLogCommonTag.setSelection(selectedIndex)
        suppressFilterCallbacks = false
    }

    private fun toggleHeight() {
        val oldHeight = layoutParams?.height ?: 0
        state.toggleCompactSize()
        ScreenLogManager.setPanelExpanded(state.large)
        val newHeight = panelHeight()
        trace("toggleHeight large=${state.large}, oldHeight=$oldHeight, newHeight=$newHeight, parent=${parent?.javaClass?.simpleName}")
        layoutParams = layoutParams.apply { height = newHeight }
        requestLayout()
        post { constrainPanelPosition() }
    }

    private fun toggleFullscreen() {
        state.toggleFullscreen()
        val container = parent as? ViewGroup
        layoutParams = (layoutParams as LayoutParams).apply {
            height = panelHeight()
            topMargin = if (state.fullscreen && container != null) {
                ScreenLogPanelInsets.topMargin(container)
            } else 0
        }
        updateFullscreenAction()
        post { constrainPanelPosition() }
    }

    private fun updateTouchTargetText() {
        binding.screenLogTitle.setText(
            if (touchMode == ScreenLogTouchMode.LOG_PANEL) R.string.screen_log_touch_current_panel
            else R.string.screen_log_touch_current_page
        )
    }

    private fun updateFullscreenAction() {
        binding.screenLogFullscreen.setText(
            if (state.fullscreen) R.string.screen_log_restore_size else R.string.screen_log_fullscreen
        )
    }

    private fun panelHeight(): Int {
        if (state.fullscreen) return LayoutParams.MATCH_PARENT
        val screenHeight = resources.displayMetrics.heightPixels
        return if (state.large) screenHeight * 2 / 3 else screenHeight / 3
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
        if (state.commonTagSelected && filter.tag !in commonTags) {
            filter = filter.copy(tag = null)
            state.filter = filter
            state.commonTagSelected = false
        }
        updateCommonTagSelection()
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
            binding.screenLogCommonTag,
            binding.screenLogTag
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
        copyEntries(entries, R.plurals.screen_log_copied_filtered_count)
    }

    private fun copyEntries(
        entries: List<com.hexin.screenlog.core.ScreenLogEntry>,
        copiedCountMessage: Int = R.plurals.screen_log_copied_count
    ) {
        val options = ScreenLogManager.copyOptions()
        val text = entries.joinToString(separator = "\n") {
            ScreenLogFormatter.formatForCopy(it, options)
        }
        copyText(text, copiedCount = entries.size, copiedCountMessage = copiedCountMessage)
    }

    private fun confirmClearLogs() {
        AlertDialog.Builder(context)
            .setTitle(R.string.screen_log_clear_memory_title)
            .setMessage(R.string.screen_log_clear_memory_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.screen_log_clear) { _, _ -> ScreenLogManager.clearMemoryLogs() }
            .show()
    }

    private fun copyText(text: String, copiedCount: Int, copiedCountMessage: Int) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("screen-log", text))
        val message = resources.getQuantityString(
            copiedCountMessage,
            copiedCount,
            copiedCount
        )
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
