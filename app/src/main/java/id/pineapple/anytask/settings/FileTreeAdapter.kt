package id.pineapple.anytask.settings

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import id.pineapple.anytask.R
import id.pineapple.recyclerviewutil.RecyclerViewTreeAdapter
import id.pineapple.anytask.resolveColor
import java.io.File
import java.util.*

class FileTreeAdapter(
		private val context: Context,
		rootDir: File,
		savedInstanceState: Bundle? = null
) : RecyclerViewTreeAdapter<FileTreeAdapter.FileInfo, FileTreeAdapter.ViewHolder>() {
	var onFileSelectedListener: OnFileSelectedListener? = null
	var prevSelectedPosition = -1
	var selectedFile: File? = null
	
	init {
		if (savedInstanceState != null) {
			restoreInstanceState(savedInstanceState)
		} else {
			setRoot(FileInfo(
					rootDir.absolutePath,
					rootDir.absolutePath,
					true
			)).expand()
		}
	}
	
	override fun saveInstanceState(): Bundle = super.saveInstanceState().apply {
		putString("selected_file", selectedFile?.absolutePath)
		putInt("prev_selected_position", prevSelectedPosition)
	}
	
	override fun restoreInstanceState(savedState: Bundle) {
		super.restoreInstanceState(savedState)
		savedState.getString("selected_file").let {
			selectedFile = if (it != null) File(it) else null
			prevSelectedPosition = savedState.getInt("prev_selected_position")
		}
	}
	
	override fun onCreateViewHolder(parent: ViewGroup, type: Int): ViewHolder =
			ViewHolder(this, parent)
	
	override fun onBindViewHolder(viewHolder: ViewHolder, node: TreeNode<FileInfo>) =
			viewHolder.bind(node)
	
	override fun loadChildren(node: FileInfo, callback: (children: Collection<FileInfo>) -> Unit) {
		try {
			if (node.isDirectory) {
				callback(File(node.path).listFiles().sortedWith(Comparator { a, b ->
					if (a.isDirectory != b.isDirectory)
						if (a.isDirectory) -1 else 1
					else
						a.name.compareTo(b.name)
				}).map {
					FileInfo(
							it.absolutePath,
							it.name,
							it.isDirectory
					)
				})
				return
			}
		} catch (e: Throwable) {
		}
		callback(emptyList())
	}
	
	interface OnFileSelectedListener {
		fun onFileSelected(file: File)
	}
	
	class ViewHolder(
			private val adapter: FileTreeAdapter,
			parent: ViewGroup
	) : RecyclerView.ViewHolder(LayoutInflater.from(adapter.context).inflate(
			R.layout.tree_item_file, parent, false
	)) {
		private val iconImageView: ImageView = itemView.findViewById(R.id.file_icon)
		private val nameTextView: TextView = itemView.findViewById(R.id.file_name)
		private var node: TreeNode<FileInfo>? = null
		
		init {
			itemView.setOnClickListener {
				node?.let { node ->
					adapter.selectedFile = File(node.data.path)
					if (adapter.prevSelectedPosition >= 0) {
						adapter.notifyItemChanged(adapter.prevSelectedPosition)
					}
					node.toggleExpanded()
					adapter.onFileSelectedListener?.onFileSelected(adapter.selectedFile!!)
					adapter.prevSelectedPosition = node.index
				}
			}
		}
		
		fun bind(node: TreeNode<FileInfo>) {
			this.node = null
			val indent = adapter.context.resources.getDimension(R.dimen.noteFolderTreeIndent) * node.level
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
				itemView.setPaddingRelative(indent.toInt(), 0, 0, 0)
			} else {
				itemView.setPadding(indent.toInt(), 0, 0, 0)
			}
			nameTextView.text = node.data.name
			iconImageView.setImageResource(
					if (node.data.isDirectory)
						if (node.expanded)
							R.drawable.ic_folder_open_black_24dp
						else
							R.drawable.ic_folder_black_24dp
					else
						R.drawable.ic_insert_drive_file_black_24dp
			)
			nameTextView.setBackgroundColor(
					if (adapter.selectedFile?.absolutePath == node.data.path)
						resolveColor(adapter.context, R.attr.selectedBackgroundColor)
					else
						0
			)
			this.node = node
		}
	}
	
	data class FileInfo(
			val path: String,
			val name: String,
			val isDirectory: Boolean
	) : Parcelable {
		override fun describeContents(): Int = 0
		
		override fun writeToParcel(parcel: Parcel, flags: Int) {
			parcel.writeString(path)
			parcel.writeString(name)
			parcel.writeByte(if (isDirectory) 1 else 0)
		}
		
		companion object CREATOR : Parcelable.Creator<FileInfo> {
			override fun createFromParcel(parcel: Parcel): FileInfo = FileInfo(
					parcel.readString()!!,
					parcel.readString()!!,
					parcel.readByte() != 0.toByte()
			)
			
			override fun newArray(size: Int): Array<FileInfo?> = arrayOfNulls(size)
		}
	}
}
