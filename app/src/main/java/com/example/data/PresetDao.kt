package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM dc_presets")
    fun getAllPresetsFlow(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM dc_presets")
    suspend fun getAllPresets(): List<PresetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: PresetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresets(presets: List<PresetEntity>)

    @Delete
    suspend fun deletePreset(preset: PresetEntity)
}
