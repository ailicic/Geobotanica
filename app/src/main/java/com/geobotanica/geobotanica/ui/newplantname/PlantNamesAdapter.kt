package com.geobotanica.geobotanica.ui.newplantname

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.PlantNameTag.*
import kotlinx.android.synthetic.main.plant_name_list_item.view.*


class PlantNamesAdapter(
        var items: List<PlantNameSearchService.SearchResult>,
        private val onClick: (PlantNameSearchService.SearchResult) -> Unit,
        private val onClickStar: (PlantNameSearchService.SearchResult) -> Unit
) : RecyclerView.Adapter<PlantNamesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.plant_name_list_item, parent, false)
        return ViewHolder(view)
    }

    @Suppress("DEPRECATION")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        val resources = holder.view.context.resources
        holder.plantName.text = item.plantName

        val plantTypeIcon = when {
            item.hasTag(COMMON) -> R.drawable.hat
            item.hasTag(SCIENTIFIC) -> R.drawable.grad_cap
            else -> throw IllegalArgumentException("Must specify either COMMON or SCIENTIFIC tag") // TODO: Handle this differently
        }
        holder.plantTypeIcon.setImageDrawable(resources.getDrawable(plantTypeIcon))

        holder.usedIcon.isVisible = item.hasTag(USED)

        val star = resources.getDrawable(R.drawable.ic_star)
        val starBorder = resources.getDrawable(R.drawable.ic_star_border)
        val starIcon = if (item.hasTag(STARRED)) star else starBorder
        holder.starredIcon.setImageDrawable(starIcon)

        holder.view.setOnClickListener { onClick(item) }
        holder.starredIcon.setOnClickListener {
            item.toggleTag(STARRED)
            val drawable = if (item.hasTag(STARRED)) star else starBorder
            holder.starredIcon.setImageDrawable(drawable)
            onClickStar(item)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val plantTypeIcon: ImageView = view.plantTypeIcon
        val plantName: TextView = view.plantName
        val usedIcon: ImageView= view.usedIcon
        val starredIcon: ImageView= view.starIcon
    }
}
