package com.geobotanica.geobotanica.viewmodel

import com.geobotanica.geobotanica.data.entity.User
import com.geobotanica.geobotanica.data.repo.AssetRepo
import com.geobotanica.geobotanica.data.repo.MapRepo
import com.geobotanica.geobotanica.data.repo.UserRepo
import com.geobotanica.geobotanica.ui.login.LoginViewModel
import com.geobotanica.geobotanica.ui.login.ViewEffect
import com.geobotanica.geobotanica.ui.login.ViewEffect.*
import com.geobotanica.geobotanica.ui.login.ViewEvent.*
import com.geobotanica.geobotanica.ui.login.ViewState
import com.geobotanica.geobotanica.util.MockkUtil.coVerifyZero
import com.geobotanica.geobotanica.util.MockkUtil.mockkObserver
import com.geobotanica.geobotanica.util.MockkUtil.verifyOne
import com.geobotanica.geobotanica.util.SpekExt.allowLiveData
import com.geobotanica.geobotanica.util.SpekExt.mockTime
import com.geobotanica.geobotanica.util.SpekExt.setupTestDispatchers
import io.mockk.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object LoginViewModelTest : Spek({
    mockTime()
    allowLiveData()
    val testDispatchers = setupTestDispatchers()

    val viewStateObserver = mockkObserver<ViewState>()
    val viewEffectObserver = mockkObserver<ViewEffect>()

    val user1 = User("user1").apply { id = 1L }
    val user2 = User("user2").apply { id = 2L }
    val userList = listOf(user1, user2)
    val userRepo = mockk<UserRepo>()
    val assetRepo = mockk<AssetRepo>()
    val mapRepo = mockk<MapRepo>()

    val loginViewModel by memoized {
        LoginViewModel(testDispatchers, userRepo, assetRepo, mapRepo).apply {
            viewState.observeForever(viewStateObserver)
            viewEffect.observeForever(viewEffectObserver)
        }
    }

    beforeEachTest {
        clearMocks(
                viewStateObserver,
                viewEffectObserver,
                userRepo,
                assetRepo,
                mapRepo,
                answers = false // Keep stubbing, reset recorded calls
        )
    }

    describe("ViewCreated Event") {
        context("No users in db") {
            beforeEachTest {
                coEvery { userRepo.getAll() } returns emptyList()
                loginViewModel.onEvent(ViewCreated(0L))
            }

            it("Should emit InitView effect once") {
                verifyOne { viewEffectObserver.onChanged(InitView) }
            }

            it("Should emit correct ViewState") {
                verifyOne { viewStateObserver.onChanged(
                        ViewState(
                                isEditTextVisible = true,
                                isNicknameSpinnerVisible = false,
                                isFabVisible = false
                        )
                ) }
            }
        }

        context("Users in db") {
            beforeEachTest {
                coEvery { userRepo.getAll() } returns userList
            }

            context("Last user = user1") {
                beforeEachTest {
                    loginViewModel.onEvent(ViewCreated(user1.id))
                }

                it("Should emit correct ViewState") {
                    verifyOne { viewStateObserver.onChanged(
                            ViewState(
                                    spinnerRowIndex = 0,
                                    nicknames = listOf(user1.nickname, user2.nickname),
                                    isEditTextVisible = false,
                                    isNicknameSpinnerVisible = true,
                                    isFabVisible = true
                            )
                    ) }
                }
            }

            context("Last user = user2") {
                beforeEachTest {
                    loginViewModel.onEvent(ViewCreated(user2.id))
                }

                it("Should emit correct ViewState") {
                    verifyOne { viewStateObserver.onChanged(
                            ViewState(
                                    spinnerRowIndex = 1,
                                    nicknames = listOf(user1.nickname, user2.nickname),
                                    isEditTextVisible = false,
                                    isNicknameSpinnerVisible = true,
                                    isFabVisible = true
                            )
                    ) }
                }
            }
        }
    }

    describe("NicknameEditTextChanged Event") {
        beforeEachTest {
            coEvery { userRepo.getAll() } returns emptyList()
            loginViewModel.onEvent(ViewCreated(0L))
        }

        context("Nickname empty") {
            beforeEachTest {
                loginViewModel.onEvent(NicknameEditTextChanged(""))
            }

            it("Should emit correct ViewState") {
                verify { viewStateObserver.onChanged(
                        ViewState(
                                isNicknameSpinnerVisible = false,
                                nicknameEditText = "",
                                isEditTextVisible = true,
                                isClearButtonVisible = false,
                                isFabVisible = false
                        )
                )}
            }
        }

        context("Nickname too short") {
            beforeEachTest {
                loginViewModel.onEvent(NicknameEditTextChanged("nn"))
            }

            it("Should emit correct ViewState") {
                verify { viewStateObserver.onChanged(
                        ViewState(
                                isNicknameSpinnerVisible = false,
                                nicknameEditText = "nn",
                                isEditTextVisible = true,
                                isClearButtonVisible = true,
                                isFabVisible = false
                        )
                )}
            }
        }

        context("Nickname ok") {
            beforeEachTest {
                loginViewModel.onEvent(NicknameEditTextChanged("Nickname"))
            }

            it("Should emit correct ViewState") {
                verify { viewStateObserver.onChanged(
                        ViewState(
                                isNicknameSpinnerVisible = false,
                                nicknameEditText = "Nickname",
                                isEditTextVisible = true,
                                isClearButtonVisible = true,
                                isFabVisible = true
                        )
                )}
            }
        }
    }

    describe("ClearButtonClicked Event") {
        beforeEachTest {
            coEvery { userRepo.getAll() } returns emptyList()
            loginViewModel.onEvent(ViewCreated(0L))
            loginViewModel.onEvent(NicknameEditTextChanged("Nickname"))
            loginViewModel.onEvent(ClearButtonClicked)
        }

        it("Should clear edit text") {

            verify { viewStateObserver.onChanged(
                    ViewState(
                            nicknameEditText = "",  // Important
                            isEditTextVisible = true
                    )
            )}
        }
    }

    describe("ItemSelected Event") {
        beforeEachTest {
            coEvery { userRepo.getAll() } returns userList
        }

        context("New user selected") {
            beforeEachTest {
                loginViewModel.onEvent(ViewCreated(0L))
                loginViewModel.onEvent(ItemSelected(2))
            }

            it("Should show nickname EditText") {

                verify { viewStateObserver.onChanged(
                        ViewState(
                                nicknames = listOf(user1.nickname, user2.nickname),
                                spinnerRowIndex = 2,
                                isNicknameSpinnerVisible = true,
                                isEditTextVisible = true,
                                isFabVisible = false
                        )
                )}
            }
        }

        context("User selected") {
            beforeEachTest {
                coEvery { userRepo.getByNickname("user2") } returns user2
                loginViewModel.onEvent(ViewCreated(0L))
                loginViewModel.onEvent(ItemSelected(1))
            }

            it("Should emit correct ViewState") {

                verify { viewStateObserver.onChanged(
                        ViewState(
                                nicknames = listOf(user1.nickname, user2.nickname),
                                spinnerRowIndex = 1,
                                isNicknameSpinnerVisible = true,
                                isEditTextVisible = false,
                                isFabVisible = true
                        )
                )}
            }
        }
    }

    describe("FabClicked Event") {
        context("Existing user selected") {
            beforeEachTest {
                coEvery { userRepo.getAll() } returns userList
                loginViewModel.onEvent(ViewCreated(user1.id))
                loginViewModel.onEvent(FabClicked)
            }

            it("Should emit NavigateToNext ViewEffect") {
                verify { viewEffectObserver.onChanged(NavigateToNext(user1.id))}
            }
        }

        context("New user") {
            beforeEachTest {
                coEvery { userRepo.getAll() } returns emptyList()
                coEvery { userRepo.insert(any()) } returns 10L
                loginViewModel.onEvent(ViewCreated(0L))
                loginViewModel.onEvent(NicknameEditTextChanged("New"))
                loginViewModel.onEvent(FabClicked)
            }

            it("Should create new user") { coVerify { userRepo.insert(User("New")) } }
            it("Should emit NavigateToNext ViewEffect") {
                verify { viewEffectObserver.onChanged(NavigateToNext(10L))}
            }
        }

        context("New user exists already") {
            beforeEachTest {
                coEvery { userRepo.getAll() } returns userList
                coEvery { userRepo.insert(any()) } returns 10L
                loginViewModel.onEvent(ViewCreated(0L))
                loginViewModel.onEvent(ItemSelected(2))
                loginViewModel.onEvent(NicknameEditTextChanged(user1.nickname))
                loginViewModel.onEvent(FabClicked)
            }

            it("Should not create new user") { coVerifyZero { userRepo.insert(any()) } }
            it("Should emit ShowUserExistsSnackbar ViewEffect") {
                verify { viewEffectObserver.onChanged(ShowUserExistsSnackbar(user1.nickname))}
            }
        }
    }

})