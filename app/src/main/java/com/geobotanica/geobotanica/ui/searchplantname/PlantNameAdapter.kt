package com.geobotanica.geobotanica.ui.searchplantname

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data_taxa.entity.PlantNameTag.*
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.SearchResult
import kotlinx.android.synthetic.main.plant_name_list_item.view.*


class PlantNameAdapter(
        private val onClickItem: (Int, SearchResult) -> Unit,
        private val onClickStar: (SearchResult) -> Unit,
        private val context: Context
) : RecyclerView.Adapter<PlantNameAdapter.ViewHolder>() {

    var items: List<SearchResult> = emptyList()

    var selectedPosition:Int = RecyclerView.NO_POSITION
    var isSelectedShown: Boolean = false // If true, last selected item will have darker background

    var showStars: Boolean = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.plant_name_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        val resources = holder.view.context.resources
        holder.plantName.text = item.plantName

        var typeface = Typeface.NORMAL
        if (item.hasTag(SCIENTIFIC))
            typeface = Typeface.ITALIC
        holder.plantName.setTypeface(null, typeface)

        val plantNameIcon = when {
            item.hasTag(COMMON) -> R.drawable.common_name
            item.hasTag(SCIENTIFIC) -> R.drawable.scientific_name
            else -> throw IllegalArgumentException("Must specify either COMMON or SCIENTIFIC tag") // TODO: Handle this differently
        }
        holder.plantNameIcon.setImageDrawable(resources.getDrawable(plantNameIcon, context.theme))

        holder.plantNameIcon.clearColorFilter()
        @Suppress("DEPRECATION")
        if (plantNameIcon == R.drawable.common_name)
//            holder.plantNameIcon.setColorFilter(resources.getColor(R.color.colorBrown, context.theme)) // Requires min API 23
            holder.plantNameIcon.setColorFilter(resources.getColor(R.color.colorBrown))

        holder.plantTypeIcon.isVisible = false
        holder.altPlantTypeIcon.isVisible = false
        val plantTypes = Plant.Type.flagsToList(item.plantTypes)
        val plantTypeCount = plantTypes.size
        if (plantTypeCount == 1 || plantTypeCount == 2) {
            val plantTypeDrawables = resources.obtainTypedArray(R.array.plant_type_drawable_array)
            holder.plantTypeIcon.run {
                setImageResource(plantTypeDrawables.getResourceId(plantTypes[0].ordinal, -1))
                isVisible = true
            }

            if (plantTypeCount == 2) {
                holder.altPlantTypeIcon.run {
                    setImageResource(plantTypeDrawables.getResourceId(plantTypes[1].ordinal, -1))
                    isVisible = true
                }
            }
            plantTypeDrawables.recycle()
        }

        holder.historyIcon.isVisible = item.hasTag(USED)
        holder.starredIcon.isVisible = showStars

        if (showStars) {
            val star = resources.getDrawable(R.drawable.ic_star, context.theme)
            val starBorder = resources.getDrawable(R.drawable.ic_star_border, context.theme)
            val starIcon = if (item.hasTag(STARRED)) star else starBorder
            holder.starredIcon.setImageDrawable(starIcon)

            holder.starredIcon.setOnClickListener {
                item.toggleTag(STARRED)
                val drawable = if (item.hasTag(STARRED)) star else starBorder
                holder.starredIcon.setImageDrawable(drawable)
                onClickStar(item)
            }
        }

        @Suppress("DEPRECATION")
        if (isSelectedShown) {
            if (position == selectedPosition)
                holder.constraintLayout.setBackgroundColor(resources.getColor(R.color.colorLightGrey))
            else
                holder.constraintLayout.setBackgroundColor(resources.getColor(R.color.colorWhite))
        }

        holder.view.setOnClickListener { onClickItem(position, item) }
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val constraintLayout: ConstraintLayout = view.constraintLayout
        val plantNameIcon: ImageView = view.plantNameIcon
        val plantName: TextView = view.plantName
        val plantTypeIcon: ImageView= view.icon
        val altPlantTypeIcon: ImageView= view.altPlantTypeIcon
        val historyIcon: ImageView= view.historyIcon
        val starredIcon: ImageView= view.starIcon
    }
}
