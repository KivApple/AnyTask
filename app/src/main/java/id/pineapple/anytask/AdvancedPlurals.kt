package id.pineapple.anytask

import android.content.res.Resources

fun Resources.getAdvancedQuantityString(id: Int, count: Int, vararg args: Any?): String {
	getStringArray(id).forEach {
		val parts = it.split(' ', limit = 2)
		if (parts.size == 2) {
			when (parts[0][0]) {
				'=' -> if (count == parts[0].drop(1).toInt()) {
					return parts[1].format(*args)
				}
				'%' -> if (count % 10 == parts[0].drop(1).toInt()) {
					return parts[1].format(*args)
				}
				'*' -> return parts[1].format(*args)
				else -> throw RuntimeException("Invalid quantity string specification: $it")
			}
		} else {
			throw RuntimeException("Invalid quantity string specification: $it")
		}
	}
	throw RuntimeException("Cannot find matching quantity string for count $count")
}
