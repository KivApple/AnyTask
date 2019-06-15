package id.pineapple.anytask

import id.pineapple.anytask.tasks.TaskScheduleOptions
import org.joda.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskScheduleOptionsUnitTest {
	@Test
	fun testOnce() {
		TaskScheduleOptions(
				mode = TaskScheduleOptions.Mode.ONCE,
				startDate = LocalDate(1970, 1, 2),
				stopDate = LocalDate(1970, 1, 2)
		).let {
			assertEquals(LocalDate(1970, 1, 2), it.findNextDate(LocalDate(1970, 1, 1))) // Before
			assertEquals(LocalDate(1970, 1, 2), it.findNextDate(LocalDate(1970, 1, 2))) // Equals
			assertNull(it.findNextDate(LocalDate(1970, 1, 3))) // After
		}
		TaskScheduleOptions(
				mode = TaskScheduleOptions.Mode.ONCE,
				startDate = LocalDate(1970, 1, 2),
				stopDate = LocalDate(1970, 1, 4)
		).let {
			assertEquals(LocalDate(1970, 1, 2),
					it.findNextDate(LocalDate(1970, 1, 1), LocalDate(1970, 1, 1))) // Now is before
			assertEquals(LocalDate(1970, 1, 2),
					it.findNextDate(LocalDate(1970, 1, 1), LocalDate(1970, 1, 2)))
			assertEquals(LocalDate(1970, 1, 3),
					it.findNextDate(LocalDate(1970, 1, 1), LocalDate(1970, 1, 3)))
			assertEquals(LocalDate(1970, 1, 4),
					it.findNextDate(LocalDate(1970, 1, 1), LocalDate(1970, 1, 4)))
			assertEquals(LocalDate(1970, 1, 4),
					it.findNextDate(LocalDate(1970, 1, 1), LocalDate(1970, 1, 5))) // Now is after
			
			assertEquals(LocalDate(1970, 1, 3),
					it.findNextDate(LocalDate(1970, 1, 2), LocalDate(1970, 1, 3)))
			assertEquals(LocalDate(1970, 1, 3),
					it.findNextDate(LocalDate(1970, 1, 3), LocalDate(1970, 1, 3)))
			assertNull(it.findNextDate(LocalDate(1970, 1, 4), LocalDate(1970, 1, 3)))
			assertNull(it.findNextDate(LocalDate(1970, 1, 5), LocalDate(1970, 1, 3)))
		}
		TaskScheduleOptions(
				mode = TaskScheduleOptions.Mode.ONCE,
				startDate = LocalDate(1970, 1, 2),
				interval = 1
		).let {
			assertEquals(LocalDate(1970, 1, 2),
					it.findNextDate(LocalDate(1970, 1, 1), LocalDate(1970, 1, 1)))
			assertEquals(LocalDate(1970, 1, 2),
					it.findNextDate(LocalDate(1970, 1, 2), LocalDate(1970, 1, 1)))
		}
	}
	
	@Test
	fun testDaily() {
		TaskScheduleOptions(
				mode = TaskScheduleOptions.Mode.DAILY,
				startDate = LocalDate(1970, 1, 2),
				stopDate = LocalDate(1970, 1, 10),
				interval = 3
		).let {
			assertEquals(LocalDate(1970, 1, 2), it.findNextDate(LocalDate(1970, 1, 1)))
			assertEquals(LocalDate(1970, 1, 2), it.findNextDate(LocalDate(1970, 1, 2)))
			assertEquals(LocalDate(1970, 1, 5), it.findNextDate(LocalDate(1970, 1, 3)))
			assertEquals(LocalDate(1970, 1, 5), it.findNextDate(LocalDate(1970, 1, 4)))
			assertEquals(LocalDate(1970, 1, 5), it.findNextDate(LocalDate(1970, 1, 5)))
			assertEquals(LocalDate(1970, 1, 8), it.findNextDate(LocalDate(1970, 1, 6)))
			assertEquals(LocalDate(1970, 1, 8), it.findNextDate(LocalDate(1970, 1, 7)))
			assertEquals(LocalDate(1970, 1, 8), it.findNextDate(LocalDate(1970, 1, 8)))
			assertNull(it.findNextDate(LocalDate(1970, 1, 9)))
			assertNull(it.findNextDate(LocalDate(1970, 1, 10)))
			assertNull(it.findNextDate(LocalDate(1970, 1, 11)))
		}
	}
	
	@Test
	fun testWeekly() {
		// 1970-01-05 - Monday
		TaskScheduleOptions(
				mode = TaskScheduleOptions.Mode.WEEKLY,
				startDate = LocalDate(1970, 1, 8),
				stopDate = LocalDate(1970, 2, 1),
				interval = 2,
				weekDaysMask = 2 or 8 // Tuesday, Thursday
		).let {
			assertEquals(LocalDate(1970, 1, 8), it.findNextDate(LocalDate(1970, 1, 1)))
			assertEquals(LocalDate(1970, 1, 8), it.findNextDate(LocalDate(1970, 1, 5)))
			assertEquals(LocalDate(1970, 1, 8), it.findNextDate(LocalDate(1970, 1, 6)))
			assertEquals(LocalDate(1970, 1, 8), it.findNextDate(LocalDate(1970, 1, 7)))
			assertEquals(LocalDate(1970, 1, 8), it.findNextDate(LocalDate(1970, 1, 8)))
			assertEquals(LocalDate(1970, 1, 20), it.findNextDate(LocalDate(1970, 1, 9)))
			assertEquals(LocalDate(1970, 1, 20), it.findNextDate(LocalDate(1970, 1, 17)))
			assertEquals(LocalDate(1970, 1, 20), it.findNextDate(LocalDate(1970, 1, 18)))
			assertEquals(LocalDate(1970, 1, 20), it.findNextDate(LocalDate(1970, 1, 19)))
			assertEquals(LocalDate(1970, 1, 20), it.findNextDate(LocalDate(1970, 1, 20)))
			assertEquals(LocalDate(1970, 1, 22), it.findNextDate(LocalDate(1970, 1, 21)))
			assertEquals(LocalDate(1970, 1, 22), it.findNextDate(LocalDate(1970, 1, 22)))
			assertNull(it.findNextDate(LocalDate(1970, 1, 23)))
		}
	}
	
	@Test
	fun testMonthly() {
		TaskScheduleOptions(
				mode = TaskScheduleOptions.Mode.MONTHLY,
				startDate = LocalDate(1970, 1, 10),
				stopDate = LocalDate(1970, 10, 9),
				interval = 3
		).let {
			assertEquals(LocalDate(1970, 1, 10), it.findNextDate(LocalDate(1970, 1, 9)))
			assertEquals(LocalDate(1970, 1, 10), it.findNextDate(LocalDate(1970, 1, 10)))
			assertEquals(LocalDate(1970, 4, 10), it.findNextDate(LocalDate(1970, 1, 11)))
			assertEquals(LocalDate(1970, 4, 10), it.findNextDate(LocalDate(1970, 1, 31)))
			assertEquals(LocalDate(1970, 4, 10), it.findNextDate(LocalDate(1970, 2, 9)))
			assertEquals(LocalDate(1970, 4, 10), it.findNextDate(LocalDate(1970, 2, 10)))
			assertEquals(LocalDate(1970, 4, 10), it.findNextDate(LocalDate(1970, 2, 11)))
			assertEquals(LocalDate(1970, 4, 10), it.findNextDate(LocalDate(1970, 3, 9)))
			assertEquals(LocalDate(1970, 4, 10), it.findNextDate(LocalDate(1970, 3, 10)))
			assertEquals(LocalDate(1970, 4, 10), it.findNextDate(LocalDate(1970, 3, 11)))
			assertEquals(LocalDate(1970, 4, 10), it.findNextDate(LocalDate(1970, 4, 9)))
			assertEquals(LocalDate(1970, 4, 10), it.findNextDate(LocalDate(1970, 4, 10)))
			assertEquals(LocalDate(1970, 7, 10), it.findNextDate(LocalDate(1970, 4, 11)))
			assertNull(it.findNextDate(LocalDate(1970, 7, 11)))
		}
		TaskScheduleOptions(
				mode = TaskScheduleOptions.Mode.MONTHLY,
				startDate = LocalDate(1970, 1, 31),
				stopDate = LocalDate(1970, 10, 9),
				interval = 1
		).let {
			assertEquals(LocalDate(1970, 1, 31), it.findNextDate(LocalDate(1970, 1, 31)))
			assertEquals(LocalDate(1970, 2, 28), it.findNextDate(LocalDate(1970, 2, 1)))
		}
	}
	
	@Test
	fun testMonthlyInverse() {
		TaskScheduleOptions(
				mode = TaskScheduleOptions.Mode.MONTHLY_INVERSE,
				startDate = LocalDate(1970, 1, 29),
				stopDate = LocalDate(1970, 10, 9),
				interval = 3
		).let {
			assertEquals(LocalDate(1970, 1, 29), it.findNextDate(LocalDate(1970, 1, 28)))
			assertEquals(LocalDate(1970, 1, 29), it.findNextDate(LocalDate(1970, 1, 29)))
			assertEquals(LocalDate(1970, 4, 28), it.findNextDate(LocalDate(1970, 1, 30)))
			assertEquals(LocalDate(1970, 4, 28), it.findNextDate(LocalDate(1970, 2, 1)))
			assertEquals(LocalDate(1970, 4, 28), it.findNextDate(LocalDate(1970, 2, 26)))
			assertEquals(LocalDate(1970, 4, 28), it.findNextDate(LocalDate(1970, 3, 29)))
			assertEquals(LocalDate(1970, 4, 28), it.findNextDate(LocalDate(1970, 4, 27)))
			assertEquals(LocalDate(1970, 4, 28), it.findNextDate(LocalDate(1970, 4, 28)))
			assertEquals(LocalDate(1970, 7, 29), it.findNextDate(LocalDate(1970, 4, 29)))
			assertNull(it.findNextDate(LocalDate(1970, 7, 30)))
		}
		TaskScheduleOptions(
				mode = TaskScheduleOptions.Mode.MONTHLY_INVERSE,
				startDate = LocalDate(1970, 2, 28),
				stopDate = LocalDate(1970, 10, 9),
				interval = 1
		).let {
			assertEquals(LocalDate(1970, 2, 28), it.findNextDate(LocalDate(1970, 1, 1)))
			assertEquals(LocalDate(1970, 2, 28), it.findNextDate(LocalDate(1970, 2, 28)))
			assertEquals(LocalDate(1970, 3, 31), it.findNextDate(LocalDate(1970, 3, 1)))
		}
	}
	
	@Test
	fun testYearly() {
		TaskScheduleOptions(
				mode = TaskScheduleOptions.Mode.YEARLY,
				startDate = LocalDate(1970, 3, 10),
				stopDate = LocalDate(1974, 3, 9),
				interval = 2
		).let {
			assertEquals(LocalDate(1970, 3, 10), it.findNextDate(LocalDate(1970, 1, 1)))
			assertEquals(LocalDate(1970, 3, 10), it.findNextDate(LocalDate(1970, 3, 10)))
			assertEquals(LocalDate(1972, 3, 10), it.findNextDate(LocalDate(1970, 3, 11)))
			assertEquals(LocalDate(1972, 3, 10), it.findNextDate(LocalDate(1972, 3, 9)))
			assertEquals(LocalDate(1972, 3, 10), it.findNextDate(LocalDate(1972, 3, 10)))
			assertNull(it.findNextDate(LocalDate(1972, 3, 11)))
		}
		TaskScheduleOptions(
				mode = TaskScheduleOptions.Mode.YEARLY,
				startDate = LocalDate(1972, 2, 29),
				stopDate = LocalDate(1980, 1, 1),
				interval = 1
		).let {
			assertEquals(LocalDate(1972, 2, 29), it.findNextDate(LocalDate(1972, 2, 29)))
			assertEquals(LocalDate(1973, 2, 28), it.findNextDate(LocalDate(1972, 3, 1)))
			assertEquals(LocalDate(1976, 2, 29), it.findNextDate(LocalDate(1976, 1, 1)))
		}
	}
}
