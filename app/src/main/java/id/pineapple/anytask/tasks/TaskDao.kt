package id.pineapple.anytask.tasks

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.joda.time.LocalTime

@Dao
interface TaskDao {
	@Query("SELECT * FROM tasks ORDER BY schedule_mode DESC, originDate ASC, position ASC")
	fun findAllSync(): List<Task>
	
	@Query("""
		SELECT
			task.id as id,
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.titleFromTemplate != 0 THEN
					template.title
				ELSE task.title
			END as title,
			task.titleFromTemplate as titleFromTemplate,
			task.completed as completed,
			task.position as position,
			task.savedPosition as savedPosition,
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.priorityFromTemplate != 0 THEN
					template.priority
				ELSE task.priority
			END	as priority,
			task.priorityFromTemplate as priorityFromTemplate,
			task.schedule_startDate as schedule_startDate,
			task.schedule_stopDate as schedule_stopDate,
			task.schedule_mode as schedule_mode,
			task.schedule_interval as schedule_interval,
			task.schedule_weekDaysMask as schedule_weekDaysMask,
			task.scheduleTemplateId as scheduleTemplateId,
			task.originDate as originDate,
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.pinnedFromTemplate != 0 THEN
					template.pinned
				ELSE task.pinned
			END as pinned,
			task.pinnedFromTemplate as pinnedFromTemplate,
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.alarmTimeFromTemplate != 0 THEN
					template.alarmTime
				ELSE task.alarmTime
			END as alarmTime,
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.noteIdFromTemplate != 0 THEN
					template.noteId
				ELSE task.noteId
			END as noteId,
			task.noteIdFromTemplate as noteIdFromTemplate,
			task.alarmTimeFromTemplate as alarmTimeFromTemplate,
			task.titleUpdateTime as titleUpdateTime,
			task.completedUpdateTime as completedUpdateTime,
			task.positionUpdateTime as positionUpdateTime,
			task.scheduleOptionsUpdateTime as scheduleOptionsUpdateTime,
			task.pinnedUpdateTime as pinnedUpdateTime,
			task.alarmTimeUpdateTime as alarmTimeUpdateTime,
			task.noteIdUpdateTime as noteIdUpdateTime,
			template.id as template_id,
			template.title as template_title,
			template.titleFromTemplate as template_titleFromTemplate,
			template.completed as template_completed,
			template.position as template_position,
			template.savedPosition as template_savedPosition,
			template.priority as template_priority,
			template.priorityFromTemplate as template_priorityFromTemplate,
			template.schedule_startDate as template_schedule_startDate,
			template.schedule_stopDate as template_schedule_stopDate,
			template.schedule_mode as template_schedule_mode,
			template.schedule_interval as template_schedule_interval,
			template.schedule_weekDaysMask as template_schedule_weekDaysMask,
			template.scheduleTemplateId as template_scheduleTemplateId,
			template.originDate as template_originDate,
			template.pinned as template_pinned,
			template.pinnedFromTemplate as template_pinnedFromTemplate,
			template.alarmTime as template_alarmTime,
			template.alarmTimeFromTemplate as template_alarmTimeFromTemplate,
			template.noteId as template_noteId,
			template.noteIdFromTemplate as template_noteIdFromTemplate,
			template.titleUpdateTime as template_titleUpdateTime,
			template.completedUpdateTime as template_completedUpdateTime,
			template.positionUpdateTime as template_positionUpdateTime,
			template.scheduleOptionsUpdateTime as template_scheduleOptionsUpdateTime,
			template.pinnedUpdateTime as template_pinnedUpdateTime,
			template.alarmTimeUpdateTime as template_alarmTimeUpdateTime,
			template.noteIdUpdateTime as template_noteIdUpdateTime,
			note.id as note_id,
			note.parentId as note_parentId,
			note.title as note_title,
			note.type as note_type,
			note.text as note_text,
			note.position as note_position,
			note.titleUpdateTime as note_titleUpdateTime,
			note.contentUpdateTime as note_contentUpdateTime,
			note.positionUpdateTime as note_positionUpdateTime
		FROM
			tasks AS task
		LEFT JOIN
			tasks AS template
		ON
			task.scheduleTemplateId = template.id
		LEFT JOIN
			notes AS note
		ON
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.noteIdFromTemplate != 0 THEN
					template.noteId
				ELSE task.noteId
			END = note.id
		WHERE
			task.id = :id
	""")
	fun findById(id: Long): LiveData<TaskInfo>
	
