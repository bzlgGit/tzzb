package com.hexin.screenlog.ui

import com.hexin.screenlog.filter.ScreenLogFilter

internal data class ScreenLogPanelState(
    var large: Boolean = false,
    var fullscreen: Boolean = false,
    var filter: ScreenLogFilter = ScreenLogFilter(),
    var commonTagSelected: Boolean = false,
    var listPosition: Int = -1,
    var listOffset: Int = 0,
    var floatingX: Int? = null,
    var floatingY: Int? = null
) {
    /** 全屏独立于小屏/中屏状态，退出时恢复原尺寸。 */
    fun toggleFullscreen() {
        fullscreen = !fullscreen
    }

    fun toggleCompactSize() {
        if (!fullscreen) large = !large
    }
}
