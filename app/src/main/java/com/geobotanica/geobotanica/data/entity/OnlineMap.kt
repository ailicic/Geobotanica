package com.geobotanica.geobotanica.data.entity

import androidx.room.*
import com.geobotanica.geobotanica.network.DownloadStatus
import com.geobotanica.geobotanica.network.DownloadStatus.*
import com.geobotanica.geobotanica.util.capitalizeWords
import com.geobotanica.geobotanica.util.replacePrefix
import com.squareup.moshi.JsonClass


@Entity(tableName = "maps",
// NOTE: Disabled foreign key constraint as asynchronous nature of asset downloads causes crashes
//    foreignKeys = [ForeignKey(
//        entity = OnlineMapFolder::class,
//        parentColumns = ["id"],
//        childColumns = ["parentFolderId"],
//        onDelete = ForeignKey.CASCADE)
//    ],
    indices = [
        Index(value = ["url"]),
        Index(value = ["parentFolderId"]),
        Index(value = ["status"])
    ])
@JsonClass(generateAdapter = true)
data class  OnlineMap(
        val url: String,
        val sizeMb: Long, // TODO: Get actual size in bytes when possible (prob after server is up)
        val timestamp: String,
        val parentFolderId: Long?,

//        @Transient // Exclude from JSON serialization // TODO: REMOVE AFTER SCRAPER IS MOVED TO SERVER
//        @ColumnInfo(name = "status") // Force include in Room DB, despite @Transient // TODO: REMOVE AFTER SCRAPER IS MOVED TO SERVER
        val status: DownloadStatus = NOT_DOWNLOADED,

        @PrimaryKey(autoGenerate = true)
        val id: Long = 0L
) {
    val filename: String
        get() = url.substringAfterLast('/')

    val filenameGzip: String get() = "$filename.gz"

    val isDownloaded: Boolean get() = status == DOWNLOADED
    val isDownloading: Boolean get() = status == DOWNLOADING
    val isNotDownloaded: Boolean get() = status == NOT_DOWNLOADED

    val printName: String
        get() = filename
            .replacePrefix("us-", "US ")
            .removeSuffix(".map")
            .removeSuffix(">") // Present if filename is too long on scraped Mapsforge website
            .replace('-', ' ')
            .capitalizeWords() +
            " ($printSize)"

    val printSize: String
        get() = "$sizeMb MB"
}


@Entity(tableName = "mapFolders",
    foreignKeys = [ForeignKey(
        entity = OnlineMapFolder::class,
        parentColumns = ["id"],
        childColumns = ["parentFolderId"],
        onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        Index(value = ["url"]),
        Index(value = ["parentFolderId"])
    ])
@JsonClass(generateAdapter = true)
data class  OnlineMapFolder(
        val url: String,
        val timestamp: String,
        val parentFolderId: Long?
) {
    @PrimaryKey(autoGenerate = true) var id: Long = 0L

    val name: String
        get() = url
                .removeSuffix("/")
                .substringAfterLast('/')

    val printName: String
        get() = name
                .replace('-', ' ')
                .replacePrefix("us", "US") // FOLDER ONLY
                .capitalizeWords()
}