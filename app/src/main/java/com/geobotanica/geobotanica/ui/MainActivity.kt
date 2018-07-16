package com.geobotanica.geobotanica.ui

import android.os.Bundle
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import com.geobotanica.geobotanica.R
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseActivity() { // TODO: Get rid of BaseActivity (single activity app now)
    override val name = this.javaClass.name.substringAfterLast('.')

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val navController = findNavController(R.id.fragment)
        setupActionBarWithNavController(this, navController)
    }
}