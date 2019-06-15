package id.pineapple.anytask.tasks

import android.arch.lifecycle.*
import android.arch.lifecycle.Observer
import android.view.View
import java.util.*

class TaskListArrayModel(
		items: Iterable<TaskInfo> = emptyList(),
		var view: View? = null
) : TaskListModel {
	private val observers = mutableSetOf<Observer<List<TaskInfo>>>()
	private var itemsSet = items.toSortedSet(TaskComparator())
	private val itemsMap = items.map { it.task.id!! to it }.toMap().toMutableMap()
	private var nextId = (items.maxBy { it.task.id!! }?.task?.id ?: 0) + 1
	
	val items: List<TaskInfo> get() = itemsSet.map { TaskInfo(it.task) }.toList()
	
	override fun observe(lifecycleOwner: LifecycleOwner, observer: Observer<List<TaskInfo>>) {
		lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
			private var firstStart = true
			
			@OnLifecycleEvent(Lifecycle.Event.ON_START)
			fun onStart() {
				if (firstStart) {
					observer.onChanged(itemsSet.toList())
					firstStart = false
				}
				observers.add(observer)
			}
			
			@OnLifecycleEvent(Lifecycle.Event.ON_START)
			fun onStop() {
				observers.remove(observer)
			}
		})
	}
	
	override fun observe(id: Long, lifecycleOwner: LifecycleOwner, observer: Observer<TaskInfo>) {
		TODO("not implemented")
	}
	
	private fun notifyDataSetChanged() {
		val list = itemsSet.toList()
		runLater {
			observers.forEach {
				it.onChanged(list)
			}
		}
	}
	
	override fun fetch(id: Long, callback: (task: TaskInfo?) -> Unit) {
		runLater {
			callback(itemsMap[id]?.task?.let { TaskInfo(it) })
		}
	}
	
	override fun insert(vararg tasks: Task, callback: ((ids: List<Long>) -> Unit)?) {
		val ids = ArrayList<Long>(tasks.size)
		val savedNextId = nextId
		itemsSet.addAll(tasks.map {
			val task = if (it.id != null) {
				if (it.id >= nextId) {
					nextId = it.id + 1
				}
				it
			} else {
				it.copy(id = nextId++)
			}
			ids.add(task.id!!)
			if (itemsMap.containsKey(task.id)) {
				tasks.forEach { t ->
					if (itemsMap[t.id]?.task === t) {
						itemsMap.remove(t.id)
					}
				}
				nextId = savedNextId
				throw IllegalStateException("Task with id ${task.id} already exists")
			}
			val taskInfo = TaskInfo(task)
			itemsMap[task.id] = taskInfo
			taskInfo
		})
		callback?.invoke(ids)
		notifyDataSetChanged()
	}
	
	override fun update(vararg tasks: Task, callback: (() -> Unit)?) {
		tasks.forEach {
			if (itemsSet.remove(itemsMap[it.id])) {
				val taskInfo = TaskInfo(it)
				itemsSet.add(taskInfo)
				itemsMap[it.id!!] = taskInfo
			} else {
				throw IllegalStateException("Task with id ${it.id} does not exist")
			}
		}
		callback?.invoke()
		notifyDataSetChanged()
	}
	
	override fun delete(vararg tasks: Task, callback: (() -> Unit)?) {
		tasks.forEach {
			val task = itemsMap.remove(it.id)
			if (task != null) {
				itemsSet.remove(task)
			} else {
				throw IllegalStateException("Task with id ${it.id} does not exist")
			}
		}
		callback?.invoke()
		notifyDataSetChanged()
	}
	
	private fun runLater(callback: () -> Unit) {
		view?.post(callback) ?: callback()
	}
	
	private class TaskComparator : Comparator<TaskInfo> {
		override fun compare(a: TaskInfo, b: TaskInfo): Int =
				when {
					a.task.id == b.task.id -> 0
					a.task.completed == b.task.completed && a.task.position != b.task.position ->
						a.task.position - b.task.position
					a.task.completed == b.task.completed -> a.task.id!!.compareTo(b.task.id!!)
					a.task.completed -> 1
					else -> -1
				}
	}
}
