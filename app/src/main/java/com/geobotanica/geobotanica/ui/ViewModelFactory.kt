package com.geobotanica.geobotanica.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import javax.inject.Inject


@Suppress("UNCHECKED_CAST")
class ViewModelFactory<T : ViewModel> @Inject constructor(private val viewModel: T) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(viewModelClass: Class<T>): T = viewModel as T
}

object BaseFragmentExt {
    inline fun <reified T : ViewModel> BaseFragment.getViewModel(
            viewModelFactory: ViewModelFactory<T>,
            block: T.() -> Unit = { }
    ): T {
        return ViewModelProviders.of(activity, viewModelFactory)
            .get(T::class.java)
            .apply { block() }
    }
}