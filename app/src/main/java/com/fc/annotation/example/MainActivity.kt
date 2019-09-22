package com.fc.annotation.example

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.fc.annotation.Debounce
import com.fc.annotation.Delay
import com.fc.annotation.Throttle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var debounceIndex: Int = 0
    private var throttleIndex: Int = 0
    private var delayIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onOpenPage(view: View) {
        startActivity(Intent(this, MainActivity::class.java))
    }

    fun onDebounceClick(view: View) {
        testDebounce(debounceIndex++)
    }

    fun onThrottleClick(view: View) {
        testThrottle(throttleIndex++)
    }

    fun onDelayClick(view: View) {
        testDelay(delayIndex++)
    }


    @Debounce(500)
    private fun testDebounce(index: Int) {
        tv_debounce.text = "debounce: $index"
    }

    @Throttle(500)
    private fun testThrottle(index: Int) {
        tv_throttle.text = "debounce: $index"
    }

    @Delay(1000)
    private fun testDelay(index: Int) {
        tv_delay.text = "debounce: $index"
    }
}
