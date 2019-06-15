package id.pineapple.recyclerviewutil

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.support.v7.widget.RecyclerView

abstract class RecyclerViewTreeAdapter<T: Parcelable?, VH: RecyclerView.ViewHolder>:
		RecyclerView.Adapter<VH>() {
	private val items = mutableListOf<TreeNode<T>>()
	
	open fun restoreInstanceState(savedState: Bundle) {
		val data = savedState.getParcelableArrayList<T>("data")!!
		val infos = savedState.getParcelableArrayList<TreeNodeInfo>("info")!!
		items.clear()
		TreeNode.restoreTo(this, data, infos)
	}
	
	open fun saveInstanceState(): Bundle = Bundle().apply {
		putParcelableArrayList("data", items.map {
			it.data
		} as ArrayList<out Parcelable?>)
		putParcelableArrayList("info", items.map {
			TreeNodeInfo(
					if (it.expanded && it.childrenLoaded) it.children.size else 0,
					it.expanded && it.childrenLoaded
			)
		} as ArrayList<out Parcelable>)
	}
	
	fun setRoot(data: T): TreeNode<T> {
		items.clear()
		items.add(TreeNode(this, data, null))
		notifyDataSetChanged()
		return items.first()
	}
	
	override fun getItemCount(): Int = items.size
	
	abstract fun loadChildren(node: T, callback: (children: Collection<T>) -> Unit)
	
	override fun getItemId(position: Int): Long = getItemId(items[position].data)
	
	override fun getItemViewType(position: Int): Int = getItemViewType(items[position].data)
	
	override fun onBindViewHolder(viewHolder: VH, position: Int) =
			onBindViewHolder(viewHolder, items[position])

	open fun getItemId(node: T): Long = -1L
	
	open fun getItemViewType(node: T): Int = 0
	
	abstract fun onBindViewHolder(viewHolder: VH, node: TreeNode<T>)
	
	class TreeNode<T: Parcelable?>(
			val adapter: RecyclerViewTreeAdapter<T, *>,
			val data: T,
			val parent: TreeNode<T>? = null
	) {
		var index = 0
			private set
		var indexInParent = 0
			private set
		var expanded = false
			private set
		var childrenLoaded = false
			private set
		var children: List<TreeNode<T>> = emptyList()
			private set
		private var totalChildCount = 0
		val level: Int = parent?.level?.plus(1) ?: 0
		
		fun expand(callback: (() -> Unit)? = null) {
			if (expanded) return
			expanded = true
			if (!childrenLoaded) {
				adapter.loadChildren(data) { children ->
					if (expanded) {
						childrenLoaded = true
						this.children = children.map {
							TreeNode(adapter, it, this)
						}
						this.children.forEachIndexed { index, child ->
							child.indexInParent = index
						}
						showChildren()
						callback?.invoke()
					}
				}
			} else {
				showChildren()
				callback?.invoke()
			}
		}
		
		fun collapse() {
			if (!expanded) return
			expanded = false
			hideChildren()
			children = emptyList()
			childrenLoaded = false
		}
		
		fun toggleExpanded() {
			if (expanded)
				collapse()
			else
				expand()
		}
		
		private fun showChildren() {
			if (parent?.expanded == false) return
			totalChildCount = children.size
			children.forEachIndexed { childIndex, child ->
				child.index = index + 1 + childIndex
			}
			adapter.items.addAll(index + 1, children)
			parent?.childSizeChanged(indexInParent, children.size)
			adapter.notifyItemChanged(index)
			adapter.notifyItemRangeInserted(index + 1, children.size)
			adapter.notifyDataSetChanged()
		}
		
		private fun hideChildren() {
			if (parent?.expanded == false) return
			val removedChildCount = totalChildCount
			while (totalChildCount > 0 && (index + 1) <= adapter.items.lastIndex) {
				adapter.items.removeAt(index + 1)
				totalChildCount--
			}
			parent?.childSizeChanged(indexInParent, -children.size)
			adapter.notifyItemChanged(index)
			adapter.notifyItemRangeRemoved(index + 1, removedChildCount)
		}
		
		private fun childSizeChanged(index: Int, delta: Int) {
			totalChildCount += delta
			children.drop(index + 1).forEach {
				it.indexChanged(delta)
			}
			parent?.childSizeChanged(indexInParent, delta)
		}
		
		private fun indexChanged(delta: Int) {
			index += delta
			children.forEach {
				it.indexChanged(delta)
			}
		}
		
		companion object {
			internal fun <T: Parcelable?>restoreTo(
					adapter: RecyclerViewTreeAdapter<T, *>,
					data: List<T>,
					infos: List<TreeNodeInfo>,
					parentIndex: Int = -1,
					index: Int = 0
			) {
				val parent = if (parentIndex >= 0) adapter.items[parentIndex] else null
				val node = TreeNode(adapter, data[index], parent)
				node.index = index
				node.expanded = infos[index].expanded
				adapter.items.add(node)
				if (node.expanded) {
					node.childrenLoaded = true
					var childIndex = index + 1
					node.children = (0 until infos[index].childCount).map {
						restoreTo(adapter, data, infos, index, childIndex)
						val child = adapter.items[childIndex]
						childIndex += 1 + child.totalChildCount
						child.indexInParent = it
						child
					}
					node.totalChildCount = childIndex - (index + 1)
				}
			}
		}
	}
	
	class TreeNodeInfo(
			val childCount: Int,
			val expanded: Boolean
	) : Parcelable {
		override fun describeContents(): Int = 0
		
		override fun writeToParcel(parcel: Parcel, flags: Int) {
			parcel.writeInt(childCount)
			parcel.writeByte(if (expanded) 1 else 0)
		}
		
		companion object CREATOR : Parcelable.Creator<TreeNodeInfo> {
			override fun createFromParcel(parcel: Parcel): TreeNodeInfo = TreeNodeInfo(
					parcel.readInt(),
					parcel.readByte() != 0.toByte()
			)
			
			override fun newArray(size: Int): Array<TreeNodeInfo?> = arrayOfNulls(size)
		}
	}
}
