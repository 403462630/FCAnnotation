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

class MainActivity : AppCompatActivity() {

    private var debounceIndex: Int = 0
    private var throttleIndex: Int = 0
    private var delayIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tv_delay.setOnClickListener(object: View.OnClickListener {

            @Delay(1000)
            override fun onClick(v: View?) {
                tv_delay.text = "tv_delay delay: $delayIndex"
                delayIndex++
            }
        })
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


    @Debounce(500, threadModel = ATMode.ASYNC)
    private fun testDebounce(index: Int) {
        tv_debounce.text = "debounce: $index"
    }

    @Throttle(500)
    private fun testThrottle(index: Int) {
        tv_throttle.text = "throttle: $index"
    }

    @Delay(
        id = "aaa",
        value = 1000, // 延迟1000毫秒执行
        threadModel = ATMode.MAIN, // 主线执行
        isFirstDelay = false, // 第一次调用此方法 不延迟执行，之后调用再延迟执行
        isUpdateArgs = true, // 当在1000毫秒之内重复调用此方法，则更新参数为最后调用此方法的参数
        isSingleMode = true // 这个比较特殊；表示多个@Delay注解的方法是否单独延迟计时，还是一起延迟计时
    )
    private fun testDelay(index: Int) {
        tv_delay.text = "delay: $index"
    }
}
