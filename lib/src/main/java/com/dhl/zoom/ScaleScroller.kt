package com.dhl.zoom

import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator

/**
 *
 * Author: duanhl
 * Create: 2021/4/11 2:05 PM
 * Description:
 *
 */
class ScaleScroller(interpolator: Interpolator? = null) {

    private val mInterpolator: Interpolator = interpolator ?: DecelerateInterpolator()

    private var mStartScale = 0f
    private var mFinalScale = 0f

    var mCurrScale = 0f
    private var mStartTime: Long = 0L

    private var mDuration = 0
    private var mDurationReciprocal = 0f
    private var mDeltaScale = 0f
    private var mFinished = true

    fun isFinish(): Boolean {
        return mFinished
    }

    fun forceFinish() {
        mFinished = true
    }

    fun startScale(startScale: Float, deltaScale: Float, duration: Int) {
        mFinished = false
        mStartScale = startScale
        mFinalScale = startScale + deltaScale
        mDeltaScale = deltaScale
        mDuration = duration
        mStartTime = AnimationUtils.currentAnimationTimeMillis()
        mDurationReciprocal = 1.0f / mDuration
    }

    fun computeScale(): Boolean {
        if (mFinished) {
            return false
        }

        val timePassed = (AnimationUtils.currentAnimationTimeMillis() - mStartTime).toInt()

        if (timePassed < mDuration) {
            val x = mInterpolator.getInterpolation(timePassed * mDurationReciprocal)
            mCurrScale = mStartScale + x * mDeltaScale
        } else {
            mCurrScale = mFinalScale
            mFinished = true
        }
        return true
    }

}