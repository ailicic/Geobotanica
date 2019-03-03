package com.geobotanica.geobotanica.ui.newplantname

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.ui.newplantname.PlantNameSearchService.PlantNameType.SCIENTIFIC
import com.geobotanica.geobotanica.ui.newplantname.PlantNameSearchService.PlantNameType.VERNACULAR
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
        val star = resources.getDrawable(R.drawable.ic_star)
        val starBorder = resources.getDrawable(R.drawable.ic_star_border)
        val starDrawable = if (item.isStarred) star else starBorder

        val plantTypeIcon = when (item.plantNameType) {
            VERNACULAR -> R.drawable.hat
            SCIENTIFIC -> R.drawable.grad_cap
        }

        holder.plantTypeIcon.setImageDrawable(resources.getDrawable(plantTypeIcon))
        holder.name.text = item.name
        holder.star.setImageDrawable(starDrawable)

        holder.view.setOnClickListener { onClick(item) }
        holder.star.setOnClickListener {
            item.isStarred = !item.isStarred
            val drawable = if (item.isStarred) star else starBorder
            holder.star.setImageDrawable(drawable)
            onClickStar(item)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val plantTypeIcon: ImageView = view.plantTypeIcon
        val name: TextView = view.name
        val star: ImageView= view.star
    }
}
