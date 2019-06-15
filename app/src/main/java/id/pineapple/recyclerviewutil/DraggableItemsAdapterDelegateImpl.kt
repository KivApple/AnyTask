package id.pineapple.recyclerviewutil

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.support.v7.widget.helper.ItemTouchHelper

class DraggableItemsAdapterDelegateImpl: DraggableItemsAdapterDelegate {
	override lateinit var draggableItemsAdapter: DraggableItemsAdapter
	private val touchHelper: ItemTouchHelper = ItemTouchHelper(TouchHelperCallback())
	
	override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
		touchHelper.attachToRecyclerView(recyclerView)
	}
	
	override fun startDrag(viewHolder: BaseViewHolder<out UniqueEntity>) {
		touchHelper.startDrag(viewHolder)
	}
	
	inner class TouchHelperCallback: ItemTouchHelper.Callback() {
		private var itemMoved = false
		
		override fun isLongPressDragEnabled(): Boolean = false
		
		override fun isItemViewSwipeEnabled(): Boolean = false
		
		override fun getMovementFlags(recyclerView: RecyclerView,
									  viewHolder: RecyclerView.ViewHolder): Int {
			if (viewHolder !is DraggableViewHolder) {
				return makeMovementFlags(0, 0)
			}
			val spanCount = (recyclerView.layoutManager as? StaggeredGridLayoutManager)?.spanCount ?: 1
			return makeMovementFlags(
					if (viewHolder.canMove())
						ItemTouchHelper.UP or ItemTouchHelper.DOWN or
								if (spanCount > 1)
									ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
								else
									0
					else
						0,
					(if (viewHolder.canSwipeLeft()) ItemTouchHelper.LEFT else 0) or
							if (viewHolder.canSwipeRight()) ItemTouchHelper.RIGHT else 0
			)
		}
		
		override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
							targetViewHolder: RecyclerView.ViewHolder): Boolean {
			if (viewHolder !is DraggableViewHolder || targetViewHolder !is DraggableViewHolder) {
				return false
			}
			if (viewHolder.canDrop(targetViewHolder)) {
				val fromPosition = viewHolder.adapterPosition
				val toPosition = targetViewHolder.adapterPosition
				draggableItemsAdapter.items.add(toPosition, draggableItemsAdapter.items.removeAt(fromPosition))
				itemMoved = true
				draggableItemsAdapter.notifyItemMoved(fromPosition, toPosition)
				return true
			}
			return false
		}
		
		override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
			if (viewHolder is DraggableViewHolder) {
				when (direction) {
					ItemTouchHelper.LEFT ->
						viewHolder.swipedLeft()
					ItemTouchHelper.RIGHT ->
						viewHolder.swipedRight()
				}
			}
		}
		
		override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
			super.clearView(recyclerView, viewHolder)
			(viewHolder as? DraggableViewHolder)?.dragFinished(itemMoved)
			itemMoved = false
		}
	}
}
