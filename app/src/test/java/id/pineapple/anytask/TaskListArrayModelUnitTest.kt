package id.pineapple.anytask

import id.pineapple.anytask.tasks.Task
import id.pineapple.anytask.tasks.TaskInfo
import id.pineapple.anytask.tasks.TaskListArrayModel
import org.junit.Assert.*
import org.junit.Test

class TaskListArrayModelUnitTest {
	@Test
	fun testOrder() {
		val model = TaskListArrayModel(listOf(
				Task(id = 1, title = "Task 1", completed = false, position = 1),
				Task(id = 2, title = "Task 2", completed = true, position = 2),
				Task(id = 3, title = "Task 3", completed = false, position = 3)
		).map { TaskInfo(it) })
		val items = model.items
		assertEquals(3, items.size)
		assertEquals(1L, items[0].id)
		assertEquals(3L, items[1].id)
		assertEquals(2L, items[2].id)
	}
	
	@Test
	fun testInsert() {
		val model = TaskListArrayModel()
		model.insert(
				Task(title = "Task 1", completed = false, position = 1),
				Task(id = 5, title = "Task 2", completed = true, position = 2),
				Task(title = "Task 3", completed = false, position = 3)
		) {
			assertEquals(3, it.size)
			assertEquals(1L, it[0])
			assertEquals(5L, it[1])
			assertEquals(6L, it[2])
		}
		val items = model.items
		assertEquals(3, items.size)
		assertEquals(1L, items[0].id)
		assertEquals(6L, items[1].id)
		assertEquals(5L, items[2].id)
	}
	
	@Test
	fun testUpdate() {
		val t1 = Task(id = 1, title = "Task 1", completed = false, position = 1)
		val model = TaskListArrayModel(listOf(
				t1,
				Task(id = 2, title = "Task 2", completed = false, position = 2)
		).map { TaskInfo(it) })
		assertEquals(1L, model.items.first().id)
		assertEquals(2L, model.items[1].id)
		model.update(t1.updateCompleted(true, -1))
		assertEquals(2L, model.items.first().id)
		assertEquals(1L, model.items[1].id)
		model.update(t1)
		assertEquals(1L, model.items.first().id)
		assertEquals(2L, model.items[1].id)
	}
	
	@Test
	fun testDelete() {
		val model = TaskListArrayModel(listOf(
				Task(id = 1, title = "Task 1", completed = false, position = 1),
				Task(id = 2, title = "Task 2", completed = true, position = 2),
				Task(id = 3, title = "Task 3", completed = false, position = 3)
		).map { TaskInfo(it) })
		model.delete(Task(id = 1, title = "Task 1", completed = false, position = 1))
		val items = model.items
		assertEquals(2, items.size)
		assertEquals(3L, items[0].id)
		assertEquals(2L, items[1].id)
	}
	
	@Test
	fun testFetch() {
		val model = TaskListArrayModel(listOf(
				Task(id = 1, title = "Task 1", completed = false, position = 1),
				Task(id = 2, title = "Task 2", completed = true, position = 2),
				Task(id = 3, title = "Task 3", completed = false, position = 3)
		).map { TaskInfo(it) })
		model.fetch(1) {
			assertNotNull(it)
			assertEquals(1L, it!!.id)
		}
		model.fetch(4) {
			assertNull(it)
		}
		model.insert(Task(id = 4, title = "Task 4", completed = false, position = 3))
		model.fetch(4) {
			assertNotNull(it)
			assertEquals(4L, it!!.id)
		}
		model.delete(Task(id = 2, title = "Task 2", completed = true, position = 2))
		model.fetch(2) {
			assertNull(it)
		}
	}
}
