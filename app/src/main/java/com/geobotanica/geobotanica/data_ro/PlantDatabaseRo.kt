package com.geobotanica.geobotanica.data_ro

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.geobotanica.geobotanica.data_ro.dao.TaxonDao
import com.geobotanica.geobotanica.data_ro.dao.VernacularDao
import com.geobotanica.geobotanica.data_ro.entity.Taxon
import com.geobotanica.geobotanica.data_ro.entity.Vernacular

@Database(
        entities = [
            Taxon::class,
            Vernacular::class
        ],
        version = 1
)
abstract class PlantDatabaseRo : RoomDatabase() {
    abstract fun taxonDao(): TaxonDao
    abstract fun vernacularDao(): VernacularDao

    companion object {
        @Volatile private var plantDatabaseRo: PlantDatabaseRo? = null
        fun getInstance(appContext: Context): PlantDatabaseRo =
                plantDatabaseRo ?: synchronized(this) {
                    plantDatabaseRo ?: buildDatabase(appContext).also{ plantDatabaseRo = it }
            }

        private fun buildDatabase(appContext: Context) =
                Room.databaseBuilder(
                        appContext,
                        PlantDatabaseRo::class.java, "CoL.sqlite")
                        .build()
    }
}