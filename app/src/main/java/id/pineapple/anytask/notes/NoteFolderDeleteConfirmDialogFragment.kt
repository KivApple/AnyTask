package id.pineapple.anytask.notes

import android.os.Bundle
import android.os.Parcelable
import android.support.design.widget.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import id.pineapple.anytask.R
import id.pineapple.anytask.UiHelper
import id.pineapple.anytask.fixLandscapeHeight
import id.pineapple.recyclerviewutil.UniqueEntity
import kotlinx.android.synthetic.main.dialog_confirm_note_folder_delete.*
import java.util.*

class NoteFolderDeleteConfirmDialogFragment : BottomSheetDialogFragment() {
	private lateinit var entities: List<UniqueEntity>
	private lateinit var model: NoteListPersistentModel
	private lateinit var controller: NoteListController
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		arguments!!.let {
			entities = it.getParcelableArrayList(ARG_ENTITIES)!!
		}
		model = NoteListPersistentModel()
		controller = NoteListController(model, context as UiHelper)
	}
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
			inflater.inflate(R.layout.dialog_confirm_note_folder_delete, container, false)
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		fixLandscapeHeight()
		note_confirm_delete_button.setOnClickListener {
			dismiss()
			controller.forceDelete(*entities.toTypedArray())
		}
		note_cancel_delete_button.setOnClickListener {
			dismiss()
		}
	}
	
	companion object {
		private const val ARG_ENTITIES = "entities"
		
		@JvmStatic
		fun newInstance(entities: List<UniqueEntity>): NoteFolderDeleteConfirmDialogFragment =
				NoteFolderDeleteConfirmDialogFragment().apply {
					arguments = Bundle().apply {
						putParcelableArrayList(ARG_ENTITIES,
								entities.toMutableList() as ArrayList<out Parcelable>)
					}
				}
	}
}
