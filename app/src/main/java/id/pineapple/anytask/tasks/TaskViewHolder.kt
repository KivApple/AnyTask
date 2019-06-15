package id.pineapple.anytask.tasks

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.support.v4.app.FragmentActivity
import android.support.v7.view.menu.MenuBuilder
import android.support.v7.view.menu.MenuPopupHelper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.*
import id.pineapple.anytask.R
import id.pineapple.anytask.notes.NoteDialogFragment
import id.pineapple.recyclerviewutil.BaseViewHolder
import id.pineapple.recyclerviewutil.DraggableViewHolder
import id.pineapple.anytask.resolveColor

@SuppressLint("ClickableViewAccessibility")
class TaskViewHolder(
		val adapter: TaskListAdapter,
		parent: ViewGroup
): BaseViewHolder<TaskInfo>(adapter.context, R.layout.item_task, parent), DraggableViewHolder {
	private val titleEditText: EditText = itemView.findViewById(R.id.task_title_edit_text)
	private val completedCheckbox: CheckBox = itemView.findViewById(R.id.task_completed_checkbox)
	private val reorderHandleImageView: ImageView = itemView.findViewById(R.id.task_reorder_handle)
	private val deleteImageButton: ImageButton = itemView.findViewById(R.id.task_delete_button)
	val showDetailsImageButton: ImageButton = itemView.findViewById(R.id.task_show_details_button)
	private val infoTextView: TextView = itemView.findViewById(R.id.task_info_text_view)
	private val noteTextView: TextView = itemView.findViewById(R.id.task_note_text_view)
	private val editing: Boolean get() = titleEditText.isFocusable
	
	init {
		var lastTitleClickX = 0.0f
		var lastTitleClickY = 0.0f
		itemView.setOnTouchListener { view, event ->
			if (event.action == MotionEvent.ACTION_DOWN) {
				val sourceLocation = IntArray(2)
				view.getLocationOnScreen(sourceLocation)
				val targetLocation = IntArray(2)
				titleEditText.getLocationOnScreen(targetLocation)
				lastTitleClickX = event.x - (targetLocation[0] - sourceLocation[0])
				lastTitleClickY = event.y - (targetLocation[1] - sourceLocation[1])
			}
			false
		}
		itemView.setOnClickListener {
			if (adapter.isSelecting) {
				adapter.toggleItemSelection(adapterPosition)
			} else if (!editing) {
				if (adapter.hasTaskDetails) {
					if (lastTitleClickX < 0.0f || lastTitleClickX > titleEditText.width) {
						showDetailsImageButton.performClick()
						return@setOnClickListener
					}
				}
				editTitle(lastTitleClickX, lastTitleClickY)
			}
		}
		itemView.setOnLongClickListener {
			adapter.toggleItemSelection(adapterPosition)
			true
		}
		completedCheckbox.setOnCheckedChangeListener { _, isChecked ->
			item?.let {
				adapter.controller.setCompleted(it, isChecked)
			}
		}
		titleEditText.setOnTouchListener { _, event ->
			if (event.action == MotionEvent.ACTION_DOWN) {
				lastTitleClickX = event.x
				lastTitleClickY = event.y
			}
			false
		}
		titleEditText.setOnClickListener {
			if (adapter.isSelecting) {
				itemView.performClick()
			} else if (item != null && !editing) {
				editTitle(lastTitleClickX, lastTitleClickY)
			}
		}
		titleEditText.setOnLongClickListener {
			if (!editing) {
				itemView.performLongClick()
				true
			} else {
				false
			}
		}
		titleEditText.setOnFocusChangeListener { _, hasFocus ->
			if (!hasFocus) {
				if (adapter.hasTaskDetails) {
					deleteImageButton.rotation = 0.0f
					deleteImageButton.animate()
							.setDuration(150)
							.alpha(0.0f)
							.rotation(45.0f)
							.setInterpolator(DecelerateInterpolator(2.0f))
							.start()
					showDetailsImageButton.animate()
							.setDuration(150)
							.alpha(1.0f)
							.rotation(90.0f)
							.setInterpolator(AccelerateInterpolator(2.0f))
							.start()
				} else {
					deleteImageButton.animate()
							.alpha(0.0f)
							.start()
				}
				titleEditText.inputType =
						titleEditText.inputType or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
				finishTitleEdit()
			}
		}
		titleEditText.addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
			}
			
			override fun afterTextChanged(s: Editable) {
				item?.let {
					if (s.contains('\n')) {
						val lines = s.split('\n')
						titleEditText.setText(lines.first())
						titleEditText.setSelection(lines.first().length)
						adapter.controller.add(
								*lines.drop(1).toTypedArray(),
								completed = it.task.completed,
								after = it.task,
								priority = it.task.priority
						)
					}
				}
			}
			
			override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
			}
		})
		reorderHandleImageView.setOnTouchListener { _, event ->
			when (event.action) {
				MotionEvent.ACTION_DOWN -> {
					finishTitleEdit(true)
					if (!adapter.isSelecting) {
						adapter.startDrag(this)
					}
				}
			}
			false
		}
		deleteImageButton.setOnClickListener { view ->
			item?.let { task ->
				if (editing) {
					finishTitleEdit(true)
					view.post {
						adapter.controller.delete(task.task)
					}
				} else if (adapter.hasTaskDetails) {
					showDetailsImageButton.performClick()
				}
				true
			}
		}
		showDetailsImageButton.setOnClickListener { _ ->
			if (adapter.isSelecting) {
				itemView.performClick()
			} else {
				if (!editing) {
					if (adapter.editingViewHolder != null) {
						adapter.finishTitleEdit()
					} else {
						item?.let {
							adapter.showTaskDetails(it)
						}
					}
				} else {
					deleteImageButton.performClick()
				}
			}
		}
		showDetailsImageButton.setOnLongClickListener {
			itemView.performLongClick()
		}
		noteTextView.paintFlags = noteTextView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
		noteTextView.setOnClickListener {
			item?.note?.let { note ->
				NoteDialogFragment.newInstance(note)
						.show((adapter.context as FragmentActivity).supportFragmentManager,
								null)
			}
		}
	}
	
	override fun doBind(item: TaskInfo, oldItem: TaskInfo?) {
		val taskChanged = item.id != oldItem?.id
		oldItem?.let {
			if (!taskChanged) {
				if (editing && item.task.title != titleEditText.text.toString()) {
					saveTitle(item)
					return
				}
			}
		}
		if (taskChanged) {
			finishTitleEdit()
			deleteImageButton.alpha = 0.0f
			showDetailsImageButton.alpha = 1.0f
		}
		if (titleEditText.text.toString() != item.task.title) {
			titleEditText.setText(item.task.title)
		}
		if (item.task.completed) {
			titleEditText.paintFlags = titleEditText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
		} else {
			titleEditText.paintFlags = titleEditText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
		}
		titleEditText.setTextColor(resolveColor(adapter.context, when {
			item.task.completed -> R.attr.textColorDisabled
			else -> R.attr.textColorPrimary
		}))
		if (completedCheckbox.isChecked != item.task.completed) {
			completedCheckbox.isChecked = item.task.completed
		}
		val infoStrings = mutableListOf<String>()
		if (adapter.hasTaskDetails) {
			if (item.task.originDate != adapter.controller.date &&
					item.task.scheduleOptions.mode == TaskScheduleOptions.Mode.ONCE) {
				infoStrings.add(adapter.context.getString(
						if (item.task.originDate < adapter.controller.date)
							R.string.postponed_from
						else
							R.string.rescheduled_from,
						adapter.dateFormatter.print(item.task.originDate)
				))
			}
			if (item.task.pinned) {
				infoStrings.add(adapter.context.getString(R.string.pinned))
			}
			if (item.task.alarmTime != null) {
				infoStrings.add(adapter.context.getString(R.string.alarm_at,
						adapter.timeFormatter.print(item.task.alarmTime)))
			}
			if (item.task.isScheduled) {
				infoStrings.add(adapter.context.getString(R.string.repeatable_task))
			}
		}
		if (adapter.enableDebugInfo) {
			val debugInfo = mutableListOf(
					"pos=${item.task.position}",
					"savedPos=${item.task.savedPosition}"
			)
			if (item.task.scheduleOptions.mode == TaskScheduleOptions.Mode.ONCE) {
				debugInfo.add("date=${item.task.scheduleOptions.startDate}")
			}
			infoStrings.add(debugInfo.joinToString(", "))
		}
		if (infoStrings.isNotEmpty()) {
			infoTextView.text = infoStrings.joinToString("\n")
			infoTextView.visibility = View.VISIBLE
		} else {
			infoTextView.visibility = View.GONE
		}
		if (item.note != null) {
			noteTextView.text = item.note.getNormalizedTitle(adapter.context)
			noteTextView.visibility = View.VISIBLE
		} else {
			noteTextView.visibility = View.GONE
		}
		reorderHandleImageView.visibility = if (item.task.completed && !adapter.altSort) View.INVISIBLE else View.VISIBLE
		showDetailsImageButton.visibility = if (adapter.hasTaskDetails) View.VISIBLE else View.GONE
		itemView.setBackgroundColor(resolveColor(adapter.context, when {
			adapter.isItemSelected(adapterPosition) -> R.attr.selectedBackgroundColor
			(!item.task.completed || adapter.altSort) && item.task.priority == 1 ->
				R.attr.mediumPriorityTaskBackground
			(!item.task.completed || adapter.altSort) && item.task.priority == 2 ->
				R.attr.highPriorityTaskBackground
			(!item.task.completed || adapter.altSort) && item.task.priority >= 3 ->
				R.attr.highestPriorityTaskBackground
			else -> 0
		}).let { if (it != 0) it else adapter.windowBackgroundColor })
	}
	
	fun editTitle(x: Float = 0.0f, y: Float = 0.0f) {
		if (editing) return
		adapter.clearSelection()
		adapter.editingViewHolder = this
		titleEditText.isFocusable = true
		titleEditText.isFocusableInTouchMode = true
		titleEditText.requestFocus()
		titleEditText.postDelayed({
			if (!titleEditText.isFocused) return@postDelayed
			val eventTime = System.currentTimeMillis()
			val event1 = MotionEvent.obtain(eventTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0)
			val event2 = MotionEvent.obtain(eventTime, eventTime, MotionEvent.ACTION_UP, x, y, 0)
			titleEditText.onTouchEvent(event1)
			titleEditText.onTouchEvent(event2)
			event1.recycle()
			event2.recycle()
		}, 400)
		deleteImageButton.alpha = 0.0f
		if (adapter.hasTaskDetails) {
			showDetailsImageButton.rotation = 0.0f
			deleteImageButton.rotation = -45.0f
			showDetailsImageButton.animate()
					.setDuration(150)
					.alpha(0.0f)
					.rotation(45.0f)
					.setInterpolator(DecelerateInterpolator(2.0f))
					.start()
			deleteImageButton.animate()
					.setDuration(150)
					.alpha(1.0f)
					.rotation(0.0f)
					.setInterpolator(AccelerateInterpolator(2.0f))
					.start()
		} else {
			deleteImageButton.rotation = 0.0f
			deleteImageButton.animate()
					.alpha(1.0f)
					.start()
		}
		titleEditText.inputType =
				titleEditText.inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS.inv()
		(titleEditText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
				.showSoftInput(titleEditText, InputMethodManager.SHOW_IMPLICIT)
	}
	
	fun saveTitle(item: TaskInfo? = this.item) {
		if (!editing) return
		item?.let {
			val newTitle = titleEditText.text.toString()
			if (it.task.title != newTitle) {
				adapter.controller.updateTitle(it, newTitle)
			}
		}
	}
	
	fun finishTitleEdit(hideSoftInput: Boolean = false) {
		if (!editing) return
		if (hideSoftInput && adapter.editingViewHolder === this) {
			hideSoftInput()
		}
		saveTitle()
		titleEditText.isFocusable = false
		titleEditText.isFocusableInTouchMode = false
		if (adapter.editingViewHolder === this) {
			adapter.editingViewHolder = null
		}
	}
	
	private fun hideSoftInput() {
		(titleEditText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
				.hideSoftInputFromWindow(titleEditText.windowToken, 0)
	}
	
	@SuppressLint("RestrictedApi")
	fun showContextMenu() {
		item?.let { task ->
			val menu = MenuBuilder(adapter.context)
			MenuInflater(adapter.context).inflate(R.menu.menu_task_options, menu)
			menu.findItem(R.id.task_option_auto_postpone).isChecked =
					adapter.controller.willAutoPostpone(task)
			menu.findItem(R.id.task_option_clear_alarm).isVisible = task.task.alarmTime != null
			menu.findItem(R.id.task_option_pin).isChecked = task.task.pinned
			menu.setCallback(object : MenuBuilder.Callback {
				override fun onMenuItemSelected(menuBuilder: MenuBuilder, item: MenuItem): Boolean {
					when (item.itemId) {
						R.id.task_option_auto_postpone ->
							adapter.controller.setAutoPostpone(task,
									!adapter.controller.willAutoPostpone(task))
						R.id.task_option_postpone_to_tomorrow ->
							adapter.controller.postponeToTomorrow(task)
						R.id.task_option_reschedule_to -> adapter.controller.askReschedule(task)
						R.id.task_option_setup_alarm -> adapter.controller.askSetAlarm(task)
						R.id.task_option_clear_alarm -> adapter.controller.clearAlarm(task)
						R.id.task_option_set_priority -> adapter.controller.askSetPriority(task)
						R.id.task_option_note -> {
							TaskNotePickerDialogFragment.newInstance(task, adapter.controller.date!!)
									.show((adapter.context as FragmentActivity).supportFragmentManager,
											null)
						}
						R.id.task_option_repetition ->
							adapter.controller.askConfigureRepetition(task)
						R.id.task_option_pin ->
							adapter.controller.setPinned(task, !task.task.pinned)
						else -> return false
					}
					return true
				}
				
				override fun onMenuModeChange(menuBuilder: MenuBuilder) {
				}
			})
			val helper = MenuPopupHelper(adapter.context, menu, showDetailsImageButton)
			helper.setForceShowIcon(true)
			helper.show()
		}
	}
	
	override fun canMove(): Boolean = item?.task?.completed == false || adapter.altSort
	
	override fun canDrop(targetViewHolder: DraggableViewHolder): Boolean =
			targetViewHolder is TaskViewHolder &&
					(targetViewHolder.item?.task?.completed == false || adapter.altSort) &&
					targetViewHolder.item?.task?.priority == item?.task?.priority
	
	override fun dragFinished(moved: Boolean) {
		if (moved) {
			adapter.controller.saveOrder(adapter.items.mapNotNull { it as? TaskInfo })
		}
	}
}
