package id.pineapple.anytask.tasks

import android.os.Bundle
import android.os.Parcelable
import id.pineapple.anytask.R
import org.joda.time.LocalDate
import java.util.*

class TaskRescheduleDialogFragment : TaskCalendarDialogFragment() {
	private lateinit var rescheduleTaskList: List<TaskInfo>
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		arguments!!.let {
			rescheduleTaskList = it.getParcelableArrayList(ARG_RESCHEDULE_TASK_LIST)!!
		}
	}
	
	override val negativeButtonText: String?
		get() = getString(R.string.cancel)
	
	override val positiveButtonText: String?
		get() = if (rescheduleTaskList.size == 1)
			getString(R.string.reschedule_task)
		else
			context!!.resources.getQuantityString(R.plurals.reschedule_tasks,
					rescheduleTaskList.size, rescheduleTaskList.size)
	
	override fun onPositiveButtonClicked() {
		controller.reschedule(*rescheduleTaskList.toTypedArray(), newDate = selectedDate)
	}
	
	override fun onDateClicked(date: LocalDate) {
	}
	
	companion object {
		private const val ARG_RESCHEDULE_TASK_LIST = "reschedule_task_list"
		
		@JvmStatic
		fun newInstance(rescheduleTaskList: List<TaskInfo>, date: LocalDate) =
				TaskRescheduleDialogFragment().apply {
					arguments = TaskCalendarDialogFragment.makeArguments(date).apply {
						putParcelableArrayList(ARG_RESCHEDULE_TASK_LIST,
								rescheduleTaskList.toMutableList() as ArrayList<out Parcelable>)
					}
				}
	}
}
