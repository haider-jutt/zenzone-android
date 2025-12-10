package com.zenimmersive.android.helper

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class LayoutMarginDecoration(var left: Int, var top: Int, var right: Int, var bottom: Int) :
    RecyclerView.ItemDecoration() {

    constructor(margin: Int) : this(margin, margin, margin, margin)

    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
    ) {
        outRect.set(left, top, right, bottom)
    }


    fun set(left: Int, top: Int, right: Int, bottom: Int) {
        this.left = left
        this.top = top
        this.right = right
        this.bottom = bottom
    }
}