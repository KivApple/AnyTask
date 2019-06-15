package id.pineapple.anytask.tasks

import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import id.pineapple.anytask.*
import kotlinx.android.synthetic.main.dialog_task_calendar.*
import org.joda.time.LocalDate

open class TaskCalendarDialogFragment : BottomSheetDialogFragment() {
	protected lateinit var date: LocalDate
	protected lateinit var model: TaskListModel
		private set
	protected lateinit var controller: TaskListController
		private set
	protected var selectedDate: LocalDate = LocalDate()
		set(value) {
			field = value
			task_calendar_view?.selectedDate = value
		}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		arguments!!.let {
			date = daysToLocalDate(it.getInt(ARG_DATE))
		}
		model = TaskListPersistentModel(date)
		controller = TaskListController(model, this, context as UiHelper,
				lightweight = true)
		selectedDate = date
	}
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
			inflater.inflate(R.layout.dialog_task_calendar, container, false)
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		fixLandscapeHeight()
		task_calendar_view.selectedDate = selectedDate
		positiveButtonText.let {
			if (it != null) {
				positive_button.text = it
			} else {
				positive_button.visibility = View.GONE
			}
		}
		negativeButtonText.let {
			if (it != null) {
				negative_button.text = it
			} else {
				negative_button.visibility = View.GONE
			}
		}
		task_calendar_view.onDateChangeListener = {
			selectedDate = it
			onDateClicked(selectedDate)
		}
		positive_button.setOnClickListener {
			dismiss()
			onPositiveButtonClicked()
		}
		negative_button.setOnClickListener {
			dismiss()
			onNegativeButtonClicked()
		}
	}
	
	protected open val positiveButtonText: String? = null
	
	protected open fun onPositiveButtonClicked() {
	}
	
	protected open val negativeButtonText: String? = null
	
	protected open fun onNegativeButtonClicked() {
	}
	
	protected open fun onDateClicked(date: LocalDate) {
		dismiss()
		(context as UiHelper).showTaskList(date)
	}
	
	companion object {
		private const val ARG_DATE = "date"
		
		@JvmStatic
		protected fun makeArguments(date: LocalDate) = Bundle().apply {
			putInt(ARG_DATE, localDateToDays(date))
		}
		
		@JvmStatic
		fun newInstance(date: LocalDate) =
				TaskCalendarDialogFragment().apply {
					arguments = makeArguments(date)
				}
	}
}
