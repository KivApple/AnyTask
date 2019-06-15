package id.pineapple.anytask

import android.os.Build
import android.support.multidex.MultiDexApplication
import android.support.v7.app.AppCompatDelegate
import android.util.Log
import net.danlew.android.joda.JodaTimeAndroid
import org.joda.time.DateTime
import java.io.File
import java.util.*

class App : MultiDexApplication() {
	override fun onCreate() {
		super.onCreate()
		systemLocale = Locale.getDefault()
		AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
		JodaTimeAndroid.init(this)
		appFilesDir = filesDir
		appVersionCode = packageManager.getPackageInfo(packageName, 0).versionCode.toLong()
		val prevDefaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
		Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
			if (appFilesDir != null) {
				File(appFilesDir, LAST_EXCEPTION_LOG_NAME).printWriter().use {
					it.println("[date=${DateTime()} thread=${thread.name} " +
							"sdkVer=${Build.VERSION.SDK_INT} appVer=$appVersionCode]")
					exception.printStackTrace(it)
				}
			}
			if (prevDefaultUncaughtExceptionHandler != null) {
				prevDefaultUncaughtExceptionHandler.uncaughtException(thread, exception)
			} else {
				Log.e(App::class.java.name, "Uncaught exception", exception)
				System.exit(-1)
			}
		}
		AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
		AppDatabase.init(this)
	}
	
	companion object {
		const val LAST_EXCEPTION_LOG_NAME = "last_exception.log"
		var appFilesDir: File? = null
			private set
		private var appVersionCode = -1L
		lateinit var systemLocale: Locale
			private set
	}
}
