package id.pineapple.anytask.notes

import android.view.View
import id.pineapple.anytask.R
import id.pineapple.anytask.UiHelper
import id.pineapple.recyclerviewutil.UniqueEntity

class NoteListController(
		private val model: NoteListModel,
		private val uiHelper: UiHelper
) {
	fun newNoteFolder(parentId: Long? = null, position: Int = 0) {
		model.insert(Note(
				type = Note.Type.FOLDER,
				title = uiHelper.getString(R.string.new_folder),
				parentId = parentId,
				position = position
		))
	}
	
	fun newNote(type: Note.Type, notePath: Array<Note> = emptyArray(), sharedElement: View? = null) {
		uiHelper.showNote(*(notePath + Note(
				type = type,
				parentId = notePath.lastOrNull()?.id,
				position = 0
		)), sharedElement = sharedElement)
	}
	
	fun saveNote(note: Note) {
		if (note.id != null) {
			model.update(note)
		} else {
			model.insert(note)
		}
	}
	
	fun saveOrder(notes: List<Note>) {
		var expectedPosition = 1
		val modifiedNotes = mutableListOf<Note>()
		notes.forEach { note ->
			if (note.position != expectedPosition) {
				modifiedNotes.add(note.updatePosition(expectedPosition))
			}
			expectedPosition++
		}
		if (modifiedNotes.isNotEmpty()) {
			model.update(*modifiedNotes.toTypedArray())
		}
	}
	
	fun askMoveToFolder(vararg entities: UniqueEntity, folderPath: Array<Note> = emptyArray()) {
		uiHelper.showMoveNotesToFolderDialog(*entities, selectedPath = folderPath)
	}
	
	fun askCopyToFolder(vararg entities: UniqueEntity, folderPath: Array<Note> = emptyArray()) {
		uiHelper.showCopyNotesToFolderDialog(*entities, selectedPath = folderPath)
	}
	
	fun moveToFolder(vararg entities: UniqueEntity, folderId: Long?) {
		val modifiedCount = entities.count {
			when (it) {
				is Note -> it.parentId != folderId
				else -> false
			}
		}
		if (modifiedCount == 0) return
		model.update(*entities.map {
			if (it.id == folderId)
				throw IllegalArgumentException("Cannot move folder to itself")
			(it as Note).updateParentId(folderId)
		}.toTypedArray())
		uiHelper.showSnackBar(
				uiHelper.getQuantityString(R.plurals.elements_moved, entities.size, entities.size),
				UiHelper.PopupDuration.VERY_LONG,
				uiHelper.getString(R.string.cancel)
		) {
			model.update(*entities.mapNotNull { it as? Note }.toTypedArray())
		}
	}
	
	fun copyToFolder(vararg entities: UniqueEntity, folderId: Long?) {
		model.insert(*entities.map {
			if (it is Note && it.type != Note.Type.FOLDER)
				it.updateParentId(folderId).copy(id = null)
			else
				throw IllegalArgumentException("Cannot copy folder")
		}.toTypedArray()) { ids ->
			uiHelper.showSnackBar(
					uiHelper.getQuantityString(R.plurals.notes_copied, entities.size, entities.size),
					UiHelper.PopupDuration.VERY_LONG,
					uiHelper.getString(R.string.cancel)
			) {
				model.delete(*ids.mapIndexed { index, id ->
					(entities[index] as Note).copy(id = id)
				}.toTypedArray())
			}
		}
	}
	
	fun delete(vararg entities: UniqueEntity) {
		val notes = entities.filter { it is Note } as List<Note>
		if (notes.any { it.type == Note.Type.FOLDER }) {
			uiHelper.showNoteFolderDeleteConfirmDialog(*entities)
		} else {
			model.delete(*notes.toTypedArray())
			uiHelper.showSnackBar(
					if (notes.size == 1)
						uiHelper.getString(R.string.note_deleted)
					else
						uiHelper.getQuantityString(R.plurals.notes_deleted, notes.size, notes.size),
					UiHelper.PopupDuration.VERY_LONG,
					uiHelper.getString(R.string.recover)
			) {
				model.insert(*notes.toTypedArray())
			}
		}
	}
	
	fun forceDelete(vararg entities: UniqueEntity) {
		val notes = entities.filter { it is Note } as List<Note>
		model.delete(*notes.toTypedArray())
	}
}
