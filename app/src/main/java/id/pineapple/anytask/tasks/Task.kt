package id.pineapple.anytask.tasks

import android.arch.persistence.room.*
import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.Keep
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import id.pineapple.anytask.daysToLocalDate
import id.pineapple.anytask.localDateToDays
import id.pineapple.anytask.notes.Note
import id.pineapple.recyclerviewutil.UniqueEntity
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.joda.time.LocalTime

@Entity(
		tableName = "tasks",
		indices = [
			Index("schedule_startDate", "schedule_stopDate"),
			Index("originDate", "scheduleTemplateId"),
			Index("scheduleTemplateId", "schedule_startDate"),
			Index("noteId")
		],
		foreignKeys = [
			ForeignKey(
					entity = Task::class,
					parentColumns = ["id"],
					childColumns = ["scheduleTemplateId"],
					onDelete = ForeignKey.CASCADE
			),
			ForeignKey(
					entity = Note::class,
					parentColumns = ["id"],
					childColumns = ["noteId"],
					onDelete = ForeignKey.SET_NULL
			)
		]
)
@Keep
data class Task(
		@PrimaryKey(autoGenerate = true) override val id: Long? = null,
		@JsonInclude(JsonInclude.Include.NON_EMPTY)
		val title: String = "",
		@JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = TrueValueFilter::class)
		val titleFromTemplate: Boolean = true,
		@JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = FalseValueFilter::class)
		val completed: Boolean = false,
		val position: Int,
		@JsonInclude(JsonInclude.Include.NON_DEFAULT)
		val savedPosition: Int = 0,
		@JsonInclude(JsonInclude.Include.NON_DEFAULT)
		val priority: Int = 0,
		@JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = TrueValueFilter::class)
		val priorityFromTemplate: Boolean = true,
		@Embedded(prefix = "schedule_")
		val scheduleOptions: TaskScheduleOptions = TaskScheduleOptions(
				mode = TaskScheduleOptions.Mode.ONCE
		),
		@JsonInclude(JsonInclude.Include.NON_NULL)
		val scheduleTemplateId: Long? = null,
		val originDate: LocalDate = LocalDate(),
		@JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = FalseValueFilter::class)
		val pinned: Boolean = false,
		@JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = TrueValueFilter::class)
		val pinnedFromTemplate: Boolean = true,
		@JsonInclude(JsonInclude.Include.NON_NULL)
		val alarmTime: LocalTime? = null,
		@JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = TrueValueFilter::class)
		val alarmTimeFromTemplate: Boolean = true,
		@JsonInclude(JsonInclude.Include.NON_NULL)
		val noteId: Long? = null,
		@JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = TrueValueFilter::class)
		val noteIdFromTemplate: Boolean = true,
		
		val titleUpdateTime: DateTime = DateTime(),
		val completedUpdateTime: DateTime = DateTime(),
		val positionUpdateTime: DateTime = DateTime(),
		val scheduleOptionsUpdateTime: DateTime = DateTime(),
		val pinnedUpdateTime: DateTime = DateTime(),
		val alarmTimeUpdateTime: DateTime = DateTime(),
		val noteIdUpdateTime: DateTime = DateTime()
) : UniqueEntity {
	val isScheduled: Boolean
		@JsonIgnore
		get() = scheduleOptions.mode != TaskScheduleOptions.Mode.ONCE || scheduleTemplateId != null
	
	fun updateTitle(title: String, sameAsTemplate: Boolean = true): Task =
			copy(title = title, titleFromTemplate = sameAsTemplate, titleUpdateTime = DateTime())
	
	fun updateCompleted(completed: Boolean, newPosition: Int): Task =
			copy(
					completed = completed,
					position = newPosition,
					savedPosition = if (completed && !this.completed) position else 0,
					completedUpdateTime = DateTime(),
					positionUpdateTime = DateTime()
			)
	
	fun updatePosition(position: Int): Task =
			copy(position = position, positionUpdateTime = DateTime())
	
	fun updatePriority(priority: Int, newPosition: Int, sameAsTemplate: Boolean = true): Task =
			copy(
					priority = priority,
					priorityFromTemplate = sameAsTemplate,
					position = newPosition,
					positionUpdateTime = DateTime()
			)
	
	fun updateScheduleOptions(scheduleOptions: TaskScheduleOptions): Task =
			copy(
					scheduleOptions = scheduleOptions,
					scheduleOptionsUpdateTime = DateTime()
			)
	
	fun cloneScheduled(date: LocalDate): Task =
			copy(
					id = null,
					title = if (titleFromTemplate) "" else title,
					originDate = date,
					scheduleOptions = scheduleOptions.copy(
							mode = TaskScheduleOptions.Mode.ONCE,
							interval = 0,
							weekDaysMask = 0
					),
					scheduleTemplateId = id,
					scheduleOptionsUpdateTime = DateTime()
			)
	
	fun updatePinned(pinned: Boolean, sameAsTemplate: Boolean = true): Task =
			copy(
					pinned = pinned,
					pinnedFromTemplate = sameAsTemplate,
					pinnedUpdateTime = DateTime()
			)
	
	fun updateAlarmTime(alarmTime: LocalTime?, sameAsTemplate: Boolean = true): Task =
			copy(
					alarmTime = alarmTime,
					alarmTimeFromTemplate = sameAsTemplate,
					alarmTimeUpdateTime = DateTime()
			)
	
	fun updateNoteId(noteId: Long?, sameAsTemplate: Boolean = true): Task =
			copy(
					noteId = noteId,
					noteIdFromTemplate = sameAsTemplate,
					noteIdUpdateTime = DateTime()
			)
	
	override fun describeContents(): Int = 0
	
	override fun writeToParcel(dest: Parcel, flags: Int) {
		dest.writeLong(id ?: 0)
		dest.writeString(title)
		dest.writeInt(if (completed) 1 else 0)
		dest.writeInt(position)
		dest.writeInt(savedPosition)
		dest.writeInt(priority)
		dest.writeInt(if (priorityFromTemplate) 1 else 0)
		dest.writeParcelable(scheduleOptions, 0)
		dest.writeLong(scheduleTemplateId ?: 0)
		dest.writeInt(localDateToDays(originDate))
		dest.writeInt(if (pinned) 1 else 0)
		dest.writeInt(if (pinnedFromTemplate) 1 else 0)
		dest.writeInt(alarmTime?.millisOfDay ?: -1)
		dest.writeInt(if (alarmTimeFromTemplate) 1 else 0)
		dest.writeLong(titleUpdateTime.millis)
		dest.writeLong(completedUpdateTime.millis)
		dest.writeLong(positionUpdateTime.millis)
		dest.writeLong(scheduleOptionsUpdateTime.millis)
		dest.writeLong(pinnedUpdateTime.millis)
		dest.writeLong(alarmTimeUpdateTime.millis)
	}
	
	@Keep
	class TrueValueFilter {
		override fun equals(other: Any?): Boolean =
				other as? Boolean == true
		override fun hashCode(): Int = 0
	}
	
	@Keep
	class FalseValueFilter {
		override fun equals(other: Any?): Boolean =
				other as? Boolean == false
		override fun hashCode(): Int = 0
	}
	
	companion object CREATOR : Parcelable.Creator<Task> {
		const val AUTO_POSITION_OFFSET = 1073741824
		
		override fun createFromParcel(source: Parcel): Task =
				Task(
						id = source.readLong().let { if (it > 0) it else null },
						title = source.readString()!!,
						completed = source.readInt() != 0,
						position = source.readInt(),
						savedPosition = source.readInt(),
						priority = source.readInt(),
						priorityFromTemplate = source.readInt() != 0,
						scheduleOptions = source.readParcelable(this::class.java.classLoader)!!,
						scheduleTemplateId = source.readLong().let { if (it > 0) it else null },
						originDate = daysToLocalDate(source.readInt()),
						pinned = source.readInt() != 0,
						pinnedFromTemplate = source.readInt() != 0,
						alarmTime = source.readInt().let {
							if (it >= 0) LocalTime.fromMillisOfDay(it.toLong()) else null
						},
						alarmTimeFromTemplate = source.readInt() != 0,
						titleUpdateTime = DateTime(source.readLong()),
						completedUpdateTime = DateTime(source.readLong()),
						positionUpdateTime = DateTime(source.readLong()),
						scheduleOptionsUpdateTime = DateTime(source.readLong()),
						pinnedUpdateTime = DateTime(source.readLong()),
						alarmTimeUpdateTime = DateTime(source.readLong())
				)
		
		override fun newArray(size: Int): Array<Task?> = arrayOfNulls(size)
	}
}
