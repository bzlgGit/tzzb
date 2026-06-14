package com.hexin.screenlog.ui

import com.hexin.screenlog.filter.ScreenLogFilter

internal data class ScreenLogPanelState(
    var large: Boolean = false,
    var filter: ScreenLogFilter = ScreenLogFilter(),
    var listPosition: Int = -1,
    var listOffset: Int = 0,
    var floatingX: Int? = null,
    var floatingY: Int? = null
)
