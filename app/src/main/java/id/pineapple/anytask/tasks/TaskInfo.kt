package id.pineapple.anytask.tasks

import android.arch.persistence.room.Embedded
import android.os.Parcel
import android.os.Parcelable
import id.pineapple.anytask.notes.Note
import id.pineapple.recyclerviewutil.UniqueEntity

data class TaskInfo(
		@Embedded
		val task: Task,
		@Embedded(prefix = "template_")
		val template: Task? = null,
		@Embedded(prefix = "note_")
		val note: Note? = null
): UniqueEntity {
	override val id: Long? get() = task.id
	
	override fun describeContents(): Int = 0
	
	override fun writeToParcel(dest: Parcel, flags: Int) {
		dest.writeParcelable(task, 0)
		dest.writeParcelable(template, 0)
		dest.writeParcelable(note, 0)
	}
	
	companion object CREATOR : Parcelable.Creator<TaskInfo> {
		override fun createFromParcel(source: Parcel): TaskInfo =
				TaskInfo(
						source.readParcelable(Task::class.java.classLoader)!!,
						source.readParcelable(Task::class.java.classLoader),
						source.readParcelable(Note::class.java.classLoader)
				)
		
		override fun newArray(size: Int): Array<TaskInfo?> = arrayOfNulls(size)
	}
}
