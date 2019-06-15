package id.pineapple.anytask.tasks

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import id.pineapple.anytask.R
import id.pineapple.anytask.UiHelper
import id.pineapple.anytask.daysToLocalDate
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import org.joda.time.LocalTime
import org.joda.time.Period
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.PeriodFormat

class TaskListController(
		val model: TaskListModel,
		lifecycleOwner: LifecycleOwner,
		private val uiHelper: UiHelper,
		private val lightweight: Boolean = false
) {
	private val dateFormatter = DateTimeFormat.shortDate()
	val date: LocalDate? = (model as? TaskListPersistentModel)?.date
	val altSort = (model as? TaskListPersistentModel)?.altSort == true
	private var items: List<TaskInfo> = emptyList()
	private var maxUncompletedPosition = mutableMapOf<Int, Int>()
	private var minCompletedPosition = 0
	private var completedCount: Int = 0
	
	init {
		if (!lightweight) {
			model.observe(lifecycleOwner, Observer { tasks ->
				items = tasks ?: emptyList()
				minCompletedPosition = Int.MAX_VALUE
				completedCount = 0
				maxUncompletedPosition.clear()
				var currentPriority = Int.MIN_VALUE
				var currentMaxUncompletedPosition = Int.MIN_VALUE
				items.forEach {
					if (it.task.completed) {
						completedCount++
					}
					if (it.task.completed && !altSort) {
						if (it.task.position < minCompletedPosition) {
							minCompletedPosition = it.task.position
						}
					} else {
						if (it.task.priority != currentPriority) {
							if (currentPriority != Int.MIN_VALUE) {
								maxUncompletedPosition[currentPriority] = currentMaxUncompletedPosition
							}
							currentPriority = it.task.priority
							currentMaxUncompletedPosition = Int.MIN_VALUE
						}
						if (it.task.position > currentMaxUncompletedPosition) {
							currentMaxUncompletedPosition = it.task.position
						}
					}
				}
				if (currentPriority != Int.MIN_VALUE && currentMaxUncompletedPosition != Int.MIN_VALUE) {
					maxUncompletedPosition[currentPriority] = currentMaxUncompletedPosition
				}
				if (minCompletedPosition == Int.MAX_VALUE) {
					minCompletedPosition = 0
				}
			})
		}
	}
	
	fun add(
			vararg titles: String,
			completed: Boolean = false,
			priority: Int = 0,
			after: Task? = items.lastOrNull { it.task.completed == completed }?.task
	) {
		if (lightweight) throw IllegalStateException("Lightweight controller cannot do this")
		var afterItemSeen = after == null
		val shiftedTasks = items.filter {
			(altSort || it.task.completed == completed) && it.task.priority == priority
		}.mapIndexedNotNull { index, it ->
			val isRescheduled = date != null && it.task.scheduleOptions.startDate != date
			val expectedPosition = if (afterItemSeen)
				index + 1 + titles.size
			else
				index + 1
			var newTask = if (isRescheduled || it.task.position != expectedPosition)
				it.task.updatePosition(expectedPosition)
			else
				null
			if (newTask != null && isRescheduled) {
				newTask = newTask.updateScheduleOptions(newTask.scheduleOptions.copy(
						startDate = date!!,
						stopDate = if (newTask.scheduleOptions.mode == TaskScheduleOptions.Mode.ONCE)
							newTask.scheduleOptions.stopDate
						else
							date
				))
			}
			if (it.task.id == after?.id) afterItemSeen = true
			newTask
		}
		if (shiftedTasks.isNotEmpty()) {
			updateOrCloneScheduled(*shiftedTasks.toTypedArray())
		}
		model.insert(*titles.mapIndexed { index, title ->
			Task(
					title = title,
					completed = completed,
					position = (after?.position ?: 0) + 1 + index,
					priority = priority,
					scheduleOptions = if (date != null)
						TaskScheduleOptions(
								mode = TaskScheduleOptions.Mode.ONCE,
								startDate = date,
								stopDate = if (date >= LocalDate())
									daysToLocalDate(Int.MAX_VALUE)
								else
									date
						)
					else
						TaskScheduleOptions(mode = TaskScheduleOptions.Mode.ONCE),
					originDate = date ?: LocalDate()
			)
		}.toTypedArray()) {
			uiHelper.editTaskTitle(it.last())
		}
	}
	
	fun updateTitle(task: TaskInfo, title: String) {
		if (!task.task.isScheduled) {
			model.update(task.task.updateTitle(title))
		} else {
			if (date == null) throw IllegalStateException("Model does not support dates")
			val template = task.template ?: task.task
			if (task.task.titleFromTemplate) {
				model.update(template.updateTitle(title))
				uiHelper.showSnackBar(
						uiHelper.getString(R.string.repeatable_task_modified),
						UiHelper.PopupDuration.VERY_LONG,
						uiHelper.getString(R.string.only_today)
				) {
					if (task.id == template.id) {
						updateOrCloneScheduled(task.task.updateTitle(title, false),
								overrideRepetition = true)
					} else {
						model.update(task.task.updateTitle(title, false))
					}
					model.update(template)
				}
			} else if (title == template.title) {
				model.update(task.task.updateTitle(title))
			}
		}
	}
	
	fun setCompleted(taskInfo: TaskInfo, completed: Boolean, clearAlarm: Boolean = false) {
		if (lightweight) throw IllegalStateException("Lightweight controller cannot do this")
		if (taskInfo.task.completed == completed && (!clearAlarm && taskInfo.task.alarmTime == null)) {
			return
		}
		var newTask = taskInfo.task
		if (taskInfo.task.completed != completed) {
			newTask = taskInfo.task.updateCompleted(
					completed,
					when {
						altSort -> taskInfo.task.position
						completed -> minCompletedPosition - 1
						else -> taskInfo.task.savedPosition
					}
			)
		}
		if (clearAlarm && newTask.alarmTime != null) {
			newTask = newTask.updateAlarmTime(null, !newTask.isScheduled)
		}
		if (date != null) {
			newTask = newTask.updateScheduleOptions(newTask.scheduleOptions.copy(
					startDate = date,
					stopDate = if ((completed || date >= LocalDate()) &&
							taskInfo.task.scheduleOptions.mode == TaskScheduleOptions.Mode.ONCE)
						maxOf(date, newTask.scheduleOptions.stopDate)
					else
						date
			))
		}
		updateOrCloneScheduled(newTask)
	}
	
	fun saveOrder(taskInfos: Iterable<TaskInfo>) {
		if (lightweight) throw IllegalStateException("Lightweight controller cannot do this")
		var expectedPriority = Int.MAX_VALUE
		var expectedPosition = 1
		val modifiedTasks = taskInfos.mapNotNull { taskInfo ->
			if (taskInfo.task.priority != expectedPriority) {
				expectedPriority = taskInfo.task.priority
				expectedPosition = 1
			}
			val isRescheduled = date != null && taskInfo.task.scheduleOptions.startDate != date
			val newTask = if ((altSort || !taskInfo.task.completed) &&
					(taskInfo.task.position != expectedPosition || isRescheduled)) {
				taskInfo.task.updatePosition(expectedPosition)
			} else {
				null
			}
			expectedPosition = (newTask?.position ?: taskInfo.task.position) + 1
			newTask
		}
		if (modifiedTasks.isNotEmpty()) {
			if (modifiedTasks.all { it.scheduleOptions.mode != TaskScheduleOptions.Mode.ONCE }) {
				model.update(*modifiedTasks.toTypedArray())
			} else {
				updateOrCloneScheduled(*modifiedTasks.toTypedArray(), overrideRepetition = true)
			}
		}
	}
	
	fun willAutoPostpone(taskInfo: TaskInfo): Boolean {
		if (date == null) throw IllegalStateException("Model does not support dates")
		return taskInfo.task.scheduleOptions.mode == TaskScheduleOptions.Mode.ONCE &&
				date < taskInfo.task.scheduleOptions.stopDate
	}
	
	fun setAutoPostpone(taskInfo: TaskInfo, autoPostpone: Boolean) {
		if (date == null) throw IllegalStateException("Model does not support dates")
		val newTask = taskInfo.task.updateScheduleOptions(taskInfo.task.scheduleOptions.copy(
				startDate = if (taskInfo.task.scheduleOptions.mode == TaskScheduleOptions.Mode.ONCE)
					taskInfo.task.scheduleOptions.startDate
				else
					date,
				stopDate = if (autoPostpone)
					daysToLocalDate(Int.MAX_VALUE)
				else
					date
		))
		updateOrCloneScheduled(newTask)
		LocalDate().let { now ->
			if (autoPostpone && !taskInfo.task.completed && date < now &&
					newTask.scheduleOptions.stopDate > date) {
				uiHelper.dismissTaskDetails(taskInfo.id!!)
			}
		}
	}
	
	fun setPinned(taskInfo: TaskInfo, pinned: Boolean) {
		if (pinned == taskInfo.task.pinned) return
		val template = taskInfo.template ?: taskInfo.task
		model.update(template.updatePinned(pinned))
		if (taskInfo.task.isScheduled && !taskInfo.task.pinnedFromTemplate) {
			model.update(taskInfo.task.updatePinned(pinned))
		}
		if (taskInfo.task.isScheduled) {
			uiHelper.showSnackBar(uiHelper.getString(
					if (pinned)
						R.string.repeatable_task_pinned
					else
						R.string.repeatable_task_unpinned
			), UiHelper.PopupDuration.VERY_LONG, uiHelper.getString(R.string.only_today)) {
				updateOrCloneScheduled(taskInfo.task.updatePinned(pinned, false),
						overrideRepetition = true)
				model.update(template)
			}
		}
		model.update(taskInfo.task.updatePinned(pinned))
	}
	
	fun setPriority(taskInfo: TaskInfo, priority: Int) {
		if (lightweight) throw IllegalStateException("Lightweight controller cannot do this")
		if (taskInfo.task.priority == priority) return
		uiHelper.dismissTaskDetails(taskInfo.id!!)
		val template = taskInfo.template ?: taskInfo.task
		model.update(template.updatePriority(
				priority,
				(maxUncompletedPosition[priority] ?: 0) + 1
		))
		if (taskInfo.task.isScheduled && !taskInfo.task.priorityFromTemplate) {
			model.update(taskInfo.task.updatePriority(
					priority,
					(maxUncompletedPosition[priority] ?: 0) + 1
			))
		}
		if (taskInfo.task.isScheduled) {
			uiHelper.showSnackBar(uiHelper.getString(R.string.repeatable_task_priority_changed),
					UiHelper.PopupDuration.VERY_LONG, uiHelper.getString(R.string.only_today)) {
				updateOrCloneScheduled(taskInfo.task
						.updatePriority(
								priority,
								(maxUncompletedPosition[priority] ?: 0) + 1,
								false
						), overrideRepetition = true)
				model.update(template)
			}
		}
	}
	
	fun askSetPriority(taskInfo: TaskInfo) {
		if (date == null) throw IllegalStateException("Model does not support dates")
		uiHelper.showTaskPriorityDialog(taskInfo, date)
	}
	
	fun reschedule(vararg taskInfos: TaskInfo, newDate: LocalDate) {
		if (date == newDate) return
		taskInfos.forEach {
			uiHelper.dismissTaskDetails(it.task.id!!)
		}
		updateOrCloneScheduled(*taskInfos.map {
			it.task.updateScheduleOptions(it.task.scheduleOptions.copy(
					startDate = newDate,
					stopDate = if (newDate >= LocalDate() &&
							it.task.scheduleOptions.mode == TaskScheduleOptions.Mode.ONCE &&
							it.task.scheduleOptions.stopDate > it.task.scheduleOptions.startDate)
						maxOf(newDate, it.task.scheduleOptions.stopDate)
					else
						newDate
			))
		}.toTypedArray())
		val dateStr = dateFormatter.print(newDate)
		uiHelper.showSnackBar(
				if (taskInfos.size == 1)
					uiHelper.getString(R.string.task_rescheduled_to, dateStr)
				else
					uiHelper.getQuantityString(R.plurals.tasks_rescheduled_to, taskInfos.size,
							taskInfos.size, dateStr),
				UiHelper.PopupDuration.VERY_LONG,
				uiHelper.getString(R.string.cancel)
		) {
			model.update(*taskInfos.map { it.task }.toTypedArray())
		}
	}
	
	fun postponeToTomorrow(vararg taskInfos: TaskInfo) {
		if (date == null) throw IllegalStateException("Model does not support dates")
		reschedule(*taskInfos, newDate = date.plusDays(1))
	}
	
	fun askReschedule(vararg taskInfos: TaskInfo) {
		if (date == null) throw IllegalStateException("Model does not support dates")
		taskInfos.forEach {
			uiHelper.dismissTaskDetails(it.task.id!!)
		}
		uiHelper.showTaskRescheduleDialog(*taskInfos, date = date)
	}
	
	fun configureRepetition(taskInfo: TaskInfo, scheduleOptions: TaskScheduleOptions) {
		if (date == null || model !is TaskListPersistentModel)
			throw IllegalStateException("Model does not support dates")
		val task = taskInfo.template ?: taskInfo.task
		uiHelper.dismissTaskDetails(task.id!!)
		var template = task.updateScheduleOptions(scheduleOptions)
		if (scheduleOptions.mode != TaskScheduleOptions.Mode.ONCE &&
				!task.isScheduled && task.completed) {
			model.insert(task.cloneScheduled(date))
			template = template.updateCompleted(
					false,
					if (altSort) template.position else template.savedPosition
			)
		}
		model.update(template)
		if (task.scheduleOptions.mode != TaskScheduleOptions.Mode.ONCE) {
			model.cloneScheduledTasksBeforeDate(task, scheduleOptions.startDate.plusDays(1)) {
				model.deleteClonedTasksAfterDate(task, scheduleOptions.startDate) {
					if (scheduleOptions.mode == TaskScheduleOptions.Mode.ONCE) {
						model.deleteTemplateReferencesAndTemplate(task)
					}
				}
			}
		}
	}
	
	fun deleteRepetition(taskInfo: TaskInfo) =
			configureRepetition(taskInfo, TaskScheduleOptions(
					mode = TaskScheduleOptions.Mode.ONCE,
					startDate = date ?: throw IllegalStateException("Model does not support dates")
			))
	
	fun askConfigureRepetition(taskInfo: TaskInfo) {
		if (date == null) throw IllegalStateException("Model does not support dates")
		uiHelper.showTaskScheduleOptionsDialog(taskInfo, date)
	}
	
	fun setAlarm(taskInfo: TaskInfo, alarmTime: LocalTime?) {
		if (date == null) throw IllegalStateException("Model does not support dates")
		uiHelper.dismissTaskDetails(taskInfo.id!!)
		val template = taskInfo.template ?: taskInfo.task
		showTimeUntilAlarm(template, alarmTime)
		model.update(template.updateAlarmTime(alarmTime))
		if (taskInfo.task.isScheduled && !taskInfo.task.alarmTimeFromTemplate) {
			model.update(taskInfo.task.updateAlarmTime(alarmTime))
		}
		if (taskInfo.task.isScheduled) {
			uiHelper.showSnackBar(uiHelper.getString(R.string.repeatable_task_alarm_changed),
					UiHelper.PopupDuration.VERY_LONG, uiHelper.getString(R.string.only_today)) {
				val newTask = taskInfo.task.updateAlarmTime(alarmTime, false)
				updateOrCloneScheduled(newTask, overrideRepetition = true)
				model.update(template)
				showTimeUntilAlarm(newTask, alarmTime)
			}
		}
	}
	
	private fun showTimeUntilAlarm(task: Task, alarmTime: LocalTime?) {
		if (date == null) throw IllegalStateException("Model does not support dates")
		val now = LocalDateTime().withMillisOfSecond(0).withSecondOfMinute(0)
		var alarmDateTime: LocalDateTime
		if (alarmTime != null) {
			alarmDateTime = date.toLocalDateTime(alarmTime)
			if (now > alarmDateTime) {
				val nextDate = task.scheduleOptions.findNextDate(date.plusDays(1))
				if (nextDate != null) {
					alarmDateTime = nextDate.toLocalDateTime(alarmTime)
				}
			}
		} else {
			alarmDateTime = LocalDateTime(0L)
		}
		val message = when {
			alarmDateTime > now -> uiHelper.getString(R.string.time_before_alarm,
					PeriodFormat.wordBased().print(Period(now, alarmDateTime)))
			alarmDateTime == now -> uiHelper.getString(R.string.alarm_will_fire_right_now)
			else -> uiHelper.getString(R.string.alarm_will_never_fire)
		}
		uiHelper.showToast(message, UiHelper.PopupDuration.VERY_LONG)
	}
	
	fun clearAlarm(taskInfo: TaskInfo) = setAlarm(taskInfo, null)
	
	fun askSetAlarm(taskInfo: TaskInfo) {
		if (date == null) throw IllegalStateException("Model does not support dates")
		uiHelper.showTaskAlarmOptionsDialog(taskInfo, date)
	}
	
	fun attachNote(taskInfo: TaskInfo, noteId: Long?) {
		if (taskInfo.task.noteId == noteId) return
		uiHelper.dismissTaskDetails(taskInfo.task.id!!)
		val template = taskInfo.template ?: taskInfo.task
		model.update(template.updateNoteId(noteId))
		if (taskInfo.task.isScheduled && !taskInfo.task.noteIdFromTemplate) {
			model.update(taskInfo.task.updateNoteId(noteId))
		}
		if (taskInfo.task.isScheduled) {
			uiHelper.showSnackBar(uiHelper.getString(R.string.repeatable_task_modified),
					UiHelper.PopupDuration.VERY_LONG, uiHelper.getString(R.string.only_today)) {
				updateOrCloneScheduled(taskInfo.task.updateNoteId(noteId, false),
						overrideRepetition = true)
				model.update(template)
			}
		}
	}
	
	fun detachNote(taskInfo: TaskInfo) = attachNote(taskInfo, null)
	
	private fun updateOrCloneScheduled(vararg tasks: Task, overrideRepetition: Boolean = false) {
		model.update(*tasks.filter {
			it.scheduleOptions.mode == TaskScheduleOptions.Mode.ONCE
		}.map {
			if (overrideRepetition && date != null && it.scheduleOptions.startDate != date)
				it.updateScheduleOptions(it.scheduleOptions.copy(
						startDate = date,
						stopDate = if (it.scheduleOptions.stopDate >= date && date >= LocalDate())
							it.scheduleOptions.stopDate
						else
							date
				))
			else
				it
		}.toTypedArray())
		val clonedTasks = tasks.filter {
			it.scheduleOptions.mode != TaskScheduleOptions.Mode.ONCE
		}.map {
			if (date == null) throw IllegalStateException("Model should support dates")
			(if (overrideRepetition) it.updateScheduleOptions(it.scheduleOptions.copy(
					startDate = date,
					stopDate = date
			)) else it).cloneScheduled(date)
		}
		if (clonedTasks.isNotEmpty()) {
			if (date == null || model !is TaskListPersistentModel)
				throw IllegalStateException("Model should be persistent to do that")
			model.insert(*clonedTasks.toTypedArray())
		}
	}
	
	fun delete(vararg tasks: Task, force: Boolean = false) {
		val scheduledTasks = tasks.filter { it.isScheduled }
		if (scheduledTasks.isEmpty() || force) {
			model.delete(*tasks)
			if (scheduledTasks.isNotEmpty()) {
				if (model !is TaskListPersistentModel)
					throw IllegalStateException("Model should be persistent")
				scheduledTasks.forEach { task ->
					if (task.scheduleTemplateId != null) {
						model.fetch(task.scheduleTemplateId) { template ->
							if (template != null) {
								model.deleteClonedTasksAfterDate(template.task, LocalDate(0L))
								model.delete(template.task)
							}
						}
					}
				}
			}
			if (!force) {
				uiHelper.showSnackBar(when {
					tasks.size == 1 -> uiHelper.getString(R.string.task_deleted)
					else -> uiHelper.getQuantityString(R.plurals.tasks_deleted, tasks.size, tasks.size)
				}, UiHelper.PopupDuration.VERY_LONG, uiHelper.getString(R.string.recover)) {
					model.insert(*tasks)
				}
			}
		} else {
			if (date == null)
				throw IllegalStateException("Model should support dates")
			uiHelper.showTaskDeleteConfirmDialog(*tasks, date = date)
		}
	}
}
