package com.hexin.screenlog.ui

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hexin.screenlog.R
import com.hexin.screenlog.api.ScreenLogLevel
import com.hexin.screenlog.core.ScreenLogEntry
import com.hexin.screenlog.core.ScreenLogFormatter
import com.hexin.screenlog.databinding.ScreenLogItemBinding

class ScreenLogAdapter(
    private val onClick: (ScreenLogEntry) -> Unit,
    private val onLongClick: (ScreenLogEntry) -> Unit
) : ListAdapter<ScreenLogEntry, ScreenLogAdapter.Holder>(DiffCallback) {

    private val selectedEntries = java.util.Collections.newSetFromMap(
        java.util.IdentityHashMap<ScreenLogEntry, Boolean>()
    )
    private var selectionMode = false
    private var searchKeyword = ""

    fun setSearchKeyword(keyword: String) {
        if (searchKeyword == keyword) return
        searchKeyword = keyword
        notifyItemRangeChanged(0, itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ScreenLogItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class Holder(
        private val binding: ScreenLogItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                currentEntry()?.let(onClick)
            }
            binding.root.setOnLongClickListener {
                currentEntry()?.let(onLongClick)
                true
            }
        }

        fun bind(entry: ScreenLogEntry, position: Int) {
            binding.screenLogTime.text = ScreenLogFormatter.formatTime(entry.timeMillis)
            binding.screenLogLevelText.text = entry.level.name.take(1)
            binding.screenLogLevelText.setTextColor(ContextCompat.getColor(itemView.context, colorFor(entry.level)))
            binding.screenLogTagText.text = highlight(entry.tag, itemView.context)
            val requestLabel = ScreenLogFormatter.requestLabel(entry)
            binding.screenLogRequestHeader.text = requestLabel?.let { highlight(it, itemView.context) }
            binding.screenLogRequestHeader.visibility = if (requestLabel == null) View.GONE else View.VISIBLE
            binding.screenLogMessage.text = highlight(ScreenLogFormatter.formatBody(entry), itemView.context)
            val extrasText = ScreenLogFormatter.visibleExtras(entry).entries.joinToString { "${it.key}=${it.value}" }
            binding.screenLogExtras.text = highlight(extrasText, itemView.context)
            binding.screenLogExtras.visibility = if (extrasText.isBlank()) View.GONE else View.VISIBLE
            val selected = selectedEntries.contains(entry)
            binding.screenLogSelection.visibility = if (selectionMode) View.VISIBLE else View.GONE
            binding.screenLogSelection.setImageResource(
                if (selected) R.drawable.screen_log_ic_checked else R.drawable.screen_log_ic_checkbox
            )
            bindPosition(position)
        }

        fun bindPosition(position: Int) {
            binding.screenLogIndex.text = (position + 1).toString()
            val selected = currentEntry()?.let(selectedEntries::contains) == true
            binding.root.setBackgroundResource(
                if (selected) R.drawable.screen_log_item_selected_bg
                else if (position % 2 == 0) R.color.screen_log_row_dark
                else R.color.screen_log_row_light
            )
        }

        private fun currentEntry(): ScreenLogEntry? {
            val position = bindingAdapterPosition
            return position.takeIf { it != RecyclerView.NO_POSITION }?.let(::getItem)
        }
    }

    override fun onBindViewHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && payloads.all { it == ROW_POSITION }) holder.bindPosition(position)
        else super.onBindViewHolder(holder, position, payloads)
    }

    override fun onCurrentListChanged(previousList: MutableList<ScreenLogEntry>, currentList: MutableList<ScreenLogEntry>) {
        super.onCurrentListChanged(previousList, currentList)
        // 淘汰旧日志或筛选后，原有条目的序号和交替背景可能改变。
        notifyItemRangeChanged(0, itemCount, ROW_POSITION)
    }

    private companion object {
        const val ROW_POSITION = "row_position"
    }

    fun setSelectionMode(enabled: Boolean) {
        if (selectionMode == enabled) return
        selectionMode = enabled
        if (!enabled) selectedEntries.clear()
        notifyItemRangeChanged(0, itemCount)
    }

    fun setSelected(entry: ScreenLogEntry, selected: Boolean) {
        if (selected) selectedEntries.add(entry) else selectedEntries.remove(entry)
        val position = currentList.indexOfFirst { it === entry }
        if (position >= 0) notifyItemChanged(position)
    }

    fun isSelected(entry: ScreenLogEntry): Boolean = selectedEntries.contains(entry)

    private fun highlight(value: String, context: Context): CharSequence {
        if (searchKeyword.isEmpty()) return value
        val styled = SpannableString(value)
        val background = ContextCompat.getColor(context, R.color.screen_log_search_highlight)
        val foreground = ContextCompat.getColor(context, R.color.screen_log_search_highlight_text)
        var start = value.indexOf(searchKeyword, ignoreCase = true)
        while (start >= 0) {
            val end = start + searchKeyword.length
            styled.setSpan(BackgroundColorSpan(background), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            styled.setSpan(ForegroundColorSpan(foreground), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            start = value.indexOf(searchKeyword, startIndex = end, ignoreCase = true)
        }
        return styled
    }

    private fun colorFor(level: ScreenLogLevel): Int {
        return when (level) {
            ScreenLogLevel.ERROR -> R.color.screen_log_error
            ScreenLogLevel.WARN -> R.color.screen_log_warn
            ScreenLogLevel.INFO -> R.color.screen_log_info
            else -> R.color.screen_log_debug
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ScreenLogEntry>() {
        override fun areItemsTheSame(oldItem: ScreenLogEntry, newItem: ScreenLogEntry): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: ScreenLogEntry, newItem: ScreenLogEntry): Boolean {
            return oldItem == newItem
        }
    }
}
