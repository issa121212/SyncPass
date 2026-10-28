package com.rojas.syncpass.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecintoDao {
    @Query("SELECT * FROM recintos_locales ORDER BY nombre COLLATE NOCASE")
    fun observar(): Flow<List<RecintoEntity>>
    @Query("SELECT * FROM recintos_locales WHERE id = :id")
    suspend fun obtener(id: Long): RecintoEntity?
    @Insert suspend fun crear(item: RecintoEntity): Long
    @Update suspend fun editar(item: RecintoEntity): Int
    @Query("DELETE FROM recintos_locales WHERE id = :id") suspend fun eliminar(id: Long): Int
}
