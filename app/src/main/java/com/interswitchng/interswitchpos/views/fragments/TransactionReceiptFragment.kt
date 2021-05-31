package com.interswitchng.interswitchpos.views.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.interswitchng.interswitchpos.R
import com.interswitchng.interswitchpos.databinding.FragmentTransactionReceiptBinding
import com.interswitchng.interswitchpos.utils.getAmountWithCurrency
import com.interswitchng.interswitchpos.views.viewmodels.AppViewModel
import com.interswitchng.smartpos.IswTxnHandler
import com.interswitchng.smartpos.models.transaction.cardpaycode.CardType
import com.interswitchng.smartpos.shared.utilities.*
import kotlinx.coroutines.*
import org.koin.android.viewmodel.ext.android.viewModel
import java.util.*

class TransactionReceiptFragment : Fragment() {
    private lateinit var binding: FragmentTransactionReceiptBinding
    private val job = Job()


    private val receiptFragmentArgs by navArgs<TransactionReceiptFragmentArgs>()

    val data by lazy { receiptFragmentArgs.transactionResponseModel.transactionResult }

    private val terminalInfo by lazy {
        IswTxnHandler().getTerminalInfo()
    }

    private val viewmodel : AppViewModel by viewModel()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_transaction_receipt, container, false)
        handleprint()
        listenToviewModel()
        setupUI()
        return binding.root
    }

    private fun handleprint() {
        Timer().schedule(object : TimerTask() {
            override fun run() {
                this@TransactionReceiptFragment.requireActivity().runOnUiThread {
                    doPrinting()
                }
            }

        }, 1000)
    }

    private fun doPrinting() {
        if (receiptFragmentArgs.withAgent) {
            val scope = CoroutineScope(Dispatchers.Main + job)
            scope.launch {
                getScreenBitMap(this@TransactionReceiptFragment.requireActivity(),
                        binding.rootViewForPrintPage)?.let { viewmodel.printSlipNew(it) }
                delay(3000L)
                binding.customerTitle.text = "*** MERCHANT COPY ***"
                delay(1000L)
                getScreenBitMap(this@TransactionReceiptFragment.requireActivity(),
                        binding.rootViewForPrintPage)?.let { viewmodel.printSlipNew(it) }
            }
        } else {
            getScreenBitMap(this@TransactionReceiptFragment.requireActivity(),
                    binding.rootViewForPrintPage)?.let { viewmodel.printSlipNew(it) }
        }

    }
    private fun listenToviewModel() {
        val owner = { lifecycle }
        with(viewmodel) {
            printerMessage.observe(owner) {
                showSnack(binding.agentTitle, it.toString())
            }
        }
    }

    private fun setupUI() {
        binding.agentValue.text = terminalInfo?.merchantNameAndLocation
        binding.terminalIdTitle.text = "TERMINAL ID: ${terminalInfo?.terminalId}"
        binding.telTitle.text = "TEL: ${terminalInfo?.agentId}"
        binding.withdrawTitle.text = data?.type?.name
        binding.channelTitle.text = "CHANNEL: ${data?.paymentType?.name}"
        binding.dateTitle.text = "DATE: ${getDate(data?.dateTime.toString())}"
        binding.timeTitle.text = "TIME: ${getTime(data?.dateTime.toString())}"
        var amount = data?.amount.toString()
        binding.amountTitle.text = getHtmlString("AMOUNT: ${amount.let { getAmountWithCurrency(it.toString(), terminalInfo!!) }}")

        val cardTypeName = when (data?.cardType) {
            CardType.MASTER -> "Master Card"
            CardType.VISA -> "Visa Card"
            CardType.VERVE -> "Verve Card"
            CardType.AMERICANEXPRESS -> "American Express Card"
            CardType.CHINAUNIONPAY -> "China Union Pay Card "
            CardType.None -> "Unknown Card"
            else -> "Unknown Card"
        }
        binding.cardTitle.text = "CARD TYPE: ${cardTypeName}"
        binding.panTitle.text = "CARD PAN: ${mask(data?.cardPan.toString())}"
        binding.expiryDateTitle.text = "EXPIRY DATE: ${formatExpiryDate(data?.cardExpiry.toString(), "/")}"
        binding.stanTitle.text  = "STAN: ${data?.getTransactionInfo()?.stan?.padStart(6, '0')}"
        binding.aidTitle.text = "AID: ${data?.AID}"

        if (data?.getTransactionStatus()?.responseCode.toString() == "00") {
            binding.retainReceiptTitle.reveal()
            binding.transactionApprovedTitle.text = "${data?.getTransactionStatus()?.responseMessage}"
        } else {
            binding.transactionApprovedTitle.text = "${data?.getTransactionStatus()?.responseMessage}"
        }
        binding.refTitle.text = "REF: ${data?.ref}"

        if (receiptFragmentArgs.rePrint) {
            binding.customerTitle.text = "*** MERCHANT COPY ***"
            binding.reprintTitle.reveal()
            binding.reprintTitle.reveal()
            binding.lineAfterReprint1.reveal()
            binding.lineBeforeReprint2.reveal()
            binding.iswReprintBtn.reveal()
        }

        binding.iswGoToLanding.setOnClickListener {
            findNavController().popBackStack(R.id.home, false)
        }

        binding.iswReprintBtn.setOnClickListener {
            val scope = CoroutineScope(Dispatchers.Main + job)
            scope.launch {
                binding.iswReprintBtn.hide()
                delay(100L)
                doPrinting()
                delay(100L)
                binding.iswReprintBtn.reveal()
            }
        }
    }
    companion object {

        @JvmStatic
        fun newInstance() = TransactionReceiptFragment()
    }
}