package com.dhl.zoomrecyclerview

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import com.dhl.zoom.ZoomGestureHelper
import com.dhl.zoom.ZoomLayoutManager
import com.dhl.zoomrecyclerview.databinding.DialogListLayoutBinding
import com.google.android.material.bottomsheet.BottomSheetDialog


/**
 *
 * Author: duanhl
 * Create: 5/3/21 9:04 PM
 * Description:
 *
 */
class BDialog(context: Context) : BottomSheetDialog(context, R.style.AppTheme_Dialog_BottomSheet) {

    private val binding = DialogListLayoutBinding.inflate(layoutInflater)
    private var height = getScreenHeight() * 7f / 10

    init {
        binding.root.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height.toInt())
        setContentView(binding.root)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        behavior.peekHeight = getScreenHeight()
        val layoutManager = ZoomLayoutManager()
        binding.rvList.layoutManager = layoutManager
        ZoomGestureHelper.initScaleGesture(binding.rvList, layoutManager)
        ZoomGestureHelper.initRecyclerViewDoubleTapGesture(binding.rvList)
        val adapter = ListAdapter(context)
        binding.rvList.adapter = adapter
        binding.rvList.addItemDecoration(ItemDetector())

        adapter.changeData(initData())
    }

}