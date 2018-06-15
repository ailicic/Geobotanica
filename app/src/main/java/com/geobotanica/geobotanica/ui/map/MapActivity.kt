package com.geobotanica.geobotanica.ui.map

import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock.EXTRA_MESSAGE
import android.support.v7.appcompat.R.id.message
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.ui.BaseActivity
import com.geobotanica.geobotanica.ui.new_record.NewRecordActivity
import kotlinx.android.synthetic.main.activity_map.*

class MapActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            val intent = Intent(this, NewRecordActivity::class.java).apply {
                putExtra(EXTRA_MESSAGE, message)
            }
            startActivity(intent)
        }
    }

}
