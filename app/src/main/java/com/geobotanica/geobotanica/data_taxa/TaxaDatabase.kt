package com.geobotanica.geobotanica.data_taxa

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.geobotanica.geobotanica.data_taxa.dao.*
import com.geobotanica.geobotanica.data_taxa.entity.*

const val DEFAULT_RESULT_LIMIT = 50

@Database(
        entities = [
            Taxon::class,
            StarredTaxon::class,
            UsedTaxon::class,
            Vernacular::class,
            StarredVernacular::class,
            UsedVernacular::class
        ],
        version = 1
)
abstract class TaxaDatabase : RoomDatabase() {
    abstract fun taxonDao(): TaxonDao
    abstract fun starredTaxonDao(): StarredTaxonDao
    abstract fun usedTaxonDao(): UsedTaxonDao
    abstract fun vernacularDao(): VernacularDao
    abstract fun starredVernacularDao(): StarredVernacularDao
    abstract fun usedVernacularDao(): UsedVernacularDao

    companion object {
        @Volatile private var plantDatabaseRo: TaxaDatabase? = null
        fun getInstance(appContext: Context): TaxaDatabase =
                plantDatabaseRo ?: synchronized(this) {
                    plantDatabaseRo ?: buildDatabase(appContext).also{ plantDatabaseRo = it }
            }

        private fun buildDatabase(appContext: Context) =
            Room.databaseBuilder(
                appContext,
                TaxaDatabase::class.java, "taxa.db"
            ).build()
    }
}