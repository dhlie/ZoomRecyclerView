package com.dhl.zoomrecyclerview.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.dhl.zoom.ZoomLayoutManager
import com.dhl.zoomrecyclerview.databinding.ListItemLayoutBinding
import kotlin.math.abs

/**
 *
 * Author: duanhl
 * Create: 2021/4/28 10:56 PM
 * Description:
 *
 */
class MyConstraintLayout : ConstraintLayout {

    private lateinit var binding: ListItemLayoutBinding
    private val textSize = 14

    private var downLeft: Int = 0
    private var downTop: Int = 0
    private var downRight: Int = 0
    private var downBottom: Int = 0
    private var downX = 0f
    private var downY = 0f
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val longPressTimeout = ViewConfiguration.getLongPressTimeout()

    constructor(context: Context) : super(context) {
    }

    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet) {
    }

    constructor(context: Context, attributeSet: AttributeSet?, def: Int) : super(context, attributeSet, def) {
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = ListItemLayoutBinding.bind(this)
        val touchListener = OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    parent.requestDisallowInterceptTouchEvent(true)
                    downLeft = v.left
                    downTop = v.top
                    downRight = v.right
                    downBottom = v.bottom
                    downX = event.rawX
                    downY = event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    var left = downLeft + dx
                    var top = downTop + dy
                    var right = downRight + dx
                    var bottom = downBottom + dy

                    left /= measuredWidth
                    top /= measuredHeight
                    right /= measuredWidth
                    bottom /= measuredHeight
                    binding.glIvTop.setGuidelinePercent(top)
                    binding.glIvStart.setGuidelinePercent(left)
                    binding.glIvEnd.setGuidelinePercent(right)
                    binding.glIvBottom.setGuidelinePercent(bottom)

                }
                MotionEvent.ACTION_CANCEL,
                MotionEvent.ACTION_UP -> {
                    parent.requestDisallowInterceptTouchEvent(false)
                    if (abs(event.rawX - downX) < touchSlop && abs(event.rawY - downY) < touchSlop
                        && event.eventTime - event.downTime < longPressTimeout) {
                        onClick()
                    }
                }
            }
            true
        }

        binding.ivIcon.setOnTouchListener(touchListener)
    }

    private fun onClick() {
        Toast.makeText(context, "click: ${binding.tvText.text}", Toast.LENGTH_SHORT).show()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        binding.tvText.textSize = textSize * getRecyclerViewScale()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun getRecyclerViewScale(): Float {
        var parent = parent
        while (parent != null && parent !is RecyclerView) {
            parent = parent.parent
        }
        val recyclerView = parent as? RecyclerView ?: return 1f
        val zoomLayoutManager = recyclerView.layoutManager as? ZoomLayoutManager ?: return 1f
        return zoomLayoutManager.scale
    }

    private fun log(msg: String) {
        Log.i("duanhl", "attach/detach:$msg")
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        log("onAttachedToWindow")
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        log("onDetachedFromWindow")
    }
}