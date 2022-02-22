package com.interswitchng.interswitchpos.views.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.interswitchng.interswitchpos.R
import com.interswitchng.interswitchpos.databinding.FragmentProcessingBinding
import com.interswitchng.interswitchpos.utils.toast
import com.interswitchng.interswitchpos.views.viewmodels.AppViewModel
import com.interswitchng.smartpos.IswTxnHandler
import com.interswitchng.smartpos.models.PaymentModel
import com.interswitchng.smartpos.models.TransactionResponseModel
import com.interswitchng.smartpos.models.core.TerminalInfo
import com.interswitchng.smartpos.models.transaction.CardOnlineProcessResult
import com.interswitchng.smartpos.models.transaction.CardReadTransactionResponse
import com.interswitchng.smartpos.models.transaction.PaymentType
import com.interswitchng.smartpos.models.transaction.cardpaycode.CardType
import com.interswitchng.smartpos.models.transaction.cardpaycode.request.AccountType
import com.interswitchng.smartpos.shared.Constants
import com.interswitchng.smartpos.shared.services.iso8583.utils.IsoUtils
import com.pixplicity.easyprefs.library.Prefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.koin.android.viewmodel.ext.android.viewModel

class ProcessingFragment : Fragment() {

    private lateinit var binding: FragmentProcessingBinding
    private val args by navArgs<ProcessingFragmentArgs>()

    private val accountType by lazy { args.accountType }
    private val amount by lazy { args.amount }
    private val cardType by lazy { args.cardType }
//    private val accountType  = AccountType.Default
//    private val cardType = CardType.VERVE
//    private val amount = "1"

    private lateinit var  paymentModel : PaymentModel

    private val viewmodel: AppViewModel by viewModel()

    private val terminalInfo by lazy {
        IswTxnHandler().getTerminalInfo()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_processing, container, false)
        paymentModel = PaymentModel(
                cardType = cardType,
                amount = amount.toInt(),
                paymentType = PaymentType.Card,
                type = PaymentModel.TransactionType.TRANSFER)
        binding.statusText.text = "PROCESSING...."
        observeViewModel()

        IswTxnHandler().runWithInternet(requireContext()) {
//            terminalInfo?.let { viewmodel.performTransfer(paymentModel, accountType, it, "2089430464", "627629") }
            terminalInfo?.let { viewmodel.performTransfer(paymentModel, accountType, it, Prefs.getString(Constants.SETTLEMENT_ACCOUNT_NUMBER, ""),
                    Prefs.getString(Constants.SETTLEMENT_BANK_CODE, "")) }

//            terminalInfo?.let { viewmodel.performBalanceInquiry(accountType, it, cardType) }
//            terminalInfo?.let { viewmodel.performWithdraw(paymentModel, accountType, it) }

        }
        return binding.root
    }

    private fun observeViewModel() {
        with(viewmodel) {
            cardReadTransactionResponse.observe(viewLifecycleOwner, Observer {
                processResponse(it)
            })
        }
    }

    private fun processResponse(response: CardReadTransactionResponse) {
        when (response.transactionResponse) {
            null -> {
                toast(requireContext(), "unable to complete transaction")
            }

            else -> {

                    var result = response.transactionResult
                    val statusModel = TransactionResponseModel(
                            transactionResult = result,
                            transactionType = paymentModel.type!!
                    )
                    println(response.transactionResponse)
                    println(result)

                runBlocking {
                    delay(2000)
                }
                val action = ProcessingFragmentDirections.actionProcessingFragmentToTransactionStatusFragment(
                        statusModel, paymentModel
                )
                findNavController().navigate(action)


            }
        }

        when (response.onlineProcessResult) {
            CardOnlineProcessResult.NO_EMV -> {
                toast(requireContext(), "Unable to get result ICC")
            }
            CardOnlineProcessResult.NO_RESPONSE -> {
                toast(requireContext(), "Unable to process transaction")
            }
            CardOnlineProcessResult.ONLINE_DENIED -> {
                toast(requireContext(), "Transaction denied")
            }
            CardOnlineProcessResult.ONLINE_APPROVED -> {

                binding.statusText.text = "CONNECTING...."

                runBlocking {
                    delay(1000)
                }

                binding.statusText.text = "RETURNING...."

                runBlocking {
                    delay(1000)
                }

                binding.statusText.text = "FINISHED...."

            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = ProcessingFragment()
    }
}