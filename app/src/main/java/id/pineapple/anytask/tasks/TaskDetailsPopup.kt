package id.pineapple.anytask.tasks

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Point
import android.os.Build
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.PopupWindow
import id.pineapple.anytask.*
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.popup_task_details.*
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import java.util.*

class TaskDetailsPopup(
		context: Context,
		private val controller: TaskListController,
		private var task: TaskInfo,
		private val onDismissListener: PopupWindow.OnDismissListener
): ContextWrapper(context), LayoutContainer, LifecycleOwner, Observer<TaskInfo>,
		PopupWindow.OnDismissListener {
	private val weekDayFormatter = DateTimeFormat.forPattern("EEEE")
	private val timeFormatter = DateTimeFormat.shortTime()
	override val containerView: View =
			LayoutInflater.from(this).inflate(R.layout.popup_task_details, null)
	private val popupWindow = PopupWindow(containerView,
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
	private val lifecycleRegistry = LifecycleRegistry(this)
	
	init {
		popupWindow.setOnDismissListener(this)
		popupWindow.isFocusable = true
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			popupWindow.elevation = resources.getDimension(R.dimen.popupWindowElevation)
		}
		controller.model.observe(task.task.id!!, this, this)
		initUI()
	}
	
	override fun onChanged(taskInfo: TaskInfo?) {
		if (taskInfo != null) {
			this.task = taskInfo
		} else {
			dismiss()
		}
	}
	
	override fun getLifecycle(): Lifecycle = lifecycleRegistry
	
	fun show(anchor: View?) {
		lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
		lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
		val gravity: Int
		val location = IntArray(2)
		if (anchor != null) {
			val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
			val screenSize = Point()
			wm.defaultDisplay.getSize(screenSize)
			anchor.getLocationOnScreen(location)
			if (location[1] >= screenSize.y / 2) {
				location[1] = screenSize.y - location[1]
				gravity = Gravity.BOTTOM or Gravity.END
				popupWindow.animationStyle = R.style.Animation_WindowSlideDownUp
			} else {
				location[1] += anchor.height
				gravity = Gravity.TOP or Gravity.END
				popupWindow.animationStyle = R.style.Animation_WindowSlideUpDown
			}
		} else {
			gravity = Gravity.CENTER
		}
		popupWindow.showAtLocation(anchor, gravity, location[0], location[1])
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			popupWindow.dimBehind(0.7f)
		}
		lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
	}
	
	fun dismiss() {
		popupWindow.dismiss()
	}
	
	override fun onDismiss() {
		lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
		lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
		lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
		onDismissListener.onDismiss()
	}
	
	private fun initUI() {
		setTextViewCompoundDrawables(task_postpone_to_tomorrow_button,
				0, R.drawable.ic_redo_black_24dp, 0, 0)
		setTextViewCompoundDrawables(task_reschedule_to_button,
				0, R.drawable.ic_swap_horiz_black_24dp, 0, 0)
		setTextViewCompoundDrawables(task_alarm_label,
				R.drawable.ic_alarm_gray_24dp, 0, 0, 0)
		setTextViewCompoundDrawables(task_note_label,
				R.drawable.ic_library_books_gray_24dp, 0, 0, 0)
		setTextViewCompoundDrawables(task_repetition_label,
				R.drawable.ic_autorenew_gray_24dp, 0, 0, 0)
		
		/* task_priority_spinner.selectableItemsAdapter = ArrayAdapter.createFromResource(
				this, R.array.task_priority_names, android.R.layout.simple_spinner_dropdown_item
		) */
		task_priority_spinner.adapter = object : ArrayAdapter<String>(
				this, android.R.layout.simple_spinner_dropdown_item,
				resources.getStringArray(R.array.task_priority_names)
		) {
			private val colors = arrayOf(
					0,
					R.attr.mediumPriorityTaskBackground,
					R.attr.highPriorityTaskBackground,
					R.attr.highestPriorityTaskBackground
			).map {
				resolveColor(this@TaskDetailsPopup, it)
			}
			
			override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
					super.getDropDownView(position, convertView, parent).apply {
						setBackgroundColor(colors[position])
					}
		}
		task_auto_postpone_checkbox.isChecked = controller.willAutoPostpone(task)
		task_pin_checkbox.isChecked = task.task.pinned
		task_priority_spinner.setSelection(
				task.task.priority.coerceIn(0 until task_priority_spinner.adapter.count)
		)
		task_alarm_label.text = if (task.task.alarmTime != null)
			getString(R.string.alarm_at, timeFormatter.print(task.task.alarmTime))
		else
			getString(R.string.reminder_not_set)
		task_note_label.text = task.note?.getNormalizedTitle(this) ?:
				getString(R.string.no_attached_note)
		updateTaskRepetitionLabelText()
		
		task_auto_postpone_checkbox.setOnCheckedChangeListener { _, isChecked ->
			controller.setAutoPostpone(task, isChecked)
		}
		task_auto_postpone_checkbox_label.setOnClickListener {
			task_auto_postpone_checkbox.performClick()
		}
		task_pin_checkbox.setOnCheckedChangeListener { _, isChecked ->
			controller.setPinned(task, isChecked)
		}
		task_pin_checkbox_label.setOnClickListener {
			task_pin_checkbox.performClick()
		}
		task_priority_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
			override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
				controller.setPriority(task, position)
			}
			
			override fun onNothingSelected(parent: AdapterView<*>) {
			}
		}
		task_postpone_to_tomorrow_button.setOnClickListener {
			controller.postponeToTomorrow(task)
		}
		task_reschedule_to_button.setOnClickListener {
			controller.askReschedule(task)
		}
		task_alarm_label.setOnClickListener {
			controller.askSetAlarm(task)
		}
		task_note_label.setOnClickListener {
			TaskNotePickerDialogFragment.newInstance(task, controller.date!!).show(
					getSupportFragmentManager(this), null
			)
		}
		task_repetition_label.setOnClickListener {
			controller.askConfigureRepetition(task)
		}
	}
	
	private fun updateTaskRepetitionLabelText() {
		(task.template ?: task.task).scheduleOptions.let { options ->
			task_repetition_label.text = if (options.mode == TaskScheduleOptions.Mode.ONCE)
				getString(R.string.no_repetition_configured)
			else
				"${
				resources.getQuantityString(R.plurals.every, options.interval)
				}${if (options.interval > 1) " ${options.interval}" else ""} ${
				resources.getQuantityString(when (options.mode) {
					TaskScheduleOptions.Mode.ONCE -> throw IllegalStateException()
					TaskScheduleOptions.Mode.DAILY -> R.plurals.every_day
					TaskScheduleOptions.Mode.WEEKLY -> R.plurals.every_week
					TaskScheduleOptions.Mode.MONTHLY, TaskScheduleOptions.Mode.MONTHLY_INVERSE ->
						R.plurals.every_month
					TaskScheduleOptions.Mode.YEARLY -> R.plurals.every_year
				}, options.interval)
				} ${
				when (options.mode) {
					TaskScheduleOptions.Mode.ONCE -> throw IllegalStateException()
					TaskScheduleOptions.Mode.DAILY -> ""
					TaskScheduleOptions.Mode.WEEKLY ->
						getString(R.string.on_weekdays, LocalDate(0L).let { date ->
							val firstDayOfWeek = Calendar.getInstance().firstDayOfWeek
							(0 until 7).mapNotNull {
								val dayOfWeek = (it + 7 - firstDayOfWeek) % 7 + 1
								if (options.weekDaysMask and (1 shl (dayOfWeek - 1)) != 0)
									weekDayFormatter.print(date.withDayOfWeek(dayOfWeek))
								else
									null
							}.joinToString(", ")
						})
					TaskScheduleOptions.Mode.MONTHLY ->
						resources.getAdvancedQuantityString(R.array.th_day_since_month_start,
								options.startDate.dayOfMonth, options.startDate.dayOfMonth)
					TaskScheduleOptions.Mode.MONTHLY_INVERSE ->
						options.startDate.withDayOfMonth(1).plusMonths(1)
								.minusDays(1).dayOfMonth.let { maxDayOfMonth ->
							resources.getAdvancedQuantityString(R.array.th_day_before_month_end,
									maxDayOfMonth - options.startDate.dayOfMonth + 1)
						}
					TaskScheduleOptions.Mode.YEARLY ->
						DateTimeFormat.forPattern(
								getString(R.string.date_pattern_without_year)
						).print(options.startDate)
				}
				}".trim()
		}
	}
}
