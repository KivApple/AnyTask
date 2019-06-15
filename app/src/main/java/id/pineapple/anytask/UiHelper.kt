package id.pineapple.anytask

import android.view.View
import id.pineapple.anytask.notes.Note
import id.pineapple.recyclerviewutil.UniqueEntity
import id.pineapple.anytask.tasks.Task
import id.pineapple.anytask.tasks.TaskInfo
import org.joda.time.LocalDate

interface UiHelper {
	fun getString(resId: Int): String
	
	fun getString(resId: Int, vararg formatArgs: Any): String
	
	fun getQuantityString(resId: Int, quantity: Int, vararg formatArgs: Any): String
	
	fun runLater(callback: () -> Unit)
	
	fun showToast(message: String, duration: PopupDuration)
	
	fun showSnackBar(message: String, duration: PopupDuration)
	
	fun showSnackBar(message: String, duration: PopupDuration, action: String, callback: () -> Unit)
	
	fun showTaskList(date: LocalDate = LocalDate())
	
	fun editTaskTitle(id: Long)
	
	fun finishTaskTitleEdit()
	
	fun dismissTaskDetails(id: Long)
	
	fun showTaskRescheduleDialog(vararg tasks: TaskInfo, date: LocalDate)
	
	fun showTaskScheduleOptionsDialog(task: TaskInfo, date: LocalDate)
	
	fun showTaskAlarmOptionsDialog(task: TaskInfo, date: LocalDate)
	
	fun showTaskDeleteConfirmDialog(vararg tasks: Task, date: LocalDate)
	
	fun showTaskPriorityDialog(task: TaskInfo, date: LocalDate)
	
	fun showNote(vararg notePath: Note, sharedElement: View? = null)
	
	fun showNoteFolderDeleteConfirmDialog(vararg entities: UniqueEntity)
	
	fun showMoveNotesToFolderDialog(
			vararg entities: UniqueEntity,
			selectedPath: Array<Note>
	)
	
	fun showCopyNotesToFolderDialog(
			vararg entities: UniqueEntity,
			selectedPath: Array<Note>
	)
	
	enum class PopupDuration {
		SHORT,
		LONG,
		VERY_LONG,
		INFINITE
	}
}
