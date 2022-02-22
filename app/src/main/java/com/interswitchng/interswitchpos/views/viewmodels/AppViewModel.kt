package com.interswitchng.interswitchpos.views.viewmodels

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interswitchng.interswitchpos.data.repository.AppRepository
import com.interswitchng.smartpos.models.BeneficiaryModel
import com.interswitchng.smartpos.models.NameEnquiryResult
import com.interswitchng.smartpos.models.PaymentModel
import com.interswitchng.smartpos.models.cardless.CardLessPaymentRequest
import com.interswitchng.smartpos.models.core.TerminalInfo
import com.interswitchng.smartpos.models.posconfig.TerminalConfig
import com.interswitchng.smartpos.models.printer.info.PrintStatus
import com.interswitchng.smartpos.models.transaction.CardReadTransactionResponse
import com.interswitchng.smartpos.models.transaction.PaymentInfo
import com.interswitchng.smartpos.models.transaction.PaymentType
import com.interswitchng.smartpos.models.transaction.cardpaycode.CardType
import com.interswitchng.smartpos.models.transaction.cardpaycode.EmvMessage
import com.interswitchng.smartpos.models.transaction.cardpaycode.EmvResult
import com.interswitchng.smartpos.models.transaction.cardpaycode.request.AccountType
import com.interswitchng.smartpos.models.transaction.cardpaycode.response.OfflineTerminalSettingsResponse
import com.interswitchng.smartpos.models.transaction.ussdqr.response.Bank
import com.interswitchng.smartpos.models.transaction.ussdqr.response.CodeResponse
import com.interswitchng.smartpos.models.transaction.ussdqr.response.PaymentStatus
import com.interswitchng.smartpos.shared.models.transaction.CardLessPaymentInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

class AppViewModel(private val appRepository: AppRepository): ViewModel() {
    private val channel = Channel<EmvMessage>()

    private val job = Job()
    val uiScope = CoroutineScope(Dispatchers.Main + job)
    val ioScope = uiScope.coroutineContext + Dispatchers.IO

    // communication channel with cardreader
    // this uses coroutine channels to update messages

    // card removed flag
    private var cardRemoved = false

    private val _emvMessage = MutableLiveData<EmvMessage>()
    val emvMessage: LiveData<EmvMessage> = _emvMessage

    private val _cardTransactionResponse = MutableLiveData<CardReadTransactionResponse>()
    val cardReadTransactionResponse :LiveData<CardReadTransactionResponse> = _cardTransactionResponse

    private val _offlineSettingsResponse = MutableLiveData<OfflineTerminalSettingsResponse>()
    val offlineSettingsResponse :LiveData<OfflineTerminalSettingsResponse> = _offlineSettingsResponse


    private val _accountValidationResponse = MutableLiveData<NameEnquiryResult<BeneficiaryModel>>()
    val accountValidationResponse :LiveData<NameEnquiryResult<BeneficiaryModel>> = _accountValidationResponse


    private val _virtualAccountResponse = MutableLiveData<CodeResponse>()
    val virtualAccountResponse :LiveData<CodeResponse> = _virtualAccountResponse

    private val _ussdTransactionResponse = MutableLiveData<CodeResponse>()
    val ussdTransactionResponse :LiveData<CodeResponse> = _ussdTransactionResponse





    private val _bankList = MutableLiveData<List<Bank>>()
    val bankList :LiveData<List<Bank>> = _bankList

    private val _paymentStatusResponse = MutableLiveData<PaymentStatus>()
    val paymentStatusResponse :LiveData<PaymentStatus> = _paymentStatusResponse

    private val _printerMessage = MutableLiveData<String>()
    val printerMessage: LiveData<String> get() = _printerMessage


    private val _terminalConfig = MutableLiveData<TerminalInfo>()
    val terminalConfig: LiveData<TerminalInfo> get() = _terminalConfig

    private val _keysDownloadSuccess = MutableLiveData<Boolean>()
    val keysDownloadSuccess: LiveData<Boolean> = _keysDownloadSuccess


