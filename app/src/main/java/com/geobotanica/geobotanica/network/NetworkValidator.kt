package com.geobotanica.geobotanica.network

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.ui.MainActivity
import com.geobotanica.geobotanica.ui.dialog.WarningDialog
import com.geobotanica.geobotanica.util.get
import com.google.android.material.snackbar.Snackbar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.suspendCoroutine


@Singleton
class NetworkValidator @Inject constructor(
        private val activity: MainActivity,
        private val defaultSharedPrefs: SharedPreferences
) {
    private val resources = activity.resources
    private val rootView: View = activity.window.decorView.findViewById(android.R.id.content)
    private val connectivityManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val sharedPrefsAllowMeteredNetwork = "allowMeteredNetwork"

    suspend fun isValid(): Boolean {
        if (! isNetworkConnected()) {
            Snackbar.make(
                    rootView,
                    resources.getString(R.string.internet_unavailable),
                    Snackbar.LENGTH_SHORT).show()
            return false
        } else if (isNetworkMetered() && ! isMeteredNetworkAllowed()) {
            return suspendCoroutine { continuation ->
                WarningDialog(
                        R.string.metered_network,
                        R.string.metered_network_confirm,
                        sharedPrefsAllowMeteredNetwork,
                        continuation
                ).show((activity as FragmentActivity).supportFragmentManager, "tag")
            }
        } else
            return true
    }

    fun isNetworkMetered(): Boolean = connectivityManager.isActiveNetworkMetered

    private fun isNetworkConnected(): Boolean = connectivityManager.activeNetworkInfo?.isConnected ?: false

    private fun isMeteredNetworkAllowed(): Boolean =
        defaultSharedPrefs.get(sharedPrefsAllowMeteredNetwork, false)
}
