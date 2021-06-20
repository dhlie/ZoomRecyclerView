package com.dhl.zoomrecyclerview.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.RecyclerView
import com.dhl.zoom.ZoomLayoutManager
import com.dhl.zoomrecyclerview.R
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
    private val constraintSet = ConstraintSet()
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
        constraintSet.clone(this)
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

                    this.left = left
                    this.top = top
                    this.right = right
                    this.bottom = bottom
                    requestLayout()
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

    private var lasttop = 0f
    private var lastleft = 0f
    private var lastright = 0f
    private var lastbottom = 0f
    private var top = 0f
    private var left = 0f
    private var right = 0f
    private var bottom = 0f
    private fun onClick() {
        Toast.makeText(context, "click: ${binding.tvText.text}", Toast.LENGTH_SHORT).show()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        binding.tvText.textSize = textSize * getRecyclerViewScale()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (lastleft != left || lasttop != top || lastright != right || lastbottom != bottom) {
            constraintSet.setGuidelinePercent(R.id.gl_iv_start, left)
            constraintSet.setGuidelinePercent(R.id.gl_iv_top, top)
            constraintSet.setGuidelinePercent(R.id.gl_iv_end, right)
            constraintSet.setGuidelinePercent(R.id.gl_iv_bottom, bottom)
            constraintSet.applyTo(this)

            lastleft = left
            lasttop = top
            lastright = right
            lastbottom = bottom
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
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