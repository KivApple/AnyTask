package id.pineapple.recyclerviewutil

import android.os.Bundle
import android.support.v7.widget.RecyclerView

interface RecyclerViewAdapterInterface {
	var items: MutableList<UniqueEntity>
	
	fun restoreInstanceState(savedState: Bundle?, callNotifyDataSetChanged: Boolean = true)
	
	fun saveInstanceState(outState: Bundle)
	
	fun getItemId(position: Int): Long
	
	fun notifyDataSetChanged()
	
	fun notifyItemInserted(position: Int)
	
	fun notifyItemRemoved(position: Int)
	
	fun notifyItemChanged(position: Int)
	
	fun notifyItemMoved(fromPosition: Int, toPosition: Int)
	
	fun notifyItemRangeInserted(positionStart: Int, itemCount: Int)
	
	fun notifyItemRangeRemoved(positionStart: Int, itemCount: Int)
	
	fun notifyItemRangeChanged(positionStart: Int, itemCount: Int)
	
	fun onAttachedToRecyclerView(recyclerView: RecyclerView)
	
	fun onDetachedFromRecyclerView(recyclerView: RecyclerView)
}
