package com.legion.winlatorhub.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PcGameDao {
    @Query("SELECT * FROM pc_games ORDER BY name ASC")
    fun getAllGames(): Flow<List<PcGameEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGames(games: List<PcGameEntity>)

    @Query("DELETE FROM pc_games")
    suspend fun clearAll()
}
