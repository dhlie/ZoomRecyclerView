package com.dhl.zoomrecyclerview

import android.content.res.Resources
import android.util.TypedValue
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 *
 * Author: duanhl
 * Create: 2021/4/11 5:15 PM
 * Description:
 *
 */


val Int.dp: Float
    get() {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), Resources.getSystem().displayMetrics)
    }

val Float.dp: Float
    get() {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, Resources.getSystem().displayMetrics)
    }

class BindingViewHolder<T : ViewBinding>(val binding: T) : RecyclerView.ViewHolder(binding.root)