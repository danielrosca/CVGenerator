package md.daniel_rosca.html

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.lang.reflect.InvocationTargetException
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

/**
 * `calculateDuration`/`sumDurations`/`Duration` are private top-level declarations in
 * HTMLGenerator.kt with no public seam. Reflection is used here purely as test
 * setup/invocation - production code is untouched, and `Duration` (also private) is never
 * named directly in this file; its `years`/`months` fields are read back reflectively too.
 */
class HTMLGeneratorDurationTest {
    // Top-level Kotlin functions in HTMLGenerator.kt compile into a class named HTMLGeneratorKt.
    private val generatorClass = Class.forName("md.daniel_rosca.html.HTMLGeneratorKt")

    private fun calculateDuration(
        start: Date,
        end: Date,
    ): Pair<Long, Long> {
        val method = generatorClass.getDeclaredMethod("calculateDuration", Date::class.java, Date::class.java)
        method.isAccessible = true
        return unwrapDuration(invoke(method, null, start, end))
    }

    private fun sumDurations(durations: List<Pair<Long, Long>>): Pair<Long, Long> {
        val durationClass = Class.forName("md.daniel_rosca.html.Duration")
        // Non-null Kotlin `Long` fields compile to primitive `long`, so the constructor needs the
        // primitive type token (javaPrimitiveType), not the boxed java.lang.Long class.
        val ctor = durationClass.getDeclaredConstructor(Long::class.javaPrimitiveType, Long::class.javaPrimitiveType)
        ctor.isAccessible = true
        val durationInstances = durations.map { (years, months) -> ctor.newInstance(years, months) }

        val method = generatorClass.getDeclaredMethod("sumDurations", List::class.java)
        method.isAccessible = true
        return unwrapDuration(invoke(method, null, durationInstances))
    }

    private fun invoke(
        method: java.lang.reflect.Method,
        target: Any?,
        vararg args: Any?,
    ): Any {
        return try {
            method.invoke(target, *args)!!
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }

    private fun unwrapDuration(result: Any): Pair<Long, Long> {
        val klass = result.javaClass
        val years = klass.getDeclaredField("years").apply { isAccessible = true }.get(result) as Long
        val months = klass.getDeclaredField("months").apply { isAccessible = true }.get(result) as Long
        return years to months
    }

    private fun dateOf(localDate: LocalDate): Date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())

    @Test
    fun `exact month boundary with no leftover days`() {
        val (years, months) = calculateDuration(dateOf(LocalDate.of(2020, 1, 1)), dateOf(LocalDate.of(2020, 4, 1)))

        assertEquals(0L, years)
        assertEquals(3L, months)
    }

    @Test
    fun `leftover days round up to an additional month`() {
        // 1 full month (Jan 1 -> Feb 1) plus 14 leftover days (Feb 1 -> Feb 15) rounds up to 2 months
        val (years, months) = calculateDuration(dateOf(LocalDate.of(2020, 1, 1)), dateOf(LocalDate.of(2020, 2, 15)))

        assertEquals(0L, years)
        assertEquals(2L, months)
    }

    @Test
    fun `same start and end date floors to 1 month, never 0`() {
        val (years, months) = calculateDuration(dateOf(LocalDate.of(2020, 1, 1)), dateOf(LocalDate.of(2020, 1, 1)))

        assertEquals(0L, years)
        assertEquals(1L, months)
    }

    @Test
    fun `a multi-year span converts to years and remainder months`() {
        val (years, months) = calculateDuration(dateOf(LocalDate.of(2018, 3, 1)), dateOf(LocalDate.of(2020, 8, 1)))

        assertEquals(2L, years)
        assertEquals(5L, months)
    }

    @Test
    fun `an open-ended job's duration measured against now`() {
        // Using LocalDate.now() minus exactly 6 months (same day-of-month) keeps this deterministic:
        // no leftover-day rounding to depend on, unlike using the wall clock's instant directly.
        val today = LocalDate.now()
        val start = dateOf(today.minusMonths(6))
        val now = dateOf(today)

        val (years, months) = calculateDuration(start, now)

        assertEquals(0L, years)
        assertEquals(6L, months)
    }

    @Test
    fun `sumDurations combines multiple durations and carries months into years`() {
        // 8 months + 1 year 5 months = 25 months total = 2 years 1 month
        val total = sumDurations(listOf(0L to 8L, 1L to 5L))

        assertEquals(2L to 1L, total)
    }

    @Test
    fun `sumDurations floors an empty or all-zero total to 1 month`() {
        val total = sumDurations(emptyList())

        assertEquals(0L to 1L, total)
    }
}
