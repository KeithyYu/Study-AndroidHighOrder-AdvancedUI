package com.wanggk.attributeanimation

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class SplashView(context: Context, attributeSet: AttributeSet?) : View(context, attributeSet) {
    companion object {
        // 大圆(里面包含很多小圆的)的半径
        private const val CIRCLE_RADIUS_BIG = 90f

        // 每一个小圆的半径
        private const val CIRCLE_RADIUS_SMALL = 18f

        // 大圆和小圆旋转的时间
        private const val ROTATION_DURATION: Long = 1000 //ms
    }

    private var mState: SplashState? = null

    private var mAnimator: ValueAnimator? = null

    // 小圆圈的颜色列表，在initialize方法里面初始化
    private lateinit var mCircleColors: IntArray

    // 屏幕正中心点坐标
    private var mCenterX = 0f
    private var mCenterY = 0f

    // 绘制圆的画笔
    private val mPaint = Paint()

    // 绘制背景的画笔
    private val mPaintBackground = Paint()

    // 整体的背景颜色
    private val mSplashBgColor = Color.WHITE

    //屏幕对角线一半
    private var mDiagonalDist = 0f

    //当前大圆旋转角度(弧度)
    private var mCurrentRotationAngle = 0f

    //当前大圆的半径
    private var mCurrentRotationRadius = CIRCLE_RADIUS_BIG

    //空心圆初始半径
    private var mHoleRadius = 0f

    init {
        initView(context)
    }

    private fun initView(context: Context) {
        mCircleColors = context.resources.getIntArray(R.array.splash_circle_colors)
        //画笔初始化, 消除锯齿
        mPaint.isAntiAlias = true
        mPaintBackground.isAntiAlias = true

        //设置样式---边框样式--描边
        mPaintBackground.style = Paint.Style.STROKE
        mPaintBackground.color = mSplashBgColor
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCenterX = (w / 2).toFloat()
        mCenterY = (h / 2).toFloat()

        mDiagonalDist = sqrt((w * w + h * h).toDouble()).toFloat() / 2f//勾股定律
    }

    override fun onDraw(canvas: Canvas) {
        if (mState == null) {
            mState = RotateState()
        }
        mState!!.drawState(canvas)
    }

    // 绘制背景
    private fun drawBackground(canvas: Canvas) {
        if (mHoleRadius > 0) {
            val width = mDiagonalDist - mHoleRadius
            mPaintBackground.strokeWidth = width
            val radius = mHoleRadius + width / 2
            canvas.drawCircle(mCenterX, mCenterY, radius, mPaintBackground)
        } else {
            canvas.drawColor(mSplashBgColor)
        }
    }

    // 绘制圆
    private fun drawCircle(canvas: Canvas) {
        val perCircleAngle = Math.PI * 2 / mCircleColors.size
        for (index in mCircleColors.indices) {
            /**
             * x = r*cos(a) +centerX
             * y=  r*sin(a) + centerY
             * 每个小圆i*间隔角度 + 旋转的角度 = 当前小圆的真实角度
             */
            val currentCircleAngel = index * perCircleAngle + mCurrentRotationAngle
            val cx = cos(currentCircleAngel) * mCurrentRotationRadius + mCenterX
            val cy = sin(currentCircleAngel) * mCurrentRotationRadius + mCenterY
            mPaint.color = mCircleColors[index]
            canvas.drawCircle(cx.toFloat(), cy.toFloat(), CIRCLE_RADIUS_SMALL, mPaint)
        }
    }

    /**
     * 状态转换
     */
    fun splashDisappear() {
        if (mState != null && mState is RotateState) {
            mState!!.cancel()
            post { mState = MergingState() }
        }
    }

    //1.刚进来的时候执行旋转动画
    //2.数据加载完毕之后进行调用我们的聚合逃逸动画
    //3.聚合逃逸完成之后，进行扩散
    //策略模式
    private abstract inner class SplashState {
        abstract fun drawState(canvas: Canvas)
        fun cancel() {
            mAnimator?.cancel()
        }
    }

    /**
     * 默认的旋转动画
     */
    private inner class RotateState : SplashState() {
        init {
            mAnimator = ValueAnimator.ofFloat(0f, (Math.PI * 2).toFloat())
            mAnimator?.apply {
                duration = ROTATION_DURATION
                interpolator = LinearInterpolator()
                addUpdateListener { animation ->
                    mCurrentRotationAngle = animation.animatedValue as Float
                    postInvalidate()
                }
                repeatCount = ValueAnimator.INFINITE
                start()
            }
        }

        override fun drawState(canvas: Canvas) {
            drawBackground(canvas)
            drawCircle(canvas)
        }
    }

    /**
     * 聚合动画
     */
    private inner class MergingState: SplashState() {
        init {
            mAnimator = ValueAnimator.ofFloat(CIRCLE_RADIUS_BIG, 0f)
            mAnimator?.apply {
                duration = ROTATION_DURATION
                interpolator = OvershootInterpolator(10f)
                addUpdateListener { animation ->
                    mCurrentRotationRadius = animation.animatedValue as Float
                    postInvalidate()
                }

                addListener(object : AnimatorListenerAdapter(){
                    override fun onAnimationEnd(animation: Animator?) {
                        super.onAnimationEnd(animation)
                        mState = ExpandState()
                    }
                })

                reverse()
            }
        }

        override fun drawState(canvas: Canvas) {
            drawBackground(canvas)
            drawCircle(canvas)
        }
    }

    /**
     * 扩散
     */
    private inner class ExpandState : SplashState(){
        init {
            mAnimator = ValueAnimator.ofFloat(0f, mDiagonalDist)
            mAnimator?.apply {
                duration = ROTATION_DURATION
                addUpdateListener { animation ->
                    mHoleRadius = animation.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }

        override fun drawState(canvas: Canvas) {
            drawBackground(canvas)
        }
    }
}