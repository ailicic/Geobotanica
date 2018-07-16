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
            Photo::class,
            Measurement::class
        ],
        exportSchema = true,
        version = 18
)
@TypeConverters(
        OffsetDateTimeConverter::class,
        MeasurementTypeConverter::class,
        PhotoTypeConverter::class,
        PlantTypeConverter::class )
abstract class GbDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun plantDao(): PlantDao
    abstract fun plantCompositeDao(): PlantCompositeDao
    abstract fun plantLocationDao(): PlantLocationDao
    abstract fun photoDao(): PhotoDao
    abstract fun measurementDao(): MeasurementDao

    companion object {
        @Volatile private var gbDatabase: GbDatabase? = null
        fun getInstance(context: Context): GbDatabase =
            gbDatabase ?: synchronized(this) {
                gbDatabase ?: buildDatabase(context).also{ gbDatabase = it }
            }

        private fun buildDatabase(context: Context) =
                Room.databaseBuilder(
                        context.applicationContext,
                        GbDatabase::class.java, "gb_db")
                        .allowMainThreadQueries()   // TODO: Remove this after going RX
                        .fallbackToDestructiveMigration() // TODO: Implement migrations after schema stagnates
                        .build()
    }
}