package com.dhl.zoom

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener

/**
 *
 * Author: duanhl
 * Create: 2021/4/11 3:48 PM
 * Description:
 *
 */
class ZoomGestureHelper {

    companion object {
        @SuppressLint("ClickableViewAccessibility")
        fun initGesture(recyclerView: RecyclerView, zoomLayoutManager: ZoomLayoutManager) {
            val scaleGestureDetector = ScaleGestureDetector(recyclerView.context, object : ScaleGestureDetector.OnScaleGestureListener {

                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                    zoomLayoutManager.setScalePivot(detector.focusX, detector.focusY)
                    return true
                }

                override fun onScaleEnd(detector: ScaleGestureDetector) {
                    zoomLayoutManager.adjustScale()
                }

                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    return zoomLayoutManager.scaleBy(detector.scaleFactor)
                }

            })

            val itemTouchListener = object : OnItemTouchListener {

                override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                    scaleGestureDetector.onTouchEvent(e)
                }

                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    return zoomLayoutManager.zoomable && e.pointerCount > 1
                }

                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {

                }
            }

            val clickGestureDetector = GestureDetector(recyclerView.context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    if (!zoomLayoutManager.zoomable) {
                        return false
                    }
                    zoomLayoutManager.setScalePivot(e.x, e.y)
                    zoomLayoutManager.doubleTap()
                    return true
                }
            })

            //添加缩放手势监听
            recyclerView.removeOnItemTouchListener(itemTouchListener)
            recyclerView.addOnItemTouchListener(itemTouchListener)

            //添加双击手势监听
            recyclerView.setOnTouchListener { _, event ->
                clickGestureDetector.onTouchEvent(event)
                false
            }
        }
    }

}