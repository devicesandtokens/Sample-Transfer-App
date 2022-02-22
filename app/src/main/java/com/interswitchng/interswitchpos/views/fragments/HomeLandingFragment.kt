package com.interswitchng.interswitchpos.views.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.interswitchng.interswitchpos.R
import com.interswitchng.interswitchpos.databinding.FragmentHomeLandingBinding
import com.interswitchng.interswitchpos.utils.showSnack
import com.interswitchng.interswitchpos.utils.toast
import com.interswitchng.interswitchpos.views.viewmodels.AppViewModel
import com.interswitchng.smartpos.IswTxnHandler
import com.interswitchng.smartpos.models.cardless.CardLessPaymentRequest
import com.interswitchng.smartpos.models.transaction.ussdqr.response.Bank
import com.interswitchng.smartpos.models.transaction.ussdqr.response.CodeResponse
import com.interswitchng.smartpos.models.transaction.ussdqr.response.PaymentStatus
import com.interswitchng.smartpos.shared.models.transaction.CardLessPaymentInfo
import org.koin.android.viewmodel.ext.android.viewModel

class HomeLandingFragment : Fragment() {

    private val viewmodel : AppViewModel by viewModel()
    private lateinit var binding: FragmentHomeLandingBinding
    private lateinit var transferRequest: CardLessPaymentRequest
    private lateinit var account : CodeResponse
    private lateinit var bankList: List<Bank>

    private val terminalInfo by lazy {
        IswTxnHandler().getTerminalInfo()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_home_landing, container, false)
        if (terminalInfo != null) {
            viewmodel.getToken(terminalInfo!!)
//            loadVirtualAccountDetails()
            loadBanks()
        }

        binding.iswTransferCard.setOnClickListener {
//            val action = HomeLandingFragmentDirections.actionHomeToCardTransactionFragment(amount = "5", paymentType = PaymentType.TRANSFER.name)
            val action = HomeLandingFragmentDirections.actionHomeToAmountFragment2()
            findNavController().navigate(action)
        }


        binding.checkPaymentBtn.setOnClickListener {
            println("this is getting here for check payment")
            if (this::account.isInitialized) {
                println("coderesponse => $account")
                viewmodel.checkPaymentStatus(reference = account.transactionReference.toString(),
                        type = com.interswitchng.smartpos.models.transaction.PaymentType.QR,
                        merchantCode = terminalInfo?.merchantCode.toString())

                viewmodel.paymentStatusResponse.observe(viewLifecycleOwner, {
                    it.let {
                        handlePaymentStatus(it)
                    }
                })
            }

        }

        binding.startUssd.setOnClickListener {
            if (this::bankList.isInitialized) {
                if (bankList.isNotEmpty()) {
                    println("bank => ${bankList.get(0).code}  ${bankList.get(0).name}")
                    var paymentInfo = CardLessPaymentInfo(amount = 100, "", surcharge = 0, additionalAmounts = 0)
//                    var request = CardLessPaymentRequest.from(
//                            terminalInfo!!,
//                            paymentInfo,
//                            CardLessPaymentRequest.TRANSACTION_USSD,
//                            bankCode = bankList.get(0).code
//                    )

//                    var qrRequest = CardLessPaymentRequest.from(
//                            terminalInfo!!,
//                            paymentInfo,
//                            CardLessPaymentRequest.TRANSACTION_QR,
//                            qrFormat = CardLessPaymentRequest.QR_FORMAT_RAW
//                    )
//                  viewmodel.initiateUssdTransaction(request)
//
//                     viewmodel.initiatQrTransaction(qrRequest, this.requireContext())
                    viewmodel.initiatePaycodeTransaction(terminalInfo = terminalInfo!!,
                        paymentInfo = paymentInfo, code = "131490085")

                    viewmodel.ussdTransactionResponse.observe(viewLifecycleOwner, {
                        it.let {
                            if (it !=  null) {
                                val dxl = AlertDialog.Builder(this.requireContext())
                                dxl.setTitle("TRANSACTION")
                                dxl.setMessage("${it.bankShortCode}")
                                dxl.setIcon(BitmapDrawable(it.qrCodeImage))
                                dxl.create().show()

                                account = it
                            }
                        }
                    })
                }
            } else {
                Toast.makeText(this.requireContext(), "bank list not initialized", Toast.LENGTH_LONG).show()
            }
        }

        val level = IswTxnHandler().getBatterLevel(requireContext())
       showSnack(binding.iswCashOutText, "Battery Level is: $level")
        return binding.root
    }


    fun loadVirtualAccountDetails() {

        println("got here for load virtual account")
        // create and request code
        var paymentInfo = CardLessPaymentInfo(amount = 100, "", surcharge = 0, additionalAmounts = 0)
        transferRequest = CardLessPaymentRequest.from(
                terminalInfo!!,
                paymentInfo,
                CardLessPaymentRequest.TRANSACTION_TRANSFER
        )

        // load account number if not loaded
            viewmodel.loadVirtualAccountDetails(transferRequest)

            viewmodel.virtualAccountResponse.observe(viewLifecycleOwner, {
                it.let {
                    if (it != null) {
                        account = it
                    }
                }
            })

    }

    fun loadBanks() {
        viewmodel.listBanks()
        viewmodel.bankList.observe(viewLifecycleOwner, {
            it.let {
                if (it != null) {
                    bankList = it
                }
            }
        })
    }

    fun handlePaymentStatus(status: PaymentStatus, customerName: String = "") {
        // clear current loading indicator
        if (status !is PaymentStatus.Pending) toast(this.requireContext(), "pending")

        // else handle status
        when (status) {

            is PaymentStatus.Complete -> {
                // get the transaction result
//                println("Payment status => ${status.transaction}")
//                status.transaction.customerName = customerName ?: ""
                toast(this.requireContext(), status.transaction.responseDescription.toString())
                // set and complete payment
            }

            // do nothing for ongoing poll, show loading indicator
            is PaymentStatus.OngoingTimeout -> {}

            is PaymentStatus.Timeout -> {
                val title = "Payment not Confirmed"
                val message = "Unable to confirm payment at the moment, please try again in 1 minute."

                // show alert dialog
                AlertDialog.Builder(requireContext())
                        .setTitle(title)
                        .setMessage(message)
                        .create().show()
            }

            is PaymentStatus.Error -> {
                // get alert title
                val title =
                        if (status.errorMsg != null) "Network Error"
                        else getString(R.string.isw_title_transaction_error)

                // getResult error message
                val message = status.errorMsg
                        ?: status.transaction?.responseDescription
                        ?: "An error occurred, please try again"


                // show alert dialog
                AlertDialog.Builder(requireContext())
                        .setTitle(title)
                        .setMessage(message)
                        .create().show()
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = HomeLandingFragment()
    }
}