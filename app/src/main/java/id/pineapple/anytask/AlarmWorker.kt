package id.pineapple.anytask

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.app.TaskStackBuilder
import androidx.work.*
import id.pineapple.anytask.tasks.TaskInfo
import org.joda.time.LocalTime
import java.util.concurrent.TimeUnit

class AlarmWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
	override fun doWork(): Result {
		val now = LocalTime()
		val whenMillis = inputData.getLong(ARG_WHEN, 0)
		val taskId = inputData.getLong(ARG_TASK_ID, 0)
		val taskTitle = inputData.getString(ARG_TASK_TITLE) ?: "(null)"
		val context = applicationContext
		val notificationManager = NotificationManagerCompat.from(context)
		val notificationBuilder = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			notificationBuilder.setSmallIcon(R.drawable.ic_alarm_gray_24dp)
		} else {
			notificationBuilder.setSmallIcon(R.mipmap.ic_launcher)
		}
		notificationBuilder.priority = NotificationCompat.PRIORITY_HIGH
		notificationBuilder.setCategory(NotificationCompat.CATEGORY_ALARM)
		notificationBuilder.setContentTitle(context.getString(R.string.alarm))
		notificationBuilder.setContentText(taskTitle)
		notificationBuilder.setWhen(whenMillis)
		notificationBuilder.setContentIntent(
				TaskStackBuilder.create(context)
						.addParentStack(AlarmActivity::class.java)
						.addNextIntent(Intent(context, AlarmActivity::class.java).apply {
							putExtra(AlarmActivity.ARG_TASK_ID, taskId)
							putExtra(AlarmActivity.ARG_TASK_TITLE, taskTitle)
							addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
						})
						.getPendingIntent(PENDING_INTENT_ID, PendingIntent.FLAG_CANCEL_CURRENT)
		)
		val notification = notificationBuilder.build()
		val notificationId = ALARM_NOTIFICATION_BASE_ID + now.millisOfDay
		notificationManager.notify(notificationId, notification)
		context.startActivity(Intent(context, AlarmActivity::class.java).apply {
			putExtra(AlarmActivity.ARG_TASK_ID, taskId)
			putExtra(AlarmActivity.ARG_TASK_TITLE, taskTitle)
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		})
		return Result.SUCCESS
	}
	
	companion object {
		private const val TAG = "AlarmWorker"
		private const val ARG_WHEN = "when"
		private const val ARG_TASK_ID = "task_id"
		private const val ARG_TASK_TITLE = "task_title"
		private const val ALARM_CHANNEL_ID = "alarms"
		private const val ALARM_NOTIFICATION_BASE_ID = 1000
		private const val PENDING_INTENT_ID = 2
		
		fun setupNotifications(context: Context) {
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
				(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).apply {
					if (getNotificationChannel(ALARM_CHANNEL_ID) == null) {
						val channel = NotificationChannel(
								ALARM_CHANNEL_ID,
								context.getString(R.string.alarms),
								NotificationManager.IMPORTANCE_HIGH
						)
						channel.setShowBadge(true)
						channel.enableLights(true)
						channel.enableVibration(true)
						channel.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
						createNotificationChannel(channel)
					}
				}
			}
		}
		
		fun clearSchedule() {
			WorkManager.getInstance().cancelAllWorkByTag(TAG).get()
		}
		
		fun schedule(task: TaskInfo) {
			if (task.task.alarmTime == null) return
			val now = LocalTime()
			if (task.task.alarmTime > now) {
				scheduleAt(task, (task.task.alarmTime.millisOfDay - now.millisOfDay).toLong())
			} else {
				scheduleNow(task)
			}
		}
		
		private fun scheduleNow(task: TaskInfo) {
			scheduleAt(task, 0L)
		}
		
		private fun scheduleAt(task: TaskInfo, delta: Long) {
			val request = OneTimeWorkRequest.Builder(AlarmWorker::class.java)
					.setInitialDelay(delta, TimeUnit.MILLISECONDS)
					.setInputData(Data.Builder()
							.putLong(ARG_WHEN, task.task.alarmTime!!.toDateTimeToday().millis)
							.putLong(ARG_TASK_ID, task.id!!)
							.putString(ARG_TASK_TITLE, task.task.title)
							.build())
					.addTag(TAG)
					.build()
			WorkManager.getInstance().enqueue(request)
		}
		
		fun updatePendingAlarmIcon(context: Context, minAlarmTime: LocalTime?) {
			// TODO
		}
	}
}
