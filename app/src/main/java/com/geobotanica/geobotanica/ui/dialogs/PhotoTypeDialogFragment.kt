//package com.geobotanica.geobotanica.ui.dialogs
//
//import android.app.Dialog
//import android.content.Context
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ArrayAdapter
//import android.widget.Toast
//import androidx.appcompat.app.AlertDialog
//import androidx.fragment.app.DialogFragment
//import androidx.lifecycle.MutableLiveData
//import com.geobotanica.geobotanica.R
//import com.geobotanica.geobotanica.data.entity.PlantPhoto
//import com.geobotanica.geobotanica.util.Lg
//import kotlinx.android.synthetic.main.photo_type_row.view.*
//
//
//// TODO: Use setItems() instead of setAdapter() ?   (Since data is static)
//
//
//data class IconLabelRowData(val icon: Int, val label: String)
//
//class PhotoTypeAdapter(context: Context, private val objects: List<IconLabelRowData>) : ArrayAdapter<IconLabelRowData>(context, -1, objects) {
//
//    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
//        Lg.d("PhotoTypeAdapter: getView(pos = $position)")
//
//        val rowView: View = if (convertView == null) {
//            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
//            inflater.inflate(com.geobotanica.geobotanica.R.layout.photo_type_row, parent, false)
//        } else
//            convertView
//        rowView.icon.setImageResource(objects[position].icon)
//        rowView.label.text = objects[position].label
//        return rowView
//    }
//}
//
//
//class PhotoTypeDialogFragment : DialogFragment() {
//
//    val photoTypeSelected = MutableLiveData<PlantPhoto.Type>()
//
//    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
//        val items = listOf(
//                IconLabelRowData(R.drawable.photo_type_full, getString(R.string.photo_type_full)),
////                    ImageLabelRowData(R.drawable.photo_type_bud, getString(R.string.photo_type_bud)),
//                IconLabelRowData(R.drawable.photo_type_leaf, getString(R.string.photo_type_leaf)),
//                IconLabelRowData(R.drawable.photo_type_flower, getString(R.string.photo_type_flower)),
//                IconLabelRowData(R.drawable.photo_type_fruit, getString(R.string.photo_type_fruit)),
//                IconLabelRowData(R.drawable.photo_type_trunk, getString(R.string.photo_type_trunk))
//        )
//
//        val adapter = PhotoTypeAdapter(activity!!.applicationContext, items)
//
////            return activity.let {
//        val builder = AlertDialog.Builder(context!!)
//        builder.setTitle("Select Photo Type").setAdapter(adapter)
//        { _, which -> // The 'which' argument contains the index position of the selected item
//            Toast.makeText(activity,
//                    resources.getStringArray(R.array.photo_type)[which], Toast.LENGTH_SHORT).show()
//        }
//        val dialog = builder.create()
////            with(dialog.listView) {
////                @Suppress("DEPRECATION")
////                divider = ColorDrawable(resources.getColor(R.color.colorDarkGrey))
////                dividerHeight = 2
////            }
//        return dialog
//    }
//}