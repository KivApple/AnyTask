package id.pineapple.anytask.tasks

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.preference.PreferenceManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.PopupWindow
import android.widget.Toast
import id.pineapple.anytask.NotificationsRefreshWorker
import id.pineapple.anytask.R
import id.pineapple.anytask.UiHelper
import id.pineapple.recyclerviewutil.*
import org.joda.time.format.DateTimeFormat

open class TaskListAdapter(
		val model: TaskListModel,
		val hasTaskDetails: Boolean,
		private val hideCompletedTasks: Boolean,
		val context: Context,
		lifecycleOwner: LifecycleOwner,
		private val selectableItemsAdapterDelegate: SelectableItemsAdapterDelegate =
				SelectableItemsAdapterDelegateImpl(),
		private val draggableItemsAdapterDelegate: DraggableItemsAdapterDelegate =
				DraggableItemsAdapterDelegateImpl()
): RecyclerViewAdapter(context), Observer<List<TaskInfo>>, SelectableItemsAdapter,
		SelectableItemsAdapterDelegate by selectableItemsAdapterDelegate,
		DraggableItemsAdapter,
		DraggableItemsAdapterDelegate by draggableItemsAdapterDelegate {
	private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
	val enableDebugInfo = sharedPreferences.getBoolean("enable_debug_info", false)
	val dateFormatter = DateTimeFormat.shortDate()!!
	val timeFormatter = DateTimeFormat.shortTime()!!
	val controller = TaskListController(model, lifecycleOwner, context as UiHelper)
	val altSort: Boolean = controller.altSort
	var editingViewHolder: TaskViewHolder? = null
	private var hiddenItems = mutableListOf<UniqueEntity>()
	var completedTasksShown: Boolean = false
		set(value) {
			if (value == field) return
			if (value) {
				items.addAll(hiddenItems)
				hiddenItems.clear()
			} else {
				val firstIndex = items.indexOfFirst { (it as? TaskInfo)?.task?.completed == true }
				if (firstIndex >= 0) {
					hiddenItems.addAll(items.drop(firstIndex))
					items.removeAll { (it as? TaskInfo)?.task?.completed == true }
				}
			}
			field = value
			notifyDataSetChanged()
		}
	var currentTaskWithOpenedDetails: TaskInfo? = null
		private set
	var currentTaskDetailsPopup: TaskDetailsPopup? = null
		private set
	val selectedTasks: List<TaskInfo>
		get() = selectedItems.mapNotNull { it as? TaskInfo }
	var completedTaskCount: Int = 0
		private set
	var windowBackgroundColor: Int = 0
		private set
	
	init {
		selectableItemsAdapterDelegate.selectableItemsAdapter = this
		draggableItemsAdapterDelegate.draggableItemsAdapter = this
		if (hasTaskDetails && controller.date == null) {
			throw IllegalArgumentException("Model supporting dates required to show task details")
		}
		registerViewType(ViewHolderFactory.create(NewTaskMarker::class.java) { _, container ->
			NewTaskViewHolder(this, container)
		})
		registerViewType(ViewHolderFactory.create(TaskInfo::class.java) { _, container ->
			TaskViewHolder(this, container)
		})
		registerViewType(ViewHolderFactory.create(SeparatorMarker::class.java) { _, container ->
			SeparatorViewHolder(this, container)
		})
		model.observe(lifecycleOwner, this)
	}
	
	override fun restoreInstanceState(savedState: Bundle?, callNotifyDataSetChanged: Boolean) {
		if (savedState != null) {
			super.restoreInstanceState(savedState, false)
			hiddenItems = savedState.getParcelableArrayList("hidden_items")!!
			completedTasksShown = savedState.getBoolean("completed_tasks_shown", false)
			completedTaskCount = savedState.getInt("completed_task_count", 0)
			if (!completedTasksShown && altSort) {
				completedTasksShown = true
			}
		} else {
			completedTasksShown = !hideCompletedTasks || altSort
			items.clear()
			addHeaders(items)
			items.add(NewTaskMarker())
		}
		selectableItemsAdapterDelegate.restoreInstanceState(savedState)
		if (callNotifyDataSetChanged) {
			notifyDataSetChanged()
		}
	}
	
	open fun addHeaders(items: MutableList<UniqueEntity>) {
	}
	
	override fun saveInstanceState(outState: Bundle) {
		super.saveInstanceState(outState)
		selectableItemsAdapterDelegate.saveInstanceState(outState)
		outState.putBoolean("completed_tasks_shown", completedTasksShown)
		outState.putParcelableArrayList("hidden_items", hiddenItems as ArrayList<out Parcelable>)
		outState.putInt("completed_task_count", completedTaskCount)
	}
	
	override fun getItemId(position: Int): Long = items[position].let {
		when (it) {
			is TaskInfo ->
				if (it.task.originDate == controller.date)
					it.task.scheduleTemplateId ?: it.id ?: -1L
				else
					it.id ?: -1L
			else -> it.id ?: -1L
		}
	}
	
	override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
		super.onAttachedToRecyclerView(recyclerView)
		draggableItemsAdapterDelegate.onAttachedToRecyclerView(recyclerView)
		var view: View = recyclerView
		while ((view.background as? ColorDrawable)?.color ?: 0 == 0 && view.parent is View) {
			view = view.parent as View
		}
		windowBackgroundColor = (view.background as? ColorDrawable)?.color ?: 0
	}
	
	override fun onChanged(tasks: List<TaskInfo>?) {
		if (tasks == null) return
		val newItems = ArrayList<UniqueEntity>(tasks.size + 1)
		addHeaders(newItems)
		hiddenItems.clear()
		var firstCompleted = true
		var hiddenEditingTask: TaskInfo? = null
		var newTaskMarkerAdded = false
		completedTaskCount = 0
		tasks.forEach {
			if (firstCompleted && it.task.completed) {
				if (!altSort) {
					newItems.add(NewTaskMarker())
					newItems.add(SeparatorMarker())
					newTaskMarkerAdded = true
				}
				firstCompleted = false
			}
			if (!it.task.completed || completedTasksShown) {
				newItems.add(it)
			} else {
				hiddenItems.add(it)
				if (editingViewHolder?.item?.id == (it as? TaskInfo)?.id) {
					hiddenEditingTask = it
				}
			}
			if (it.task.completed) {
				completedTaskCount++
			}
		}
		if (!newTaskMarkerAdded) {
			newItems.add(NewTaskMarker())
		}
		if (items != newItems) {
			if (hiddenEditingTask != null) {
				editingViewHolder?.bind(hiddenEditingTask!!)
				editingViewHolder?.finishTitleEdit(true)
				editingViewHolder = null
			}
			items = newItems
			updateSelectedItems(false)
			notifyDataSetChanged()
			NotificationsRefreshWorker.scheduleNow()
		}
	}
	
	override fun onSelectionChanged() {
		finishTitleEdit()
	}
	
	fun editTitle(id: Long) {
		ensureItemVisible(id) {
			(it as? TaskViewHolder)?.editTitle()
		}
	}
	
	fun saveTitleEdit() {
		editingViewHolder?.saveTitle()
	}
	
	open fun finishTitleEdit() {
		editingViewHolder?.finishTitleEdit(true)
	}
	
	fun deleteSelection() {
		controller.delete(*(selectedItems.mapNotNull { (it as? TaskInfo)?.task }).toTypedArray())
	}
	
	fun copySelection() {
		val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
		clipboardManager.primaryClip = ClipData.newPlainText("text",
				selectedItems.mapNotNull {
					(it as? TaskInfo)?.task?.title
				}.joinToString("\n"))
		Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
	}
	
	fun showTaskDetails(task: TaskInfo) {
		if (!hasTaskDetails) return
		if (sharedPreferences.getBoolean("enable_task_context_menu", true)) {
			val viewHolder = getViewHolder(task.id!!) as? TaskViewHolder
			if (viewHolder != null) {
				viewHolder.showContextMenu()
				return
			}
		}
		currentTaskWithOpenedDetails = task
		getViewHolder<TaskViewHolder>(task.id!!)?.bind(task)
		currentTaskDetailsPopup = TaskDetailsPopup(context, controller, task, PopupWindow.OnDismissListener {
			currentTaskDetailsPopup = null
			currentTaskWithOpenedDetails = null
			getViewHolder<TaskViewHolder>(task.id!!)?.bind(task)
		}).apply {
			show(getViewHolder<TaskViewHolder>(task.id!!)?.showDetailsImageButton)
		}
	}
	
	class NewTaskMarker : UniqueEntity {
		override val id: Long? = 0L
		override fun equals(other: Any?): Boolean = other is NewTaskMarker
		override fun hashCode(): Int = 0
		override fun describeContents(): Int = 0
		override fun writeToParcel(dest: Parcel, flags: Int) {
		}
		
		companion object CREATOR : Parcelable.Creator<NewTaskMarker> {
			override fun createFromParcel(source: Parcel): NewTaskMarker =
					NewTaskMarker()
			
			override fun newArray(size: Int): Array<NewTaskMarker?> =
					arrayOfNulls(size)
		}
	}
	
	class SeparatorMarker : UniqueEntity {
		override val id: Long? = Long.MIN_VALUE
		override fun equals(other: Any?): Boolean = other is NewTaskMarker
		override fun hashCode(): Int = 0
		override fun describeContents(): Int = 0
		override fun writeToParcel(dest: Parcel, flags: Int) {
		}
		
		companion object CREATOR : Parcelable.Creator<SeparatorMarker> {
			override fun createFromParcel(source: Parcel): SeparatorMarker =
					SeparatorMarker()
			
			override fun newArray(size: Int): Array<SeparatorMarker?> = arrayOfNulls(size)
		}
	}
}
