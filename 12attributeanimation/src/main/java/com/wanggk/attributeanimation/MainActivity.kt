package com.wanggk.attributeanimation

import android.os.Bundle
import android.os.Handler
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private var mMainView: FrameLayout? = null
    private var splashView: SplashView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//        val view = findViewById<ImageView>(R.id.iv)
        //将动画层盖在实际的操作图层上
        mMainView = FrameLayout(this)
        splashView = SplashView(this, null)
        mMainView?.addView(splashView)

        setContentView(mMainView)

        startLoadData()
    }

    //    ------------------------加载动画--------------------------------------
    var handler = Handler()
    private fun startLoadData() {
        handler.postDelayed({ //数据加载完毕，进入主界面--->开启后面的两个动画
            splashView!!.splashDisappear()
        }, 5000) //延迟时间
    }
}