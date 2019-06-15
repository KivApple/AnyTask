package id.pineapple.recyclerviewutil

import android.os.Bundle

class SelectableItemsAdapterDelegateImpl: SelectableItemsAdapterDelegate {
	override lateinit var selectableItemsAdapter: SelectableItemsAdapter
	private var selectedItemsSet = mutableSetOf<UniqueEntity>()
	override val selectedItems: Collection<UniqueEntity> get() = selectedItemsSet
	private var selectedItemIdsSet = mutableSetOf<Long>()
	override val selectedItemIds: Collection<Long> get() = selectedItemIdsSet
	private val onSelectionChangedListeners = mutableSetOf<SelectableItemsAdapterDelegate.OnSelectionChangedListener>()
	override val isSelecting: Boolean get() = selectedItemIdsSet.isNotEmpty()
	
	override fun restoreInstanceState(savedState: Bundle?, callNotifyDataSetChanged: Boolean) {
		if (savedState == null) return
		selectedItemIdsSet = savedState.getLongArray("selection")!!.toMutableSet()
		selectedItemsSet = selectableItemsAdapter.items.asSequence().filterIndexed { index, _ ->
			selectedItemIdsSet.contains(selectableItemsAdapter.getItemId(index))
		}.toMutableSet()
		if (callNotifyDataSetChanged) {
			selectableItemsAdapter.notifyDataSetChanged()
		}
	}
	
	override fun saveInstanceState(outState: Bundle) {
		outState.putLongArray("selection", selectedItemIdsSet.toLongArray())
	}
	
	override fun addOnSelectionChangedListener(listener: SelectableItemsAdapterDelegate.OnSelectionChangedListener) {
		onSelectionChangedListeners.add(listener)
		listener.onSelectionChanged(selectedItems)
	}
	
	override fun removeOnSelectionChangedListener(listener: SelectableItemsAdapterDelegate.OnSelectionChangedListener) {
		onSelectionChangedListeners.remove(listener)
	}
	
	private fun notifySelectionChanged() {
		selectableItemsAdapter.onSelectionChanged()
		onSelectionChangedListeners.forEach {
			it.onSelectionChanged(selectedItems)
		}
	}
	
	override fun updateSelectedItems(callNotifyDataSetChanged: Boolean) {
		val oldSelectedCount = selectedItemIdsSet.size
		selectedItemIdsSet = selectableItemsAdapter.items.mapNotNull { it.id }.filter {
			selectedItemIdsSet.contains(it)
		}.toMutableSet()
		selectedItemsSet = selectedItemsSet.filter {
			selectedItemIdsSet.contains(it.id)
		}.toMutableSet()
		if (oldSelectedCount != selectedItemIdsSet.size) {
			if (callNotifyDataSetChanged) {
				notifySelectionChanged()
			}
			notifySelectionChanged()
		}
	}
	
	override fun clearSelection() {
		selectedItemsSet.clear()
		selectedItemIdsSet.clear()
		selectableItemsAdapter.notifyDataSetChanged()
		notifySelectionChanged()
	}
	
	override fun isItemSelected(position: Int) = selectedItemIdsSet.contains(selectableItemsAdapter.getItemId(position))
	
	override fun selectItem(position: Int, callNotifyDataSetChanged: Boolean) {
		selectedItemsSet.add(selectableItemsAdapter.items[position])
		selectedItemIdsSet.add(selectableItemsAdapter.getItemId(position))
		if (callNotifyDataSetChanged) {
			selectableItemsAdapter.notifyItemChanged(position)
		}
		notifySelectionChanged()
	}
	
	override fun deselectItem(position: Int, callNotifyDataSetChanged: Boolean) {
		selectedItemsSet.remove(selectableItemsAdapter.items[position])
		selectedItemIdsSet.remove(selectableItemsAdapter.getItemId(position))
		if (callNotifyDataSetChanged) {
			selectableItemsAdapter.notifyItemChanged(position)
		}
		notifySelectionChanged()
	}
	
	override fun toggleItemSelection(position: Int, callNotifyDataSetChanged: Boolean) {
		if (isItemSelected(position)) {
			deselectItem(position, callNotifyDataSetChanged)
		} else {
			selectItem(position, callNotifyDataSetChanged)
		}
	}
}
