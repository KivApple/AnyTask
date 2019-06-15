package id.pineapple.recyclerviewutil

import android.support.v7.widget.RecyclerView

interface DraggableItemsAdapterDelegate {
	var draggableItemsAdapter: DraggableItemsAdapter
	
	fun onAttachedToRecyclerView(recyclerView: RecyclerView)
	
	fun startDrag(viewHolder: BaseViewHolder<out UniqueEntity>)
}
