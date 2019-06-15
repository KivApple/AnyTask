package id.pineapple.anytask.notes

import android.os.Bundle
import android.os.Parcelable
import id.pineapple.anytask.R
import id.pineapple.recyclerviewutil.UniqueEntity
import java.util.*

class CopyNotesToFolderDialogFragment: NoteTreeDialogFragment() {
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
		get() = getString(R.string.copy)
	
	override val negativeButtonTitle: String?
		get() = getString(R.string.cancel)
	
	override fun onPositiveButtonClicked() {
		controller.copyToFolder(*entities.toTypedArray(), folderId = selectedNoteId)
	}
	
	companion object {
		private const val ARG_ENTITIES = "entities"
		
		@JvmStatic
		fun newInstance(
				entities: List<UniqueEntity>,
				selectedPath: Array<Note>
		): CopyNotesToFolderDialogFragment =
				CopyNotesToFolderDialogFragment().apply {
					arguments = NoteTreeDialogFragment.makeArguments(selectedPath).apply {
						putParcelableArrayList(ARG_ENTITIES,
								entities.toMutableList() as ArrayList<out Parcelable>)
					}
				}
	}
}
