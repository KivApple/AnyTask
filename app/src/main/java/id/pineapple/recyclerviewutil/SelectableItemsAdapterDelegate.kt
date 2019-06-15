package id.pineapple.recyclerviewutil

import android.os.Bundle

interface SelectableItemsAdapterDelegate {
	var selectableItemsAdapter: SelectableItemsAdapter
	
	val selectedItems: Collection<UniqueEntity>
	
	val selectedItemIds: Collection<Long>
	
	val isSelecting: Boolean
	
	fun restoreInstanceState(savedState: Bundle?, callNotifyDataSetChanged: Boolean = true)
	
	fun saveInstanceState(outState: Bundle)
	
	fun addOnSelectionChangedListener(listener: OnSelectionChangedListener)
	
	fun removeOnSelectionChangedListener(listener: OnSelectionChangedListener)
	
	fun updateSelectedItems(callNotifyDataSetChanged: Boolean)
	
	fun clearSelection()
	
	fun isItemSelected(position: Int): Boolean
	
	fun selectItem(position: Int, callNotifyDataSetChanged: Boolean = true)
	
	fun deselectItem(position: Int, callNotifyDataSetChanged: Boolean = true)
	
	fun toggleItemSelection(position: Int, callNotifyDataSetChanged: Boolean = true)
	
	interface OnSelectionChangedListener {
		fun onSelectionChanged(selectedItems: Collection<UniqueEntity>)
	}
}
