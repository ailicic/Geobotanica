package com.geobotanica.geobotanica.ui.map

import android.arch.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.User
import com.geobotanica.geobotanica.data.repo.UserRepo
import com.geobotanica.geobotanica.ui.BaseActivity
import com.geobotanica.geobotanica.ui.newplanttype.NewPlantTypeActivity
import kotlinx.android.synthetic.main.activity_map.*
import javax.inject.Inject

class MapActivity : BaseActivity() {
    @Inject lateinit var userRepo: UserRepo

    override val name = this.javaClass.name.substringAfterLast('.')
    var userId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        setSupportActionBar(toolbar)

        activityComponent.inject(this)

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
