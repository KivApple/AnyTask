package id.pineapple.anytask.tasks

import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.PreferenceManager
import android.support.v7.view.ActionMode
import android.view.*
import id.pineapple.anytask.R
import id.pineapple.anytask.daysToLocalDate
import id.pineapple.anytask.localDateToDays
import id.pineapple.recyclerviewutil.SelectableItemsAdapterDelegate
import id.pineapple.recyclerviewutil.UniqueEntity
import kotlinx.android.synthetic.main.fragment_task_list.*
import org.joda.time.LocalDate

class TaskListFragment : Fragment(), SelectableItemsAdapterDelegate.OnSelectionChangedListener,
		ActionMode.Callback, TaskListInterface {
	private lateinit var sharedPreferences: SharedPreferences
	private lateinit var date: LocalDate
	private lateinit var model: TaskListModel
	var adapter: TaskListAdapter? = null
		private set
	private var actionMode: ActionMode? = null
	private var savedAdapterState: Bundle? = null
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
		arguments!!.let {
			date = daysToLocalDate(it.getInt(ARG_DATE))
		}
		savedAdapterState = savedInstanceState?.getBundle(STATE_ADAPTER_DATA)
	}
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
							  savedInstanceState: Bundle?): View? {
		return inflater.inflate(R.layout.fragment_task_list, container, false)
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		model = TaskListPersistentModel(
				date,
				altSort = !sharedPreferences.getBoolean("move_completed_tasks_down", true)
		)
		(model as? TaskListArrayModel)?.view = view
		adapter = TaskListAdapter(
				model,
				true,
				sharedPreferences.getBoolean("hide_completed_tasks", false),
				view.context,
				this
		)
		adapter!!.restoreInstanceState(savedAdapterState)
		task_recycler_view.adapter = adapter
		task_recycler_view.setOnTouchListener { _, event ->
			when (event.action) {
				MotionEvent.ACTION_DOWN -> {
					val child = task_recycler_view.findChildViewUnder(event.x, event.y)
					if (child == null) {
						adapter?.finishTitleEdit()
					}
				}
			}
			false
		}
		adapter!!.addOnSelectionChangedListener(this)
	}
	
	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		adapter?.let {
			savedAdapterState = it.saveInstanceState()
			outState.putBundle(STATE_ADAPTER_DATA, savedAdapterState)
		}
	}
	
	override fun onDestroyView() {
		super.onDestroyView()
		adapter = null
		(model as? TaskListArrayModel)?.view = null
	}
	
	override fun onPause() {
		adapter?.saveTitleEdit()
		super.onPause()
	}
	
	override fun onStop() {
		finishActionMode()
		super.onStop()
	}
	
	override fun editTaskTitle(id: Long) {
		adapter?.editTitle(id)
	}
	
	override fun finishTaskEdit() {
		adapter?.finishTitleEdit()
	}
	
	override fun onSelectionChanged(selectedItems: Collection<UniqueEntity>) {
		if (selectedItems.isNotEmpty()) {
			if (actionMode != null) {
				actionMode?.invalidate()
			} else {
				actionMode = (context!! as AppCompatActivity).startSupportActionMode(this)
			}
		} else {
			actionMode?.finish()
		}
	}
	
	override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
		mode.menuInflater.inflate(R.menu.menu_task_actions, menu)
		return true
	}
	
	override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
		return false
	}
	
	override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.action_task_copy -> adapter?.copySelection()
			R.id.action_task_reschedule_to ->
				adapter?.let {
					it.controller.askReschedule(*it.selectedTasks.toTypedArray())
				}
			R.id.action_task_postpone_to_tomorrow ->
				adapter?.let {
					it.controller.postponeToTomorrow(*it.selectedTasks.toTypedArray())
				}
			R.id.action_task_delete -> adapter?.deleteSelection()
			else -> return false
		}
		return true
	}
	
	override fun onDestroyActionMode(mode: ActionMode) {
		actionMode = null
		adapter?.clearSelection()
	}
	
	fun finishActionMode() {
		actionMode?.finish()
	}
	
	companion object {
		private const val ARG_DATE = "date"
		
		private const val STATE_ADAPTER_DATA = "adapter_data"
		
		@JvmStatic
		fun newInstance(date: LocalDate) =
				TaskListFragment().apply {
					arguments = Bundle().apply {
						putInt(ARG_DATE, localDateToDays(date))
					}
				}
	}
}
