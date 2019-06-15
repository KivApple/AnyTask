package id.pineapple.anytask

import android.arch.persistence.room.TypeConverter
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.joda.time.*

private val EPOCH_START = LocalDate(0)

fun localDateToDays(localDate: LocalDate) = Days.daysBetween(EPOCH_START, localDate).days

fun daysToLocalDate(days: Int): LocalDate = EPOCH_START.plusDays(days)

fun localDateToMonths(localDate: LocalDate) = Months.monthsBetween(EPOCH_START, localDate).months

fun monthsToLocalDate(months: Int): LocalDate = EPOCH_START.plusMonths(months)

class DateConverter {
	@TypeConverter
	fun dateTimeToLong(dateTime: DateTime?): Long? = dateTime?.millis
	
	@TypeConverter
	fun longToDateTime(long: Long?): DateTime? =
			if (long != null)
				DateTime(long)
			else
				null
	
	@TypeConverter
	fun localDateToInt(localDate: LocalDate?): Int? =
			if (localDate != null)
				localDateToDays(localDate)
			else
				null
	
	@TypeConverter
	fun intToLocalDate(int: Int?): LocalDate? =
			if (int != null)
				daysToLocalDate(int)
			else
				null
	
	@TypeConverter
	fun localTimeToInt(localTime: LocalTime?): Int? =
			localTime?.millisOfDay
	
	@TypeConverter
	fun intToLocalTime(int: Int?): LocalTime? =
			if (int != null)
				LocalTime.fromMillisOfDay(int.toLong())
			else
				null
}

class DateTimeSerializer: StdSerializer<DateTime>(DateTime::class.java) {
	override fun serialize(value: DateTime, gen: JsonGenerator, provider: SerializerProvider) {
		gen.writeString(value.toString())
	}
}

class DateTimeDeserializer: StdDeserializer<DateTime>(DateTime::class.java) {
	override fun deserialize(p: JsonParser, ctxt: DeserializationContext): DateTime =
			DateTime.parse(p.readValueAs(String::class.java))
}

class LocalDateSerializer: StdSerializer<LocalDate>(LocalDate::class.java) {
	override fun serialize(value: LocalDate, gen: JsonGenerator, provider: SerializerProvider) {
		gen.writeString(value.toString())
	}
}

class LocalDateDeserializer: StdDeserializer<LocalDate>(LocalDate::class.java) {
	override fun deserialize(p: JsonParser, ctxt: DeserializationContext): LocalDate =
			LocalDate.parse(p.readValueAs(String::class.java))
}

class LocalTimeSerializer: StdSerializer<LocalTime>(LocalTime::class.java) {
	override fun serialize(value: LocalTime, gen: JsonGenerator, provider: SerializerProvider) {
		gen.writeString(value.toString())
	}
}

class LocalTimeDeserializer: StdDeserializer<LocalTime>(LocalTime::class.java) {
	override fun deserialize(p: JsonParser, ctxt: DeserializationContext): LocalTime =
			LocalTime.parse(p.readValueAs(String::class.java))
}

class DateTimeModule: SimpleModule() {
	init {
		addSerializer(DateTimeSerializer())
		addDeserializer(DateTime::class.java, DateTimeDeserializer())
		addSerializer(LocalDateSerializer())
		addDeserializer(LocalDate::class.java, LocalDateDeserializer())
		addSerializer(LocalTimeSerializer())
		addDeserializer(LocalTime::class.java, LocalTimeDeserializer())
	}
}
