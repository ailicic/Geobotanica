package com.geobotanica.geobotanica.data

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import com.geobotanica.geobotanica.data.dao.LocationDao
import com.geobotanica.geobotanica.data.dao.PhotoDao
import com.geobotanica.geobotanica.data.dao.PlantDao
import com.geobotanica.geobotanica.data.dao.UserDao
import com.geobotanica.geobotanica.data.entity.Location
import com.geobotanica.geobotanica.data.entity.Photo
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data.entity.User

@Database(
        entities = [
            User::class,
            Plant::class,
            Location::class,
            Photo::class
        ],
        version = 1
)
abstract class GbDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun plantDao(): PlantDao
    abstract fun locationDao(): LocationDao
    abstract fun photoDao(): PhotoDao

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
                        .build()
    }
}