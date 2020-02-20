package com.geobotanica.geobotanica.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.android.worker.DownloadStatusSynchronizer
import com.geobotanica.geobotanica.data.entity.OnlineAsset
import com.geobotanica.geobotanica.data.entity.User
import com.geobotanica.geobotanica.data.repo.AssetRepo
import com.geobotanica.geobotanica.data.repo.MapRepo
import com.geobotanica.geobotanica.data.repo.UserRepo
import com.geobotanica.geobotanica.ui.login.OnlineAssetId.*
import com.geobotanica.geobotanica.ui.login.ViewEffect.*
import com.geobotanica.geobotanica.ui.login.ViewEvent.*
import com.geobotanica.geobotanica.util.GbDispatchers
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.SingleLiveEvent
import com.geobotanica.geobotanica.util.mutableLiveData
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class LoginViewModel @Inject constructor (
        private val dispatchers: GbDispatchers,
        private val userRepo: UserRepo,
        private val assetRepo: AssetRepo,
        private val mapRepo: MapRepo,
        private val downloadStatusSynchronizer: DownloadStatusSynchronizer
//        ,onlineMapScraper: OnlineMapScraper // Uncomment to perform scrape TODO: REMOVE AFTER SCRAPER MOVED TO SERVER
): ViewModel() {
    private val _viewState = mutableLiveData(ViewState())
    val viewState: LiveData<ViewState> = _viewState

    private val _viewEffect = SingleLiveEvent<ViewEffect>()
    val viewEffect: LiveData<ViewEffect> = _viewEffect

    private var selectedUserId: Long = 0L
    private val minLength = 3

    fun onEvent(event: ViewEvent): Unit = when (event) {
        is ViewCreated -> {
            emitViewEffect(InitView)
            viewModelScope.launch(dispatchers.main) {
                selectedUserId = event.lastUserId
                val users = userRepo.getAll()
                val lastUser = users.firstOrNull { it.id == event.lastUserId }
                val lastRowIndex = lastUser?.let { users.indexOf(it) } ?: 0
                val nicknames = users.map { it.nickname }

                updateViewState(
                        spinnerRowIndex = lastRowIndex,
                        nicknames = nicknames,
                        isEditTextVisible = nicknames.isEmpty(),
                        isNicknameSpinnerVisible =  nicknames.isNotEmpty(),
                        isFabVisible = nicknames.isNotEmpty()
                )
            }; Unit
        }
        is NicknameEditTextChanged -> {
            val editText = event.editText
            val isClearButtonVisible = editText.isNotBlank()
            updateViewState(
                    nicknameEditText = editText,
                    isClearButtonVisible = isClearButtonVisible,
                    isFabVisible = isNicknameValid(editText)
            )
        }
        is ClearButtonClicked -> updateViewState(nicknameEditText = "")
        is ItemSelected -> {
            if (newUserSelected(event.rowIndex)) {
                selectedUserId = 0L
                val editText = viewState.value?.nicknameEditText
                val isClearButtonVisible = viewState.value?.nicknameEditText?.isNotEmpty() ?: false
                updateViewState(
                        spinnerRowIndex = event.rowIndex,
                        isEditTextVisible = true,
                        isClearButtonVisible = isClearButtonVisible,
                        isFabVisible = isNicknameValid(editText ?: "")
                )
            } else {
                viewState.value?.nicknames?.get(event.rowIndex)?.let { nickname ->
                    viewModelScope.launch(dispatchers.main) {
                        selectedUserId = userRepo.getByNickname(nickname).id
                        updateViewState(
                                spinnerRowIndex = event.rowIndex,
                                isEditTextVisible = false,
                                isClearButtonVisible = false,
                                isFabVisible = true
                        )
                    }
                }
            }; Unit
        }
        is FabClicked -> {
                if (selectedUserId != 0L)
                    emitViewEffect(NavigateToNext(selectedUserId))
                else {
                    viewModelScope.launch(dispatchers.main) {
                        val newNickname = viewState.value?.nicknameEditText ?: ""
                        val nicknames = userRepo.getAll().map { it.nickname }
                        if (nicknames.contains(newNickname)) {
                            emitViewEffect(ShowUserExistsSnackbar(newNickname))
                            return@launch
                        }

                        val newUserId = createUser(newNickname)
                        Lg.d("Created new User: $newNickname (id = $newUserId)")
                        emitViewEffect(NavigateToNext(newUserId))
                    }; Unit
                }
        }
    }

    private fun isNicknameValid(nickname: String) = nickname.length >= minLength

    private fun newUserSelected(rowIndex: Int): Boolean = rowIndex == viewState.value?.nicknames?.size

    private suspend fun createUser(nickname: String): Long = withContext(dispatchers.io) {
        userRepo.insert(User(nickname))
    }

    private fun updateViewState(
            nicknames: List<String> = viewState.value?.nicknames ?: emptyList(),
            spinnerRowIndex: Int = viewState.value?.spinnerRowIndex ?: 0,
            isNicknameSpinnerVisible: Boolean = viewState.value?.isNicknameSpinnerVisible ?: false,
            isEditTextVisible: Boolean = viewState.value?.isEditTextVisible ?: false,
            nicknameEditText: String = viewState.value?.nicknameEditText ?: "",
            isClearButtonVisible: Boolean = viewState.value?.isClearButtonVisible ?: false,
            isFabVisible: Boolean = viewState.value?.isFabVisible ?: false
    ) {
        _viewState.value = viewState.value?.copy(
                nicknames = nicknames,
                spinnerRowIndex = spinnerRowIndex,
                isNicknameSpinnerVisible = isNicknameSpinnerVisible,
                isEditTextVisible = isEditTextVisible,
                nicknameEditText = nicknameEditText,
                isClearButtonVisible = isClearButtonVisible,
                isFabVisible = isFabVisible
        )
    }

    private fun emitViewEffect(viewEffect: ViewEffect) {
        _viewEffect.value = viewEffect
    }

    suspend fun getNextFragmentId(): Int {
        if (assetRepo.isEmpty())
            return R.id.action_login_to_download_assets

        val onlineAssets = assetRepo.getAll()
        val mapFoldersAsset = onlineAssets.find { it.id == MAP_FOLDER_LIST.id } ?: throw IllegalStateException()
        val mapListAsset = onlineAssets.find { it.id == MAP_LIST.id } ?: throw IllegalStateException()
        val worldMapAsset = onlineAssets.find { it.id == WORLD_MAP.id } ?: throw IllegalStateException()
        val plantNamesAsset = onlineAssets.find { it.id == PLANT_NAMES.id } ?: throw IllegalStateException()


        return if (! mapFoldersAsset.isDownloaded  || ! mapListAsset.isDownloaded ||
                ! worldMapAsset.isDownloaded || ! plantNamesAsset.isDownloaded)
        {
            R.id.action_login_to_download_assets
        } else if (mapRepo.getInitiatedDownloads().isEmpty()) {
            R.id.action_login_to_local_maps
        } else
            R.id.action_login_to_map
    }

    fun syncDownloadsStatus() = viewModelScope.launch(dispatchers.io) {
        importOnlineAssetList()
        downloadStatusSynchronizer.syncAll()
    }

    private suspend fun importOnlineAssetList() {
        if (assetRepo.isEmpty()) {
            Lg.d("LoginViewModel: Importing onlineAssetList")
            assetRepo.insert(onlineAssetList)
        }
    }
}

