package com.example.carrerastaxi.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.carrerastaxi.data.TripEntity
import com.example.carrerastaxi.data.DailyStatsEntity
import com.example.carrerastaxi.data.TrafficEntity

/**
 * Base de datos Room para almacenar las carreras
 */
@Database(
    entities = [
        CarreraEntity::class,
        TripEntity::class,
        DailyStatsEntity::class,
        TrafficEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun carreraDao(): CarreraDao
    abstract fun tripDao(): TripDao
    abstract fun dailyStatsDao(): DailyStatsDao
    abstract fun trafficDao(): TrafficDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Obtiene la instancia única de la base de datos (Singleton)
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "carrerastaxi_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