    fun readCard(amount: Int, terminalInfo: TerminalInfo) {
        with(uiScope) {
            // launch job in IO thread to listen for messages
            launch(ioScope) {
                // listen and  publish all received messages
                for (message in channel) {
                    cardRemoved = cardRemoved || message is EmvMessage.CardRemoved
                    _emvMessage.postValue(message)
                }
            }

            // trigger transaction setup in IO thread
            launch(ioScope) {
                // setup transaction
                appRepository.setupTransaction(amount, terminalInfo, uiScope, channel)
            }
        }
    }

    fun startTransaction() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val result = appRepository.startTransaction(this)
                withContext(Dispatchers.Main) {
                    when (result) {
                        EmvResult.CANCELLED -> {
                            val reason = "Error processing card transaction"
                            _emvMessage.value = EmvMessage.TransactionCancelled(-1, reason)
                        }
                        EmvResult.OFFLINE_APPROVED -> {
                            _emvMessage.value = EmvMessage.ProcessingTransaction
                        }
                        EmvResult.OFFLINE_DENIED -> {
                            println("Transaction canceled")
                        }
                        EmvResult.ONLINE_REQUIRED -> {
                            _emvMessage.value = EmvMessage.ProcessingTransaction
                        }
                    }
                }

            }
        }
    }

    fun startClssTransaction() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val result = appRepository.startClssTransaction(this)
            }
        }
    }

    fun appSelected(index: Int) {
        viewModelScope.launch {
            channel.send(EmvMessage.AppSelected(index))
        }
    }


    fun performTransfer(paymentModel: PaymentModel,
                        accountType: AccountType,
                        terminalInfo: TerminalInfo,
                        destinationAccountNumber: String,
                        receivingInstitutionId: String?)
    {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val result = appRepository.performTransfer(paymentModel, accountType, terminalInfo, destinationAccountNumber, receivingInstitutionId)
                withContext(Dispatchers.Main) {
                    _cardTransactionResponse.postValue(result)
                }
            }
        }
    }

    fun performWithdraw(paymentModel: PaymentModel,
                        accountType: AccountType,
                        terminalInfo: TerminalInfo)
    {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val result = appRepository.performWithDrawal(paymentModel, accountType, terminalInfo)
                withContext(Dispatchers.Main) {
                    _cardTransactionResponse.postValue(result)
                }
            }
        }
    }

    fun performBalanceInquiry(
                        accountType: AccountType,
                        terminalInfo: TerminalInfo,
                        cardType: CardType
    )
    {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val result = appRepository.performBalanceInquiry(accountType, terminalInfo, cardType)
                withContext(Dispatchers.Main) {
                    _cardTransactionResponse.postValue(result)
                }
            }
        }
    }

    fun loadVirtualAccountDetails(
           request: CardLessPaymentRequest
    )
    {
        println("got here in viewmodel for virtual account")
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val result = appRepository.loadVirtualAccountDetails(request)
                withContext(Dispatchers.Main) {
                    println("virtual account details => ${result}")
                    _virtualAccountResponse.postValue(result)
                }
            }
        }
    }

    fun initiateUssdTransaction(
            request: CardLessPaymentRequest
    )
    {
        println("got here in viewmodel for ussd transaction")
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val result = appRepository.initiateUssdTransaction(request)
                withContext(Dispatchers.Main) {
                    println("virtual account details => ${result}")
                    _ussdTransactionResponse.postValue(result)
                }
            }
        }
    }


    fun initiatQrTransaction(
            request: CardLessPaymentRequest, context: Context
    )
    {
        println("got here in viewmodel for QR transaction")
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val result = appRepository.initiateQrTransaction(request, context)
                withContext(Dispatchers.Main) {
                    println("Qr  details => ${result?.qrCodeImage}")
                    _ussdTransactionResponse.postValue(result)
                }
            }
        }
    }

    fun initiatePaycodeTransaction(
            terminalInfo: TerminalInfo,
            paymentInfo: CardLessPaymentInfo, code: String
    )
    {
        println("got here in viewmodel for paycode transaction")
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val result = appRepository.initiatePaycodeTransaction(terminalInfo, paymentInfo, code)
                withContext(Dispatchers.Main) {
                    println("paycode details => ${result}")
                }
            }
        }
    }

    fun listBanks()
    {
        println("got here in viewmodel for list banks")
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val result = appRepository.listBanks()
                withContext(Dispatchers.Main) {
                    println("cardless banks => ${result}")
                    _bankList.postValue(result)
                }
            }
        }
    }

    fun checkPaymentStatus(
            reference: String,
            type: PaymentType,
            merchantCode: String
    )
    {
        println("got here in viewmodel for check payment status")
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val status = appRepository.checkPayment(transactionReference = reference,
                        merchantCode = merchantCode, paymentType = type)
                withContext(Dispatchers.Main) {
                    println(" => ${status}")
                    _paymentStatusResponse.postValue(status)
                }
            }
        }
    }


    fun performofflineDeposit(
            amount: String,
            terminalInfo: TerminalInfo,
            accountNumber: String
    )
    {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val result = appRepository.performOfflineDeposit(terminalInfo, amount, accountNumber)
                withContext(Dispatchers.Main) {
                    println("offline deposit result => $result")
                }
            }
        }
    }

    fun performGetOfflineDepositSettings(
            terminalInfo: TerminalInfo
    )
    {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val result = appRepository.performGetOfflineSettings(terminalInfo)
                withContext(Dispatchers.Main) {
                    println("offline deposit result => $result")
                    if (result?.responseCode == "Success") {
                        _offlineSettingsResponse.postValue(result)
                    }
                }
            }
        }
    }

    fun cancelTransaction() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                appRepository.cancelTransaction(this)
            }
        }
    }

    fun downloadParamsAndKey() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val res = appRepository.downloadKimParams()
                withContext(Dispatchers.Main) {
                    _terminalConfig.postValue(res)
                }
            }
        }
    }


    fun downloadNibss(terminalId: String, ip: String, port: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val res = appRepository.downloadNibsParams(terminalId, ip, port)
                withContext(Dispatchers.Main) {
                    _terminalConfig.postValue(res)
                }
            }
        }
    }

    fun downloadKey(terminalInfo: TerminalInfo, isNibss: Boolean) {
        println("info: $terminalInfo")
         viewModelScope.launch {
            withContext(Dispatchers.IO) {
                var resp = appRepository.downloadKeys(terminalInfo, isNibss)
                if (resp != null) {
                   _keysDownloadSuccess.postValue(resp)
                }
            }
        }
    }

    fun downloadNibssKey(terminalInfo: TerminalInfo, isNibss: Boolean) {
        println("info: $terminalInfo")
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                var resp = appRepository.downloadKeysNibss(terminalInfo, isNibss)
                if (resp != null) {
                    _keysDownloadSuccess.postValue(resp)
                }
            }
        }
    }

    fun getToken(terminalInfo: TerminalInfo) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                appRepository.getToken(terminalInfo)
            }
        }
    }

    fun printSlipNew(bitmap: Bitmap) {

        uiScope.launch {
            // get printer status on IO thread
            val printStatus = withContext(ioScope) {
                appRepository.canPrint()
            }

            when (printStatus) {
                is PrintStatus.Error -> {
                    _printerMessage.value = printStatus.message
                }
                else -> {
                    // print receipt in IO thread
                    val status = withContext(ioScope) { appRepository.printSlipNew(bitmap) }
                    // publish print message
                    _printerMessage.value = status.message
                }
            }
        }
    }




    fun validateBeneficiary(bankCode: String, accountNumber: String)
    {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val result = appRepository.validateBeneficiary(bankCode, accountNumber)
                withContext(Dispatchers.Main) {
                    _accountValidationResponse.postValue(result)
                }
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }


}