package id.pineapple.anytask

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.PopupWindow
import android.widget.TextView
import java.util.*

const val IMPORTANT_NOTIFICATIONS_CHANNEL = "important_notifications"

fun Context.applyLocaleFromPreferences(sharedPreferences: SharedPreferences) {
	val localeCode = sharedPreferences.getString("language", "")!!
	val l = if (localeCode.isEmpty()) App.systemLocale else Locale(localeCode)
	if (l != Locale.getDefault()) {
		Locale.setDefault(l)
	}
	if (
			l != if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
				resources.configuration.locales[0]
			else
				resources.configuration.locale
	) {
		resources.updateConfiguration(resources.configuration.apply {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
				setLocale(l)
			} else {
				locale = l
			}
		}, resources.displayMetrics)
	}
}

fun BottomSheetDialogFragment.fixLandscapeHeight() {
	view?.viewTreeObserver?.addOnGlobalLayoutListener(
			object : ViewTreeObserver.OnGlobalLayoutListener {
				override fun onGlobalLayout() {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
						view?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
					}
					val bottomSheet = dialog?.findViewById<FrameLayout>(
							android.support.design.R.id.design_bottom_sheet
					)
					if (bottomSheet != null) {
						val behavior = BottomSheetBehavior.from(bottomSheet)
						behavior.state = BottomSheetBehavior.STATE_EXPANDED
						behavior.peekHeight = 0
					}
				}
			}
	)
}

fun setTextViewCompoundDrawables(
		textView: TextView, start: Int, top: Int, end: Int, bottom: Int
) {
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
		textView.setCompoundDrawablesRelativeWithIntrinsicBounds(start, top, end, bottom)
	} else {
		val drawables = listOf(start, top, end, bottom).map {
			if (it != 0) ContextCompat.getDrawable(textView.context, it) else null
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			textView.setCompoundDrawablesRelativeWithIntrinsicBounds(
					drawables[0], drawables[1], drawables[2], drawables[3]
			)
		} else {
			textView.setCompoundDrawablesWithIntrinsicBounds(
					drawables[0], drawables[1], drawables[2], drawables[3]
			)
		}
	}
}

fun drawableResourceToBitmap(context: Context, resId: Int): Bitmap {
	var drawable = ContextCompat.getDrawable(context, resId)!!
	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
		drawable = DrawableCompat.wrap(drawable).mutate()
	}
	val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight,
			Bitmap.Config.ARGB_8888)
	val canvas = Canvas(bitmap)
	drawable.setBounds(0, 0, canvas.width, canvas.height)
	drawable.draw(canvas)
	return bitmap
}

tailrec fun getSupportFragmentManager(context: Context): FragmentManager? = when (context) {
	is FragmentActivity -> context.supportFragmentManager
	is ContextWrapper -> getSupportFragmentManager(context.baseContext)
	else -> null
}

fun PopupWindow.dimBehind(dimAmount: Float = 0.3f) {
	val container = contentView.rootView
	val context = contentView.context
	val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
	val p = container.layoutParams as WindowManager.LayoutParams
	p.flags = p.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
	p.dimAmount = dimAmount
	wm.updateViewLayout(container, p)
}

fun setupNotifications(context: Context) {
	if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
		(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).apply {
			if (getNotificationChannel(IMPORTANT_NOTIFICATIONS_CHANNEL) == null) {
				val channel = NotificationChannel(
						IMPORTANT_NOTIFICATIONS_CHANNEL,
						context.getString(R.string.important_notifications),
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
