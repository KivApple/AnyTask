package id.pineapple.anytask

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.support.v7.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.view.WindowManager
import id.pineapple.anytask.tasks.TaskInfo
import id.pineapple.anytask.tasks.TaskListController
import id.pineapple.anytask.tasks.TaskListPersistentModel
import kotlinx.android.synthetic.main.activity_alarm.*
import org.joda.time.LocalDate

class AlarmActivity : UiHelperActivity() {
	private lateinit var sharedPreferences: SharedPreferences
	private lateinit var vibrator: Vibrator
	private var mediaPlayer: MediaPlayer? = null
	private lateinit var model: TaskListPersistentModel
	private lateinit var controller: TaskListController
	private var task: TaskInfo? = null
	private var vibrating = false
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
		applyLocaleFromPreferences(sharedPreferences)
		setContentView(R.layout.activity_alarm)
		window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
				WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
		vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
		model = TaskListPersistentModel(LocalDate())
		controller = TaskListController(model, this, this)
		task_title_label.text = intent.getStringExtra(ARG_TASK_TITLE)
		mark_task_as_completed_checkbox.visibility = View.INVISIBLE
		alarm_dismiss_button.setOnClickListener {
			dismiss()
		}
		model.fetch(intent.getLongExtra(ARG_TASK_ID, 0)) {
			task = it
			if (it != null) {
				mark_task_as_completed_checkbox.visibility = View.VISIBLE
				if (it.task.alarmTime == null) {
					val intent = Intent(this, MainActivity::class.java)
					startActivity(intent)
					finish()
					return@fetch
				}
			}
			playAlarmRingtoneAndVibrate()
		}
	}
	
	private fun playAlarmRingtoneAndVibrate() {
		val ringtoneName = sharedPreferences.getString("alarm_ringtone", null)
		var ringtoneUri: Uri? = null
		if (ringtoneName?.isBlank() != true) {
			if (ringtoneName != null) {
				ringtoneUri = Uri.parse(ringtoneName)
			}
			if (ringtoneUri == null) {
				RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
						?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
						?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
			}
		}
		if (ringtoneUri != null) {
			try {
				mediaPlayer = MediaPlayer()
				mediaPlayer?.setDataSource(this, ringtoneUri)
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					mediaPlayer?.setAudioAttributes(AudioAttributes.Builder()
							.setUsage(AudioAttributes.USAGE_ALARM)
							.build())
				} else {
					mediaPlayer?.setAudioStreamType(AudioManager.STREAM_ALARM)
				}
				mediaPlayer?.isLooping = true
			} catch (e: Throwable) {
				Log.e(this::class.java.name, "Failed to setup alarm sound player", e)
				mediaPlayer = null
			}
		}
		mediaPlayer?.prepare()
		mediaPlayer?.start()
		if (sharedPreferences.getBoolean("notifications_alarm_vibrate", true)) {
			val pattern = longArrayOf(
					0, 1000, 0, 200, 800
			)
			val repeat = 2
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				vibrator.vibrate(
						VibrationEffect.createWaveform(pattern, repeat),
						AudioAttributes.Builder()
								.setUsage(AudioAttributes.USAGE_ALARM)
								.build()
				)
			} else {
				vibrator.vibrate(pattern, repeat)
			}
			vibrating = true
		}
	}
	
	override fun onDestroy() {
		if (vibrating) {
			vibrator.cancel()
		}
		mediaPlayer?.stop()
		super.onDestroy()
	}
	
	override fun onBackPressed() {
	}
	
	private fun dismiss() {
		task?.let { task ->
			if (task.task.alarmTime != null) {
				controller.setCompleted(task, mark_task_as_completed_checkbox.isChecked,
						clearAlarm = true)
			}
		}
		finish()
	}
	
	companion object {
		const val ARG_TASK_ID = "task_id"
		const val ARG_TASK_TITLE = "task_title"
	}
}
