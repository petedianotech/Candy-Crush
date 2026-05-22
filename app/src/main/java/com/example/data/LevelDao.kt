package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LevelDao {
    @Query("SELECT * FROM candy_levels ORDER BY id ASC")
    fun getAllLevels(): Flow<List<CandyLevel>>

    @Query("SELECT * FROM candy_levels WHERE id = :id LIMIT 1")
    suspend fun getLevelById(id: Int): CandyLevel?

    @Update
    suspend fun updateLevel(level: CandyLevel)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLevels(levels: List<CandyLevel>)
}
