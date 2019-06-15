package id.pineapple.anytask.settings

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v14.preference.SwitchPreference
import android.support.v4.app.FragmentActivity
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceManager
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat
import com.takisoft.fix.support.v7.preference.RingtonePreference
import id.pineapple.anytask.*
import java.io.File

class SettingsFragment: PreferenceFragmentCompat(),
		SharedPreferences.OnSharedPreferenceChangeListener {
	override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
		context?.applyLocaleFromPreferences(preferenceManager.sharedPreferences)
		setPreferencesFromResource(R.xml.pref_main, rootKey)
		findPreference("color_scheme").let { preference ->
			bindPreferenceSummaryToValue(preference)
			preference.setOnPreferenceChangeListener { _, newValue ->
				preference.summary = newValue?.toString()
				restartMainActivity()
				true
			}
		}
		findPreference("language").let { preference ->
			bindPreferenceSummaryToValue(preference)
			preference.setOnPreferenceChangeListener { _, newValue ->
				preference.summary = newValue?.toString()
				restartMainActivity()
				true
			}
		}
		bindPreferenceSummaryToValue(findPreference("alarm_ringtone"))
		findPreference("enable_pin").let {
			(it as SwitchPreference).isChecked = PinPadDialogFragment.pinEnabled(it.sharedPreferences)
			it.setOnPreferenceChangeListener { _, value ->
				val enabled = value as Boolean
				if (enabled) {
					(context as MainActivity).showPinPad(PinPadDialogFragment.Action.SET)
				} else {
					(context as MainActivity).showPinPad(PinPadDialogFragment.Action.CLEAR)
				}
				false
			}
			findPreference("change_pin").isVisible = it.isChecked
		}
		findPreference("change_pin").setOnPreferenceClickListener {
			(context as MainActivity).showPinPad(PinPadDialogFragment.Action.SET)
			true
		}
		bindPreferenceSummaryToValue(findPreference("lock_timeout"))
		findPreference("export_data_to_file").setOnPreferenceClickListener { _ ->
			FilePickerDialogFragment.newInstance(false).let {
				it.setTargetFragment(this, REQUEST_EXPORT_TO_FILE)
				it.show(activity!!.supportFragmentManager, null)
			}
			true
		}
		findPreference("import_data_from_file").setOnPreferenceClickListener { _ ->
			FilePickerDialogFragment.newInstance(true).let {
				it.setTargetFragment(this, REQUEST_IMPORT_FROM_FILE)
				it.show(activity!!.supportFragmentManager, null)
			}
			true
		}
		findPreference("show_last_crash_log").setOnPreferenceClickListener {
			LastCrashLogDialogFragment.newInstance().show(
					(activity as FragmentActivity).supportFragmentManager,
					null
			)
			true
		}
	}
	
	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
		findPreference("enable_pin").let {
			val savedListener = it.onPreferenceChangeListener
			it.onPreferenceChangeListener = null
			(it as SwitchPreference).isChecked = PinPadDialogFragment.pinEnabled(it.sharedPreferences)
			it.onPreferenceChangeListener = savedListener
			findPreference("change_pin").isVisible = it.isChecked
		}
	}
	
	override fun onResume() {
		super.onResume()
		PreferenceManager.getDefaultSharedPreferences(context)
				.registerOnSharedPreferenceChangeListener(this)
	}
	
	override fun onPause() {
		PreferenceManager.getDefaultSharedPreferences(context)
				.unregisterOnSharedPreferenceChangeListener(this)
		super.onPause()
	}
	
	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		when (requestCode) {
			REQUEST_EXPORT_TO_FILE -> {
				if (resultCode == Activity.RESULT_OK && data != null) {
					val file = File(data.getStringExtra("selectedFile"))
					if (file.exists()) {
						ConfirmDialogFragment.newInstance(
								getString(R.string.ask_override_file, file.absolutePath),
								getString(R.string.continue_),
								getString(R.string.cancel),
								data
						).let {
							it.setTargetFragment(this, REQUEST_CONFIRM_EXPORT_TO_FILE)
							it.show(activity!!.supportFragmentManager, null)
						}
					} else {
						DataExportWorker.schedule(file)
					}
				}
				return
			}
			REQUEST_CONFIRM_EXPORT_TO_FILE -> {
				if (resultCode == Activity.RESULT_OK && data != null) {
					DataExportWorker.schedule(File(data.getStringExtra("selectedFile")))
				}
				return
			}
			REQUEST_IMPORT_FROM_FILE -> {
				if (resultCode == Activity.RESULT_OK && data != null) {
					ConfirmDialogFragment.newInstance(
							getString(R.string.confirm_import),
							getString(R.string.continue_),
							getString(R.string.cancel),
							data
					).let {
						it.setTargetFragment(this, REQUEST_CONFIRM_IMPORT_FROM_FILE)
						it.show(activity!!.supportFragmentManager, null)
					}
				}
				return
			}
			REQUEST_CONFIRM_IMPORT_FROM_FILE -> {
				if (resultCode == Activity.RESULT_OK && data != null) {
					val file = File(data.getStringExtra("selectedFile"))
					val workerId = DataImportWorker.schedule(file)
					ImportProgressDialogFragment.newInstance(workerId)
							.show(activity!!.supportFragmentManager, null)
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, data)
	}
	
	private fun restartMainActivity() {
		activity!!.finish()
		val intent = Intent(context, MainActivity::class.java)
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		startActivity(intent)
	}
	
	private val sBindPreferenceSummaryToValueListener = Preference.OnPreferenceChangeListener { preference, value ->
		val stringValue = value?.toString()
		if (preference is ListPreference) {
			val index = preference.findIndexOfValue(stringValue)
			preference.setSummary(if (index >= 0) preference.entries[index] else null)
		} else if (preference is RingtonePreference) {
			preference.summary = if (value != null && preference.ringtoneTitle != null)
				preference.ringtoneTitle
			else
				getString(R.string.silent)
		} else {
			preference.summary = stringValue
		}
		true
	}
	
	private fun bindPreferenceSummaryToValue(preference: Preference) {
		preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener
		sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
				PreferenceManager
						.getDefaultSharedPreferences(preference.context)
						.getString(preference.key, ""))
	}
	
	companion object {
		private const val REQUEST_EXPORT_TO_FILE = 1
		private const val REQUEST_CONFIRM_EXPORT_TO_FILE = 2
		private const val REQUEST_IMPORT_FROM_FILE = 3
		private const val REQUEST_CONFIRM_IMPORT_FROM_FILE = 4
		
		@JvmStatic
		fun newInstance() = SettingsFragment()
	}
}
