package network.omisego.omgwallet

/*
 * OmiseGO
 *
 * Created by Phuchit Sirimongkolsathien on 5/10/2018 AD.
 * Copyright © 2017-2018 OmiseGO. All rights reserved.
 */

import androidx.test.ext.junit.runners.AndroidJUnit4
import co.omisego.omisego.model.params.LoginParams
import network.omisego.omgwallet.setup.base.BaseInstrumentalTest
import network.omisego.omgwallet.setup.config.TestData
import network.omisego.omgwallet.setup.screen.BalanceScreen
import network.omisego.omgwallet.setup.screen.MainScreen
import network.omisego.omgwallet.setup.screen.ProfileScreen
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldNotBe
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainTest : BaseInstrumentalTest() {
    private val balanceScreen: BalanceScreen by lazy { BalanceScreen() }
    private val mainScreen: MainScreen by lazy { MainScreen() }
    private val profileScreen: ProfileScreen by lazy { ProfileScreen() }

    companion object : BaseInstrumentalTest() {
        @BeforeClass
        @JvmStatic
        fun setupClass() {
            setupClient()
            localRepository.deleteSession()
            val response = client.login(LoginParams(TestData.USER_EMAIL, TestData.USER_PASSWORD)).execute()
            val clientAuthenticationToken = response.body()?.data!!
            localRepository.saveSession(clientAuthenticationToken)
        }
    }

    @Before
    fun setup() {
        setupClient()
        localRepository.deleteTransactionRequest()
        localRepository.deleteTokenPrimary()
        registerIdlingResource()
        start()
    }

    @After
    fun teardown() {
        unregisterIdlingResource()
    }

    @Test
    fun testShowMainBottomBar() {
        mainScreen {
            fabQR.isDisplayed()
            bottomNavigation.isDisplayed()
        }
    }

    @Test
    fun testShowBalance() {
        balanceScreen {
            recyclerView {
                isDisplayed()
                firstChild<BalanceScreen.Item> {
                    isVisible()
                    tvTokenLogo {
                        isVisible()
                    }
                    tvCurrencyName {
                        isVisible()
                    }
                }
            }
        }
        hasToolbarTitle(stringRes(R.string.balance_title))
    }

    @Test
    fun testShowProfile() {
        mainScreen.bottomNavigation.setSelectedItem(R.id.profile)
        hasToolbarTitle(stringRes(R.string.profile_title))
        profileScreen {
            tvTransaction.isDisplayed()
            tvFingerprintTitle.isDisplayed()
            tvSignOut.isDisplayed()
        }
    }

    @Test
    fun testPrimaryTokenShouldBeVisibleWhenFirstTimeLogin() {
        balanceScreen {
            this.recyclerView.isDisplayed()

            val primaryToken = selectPrimaryToken(localRepository.loadWallets()!!, null)
            val position = localRepository.loadWallets()?.data?.get(0)?.balances?.indexOfFirst { it.token.id == primaryToken.id }

            position shouldNotBe null
            position?.shouldBeGreaterThan(-1)

            recyclerView {
                childAt<BalanceScreen.Item>(position!!) {
                    tvPrimaryToken.isDisplayed()
                }
            }
        }
    }

    @Test
    fun testRepeatlySwitchTab() {
        for (i in 0..1) {
            mainScreen.bottomNavigation.setSelectedItem(R.id.profile)
            hasToolbarTitle(stringRes(R.string.profile_title))
            profileScreen.tvTransaction.isDisplayed()
            mainScreen.bottomNavigation.setSelectedItem(R.id.balance)
            hasToolbarTitle(stringRes(R.string.balance_title))
            balanceScreen.recyclerView.isDisplayed()
        }
    }
}
