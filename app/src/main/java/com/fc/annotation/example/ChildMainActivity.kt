package com.fc.annotation.example

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.fc.annotation.ATMode
import com.fc.annotation.Debounce
import com.fc.annotation.Delay
import com.fc.annotation.Throttle
import com.fc.annotation.core.ATMethodManager
import kotlinx.android.synthetic.main.activity_main.*

open class ChildMainActivity : MainActivity() {

    private var debounceIndex: Int = 0
    private var throttleIndex: Int = 0
    private var delayIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tv_delay.setOnClickListener(object: View.OnClickListener {

            @Delay(1000)
            override fun onClick(v: View?) {
                tv_delay.text = "tv_delay delay: $delayIndex"
                delayIndex++
            }
        })
    }

    override fun onOpenPage(view: View) {
        startActivity(Intent(this, MainActivity::class.java))
    }

    override fun onDebounceClick(view: View) {
        testDebounce(debounceIndex++)
    }

    override fun onThrottleClick(view: View) {
        testThrottle(throttleIndex++)
    }

    override fun onDelayClick(view: View) {
        testDelay(delayIndex++)
    }
}
