package com.dhl.zoom

import android.graphics.PointF
import android.graphics.Rect
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.util.SparseArray
import android.view.View
import android.view.View.MeasureSpec
import androidx.recyclerview.widget.*
import androidx.recyclerview.widget.RecyclerView.Adapter
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round


/**
 *
 * Author: duanhl
 * Create: 2021/4/10 1:47 PM
 * Description:
 *
 */
class ZoomLayoutManager : RecyclerView.LayoutManager() {

    companion object {
        const val MIN_SCALE = .9f
        const val MAX_SCALE = 3f
        const val DOUBLE_TAP_SCALE = 2f
        const val NORMAL_SCALE = 1f

        const val MSG_SCALE = 1
        const val SCALE_ANIMATION = 300
        const val ANIMATION_INTERVAL: Int = 17 // 1000 / 60
    }

    var scale: Float = 1f
    var zoomable = true
    private var pivotX: Float = 0f
    private var pivotY: Float = 0f
    private val rect = Rect()
    private var childrenLeft = Int.MIN_VALUE
    private var firstVisibleTop = 0
    private var firstVisiblePosition = 0
    private val detachedViewCache = SparseArray<View>()
    private var recycler: RecyclerView.Recycler? = null
    private var scaleScroller: ScaleScroller = ScaleScroller()
    private var orientationHelper = OrientationHelper.createOrientationHelper(this, RecyclerView.VERTICAL)

    /**
     * Works the same way as [android.widget.AbsListView.setSmoothScrollbarEnabled].
     * see [android.widget.AbsListView.setSmoothScrollbarEnabled]
     */
     var smoothScrollbarEnabled = true


