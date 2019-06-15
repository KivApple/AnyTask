package id.pineapple.anytask.tasks

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import id.pineapple.anytask.*
import kotlinx.android.synthetic.main.dialog_task_schedule_options.*
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat

class TaskScheduleOptionsDialogFragment : BottomSheetDialogFragment() {
	private val dateFormatter = DateTimeFormat.shortDate()
	private lateinit var task: TaskInfo
	private lateinit var scheduleTemplate: Task
	private lateinit var date: LocalDate
	private lateinit var model: TaskListPersistentModel
	private lateinit var controller: TaskListController
	private var newStopDate: LocalDate? = null
	private var savedScheduleInterval: Int? = null
	private var savedScheduleMode: TaskScheduleOptions.Mode? = null
	private var savedWeekdayMask: Int? = null
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		arguments!!.let {
			task = it.getParcelable(ARG_TASK)!!
			date = daysToLocalDate(it.getInt(ARG_DATE))
		}
		savedInstanceState?.let {
			if (it.containsKey(STATE_NEW_STOP_DATE)) {
				newStopDate = daysToLocalDate(it.getInt(STATE_NEW_STOP_DATE))
			}
			savedScheduleInterval = it.getInt(STATE_INTERVAL)
			savedScheduleMode = TaskScheduleOptions.Mode.values()[it.getInt(STATE_MODE)]
			savedWeekdayMask = it.getInt(STATE_WEEKDAY_MASK)
		}
		if (newStopDate == null) {
			if (!task.task.isScheduled) {
				newStopDate = daysToLocalDate(Int.MAX_VALUE)
			}
		}
		model = TaskListPersistentModel(date)
		controller = TaskListController(model, this, context as UiHelper,
				lightweight = true)
		scheduleTemplate = task.template ?: task.task
	}
	
	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		newStopDate?.let {
			outState.putInt(STATE_NEW_STOP_DATE, localDateToDays(it))
		}
		task_repetition_interval_picker.value.let {
			savedScheduleInterval = it
			outState.putInt(STATE_INTERVAL, it)
		}
		when (task_repetition_mode_picker.value) {
			MODE_DAILY -> TaskScheduleOptions.Mode.DAILY
			MODE_WEEKLY -> TaskScheduleOptions.Mode.WEEKLY
			MODE_MONTHLY -> if (task_monthly_repetition_before_end.isChecked)
				TaskScheduleOptions.Mode.MONTHLY_INVERSE
			else
				TaskScheduleOptions.Mode.MONTHLY
			MODE_YEARLY -> TaskScheduleOptions.Mode.YEARLY
			else -> throw IllegalStateException()
		}.let {
			savedScheduleMode = it
			outState.putInt(STATE_MODE, it.ordinal)
		}
		(1..7).map {
			if (task_repetition_week_view.getWeekDayState(it + 1))
				1 shl (it - 1)
			else
				0
		}.fold(0) { value, it -> value or it }.let {
			savedWeekdayMask = it
			outState.putInt(STATE_WEEKDAY_MASK, it)
		}
	}
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
			inflater.inflate(R.layout.dialog_task_schedule_options, container, false)
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		fixLandscapeHeight()
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			task_repetition_stop_date.setCompoundDrawablesRelativeWithIntrinsicBounds(
					ContextCompat.getDrawable(context!!, R.drawable.ic_autorenew_gray_24dp),
					null, null, null
			)
		} else {
			task_repetition_stop_date.setCompoundDrawablesWithIntrinsicBounds(
					ContextCompat.getDrawable(context!!, R.drawable.ic_autorenew_gray_24dp),
					null, null, null
			)
		}
		
		task_repetition_interval_picker.minValue = 1
		task_repetition_interval_picker.maxValue = Int.MAX_VALUE
		task_repetition_interval_picker.wrapSelectorWheel = false
		(savedScheduleInterval ?: scheduleTemplate.scheduleOptions.interval).let {
			task_repetition_interval_picker.value = it.coerceAtLeast(1)
		}
		updateIntervalLabels()
		(savedScheduleMode ?: scheduleTemplate.scheduleOptions.mode).let {
			task_repetition_mode_picker.value = scheduleModeToIndex(it)
		}
		updateWeekView()
		val daySinceMonthStart = date.dayOfMonth
		val maxDayOfMonth = date.withDayOfMonth(1).plusMonths(1).minusDays(1).dayOfMonth
		val dayBeforeMonthEnd = maxDayOfMonth - daySinceMonthStart + 1
		task_monthly_repetition_since_start.text =
				resources.getAdvancedQuantityString(R.array.th_day_since_month_start,
						daySinceMonthStart, daySinceMonthStart)
		task_monthly_repetition_before_end.text =
				resources.getAdvancedQuantityString(R.array.th_day_before_month_end,
						dayBeforeMonthEnd, dayBeforeMonthEnd)
		if ((savedScheduleMode ?: scheduleTemplate.scheduleOptions.mode) ==
				TaskScheduleOptions.Mode.MONTHLY_INVERSE) {
			task_monthly_repetition_before_end.isChecked = true
		} else {
			task_monthly_repetition_since_start.isChecked = true
		}
		task_repetition_yearly_date_label.text = DateTimeFormat.forPattern(
				getString(R.string.date_pattern_without_year)
		).print(date)
		onRepetitionModeChanged()
		(newStopDate ?: scheduleTemplate.scheduleOptions.stopDate).let { stopDate ->
			task_repetition_stop_date.text = if (stopDate != daysToLocalDate(Int.MAX_VALUE))
				getString(R.string.repeat_until, dateFormatter.print(stopDate))
			else
				getString(R.string.repeat_forever)
		}
		if (!task.task.isScheduled) {
			task_delete_repetition_button.visibility = View.GONE
		}
		
		task_repetition_interval_picker.setOnValueChangedListener { _, _, _ ->
			updateIntervalLabels()
		}
		task_repetition_mode_picker.setOnValueChangedListener { _, _, _ ->
			onRepetitionModeChanged()
		}
		task_repetition_week_view.setOnWeekDayClickListener {
			val weekDay = ((it - 1) + 6) % 7 + 1
			if (weekDay != date.dayOfWeek) {
				task_repetition_week_view.setWeekDayState(it,
						!task_repetition_week_view.getWeekDayState(it))
			} else {
				task_repetition_week_view.setWeekDayState(it, true)
			}
		}
		task_repetition_stop_date.setOnClickListener { _ ->
			DatePickerDialogFragment.newInstance(
					if (scheduleTemplate.scheduleOptions.stopDate != daysToLocalDate(Int.MAX_VALUE))
						scheduleTemplate.scheduleOptions.stopDate
					else
						date,
					true
			).let {
				it.setTargetFragment(this, REQUEST_SET_STOP_DATE)
				it.show(fragmentManager, null)
			}
		}
		task_repetition_save_button.setOnClickListener { _ ->
			dismiss()
			(context as UiHelper).dismissTaskDetails(task.id!!)
			controller.configureRepetition(task, TaskScheduleOptions(
					mode = when (task_repetition_mode_picker.value) {
						MODE_DAILY -> TaskScheduleOptions.Mode.DAILY
						MODE_WEEKLY -> TaskScheduleOptions.Mode.WEEKLY
						MODE_MONTHLY -> if (task_monthly_repetition_before_end.isChecked)
							TaskScheduleOptions.Mode.MONTHLY_INVERSE
						else
							TaskScheduleOptions.Mode.MONTHLY
						MODE_YEARLY -> TaskScheduleOptions.Mode.YEARLY
						else -> throw IllegalStateException()
					},
					interval = task_repetition_interval_picker.value.coerceAtLeast(1),
					weekDaysMask = if (task_repetition_mode_picker.value == MODE_WEEKLY)
						(1..7).map {
							if (task_repetition_week_view.getWeekDayState(it + 1))
								1 shl (it - 1)
							else
								0
						}.fold(0) { value, it -> value or it }
					else 0,
					startDate = date,
					stopDate = newStopDate ?: scheduleTemplate.scheduleOptions.stopDate
			))
		}
		task_delete_repetition_button.setOnClickListener {
			dismiss()
			(context as UiHelper).dismissTaskDetails(task.id!!)
			controller.deleteRepetition(task)
		}
	}
	
	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		when (requestCode) {
			REQUEST_SET_STOP_DATE -> if (resultCode == Activity.RESULT_OK) {
				newStopDate = if (data != null)
					daysToLocalDate(data.getIntExtra(
							DatePickerDialogFragment.RESULT_EXTRA_DATE, Int.MAX_VALUE
					))
				else
					daysToLocalDate(Int.MAX_VALUE)
				if (data != null) {
					task_repetition_stop_date?.text = getString(R.string.repeat_until,
							dateFormatter.print(newStopDate))
				} else {
					task_repetition_stop_date?.text = getString(R.string.repeat_forever)
				}
			}
		}
	}
	
	private fun updateIntervalLabels() {
		val interval = task_repetition_interval_picker.value
		every_label.text = resources.getQuantityString(R.plurals.every, interval)
		val modeNameResources = listOf(
				R.plurals.every_day, R.plurals.every_week,
				R.plurals.every_month, R.plurals.every_year
		)
		val modeNames = modeNameResources.map {
			resources.getQuantityString(it, interval)
		}
		task_repetition_mode_picker.minValue = 0
		task_repetition_mode_picker.maxValue = modeNames.lastIndex
		task_repetition_mode_picker.displayedValues = modeNames.toTypedArray()
	}
	
	private fun onRepetitionModeChanged() {
		when (task_repetition_mode_picker.value) {
			MODE_DAILY -> {
				task_repetition_week_view.visibility = View.INVISIBLE
				task_monthly_repetition_mode_radio_group.visibility = View.INVISIBLE
				task_repetition_yearly_date_label.visibility = View.INVISIBLE
			}
			MODE_WEEKLY -> {
				task_monthly_repetition_mode_radio_group.visibility = View.INVISIBLE
				task_repetition_yearly_date_label.visibility = View.INVISIBLE
				task_repetition_week_view.visibility = View.VISIBLE
			}
			MODE_MONTHLY -> {
				task_repetition_week_view.visibility = View.INVISIBLE
				task_repetition_yearly_date_label.visibility = View.INVISIBLE
				task_monthly_repetition_mode_radio_group.visibility = View.VISIBLE
			}
			MODE_YEARLY -> {
				task_repetition_week_view.visibility = View.INVISIBLE
				task_monthly_repetition_mode_radio_group.visibility = View.INVISIBLE
				task_repetition_yearly_date_label.visibility = View.VISIBLE
			}
		}
	}
	
	private fun updateWeekView() {
		if (savedWeekdayMask != null) {
			val curDayOfWeek = date.dayOfWeek
			(1..7).forEach {
				task_repetition_week_view.setWeekDayState(it + 1,
						savedWeekdayMask!! and (1 shl (it - 1)) != 0 ||
								(it == curDayOfWeek && savedWeekdayMask!! == 0))
			}
		} else {
			scheduleTemplate.scheduleOptions.let { options ->
				val curDayOfWeek = options.startDate.dayOfWeek
				(1..7).forEach {
					task_repetition_week_view.setWeekDayState(it + 1,
							options.weekDaysMask and (1 shl (it - 1)) != 0 ||
									(it == curDayOfWeek && options.weekDaysMask == 0))
				}
			}
		}
	}
	
	companion object {
		private const val ARG_TASK = "task"
		private const val ARG_DATE = "date"
		
		private const val STATE_NEW_STOP_DATE = "new_stop_date"
		private const val STATE_INTERVAL = "interval"
		private const val STATE_MODE = "mode"
		private const val STATE_WEEKDAY_MASK = "weekday_mask"
		
		private const val MODE_DAILY = 0
		private const val MODE_WEEKLY = 1
		private const val MODE_MONTHLY = 2
		private const val MODE_YEARLY = 3
		
		private const val REQUEST_SET_STOP_DATE = 1
		
		@JvmStatic
		fun newInstance(task: TaskInfo, date: LocalDate): TaskScheduleOptionsDialogFragment =
				TaskScheduleOptionsDialogFragment().apply {
					arguments = Bundle().apply {
						putParcelable(ARG_TASK, task)
						putInt(ARG_DATE, localDateToDays(date))
					}
				}
		
		private fun scheduleModeToIndex(mode: TaskScheduleOptions.Mode) =
				when (mode) {
					TaskScheduleOptions.Mode.ONCE, TaskScheduleOptions.Mode.DAILY -> MODE_DAILY
					TaskScheduleOptions.Mode.WEEKLY -> MODE_WEEKLY
					TaskScheduleOptions.Mode.MONTHLY, TaskScheduleOptions.Mode.MONTHLY_INVERSE ->
						MODE_MONTHLY
					TaskScheduleOptions.Mode.YEARLY -> MODE_YEARLY
				}
	}
}
