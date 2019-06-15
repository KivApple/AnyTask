package id.pineapple.anytask.notes

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import android.content.Context
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.View
import id.pineapple.anytask.R
import id.pineapple.anytask.SimpleTextViewHolder
import id.pineapple.anytask.UiHelper
import id.pineapple.recyclerviewutil.*

open class NoteListAdapter(
		model: NoteListModel,
		private val notePath: Array<Note>,
		val context: Context,
		lifecycleOwner: LifecycleOwner,
		private val selectableItemsAdapterDelegate: SelectableItemsAdapterDelegate =
				SelectableItemsAdapterDelegateImpl(),
		private val draggableItemsAdapterDelegate: DraggableItemsAdapterDelegate =
				DraggableItemsAdapterDelegateImpl()
) : RecyclerViewAdapter(context), Observer<List<Note>>, SelectableItemsAdapter,
		SelectableItemsAdapterDelegate by selectableItemsAdapterDelegate,
		DraggableItemsAdapter,
		DraggableItemsAdapterDelegate by draggableItemsAdapterDelegate {
	val controller: NoteListController = NoteListController(model, context as UiHelper)
	val canCopySelection: Boolean
		get() = selectedItems.all { it is Note && it.type != Note.Type.FOLDER }
	
	init {
		selectableItemsAdapterDelegate.selectableItemsAdapter = this
		draggableItemsAdapterDelegate.draggableItemsAdapter = this
		registerViewType(ViewHolderFactory.create(Note::class.java, { _, container ->
			TextNoteViewHolder(this, container)
		}, { it.type == Note.Type.TEXT }))
		registerViewType(ViewHolderFactory.create(Note::class.java, { _, container ->
			ListNoteViewHolder(this, container)
		}, { it.type == Note.Type.LIST }))
		registerViewType(ViewHolderFactory.create(Note::class.java, { _, container ->
			NoteFolderViewHolder(this, container)
		}, { it.type == Note.Type.FOLDER }))
		registerViewType(SimpleTextViewHolder.Factory())
		model.observe(notePath.lastOrNull()?.id, lifecycleOwner, this)
	}
	
	override fun restoreInstanceState(savedState: Bundle?, callNotifyDataSetChanged: Boolean) {
		if (savedState != null) {
			super.restoreInstanceState(savedState, false)
			selectableItemsAdapterDelegate.restoreInstanceState(savedState)
			if (callNotifyDataSetChanged) {
				notifyDataSetChanged()
			}
		} else {
			items.clear()
			addHeaders(items)
			if (callNotifyDataSetChanged) {
				notifyDataSetChanged()
			}
		}
	}
	
	override fun saveInstanceState(outState: Bundle) {
		super.saveInstanceState(outState)
		selectableItemsAdapterDelegate.saveInstanceState(outState)
	}
	
	override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
		super.onAttachedToRecyclerView(recyclerView)
		draggableItemsAdapterDelegate.onAttachedToRecyclerView(recyclerView)
	}
	
	open fun addHeaders(items: MutableList<UniqueEntity>) {
	}
	
	override fun onChanged(notes: List<Note>?) {
		if (notes == null) return
		val newItems = mutableListOf<UniqueEntity>()
		addHeaders(newItems)
		if (notes.isNotEmpty()) {
			newItems.addAll(notes)
		} else {
			newItems.add(SimpleTextViewHolder.Marker(0, context.getString(R.string.no_notes)))
		}
		if (newItems != items) {
			items = newItems
			updateSelectedItems(false)
			notifyDataSetChanged()
		}
	}
	
	fun openNote(note: Note, sharedElement: View? = null) {
		//if (note.type == Note.Type.FOLDER) {
			(context as UiHelper).showNote(*(notePath + note), sharedElement = sharedElement)
		//} else {
		//	(context as UiHelper).showNote(note, sharedElement = sharedElement)
		//}
	}
	
	fun moveSelection() {
		controller.askMoveToFolder(*selectedItems.toTypedArray(), folderPath = notePath)
	}
	
	fun copySelection() {
		if (canCopySelection) {
			val items = selectedItems.toTypedArray()
			clearSelection()
			controller.askCopyToFolder(*items, folderPath = notePath)
		}
	}
	
	fun deleteSelection() {
		controller.delete(*selectedItems.toTypedArray())
	}
	
	override fun startDrag(viewHolder: BaseViewHolder<out UniqueEntity>) {
		if (isSelecting) {
			toggleItemSelection(viewHolder.adapterPosition)
		} else if (viewHolder is NoteViewHolder) {
			selectItem(viewHolder.adapterPosition, false)
			viewHolder.bind(items[viewHolder.adapterPosition] as Note)
			draggableItemsAdapterDelegate.startDrag(viewHolder)
		}
	}
}
