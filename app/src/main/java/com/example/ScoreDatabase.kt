package com.example

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "completion_times")
data class CompletionTime(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val difficulty: String,
    val timeSeconds: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface CompletionTimeDao {
    @Query("SELECT * FROM completion_times WHERE difficulty = :difficulty ORDER BY timeSeconds ASC, timestamp ASC LIMIT 5")
    fun getTopTimesForDifficulty(difficulty: String): Flow<List<CompletionTime>>

    @Query("SELECT * FROM completion_times ORDER BY timeSeconds ASC, timestamp ASC")
    fun getAllTimes(): Flow<List<CompletionTime>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTime(completionTime: CompletionTime)

    @Query("DELETE FROM completion_times")
    suspend fun clearAll()
}

@Database(entities = [CompletionTime::class], version = 1, exportSchema = false)
abstract class ScoreDatabase : RoomDatabase() {
    abstract fun completionTimeDao(): CompletionTimeDao

    companion object {
        @Volatile
        private var INSTANCE: ScoreDatabase? = null

        fun getDatabase(context: Context): ScoreDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScoreDatabase::class.java,
                    "score_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class ScoreRepository(private val completionTimeDao: CompletionTimeDao) {
    fun getTopTimesForDifficulty(difficulty: String): Flow<List<CompletionTime>> =
        completionTimeDao.getTopTimesForDifficulty(difficulty)

    val allTimes: Flow<List<CompletionTime>> = completionTimeDao.getAllTimes()

    suspend fun insert(completionTime: CompletionTime) {
        completionTimeDao.insertTime(completionTime)
    }

    suspend fun clear() {
        completionTimeDao.clearAll()
    }
}
