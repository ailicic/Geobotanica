package com.geobotanica.geobotanica.ui.compoundview

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.app.AlertDialog
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.util.getString
import com.google.android.material.floatingactionbutton.FloatingActionButton


class DeletePhotoButton @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FloatingActionButton(context, attrs, defStyleAttr) {

    lateinit var onDeletePhoto: () -> Unit

    init {
        setOnClickListener { showDeletePhotoDialog() }
    }

    private fun showDeletePhotoDialog() {
        AlertDialog.Builder(context).apply {
            setTitle(getString(R.string.delete_photo))
            setMessage(getString(R.string.delete_photo_confirm))
            setPositiveButton(getString(R.string.yes)) { _, _ -> onDeletePhoto() }
            setNegativeButton(getString(R.string.no)) { dialog, _ -> dialog.dismiss() }
            create()
        }.show()
    }
}