package com.geobotanica.geobotanica.ui.map

import android.content.Intent
import android.os.Bundle
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.User
import com.geobotanica.geobotanica.data.repo.UserRepo
import com.geobotanica.geobotanica.ui.BaseActivity
import com.geobotanica.geobotanica.ui.newPlant.NewPlantActivity
import javax.inject.Inject
import kotlinx.android.synthetic.main.activity_map.*

class MapActivity : BaseActivity() {
    @Inject lateinit var userRepo: UserRepo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        setSupportActionBar(toolbar)

        activityComponent.inject(this)

        fab.setOnClickListener { _ ->
            val intent = Intent(this, NewPlantActivity::class.java)
                    .putExtra(getString(R.string.extra_user_id), getGuestUserId())
            startActivity(intent)
        }
    }

    private fun getGuestUserId(): Long {
        val guestUserNickname: String = "Guest"
        return if (userRepo.contains(guestUserNickname))
            userRepo.getByNickname(guestUserNickname)[0].id
        else
            userRepo.insert(User(guestUserNickname))
    }
}
