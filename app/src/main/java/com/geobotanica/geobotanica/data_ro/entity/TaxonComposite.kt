package com.geobotanica.geobotanica.data_ro.entity

import androidx.room.Embedded
import androidx.room.Relation

class TaxonComposite {
    @Embedded lateinit var taxon: Taxon

    @Relation(parentColumn = "id", entityColumn = "taxonId")
    var commonNames: List<Vernacular> = emptyList()

    override fun toString(): String {
        val sb = StringBuilder("\n$taxon")
        commonNames.forEach { sb.append("\n$it") }
        return sb.toString()
    }
}