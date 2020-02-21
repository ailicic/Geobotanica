package com.geobotanica.geobotanica.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities.*
import android.os.Build
import androidx.preference.PreferenceManager
import com.geobotanica.geobotanica.network.NetworkValidator.NetworkState.*
import com.geobotanica.geobotanica.util.get
import com.geobotanica.geobotanica.util.put
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class NetworkValidator @Inject constructor(appContext: Context) {

    private val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val defaultSharedPrefs = PreferenceManager.getDefaultSharedPreferences(appContext)

    private val sharedPrefsAllowMeteredNetwork = "allowMeteredNetwork"

    fun allowMeteredNetwork() = defaultSharedPrefs.put(sharedPrefsAllowMeteredNetwork to true)

    fun getStatus(): NetworkState {
        return if (! isNetworkConnected())
            INVALID
        else if (isNetworkMetered() && ! isMeteredNetworkAllowed())
            VALID_IF_METERED_PERMITTED
        else
            VALID
    }

    private fun isNetworkMetered(): Boolean = connectivityManager.isActiveNetworkMetered

    private fun isNetworkConnected(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
             connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)?.run {
                 hasTransport(TRANSPORT_WIFI) || hasTransport(TRANSPORT_CELLULAR) || hasTransport(TRANSPORT_ETHERNET)
            } ?: false
        } else { @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.isConnected ?: false
        }
    }

    private fun isMeteredNetworkAllowed(): Boolean =
        defaultSharedPrefs.get(sharedPrefsAllowMeteredNetwork, false)

    enum class NetworkState {
        INVALID, VALID, VALID_IF_METERED_PERMITTED
    }
}
