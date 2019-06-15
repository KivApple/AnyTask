package id.pineapple.anytask.tasks

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import id.pineapple.anytask.R
import id.pineapple.anytask.UiHelper
import id.pineapple.anytask.daysToLocalDate
import id.pineapple.anytask.localDateToDays
import org.joda.time.LocalDate

class TaskPriorityDialogFragment : DialogFragment() {
	private lateinit var date: LocalDate
	private lateinit var task: TaskInfo
	private lateinit var model: TaskListModel
	private lateinit var controller: TaskListController
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		arguments!!.let {
			date = daysToLocalDate(it.getInt(ARG_DATE))
			task = it.getParcelable(ARG_TASK)!!
		}
		model = TaskListPersistentModel(date)
		controller = TaskListController(model, this, context as UiHelper)
	}
	
	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
			AlertDialog.Builder(context)
					.setTitle(R.string.task_priority)
					.setSingleChoiceItems(R.array.task_priority_names, task.task.priority) { _, index ->
						controller.setPriority(task, index)
						dismiss()
					}
					.create()
	
	companion object {
		private const val ARG_DATE = "date"
		private const val ARG_TASK = "task"
		
		@JvmStatic
		fun newInstance(task: TaskInfo, date: LocalDate) =
				TaskPriorityDialogFragment().apply {
					arguments = Bundle().apply {
						putParcelable(ARG_TASK, task)
						putInt(ARG_DATE, localDateToDays(date))
					}
				}
	}
}