data class ViewState(
        val nicknames: List<String> = emptyList(),
        val spinnerRowIndex: Int = 0,
        val isNicknameSpinnerVisible: Boolean = false,
        val isEditTextVisible: Boolean = true,
        val nicknameEditText: String = "",
        val isClearButtonVisible: Boolean = false,
        val isFabVisible: Boolean = false
)

sealed class ViewEvent {
    data class ViewCreated(val lastUserId: Long) : ViewEvent()
    data class NicknameEditTextChanged(val editText: String) : ViewEvent()
    object ClearButtonClicked : ViewEvent()
    data class ItemSelected(val rowIndex: Int) : ViewEvent()
    object FabClicked : ViewEvent()
}

sealed class ViewEffect {
    object InitView : ViewEffect()
    data class ShowUserExistsSnackbar(val nickname: String) : ViewEffect()
    data class NavigateToNext(val userId: Long) : ViewEffect()
}

// TODO: Get from API
val onlineAssetList = listOf(
        OnlineAsset(
                "Map metadata",
                "http://people.okanagan.bc.ca/ailicic/Maps/map_folders.json.gz",
                "",
                false,
                353,
                1_407,
                13
        ),
        OnlineAsset(
                "Map list",
                "http://people.okanagan.bc.ca/ailicic/Maps/maps.json.gz",
                "",
                false,
                5_812,
                41_751,
                288
        ),
        OnlineAsset(
                "World map",
                "http://people.okanagan.bc.ca/ailicic/Maps/world.map.gz",
                "",
                false,
                2_715_512,
                3_276_950,
                1
        ),
        OnlineAsset(
                "Plant name database",
                "http://people.okanagan.bc.ca/ailicic/Maps/taxa.db.gz",
                "databases",
                true,
                29_037_482,
                129_412_096,
                1
        )
)

// TODO: This is fragile. If asset list changes in updated version it will break.
enum class OnlineAssetId(val id: Long) {
    MAP_FOLDER_LIST(1L),
    MAP_LIST(2L),
    WORLD_MAP(3L),
    PLANT_NAMES(4L)
}