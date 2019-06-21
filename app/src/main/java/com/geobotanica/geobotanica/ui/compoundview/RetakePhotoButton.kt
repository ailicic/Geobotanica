package com.geobotanica.geobotanica.ui.compoundview

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.app.AlertDialog
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.util.getString
import com.google.android.material.floatingactionbutton.FloatingActionButton


class RetakePhotoButton @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FloatingActionButton(context, attrs, defStyleAttr) {

    lateinit var onRetakePhoto: () -> Unit

    init {
        setOnClickListener { showRetakePhotoDialog() }
    }

    private fun showRetakePhotoDialog() {
        AlertDialog.Builder(context).apply {
            setTitle(getString(R.string.retake_photo))
            setMessage(getString(R.string.delete_photo_confirm))
            setPositiveButton(getString(R.string.yes)) { _, _ -> onRetakePhoto() }
            setNegativeButton(getString(R.string.no)) { dialog, _ -> dialog.dismiss() }
            create()
        }.show()
    }
}