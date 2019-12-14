package com.geobotanica.geobotanica.data_taxa.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.geobotanica.geobotanica.util.GbTime
import org.threeten.bp.Instant

@Entity(tableName = "tags",
        foreignKeys = [
            ForeignKey(
                    entity = Vernacular::class,
                    parentColumns = ["id"],
                    childColumns = ["vernacularId"],
                    onDelete = ForeignKey.CASCADE),
            ForeignKey(
                    entity = Taxon::class,
                    parentColumns = ["id"],
                    childColumns = ["taxonId"],
                    onDelete = ForeignKey.CASCADE)
        ],
        indices = [
            Index(value = ["vernacularId"]),
            Index(value = ["taxonId"]),
            Index(value = ["tag", "vernacularId"]),
            Index(value = ["tag", "taxonId"]),
            Index(value = ["timestamp"])
        ])
data class Tag(
    val tag: Int, // = PlantNameTag.ordinal (no ORed bitflags in db)
    val vernacularId: Long? = null,
    val taxonId: Long? = null,
    val timestamp: Instant = GbTime.now()
) {
    @PrimaryKey(autoGenerate = true) var id: Long = 0L
}

enum class PlantNameTag(val flag: Int) {
    COMMON(     0b0000_0001),
    SCIENTIFIC( 0b0000_0010),
    STARRED(    0b0000_0100),
    USED(       0b0000_1000)
}