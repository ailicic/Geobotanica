package com.geobotanica.geobotanica.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import javax.inject.Inject


class ViewModelFactory<VM : ViewModel> @Inject constructor(
    private val viewModel: VM
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(viewModelClass: Class<T>): T = viewModel as T
}

object BaseFragmentExt {
    inline fun <reified VM : ViewModel> BaseFragment.getViewModel(
        viewModelFactory: ViewModelFactory<VM>,
        inject: VM.() -> Unit
    ): VM {
        return ViewModelProviders.of(this, viewModelFactory)
            .get(VM::class.java)
            .apply { inject() }
    }
}
