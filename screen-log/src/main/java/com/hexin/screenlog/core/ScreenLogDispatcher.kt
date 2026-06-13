package com.hexin.screenlog.core

import android.os.Handler
import android.os.Looper
import java.util.concurrent.CopyOnWriteArrayList

object ScreenLogDispatcher {

    interface Listener {
        fun onLogsChanged()
        fun onAutoScrollChanged(paused: Boolean)
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArrayList<Listener>()
    private val entries = ArrayList<ScreenLogEntry>()
    private var maxMemoryEntries = 2000
    @Volatile
    private var autoScrollPaused = false

    fun configure(maxMemoryEntries: Int) {
        this.maxMemoryEntries = maxMemoryEntries.coerceAtLeast(200)
    }

    fun replace(newEntries: List<ScreenLogEntry>) {
        synchronized(entries) {
            entries.clear()
            entries.addAll(newEntries.takeLast(maxMemoryEntries))
        }
        notifyLogsChanged()
    }

    fun append(entry: ScreenLogEntry) {
        synchronized(entries) {
            entries.add(entry)
            if (entries.size > maxMemoryEntries) {
                entries.subList(0, entries.size - maxMemoryEntries).clear()
            }
        }
        notifyLogsChanged()
    }

    fun clear() {
        synchronized(entries) {
            entries.clear()
        }
        notifyLogsChanged()
    }

    fun snapshot(): List<ScreenLogEntry> {
        return synchronized(entries) { entries.toList() }
    }

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    fun isAutoScrollPaused(): Boolean = autoScrollPaused

    fun setAutoScrollPaused(paused: Boolean) {
        autoScrollPaused = paused
        mainHandler.post {
            listeners.forEach { it.onAutoScrollChanged(paused) }
        }
    }

    private fun notifyLogsChanged() {
        mainHandler.post {
            listeners.forEach { it.onLogsChanged() }
        }
    }
}
