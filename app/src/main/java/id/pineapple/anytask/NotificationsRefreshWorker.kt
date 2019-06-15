package id.pineapple.anytask

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.app.TaskStackBuilder
import androidx.work.*
import id.pineapple.anytask.tasks.TaskListPersistentModel
import org.joda.time.DateTime
import org.joda.time.Duration
import java.util.concurrent.TimeUnit

class NotificationsRefreshWorker(context: Context, params: WorkerParameters) :
		Worker(context, params) {
	override fun doWork(): Result {
		val now = DateTime()
		lastExecuteDateTime = now
		val localTime = now.toLocalTime().withSecondOfMinute(0).withMillisOfSecond(0)
		val model = TaskListPersistentModel(now.toLocalDate(), reactive = false)
		val tasks = model.fetchTasksSync()
		val pinnedTaskTitles = tasks.filter {
			it.task.pinned && !it.task.completed
		}.map { it.task.title }
		val alarmTasks = tasks.filter {
			it.task.alarmTime != null && !it.task.completed && it.task.alarmTime >= localTime
		}
		val context = applicationContext
		val notificationManager = NotificationManagerCompat.from(context)
		if (pinnedTaskTitles.isNotEmpty()) {
			val stackBuilder = TaskStackBuilder.create(context)
			stackBuilder.addParentStack(MainActivity::class.java)
			stackBuilder.addNextIntent(Intent(context, MainActivity::class.java))
			val pendingIntent = stackBuilder.getPendingIntent(
					PENDING_INTENT_ID, PendingIntent.FLAG_UPDATE_CURRENT
			)
			val inboxStyle = NotificationCompat.InboxStyle()
			pinnedTaskTitles.forEach { inboxStyle.addLine(it) }
			val notificationBuilder = NotificationCompat.Builder(context, PINNED_TASKS_CHANNEL_ID)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				notificationBuilder.setSmallIcon(R.drawable.ic_check_24dp)
			} else {
				notificationBuilder.setSmallIcon(R.mipmap.ic_launcher)
			}
			notificationBuilder.priority = NotificationCompat.PRIORITY_DEFAULT
			notificationBuilder.setCategory(NotificationCompat.CATEGORY_EVENT)
			notificationBuilder.setContentTitle(context.getString(R.string.pinned_tasks))
			notificationBuilder.setContentText(pinnedTaskTitles.joinToString(", "))
			notificationBuilder.setStyle(inboxStyle)
			notificationBuilder.setContentIntent(pendingIntent)
			notificationBuilder.setShowWhen(false)
			notificationBuilder.setOngoing(true)
			val notification = notificationBuilder.build()
			notificationManager.notify(PINNED_TASKS_NOTIFICATION_ID, notification)
		} else {
			notificationManager.cancel(PINNED_TASKS_NOTIFICATION_ID)
		}
		AlarmWorker.clearSchedule()
		alarmTasks.forEach { AlarmWorker.schedule(it) }
		AlarmWorker.updatePendingAlarmIcon(context, alarmTasks.minBy { it.task.alarmTime!! }?.task?.alarmTime)
		schedule()
		return Result.SUCCESS
	}
	
	class DateChangedReceiver : BroadcastReceiver() {
		@SuppressLint("UnsafeProtectedBroadcastReceiver")
		override fun onReceive(context: Context, intent: Intent) {
			NotificationsRefreshWorker.schedule()
		}
	}
	
	companion object {
		private const val UNIQUE_WORK_NAME = "NotificationsRefreshWorker"
		private const val PINNED_TASKS_CHANNEL_ID = "pinned_tasks"
		private const val PINNED_TASKS_NOTIFICATION_ID = 1
		private const val PENDING_INTENT_ID = 1
		private const val SCHEDULE_NOW_DELAY = 100L
		@Volatile
		private var lastExecuteDateTime: DateTime? = null
		
		fun setupNotifications(context: Context) {
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
				(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).apply {
					if (getNotificationChannel(PINNED_TASKS_CHANNEL_ID) == null) {
						val channel = NotificationChannel(
								PINNED_TASKS_CHANNEL_ID,
								context.getString(R.string.pinned_tasks),
								NotificationManager.IMPORTANCE_LOW
						)
						channel.enableLights(false)
						channel.enableVibration(false)
						channel.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
						createNotificationChannel(channel)
					}
				}
			}
		}
		
		fun schedule() {
			val now = DateTime()
			if (now.toLocalDate() == lastExecuteDateTime?.toLocalDate()) {
				scheduleAt(Duration(now, now.withMillisOfDay(0).plusDays(1)).millis)
			} else {
				scheduleNow()
			}
		}
		
		fun scheduleNow() {
			scheduleAt(SCHEDULE_NOW_DELAY)
		}
		
		private fun scheduleAt(delta: Long) {
			val request = OneTimeWorkRequest.Builder(NotificationsRefreshWorker::class.java)
					.setInitialDelay(delta, TimeUnit.MILLISECONDS)
					.build()
			WorkManager.getInstance()
					.beginUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
					.enqueue()
		}
	}
}
