package id.pineapple.recyclerviewutil

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

abstract class BaseViewHolder<T: UniqueEntity>(itemView: View) :
		RecyclerView.ViewHolder(itemView) {
	var item: T? = null
		 private set
	
	constructor(context: Context, resId: Int, parent: ViewGroup) :
			this(LayoutInflater.from(context).inflate(resId, parent, false))
	
	fun bind(item: T) {
		val oldItem = this.item
		this.item = null
		doBind(item, oldItem)
		this.item = item
	}
	
	open fun doBind(item: T, oldItem: T?) {
	}
	
	open fun onRecycled() {
	}
}
