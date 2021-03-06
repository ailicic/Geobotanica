package com.geobotanica.geobotanica.ui.login

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.ui.login.ViewEffect.*
import com.geobotanica.geobotanica.ui.login.ViewEvent.*
import com.geobotanica.geobotanica.util.*
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.coroutines.launch
import javax.inject.Inject


class LoginFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<LoginViewModel>
    private lateinit var viewModel: LoginViewModel

    private var spinnerAdapter: ArrayAdapter<String>? = null
    private val sharedPrefsLastUserId = "lastUserId"

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity.applicationComponent.inject(this)

        viewModel = getViewModel(viewModelFactory)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val lastUserId = sharedPrefs.get(sharedPrefsLastUserId, 0L)
        bindViewModel()
        viewModel.onEvent(ViewCreated(lastUserId))
    }

    override fun onStart() {
        super.onStart()
        viewModel.syncDownloadsStatus()
    }

    private fun render(viewState: ViewState) {
        Lg.d("render(): $viewState")
        if (viewState.nicknames.size != spinnerAdapter?.count) {
            spinnerAdapter?.clear()
            spinnerAdapter?.addAll(viewState.nicknames)
            spinnerAdapter?.add("New user")
        }

        if (nicknameSpinner.selectedItemPosition != viewState.spinnerRowIndex)
            nicknameSpinner.setSelection(viewState.spinnerRowIndex)

        nicknameSpinner.isVisible = viewState.isNicknameSpinnerVisible
        nicknameTextInput.isVisible = viewState.isEditTextVisible

        if (nicknameEditText.text.toString() != viewState.nicknameEditText)
            nicknameEditText.setText(viewState.nicknameEditText)

        clearButton.isVisible = viewState.isClearButtonVisible
        fab.isVisible = viewState.isFabVisible
    }

    private fun execute(viewEffect: ViewEffect) = when(viewEffect) {
        is InitView -> {
            Lg.d("InitView")
            initSpinner()
            bindListeners()
        }
        is ShowUserExistsSnackbar -> showSnackbar(getString(R.string.userExists, viewEffect.nickname))
        is NavigateToNext -> {
            hideKeyboard(this)
            val userId = viewEffect.userId
            sharedPrefs.put(sharedPrefsLastUserId to userId)
            if (! arePermissionsGranted())
                navigateTo(R.id.action_login_to_permissions, createBundle(userId))
            else {
                lifecycleScope.launch {
                    navigateTo(viewModel.getNextFragmentId(), createBundle(userId))
                }
            }; Unit
        }
    }

    private fun initSpinner() {
        spinnerAdapter = ArrayAdapter(mainActivity, R.layout.spinner_user_item)
        spinnerAdapter?.setDropDownViewResource(R.layout.spinner_user_dropdown_item)
        nicknameSpinner.adapter = spinnerAdapter
    }

    private fun bindListeners() {
        nicknameSpinner.onItemSelected { viewModel.onEvent(ItemSelected(it)) }
        nicknameEditText.onTextChanged { viewModel.onEvent(NicknameEditTextChanged(it)) }
        clearButton.setOnClickListener { viewModel.onEvent(ClearButtonClicked) }
        fab.setOnClickListener { viewModel.onEvent(FabClicked) }
    }

    private fun bindViewModel() {
        viewModel.viewState.observe(viewLifecycleOwner) { render(it) }
        viewModel.viewEffect.observe(viewLifecycleOwner) { execute(it) }
    }

    private fun arePermissionsGranted(): Boolean =
        isPermissionGranted(WRITE_EXTERNAL_STORAGE) && isPermissionGranted(ACCESS_FINE_LOCATION)

    private fun createBundle(userId: Long) = bundleOf(userIdKey to userId)
}