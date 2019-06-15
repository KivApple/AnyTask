package id.pineapple.anytask.notes

import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import id.pineapple.anytask.R
import id.pineapple.anytask.fixLandscapeHeight
import id.pineapple.anytask.tasks.TaskListInterface

class NoteDialogFragment: BottomSheetDialogFragment(), TaskListInterface {
	private lateinit var note: Note
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		arguments!!.let {
			note = it.getParcelable(ARG_NOTE)!!
		}
	}
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
			inflater.inflate(R.layout.dialog_note, container, false)
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		fixLandscapeHeight()
		if (savedInstanceState == null) {
			val fragment = NoteFragment.newInstance(note, isDialog = true)
			childFragmentManager.beginTransaction()
					.add(R.id.dialog_note_content, fragment, "note_fragment")
					.commit()
		}
	}
	
	override fun editTaskTitle(id: Long) {
		(childFragmentManager.findFragmentByTag("note_fragment") as? NoteFragment)?.editTaskTitle(id)
	}
	
	override fun finishTaskEdit() {
		(childFragmentManager.findFragmentByTag("note_fragment") as? NoteFragment)?.finishTaskEdit()
	}
	
	companion object {
		private const val ARG_NOTE = "note"
		
		@JvmStatic
		fun newInstance(note: Note): NoteDialogFragment =
				NoteDialogFragment().apply {
					arguments = Bundle().apply {
						putParcelable(ARG_NOTE, note)
					}
				}
	}
}
