package com.geobotanica.geobotanica.ui.dialog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.geobotanica.geobotanica.R
import kotlinx.android.synthetic.main.list_item.view.*

class ListItemAdapter<T: Enum<T>>(
        private val items: List<T>,
        private val drawableArrayResId: Int,
        private val onItemSelected: (T) -> Unit
) : RecyclerView.Adapter<ListItemAdapter<T>.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        val resources = holder.view.context.resources
        holder.text.text = item.toString()

        val drawableArray = resources.obtainTypedArray(drawableArrayResId)
        holder.icon.setImageResource(drawableArray.getResourceId(item.ordinal, -1))
        drawableArray.recycle()

        holder.view.setOnClickListener { onItemSelected(item) }
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val constraintLayout: ConstraintLayout = view.constraintLayout
        val icon: ImageView= view.icon
        val text: TextView = view.text
    }
}
