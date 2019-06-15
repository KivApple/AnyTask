package id.pineapple.anytask.settings

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.app.TaskStackBuilder
import android.util.Log
import androidx.work.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import id.pineapple.anytask.*
import id.pineapple.anytask.R
import id.pineapple.anytask.notes.Note
import id.pineapple.anytask.tasks.TaskScheduleOptions
import java.io.File
import java.util.*

class DataImportWorker(context: Context, workerParams: WorkerParameters):
		Worker(context, workerParams) {
	override fun doWork(): Result {
		val file = File(inputData.getString("inputFile"))
		val context = applicationContext
		val notificationManager = NotificationManagerCompat.from(context)
		NotificationCompat.Builder(context, IMPORTANT_NOTIFICATIONS_CHANNEL).let {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
				it.setSmallIcon(R.drawable.ic_file_download_black_24dp)
			else
				it.setSmallIcon(R.mipmap.ic_launcher)
			it.priority = NotificationCompat.PRIORITY_HIGH
			it.setCategory(NotificationCompat.CATEGORY_PROGRESS)
			it.setOngoing(true)
			it.setProgress(0, 0, true)
			it.setContentTitle(context.getString(R.string.data_import))
			it.setContentText(file.absolutePath)
			notificationManager.notify(NOTIFICATION_ID, it.build())
		}
		return try {
			doImport(file)
			NotificationCompat.Builder(context, IMPORTANT_NOTIFICATIONS_CHANNEL).let {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
					it.setSmallIcon(R.drawable.ic_file_download_black_24dp)
				else
					it.setSmallIcon(R.mipmap.ic_launcher)
				it.priority = NotificationCompat.PRIORITY_HIGH
				it.setCategory(NotificationCompat.CATEGORY_PROGRESS)
				it.setContentTitle(context.getString(R.string.data_import_finished))
				it.setContentText(file.absolutePath)
				it.setContentIntent(
						TaskStackBuilder.create(context).apply {
							addParentStack(MainActivity::class.java)
							addNextIntent(Intent(context, MainActivity::class.java))
						}.getPendingIntent(PENDING_INTENT, PendingIntent.FLAG_UPDATE_CURRENT)
				)
				it.setDefaults(Notification.DEFAULT_VIBRATE)
				notificationManager.notify(NOTIFICATION_ID, it.build())
			}
			Result.SUCCESS
		} catch (e: Throwable) {
			Log.e("DataImportWorker", "Import failed", e)
			NotificationCompat.Builder(context, IMPORTANT_NOTIFICATIONS_CHANNEL).let {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
					it.setSmallIcon(R.drawable.ic_file_download_black_24dp)
				else
					it.setSmallIcon(R.mipmap.ic_launcher)
				it.priority = NotificationCompat.PRIORITY_HIGH
				it.setCategory(NotificationCompat.CATEGORY_ERROR)
				it.setContentTitle(context.getString(R.string.data_import_failed))
				it.setContentText(e.toString())
				it.setDefaults(Notification.DEFAULT_VIBRATE)
				notificationManager.notify(NOTIFICATION_ID, it.build())
			}
			Result.FAILURE
		}
	}
	
	private fun doImport(file: File) {
		val objectMapper = jacksonObjectMapper()
				.registerModule(DateTimeModule())
				.registerModule(TaskScheduleOptions.JacksonModule())
				.registerModule(Note.JacksonModule())
		val data = objectMapper.readValue<ExchangeData>(file, ExchangeData::class.java)
		if (data.magic != ExchangeData.MAGIC) {
			throw IllegalStateException("Invalid data file magic")
		}
		if (data.version > ExchangeData.VERSION) {
			throw IllegalStateException("Data file created in the newer version of application")
		}
		AppDatabase.instance.beginTransaction()
		val taskDao = AppDatabase.instance.taskDao()
		val noteDao = AppDatabase.instance.noteDao()
		taskDao.deleteAll()
		noteDao.deleteAll()
		AppDatabase.instance.query("PRAGMA defer_foreign_keys=1", arrayOf()).use {
			it.moveToFirst()
		}
		taskDao.insert(*data.tasks.toTypedArray())
		noteDao.insert(*data.notes.toTypedArray())
		AppDatabase.instance.setTransactionSuccessful()
		AppDatabase.instance.endTransaction()
	}
	
	companion object {
		private const val NOTIFICATION_ID = 2
		private const val PENDING_INTENT = 5
		
		fun schedule(inputFile: File): UUID {
			val request = OneTimeWorkRequest.Builder(DataImportWorker::class.java)
					.setInputData(
							Data.Builder()
									.putString("inputFile", inputFile.absolutePath)
									.build()
					)
					.build()
			WorkManager.getInstance().enqueue(request)
			return request.id
		}
	}
}
