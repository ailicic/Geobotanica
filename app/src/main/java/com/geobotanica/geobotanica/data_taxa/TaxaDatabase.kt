package com.geobotanica.geobotanica.data_taxa

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.geobotanica.geobotanica.data.entity.OffsetDateTimeConverter
import com.geobotanica.geobotanica.data_taxa.dao.TagDao
import com.geobotanica.geobotanica.data_taxa.dao.TaxonDao
import com.geobotanica.geobotanica.data_taxa.dao.VernacularDao
import com.geobotanica.geobotanica.data_taxa.entity.Tag
import com.geobotanica.geobotanica.data_taxa.entity.Taxon
import com.geobotanica.geobotanica.data_taxa.entity.Vernacular

const val DEFAULT_RESULT_LIMIT = 50

@Database(
        entities = [
            Taxon::class,
            Vernacular::class,
            Tag::class
        ],
        version = 1
)
@TypeConverters(OffsetDateTimeConverter::class )
abstract class TaxaDatabase : RoomDatabase() {
    abstract fun taxonDao(): TaxonDao
    abstract fun vernacularDao(): VernacularDao
    abstract fun tagDao(): TagDao

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