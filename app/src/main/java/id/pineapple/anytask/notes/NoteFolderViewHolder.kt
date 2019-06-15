package id.pineapple.anytask.notes

import android.support.v4.view.ViewCompat
import android.view.ViewGroup
import android.widget.TextView
import id.pineapple.anytask.R
import id.pineapple.anytask.resolveColor

class NoteFolderViewHolder(
		adapter: NoteListAdapter,
		parent: ViewGroup
): NoteViewHolder(
		adapter, R.layout.item_note_folder, parent
) {
	private val titleTextView: TextView = itemView.findViewById(R.id.note_folder_title_text_view)
	
	override fun doBind(item: Note, oldItem: Note?) {
		titleTextView.text = item.title
		itemView.setBackgroundColor(resolveColor(adapter.context,
				if (adapter.isItemSelected(adapterPosition))
					R.attr.selectedBackgroundColor
				else
					R.attr.normalBackgroundColor
		))
		ViewCompat.setTransitionName(itemView, "note:${item.id}")
	}
}