	@Query("""
		SELECT
			task.id as id,
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.titleFromTemplate != 0 THEN
					template.title
				ELSE task.title
			END as title,
			task.titleFromTemplate as titleFromTemplate,
			task.completed as completed,
			task.position as position,
			task.savedPosition as savedPosition,
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.priorityFromTemplate != 0 THEN
					template.priority
				ELSE task.priority
			END	as priority,
			task.priorityFromTemplate as priorityFromTemplate,
			task.schedule_startDate as schedule_startDate,
			task.schedule_stopDate as schedule_stopDate,
			task.schedule_mode as schedule_mode,
			task.schedule_interval as schedule_interval,
			task.schedule_weekDaysMask as schedule_weekDaysMask,
			task.scheduleTemplateId as scheduleTemplateId,
			task.originDate as originDate,
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.pinnedFromTemplate != 0 THEN
					template.pinned
				ELSE task.pinned
			END as pinned,
			task.pinnedFromTemplate as pinnedFromTemplate,
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.alarmTimeFromTemplate != 0 THEN
					template.alarmTime
				ELSE task.alarmTime
			END as alarmTime,
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.noteIdFromTemplate != 0 THEN
					template.noteId
				ELSE task.noteId
			END as noteId,
			task.noteIdFromTemplate as noteIdFromTemplate,
			task.alarmTimeFromTemplate as alarmTimeFromTemplate,
			task.titleUpdateTime as titleUpdateTime,
			task.completedUpdateTime as completedUpdateTime,
			task.positionUpdateTime as positionUpdateTime,
			task.scheduleOptionsUpdateTime as scheduleOptionsUpdateTime,
			task.pinnedUpdateTime as pinnedUpdateTime,
			task.alarmTimeUpdateTime as alarmTimeUpdateTime,
			task.noteIdUpdateTime as noteIdUpdateTime,
			template.id as template_id,
			template.title as template_title,
			template.titleFromTemplate as template_titleFromTemplate,
			template.completed as template_completed,
			template.position as template_position,
			template.savedPosition as template_savedPosition,
			template.priority as template_priority,
			template.priorityFromTemplate as template_priorityFromTemplate,
			template.schedule_startDate as template_schedule_startDate,
			template.schedule_stopDate as template_schedule_stopDate,
			template.schedule_mode as template_schedule_mode,
			template.schedule_interval as template_schedule_interval,
			template.schedule_weekDaysMask as template_schedule_weekDaysMask,
			template.scheduleTemplateId as template_scheduleTemplateId,
			template.originDate as template_originDate,
			template.pinned as template_pinned,
			template.pinnedFromTemplate as template_pinnedFromTemplate,
			template.alarmTime as template_alarmTime,
			template.alarmTimeFromTemplate as template_alarmTimeFromTemplate,
			template.noteId as template_noteId,
			template.noteIdFromTemplate as template_noteIdFromTemplate,
			template.titleUpdateTime as template_titleUpdateTime,
			template.completedUpdateTime as template_completedUpdateTime,
			template.positionUpdateTime as template_positionUpdateTime,
			template.scheduleOptionsUpdateTime as template_scheduleOptionsUpdateTime,
			template.pinnedUpdateTime as template_pinnedUpdateTime,
			template.alarmTimeUpdateTime as template_alarmTimeUpdateTime,
			template.noteIdUpdateTime as template_noteIdUpdateTime,
			note.id as note_id,
			note.parentId as note_parentId,
			note.title as note_title,
			note.type as note_type,
			note.text as note_text,
			note.position as note_position,
			note.titleUpdateTime as note_titleUpdateTime,
			note.contentUpdateTime as note_contentUpdateTime,
			note.positionUpdateTime as note_positionUpdateTime
		FROM
			tasks AS task
		LEFT JOIN
			tasks AS template
		ON
			task.scheduleTemplateId = template.id
		LEFT JOIN
			notes AS note
		ON
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.noteIdFromTemplate != 0 THEN
					template.noteId
				ELSE task.noteId
			END = note.id
		WHERE
			task.id = :id
	""")
	fun findByIdSync(id: Long): TaskInfo?
	
