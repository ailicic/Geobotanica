package com.geobotanica.geobotanica.ui.newplantname

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.geobotanica.geobotanica.R
import kotlinx.android.synthetic.main.plant_name_list_item.view.*


class PlantNamesRecyclerViewAdapter(
        var items: List<PlantNameSearchService.SearchResult>,
        private val onClick: (PlantNameSearchService.SearchResult) -> Unit
) : RecyclerView.Adapter<PlantNamesRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.plant_name_list_item, parent, false)
        return ViewHolder(view)
    }

    @Suppress("DEPRECATION")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        val plantTypeIcon = when (item.plantNameType) {
            PlantNameSearchService.PlantNameType.VERNACULAR -> R.drawable.hat
            PlantNameSearchService.PlantNameType.SCIENTIFIC -> R.drawable.grad_cap
        }
        holder.plantTypeIcon.setImageDrawable(holder.view.context.resources.getDrawable(plantTypeIcon))
        holder.name.text = item.name
        holder.view.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val plantTypeIcon: ImageView = view.plantTypeIcon
        val name: TextView = view.name
    }
}
