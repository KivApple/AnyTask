package id.pineapple.anytask

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import android.arch.persistence.room.migration.Migration
import android.content.Context
import id.pineapple.anytask.notes.Note
import id.pineapple.anytask.notes.NoteDao
import id.pineapple.anytask.tasks.Task
import id.pineapple.anytask.tasks.TaskDao
import id.pineapple.anytask.tasks.TaskScheduleOptions
import org.joda.time.DateTime

@Database(
		entities = [Task::class, Note::class],
		version = 6
)
@TypeConverters(
		DateConverter::class, TaskScheduleOptions.ModeConverter::class,
		Note.TypeConverter::class
)
abstract class AppDatabase : RoomDatabase() {
	abstract fun taskDao(): TaskDao
	
	abstract fun noteDao(): NoteDao
	
	companion object {
		lateinit var instance: AppDatabase
			private set
		
		fun init(context: Context) {
			instance = Room
					//.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
					.databaseBuilder(context, AppDatabase::class.java, "db.sqlite")
					.addMigrations(
							object : Migration(1, 2) {
								override fun migrate(db: SupportSQLiteDatabase) {
									db.execSQL("ALTER TABLE tasks ADD titleFromTemplate INTEGER NOT NULL DEFAULT 1")
								}
							},
							object : Migration(2, 3) {
								override fun migrate(db: SupportSQLiteDatabase) {
									db.execSQL("DROP TABLE cloned_task_markers")
								}
							},
							object : Migration(3, 4) {
								override fun migrate(db: SupportSQLiteDatabase) {
									db.execSQL("DROP INDEX index_tasks_scheduleTemplateId_completed")
									db.execSQL("DROP INDEX index_tasks_scheduleTemplateId_pinnedFromTemplate")
									db.execSQL("DROP INDEX index_tasks_scheduleTemplateId_alarmTimeFromTemplate")
									db.execSQL("CREATE INDEX `index_tasks_originDate_scheduleTemplateId` ON `tasks` (`originDate`, `scheduleTemplateId`)")
								}
							},
							object : Migration(4, 5) {
								override fun migrate(db: SupportSQLiteDatabase) {
									db.execSQL("ALTER TABLE notes RENAME TO notes_old")
									db.execSQL("CREATE TABLE notes (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `parentId` INTEGER, `title` TEXT NOT NULL, `type` INTEGER NOT NULL, `text` TEXT NOT NULL, `position` INTEGER NOT NULL, `titleUpdateTime` INTEGER NOT NULL, `contentUpdateTime` INTEGER NOT NULL, `positionUpdateTime` INTEGER NOT NULL, FOREIGN KEY(`parentId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
									db.execSQL("INSERT INTO notes (id, title, type, text, position, titleUpdateTime, contentUpdateTime, positionUpdateTime) SELECT id, title, type, text, position, titleUpdateTime, contentUpdateTime, positionUpdateTime FROM notes_old")
									db.execSQL("DROP TABLE notes_old")
									db.execSQL("CREATE INDEX `index_notes_parentId_position` ON `notes` (`parentId`, `position`)")
								}
							},
							object : Migration(5, 6) {
								override fun migrate(db: SupportSQLiteDatabase) {
									db.execSQL("ALTER TABLE tasks RENAME TO tasks_old")
									db.execSQL("CREATE TABLE `tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `title` TEXT NOT NULL, `titleFromTemplate` INTEGER NOT NULL, `completed` INTEGER NOT NULL, `position` INTEGER NOT NULL, `savedPosition` INTEGER NOT NULL, `priority` INTEGER NOT NULL, `priorityFromTemplate` INTEGER NOT NULL, `scheduleTemplateId` INTEGER, `originDate` INTEGER NOT NULL, `pinned` INTEGER NOT NULL, `pinnedFromTemplate` INTEGER NOT NULL, `alarmTime` INTEGER, `alarmTimeFromTemplate` INTEGER NOT NULL, `noteId` INTEGER, `noteIdFromTemplate` INTEGER NOT NULL, `titleUpdateTime` INTEGER NOT NULL, `completedUpdateTime` INTEGER NOT NULL, `positionUpdateTime` INTEGER NOT NULL, `scheduleOptionsUpdateTime` INTEGER NOT NULL, `pinnedUpdateTime` INTEGER NOT NULL, `alarmTimeUpdateTime` INTEGER NOT NULL, `noteIdUpdateTime` INTEGER NOT NULL, `schedule_startDate` INTEGER NOT NULL, `schedule_stopDate` INTEGER NOT NULL, `schedule_mode` INTEGER NOT NULL, `schedule_interval` INTEGER NOT NULL, `schedule_weekDaysMask` INTEGER NOT NULL, FOREIGN KEY(`scheduleTemplateId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`noteId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")
									db.execSQL("INSERT INTO tasks (id, title, titleFromTemplate, completed, position, savedPosition, priority, priorityFromTemplate, schedule_startDate, schedule_stopDate, schedule_mode, schedule_interval, schedule_weekDaysMask, scheduleTemplateId, originDate, pinned, pinnedFromTemplate, alarmTime, alarmTimeFromTemplate, titleUpdateTime, completedUpdateTime, positionUpdateTime, scheduleOptionsUpdateTime, pinnedUpdateTime, alarmTimeUpdateTime, noteId, noteIdFromTemplate, noteIdUpdateTime) SELECT id, title, titleFromTemplate, completed, position, savedPosition, priority, priorityFromTemplate, schedule_startDate, schedule_stopDate, schedule_mode, schedule_interval, schedule_weekDaysMask, scheduleTemplateId, originDate, pinned, pinnedFromTemplate, alarmTime, alarmTimeFromTemplate, titleUpdateTime, completedUpdateTime, positionUpdateTime, scheduleOptionsUpdateTime, pinnedUpdateTime, alarmTimeUpdateTime, NULL as noteId, 1 as noteIdFromTemplate, ? as noteIdUpdateTime FROM tasks_old", arrayOf(DateTime().millis))
									db.execSQL("DROP TABLE tasks_old")
									db.execSQL("CREATE INDEX `index_tasks_schedule_startDate_schedule_stopDate` ON `tasks` (`schedule_startDate`, `schedule_stopDate`)")
									db.execSQL("CREATE INDEX `index_tasks_originDate_scheduleTemplateId` ON `tasks` (`originDate`, `scheduleTemplateId`)")
									db.execSQL("CREATE INDEX `index_tasks_scheduleTemplateId_schedule_startDate` ON `tasks` (`scheduleTemplateId`, `schedule_startDate`)")
									db.execSQL("CREATE INDEX `index_tasks_noteId` ON `tasks` (`noteId`)")
								}
							}
					)
					.build()
		}
	}
}
