package id.pineapple.anytask.tasks

import android.os.Bundle
import android.os.Parcelable
import android.support.design.widget.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import id.pineapple.anytask.*
import kotlinx.android.synthetic.main.dialog_confirm_task_delete.*
import org.joda.time.LocalDate
import java.util.*

class TaskDeleteConfirmDialogFragment : BottomSheetDialogFragment() {
	private lateinit var tasks: List<Task>
	private lateinit var date: LocalDate
	private lateinit var model: TaskListPersistentModel
	private lateinit var controller: TaskListController
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		arguments!!.let {
			tasks = it.getParcelableArrayList(ARG_TASKS)!!
			date = daysToLocalDate(it.getInt(ARG_DATE))
		}
		model = TaskListPersistentModel(date)
		controller = TaskListController(model, this, context as UiHelper,
				lightweight = true)
	}
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
			inflater.inflate(R.layout.dialog_confirm_task_delete, container, false)
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		fixLandscapeHeight()
		task_confirm_delete_button.setOnClickListener {
			dismiss()
			controller.delete(*tasks.toTypedArray(), force = true)
		}
		task_cancel_delete_button.setOnClickListener {
			dismiss()
		}
	}
	
	companion object {
		private const val ARG_TASKS = "tasks"
		private const val ARG_DATE = "date"
		
		fun newInstance(tasks: List<Task>, date: LocalDate): TaskDeleteConfirmDialogFragment =
				TaskDeleteConfirmDialogFragment().apply {
					arguments = Bundle().apply {
						putParcelableArrayList(ARG_TASKS,
								tasks.toMutableList() as ArrayList<out Parcelable>)
						putInt(ARG_DATE, localDateToDays(date))
					}
				}
	}
}
