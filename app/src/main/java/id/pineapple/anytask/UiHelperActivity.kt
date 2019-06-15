package id.pineapple.anytask

import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import id.pineapple.anytask.notes.Note
import id.pineapple.recyclerviewutil.UniqueEntity
import id.pineapple.anytask.tasks.Task
import id.pineapple.anytask.tasks.TaskInfo
import org.joda.time.LocalDate

abstract class UiHelperActivity : AppCompatActivity(), UiHelper {
	private val contentView: View get() = findViewById(android.R.id.content)
	
	override fun getQuantityString(resId: Int, quantity: Int, vararg formatArgs: Any): String =
			resources.getQuantityString(resId, quantity, *formatArgs)
	
	override fun runLater(callback: () -> Unit) {
		contentView.post(callback)
	}
	
	protected open fun findSnackBarContainer(): View = contentView
	
	override fun showSnackBar(message: String, duration: UiHelper.PopupDuration) {
		Snackbar.make(findSnackBarContainer(), message, when (duration) {
			UiHelper.PopupDuration.SHORT -> Snackbar.LENGTH_SHORT
			UiHelper.PopupDuration.LONG -> Snackbar.LENGTH_LONG
			UiHelper.PopupDuration.VERY_LONG -> 6000
			else -> Snackbar.LENGTH_INDEFINITE
		}).show()
	}
	
	override fun showSnackBar(message: String, duration: UiHelper.PopupDuration, action: String, callback: () -> Unit) {
		Snackbar.make(findSnackBarContainer(), message, when (duration) {
			UiHelper.PopupDuration.SHORT -> Snackbar.LENGTH_SHORT
			UiHelper.PopupDuration.LONG -> Snackbar.LENGTH_LONG
			UiHelper.PopupDuration.VERY_LONG -> 6000
			else -> Snackbar.LENGTH_INDEFINITE
		}).setAction(action) {
			callback()
		}.show()
	}
	
	override fun showToast(message: String, duration: UiHelper.PopupDuration) =
			Toast.makeText(this, message, when (duration) {
				UiHelper.PopupDuration.SHORT -> Toast.LENGTH_SHORT
				else -> Toast.LENGTH_LONG
			}).show()
	
	override fun showTaskList(date: LocalDate) {
		throw NotImplementedError()
	}
	
	override fun editTaskTitle(id: Long) {
		throw NotImplementedError()
	}
	
	override fun finishTaskTitleEdit() {
		throw NotImplementedError()
	}
	
	override fun dismissTaskDetails(id: Long) {
		throw NotImplementedError()
	}
	
	override fun showTaskRescheduleDialog(vararg tasks: TaskInfo, date: LocalDate) {
		throw NotImplementedError()
	}
	
	override fun showTaskScheduleOptionsDialog(task: TaskInfo, date: LocalDate) {
		throw NotImplementedError()
	}
	
	override fun showTaskAlarmOptionsDialog(task: TaskInfo, date: LocalDate) {
		throw NotImplementedError()
	}
	
	override fun showTaskDeleteConfirmDialog(vararg tasks: Task, date: LocalDate) {
		throw NotImplementedError()
	}
	
	override fun showTaskPriorityDialog(task: TaskInfo, date: LocalDate) {
		throw NotImplementedError()
	}
	
	override fun showNote(vararg notePath: Note, sharedElement: View?) {
		throw NotImplementedError()
	}
	
	override fun showNoteFolderDeleteConfirmDialog(vararg entities: UniqueEntity) {
		throw NotImplementedError()
	}
	
	override fun showMoveNotesToFolderDialog(
			vararg entities: UniqueEntity,
			selectedPath: Array<Note>
	) {
		throw NotImplementedError()
	}
	
	override fun showCopyNotesToFolderDialog(
			vararg entities: UniqueEntity,
			selectedPath: Array<Note>
	) {
		throw NotImplementedError()
	}
}