    private val handler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_SCALE -> {
                    computeScale()
                }
            }
        }
    }

    private fun computeScale() {
        val time = SystemClock.uptimeMillis()
        handler.removeMessages(MSG_SCALE)
        if (scaleScroller.computeScale()) {
            scaleTo(scaleScroller.mCurrScale)

            val dt = SystemClock.uptimeMillis() - time
            if (dt > ANIMATION_INTERVAL) {
                handler.sendEmptyMessage(MSG_SCALE)
            } else {
                handler.sendEmptyMessageDelayed(MSG_SCALE, ANIMATION_INTERVAL - dt)
            }

        }
    }

    fun setScalePivot(pivotX: Float, pivotY: Float) {
        this.pivotX = pivotX
        this.pivotY = pivotY
    }

    fun doubleTap() {
        val scaleTo = if (scale == NORMAL_SCALE) DOUBLE_TAP_SCALE else NORMAL_SCALE
        startScale(scaleTo, SCALE_ANIMATION)
    }

    /**
     * 缩放手势结束后调整缩放倍数
     */
    fun adjustScale() {
        if (scale < NORMAL_SCALE) {
            startScale(NORMAL_SCALE, SCALE_ANIMATION)
        } else if (scale > DOUBLE_TAP_SCALE) {
            startScale(DOUBLE_TAP_SCALE, SCALE_ANIMATION)
        }
    }

    fun startScale(finalScale: Float, duration: Int) {
        if (!zoomable) {
            return
        }
        handler.removeMessages(MSG_SCALE)
        scaleScroller.startScale(scale, finalScale - scale, duration)
        handler.sendEmptyMessage(MSG_SCALE)
    }

    fun scaleBy(deltaScale: Float): Boolean {
        return scaleTo(scale * deltaScale)
    }

    fun scaleTo(scaleTo: Float): Boolean {
        if (!zoomable || scaleTo == scale || childCount == 0 || recycler == null) {
            return false
        }

        var newScale = scaleTo
        newScale = max(min(newScale, MAX_SCALE), MIN_SCALE)

        val topView = getChildAt(0)!!
        val top = getDecoratedTop(topView)
        val topViewPosition = getPosition(topView)

        val ratioScale: Float = newScale / scale
        var scaledTop = ((top - pivotY) * ratioScale + pivotY).toInt()
        var scaledLeft = ((childrenLeft - pivotX) * ratioScale + pivotX).toInt()
        // trim top
        if (topViewPosition == 0) {
            scaledTop = min(0, scaledTop)
        }

        // trim left
        scaledLeft = when {
            newScale == NORMAL_SCALE -> paddingStart
            newScale < NORMAL_SCALE -> {
                (paddingStart + getHorizontalSpace() * (1 - newScale) / 2).toInt()
            }
            else -> {
                val oldWidth = getDecoratedMeasuredWidth(topView)
                val newWidth = (oldWidth * ratioScale).toInt()
                max(min(paddingStart, scaledLeft), width - paddingEnd - newWidth)
            }
        }

        scale = newScale

        fillList(scaledLeft, scaledTop, true, recycler!!, null)
        return true
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.WRAP_CONTENT,
            RecyclerView.LayoutParams.WRAP_CONTENT
        )
    }

    override fun isAutoMeasureEnabled(): Boolean {
        return true
    }

    override fun onAdapterChanged(oldAdapter: Adapter<*>?, newAdapter: Adapter<*>?) {
        removeAllViews()
        firstVisiblePosition = 0
        firstVisibleTop = 0
    }

    fun scrollToPositionWithOffset(position: Int, offset: Int) {
        if (childCount == 0 || position < 0 || position >= itemCount) {
            return
        }
        firstVisiblePosition = position
        firstVisibleTop = offset

        requestLayout()
    }

    override fun scrollToPosition(position: Int) {
        scrollToPositionWithOffset(position, 0)
    }

    override fun smoothScrollToPosition(recyclerView: RecyclerView, state: RecyclerView.State, position: Int) {
        if (childCount == 0 || position < 0 || position >= state.itemCount) {
            return
        }
        val scroller: LinearSmoothScroller = object : LinearSmoothScroller(recyclerView.context) {
            override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
                val firstChildPos = getPosition(getChildAt(0)!!)
                val direction = if (targetPosition < firstChildPos) -1f else 1f
                return PointF(0f, direction)
            }
        }
        scroller.targetPosition = position
        startSmoothScroll(scroller)
    }

    override fun canScrollHorizontally(): Boolean {
        return scale > 1f
    }

    override fun canScrollVertically(): Boolean {
        return true
    }

    /**
     * 计算滚动条的位置
     *
     * see [com.dhl.zoom.ZoomLayoutManager.computeVerticalScrollOffset]
     * see [com.dhl.zoom.ZoomLayoutManager.computeVerticalScrollRange]
     * see [com.dhl.zoom.ZoomLayoutManager.computeVerticalScrollExtent]
     */
    override fun computeVerticalScrollOffset(state: RecyclerView.State): Int {
        if (childCount == 0 || state.itemCount == 0) {
            return 0
        }
        val startChild = getChildAt(0) ?: return 0
        val endChild = getChildAt(childCount - 1) ?: return 0
        val minPosition = getPosition(startChild)
        val itemsBefore = max(0, minPosition)
        if (!smoothScrollbarEnabled) {
            if (itemsBefore == 0 && orientationHelper.getDecoratedStart(startChild) < 0) {
                return 1
            }
            return itemsBefore
        }
        val laidOutArea = orientationHelper.getDecoratedEnd(endChild) - orientationHelper.getDecoratedStart(startChild)
        val itemRange = getPosition(endChild) - getPosition(startChild) + 1
        val avgSizePerRow = laidOutArea.toFloat() / itemRange

        return round(itemsBefore * avgSizePerRow + ((orientationHelper.startAfterPadding - orientationHelper.getDecoratedStart(startChild)))).toInt()
    }

    override fun computeVerticalScrollRange(state: RecyclerView.State): Int {
        if (childCount == 0 || state.itemCount == 0) {
            return 0
        }
        val startChild = getChildAt(0) ?: return 0
        val endChild = getChildAt(childCount - 1) ?: return 0
        if (!smoothScrollbarEnabled) {
            return state.itemCount
        }
        // smooth scrollbar enabled. try to estimate better.
        // smooth scrollbar enabled. try to estimate better.
        val laidOutArea = orientationHelper.getDecoratedEnd(endChild) - orientationHelper.getDecoratedStart(startChild)
        val laidOutRange = getPosition(endChild) - getPosition(startChild) + 1
        // estimate a size for full list.
        // estimate a size for full list.
        return (laidOutArea.toFloat() / laidOutRange * state.itemCount).toInt()
    }

    override fun computeVerticalScrollExtent(state: RecyclerView.State): Int {
        if (childCount == 0 || state.itemCount == 0) {
            return 0
        }
        val startChild = getChildAt(0) ?: return 0
        val endChild = getChildAt(childCount - 1) ?: return 0
        if (!smoothScrollbarEnabled) {
            return getPosition(endChild) - getPosition(startChild) + 1
        }
        val extend = orientationHelper.getDecoratedEnd(endChild) - orientationHelper.getDecoratedStart(startChild)
        return min(orientationHelper.totalSpace, extend)
    }

    override fun scrollHorizontallyBy(dx: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
        if (childCount == 0) {
            return 0
        }

        val child = getChildAt(0)!!
        val left = getDecoratedLeft(child)
        val right = getDecoratedRight(child)
        if (right - left <= getHorizontalSpace()) {
            return 0
        }

        var clampedDX = dx
        if (dx > 0) {//从右往左滑
            val rightEdge = width - paddingEnd
            if (right - dx < rightEdge) {
                clampedDX = right - rightEdge
            }
        } else {//从左往右滑
            val leftEdge = paddingStart
            if (left - dx > leftEdge) {
                clampedDX = left - leftEdge
            }
        }
        childrenLeft -= clampedDX
        offsetChildrenHorizontal(-clampedDX)
        return clampedDX
    }

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
        if (childCount == 0) {
            return 0
        }

        val topView = getChildAt(0)!!
        val bottomView = getChildAt(childCount - 1)!!
        val top = getDecoratedTop(topView)
        val bottom = getDecoratedBottom(bottomView)
        val itemCount = state.itemCount

        val delta = if (dy > 0) {// 从下往上滑
            when {
                bottom < height -> 0            //底部未填充满, 没有更多 item 了
                bottom - dy >= height -> dy     //滑动 dy 距离后 bottom 任然不可见
                getPosition(bottomView) + 1 < itemCount -> dy //有更多 item
                else -> bottom - height
            }
        } else {// 从上往下滑
            when {
                top - dy <= 0 -> dy
                getPosition(topView) - 1 >= 0 -> dy
                else -> top
            }
        }

        if (delta != 0) {
            offsetChildrenVertical(-delta)
            firstVisibleTop += -delta
            fillList(childrenLeft, firstVisibleTop, false, recycler, state)
        }
        return delta
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        if (state.itemCount == 0) {
            firstVisiblePosition = 0
            firstVisibleTop = 0
            detachAndScrapAttachedViews(recycler)
            return
        }
        if (this.recycler == null) {
            this.recycler = recycler
        }
        if (childrenLeft == Int.MIN_VALUE) {
            childrenLeft = paddingStart
        }

        val relayout = findTopViewPosition(childrenLeft, firstVisibleTop, false, recycler, state)

        detachAndScrapAttachedViews(recycler)

        fill(relayout, recycler, state)
    }

    private fun fillList(left: Int, top: Int, reLayout: Boolean, recycler: RecyclerView.Recycler, state: RecyclerView.State?) {
        val relayout = findTopViewPosition(left, top, reLayout, recycler, state)
        fill(relayout, recycler, state)
    }

    /**
     * 填充 RecyclerView
     *
     * @param reLayout: 所有子 view 是否都要重新 measure/layout
     *
     */
    private fun fill(reLayout: Boolean, recycler: RecyclerView.Recycler, state: RecyclerView.State?) {
        /**
         * 先把所有 item 放入缓存, 填充的时候会先从缓存里取对应位置的 item
         * 填充结束后, 缓存里剩余的是被移除屏幕的, 要放入缓冲池中
         */
        for (i in 0 until childCount) {
            val child = getChildAt(i)!!
            val position: Int = getPosition(child)
            detachedViewCache.put(position, child)
        }
        for (i in 0 until detachedViewCache.size()) {
            detachView(detachedViewCache.valueAt(i))
        }

        /**
         * 从上到下填充
         */
        var nextTop = firstVisibleTop
        var nextPosition = firstVisiblePosition
        while (nextTop < height && nextPosition < itemCount) {
            var view: View? = detachedViewCache.get(nextPosition).apply { detachedViewCache.remove(nextPosition) }
            if (view != null) {
                attachView(view)
                if (reLayout) {
                    measureChildWithScale(view, 0, 0)
                    layoutDecoratedWithMargins(
                        view,
                        childrenLeft,
                        nextTop,
                        childrenLeft + getDecoratedMeasuredWidth(view),
                        nextTop + getDecoratedMeasuredHeight(view)
                    )
                }
            } else {
                view = recycler.getViewForPosition(nextPosition)
                addView(view)
                measureChildWithScale(view, 0, 0)
                layoutDecoratedWithMargins(
                    view,
                    childrenLeft,
                    nextTop,
                    childrenLeft + getDecoratedMeasuredWidth(view),
                    nextTop + getDecoratedMeasuredHeight(view)
                )
            }
            nextTop += getDecoratedMeasuredHeight(view)
            nextPosition++
        }

        //回收屏幕外的 view
        for (i in 0 until detachedViewCache.size()) {
            removeAndRecycleView(detachedViewCache.valueAt(i), recycler)
        }
        detachedViewCache.clear()
    }

    /**
     * 计算第一个 view 在 RecyclerView 中的位置
     */
    private fun findTopViewPosition(left: Int, top: Int, reLayout: Boolean, recycler: RecyclerView.Recycler, state: RecyclerView.State?): Boolean {
        childrenLeft = left
        //先回收移出屏幕的 view, 为后面步骤提供缓存
        for (i in 0 until childCount) {
            val child = getChildAt(i) ?: continue
            if (getDecoratedBottom(child) <= 0) {
                detachedViewCache.put(i, child)
            } else {
                break
            }
        }
        for (i in childCount - 1 downTo 0) {
            val child = getChildAt(i) ?: continue
            if (getDecoratedTop(child) >= height) {
                detachedViewCache.put(i, child)
            } else {
                break
            }
        }
        for (i in 0 until detachedViewCache.size()) {
            removeAndRecycleView(detachedViewCache.valueAt(i), recycler)
        }
        detachedViewCache.clear()

        /**
         * 判断当前位置是否是想要的位置, 是的话直接 return
         */
        val topView = getChildAt(0)
        val bottomView = getChildAt(childCount - 1)
        if (topView != null && bottomView != null) {
            val topViewTop = getDecoratedTop(topView)
            val bottomViewBottom = getDecoratedBottom(bottomView)
            if (topViewTop <= 0 && bottomViewBottom >= height && top == topViewTop && firstVisiblePosition == getPosition(topView)) {
                //RecyclerView 已被填满, 并且位置是对的
                return reLayout
            }
        }

        /**
         * 把屏幕中的 view 放入缓存供后续使用, 避免创建不必要的 view
         */
        for (i in 0 until childCount) {
            val child = getChildAt(i) ?: continue
            val position: Int = getPosition(child)
            detachedViewCache.put(position, child)
        }

        var needLayout = reLayout
        val topViewTop: Int
        val topViewPosition: Int
        val itemCount = state?.itemCount ?: itemCount
        if (firstVisiblePosition >= itemCount) firstVisiblePosition = itemCount - 1
        if (firstVisiblePosition < 0) firstVisiblePosition = 0

        /**
         * 找出第一个 item 的 position 和 top
         */
        if (top <= 0) {// 从 RecyclerView 上方不可见位置开始布局
            var firstViewTop = top
            var lastViewBottom = top
            var firstViewPosition = firstVisiblePosition
            var lastViewPosition = firstVisiblePosition
            var position = lastViewPosition

            /**
             * First, 从 top 位置开始往下填充, 直到最后一个 view 超出 RecyclerView 或者没有更多 view
             */
            while (lastViewBottom < height && position < itemCount) {
                var view: View? = detachedViewCache.get(position)
                if (view == null) {
                    view = recycler.getViewForPosition(position)
                    measureChildWithScale(view, 0, 0)
                    lastViewBottom += getDecoratedMeasuredHeight(view)
                    recycler.recycleView(view)
                } else {
                    if (needLayout) {
                        measureChildWithScale(view, 0, 0)
                    }
                    lastViewBottom += getDecoratedMeasuredHeight(view)
                }

                lastViewPosition = position
                position++
            }

            /**
             * Second, 第一步填充结束后如果 RecyclerView 未被填满, 则从 top 位置开始往上填充,
             * 直到 item 的总高度大于 RecyclerView 的高度或者没有更多 view 为止
             */
            position = firstViewPosition - 1
            while (lastViewBottom - firstViewTop < height && position >= 0) {
                var view: View? = detachedViewCache.get(position)
                if (view == null) {
                    view = recycler.getViewForPosition(position)
                    measureChildWithScale(view, 0, 0)
                    firstViewTop -= getDecoratedMeasuredHeight(view)
                    recycler.recycleView(view)
                } else {
                    if (needLayout) {
                        measureChildWithScale(view, 0, 0)
                    }
                    firstViewTop -= getDecoratedMeasuredHeight(view)
                }

                firstViewPosition = position
                position--
            }

            /**
             * Third, 两个方向填充完后如果:
             * 1. item 总高度仍然小于 RecyclerView 的高度, 说明 RecyclerView 所有 item 都能显示出来, 切不能滑动
             * 此时第一个 item 的 position 是 0, top 也是 0
             * 2. item 总高度大于 RecyclerView 的高度, 从最后一个 item 开始倒着遍历, 找出第一个 item 的位置
             * (因为 top 可能在 RecyclerView 上方很远的位置, 会有很多不可见的 view)
             */
            if (lastViewBottom - firstViewTop < height) {
                topViewTop = 0
                topViewPosition = firstViewPosition
                needLayout = true
            } else {
                if (lastViewBottom < height) {
                    lastViewBottom = height
                    needLayout = true
                }

                firstViewTop = lastViewBottom
                position = lastViewPosition + 1
                do {
                    position--
                    var view: View? = detachedViewCache.get(position)
                    if (view == null) {
                        view = recycler.getViewForPosition(position)
                        measureChildWithScale(view, 0, 0)
                        recycler.recycleView(view)
                    }
                    firstViewTop -= getDecoratedMeasuredHeight(view)
                    firstViewPosition = position
                } while (firstViewTop > 0)

                topViewTop = firstViewTop
                topViewPosition = firstViewPosition
            }
        } else if (top >= height) {// 从 RecyclerView 下方不可见位置开始布局
            var firstViewTop = top
            var lastViewBottom = top
            var firstViewPosition = firstVisiblePosition
            var lastViewPosition = firstVisiblePosition
            var position = firstVisiblePosition - 1

            /**
             * First, 从 top 位置开始往上填充, 直到最上方的 view 超出 RecyclerView 或者没有更多 view
             */
            while (firstViewTop > 0 && position >= 0) {
                var view: View? = detachedViewCache.get(position)
                if (view == null) {
                    view = recycler.getViewForPosition(position)
                    measureChildWithScale(view, 0, 0)
                    firstViewTop -= getDecoratedMeasuredHeight(view)
                    recycler.recycleView(view)
                } else {
                    if (needLayout) {
                        measureChildWithScale(view, 0, 0)
                    }
                    firstViewTop -= getDecoratedMeasuredHeight(view)
                }

                firstViewPosition = position
                position--
            }

            /**
             * Second, 第一步填充结束后如果 RecyclerView 未被填满, 则从 top 位置开始往下填充,
             * 直到 item 的总高度大于 RecyclerView 的高度或者没有更多 view 为止
             */
            position = lastViewPosition
            while (lastViewBottom - firstViewTop < height && position < itemCount) {
                var view: View? = detachedViewCache.get(position)
                if (view == null) {
                    view = recycler.getViewForPosition(position)
                    measureChildWithScale(view, 0, 0)
                    lastViewBottom += getDecoratedMeasuredHeight(view)
                    recycler.recycleView(view)
                } else {
                    if (needLayout) {
                        measureChildWithScale(view, 0, 0)
                    }
                    lastViewBottom += getDecoratedMeasuredHeight(view)
                }

                lastViewPosition = position
                position++
            }

            /**
             * Third, 两个方向填充完后如果:
             * 1. item 总高度仍然小于 RecyclerView 的高度, 说明 RecyclerView 所有 item 都能显示出来, 切不能滑动
             * 此时第一个 item 的 position 是 0, top 也是 0
             * 2. item 总高度大于 RecyclerView 的高度, firstViewTop, firstViewPosition 即是第一个 view 的位置
             */
            if (lastViewBottom - firstViewTop < height) {
                topViewTop = 0
                topViewPosition = firstViewPosition
                needLayout = true
            } else {
                if (firstViewTop > 0) {
                    firstViewTop = 0
                    needLayout = true
                }
                topViewTop = firstViewTop
                topViewPosition = firstViewPosition
            }
        } else {// 从 RecyclerView 中间位置开始布局
            var firstViewTop = top
            var lastViewBottom = top
            var firstViewPosition = firstVisiblePosition
            var lastViewPosition = firstVisiblePosition
            var position = firstVisiblePosition - 1

            /**
             * First, 从 top 位置开始往上填充, 直到最上方的 view 超出 RecyclerView 或者没有更多 view
             */
            while (firstViewTop > 0 && position >= 0) {
                var view: View? = detachedViewCache.get(position)
                if (view == null) {
                    view = recycler.getViewForPosition(position)
                    measureChildWithScale(view, 0, 0)
                    firstViewTop -= getDecoratedMeasuredHeight(view)
                    recycler.recycleView(view)
                } else {
                    if (needLayout) {
                        measureChildWithScale(view, 0, 0)
                    }
                    firstViewTop -= getDecoratedMeasuredHeight(view)
                }

                firstViewPosition = position
                position--
            }

            /**
             * 如果上方未填满, RecyclerView 从第一个 item 开始显示
             */
            if (firstViewTop > 0) {
                topViewTop = 0
                topViewPosition = firstViewPosition
                needLayout = true
            } else {
                /**
                 * Second, 从 top 位置开始往下填充, 直到最下方 item 超出 RecyclerView 或者没有更多 view 为止
                 */
                position = lastViewPosition
                while (lastViewBottom < height && position < itemCount) {
                    var view: View? = detachedViewCache.get(position)
                    if (view == null) {
                        view = recycler.getViewForPosition(position)
                        measureChildWithScale(view, 0, 0)
                        lastViewBottom += getDecoratedMeasuredHeight(view)
                        recycler.recycleView(view)
                    } else {
                        if (needLayout) {
                            measureChildWithScale(view, 0, 0)
                        }
                        lastViewBottom += getDecoratedMeasuredHeight(view)
                    }

                    lastViewPosition = position
                    position++
                }

                /**
                 * Third, 第一个 item 超出 RecyclerView 的情况下, 如果:
                 * 1. 最后一个 item 超出 RecyclerView, 则 firstViewTop, firstViewPosition 即是第一个 item 的位置
                 * 2. 最后一个 item 未超出 RecyclerView, 从 RecyclerView 底部开始往上填充, 找到第一个 item 的位置
                 */
                if (lastViewBottom >= height) {
                    topViewTop = firstViewTop
                    topViewPosition = firstViewPosition
                } else {
                    firstViewTop = height
                    position = lastViewPosition + 1
                    do {
                        position--
                        var view: View? = detachedViewCache.get(position)
                        if (view == null) {
                            view = recycler.getViewForPosition(position)
                            measureChildWithScale(view, 0, 0)
                            firstViewTop -= getDecoratedMeasuredHeight(view)
                            recycler.recycleView(view)
                        } else {
                            if (needLayout) {
                                measureChildWithScale(view, 0, 0)
                            }
                            firstViewTop -= getDecoratedMeasuredHeight(view)
                        }
                        firstViewPosition = position
                    } while (firstViewTop > 0 && position > 0)

                    if (firstViewTop > 0) {
                        firstViewTop = 0
                    }
                    topViewTop = firstViewTop
                    topViewPosition = firstViewPosition
                    needLayout = true
                }
            }
        }

        firstVisibleTop = topViewTop
        firstVisiblePosition = topViewPosition
        detachedViewCache.clear()
        return needLayout
    }

    private fun measureChildWithScale(child: View, widthUsed: Int, heightUsed: Int) {
        var widUsed = widthUsed
        var heiUsed = heightUsed
        val lp = child.layoutParams as RecyclerView.LayoutParams
        calculateItemDecorationsForChild(child, rect)
        widUsed += rect.left + rect.right
        heiUsed += rect.top + rect.bottom
        val scaledWidth = ((width - (paddingLeft + paddingRight + lp.leftMargin + lp.rightMargin + widUsed)) * scale).toInt()
        val scaledHeight = (lp.height.toFloat() / lp.width * scaledWidth).toInt()
        val widthSpec = MeasureSpec.makeMeasureSpec(scaledWidth, MeasureSpec.EXACTLY)
        val heightSpec = MeasureSpec.makeMeasureSpec(scaledHeight, MeasureSpec.EXACTLY)
        if (shouldMeasureChild(child, widthSpec, heightSpec, lp)) {
            child.measure(widthSpec, heightSpec)
        }
    }

    private fun isMeasurementUpToDate(childSize: Int, spec: Int, dimension: Int): Boolean {
        val specMode = MeasureSpec.getMode(spec)
        val specSize = MeasureSpec.getSize(spec)
        if (dimension > 0 && childSize != dimension) {
            return false
        }
        when (specMode) {
            MeasureSpec.UNSPECIFIED -> return true
            MeasureSpec.AT_MOST -> return specSize >= childSize
            MeasureSpec.EXACTLY -> return specSize == childSize
        }
        return false
    }

    private fun shouldMeasureChild(child: View, widthSpec: Int, heightSpec: Int, lp: RecyclerView.LayoutParams): Boolean {
        return child.isLayoutRequested
                || !isMeasurementCacheEnabled
                || !isMeasurementUpToDate(child.width, widthSpec, lp.width)
                || !isMeasurementUpToDate(child.height, heightSpec, lp.height)
    }

    private fun getHorizontalSpace(): Int {
        return width - paddingRight - paddingLeft
    }

    private fun getVerticalSpace(): Int {
        return height - paddingBottom - paddingTop
    }

}