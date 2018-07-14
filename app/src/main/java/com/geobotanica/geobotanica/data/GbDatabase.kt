package com.geobotanica.geobotanica.data

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
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
        exportSchema = false,
        version = 18
)
@TypeConverters(
        DateTimeConverter::class,
        MeasurementTypeConverter::class,
        PhotoTypeConverter::class,
        PlantTypeConverter::class )
abstract class GbDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun plantDao(): PlantDao
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