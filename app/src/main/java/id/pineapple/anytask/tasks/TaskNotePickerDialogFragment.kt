package id.pineapple.anytask.tasks

import android.os.Bundle
import id.pineapple.anytask.R
import id.pineapple.anytask.UiHelper
import id.pineapple.anytask.daysToLocalDate
import id.pineapple.anytask.localDateToDays
import id.pineapple.anytask.notes.NoteTreeDialogFragment
import org.joda.time.LocalDate

class TaskNotePickerDialogFragment: NoteTreeDialogFragment() {
	private lateinit var taskInfo: TaskInfo
	private lateinit var date: LocalDate
	private lateinit var taskModel: TaskListModel
	private lateinit var taskController: TaskListController
	
	override val showOnlyFolders: Boolean = false
	
	override val canSelectFolders: Boolean get() = false
	
	override val positiveButtonTitle: String?
		get() = getString(R.string.select)
	
	override val negativeButtonTitle: String?
		get() = getString(R.string.clear)
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		arguments!!.let {
			taskInfo = it.getParcelable(ARG_TASK_INFO)!!
			date = daysToLocalDate(it.getInt(ARG_DATE))
		}
		taskModel = TaskListPersistentModel(date, reactive = false)
		taskController = TaskListController(taskModel, this, context as UiHelper,
				lightweight = true)
	}
	
	override fun onPositiveButtonClicked() {
		if (selectedNoteId != null) {
			taskController.attachNote(taskInfo, selectedNoteId)
		}
	}
	
	override fun onNegativeButtonClicked() {
		taskController.detachNote(taskInfo)
	}
	
	companion object {
		private const val ARG_TASK_INFO = "task_info"
		private const val ARG_DATE = "date"
		
		@JvmStatic
		fun newInstance(taskInfo: TaskInfo, date: LocalDate): TaskNotePickerDialogFragment =
				TaskNotePickerDialogFragment().apply {
					arguments = makeArguments(emptyArray()).apply {
						putParcelable(ARG_TASK_INFO, taskInfo)
						putInt(ARG_DATE, localDateToDays(date))
					}
				}
	}
}
