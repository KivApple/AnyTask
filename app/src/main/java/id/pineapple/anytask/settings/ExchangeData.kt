package id.pineapple.anytask.settings

import android.support.annotation.Keep
import id.pineapple.anytask.notes.Note
import id.pineapple.anytask.tasks.Task
import org.joda.time.DateTime

@Keep
data class ExchangeData(
		val magic: String,
		val version: Int,
		val tasks: List<Task>,
		val notes: List<Note>,
		val dateTime: DateTime = DateTime()
) {
	companion object {
		const val MAGIC = "id.pineapple.anytask"
		const val VERSION = 1
	}
}
