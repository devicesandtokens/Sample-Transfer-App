package com.interswitchng.interswitchpos.data.repository

import android.graphics.Bitmap
import com.interswitchng.smartpos.IswTxnHandler
import com.interswitchng.smartpos.models.PaymentModel
import com.interswitchng.smartpos.models.core.TerminalInfo
import com.interswitchng.smartpos.models.printer.info.PrintStatus
import com.interswitchng.smartpos.models.transaction.CardReadTransactionResponse
import com.interswitchng.smartpos.models.transaction.cardpaycode.EmvMessage
import com.interswitchng.smartpos.models.transaction.cardpaycode.EmvResult
import com.interswitchng.smartpos.models.transaction.cardpaycode.request.AccountType
import com.interswitchng.smartpos.shared.interfaces.device.POSDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import com.interswitchng.smartpos.models.BeneficiaryModel
import com.interswitchng.smartpos.models.NameEnquiryResult

class AppRepository(posDevice: POSDevice) {

    val handler = IswTxnHandler(posDevice)

    private val emv by lazy { posDevice.getEmvCardReader() }

    fun getCardPAN() = emv.getPan()


    suspend fun setupTransaction(amount: Int, terminalInfo: TerminalInfo, scope: CoroutineScope, channel: Channel<EmvMessage>) {
        handler.setupTransaction(amount, terminalInfo, scope, channel)
    }


    suspend fun startTransaction(scope: CoroutineScope): EmvResult {
        return withContext(Dispatchers.IO) {
            handler.startTransaction()
        }
    }

    suspend fun performTransfer (paymentModel: PaymentModel,
                                 accountType: AccountType,
                                 terminalInfo: TerminalInfo,
                                 destinationAccountNumber: String,
                                 receivingInstitutionId: String? ): CardReadTransactionResponse {
        return withContext(Dispatchers.IO) {
            handler.processCashoutTransferTransaction(paymentModel, accountType, terminalInfo, destinationAccountNumber, receivingInstitutionId)
        }

    }

    suspend fun cancelTransaction(scope: CoroutineScope) {
        return withContext(Dispatchers.IO) {
            handler.cancelTransaction()
        }
    }

    suspend fun downloadKimParams(): TerminalInfo? {
        println("getting here")
        val response = handler.downloadTmKimParam()
        return if (response != null) {
            val info = handler.getTerminalInfoFromResponse(response)
            println("info: $info")
            handler.saveTerminalInfo(info)
            downloadKeys(info)
            return info
        } else {
            null
        }
    }

    suspend fun downloadKeys(terminalInfo: TerminalInfo): Boolean? {
        return handler.downloadKeys(terminalId = terminalInfo.terminalId, ip = terminalInfo.serverIp, port = terminalInfo.serverPort)
    }

    suspend fun validateBeneficiary(bankCode: String, accountNumber: String): NameEnquiryResult<BeneficiaryModel>? {
        return handler.validateBankDetails(bankCode, accountNumber)
    }


    suspend fun getToken(terminalInfo: TerminalInfo) {
         handler.getToken(terminalInfo)
    }

    suspend fun canPrint(): PrintStatus {
        return handler.checkPrintStatus()
    }

    suspend fun printSlipNew(bitmap: Bitmap): PrintStatus {
        return handler.printslip(bitmap)
    }



}
