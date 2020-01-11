@file:Suppress("FunctionName")

package dev.nohus.autokonfig

import java.time.*
import java.time.format.DateTimeParseException
import java.util.*

/**
 * Created by Marcin Wisniowski (Nohus) on 11/01/2020.
 */

data class SettingType<T>(val transform: (String) -> T)

val StringSettingType: SettingType<String> = SettingType(::mapString)
val IntSettingType: SettingType<Int> = SettingType(::mapInt)
val LongSettingType: SettingType<Long> = SettingType(::mapLong)
val FloatSettingType: SettingType<Float> = SettingType(::mapFloat)
val DoubleSettingType: SettingType<Double> = SettingType(::mapDouble)
fun <T : Enum<T>> EnumSettingType(enum: Class<T>): SettingType<T> = SettingType { mapEnum(it, enum) }
val InstantSettingType: SettingType<Instant> = SettingType(::mapInstant)
val DurationSettingType: SettingType<Duration> = SettingType(::mapDuration)
val LocalTimeSettingType: SettingType<LocalTime> = SettingType(::mapLocalTime)
val LocalDateSettingType: SettingType<LocalDate> = SettingType(::mapLocalDate)
val LocalDateTimeSettingType: SettingType<LocalDateTime> = SettingType(::mapLocalDateTime)
fun <T> ListSettingType(type: SettingType<T>): SettingType<List<T>> = SettingType { mapList(it, type) }
fun <T> ListSettingType(type: SettingType<T>, separator: Regex): SettingType<List<T>> = SettingType { mapList(it, type, separator) }
fun <T> ListSettingType(type: SettingType<T>, separator: String): SettingType<List<T>> = SettingType { mapList(it, type, separator) }
fun <T> SetSettingType(type: SettingType<T>): SettingType<Set<T>> = SettingType { mapSet(it, type) }
fun <T> SetSettingType(type: SettingType<T>, separator: Regex): SettingType<Set<T>> = SettingType { mapSet(it, type, separator) }
fun <T> SetSettingType(type: SettingType<T>, separator: String): SettingType<Set<T>> = SettingType { mapSet(it, type, separator) }
val BooleanSettingType: SettingType<Boolean> = SettingType(::mapBoolean)

private fun mapString(value: String) = value
private fun mapInt(value: String) = try { value.toInt() } catch (e: NumberFormatException) { throw SettingParseException("must be an Int number", e) }
private fun mapLong(value: String) = try { value.toLong() } catch (e: NumberFormatException) { throw SettingParseException("must be a Long number", e) }
private fun mapFloat(value: String) = try { value.toFloat() } catch (e: NumberFormatException) { throw SettingParseException("must be a Float number", e) }
private fun mapDouble(value: String) = try { value.toDouble() } catch (e: NumberFormatException) { throw SettingParseException("must be a Double number", e) }
private fun <T : Enum<T>> mapEnum(value: String, enum: Class<T>): T {
    val map = EnumSet.allOf(enum).map { it.name to it }.toMap()
    return try {
        map[value] ?: map.entries.first { it.key.toLowerCase(Locale.US) == value.toLowerCase(Locale.US) }.value
    } catch (e: NoSuchElementException) {
        throw SettingParseException("possible values are ${map.keys}", e)
    }
}
private fun mapInstant(value: String) = try { Instant.parse(value) } catch (e: DateTimeParseException) { throw SettingParseException("must be an Instant", e) }
private fun mapDuration(value: String) = try { Duration.parse(value) } catch (e: DateTimeParseException) { throw SettingParseException("must be a Duration", e) }
private fun mapLocalTime(value: String) = try { LocalTime.parse(value) } catch (e: DateTimeParseException) { throw SettingParseException("must be a LocalTime", e) }
private fun mapLocalDate(value: String) = try { LocalDate.parse(value) } catch (e: DateTimeParseException) { throw SettingParseException("must be a LocalDate", e) }
private fun mapLocalDateTime(value: String) = try { LocalDateTime.parse(value) } catch (e: DateTimeParseException) { throw SettingParseException("must be a LocalDateTime", e) }
private val separatorRegex = Regex(",\\s*")
private fun <T> mapList(value: String, type: SettingType<T>) = value.split(separatorRegex).map { type.transform(it) }
private fun <T> mapList(value: String, type: SettingType<T>, separator: Regex) = value.split(separator).map { type.transform(it) }
private fun <T> mapList(value: String, type: SettingType<T>, separator: String) = value.split(separator).map { type.transform(it) }
private fun <T> mapSet(value: String, type: SettingType<T>) = mapList(value, type).toSet()
private fun <T> mapSet(value: String, type: SettingType<T>, separator: Regex) = mapList(value, type, separator).toSet()
private fun <T> mapSet(value: String, type: SettingType<T>, separator: String) = mapList(value, type, separator).toSet()
private fun mapBoolean(value: String) = value in listOf("true", "yes", "1")