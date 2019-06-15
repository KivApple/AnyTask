package id.pineapple.anytask.notes

import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import id.pineapple.anytask.R
import id.pineapple.anytask.UiHelper
import id.pineapple.anytask.fixLandscapeHeight
import kotlinx.android.synthetic.main.dialog_notes_tree.*
import java.util.*

abstract class NoteTreeDialogFragment: BottomSheetDialogFragment() {
	protected lateinit var model: NoteListPersistentModel
		private set
	protected lateinit var controller: NoteListController
		private set
	private lateinit var selectedPath: Array<Note>
	private var adapter: NoteTreeAdapter? = null
	private var savedAdapterState: Bundle? = null
	val selectedNoteId: Long?
		get() = if (adapter != null)
			adapter?.selectedItemId
		else
			selectedPath.lastOrNull()?.id
	val selectedNote: Note?
		get() = adapter?.selectedItem
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		arguments!!.let {
			selectedPath = it.getParcelableArrayList<Note>(ARG_SELECTED_PATH)!!.toTypedArray()
		}
		savedAdapterState = savedInstanceState?.getBundle(STATE_ADAPTER_DATA)
		model = NoteListPersistentModel()
		controller = NoteListController(model, context as UiHelper)
	}
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
			inflater.inflate(R.layout.dialog_notes_tree, container, false)
	
	protected open val showOnlyFolders: Boolean = true
	
	protected open val canSelectFolders: Boolean = true
	
	protected open val hiddenIds: Set<Long> = emptySet()
	
	protected open val positiveButtonTitle: String? = null
	
	protected open val negativeButtonTitle: String? = null
	
	open fun onPositiveButtonClicked() {
	}
	
	open fun onNegativeButtonClicked() {
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		fixLandscapeHeight()
		adapter = NoteTreeAdapter(
				context!!,
				hiddenIds,
				showOnlyFolders,
				canSelectFolders,
				selectedPath,
				savedAdapterState
		)
		notes_tree_recycler_view.adapter = adapter
		notes_tree_recycler_view.setHasFixedSize(true)
		(notes_tree_recycler_view.layoutManager as LinearLayoutManager).isAutoMeasureEnabled = true
		notes_tree_recycler_view.isNestedScrollingEnabled = true
		positiveButtonTitle?.let { title ->
			select_button.text = title
			select_button.setOnClickListener {
				dismiss()
				onPositiveButtonClicked()
			}
		}
		negativeButtonTitle?.let { title ->
			move_cancel_button.text = title
			move_cancel_button.setOnClickListener {
				dismiss()
				onNegativeButtonClicked()
			}
		}
	}
	
	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		adapter?.let { adapter ->
			savedAdapterState = adapter.saveInstanceState()
			outState.putBundle(STATE_ADAPTER_DATA, savedAdapterState)
		}
	}
	
	override fun onDestroyView() {
		adapter = null
		super.onDestroyView()
	}
	
	companion object {
		private const val ARG_SELECTED_PATH = "selected_path"
		
		private const val STATE_ADAPTER_DATA = "adapter_data"
		
		@JvmStatic
		fun makeArguments(selectedPath: Array<Note>): Bundle = Bundle().apply {
			putParcelableArrayList(ARG_SELECTED_PATH, selectedPath.toMutableList() as ArrayList)
		}
	}
}
