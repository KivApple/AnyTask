package id.pineapple.anytask.tasks

import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import id.pineapple.anytask.*
import kotlinx.android.synthetic.main.dialog_task_alarm_options.*
import org.joda.time.LocalDate
import org.joda.time.LocalTime

class TaskAlarmOptionsDialogFragment : BottomSheetDialogFragment() {
	private lateinit var task: TaskInfo
	private lateinit var date: LocalDate
	private lateinit var model: TaskListPersistentModel
	private lateinit var controller: TaskListController
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		arguments!!.let {
			task = it.getParcelable(ARG_TASK)!!
			date = daysToLocalDate(it.getInt(ARG_DATE))
		}
		model = TaskListPersistentModel(date)
		controller = TaskListController(model, this, context as UiHelper,
				lightweight = true)
	}
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
			inflater.inflate(R.layout.dialog_task_alarm_options, container, false)
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		fixLandscapeHeight()
		task.task.alarmTime?.let {
			task_alarm_time_picker.currentHour = it.hourOfDay
			task_alarm_time_picker.currentMinute = it.minuteOfHour
		}
		if (task.task.alarmTime == null) {
			task_delete_alarm_button.visibility = View.GONE
		}
		task_alarm_set_button.setOnClickListener {
			dismiss()
			controller.setAlarm(task, LocalTime(
					task_alarm_time_picker.currentHour,
					task_alarm_time_picker.currentMinute
			))
		}
		task_delete_alarm_button.setOnClickListener {
			dismiss()
			controller.clearAlarm(task)
		}
	}
	
	companion object {
		private const val ARG_TASK = "task"
		private const val ARG_DATE = "date"
		
		@JvmStatic
		fun newInstance(task: TaskInfo, date: LocalDate): TaskAlarmOptionsDialogFragment =
				TaskAlarmOptionsDialogFragment().apply {
					arguments = Bundle().apply {
						putParcelable(ARG_TASK, task)
						putInt(ARG_DATE, localDateToDays(date))
					}
				}
	}
}
