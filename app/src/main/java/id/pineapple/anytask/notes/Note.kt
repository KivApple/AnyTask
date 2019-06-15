package id.pineapple.anytask.notes

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.Keep
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import id.pineapple.anytask.R
import id.pineapple.recyclerviewutil.UniqueEntity
import id.pineapple.anytask.tasks.Task
import id.pineapple.anytask.tasks.TaskInfo
import org.joda.time.DateTime

@Entity(
		tableName = "notes",
		indices = [
			Index("parentId", "position")
		],
		foreignKeys = [
			ForeignKey(
					entity = Note::class,
					parentColumns = ["id"],
					childColumns = ["parentId"],
					onDelete = ForeignKey.CASCADE
			)
		]
)
@Keep
data class Note(
		@PrimaryKey(autoGenerate = true) override val id: Long? = null,
		@JsonInclude(JsonInclude.Include.NON_NULL)
		val parentId: Long? = null,
		@JsonInclude(JsonInclude.Include.NON_EMPTY)
		val title: String = "",
		val type: Type,
		@JsonInclude(JsonInclude.Include.NON_EMPTY)
		val text: String = "",
		val position: Int,
		val titleUpdateTime: DateTime = DateTime(),
		val contentUpdateTime: DateTime = DateTime(),
		val positionUpdateTime: DateTime = DateTime()
) : UniqueEntity {
	@JsonIgnore
	fun getNormalizedTitle(context: Context): String =
			when {
				title.isNotBlank() -> title
				type == Type.TEXT && text.isNotBlank() -> text
				type == Type.LIST -> context.getString(R.string.list)
				type == Type.FOLDER -> context.getString(R.string.folder)
				else -> context.getString(R.string.note)
			}
	
	fun updateTitle(title: String) =
			copy(
					title = title,
					titleUpdateTime = DateTime()
			)
	
	private fun updateText(type: Type, text: String) =
			copy(
					type = type,
					text = text,
					contentUpdateTime = DateTime()
			)
	
	fun updateText(text: String) = updateText(Type.TEXT, text)
	
	fun updateList(tasks: Collection<TaskInfo>) = updateText(Type.LIST, tasks.joinToString("\n") {
		"${if (it.task.completed) "+" else "-"}${it.task.title}"
	})
	
	@JsonIgnore
	fun getList(): List<TaskInfo> = when (type) {
		Type.TEXT -> text.split('\n').filter { it.isNotBlank() }.mapIndexed { index, s ->
			Task(
					id = index.toLong() + 1,
					title = s,
					position = index + 1
			)
		}
		Type.LIST -> text.split('\n').filter { it.isNotBlank() }.mapIndexed { index, s ->
			Task(
					id = index.toLong() + 1,
					title = s.substring(1),
					completed = s[0] == '+',
					position = index + 1
			)
		}
		Type.FOLDER -> throw IllegalStateException()
	}.map { TaskInfo(it) }
	
	fun updatePosition(position: Int) =
			copy(
					position = position,
					positionUpdateTime = DateTime()
			)
	
	fun updateParentId(folderId: Long?) =
			copy(
					parentId = folderId,
					position = 0,
					positionUpdateTime = DateTime()
			)
	
	override fun writeToParcel(parcel: Parcel, flags: Int) {
		parcel.writeLong(id ?: 0L)
		parcel.writeLong(parentId ?: 0L)
		parcel.writeString(title)
		parcel.writeInt(type.ordinal)
		parcel.writeString(text)
		parcel.writeInt(position)
		parcel.writeLong(titleUpdateTime.millis)
		parcel.writeLong(contentUpdateTime.millis)
		parcel.writeLong(positionUpdateTime.millis)
	}
	
	override fun describeContents(): Int = 0
	
	@Keep
	enum class Type {
		TEXT,
		LIST,
		FOLDER
	}
	
	class TypeConverter {
		@android.arch.persistence.room.TypeConverter
		fun typeToInt(type: Type): Int = type.ordinal
		
		@android.arch.persistence.room.TypeConverter
		fun intToType(int: Int): Type = Type.values()[int]
	}
	
	class TypeSerializer: StdSerializer<Type>(Type::class.java) {
		override fun serialize(value: Type, gen: JsonGenerator, provider: SerializerProvider) {
			gen.writeString(value.toString().toLowerCase())
		}
	}
	
	class TypeDeserializer: StdDeserializer<Type>(Type::class.java) {
		override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Type =
				Type.valueOf(p.readValueAs(String::class.java).toUpperCase())
	}
	
	class JacksonModule: SimpleModule() {
		init {
			addSerializer(TypeSerializer())
			addDeserializer(Type::class.java, TypeDeserializer())
		}
	}
	
	companion object CREATOR : Parcelable.Creator<Note> {
		override fun createFromParcel(parcel: Parcel): Note =
				Note(
						id = parcel.readLong().let { if (it > 0) it else null },
						parentId = parcel.readLong().let { if (it > 0) it else null },
						title = parcel.readString()!!,
						type = Type.values()[parcel.readInt()],
						text = parcel.readString()!!,
						position = parcel.readInt(),
						titleUpdateTime = DateTime(parcel.readLong()),
						contentUpdateTime = DateTime(parcel.readLong()),
						positionUpdateTime = DateTime(parcel.readLong())
				)
		
		override fun newArray(size: Int): Array<Note?> = arrayOfNulls(size)
	}
}
