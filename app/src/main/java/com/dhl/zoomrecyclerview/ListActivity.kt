package com.dhl.zoomrecyclerview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.dhl.zoom.ZoomGestureHelper
import com.dhl.zoom.ZoomLayoutManager
import com.dhl.zoomrecyclerview.databinding.ActivityListLayoutBinding
import com.dhl.zoomrecyclerview.databinding.ListItemLayoutBinding
import kotlin.random.Random

class ListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListLayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val layoutManager = ZoomLayoutManager()
        binding.rvList.layoutManager = layoutManager
        ZoomGestureHelper.initGesture(binding.rvList, layoutManager)
        val adapter = ListAdapter(applicationContext)
        binding.rvList.adapter = adapter
        binding.rvList.addItemDecoration(ItemDetector())

        adapter.changeData(initData())

        binding.btn1.setOnClickListener {
            val pos = 10
            binding.rvList.scrollToPosition(pos)
            Toast.makeText(applicationContext, "scrollToPosition:$pos", Toast.LENGTH_SHORT).show()
        }
        binding.btn2.setOnClickListener {
            val pos = 20
            binding.rvList.smoothScrollToPosition(pos)
            Toast.makeText(applicationContext, "smoothScrollToPosition:$pos", Toast.LENGTH_SHORT).show()
        }
        binding.btn3.setOnClickListener {
            val pos = 20
            val offset = Random.nextInt(-15000, 15000)
            layoutManager.scrollToPositionWithOffset(pos, offset)
            Toast.makeText(applicationContext, "scrollToPositionWithOffset pos:$pos offset:$offset", Toast.LENGTH_SHORT).show()
        }
        binding.btn4.setOnClickListener {
            layoutManager.zoomable = !layoutManager.zoomable
            binding.btn4.text = if (layoutManager.zoomable) "disable zoom" else "enable zoom"
        }
    }
}

fun initData(): MutableList<ItemBean> {
    val drawables = arrayOf(
        R.drawable.d5,
        R.drawable.d1,
        R.drawable.d2,
        R.drawable.d4,
        R.drawable.d5,
        R.drawable.d6,
        R.drawable.d7,
        R.drawable.d8,
        R.drawable.d9,
        R.drawable.d10,
        R.drawable.d11,
        R.drawable.d12
    )
    val data = mutableListOf<ItemBean>()
    for (i in 0 until 30) {
        val item = ItemBean().apply {
            drawableId = drawables[i % drawables.size]
            text = "drawable:${drawableId.toString(16)}"
        }
        data.add(item)
    }
    return data
}

class ItemBean {
    var drawableId = 0
    var text = ""
}

class ListAdapter(private val context: Context) : Adapter<BindingViewHolder<ListItemLayoutBinding>>() {

    private val layoutInflater = LayoutInflater.from(context)
    private var data: MutableList<ItemBean>? = null
    private var viewHolderCount = 0

    fun changeData(data: MutableList<ItemBean>?) {
        this.data = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingViewHolder<ListItemLayoutBinding> {
        Log.i("duanhl", "ViewHolder:onCreate: viewHolderCount:${++viewHolderCount}")
        val binding = ListItemLayoutBinding.inflate(layoutInflater, parent, false)
        return BindingViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return data?.size ?: 0
    }

    override fun onBindViewHolder(holder: BindingViewHolder<ListItemLayoutBinding>, position: Int) {
        val item = data?.get(position) ?: return

        val drawable = context.resources.getDrawable(item.drawableId)
        holder.binding.ivImage.setImageDrawable(drawable)
        holder.itemView.layoutParams.apply {
            width = drawable.intrinsicWidth
            height = drawable.intrinsicHeight

        }
        holder.binding.tvText.text = "Position: $position\n ${item.text}"

        Log.i("duanhl", "ViewHolder:onBind: position:$position")
    }

}

class ItemDetector : RecyclerView.ItemDecoration() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xffff0000.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1.dp
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val layoutManager = parent.layoutManager ?: return
        for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            c.drawRect(
                layoutManager.getDecoratedLeft(child).toFloat(),
                layoutManager.getDecoratedTop(child).toFloat(),
                layoutManager.getDecoratedRight(child).toFloat(),
                layoutManager.getDecoratedBottom(child).toFloat(),
                paint
            )
        }
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        val dp8 = 8.dp.toInt()
        val verPadding = dp8 * 6
        outRect.left = dp8 * 2
        outRect.right = dp8 * 2

        when (position) {
            0 -> {
                outRect.top = dp8
                outRect.bottom = verPadding / 2
            }
            state.itemCount - 1 -> {
                outRect.bottom = dp8
                outRect.top = verPadding / 2
            }
            else -> {
                outRect.top = verPadding / 2
                outRect.bottom = verPadding / 2
            }
        }
    }

}