	@Query("""
		SELECT
			task.id as id,
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.titleFromTemplate != 0 THEN
					template.title
				ELSE task.title
			END as title,
			task.titleFromTemplate as titleFromTemplate,
			task.completed as completed,
			task.position as position,
			task.savedPosition as savedPosition,
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.priorityFromTemplate != 0 THEN
					template.priority
				ELSE task.priority
			END	as priority,
			task.priorityFromTemplate as priorityFromTemplate,
			task.schedule_startDate as schedule_startDate,
			task.schedule_stopDate as schedule_stopDate,
			task.schedule_mode as schedule_mode,
			task.schedule_interval as schedule_interval,
			task.schedule_weekDaysMask as schedule_weekDaysMask,
			task.scheduleTemplateId as scheduleTemplateId,
			task.originDate as originDate,
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.pinnedFromTemplate != 0 THEN
					template.pinned
				ELSE task.pinned
			END as pinned,
			task.pinnedFromTemplate as pinnedFromTemplate,
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.alarmTimeFromTemplate != 0 THEN
					template.alarmTime
				ELSE task.alarmTime
			END as alarmTime,
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.noteIdFromTemplate != 0 THEN
					template.noteId
				ELSE task.noteId
			END as noteId,
			task.noteIdFromTemplate as noteIdFromTemplate,
			task.alarmTimeFromTemplate as alarmTimeFromTemplate,
			task.titleUpdateTime as titleUpdateTime,
			task.completedUpdateTime as completedUpdateTime,
			task.positionUpdateTime as positionUpdateTime,
			task.scheduleOptionsUpdateTime as scheduleOptionsUpdateTime,
			task.pinnedUpdateTime as pinnedUpdateTime,
			task.alarmTimeUpdateTime as alarmTimeUpdateTime,
			task.noteIdUpdateTime as noteIdUpdateTime,
			template.id as template_id,
			template.title as template_title,
			template.titleFromTemplate as template_titleFromTemplate,
			template.completed as template_completed,
			template.position as template_position,
			template.savedPosition as template_savedPosition,
			template.priority as template_priority,
			template.priorityFromTemplate as template_priorityFromTemplate,
			template.schedule_startDate as template_schedule_startDate,
			template.schedule_stopDate as template_schedule_stopDate,
			template.schedule_mode as template_schedule_mode,
			template.schedule_interval as template_schedule_interval,
			template.schedule_weekDaysMask as template_schedule_weekDaysMask,
			template.scheduleTemplateId as template_scheduleTemplateId,
			template.originDate as template_originDate,
			template.pinned as template_pinned,
			template.pinnedFromTemplate as template_pinnedFromTemplate,
			template.alarmTime as template_alarmTime,
			template.alarmTimeFromTemplate as template_alarmTimeFromTemplate,
			template.noteId as template_noteId,
			template.noteIdFromTemplate as template_noteIdFromTemplate,
			template.titleUpdateTime as template_titleUpdateTime,
			template.completedUpdateTime as template_completedUpdateTime,
			template.positionUpdateTime as template_positionUpdateTime,
			template.scheduleOptionsUpdateTime as template_scheduleOptionsUpdateTime,
			template.pinnedUpdateTime as template_pinnedUpdateTime,
			template.alarmTimeUpdateTime as template_alarmTimeUpdateTime,
			template.noteIdUpdateTime as template_noteIdUpdateTime,
			note.id as note_id,
			note.parentId as note_parentId,
			note.title as note_title,
			note.type as note_type,
			note.text as note_text,
			note.position as note_position,
			note.titleUpdateTime as note_titleUpdateTime,
			note.contentUpdateTime as note_contentUpdateTime,
			note.positionUpdateTime as note_positionUpdateTime
		FROM
			tasks AS task
		LEFT JOIN
			tasks AS template
		ON
			task.scheduleTemplateId = template.id
		LEFT JOIN
			notes AS note
		ON
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.noteIdFromTemplate != 0 THEN
					template.noteId
				ELSE task.noteId
			END = note.id
		WHERE
			:date >= task.schedule_startDate AND :date <= task.schedule_stopDate OR
			task.originDate = :date
		ORDER BY
			task.completed ASC,
			CASE task.completed
				WHEN 0 THEN CASE
					WHEN task.scheduleTemplateId IS NOT NULL AND task.priorityFromTemplate != 0 THEN
						template.priority
					ELSE task.priority
				END
				ELSE 0
			END DESC,
			CASE task.schedule_startDate
				WHEN :date THEN task.position
				ELSE task.position + ${Task.AUTO_POSITION_OFFSET}
			END ASC
	""")
	fun findByDate(date: LocalDate): LiveData<List<TaskInfo>>
	
