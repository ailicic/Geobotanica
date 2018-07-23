package com.geobotanica.geobotanica.util

import android.graphics.BitmapFactory
import android.widget.ImageView

object ImageViewExt {
    fun ImageView.setScaledBitmap(photoFilePath: String) {
        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(photoFilePath, bmOptions)

        val bitmapWidth = bmOptions.outWidth
        val bitmapHeight = bmOptions.outHeight
        val scaleFactor = Math.min(bitmapWidth/width, bitmapHeight/height)

        bmOptions.inJustDecodeBounds = false
        bmOptions.inSampleSize = scaleFactor

        setImageBitmap( BitmapFactory.decodeFile(photoFilePath, bmOptions) )
    }
}
