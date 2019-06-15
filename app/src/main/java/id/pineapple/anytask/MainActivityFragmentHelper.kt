package id.pineapple.anytask

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.view.ViewCompat
import android.support.v7.app.ActionBar
import android.view.View
import android.widget.LinearLayout
import id.pineapple.anytask.notes.NoteDialogFragment
import id.pineapple.anytask.notes.NoteFragment
import id.pineapple.anytask.settings.SettingsFragment
import id.pineapple.anytask.tasks.TaskListFragment
import id.pineapple.anytask.tasks.TaskListTabsFragment
import kotlinx.android.synthetic.main.activity_main.*

class MainActivityFragmentHelper(
		private val activity: MainActivity
) : FragmentManager.FragmentLifecycleCallbacks() {
	private val resumedFragments = mutableSetOf<Fragment>()
	var pinPadDialogFragment: PinPadDialogFragment? = null
		private set
	private var taskListTabsFragment: TaskListTabsFragment? = null
	val taskListFragment: TaskListFragment?
		get() = taskListTabsFragment?.taskListFragment
	var noteFragment: NoteFragment? = null
		private set
	var noteDialogFragment: NoteDialogFragment? = null
		private set
	var activeDialogFragment: DialogFragment? = null
		private set
	val dialogFragments = mutableSetOf<DialogFragment>()
	private var savedActionBarContentInsetsStart: Int? = null
	private var savedActionBarContentInsetsEnd: Int? = null
	private var taskListTabsFragmentSavedState: FragmentState? = null
	private var noteFragmentSavedState: FragmentState? = null
	private var settingsFragmentSavedState: FragmentState? = null
	private var bottomNavigationViewHiddenBySoftKeyboard = false
	private var currentActionBarCustomViewProvider: ActionBarCustomViewProvider? = null
	
	init {
		activity.supportFragmentManager.registerFragmentLifecycleCallbacks(this, false)
	}
	
	fun saveInstanceState(): Bundle = Bundle().apply {
		putParcelable("taskListTabs", taskListTabsFragmentSavedState)
		putParcelable("note", noteFragmentSavedState)
		putParcelable("settings", settingsFragmentSavedState)
	}
	
	fun restoreInstanceState(savedState: Bundle) {
		taskListTabsFragmentSavedState = savedState.getParcelable("taskListTabs")
		noteFragmentSavedState = savedState.getParcelable("note")
		settingsFragmentSavedState = savedState.getParcelable("settings")
	}
	
	override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
		when (f) {
			is PinPadDialogFragment -> pinPadDialogFragment = f
			is TaskListTabsFragment -> taskListTabsFragment = f
			is NoteFragment -> noteFragment = f
			is NoteDialogFragment -> noteDialogFragment = f
		}
		if (f is DialogFragment) {
			dialogFragments.add(f)
		}
	}
	
	override fun onFragmentStopped(fm: FragmentManager, f: Fragment) {
		when (f) {
			is PinPadDialogFragment -> pinPadDialogFragment = null
			is TaskListTabsFragment -> taskListTabsFragment = null
			is NoteFragment -> if (noteFragment == f) {
				noteFragment = null
			}
			is NoteDialogFragment -> noteDialogFragment = null
		}
		if (f is DialogFragment) {
			dialogFragments.remove(f)
		}
	}
	
	override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
		resumedFragments.add(f)
		if (f is DialogFragment) {
			activeDialogFragment = f
		}
		handleResumedFragmentsChanged()
	}
	
	override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
		resumedFragments.remove(f)
		when (f) {
			is TaskListTabsFragment ->
				taskListTabsFragmentSavedState = FragmentState(fm, f)
			is NoteFragment ->
				noteFragmentSavedState = FragmentState(fm, f)
			is SettingsFragment ->
				settingsFragmentSavedState = FragmentState(fm, f)
		}
		if (f is DialogFragment && activeDialogFragment == f) {
			activeDialogFragment = null
		}
		handleResumedFragmentsChanged()
	}
	
	private fun handleResumedFragmentsChanged() {
		val actionBarTitleProvider = resumedFragments.firstOrNull {
			it is ActionBarTitleProvider
		} as? ActionBarTitleProvider
		val actionBarCustomViewProvider = resumedFragments.firstOrNull {
			it is ActionBarCustomViewProvider
		} as? ActionBarCustomViewProvider
		if (actionBarCustomViewProvider != currentActionBarCustomViewProvider) {
			currentActionBarCustomViewProvider = actionBarCustomViewProvider
			val actionBarCustomView = actionBarCustomViewProvider?.onCreateActionBarCustomView(
					activity.layoutInflater, LinearLayout(activity.supportActionBar?.themedContext)
			)
			activity.supportActionBar?.setCustomView(
					actionBarCustomView,
					(actionBarCustomView?.layoutParams as? LinearLayout.LayoutParams)?.let {
						ActionBar.LayoutParams(it.width, it.height, it.gravity)
					}
			)
			if (actionBarCustomView != null) {
				if (savedActionBarContentInsetsStart == null) {
					savedActionBarContentInsetsStart = activity.toolbar.contentInsetStart
				}
				if (savedActionBarContentInsetsEnd == null) {
					savedActionBarContentInsetsEnd = activity.toolbar.contentInsetEnd
				}
				activity.toolbar.setContentInsetsAbsolute(0, 0)
			} else {
				if (savedActionBarContentInsetsStart != null &&
						savedActionBarContentInsetsEnd != null) {
					activity.toolbar.setContentInsetsAbsolute(
							savedActionBarContentInsetsStart!!,
							savedActionBarContentInsetsEnd!!
					)
				}
			}
			activity.supportActionBar?.setDisplayShowTitleEnabled(actionBarCustomView == null)
			activity.supportActionBar?.setDisplayShowCustomEnabled(actionBarCustomView != null)
		}
		val actionBarTitle = actionBarTitleProvider?.actionBarTitle
				?: activity.getString(R.string.app_name)
		val actionBarSubtitle = actionBarTitleProvider?.actionBarSubtitle
		activity.supportActionBar?.title = actionBarTitle
		activity.supportActionBar?.subtitle = actionBarSubtitle
		var navigationItemId: Int? = null
		resumedFragments.forEach {
			when (it) {
				is TaskListTabsFragment -> navigationItemId = R.id.nav_tasks
				is NoteFragment -> navigationItemId = R.id.nav_notes
				is SettingsFragment -> navigationItemId = R.id.nav_settings
			}
		}
		if (navigationItemId != null) {
			activity.bottom_navigation_view.setOnNavigationItemSelectedListener(null)
			activity.bottom_navigation_view.selectedItemId = navigationItemId!!
			activity.bottom_navigation_view.setOnNavigationItemSelectedListener(activity)
			if (activity.softKeyboardVisible) {
				activity.bottom_navigation_view.visibility = View.GONE
				bottomNavigationViewHiddenBySoftKeyboard = true
			} else {
				if (bottomNavigationViewHiddenBySoftKeyboard) {
					activity.bottom_navigation_view.postDelayed({
						activity.bottom_navigation_view.visibility = View.VISIBLE
					}, 100)
					bottomNavigationViewHiddenBySoftKeyboard = false
				} else {
					activity.bottom_navigation_view.visibility = View.VISIBLE
				}
			}
		} else {
			activity.bottom_navigation_view.visibility = View.GONE
			bottomNavigationViewHiddenBySoftKeyboard = false
		}
		activity.supportActionBar?.show()
		activity.supportActionBar?.setDisplayHomeAsUpEnabled(navigationItemId == null ||
				(resumedFragments.firstOrNull {
					it is NoteFragment
				} as? NoteFragment)?.notePath?.isNotEmpty() == true)
		resumedFragments.forEach {
			if (it is OnSoftKeyboardVisibilityChangedListener) {
				it.onSoftKeyboardVisibilityChangedListener(activity.softKeyboardVisible)
			}
		}
	}
	
	fun handleSoftKeyboardVisibilityChanged() {
		handleResumedFragmentsChanged()
	}
	
	fun showFragment(fragment: Fragment, sharedElement: View? = null) {
		when (fragment) {
			is PinPadDialogFragment -> {
				if (pinPadDialogFragment != null && pinPadDialogFragment?.action == fragment.action) {
					return
				}
			}
			is TaskListTabsFragment -> {
				val prevFragment = resumedFragments.firstOrNull {
					it is TaskListTabsFragment
				} as? TaskListTabsFragment
				if (prevFragment != null && prevFragment.currentDate == fragment.currentDate) {
					return
				}
				if (prevFragment == null) taskListTabsFragmentSavedState?.restore(fragment)
				taskListTabsFragmentSavedState = null
				for (i in 0 until activity.supportFragmentManager.backStackEntryCount) {
					activity.supportFragmentManager.popBackStack()
				}
			}
			is NoteFragment -> {
				val prevFragment = resumedFragments.firstOrNull {
					it is NoteFragment
				} as? NoteFragment
				if (prevFragment != null) {
					if (prevFragment.notePath!!.contentEquals(fragment.notePath!!)) {
						return
					}
				} else {
					noteFragmentSavedState?.restore(fragment)
				}
				noteFragmentSavedState = null
				if (fragment.notePath!!.isEmpty()) {
					for (i in 0 until activity.supportFragmentManager.backStackEntryCount) {
						activity.supportFragmentManager.popBackStack()
					}
				}
			}
			is SettingsFragment -> {
				val prevFragment = resumedFragments.firstOrNull {
					it is SettingsFragment
				}
				if (prevFragment != null) {
					return
				}
				settingsFragmentSavedState?.restore(fragment)
				settingsFragmentSavedState = null
				for (i in 0 until activity.supportFragmentManager.backStackEntryCount) {
					activity.supportFragmentManager.popBackStack()
				}
			}
		}
		if (fragment is DialogFragment) {
			fragment.show(activity.supportFragmentManager, null)
			return
		}
		val transaction = activity.supportFragmentManager.beginTransaction()
		transaction.replace(R.id.fragment_container, fragment)
		if (sharedElement != null) {
			transaction.addSharedElement(sharedElement,
					ViewCompat.getTransitionName(sharedElement)
							?: throw IllegalArgumentException("Shared element view should have transitionName set"))
		}
		if (fragment !is TaskListTabsFragment) {
			transaction.addToBackStack(null)
		}
		transaction.commit()
	}
	
	fun clearSavedStates() {
		taskListTabsFragmentSavedState = null
		noteFragmentSavedState = null
		settingsFragmentSavedState = null
	}
	
	class FragmentState(
			private val arguments: Bundle?,
			private val state: Fragment.SavedState?
	): Parcelable {
		constructor(
				fragmentManager: FragmentManager,
				fragment: Fragment
		): this(fragment.arguments, fragmentManager.saveFragmentInstanceState(fragment))
		
		fun restore(fragment: Fragment) {
			fragment.arguments = arguments
			fragment.setInitialSavedState(state)
		}
		
		override fun describeContents(): Int = 0
		
		override fun writeToParcel(dest: Parcel, flags: Int) {
			dest.writeParcelable(arguments, 0)
			dest.writeParcelable(state, 0)
		}
		
		companion object CREATOR: Parcelable.Creator<FragmentState> {
			override fun createFromParcel(source: Parcel): FragmentState =
					FragmentState(
							source.readParcelable<Bundle>(Bundle::class.java.classLoader),
							source.readParcelable(Fragment.SavedState::class.java.classLoader)
					)
			
			override fun newArray(size: Int): Array<FragmentState?> = arrayOfNulls(size)
		}
	}
}
