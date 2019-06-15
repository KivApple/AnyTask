package id.pineapple.anytask

import android.content.Context
import android.util.TypedValue

fun resolveColor(context: Context, resId: Int): Int {
	val value = TypedValue()
	context.theme.resolveAttribute(resId, value, true)
	return value.data
}
