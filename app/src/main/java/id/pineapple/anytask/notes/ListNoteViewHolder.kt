package id.pineapple.anytask.notes

import android.support.v4.view.ViewCompat
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import id.pineapple.anytask.R
import id.pineapple.anytask.resolveColor

class ListNoteViewHolder(
		adapter: NoteListAdapter,
		parent: ViewGroup
) : NoteViewHolder(adapter, R.layout.item_text_note, parent) {
	private val titleTextView: TextView = itemView.findViewById(R.id.note_item_title)
	private val contentTextView: TextView = itemView.findViewById(R.id.note_item_text)
	
	override fun doBind(item: Note, oldItem: Note?) {
		titleTextView.text = item.title
		titleTextView.visibility = if (item.title.isNotEmpty()) View.VISIBLE else View.GONE
		contentTextView.text = item.getList().filter { !it.task.completed }.joinToString("\n") {
			it.task.title
		}
		itemView.setBackgroundColor(resolveColor(adapter.context, when {
			adapter.isItemSelected(adapterPosition) -> R.attr.selectedBackgroundColor
			else -> R.attr.normalBackgroundColor
		}))
		ViewCompat.setTransitionName(itemView, "note:${item.id}")
	}
}
