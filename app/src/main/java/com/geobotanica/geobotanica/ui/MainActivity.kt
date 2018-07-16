package com.geobotanica.geobotanica.ui

import androidx.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.User
import com.geobotanica.geobotanica.data.repo.UserRepo
import com.geobotanica.geobotanica.ui.newplanttype.NewPlantTypeActivity
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

class MainActivity : BaseActivity() { // TODO: Get rid of BaseActivity (single activity app now)
    @Inject lateinit var userRepo: UserRepo

    override val name = this.javaClass.name.substringAfterLast('.')
    var userId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        activityComponent.inject(this)

        val navController = findNavController(R.id.fragment)
        setupActionBarWithNavController(this, navController)

        createGuestUser()
        fab.setOnClickListener { _ ->
            val intent = Intent(this, NewPlantTypeActivity::class.java)
                    .putExtra(getString(R.string.extra_user_id), userId)
            startActivity(intent)
        }
    }

    private fun createGuestUser() {
        userRepo.get(1).observe(this, Observer<User> {
            userId = if (it != null) {
                it.id
            } else {
                userRepo.insert(User("Guest"))
            }
        })
    }
}