	@Query("""
		SELECT
			task.id as id,
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.titleFromTemplate != 0 THEN
					template.title
				ELSE task.title
			END as title,
			task.titleFromTemplate as titleFromTemplate,
			task.completed as completed,
			task.position as position,
			task.savedPosition as savedPosition,
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.priorityFromTemplate != 0 THEN
					template.priority
				ELSE task.priority
			END	as priority,
			task.priorityFromTemplate as priorityFromTemplate,
			task.schedule_startDate as schedule_startDate,
			task.schedule_stopDate as schedule_stopDate,
			task.schedule_mode as schedule_mode,
			task.schedule_interval as schedule_interval,
			task.schedule_weekDaysMask as schedule_weekDaysMask,
			task.scheduleTemplateId as scheduleTemplateId,
			task.originDate as originDate,
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.pinnedFromTemplate != 0 THEN
					template.pinned
				ELSE task.pinned
			END as pinned,
			task.pinnedFromTemplate as pinnedFromTemplate,
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.alarmTimeFromTemplate != 0 THEN
					template.alarmTime
				ELSE task.alarmTime
			END as alarmTime,
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.noteIdFromTemplate != 0 THEN
					template.noteId
				ELSE task.noteId
			END as noteId,
			task.noteIdFromTemplate as noteIdFromTemplate,
			task.alarmTimeFromTemplate as alarmTimeFromTemplate,
			task.titleUpdateTime as titleUpdateTime,
			task.completedUpdateTime as completedUpdateTime,
			task.positionUpdateTime as positionUpdateTime,
			task.scheduleOptionsUpdateTime as scheduleOptionsUpdateTime,
			task.pinnedUpdateTime as pinnedUpdateTime,
			task.alarmTimeUpdateTime as alarmTimeUpdateTime,
			task.noteIdUpdateTime as noteIdUpdateTime,
			template.id as template_id,
			template.title as template_title,
			template.titleFromTemplate as template_titleFromTemplate,
			template.completed as template_completed,
			template.position as template_position,
			template.savedPosition as template_savedPosition,
			template.priority as template_priority,
			template.priorityFromTemplate as template_priorityFromTemplate,
			template.schedule_startDate as template_schedule_startDate,
			template.schedule_stopDate as template_schedule_stopDate,
			template.schedule_mode as template_schedule_mode,
			template.schedule_interval as template_schedule_interval,
			template.schedule_weekDaysMask as template_schedule_weekDaysMask,
			template.scheduleTemplateId as template_scheduleTemplateId,
			template.originDate as template_originDate,
			template.pinned as template_pinned,
			template.pinnedFromTemplate as template_pinnedFromTemplate,
			template.alarmTime as template_alarmTime,
			template.alarmTimeFromTemplate as template_alarmTimeFromTemplate,
			template.noteId as template_noteId,
			template.noteIdFromTemplate as template_noteIdFromTemplate,
			template.titleUpdateTime as template_titleUpdateTime,
			template.completedUpdateTime as template_completedUpdateTime,
			template.positionUpdateTime as template_positionUpdateTime,
			template.scheduleOptionsUpdateTime as template_scheduleOptionsUpdateTime,
			template.pinnedUpdateTime as template_pinnedUpdateTime,
			template.alarmTimeUpdateTime as template_alarmTimeUpdateTime,
			template.noteIdUpdateTime as template_noteIdUpdateTime,
			note.id as note_id,
			note.parentId as note_parentId,
			note.title as note_title,
			note.type as note_type,
			note.text as note_text,
			note.position as note_position,
			note.titleUpdateTime as note_titleUpdateTime,
			note.contentUpdateTime as note_contentUpdateTime,
			note.positionUpdateTime as note_positionUpdateTime
		FROM
			tasks AS task
		LEFT JOIN
			tasks AS template
		ON
			task.scheduleTemplateId = template.id
		LEFT JOIN
			notes AS note
		ON
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.noteIdFromTemplate != 0 THEN
					template.noteId
				ELSE task.noteId
			END = note.id
		WHERE
			:date >= task.schedule_startDate AND :date <= task.schedule_stopDate OR
			task.originDate = :date
		ORDER BY
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.priorityFromTemplate != 0 THEN
					template.priority
				ELSE task.priority
			END DESC,
			CASE task.schedule_startDate
				WHEN :date THEN task.position
				ELSE task.position + ${Task.AUTO_POSITION_OFFSET}
			END ASC
	""")
	fun findByDateAltSort(date: LocalDate): LiveData<List<TaskInfo>>
	
