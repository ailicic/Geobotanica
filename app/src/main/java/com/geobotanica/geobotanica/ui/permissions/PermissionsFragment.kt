package com.geobotanica.geobotanica.ui.permissions

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.getFromBundle
import kotlinx.android.synthetic.main.fragment_permissions.*
import javax.inject.Inject


class PermissionsFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<PermissionsViewModel>
    private lateinit var viewModel: PermissionsViewModel

    private val missingPermissions = mutableListOf<String>()
    private val permissionsRequestCode = 0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)

        viewModel = getViewModel(viewModelFactory) {
            userId = getFromBundle(userIdKey)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_permissions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updatePermissionsStatus()
        bindListeners()
    }

    private fun updatePermissionsStatus() {
        writeExternalStorageCheckMark.isVisible = isPermissionGranted(WRITE_EXTERNAL_STORAGE)
        accessFineLocationCheckMark.isVisible = isPermissionGranted(ACCESS_FINE_LOCATION)
        if (areAllPermissionsGranted()) {
            Lg.i("All permissions granted")
            fab.isVisible = true
            permissionsButton.isVisible = false
        }
    }

    private fun bindListeners() {
        permissionsButton.setOnClickListener { requestPermissions() }
        fab.setOnClickListener { navigateToNext() }
    }

    private fun requestPermissions() {
        missingPermissions.clear()
        if (! isPermissionGranted(WRITE_EXTERNAL_STORAGE))
            missingPermissions.add(WRITE_EXTERNAL_STORAGE)
        if (! isPermissionGranted(ACCESS_FINE_LOCATION))
            missingPermissions.add(ACCESS_FINE_LOCATION)
        requestPermissions(missingPermissions.toTypedArray(), permissionsRequestCode)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            permissionsRequestCode -> {
                if (! areAllPermissionsGranted())
                    showSnackbar(getString(R.string.permissions_required))
                updatePermissionsStatus()
            }
            else -> { }
        }
    }

    private fun areAllPermissionsGranted(): Boolean =
        isPermissionGranted(WRITE_EXTERNAL_STORAGE) && isPermissionGranted(ACCESS_FINE_LOCATION)

    private fun navigateToNext() =
        navigateTo(R.id.action_permissions_to_downloadAssets, bundleOf(userIdKey to viewModel.userId), R.id.permissionsFragment)
}