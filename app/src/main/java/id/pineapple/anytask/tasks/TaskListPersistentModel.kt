package id.pineapple.anytask.tasks

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.os.AsyncTask
import id.pineapple.anytask.AppDatabase
import id.pineapple.anytask.observeOnce
import org.joda.time.LocalDate

class TaskListPersistentModel(
		val date: LocalDate,
		reactive: Boolean = true,
		val altSort: Boolean = false
) : TaskListModel {
	private val dao = AppDatabase.instance.taskDao()
	private val liveData: LiveData<List<TaskInfo>>? =
			if (reactive)
				if (altSort)
					dao.findByDateAltSort(date)
				else
					dao.findByDate(date)
			else
				null
	
	override fun observe(lifecycleOwner: LifecycleOwner, observer: Observer<List<TaskInfo>>) {
		if (liveData == null)
			throw IllegalStateException("Model is not reactive")
		liveData.observe(lifecycleOwner, TaskListProxy(observer, date))
	}
	
	override fun observe(id: Long, lifecycleOwner: LifecycleOwner, observer: Observer<TaskInfo>) {
		if (liveData == null)
			throw IllegalStateException("Model is not reactive")
		dao.findById(id).observe(lifecycleOwner, observer)
	}
	
	override fun fetch(id: Long, callback: (task: TaskInfo?) -> Unit) {
		if (liveData == null)
			throw IllegalStateException("Model is not reactive")
		dao.findById(id).observeOnce(Observer { callback(it) })
	}
	
	fun fetchSync(id: Long): TaskInfo? = dao.findByIdSync(id)
	
	fun fetchByDateRange(startDate: LocalDate, stopDate: LocalDate,
						 callback: (tasks: Map<LocalDate, List<Task>>) -> Unit) {
		TaskRangeFetcherAsyncTask(dao, startDate, stopDate, callback).execute()
	}
	
	override fun insert(vararg tasks: Task, callback: ((ids: List<Long>) -> Unit)?) {
		TaskInsertAsyncTask(dao, callback).execute(*tasks)
	}
	
	override fun update(vararg tasks: Task, callback: (() -> Unit)?) {
		TaskUpdateAsyncTask(dao, callback).execute(*tasks)
	}
	
	override fun delete(vararg tasks: Task, callback: (() -> Unit)?) {
		TaskDeleteAsyncTask(dao, callback).execute(*tasks)
	}
	
	fun cloneScheduledTasksBeforeDate(template: Task, date: LocalDate, callback: (() -> Unit)? = null) {
		CloneScheduledTasksBeforeDateAsyncTask(date, dao, callback).execute(template)
	}
	
	fun deleteClonedTasksAfterDate(template: Task, date: LocalDate, callback: (() -> Unit)? = null) {
		DeleteClonedTasksAfterDateAsyncTask(date, dao, callback).execute(template)
	}
	
	fun deleteTemplateReferencesAndTemplate(template: Task, callback: (() -> Unit)? = null) {
		DeleteTemplateReferencesAndTemplateAsyncTask(dao, callback).execute(template)
	}
	
	fun fetchTasksSync(): List<TaskInfo> = filterTasksByDate(date, dao.findByDateSync(date))
	
	private class TaskListProxy(
			private val observer: Observer<List<TaskInfo>>,
			private val date: LocalDate
	) : Observer<List<TaskInfo>> {
		override fun onChanged(tasks: List<TaskInfo>?) {
			TaskFilterAsyncTask(observer, date).execute(*(tasks?.toTypedArray() ?: emptyArray()))
		}
	}
	
	private class TaskFilterAsyncTask(
			private val observer: Observer<List<TaskInfo>>,
			private val date: LocalDate
	) : AsyncTask<TaskInfo, Void?, List<TaskInfo>>() {
		override fun doInBackground(vararg tasks: TaskInfo): List<TaskInfo> =
				filterTasksByDate(date, tasks.toList())
		
		override fun onPostExecute(result: List<TaskInfo>) {
			observer.onChanged(result)
		}
	}
	
	private class TaskRangeFetcherAsyncTask(
			private val dao: TaskDao,
			private val startDate: LocalDate,
			private val stopDate: LocalDate,
			private val callback: (tasks: Map<LocalDate, List<Task>>) -> Unit
	): AsyncTask<Void?, Void?, Map<LocalDate, List<Task>>>() {
		override fun doInBackground(vararg params: Void?): Map<LocalDate, List<Task>> {
			val tasks = dao.findByDateRangeSync(startDate, stopDate).map {
				TaskInfo(it)
			}
			val result = mutableMapOf<LocalDate, List<Task>>()
			var date = startDate
			while (date <= stopDate) {
				result[date] = filterTasksByDate(date, tasks).map { it.task }
				date = date.plusDays(1)
			}
			return result
		}
		
		override fun onPostExecute(result: Map<LocalDate, List<Task>>) {
			callback(result)
		}
	}
	
	private class TaskInsertAsyncTask(
			private val dao: TaskDao,
			private val callback: ((ids: List<Long>) -> Unit)?
	) : AsyncTask<Task, Void?, List<Long>>() {
		override fun doInBackground(vararg params: Task): List<Long> =
				dao.insert(*params)
		
		override fun onPostExecute(result: List<Long>) {
			callback?.invoke(result)
		}
	}
	
	private class TaskUpdateAsyncTask(
			private val dao: TaskDao,
			private val callback: (() -> Unit)?
	) : AsyncTask<Task, Void?, Void?>() {
		override fun doInBackground(vararg tasks: Task): Void? {
			dao.update(*tasks.map {
				if (it.scheduleTemplateId == null || !it.titleFromTemplate)
					it
				else
					it.copy(title = "")
			}.toTypedArray())
			tasks.filter {
				it.scheduleTemplateId != null && it.titleFromTemplate
			}.forEach {
				dao.updateTitle(it.scheduleTemplateId!!, it.title, it.titleUpdateTime)
			}
			return null
		}
		
		override fun onPostExecute(result: Void?) {
			callback?.invoke()
		}
	}
	
	private class TaskDeleteAsyncTask(
			private val dao: TaskDao,
			private val callback: (() -> Unit)?
	) : AsyncTask<Task, Void?, Void?>() {
		override fun doInBackground(vararg params: Task): Void? {
			dao.delete(*params)
			return null
		}
		
		override fun onPostExecute(result: Void?) {
			callback?.invoke()
		}
	}
	
	private class CloneScheduledTasksBeforeDateAsyncTask(
			private val date: LocalDate,
			private val dao: TaskDao,
			private val callback: (() -> Unit)?
	) : AsyncTask<Task, Void?, Void?>() {
		override fun doInBackground(vararg templateTasks: Task): Void? {
			templateTasks.forEach { templateTask ->
				AppDatabase.instance.beginTransaction()
				val alreadyClonedDates =
						dao.findClonedTaskDatesByTemplateIdBeforeDateSync(templateTask.id!!, date)
								.toSet()
				var currentDate: LocalDate? = templateTask.scheduleOptions.startDate
				while (currentDate != null && currentDate < date) {
					if (!alreadyClonedDates.contains(currentDate)) {
						dao.insert(templateTask.updateScheduleOptions(TaskScheduleOptions(
								mode = TaskScheduleOptions.Mode.ONCE,
								startDate = currentDate,
								stopDate = currentDate
						)).cloneScheduled(currentDate)).first()
					}
					currentDate = templateTask.scheduleOptions.findNextDate(currentDate.plusDays(1))
				}
				AppDatabase.instance.setTransactionSuccessful()
				AppDatabase.instance.endTransaction()
			}
			return null
		}
		
		override fun onPostExecute(result: Void?) {
			callback?.invoke()
		}
	}
	
	private class DeleteClonedTasksAfterDateAsyncTask(
			private val date: LocalDate,
			private val dao: TaskDao,
			private val callback: (() -> Unit)?
	) : AsyncTask<Task, Void?, Void?>() {
		override fun doInBackground(vararg templateTasks: Task): Void? {
			templateTasks.forEach { templateTask ->
				dao.deleteByTemplateIdAfterDate(templateTask.id!!, date)
			}
			return null
		}
		
		override fun onPostExecute(result: Void?) {
			callback?.invoke()
		}
	}
	
	private class DeleteTemplateReferencesAndTemplateAsyncTask(
			private val dao: TaskDao,
			private val callback: (() -> Unit)?
	) : AsyncTask<Task, Void?, Void?>() {
		override fun doInBackground(vararg templates: Task): Void? {
			templates.forEach { template ->
				AppDatabase.instance.beginTransaction()
				dao.deleteTaskTemplateReferences(template.id!!, template.title, template.priority,
						template.pinned, template.alarmTime, template.noteId)
				dao.delete(template)
				AppDatabase.instance.setTransactionSuccessful()
				AppDatabase.instance.endTransaction()
			}
			return null
		}
		
		override fun onPostExecute(result: Void?) {
			callback?.invoke()
		}
	}
	
	companion object {
		fun filterTasksByDate(date: LocalDate, tasks: List<TaskInfo>): List<TaskInfo> {
			val hiddenTaskIds = tasks.mapNotNull {
				if (it.task.originDate == date)
					it.task.scheduleTemplateId
				else
					null
			}.toSet()
			return tasks.filter { task ->
				!hiddenTaskIds.contains(task.task.id) &&
						when {
							task.task.completed -> date == task.task.scheduleOptions.startDate
							else -> task.task.scheduleOptions.findNextDate(date) == date
						}
			}
		}
	}
}