	@Query("""
		SELECT
			task.id as id,
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.titleFromTemplate != 0 THEN
					template.title
				ELSE task.title
			END as title,
			task.titleFromTemplate as titleFromTemplate,
			task.completed as completed,
			task.position as position,
			task.savedPosition as savedPosition,
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.priorityFromTemplate != 0 THEN
					template.priority
				ELSE task.priority
			END	as priority,
			task.priorityFromTemplate as priorityFromTemplate,
			task.schedule_startDate as schedule_startDate,
			task.schedule_stopDate as schedule_stopDate,
			task.schedule_mode as schedule_mode,
			task.schedule_interval as schedule_interval,
			task.schedule_weekDaysMask as schedule_weekDaysMask,
			task.scheduleTemplateId as scheduleTemplateId,
			task.originDate as originDate,
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.pinnedFromTemplate != 0 THEN
					template.pinned
				ELSE task.pinned
			END as pinned,
			task.pinnedFromTemplate as pinnedFromTemplate,
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.alarmTimeFromTemplate != 0 THEN
					template.alarmTime
				ELSE task.alarmTime
			END as alarmTime,
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.noteIdFromTemplate != 0 THEN
					template.noteId
				ELSE task.noteId
			END as noteId,
			task.noteIdFromTemplate as noteIdFromTemplate,
			task.alarmTimeFromTemplate as alarmTimeFromTemplate,
			task.titleUpdateTime as titleUpdateTime,
			task.completedUpdateTime as completedUpdateTime,
			task.positionUpdateTime as positionUpdateTime,
			task.scheduleOptionsUpdateTime as scheduleOptionsUpdateTime,
			task.pinnedUpdateTime as pinnedUpdateTime,
			task.alarmTimeUpdateTime as alarmTimeUpdateTime,
			task.noteIdUpdateTime as noteIdUpdateTime,
			template.id as template_id,
			template.title as template_title,
			template.titleFromTemplate as template_titleFromTemplate,
			template.completed as template_completed,
			template.position as template_position,
			template.savedPosition as template_savedPosition,
			template.priority as template_priority,
			template.priorityFromTemplate as template_priorityFromTemplate,
			template.schedule_startDate as template_schedule_startDate,
			template.schedule_stopDate as template_schedule_stopDate,
			template.schedule_mode as template_schedule_mode,
			template.schedule_interval as template_schedule_interval,
			template.schedule_weekDaysMask as template_schedule_weekDaysMask,
			template.scheduleTemplateId as template_scheduleTemplateId,
			template.originDate as template_originDate,
			template.pinned as template_pinned,
			template.pinnedFromTemplate as template_pinnedFromTemplate,
			template.alarmTime as template_alarmTime,
			template.alarmTimeFromTemplate as template_alarmTimeFromTemplate,
			template.noteId as template_noteId,
			template.noteIdFromTemplate as template_noteIdFromTemplate,
			template.titleUpdateTime as template_titleUpdateTime,
			template.completedUpdateTime as template_completedUpdateTime,
			template.positionUpdateTime as template_positionUpdateTime,
			template.scheduleOptionsUpdateTime as template_scheduleOptionsUpdateTime,
			template.pinnedUpdateTime as template_pinnedUpdateTime,
			template.alarmTimeUpdateTime as template_alarmTimeUpdateTime,
			template.noteIdUpdateTime as template_noteIdUpdateTime,
			note.id as note_id,
			note.parentId as note_parentId,
			note.title as note_title,
			note.type as note_type,
			note.text as note_text,
			note.position as note_position,
			note.titleUpdateTime as note_titleUpdateTime,
			note.contentUpdateTime as note_contentUpdateTime,
			note.positionUpdateTime as note_positionUpdateTime
		FROM
			tasks AS task
		LEFT JOIN
			tasks AS template
		ON
			task.scheduleTemplateId = template.id
		LEFT JOIN
			notes AS note
		ON
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.noteIdFromTemplate != 0 THEN
					template.noteId
				ELSE task.noteId
			END = note.id
		WHERE
			:date >= task.schedule_startDate AND :date <= task.schedule_stopDate OR
			task.originDate = :date
		ORDER BY
			task.completed ASC,
			CASE task.completed
				WHEN 0 THEN task.priority
				ELSE 0
			END DESC,
			CASE task.schedule_startDate
				WHEN :date THEN task.position
				ELSE task.position + ${Task.AUTO_POSITION_OFFSET}
			END ASC
	""")
	fun findByDateSync(date: LocalDate): List<TaskInfo>
	
