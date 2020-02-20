package com.geobotanica.geobotanica.data_taxa

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.geobotanica.geobotanica.data.entity.DateTimeConverter
import com.geobotanica.geobotanica.data_taxa.dao.TagDao
import com.geobotanica.geobotanica.data_taxa.dao.TaxonDao
import com.geobotanica.geobotanica.data_taxa.dao.TypeDao
import com.geobotanica.geobotanica.data_taxa.dao.VernacularDao
import com.geobotanica.geobotanica.data_taxa.entity.*

const val DEFAULT_RESULT_LIMIT = 50

@Database(
        entities = [
            Taxon::class,
            Vernacular::class,
            Tag::class,
            TaxonType::class,
            VernacularType::class
        ],
        version = 1
)
@TypeConverters(DateTimeConverter::class )
abstract class TaxaDatabase : RoomDatabase() {
    abstract fun taxonDao(): TaxonDao
    abstract fun vernacularDao(): VernacularDao
    abstract fun tagDao(): TagDao
    abstract fun typeDao(): TypeDao

    companion object {
        @Volatile private var taxaDatabase: TaxaDatabase? = null

        fun getInstance(appContext: Context): TaxaDatabase =
                taxaDatabase ?: synchronized(this) {
                    taxaDatabase ?: buildDatabase(appContext).also { taxaDatabase = it }
            }

        private fun buildDatabase(appContext: Context) =
            Room.databaseBuilder(
                appContext,
                TaxaDatabase::class.java, "taxa.db"
            ).build()
    }
}