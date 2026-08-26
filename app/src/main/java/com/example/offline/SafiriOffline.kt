package com.example.offline

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
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "gps_points")
data class GpsPointEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val latitude: Double,
    val longitude: Double,
    val speedKph: Double,
    val accuracyMeters: Double,
    val capturedAtEpochMs: Long,
    val synced: Boolean = false
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey val id: String,
    val type: String,
    val payload: String,
    val createdAtEpochMs: Long,
    val attempts: Int = 0,
    val lastError: String? = null
)

@Entity(tableName = "trip_cache")
data class TripCacheEntity(
    @PrimaryKey val tripId: String,
    val routeId: String,
    val busPlate: String,
    val status: String,
    val cachedAtEpochMs: Long
)

@Dao
interface SafiriOfflineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGpsPoint(point: GpsPointEntity)

    @Query("SELECT * FROM gps_points WHERE synced = 0 ORDER BY capturedAtEpochMs ASC")
    suspend fun pendingGpsPoints(): List<GpsPointEntity>

    @Query("UPDATE gps_points SET synced = 1 WHERE id IN (:ids)")
    suspend fun markGpsSynced(ids: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: SyncQueueEntity)

    @Query("SELECT * FROM sync_queue ORDER BY createdAtEpochMs ASC")
    fun observeQueue(): Flow<List<SyncQueueEntity>>

    @Query("SELECT * FROM sync_queue ORDER BY createdAtEpochMs ASC LIMIT :limit")
    suspend fun nextQueueItems(limit: Int): List<SyncQueueEntity>

    @Update
    suspend fun updateQueueItem(item: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun removeQueueItem(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun cacheTrip(trip: TripCacheEntity)

    @Query("SELECT * FROM trip_cache ORDER BY cachedAtEpochMs DESC")
    fun observeTrips(): Flow<List<TripCacheEntity>>
}

@Database(
    entities = [GpsPointEntity::class, SyncQueueEntity::class, TripCacheEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SafiriDatabase : RoomDatabase() {
    abstract fun safiriOfflineDao(): SafiriOfflineDao

    companion object {
        @Volatile private var INSTANCE: SafiriDatabase? = null

        fun get(context: Context): SafiriDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SafiriDatabase::class.java,
                    "safiri_offline.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
