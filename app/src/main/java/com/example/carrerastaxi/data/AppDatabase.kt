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
        TrafficEntity::class,
        SessionStateEntity::class,
        VehicleProfileEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun carreraDao(): CarreraDao
    abstract fun tripDao(): TripDao
    abstract fun dailyStatsDao(): DailyStatsDao
    abstract fun trafficDao(): TrafficDao
    abstract fun sessionDao(): SessionDao
    abstract fun vehicleProfileDao(): VehicleProfileDao

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
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `session_state` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `journeyActive` INTEGER NOT NULL,
                        `tripState` TEXT NOT NULL,
                        `totalMeters` REAL NOT NULL,
                        `metersWithPassenger` REAL NOT NULL,
                        `metersWithoutPassenger` REAL NOT NULL,
                        `secondsTotal` INTEGER NOT NULL,
                        `secondsWithPassenger` INTEGER NOT NULL,
                        `secondsWithoutPassenger` INTEGER NOT NULL,
                        `secondsStopped` INTEGER NOT NULL,
                        `secondsMoving` INTEGER NOT NULL,
                        `earnings` REAL NOT NULL,
                        `earningsDistance` REAL NOT NULL,
                        `earningsTime` REAL NOT NULL,
                        `tripMeters` REAL NOT NULL,
                        `tripSeconds` INTEGER NOT NULL,
                        `tripEarnings` REAL NOT NULL,
                        `lastLat` REAL NOT NULL,
                        `lastLon` REAL NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `vehicle_profiles` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` TEXT NOT NULL,
                        `baseFare` REAL NOT NULL,
                        `pricePerKm` REAL NOT NULL,
                        `pricePerMin` REAL NOT NULL,
                        `kmPerLiter` REAL NOT NULL,
                        `fuelPrice` REAL NOT NULL,
                        `isActive` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                // insertar perfiles por defecto
                database.execSQL(
                    """
                    INSERT INTO vehicle_profiles (type, baseFare, pricePerKm, pricePerMin, kmPerLiter, fuelPrice, isActive)
                    VALUES ('TAXI', 7.0, 5.0, 1.0, 10.0, 7.0, 1)
                    """
                )
                database.execSQL(
                    """
                    INSERT INTO vehicle_profiles (type, baseFare, pricePerKm, pricePerMin, kmPerLiter, fuelPrice, isActive)
                    VALUES ('MOTOTAXI', 5.0, 3.5, 0.8, 35.0, 7.0, 0)
                    """
                )
            }
        }
    }
}
