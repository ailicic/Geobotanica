package com.geobotanica.geobotanica.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.geobotanica.geobotanica.data.dao.*
import com.geobotanica.geobotanica.data.entity.*

@Database(
        entities = [
            OnlineAsset::class,
            OnlineMap::class,
            OnlineMapFolder::class,
            User::class,
            Plant::class,
            PlantLocation::class,
            PlantPhoto::class,
            PlantMeasurement::class,
            Geolocation::class
        ],
        exportSchema = true,
        version = 1
)
@TypeConverters(
        PlantTypeConverter::class,
        PhotoTypeConverter::class,
        MeasurementTypeConverter::class,
        DateTimeConverter::class,
        DownloadStatusConverter::class)
abstract class GbDatabase : RoomDatabase() {
    abstract fun assetDao(): OnlineAssetDao
    abstract fun mapDao(): OnlineMapDao
    abstract fun mapFolderDao(): OnlineMapFolderDao
    abstract fun userDao(): UserDao
    abstract fun plantDao(): PlantDao
    abstract fun plantCompositeDao(): PlantCompositeDao
    abstract fun plantLocationDao(): PlantLocationDao
    abstract fun photoDao(): PlantPhotoDao
    abstract fun measurementDao(): PlantMeasurementDao
    abstract fun geolocationDao(): GeolocationDao

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