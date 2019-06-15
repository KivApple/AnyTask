package id.pineapple.anytask.notes

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*

@Dao
interface NoteDao {
	@Query("SELECT * FROM notes ORDER BY parentId ASC, position ASC")
	fun findAllSync(): List<Note>
	
	@Query("SELECT * FROM notes WHERE id = :id")
	fun findById(id: Long): LiveData<Note>
	
	@Query("SELECT * FROM notes WHERE parentId IS :parentId ORDER BY position ASC")
	fun findByParentId(parentId: Long?): LiveData<List<Note>>
	
	@Insert
	fun insert(vararg notes: Note): List<Long>
	
	@Update
	fun update(vararg notes: Note)
	
	@Delete
	fun delete(vararg notes: Note)
	
	@Query("DELETE FROM notes")
	fun deleteAll()
}
