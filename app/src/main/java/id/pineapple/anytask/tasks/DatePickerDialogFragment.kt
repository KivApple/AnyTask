package id.pineapple.anytask.tasks

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import id.pineapple.anytask.R
import id.pineapple.anytask.localDateToDays
import org.joda.time.LocalDate

class DatePickerDialogFragment : TaskCalendarDialogFragment() {
	private var hasDeleteButton: Boolean = false
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		arguments!!.let {
			hasDeleteButton = it.getBoolean(ARG_HAS_DELETE_BUTTON)
		}
	}
	
	override val negativeButtonText: String?
		get() = getString(if (hasDeleteButton) R.string.clear else R.string.cancel)
	
	override fun onNegativeButtonClicked() {
		if (hasDeleteButton) {
			targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, null)
		}
	}
	
	override val positiveButtonText: String?
		get() = getString(R.string.select)
	
	override fun onPositiveButtonClicked() {
		targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, Intent().apply {
			putExtra(RESULT_EXTRA_DATE, localDateToDays(selectedDate))
		})
	}
	
	override fun onDateClicked(date: LocalDate) {
	}
	
	companion object {
		private const val ARG_HAS_DELETE_BUTTON = "has_delete_button"
		
		const val RESULT_EXTRA_DATE = "date"
		
		@JvmStatic
		fun newInstance(initialDate: LocalDate, hasDeleteButton: Boolean = false) =
				DatePickerDialogFragment().apply {
					arguments = TaskCalendarDialogFragment.makeArguments(initialDate).apply {
						putBoolean(ARG_HAS_DELETE_BUTTON, hasDeleteButton)
					}
				}
	}
}
