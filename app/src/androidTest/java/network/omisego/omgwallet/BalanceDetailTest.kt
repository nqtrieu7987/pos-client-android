package network.omisego.omgwallet

import androidx.test.ext.junit.runners.AndroidJUnit4
import co.omisego.omisego.model.TransactionRequestType
import co.omisego.omisego.model.params.LoginParams
import co.omisego.omisego.model.params.TransactionRequestParams
import com.agoda.kakao.text.KButton
import network.omisego.omgwallet.extension.logi
import network.omisego.omgwallet.setup.base.BaseInstrumentalTest
import network.omisego.omgwallet.setup.config.TestData
import network.omisego.omgwallet.setup.screen.BalanceDetailScreen
import network.omisego.omgwallet.setup.screen.BalanceScreen
import network.omisego.omgwallet.setup.screen.MainScreen
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotBe
import org.amshove.kluent.shouldNotEqualTo
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

/*
 * OmiseGO
 *
 * Created by Phuchit Sirimongkolsathien on 22/10/2018 AD.
 * Copyright © 2017-2018 OmiseGO. All rights reserved.
 */
@RunWith(AndroidJUnit4::class)
class BalanceDetailTest : BaseInstrumentalTest() {
    private val mainScreen: MainScreen by lazy { MainScreen() }
    private val balanceScreen: BalanceScreen by lazy { BalanceScreen() }
    private val balanceDetailScreen: BalanceDetailScreen by lazy { BalanceDetailScreen() }

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
        registerIdlingResource()
        start()
    }

    @After
    fun teardown() {
        unregisterIdlingResource()
    }

    @Test
    fun testBalanceDetailShouldBeDisplayedCorrectly() {
        mainScreen {
            bottomNavigation.isDisplayed()

            val balances = localRepository.loadWallets()?.data?.get(0)?.balances!!
            val totalPage = balances.size
            val firstToken = balances[0].token
            val lastToken = balances[totalPage - 1].token

            /* Click at first token */
            balanceScreen {
                recyclerView {
                    firstChild<BalanceScreen.Item> {
                        click()
                    }
                }
            }

            balanceDetailScreen {
                viewpager.isDisplayed()
                indicator.isDisplayed()
                tvTokenPrimaryHelper.isDisplayed()

                /* Verify page, token's symbol and balance amount */
                viewpager {
                    isAtPage(0)
                    hasDescendant {
                        withText(firstToken.symbol)
                    }
                    hasDescendant {
                        withText(balances[0].displayAmount())
                    }
                }

                pressBack()
            }

            /* Do the same thing with the last token */
            balanceScreen {
                recyclerView {
                    swipeUp()
                    lastChild<BalanceScreen.Item> {
                        click()
                    }
                }
            }

            balanceDetailScreen {
                viewpager.isDisplayed()
                indicator.isDisplayed()
                tvTokenPrimaryHelper.isDisplayed()

                viewpager {
                    isAtPage(totalPage - 1)
                    hasDescendant {
                        withText(lastToken.symbol)
                    }
                    hasDescendant {
                        withText(balances[totalPage - 1].displayAmount())
                    }
                }

                pressBack()
            }
        }
    }

    @Test
    fun testBalanceDetailShouldBeSwipable() {
        mainScreen {
            bottomNavigation.isDisplayed()

            /* Click at first token */
            balanceScreen {
                recyclerView {
                    firstChild<BalanceScreen.Item> {
                        click()
                    }
                }
            }

            balanceDetailScreen {
                viewpager.isDisplayed()
                indicator.isDisplayed()
                tvTokenPrimaryHelper.isDisplayed()

                viewpager {
                    swipeLeft()
                    isAtPage(1)
                    swipeLeft()
                    isAtPage(2)
                    swipeRight()
                    isAtPage(1)
                    swipeRight()
                    isAtPage(0)
                }
            }
        }
    }

    @Test
    fun testSetPrimaryToken() {
        mainScreen {
            bottomNavigation.isDisplayed()

            /* Prepare data for verification */
            val oldFormattedIds = localRepository.loadTransactionRequest()
            val primaryTokenId = localRepository.loadTokenPrimary()
            val balances = localRepository.loadWallets()?.data?.get(0)?.balances!!
            val nextPrimaryBalance = balances.find { it.token.id != primaryTokenId }!!
            val nextPrimaryBalanceIndex = balances.indexOfFirst { it.token.id == nextPrimaryBalance.token.id }

            /* Go to balance detail page by clicking at the first token row */
            balanceScreen {
                recyclerView {
                    childAt<BalanceScreen.Item>(nextPrimaryBalanceIndex) {
                        click()
                    }
                }
            }

            balanceDetailScreen {

                /* Verify there is a button with text "Set Primary" */
                viewpager {
                    hasDescendant {
                        withText(stringRes(R.string.balance_detail_token_set_primary))
                    }
                }

                /* Click the button "Set Primary" */
                val btnSetPrimary = KButton {
                    withId(R.id.btnSetPrimary)
                    withSibling { withText(nextPrimaryBalance.token.symbol) }
                }
                btnSetPrimary.click()

                /* Verify that tokenId should be changed */
                primaryTokenId shouldNotBe localRepository.loadTokenPrimary()

                /* Verify that the button is disabled and change the text to "Primary" */
                btnSetPrimary.isDisabled()
                btnSetPrimary.hasText(R.string.balance_detail_token_primary)

                /* Go back to balance list */
                pressBack()
            }

            balanceScreen {
                /* Verify that the small gray "Primary" indicator has set at the correct balance row */
                recyclerView {
                    childAt<BalanceScreen.Item>(nextPrimaryBalanceIndex) {
                        tvCurrencyName.hasText(nextPrimaryBalance.token.name)
                        tvPrimaryToken.isDisplayed()
                    }
                }

                /* Verify new transaction request ids are generated */
                val newFormattedIds = localRepository.loadTransactionRequest()
                oldFormattedIds shouldNotEqualTo newFormattedIds

                /* Verify the transaction request ids are correct */
                val sendTxId = newFormattedIds.split("|")[1]
                val receiveTxId = newFormattedIds.split("|")[0]
                val sendTx = client.retrieveTransactionRequest(TransactionRequestParams(sendTxId)).execute()
                val receiveTx = client.retrieveTransactionRequest(TransactionRequestParams(receiveTxId)).execute()

                /* Prevent espresso stop the test by validate view (because the operation is coming from MessageQueue)*/
                recyclerView.isDisplayed()

                logi(sendTx.body()?.data)
                logi(receiveTx.body()?.data)

                /* verify transaction request type and token id are correct */
                sendTx.body()?.data?.type shouldEqual TransactionRequestType.SEND
                receiveTx.body()?.data?.type shouldEqual TransactionRequestType.RECEIVE
                sendTx.body()?.data?.token?.id shouldEqual nextPrimaryBalance.token.id
                receiveTx.body()?.data?.token?.id shouldEqual nextPrimaryBalance.token.id
            }
        }
    }
}
