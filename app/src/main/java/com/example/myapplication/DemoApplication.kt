package com.example.myapplication

import android.app.Application
import com.hexin.screenlog.api.ScreenLogKit

class DemoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ScreenLogKit.init(
            application = this,
            hiddenRequestInterfaces = setOf("/mock/large-response", "/mock/frequent-poll")
        )
    }
}
