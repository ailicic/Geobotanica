package com.geobotanica.geobotanica.ui.new_plant_name

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Location
import com.geobotanica.geobotanica.ui.BaseActivity
import com.geobotanica.geobotanica.ui.add_measurement.AddMeasurementsActivity
import com.geobotanica.geobotanica.util.Lg
import kotlinx.android.synthetic.main.activity_new_plant_name.*
import kotlinx.android.synthetic.main.gps_compound_view.view.*

// TODO: Consider using Fragment in activity for Snackbar
class NewPlantNameActivity : BaseActivity() {
    override val name = this.javaClass.name.substringAfterLast('.')
    private var userId = 0L
    private var plantType = 0
    private var photoFilePath: String = ""
    private var plantLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_plant_name)

        userId = intent.getLongExtra(getString(R.string.extra_user_id), -1L)
        plantType = intent.getIntExtra(getString(R.string.extra_plant_type), -1)
        photoFilePath = intent.getStringExtra(getString(R.string.extra_plant_photo_path))
        plantLocation = intent.getSerializableExtra(getString(R.string.extra_location)) as? Location
        Lg.d("Intent extras: userId=$userId, plantType=$plantType, photoFilePath=$photoFilePath, plantLocation=$plantLocation")

        plantLocation?.let { gps.setLocation(it) }
    }

    override fun onResume() {
        super.onResume()
        val viewTreeObserver = plantPhoto.viewTreeObserver
        if (viewTreeObserver.isAlive) {
            viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                @Suppress("DEPRECATION")
                override fun onGlobalLayout() {
                    plantPhoto.viewTreeObserver.removeGlobalOnLayoutListener(this)
                    plantPhoto.setImageBitmap(getScaledBitmap())
                }
            })
        }
        fab.setOnClickListener(::onFabPressed)
    }



    // TODO: Push validation into the repo?
    private fun onFabPressed(view: View) {
        Lg.d("NewPlantFragment: onSaveButtonPressed()")

        val commonName: String = commonNameEditText.editText!!.text.toString().trim()
        val latinName: String = latinNameEditText.editText!!.text.toString().trim()

        if (commonName.isEmpty() && latinName.isEmpty()) {
            Snackbar.make(view, "Provide a plant name", Snackbar.LENGTH_LONG).setAction("Action", null).show()
            return
        }

        val intent = Intent(this, AddMeasurementsActivity::class.java)
                .putExtra(getString(R.string.extra_user_id), userId)
                .putExtra(getString(R.string.extra_plant_type), plantType)
                .putExtra(getString(R.string.extra_plant_photo_path), photoFilePath)
                .putExtra(getString(R.string.extra_plant_common_name), commonName)
                .putExtra(getString(R.string.extra_plant_latin_name), latinName)
        if (gps.gpsSwitch.isChecked)
            intent.putExtra(getString(R.string.extra_location), gps.currentLocation)
        startActivity(intent)
        finish()
    }

    private fun getScaledBitmap(): Bitmap {
        val imageViewWidth = plantPhoto.width
        val imageViewHeight = plantPhoto.height

        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(photoFilePath, bmOptions)
        val bitmapWidth = bmOptions.outWidth
        val bitmapHeight = bmOptions.outHeight
        val scaleFactor = Math.min(bitmapWidth/imageViewWidth, bitmapHeight/imageViewHeight)

        bmOptions.inJustDecodeBounds = false
        bmOptions.inSampleSize = scaleFactor

        return BitmapFactory.decodeFile(photoFilePath, bmOptions)
    }

}
