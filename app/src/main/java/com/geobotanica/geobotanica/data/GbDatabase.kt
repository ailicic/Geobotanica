package com.geobotanica.geobotanica.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.geobotanica.geobotanica.data.dao.*
import com.geobotanica.geobotanica.data.entity.*

@Database(
        entities = [
            User::class,
            Plant::class,
            PlantLocation::class,
            PlantPhoto::class,
            PlantMeasurement::class
        ],
        exportSchema = true,
        version = 2
)
@TypeConverters(
        PlantTypeConverter::class,
        PhotoTypeConverter::class,
        MeasurementTypeConverter::class,
        OffsetDateTimeConverter::class )
abstract class GbDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun plantDao(): PlantDao
    abstract fun plantCompositeDao(): PlantCompositeDao
    abstract fun plantLocationDao(): PlantLocationDao
    abstract fun photoDao(): PlantPhotoDao
    abstract fun measurementDao(): PlantMeasurementDao

    companion object {
        @Volatile private var gbDatabase: GbDatabase? = null
        fun getInstance(context: Context): GbDatabase =
            gbDatabase ?: synchronized(this) {
                gbDatabase ?: buildDatabase(context).also{ gbDatabase = it }
            }

        private fun buildDatabase(appContext: Context) =
            Room.databaseBuilder(
                appContext,
                GbDatabase::class.java, "gb.db"
            ).fallbackToDestructiveMigration() // TODO: Implement migrations after schema stagnates
            .build()
    }
}