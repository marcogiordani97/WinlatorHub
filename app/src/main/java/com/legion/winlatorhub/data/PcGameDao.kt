package com.legion.winlatorhub.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PcGameDao {
    @Query("SELECT * FROM pc_games ORDER BY name COLLATE NOCASE ASC")
    abstract fun getAllGames(): Flow<List<PcGameEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertGames(games: List<PcGameEntity>)

    @Query("DELETE FROM pc_games")
    abstract suspend fun clearAll()

    /** Sostituzione atomica: la UI non vede mai la libreria vuota a metà scansione. */
    @Transaction
    open suspend fun replaceAll(games: List<PcGameEntity>) {
        clearAll()
        insertGames(games)
    }
}
