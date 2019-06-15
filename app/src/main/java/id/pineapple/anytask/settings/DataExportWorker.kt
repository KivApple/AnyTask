package id.pineapple.anytask.settings

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.FileProvider
import android.util.Log
import androidx.work.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import id.pineapple.anytask.AppDatabase
import id.pineapple.anytask.DateTimeModule
import id.pineapple.anytask.IMPORTANT_NOTIFICATIONS_CHANNEL
import id.pineapple.anytask.R
import id.pineapple.anytask.notes.Note
import id.pineapple.anytask.tasks.TaskScheduleOptions
import java.io.File
import java.util.*

class DataExportWorker(context: Context, workerParams: WorkerParameters):
		Worker(context, workerParams) {
	override fun doWork(): Result {
		val file = File(inputData.getString("outputFile"))
		val context = applicationContext
		val notificationManager = NotificationManagerCompat.from(context)
		NotificationCompat.Builder(context, IMPORTANT_NOTIFICATIONS_CHANNEL).let {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
				it.setSmallIcon(R.drawable.ic_file_upload_black_24dp)
			else
				it.setSmallIcon(R.mipmap.ic_launcher)
			it.priority = NotificationCompat.PRIORITY_HIGH
			it.setCategory(NotificationCompat.CATEGORY_PROGRESS)
			it.setOngoing(true)
			it.setProgress(0, 0, true)
			it.setContentTitle(context.getString(R.string.data_export))
			it.setContentText(file.absolutePath)
			notificationManager.notify(NOTIFICATION_ID, it.build())
		}
		return try {
			doExport(file)
			NotificationCompat.Builder(context, IMPORTANT_NOTIFICATIONS_CHANNEL).let {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
					it.setSmallIcon(R.drawable.ic_file_upload_black_24dp)
				else
					it.setSmallIcon(R.mipmap.ic_launcher)
				it.priority = NotificationCompat.PRIORITY_HIGH
				it.setCategory(NotificationCompat.CATEGORY_PROGRESS)
				it.setContentTitle(context.getString(R.string.data_export_finished))
				it.setContentText(file.absolutePath)
				val fileProviderAuthority =
						"${context.applicationContext.packageName}.GenericFileProvider"
				it.setContentIntent(
						PendingIntent.getActivity(context, VIEW_PENDING_INTENT,
								Intent(Intent.ACTION_VIEW).apply {
									setDataAndType(FileProvider.getUriForFile(context,
											fileProviderAuthority, file
									), "text/*")
									addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
											Intent.FLAG_GRANT_READ_URI_PERMISSION)
								}, PendingIntent.FLAG_CANCEL_CURRENT)
				)
				it.addAction(0, context.getString(R.string.share), PendingIntent.getActivity(
						context, SHARE_PENDING_INTENT,
						Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
							type = "text/*"
							putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context,
									fileProviderAuthority, file))
							addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
									Intent.FLAG_GRANT_READ_URI_PERMISSION)
						}, context.getString(R.string.share)), PendingIntent.FLAG_CANCEL_CURRENT
				))
				it.setDefaults(Notification.DEFAULT_VIBRATE)
				notificationManager.notify(NOTIFICATION_ID, it.build())
			}
			Result.SUCCESS
		} catch (e: Throwable) {
			Log.e("DataExportWorker", "Export failed", e)
			NotificationCompat.Builder(context, IMPORTANT_NOTIFICATIONS_CHANNEL).let {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
					it.setSmallIcon(R.drawable.ic_file_upload_black_24dp)
				else
					it.setSmallIcon(R.mipmap.ic_launcher)
				it.priority = NotificationCompat.PRIORITY_HIGH
				it.setCategory(NotificationCompat.CATEGORY_ERROR)
				it.setContentTitle(context.getString(R.string.data_export_failed))
				it.setContentText(e.toString())
				it.setDefaults(Notification.DEFAULT_VIBRATE)
				notificationManager.notify(NOTIFICATION_ID, it.build())
			}
			Result.FAILURE
		}
	}
	
	private fun doExport(file: File) {
		val taskDao = AppDatabase.instance.taskDao()
		val noteDao = AppDatabase.instance.noteDao()
		val tasks = taskDao.findAllSync()
		val notes = noteDao.findAllSync()
		val data = ExchangeData(ExchangeData.MAGIC, ExchangeData.VERSION, tasks, notes)
		val objectMapper = jacksonObjectMapper()
				.registerModule(DateTimeModule())
				.registerModule(TaskScheduleOptions.JacksonModule())
				.registerModule(Note.JacksonModule())
		objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, data)
	}
	
	companion object {
		private const val NOTIFICATION_ID = 2
		private const val VIEW_PENDING_INTENT = 3
		private const val SHARE_PENDING_INTENT = 4
		
		fun schedule(outputFile: File): UUID {
			val request = OneTimeWorkRequest.Builder(DataExportWorker::class.java)
					.setInputData(
							Data.Builder()
									.putString("outputFile", outputFile.absolutePath)
									.build()
					)
					.build()
			WorkManager.getInstance().enqueue(request)
			return request.id
		}
	}
}
