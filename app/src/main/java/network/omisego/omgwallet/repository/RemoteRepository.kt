package network.omisego.omgwallet.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import co.omisego.omisego.OMGAPIClient
import co.omisego.omisego.model.APIError
import co.omisego.omisego.model.OMGResponse
import co.omisego.omisego.model.TransactionConsumption
import co.omisego.omisego.model.TransactionRequest
import co.omisego.omisego.model.params.LoginParams
import co.omisego.omisego.model.params.SignUpParams
import co.omisego.omisego.model.params.TransactionConsumptionActionParams
import co.omisego.omisego.model.params.TransactionListParams
import co.omisego.omisego.model.params.client.TransactionRequestCreateParams
import co.omisego.omisego.operation.startListeningEvents
import co.omisego.omisego.websocket.SocketClientContract
import co.omisego.omisego.websocket.event.TransactionConsumptionFinalizedEvent
import co.omisego.omisego.websocket.event.TransactionConsumptionRequestEvent
import co.omisego.omisego.websocket.listener.SocketCustomEventListener
import network.omisego.omgwallet.repository.contract.BalanceDataRepository
import network.omisego.omgwallet.extension.logi
import network.omisego.omgwallet.extension.subscribe
import network.omisego.omgwallet.extension.subscribeSingleEvent
import network.omisego.omgwallet.util.Event
import network.omisego.omgwallet.model.APIResult
import network.omisego.omgwallet.storage.Storage
import retrofit2.Response

/*
 * OmiseGO
 *
 * Created by Phuchit Sirimongkolsathien on 26/9/2018 AD.
 * Copyright © 2017-2018 OmiseGO. All rights reserved.
 */

class RemoteRepository(val client: OMGAPIClient) : BalanceDataRepository {
    fun createTransactionRequest(params: TransactionRequestCreateParams): Response<OMGResponse<TransactionRequest>> {
        return client.createTransactionRequest(params).execute()
    }

    override fun loadWallet(liveAPIResult: MutableLiveData<Event<APIResult>>) {
        client.getWallets().subscribeSingleEvent(liveAPIResult)
    }

    fun signIn(params: LoginParams, liveAPIResult: MutableLiveData<Event<APIResult>>): LiveData<Event<APIResult>> {
        return client.login(params).subscribeSingleEvent(liveAPIResult)
    }

    fun signup(params: SignUpParams, liveAPIResult: MutableLiveData<APIResult>) {
        client.signup(params).subscribe(liveAPIResult)
    }

    fun getTransactions(params: TransactionListParams, liveAPIResult: MutableLiveData<APIResult>) {
        client.getTransactions(params).subscribe(liveAPIResult)
    }

    fun approveTransaction(id: String, liveAPIResult: MutableLiveData<Event<APIResult>>) {
        client
            .approveTransactionConsumption(TransactionConsumptionActionParams(id))
            .subscribeSingleEvent(liveAPIResult)
    }

    fun rejectTransaction(id: String, liveAPIResult: MutableLiveData<Event<APIResult>>) {
        client
            .rejectTransactionConsumption(TransactionConsumptionActionParams(id))
            .subscribeSingleEvent(liveAPIResult)
    }

    fun listenUserSocketEvent(
        socketClient: SocketClientContract.Client,
        liveConsumptionRequestEvent: MutableLiveData<TransactionConsumption>,
        liveConsumptionRequestFailEvent: MutableLiveData<APIError>,
        liveConsumptionFinalizedEvent: MutableLiveData<TransactionConsumption>,
        liveConsumptionFinalizedFailEvent: MutableLiveData<APIError>
    ) {
        /* Listen for request event */
        val user = Storage.loadUser()
        logi("load the user ${user?.email}")
        user?.startListeningEvents(
            socketClient,
            listener = SocketCustomEventListener.forEvent<TransactionConsumptionRequestEvent> {
                // Show confirmation fragment here
                val txConsumption = it.socketReceive
                if (txConsumption.error == null) {
                    liveConsumptionRequestEvent.value = txConsumption.data
                } else {
                    liveConsumptionRequestFailEvent.value = txConsumption.error
                }
            })
        logi("listening for consumption request event successfully.")

        user?.startListeningEvents(
            socketClient,
            listener = SocketCustomEventListener.forEvent<TransactionConsumptionFinalizedEvent> {
                /* TODO: Handle transaction consumption finalized event here */
                val txConsumption = it.socketReceive
                if (txConsumption.error == null) {
                    liveConsumptionFinalizedEvent.value = txConsumption.data
                } else {
                    liveConsumptionFinalizedFailEvent.value = txConsumption.error
                }
            })
        logi("listening for consumption finalized event successfully.")
    }

    fun stopListeningToUserSocketEvent(socketClient: SocketClientContract.Client) {
        val user = Storage.loadUser()
        user?.stopListening(socketClient)
    }
}
