package network.omisego.omgwallet

/*
 * OmiseGO
 *
 * Created by Phuchit Sirimongkolsathien on 5/10/2018 AD.
 * Copyright © 2017-2018 OmiseGO. All rights reserved.
 */

import androidx.test.runner.AndroidJUnit4
import co.omisego.omisego.model.params.LoginParams
import network.omisego.omgwallet.R
import network.omisego.omgwallet.setup.base.BaseInstrumentalTest
import network.omisego.omgwallet.setup.config.MockData
import network.omisego.omgwallet.setup.config.TestData
import network.omisego.omgwallet.setup.screen.MainScreen
import network.omisego.omgwallet.setup.screen.QRScreen
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QRTest : BaseInstrumentalTest() {
    private val qrScreen: QRScreen by lazy { QRScreen() }
    private val mainScreen: MainScreen by lazy { MainScreen() }

    @Before
    fun setup() {
        setupClient()
        localRepository.deleteSession()
        val response = client.login(LoginParams(TestData.USER_EMAIL, TestData.USER_PASSWORD)).execute()
        val clientAuthenticationToken = response.body()?.data!!
        localRepository.saveSession(clientAuthenticationToken)
        localRepository.deleteFingerprintSession()
        localRepository.saveWallets(MockData.walletList)
        start()
        registerIdlingResource()
    }

    @After
    fun teardown() {
        unregisterIdlingResource()
    }

    @Test
    fun testShowQR() {
        mainScreen.fabQR.click()
        qrScreen {
            hasToolbarTitle(stringRes(R.string.show_qr_title))
            ivQR {
                isDisplayed()
            }
        }
    }

    @Test
    fun testCloseQR() {
        mainScreen.fabQR.click()
        hasToolbarTitle(stringRes(R.string.show_qr_title))
        qrScreen.pressBack()
        hasToolbarTitle(stringRes(R.string.balance_title))
        mainScreen.bottomNavigation.setSelectedItem(R.id.profile)
        mainScreen.fabQR.click()
        qrScreen.pressBack()
        hasToolbarTitle(stringRes(R.string.profile_title))
    }
}