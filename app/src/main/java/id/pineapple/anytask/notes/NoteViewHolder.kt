package id.pineapple.anytask.notes

import android.view.ViewGroup
import id.pineapple.recyclerviewutil.BaseViewHolder
import id.pineapple.recyclerviewutil.DraggableViewHolder

abstract class NoteViewHolder(protected val adapter: NoteListAdapter, resId: Int, parent: ViewGroup):
		BaseViewHolder<Note>(adapter.context, resId, parent), DraggableViewHolder {
	init {
		itemView.setOnClickListener { _ ->
			if (adapter.isSelecting) {
				adapter.startDrag(this)
			} else {
				item?.let {
					adapter.openNote(it, sharedElement = itemView)
				}
			}
		}
		itemView.setOnLongClickListener {
			adapter.startDrag(this)
			true
		}
	}
	
	override fun canMove(): Boolean = true
	
	override fun canDrop(targetViewHolder: DraggableViewHolder): Boolean =
			targetViewHolder is NoteViewHolder
	
	override fun dragFinished(moved: Boolean) {
		if (moved) {
			adapter.controller.saveOrder(adapter.items.mapNotNull { it as? Note })
			adapter.clearSelection()
		}
	}
}
