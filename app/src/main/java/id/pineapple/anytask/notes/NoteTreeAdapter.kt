package id.pineapple.anytask.notes

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import id.pineapple.anytask.R
import id.pineapple.recyclerviewutil.RecyclerViewTreeAdapter
import id.pineapple.anytask.resolveColor

class NoteTreeAdapter(
		private val context: Context,
		private val folderIdsBlacklist: Set<Long>,
		private val showOnlyFolders: Boolean,
		val canSelectFolders: Boolean,
		selectedPath: Array<Note>? = null,
		savedInstanceState: Bundle? = null
): RecyclerViewTreeAdapter<Note?, NoteTreeAdapter.ViewHolder>() {
	private val model: NoteListModel = NoteListPersistentModel()
	var prevSelectedPosition: Int = -1
	var selectedItemId: Long? = null
		private set
	var selectedItem: Note? = null
		private set
	
	init {
		setHasStableIds(true)
		if (savedInstanceState != null) {
			restoreInstanceState(savedInstanceState)
		} else {
			if (selectedPath == null) {
				setRoot(null).expand()
			} else {
				var offset = 0
				var node: TreeNode<Note?>? = setRoot(null)
				var callback: () -> Unit = {}
				callback = {
					if (offset < selectedPath.size) {
						node = node?.children?.firstOrNull {
							it.data?.id == selectedPath[offset].id
						}
						offset++
						node?.expand(callback)
					} else {
						selectedItemId = selectedPath.lastOrNull()?.id
						selectedItem = selectedPath.lastOrNull()
					}
				}
				node?.expand(callback)
			}
		}
	}
	
	override fun restoreInstanceState(savedState: Bundle) {
		super.restoreInstanceState(savedState)
		selectedItemId = savedState.getLong("selected_item_id").let {
			if (it > 0) it else null
		}
		selectedItem = savedState.getParcelable("selected_item")
	}
	
	override fun saveInstanceState(): Bundle =
			super.saveInstanceState().apply {
				putLong("selected_item_id", selectedItemId ?: 0L)
				putParcelable("selected_item", selectedItem)
			}
	
	override fun getItemId(node: Note?): Long = node?.id ?: 0L
	
	override fun onCreateViewHolder(parent: ViewGroup, type: Int): ViewHolder =
			ViewHolder(this, parent)
	
	override fun onBindViewHolder(viewHolder: ViewHolder, node: TreeNode<Note?>) =
			viewHolder.bind(node)
	
	override fun loadChildren(node: Note?, callback: (children: Collection<Note?>) -> Unit) =
			model.fetch(node?.id) { folders ->
				callback(folders.filter {
					!showOnlyFolders ||
							!folderIdsBlacklist.contains(it.id) && it.type == Note.Type.FOLDER
				})
			}
	
	class ViewHolder(
			private val adapter: NoteTreeAdapter,
			parent: ViewGroup
	): RecyclerView.ViewHolder(LayoutInflater.from(adapter.context).inflate(
			R.layout.tree_item_note_folder, parent, false
	)) {
		private val iconImageView: ImageView = itemView.findViewById(R.id.note_folder_icon)
		private val titleTextView: TextView = itemView.findViewById(R.id.note_folder_title)
		private var node: TreeNode<Note?>? = null
		
		init {
			itemView.setOnClickListener {
				node?.let { node ->
					if (node.data?.type != Note.Type.FOLDER || adapter.canSelectFolders) {
						adapter.selectedItemId = node.data?.id
						adapter.selectedItem = node.data
						if (adapter.prevSelectedPosition >= 0) {
							adapter.notifyItemChanged(adapter.prevSelectedPosition)
						}
						node.toggleExpanded()
						adapter.prevSelectedPosition = node.index
					} else {
						adapter.selectedItemId = null
						adapter.selectedItem = null
						if (adapter.prevSelectedPosition >= 0) {
							adapter.notifyItemChanged(adapter.prevSelectedPosition)
						}
						node.toggleExpanded()
						adapter.prevSelectedPosition = node.index
					}
				}
			}
		}
		
		fun bind(node: TreeNode<Note?>) {
			this.node = null
			val indent = adapter.context.resources.getDimension(R.dimen.noteFolderTreeIndent) * node.level
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
				itemView.setPaddingRelative(indent.toInt(), 0, 0, 0)
			} else {
				itemView.setPadding(indent.toInt(), 0, 0, 0)
			}
			iconImageView.setImageResource(
					when {
						node.data?.type == Note.Type.LIST -> R.drawable.ic_view_list_black_24dp
						node.data?.type != Note.Type.FOLDER -> R.drawable.ic_insert_drive_file_black_24dp
						node.expanded -> R.drawable.ic_folder_open_black_24dp
						else -> R.drawable.ic_folder_black_24dp
					}
			)
			titleTextView.text = node.data?.getNormalizedTitle(adapter.context) ?:
					adapter.context.getString(R.string.home_folder)
			titleTextView.setBackgroundColor(
					if (adapter.selectedItemId == node.data?.id &&
							(node.data != null || adapter.canSelectFolders))
						resolveColor(adapter.context, R.attr.selectedBackgroundColor)
					else
						0
			)
			this.node = node
		}
	}
}
