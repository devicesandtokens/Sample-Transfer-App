package com.interswitchng.interswitchpos.data.repository

import android.content.Context
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
import com.interswitchng.smartpos.models.cardless.CardLessPaymentRequest
import com.interswitchng.smartpos.models.transaction.PaymentInfo
import com.interswitchng.smartpos.models.transaction.PaymentType
import com.interswitchng.smartpos.models.transaction.TransactionResult
import com.interswitchng.smartpos.models.transaction.cardpaycode.CardType
import com.interswitchng.smartpos.models.transaction.cardpaycode.request.PurchaseType
import com.interswitchng.smartpos.models.transaction.cardpaycode.response.OffineDeposit
import com.interswitchng.smartpos.models.transaction.cardpaycode.response.OfflineDepositResponse
import com.interswitchng.smartpos.models.transaction.cardpaycode.response.OfflineTerminalSettingsResponse
import com.interswitchng.smartpos.models.transaction.ussdqr.response.Bank
import com.interswitchng.smartpos.models.transaction.ussdqr.response.CodeResponse
import com.interswitchng.smartpos.models.transaction.ussdqr.response.PaymentStatus
import com.interswitchng.smartpos.shared.models.transaction.CardLessPaymentInfo

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

    suspend fun startClssTransaction(scope: CoroutineScope) {
        return withContext(Dispatchers.IO) {
            handler.startClssTransaction()
        }
    }

    suspend fun performTransfer (paymentModel: PaymentModel,
                                 accountType: AccountType,
                                 terminalInfo: TerminalInfo,
                                 destinationAccountNumber: String,
                                 receivingInstitutionId: String? ): CardReadTransactionResponse {
        return withContext(Dispatchers.IO) {
            handler.processTransferTransaction(paymentModel, accountType, terminalInfo, destinationAccountNumber, receivingInstitutionId)
        }

    }

    suspend fun performDeposit (paymentModel: PaymentModel,
                                 accountType: AccountType,
                                 terminalInfo: TerminalInfo): CardReadTransactionResponse {
        return withContext(Dispatchers.IO) {
            handler.processDepositTransaction(paymentModel, accountType, terminalInfo)
        }

    }

    suspend fun performWithDrawal (paymentModel: PaymentModel,
                                accountType: AccountType,
                                terminalInfo: TerminalInfo): CardReadTransactionResponse {
        return withContext(Dispatchers.IO) {
            handler.processWithdawalTransaction(paymentModel, accountType, terminalInfo)
        }

    }

    suspend fun performOfflineDeposit(terminalInfo: TerminalInfo, amount: String, accountNumber: String): OfflineDepositResponse {
        return withContext(Dispatchers.IO) {
            handler.makeOfflinePayment(amount = amount, terminalInfo = terminalInfo, accountNumber = accountNumber)
        }
    }

    suspend fun performGetOfflineSettings(terminalInfo: TerminalInfo): OfflineTerminalSettingsResponse? {
        return withContext(Dispatchers.IO) {
            handler.getOfflineSettings(terminalInfo = terminalInfo)
        }
    }

    suspend fun performBalanceInquiry (
                                 accountType: AccountType,
                                 terminalInfo: TerminalInfo,
                                 cardType: CardType
    ): CardReadTransactionResponse {
        return withContext(Dispatchers.IO) {
            handler.processPinSelect(accountType, terminalInfo, cardType)
        }

    }

    suspend fun cancelTransaction(scope: CoroutineScope) {
        return withContext(Dispatchers.IO) {
            handler.cancelTransaction()
        }
    }

    suspend fun downloadKimParams(): TerminalInfo? {
        println("getting here settings")
        val response = handler.downloadTmKimParam()
        return if (response != null) {
            val info = handler.getTerminalInfoFromResponse(response)
            println("info: $info")
            handler.saveTerminalInfo(info)
            downloadKeys(info, false)
            return info
        } else {
            null
        }
    }

    suspend fun downloadNibsParams(terminalId: String, ip: String, port: String): TerminalInfo? {
        println("getting here settings")
        val response = handler.downloadTmNibParam(terminalId, ip, port.toInt())
        return if (response != null) {
            val info = handler.getTerminalInfo()
            println("info: $info")
            return info
        } else {
            null
        }
    }

    suspend fun downloadKeys(terminalInfo: TerminalInfo, isNibss: Boolean): Boolean? {
        println("this got here")
        return handler.downloadKeys(terminalId = terminalInfo.terminalId, ip = terminalInfo.serverIp, port = terminalInfo.serverPort, isNibbsTest = isNibss)
    }

    suspend fun downloadKeysNibss(terminalInfo: TerminalInfo, isNibss: Boolean): Boolean? {
        return handler.downloadNibssKeys(terminalId = terminalInfo.terminalId, ip = terminalInfo.serverIp, port = terminalInfo.serverPort, isNibbsTest = isNibss)
    }

    suspend fun validateBeneficiary(bankCode: String, accountNumber: String): NameEnquiryResult<BeneficiaryModel>? {
        return handler.validateBankDetails(bankCode, accountNumber)
    }

    suspend fun loadVirtualAccountDetails(request: CardLessPaymentRequest): CodeResponse? {
        request.stan = handler.getSystemStan()
        return handler.loadVirtualAccountDetails(request)
    }

    suspend fun listBanks (): List<Bank>? {
        return handler.loadbanks()
    }

    suspend fun initiateUssdTransaction(request: CardLessPaymentRequest): CodeResponse? {
        request.stan = handler.getSystemStan()
        return handler.initiateUssdTransactions(request)
    }

    suspend fun initiateQrTransaction(request: CardLessPaymentRequest, context: Context): CodeResponse? {
        println("got here for qr transaction")
        request.stan = handler.getSystemStan()
        return handler.initiateQrTransactions(request, context)
    }


    suspend fun initiatePaycodeTransaction(terminalInfo: TerminalInfo,
                                           paymentInfo: CardLessPaymentInfo, code: String): TransactionResult? {
        println("got here for paycode transaction")
        return handler.processPayCode(terminalInfo, paymentInfo, code)
    }

    suspend fun checkPayment(transactionReference: String,
                             merchantCode: String, paymentType: PaymentType): PaymentStatus? {
         return handler.checkPaymentStatus(transactionReference, merchantCode, paymentType)
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
