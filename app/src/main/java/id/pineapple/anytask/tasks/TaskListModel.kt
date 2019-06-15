package id.pineapple.anytask.tasks

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer

interface TaskListModel {
	fun observe(lifecycleOwner: LifecycleOwner, observer: Observer<List<TaskInfo>>)
	
	fun observe(id: Long, lifecycleOwner: LifecycleOwner, observer: Observer<TaskInfo>)
	
	fun fetch(id: Long, callback: (task: TaskInfo?) -> Unit)
	
	fun insert(vararg tasks: Task, callback: ((ids: List<Long>) -> Unit)? = null)
	
	fun update(vararg tasks: Task, callback: (() -> Unit)? = null)
	
	fun delete(vararg tasks: Task, callback: (() -> Unit)? = null)
}
