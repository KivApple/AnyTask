package id.pineapple.anytask.notes

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer

interface NoteListModel {
	fun observe(folder: Long?, lifecycleOwner: LifecycleOwner, observer: Observer<List<Note>>)
	
	fun fetch(parentId: Long?, callback: (notes: List<Note>) -> Unit)
	
	fun fetch(id: Long, callback: (note: Note?) -> Unit)
	
	fun insert(vararg notes: Note, callback: ((ids: List<Long>) -> Unit)? = null)
	
	fun update(vararg notes: Note, callback: (() -> Unit)? = null)
	
	fun delete(vararg notes: Note, callback: (() -> Unit)? = null)
}
