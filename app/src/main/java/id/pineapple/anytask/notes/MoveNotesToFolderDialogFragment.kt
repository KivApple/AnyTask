package id.pineapple.anytask.notes

import android.os.Bundle
import android.os.Parcelable
import id.pineapple.anytask.R
import id.pineapple.recyclerviewutil.UniqueEntity
import java.util.*

class MoveNotesToFolderDialogFragment: NoteTreeDialogFragment() {
	private lateinit var entities: List<UniqueEntity>
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		arguments!!.let {
			entities = it.getParcelableArrayList(ARG_ENTITIES)!!
		}
	}
	
	override val hiddenIds: Set<Long>
		get() = entities.mapNotNull { (it as? Note)?.id }.toSet()
	
	override val positiveButtonTitle: String?
		get() = getString(R.string.move)
	
	override val negativeButtonTitle: String?
		get() = getString(R.string.cancel)
	
	override fun onPositiveButtonClicked() {
		controller.moveToFolder(*entities.toTypedArray(), folderId = selectedNoteId)
	}
	
	companion object {
		private const val ARG_ENTITIES = "entities"
		
		@JvmStatic
		fun newInstance(
				entities: List<UniqueEntity>,
				selectedPath: Array<Note>
		): MoveNotesToFolderDialogFragment =
				MoveNotesToFolderDialogFragment().apply {
					arguments = NoteTreeDialogFragment.makeArguments(selectedPath).apply {
						putParcelableArrayList(ARG_ENTITIES,
								entities.toMutableList() as ArrayList<out Parcelable>)
					}
				}
	}
}
