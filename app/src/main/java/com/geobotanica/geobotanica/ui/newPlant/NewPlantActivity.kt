package com.geobotanica.geobotanica.ui.newPlant

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.geobotanica.geobotanica.ui.BaseActivity
import com.geobotanica.geobotanica.R

import kotlinx.android.synthetic.main.activity_new_plant.*



class NewPlantActivity : BaseActivity() {
    override val name = this.javaClass.name.substringAfterLast('.')

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_plant)
        setSupportActionBar(toolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_new_record, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
