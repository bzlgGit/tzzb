package com.hexin.screenlog.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.hexin.screenlog.R
import com.hexin.screenlog.api.ScreenLogLevel
import com.hexin.screenlog.core.ScreenLogEntry
import com.hexin.screenlog.core.ScreenLogFormatter

class ScreenLogAdapter(
    private val onCopy: (ScreenLogEntry) -> Unit
) : RecyclerView.Adapter<ScreenLogAdapter.Holder>() {

    private val items = ArrayList<ScreenLogEntry>()

    fun submitList(newItems: List<ScreenLogEntry>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.screen_log_item, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val time: TextView = itemView.findViewById(R.id.screenLogTime)
        private val level: TextView = itemView.findViewById(R.id.screenLogLevelText)
        private val tag: TextView = itemView.findViewById(R.id.screenLogTagText)
        private val message: TextView = itemView.findViewById(R.id.screenLogMessage)
        private val extras: TextView = itemView.findViewById(R.id.screenLogExtras)

        init {
            itemView.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) onCopy(items[position])
                true
            }
        }

        fun bind(entry: ScreenLogEntry) {
            time.text = ScreenLogFormatter.formatTime(entry.timeMillis)
            level.text = entry.level.name.take(1)
            level.setTextColor(ContextCompat.getColor(itemView.context, colorFor(entry.level)))
            tag.text = entry.tag
            message.text = entry.message
            val extrasText = entry.extras.orEmpty().entries.joinToString { "${it.key}=${it.value}" }
            extras.text = extrasText
            extras.visibility = if (extrasText.isBlank()) View.GONE else View.VISIBLE
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
}
