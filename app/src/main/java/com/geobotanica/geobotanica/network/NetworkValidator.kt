package com.geobotanica.geobotanica.network

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.net.ConnectivityManager
import android.preference.PreferenceManager
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.ui.MainActivity
import com.geobotanica.geobotanica.ui.dialog.WarningDialog
import com.geobotanica.geobotanica.util.get
import com.geobotanica.geobotanica.util.put
import com.google.android.material.snackbar.Snackbar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkValidator @Inject constructor(
        private val activity: MainActivity,
        private val defaultSharedPrefs: SharedPreferences
) {
    private val resources = activity.resources
    private val rootView: View = activity.window.decorView.findViewById(android.R.id.content)
    private val connectivityManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val sharedPrefsAllowMeteredNetwork = "allowMeteredNetwork"

    fun runIfValid(onValid: () -> Unit) {
        if (! isNetworkConnected()) {
            Snackbar.make(
                    rootView,
                    resources.getString(R.string.internet_unavailable),
                    Snackbar.LENGTH_SHORT).show()
        } else if (isNetworkMetered() && ! isMeteredNetworkAllowed()) {
            WarningDialog(
                    R.string.metered_network,
                    R.string.metered_network_confirm,
                    onValid,
                    ::onAllowMeteredNetworkForever
            ).show((activity as FragmentActivity).supportFragmentManager, "tag")
        } else
            onValid()
    }
    private fun isNetworkConnected(): Boolean = connectivityManager.activeNetworkInfo?.isConnected ?: false

    private fun isNetworkMetered(): Boolean = connectivityManager.isActiveNetworkMetered

    private fun isMeteredNetworkAllowed(): Boolean =
        defaultSharedPrefs.get(sharedPrefsAllowMeteredNetwork, false)

    private fun onAllowMeteredNetworkForever(isAllowed: Boolean) {
        defaultSharedPrefs.put(sharedPrefsAllowMeteredNetwork to isAllowed)
    }
}
