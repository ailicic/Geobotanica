package com.geobotanica.geobotanica.ui.login

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.ui.login.ViewEffect.*
import com.geobotanica.geobotanica.ui.login.ViewEvent.*
import com.geobotanica.geobotanica.util.*
import kotlinx.android.synthetic.main.fragment_login.*
import javax.inject.Inject


class LoginFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<LoginViewModel>
    private lateinit var viewModel: LoginViewModel

    private var spinnerAdapter: ArrayAdapter<String>? = null
    private val sharedPrefsLastUserId = "lastUserId"

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)

        viewModel = getViewModel(viewModelFactory)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViewModel()

        val lastUserId = sharedPrefs.get(sharedPrefsLastUserId, 0L)
        viewModel.onEvent(ViewCreated(lastUserId))
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
            sharedPrefs.put(sharedPrefsLastUserId to viewEffect.userId)
            navigateTo(
                    R.id.action_login_to_map,
                    bundleOf(userIdKey to viewEffect.userId)
            )
        }
    }

    private fun initSpinner() {
        spinnerAdapter = ArrayAdapter(activity, R.layout.spinner_user_item)
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
        viewModel.viewState.observe(viewLifecycleOwner, Observer { render(it) })
        viewModel.viewEffect.observe(viewLifecycleOwner, Observer { execute(it) })
    }
}