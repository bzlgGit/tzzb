package com.hexin.screenlog.ui

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
    private val onCopy: (ScreenLogEntry) -> Unit
) : ListAdapter<ScreenLogEntry, ScreenLogAdapter.Holder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ScreenLogItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class Holder(
        private val binding: ScreenLogItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) onCopy(getItem(position))
                true
            }
        }

        fun bind(entry: ScreenLogEntry) {
            binding.screenLogTime.text = ScreenLogFormatter.formatTime(entry.timeMillis)
            binding.screenLogLevelText.text = entry.level.name.take(1)
            binding.screenLogLevelText.setTextColor(ContextCompat.getColor(itemView.context, colorFor(entry.level)))
            binding.screenLogTagText.text = entry.tag
            binding.screenLogMessage.text = entry.message
            val extrasText = entry.extras.orEmpty().entries.joinToString { "${it.key}=${it.value}" }
            binding.screenLogExtras.text = extrasText
            binding.screenLogExtras.visibility = if (extrasText.isBlank()) View.GONE else View.VISIBLE
        }
    }

    private fun colorFor(level: ScreenLogLevel): Int {
        return when (level) {
            ScreenLogLevel.ERROR -> R.color.screen_log_error
            ScreenLogLevel.WARN -> R.color.screen_log_warn
            ScreenLogLevel.INFO -> R.color.screen_log_info
            else -> R.color.screen_log_accent
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
