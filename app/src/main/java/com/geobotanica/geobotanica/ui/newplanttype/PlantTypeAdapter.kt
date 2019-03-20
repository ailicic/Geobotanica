package com.geobotanica.geobotanica.ui.newplanttype

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Plant
import kotlinx.android.synthetic.main.plant_type_list_item.view.*


class PlantTypeAdapter(
        private val items: List<Plant.Type>,
        private val onClick: (Plant.Type) -> Unit
) : RecyclerView.Adapter<PlantTypeAdapter.ViewHolder>() {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.plant_type_list_item, parent, false)
        return ViewHolder(view)
    }

    @Suppress("DEPRECATION")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        val resources = holder.view.context.resources
        holder.plantTypeText.text = resources.getStringArray(R.array.plant_type_array)[item.ordinal]

        val plantTypeDrawables = resources.obtainTypedArray(R.array.plantTypes)
        holder.plantTypeIcon.setImageResource(plantTypeDrawables.getResourceId(item.ordinal, -1))
        plantTypeDrawables.recycle()

        holder.view.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val constraintLayout: ConstraintLayout = view.constraintLayout
        val plantTypeIcon: ImageView= view.plantTypeIcon
        val plantTypeText: TextView = view.plantTypeText
    }
}