	@Query("""
		SELECT
			task.id as id,
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.titleFromTemplate != 0 THEN
					template.title
				ELSE task.title
			END as title,
			task.titleFromTemplate as titleFromTemplate,
			task.completed as completed,
			task.position as position,
			task.savedPosition as savedPosition,
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.priorityFromTemplate != 0 THEN
					template.priority
				ELSE task.priority
			END	as priority,
			task.priorityFromTemplate as priorityFromTemplate,
			task.schedule_startDate as schedule_startDate,
			task.schedule_stopDate as schedule_stopDate,
			task.schedule_mode as schedule_mode,
			task.schedule_interval as schedule_interval,
			task.schedule_weekDaysMask as schedule_weekDaysMask,
			task.scheduleTemplateId as scheduleTemplateId,
			task.originDate as originDate,
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.pinnedFromTemplate != 0 THEN
					template.pinned
				ELSE task.pinned
			END as pinned,
			task.pinnedFromTemplate as pinnedFromTemplate,
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.alarmTimeFromTemplate != 0 THEN
					template.alarmTime
				ELSE task.alarmTime
			END as alarmTime,
			CASE
				WHEN task.scheduleTemplateId IS NOT NULL AND task.noteIdFromTemplate != 0 THEN
					template.noteId
				ELSE task.noteId
			END as noteId,
			task.noteIdFromTemplate as noteIdFromTemplate,
			task.alarmTimeFromTemplate as alarmTimeFromTemplate,
			task.titleUpdateTime as titleUpdateTime,
			task.completedUpdateTime as completedUpdateTime,
			task.positionUpdateTime as positionUpdateTime,
			task.scheduleOptionsUpdateTime as scheduleOptionsUpdateTime,
			task.pinnedUpdateTime as pinnedUpdateTime,
			task.alarmTimeUpdateTime as alarmTimeUpdateTime,
			task.noteIdUpdateTime as noteIdUpdateTime
		FROM
			tasks AS task
		LEFT JOIN
			tasks AS template
		ON
			task.scheduleTemplateId = template.id
		WHERE
			:stopDate >= task.schedule_startDate AND task.schedule_stopDate >= :startDate OR
			task.originDate >= :startDate AND task.originDate <= :stopDate
	""")
	fun findByDateRangeSync(startDate: LocalDate, stopDate: LocalDate): List<Task>
	
