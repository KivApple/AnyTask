package id.pineapple.anytask.tasks

import android.arch.persistence.room.TypeConverter
import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.Keep
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import id.pineapple.anytask.daysToLocalDate
import id.pineapple.anytask.localDateToDays
import org.joda.time.*

@Keep
data class TaskScheduleOptions(
		val startDate: LocalDate = daysToLocalDate(0),
		@JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = StopDateFilter::class)
		val stopDate: LocalDate = daysToLocalDate(Int.MAX_VALUE),
		val mode: Mode,
		@JsonInclude(JsonInclude.Include.NON_DEFAULT)
		val interval: Int = 0,
		@JsonProperty("weekDays")
		@JsonInclude(JsonInclude.Include.NON_DEFAULT)
		@JsonSerialize(using = WeekDaysMaskSerializer::class)
		@JsonDeserialize(using = WeekDaysMaskDeserializer::class)
		val weekDaysMask: Int = 0
): Parcelable {
	fun findNextDate(date: LocalDate, now: LocalDate = LocalDate()): LocalDate? {
		val nextDate = when {
			date in startDate..stopDate -> when (mode) {
				Mode.ONCE ->
					if (now >= date)
						minOf(now, stopDate)
					else if (startDate > now && date == startDate)
						startDate
					else
						null
				Mode.DAILY -> {
					val remainder = Days.daysBetween(startDate, date).days % interval
					if (remainder == 0)
						date
					else
						date.plusDays(interval - remainder)
				}
				Mode.WEEKLY -> {
					var minEnabledDayOfWeek = 0
					var maxEnabledDayOfWeek = 0
					for (bitIndex in 0 until 7) {
						if (((1 shl bitIndex) and weekDaysMask) != 0) {
							maxEnabledDayOfWeek = bitIndex + 1
							if (minEnabledDayOfWeek == 0) {
								minEnabledDayOfWeek = maxEnabledDayOfWeek
							}
						}
					}
					if (maxEnabledDayOfWeek == 0) {
						throw IllegalStateException("weekDayMask should define at least one enabled weekday!")
					}
					val remainder = Weeks.weeksBetween(
							startDate,
							date.withDayOfWeek(startDate.dayOfWeek)
					).weeks % interval
					var nextDate = date.plusWeeks(remainder)
					if (remainder == 0) {
						if (nextDate.dayOfWeek > maxEnabledDayOfWeek) {
							nextDate = nextDate.plusWeeks(interval).withDayOfWeek(minEnabledDayOfWeek)
						}
					} else {
						nextDate = nextDate.withDayOfWeek(minEnabledDayOfWeek)
					}
					var dayOfWeek = 0
					for (index in nextDate.dayOfWeek..7) {
						if (((1 shl (index - 1)) and weekDaysMask) != 0) {
							dayOfWeek = index
							break
						}
					}
					nextDate.withDayOfWeek(dayOfWeek)
				}
				Mode.MONTHLY -> {
					val months = Months.monthsBetween(
							startDate.withDayOfMonth(1),
							date.withDayOfMonth(1)
					).months
					val remainder = months % interval
					startDate.plusMonths(
							if (remainder > 0 || date.dayOfMonth > startDate.dayOfMonth)
								months + interval - remainder
							else
								months
					)
				}
				Mode.MONTHLY_INVERSE -> {
					val months = Months.monthsBetween(
							startDate.withDayOfMonth(1),
							date.withDayOfMonth(1)
					).months
					val remainder = months % interval
					val maxDayOfStartMonth = startDate.withDayOfMonth(1).plusMonths(1)
							.minusDays(1).dayOfMonth
					val maxDayOfCurMonth = date.withDayOfMonth(1).plusMonths(1)
							.minusDays(1).dayOfMonth
					startDate.withDayOfMonth(1).plusMonths(
							if (remainder > 0 || (maxDayOfCurMonth - date.dayOfMonth <
											maxDayOfStartMonth - startDate.dayOfMonth))
								months + interval - remainder
							else
								months
					).let {
						val maxDayOfMonth = it.plusMonths(1).minusDays(1).dayOfMonth
						val dayOfMonth = maxDayOfMonth - (maxDayOfStartMonth - startDate.dayOfMonth)
						it.withDayOfMonth(dayOfMonth.coerceAtLeast(1))
					}
				}
				Mode.YEARLY -> {
					val years = Years.yearsBetween(
							startDate.withDayOfYear(1),
							date.withDayOfYear(1)
					).years
					val remainder = years % interval
					startDate.plusYears(
							if (remainder > 0 || date.monthOfYear > startDate.monthOfYear ||
									date.dayOfMonth > startDate.dayOfMonth)
								years + interval - remainder
							else
								years
					)
				}
			}
			date < startDate -> {
				when {
					mode != Mode.ONCE -> startDate
					date <= now -> maxOf(minOf(now, stopDate), startDate)
					else -> minOf(now, stopDate)
				}
			}
			else -> null
		}
		return if (nextDate == null || nextDate <= stopDate)
			nextDate
		else
			null
	}
	
	override fun describeContents(): Int = 0
	
	override fun writeToParcel(parcel: Parcel, flags: Int) {
		parcel.writeInt(localDateToDays(startDate))
		parcel.writeInt(localDateToDays(stopDate))
		parcel.writeInt(mode.ordinal)
		parcel.writeInt(interval)
		parcel.writeInt(weekDaysMask)
	}
	
	@Keep
	enum class Mode {
		ONCE,
		DAILY,
		WEEKLY,
		MONTHLY,
		MONTHLY_INVERSE,
		YEARLY
	}
	
	class ModeConverter {
		@TypeConverter
		fun modeToInt(mode: Mode) = mode.ordinal
		
		@TypeConverter
		fun intToMode(int: Int) = Mode.values()[int]
	}
	
	class ModeSerializer: StdSerializer<Mode>(Mode::class.java) {
		override fun serialize(value: Mode, gen: JsonGenerator, provider: SerializerProvider) {
			gen.writeString(value.toString().toLowerCase())
		}
	}
	
	class ModeDeserializer: StdDeserializer<Mode>(Mode::class.java) {
		override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Mode =
				Mode.valueOf(p.readValueAs(String::class.java).toUpperCase())
	}
	
	@Keep
	class WeekDaysMaskSerializer: StdSerializer<Int>(Int::class.java) {
		override fun serialize(value: Int, gen: JsonGenerator, provider: SerializerProvider) {
			val values = (1 until 7).filter { value and (1 shl (it - 1)) != 0 }.toIntArray()
			gen.writeArray(values, 0, values.size)
		}
	}
	
	@Keep
	class WeekDaysMaskDeserializer: StdDeserializer<Int>(Int::class.java) {
		override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Int =
				p.readValueAs(IntArray::class.java).fold(0) { acc, value ->
					acc or (1 shl (value - 1))
				}
	}
	
	class JacksonModule: SimpleModule() {
		init {
			addSerializer(ModeSerializer())
			addDeserializer(Mode::class.java, ModeDeserializer())
		}
	}
	
	@Keep
	class StopDateFilter {
		override fun equals(other: Any?): Boolean =
				if (other is LocalDate) localDateToDays(other) == Int.MAX_VALUE else false
		override fun hashCode(): Int = 0
	}
	
	companion object CREATOR : Parcelable.Creator<TaskScheduleOptions> {
		override fun createFromParcel(parcel: Parcel): TaskScheduleOptions = TaskScheduleOptions(
				startDate = daysToLocalDate(parcel.readInt()),
				stopDate = daysToLocalDate(parcel.readInt()),
				mode = Mode.values()[parcel.readInt()],
				interval = parcel.readInt(),
				weekDaysMask = parcel.readInt()
		)
		
		override fun newArray(size: Int): Array<TaskScheduleOptions?> = arrayOfNulls(size)
	}
}
