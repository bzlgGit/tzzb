package com.example.myapplication

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.hexin.screenlog.api.ScreenLog

class SecondPageActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 24, 24, 24)
            addView(TextView(this@SecondPageActivity).apply {
                text = "Second page: check the floating log entry here"
                textSize = 18f
            })
            addView(Button(this@SecondPageActivity).apply {
                text = "Add log from second page"
                isAllCaps = false
                setOnClickListener { ScreenLog.i("SecondPage", "log from second page") }
            })
            addView(Button(this@SecondPageActivity).apply {
                text = "Back to main page"
                isAllCaps = false
                setOnClickListener { finish() }
            })
        })
    }
}