	@Query("""
		SELECT
			task.originDate
		FROM
			tasks AS task
		WHERE
			task.scheduleTemplateId = :templateTaskId AND
			task.originDate < :date
	""")
	fun findClonedTaskDatesByTemplateIdBeforeDateSync(
			templateTaskId: Long, date: LocalDate
	): List<LocalDate>
	
	@Insert
	fun insert(vararg tasks: Task): List<Long>
	
	@Update
	fun update(vararg tasks: Task)
	
	@Query("""
		UPDATE
			tasks
		SET
			title = :title,
			titleUpdateTime = :updateTime
		WHERE
			id = :id
	""")
	fun updateTitle(id: Long, title: String, updateTime: DateTime)
	
	@Delete
	fun delete(vararg task: Task)
	
	@Query(
			"""
				DELETE
				FROM
					tasks
				WHERE
					scheduleTemplateId = :templateTaskId AND
					schedule_startDate > :date
			"""
	)
	fun deleteByTemplateIdAfterDate(templateTaskId: Long, date: LocalDate)
	
	@Query(
			"""
				UPDATE
					tasks
				SET
					scheduleTemplateId = NULL,
					title = :title,
					titleFromTemplate = 1,
					titleUpdateTime = :now,
					priority = CASE WHEN priorityFromTemplate != 0 THEN :priority ELSE priority END,
					positionUpdateTime = CASE WHEN priorityFromTemplate != 0 THEN :now ELSE positionUpdateTime END,
					priorityFromTemplate = 1,
					pinned = CASE WHEN pinnedFromTemplate != 0 THEN :pinned ELSE pinned END,
					pinnedUpdateTime = CASE WHEN pinnedFromTemplate != 0 THEN :now ELSE pinnedUpdateTime END,
					pinnedFromTemplate = 1,
					alarmTime = CASE WHEN alarmTimeFromTemplate != 0 THEN :alarmTime ELSE alarmTime END,
					alarmTimeUpdateTime = CASE WHEN alarmTimeFromTemplate != 0 THEN :now ELSE alarmTimeUpdateTime END,
					alarmTimeFromTemplate = 1,
					noteId = CASE WHEN noteIdFromTemplate != 0 THEN :noteId ELSE noteId END,
					noteIdUpdateTime = CASE WHEN noteIdFromTemplate != 0 THEN :now ELSE noteIdUpdateTime END,
					noteIdFromTemplate = 1
				WHERE
					scheduleTemplateId = :templateTaskId
			"""
	)
	fun deleteTaskTemplateReferences(
			templateTaskId: Long, title: String, priority: Int, pinned: Boolean,
			alarmTime: LocalTime?, noteId: Long?, now: DateTime = DateTime()
	)
	
	@Query("DELETE FROM tasks")
	fun deleteAll()
}
