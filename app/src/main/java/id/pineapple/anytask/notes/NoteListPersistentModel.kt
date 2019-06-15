package id.pineapple.anytask.notes

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import android.os.AsyncTask
import id.pineapple.anytask.AppDatabase
import id.pineapple.anytask.observeOnce

class NoteListPersistentModel : NoteListModel {
	private val dao = AppDatabase.instance.noteDao()
	
	override fun observe(folder: Long?, lifecycleOwner: LifecycleOwner, observer: Observer<List<Note>>) {
		dao.findByParentId(folder).observe(lifecycleOwner, observer)
	}
	
	override fun fetch(id: Long, callback: (note: Note?) -> Unit) {
		dao.findById(id).observeOnce(Observer { callback(it) })
	}
	
	override fun fetch(parentId: Long?, callback: (notes: List<Note>) -> Unit) {
		dao.findByParentId(parentId).observeOnce(Observer { callback(it ?: emptyList()) })
	}
	
	override fun insert(vararg notes: Note, callback: ((ids: List<Long>) -> Unit)?) {
		NoteInsertAsyncTask(dao, callback).execute(*notes)
	}
	
	override fun update(vararg notes: Note, callback: (() -> Unit)?) {
		NoteUpdateAsyncTask(dao, callback).execute(*notes)
	}
	
	override fun delete(vararg notes: Note, callback: (() -> Unit)?) {
		NoteDeleteAsyncTask(dao, callback).execute(*notes)
	}
	
	private class NoteInsertAsyncTask(
			private val dao: NoteDao,
			private val callback: ((ids: List<Long>) -> Unit)?
	) : AsyncTask<Note, Void?, List<Long>>() {
		override fun doInBackground(vararg params: Note): List<Long> =
				dao.insert(*params)
		
		override fun onPostExecute(result: List<Long>) {
			callback?.invoke(result)
		}
	}
	
	private class NoteUpdateAsyncTask(
			private val dao: NoteDao,
			private val callback: (() -> Unit)?
	) : AsyncTask<Note, Void?, Void?>() {
		override fun doInBackground(vararg params: Note): Void? {
			dao.update(*params)
			return null
		}
		
		override fun onPostExecute(result: Void?) {
			callback?.invoke()
		}
	}
	
	private class NoteDeleteAsyncTask(
			private val dao: NoteDao,
			private val callback: (() -> Unit)?
	) : AsyncTask<Note, Void?, Void?>() {
		override fun doInBackground(vararg params: Note): Void? {
			dao.delete(*params)
			return null
		}
		
		override fun onPostExecute(result: Void?) {
			callback?.invoke()
		}
	}
}
