package id.pineapple.anytask

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.BottomNavigationView
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import id.pineapple.anytask.notes.*
import id.pineapple.recyclerviewutil.UniqueEntity
import id.pineapple.anytask.settings.SettingsFragment
import id.pineapple.anytask.tasks.*
import kotlinx.android.synthetic.main.activity_main.*
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.joda.time.Seconds

class MainActivity : UiHelperActivity(), BottomNavigationView.OnNavigationItemSelectedListener,
		ViewTreeObserver.OnGlobalLayoutListener {
	lateinit var fragmentHelper: MainActivityFragmentHelper
		private set
	private lateinit var sharedPreferences: SharedPreferences
	private lateinit var rootView: View
	var softKeyboardVisible = false
		private set
	private var lastStopTime: DateTime? = null
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setupNotifications(this)
		AlarmWorker.setupNotifications(this)
		NotificationsRefreshWorker.setupNotifications(this)
		NotificationsRefreshWorker.schedule()
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
		applyLocaleFromPreferences(sharedPreferences)
		val colorSchemeKey = sharedPreferences.getString("color_scheme",
				getString(R.string.color_scheme_default_key))
		val colorSchemeId = resources.getIdentifier(colorSchemeKey, "style",
				applicationContext.packageName)
		if (colorSchemeId != 0) {
			setTheme(colorSchemeId)
		}
		
		setContentView(R.layout.activity_main)
		setSupportActionBar(toolbar)
		rootView = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
		rootView.viewTreeObserver.addOnGlobalLayoutListener(this)
		fragmentHelper = MainActivityFragmentHelper(this)
		if (savedInstanceState != null) {
			fragmentHelper.restoreInstanceState(savedInstanceState.getBundle(STATE_FRAGMENT_HELPER)!!)
		}
		bottom_navigation_view.setOnNavigationItemSelectedListener(this)
		bottom_navigation_view.visibility = View.GONE
		if (savedInstanceState == null) {
			if (PinPadDialogFragment.pinEnabled(sharedPreferences)) {
				showPinPad(PinPadDialogFragment.Action.CHECK)
			} else {
				showTaskList()
			}
		} else {
			lastStopTime = DateTime(savedInstanceState.getLong(STATE_LAST_STOP_TIME))
		}
	}
	
	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putBundle(STATE_FRAGMENT_HELPER, fragmentHelper.saveInstanceState())
		outState.putLong(STATE_LAST_STOP_TIME, DateTime().millis)
	}
	
	override fun onStart() {
		super.onStart()
		if (lastStopTime != null && PinPadDialogFragment.pinEnabled(sharedPreferences)) {
			val lockTimeout = sharedPreferences.getString("lock_timeout",
					getString(R.string.lock_timeout_default_value))!!.toInt()
			if (lockTimeout > 0) {
				val now = DateTime()
				val timeSpent = Seconds.secondsBetween(lastStopTime, now).seconds
				if (timeSpent > lockTimeout) {
					showPinPad(PinPadDialogFragment.Action.CHECK)
				}
			}
		}
	}
	
	override fun onStop() {
		super.onStop()
		lastStopTime = DateTime()
	}
	
	override fun onBackPressed() {
		super.onBackPressed()
		fragment_container.post {
			fragmentHelper.clearSavedStates()
		}
	}
	
	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			android.R.id.home -> onBackPressed()
			else -> return super.onOptionsItemSelected(item)
		}
		return true
	}
	
	override fun onNavigationItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.nav_tasks -> {
				showTaskList()
				true
			}
			R.id.nav_notes -> {
				showNote()
				true
			}
			R.id.nav_settings -> {
				showSettings()
				true
			}
			else -> false
		}
	}
	
	override fun onGlobalLayout() {
		val ratio = (rootView.height.toFloat() / rootView.rootView.height)
		val isVisible = ratio < 0.85f
		if (isVisible != softKeyboardVisible) {
			softKeyboardVisible = isVisible
			fragmentHelper.handleSoftKeyboardVisibilityChanged()
		}
	}
	
	override fun findSnackBarContainer(): View {
		val dialogFragment = fragmentHelper.activeDialogFragment
		return if (dialogFragment != null && dialogFragment.dialog.isShowing)
			dialogFragment.view ?: fragment_container
		else
			fragment_container
	}
	
	fun showPinPad(action: PinPadDialogFragment.Action) =
			fragmentHelper.showFragment(PinPadDialogFragment.newInstance(action))
	
	override fun showTaskList(date: LocalDate) =
			fragmentHelper.showFragment(TaskListTabsFragment.newInstance(date))
	
	override fun editTaskTitle(id: Long) {
		listOf<TaskListInterface?>(
				fragmentHelper.taskListFragment,
				fragmentHelper.noteFragment,
				fragmentHelper.noteDialogFragment
		).forEach {
			it?.editTaskTitle(id)
		}
	}
	
	override fun finishTaskTitleEdit() {
		listOf<TaskListInterface?>(
				fragmentHelper.taskListFragment,
				fragmentHelper.noteFragment,
				fragmentHelper.noteDialogFragment
		).forEach {
			it?.finishTaskEdit()
		}
	}
	
	override fun dismissTaskDetails(id: Long) {
		if (fragmentHelper.taskListFragment?.adapter?.currentTaskWithOpenedDetails?.task?.id == id) {
			fragmentHelper.taskListFragment?.adapter?.currentTaskDetailsPopup?.dismiss()
		}
	}
	
	override fun showTaskRescheduleDialog(vararg tasks: TaskInfo, date: LocalDate) =
			TaskRescheduleDialogFragment.newInstance(tasks.toList(), date)
					.show(supportFragmentManager, null)
	
	override fun showTaskScheduleOptionsDialog(task: TaskInfo, date: LocalDate) =
			TaskScheduleOptionsDialogFragment.newInstance(task, date)
					.show(supportFragmentManager, null)
	
	override fun showTaskAlarmOptionsDialog(task: TaskInfo, date: LocalDate) =
			TaskAlarmOptionsDialogFragment.newInstance(task, date)
					.show(supportFragmentManager, null)
	
	override fun showTaskDeleteConfirmDialog(vararg tasks: Task, date: LocalDate) =
			TaskDeleteConfirmDialogFragment.newInstance(tasks.toList(), date)
					.show(supportFragmentManager, null)
	
	override fun showTaskPriorityDialog(task: TaskInfo, date: LocalDate) =
			TaskPriorityDialogFragment.newInstance(task, date)
					.show(supportFragmentManager, null)
	
	override fun showNote(vararg notePath: Note, sharedElement: View?) =
			fragmentHelper.showFragment(NoteFragment.newInstance(*notePath),
					sharedElement = sharedElement)
	
	override fun showNoteFolderDeleteConfirmDialog(vararg entities: UniqueEntity) =
			NoteFolderDeleteConfirmDialogFragment.newInstance(entities.toList())
					.show(supportFragmentManager, null)
	
	override fun showMoveNotesToFolderDialog(
			vararg entities: UniqueEntity,
			selectedPath: Array<Note>
	) = MoveNotesToFolderDialogFragment.newInstance(entities.toList(), selectedPath)
			.show(supportFragmentManager, null)
	
	override fun showCopyNotesToFolderDialog(
			vararg entities: UniqueEntity,
			selectedPath: Array<Note>
	) = CopyNotesToFolderDialogFragment.newInstance(entities.toList(), selectedPath)
			.show(supportFragmentManager, null)
	
	private fun showSettings() =
			fragmentHelper.showFragment(SettingsFragment.newInstance())
	
	companion object {
		private const val STATE_LAST_STOP_TIME = "last_stop_time"
		private const val STATE_FRAGMENT_HELPER = "fragment_helper"
	}
}
