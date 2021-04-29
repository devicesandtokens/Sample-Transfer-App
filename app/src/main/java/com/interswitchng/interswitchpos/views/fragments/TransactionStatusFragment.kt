package com.interswitchng.interswitchpos.views.fragments

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.interswitchng.interswitchpos.R
import com.interswitchng.interswitchpos.databinding.FragmentTransactionStatusBinding
import com.interswitchng.interswitchpos.utils.getAmountWithCurrency
import com.interswitchng.interswitchpos.utils.hide
import com.interswitchng.interswitchpos.utils.showSnack
import com.interswitchng.interswitchpos.views.viewmodels.AppViewModel
import com.interswitchng.smartpos.IswTxnHandler
import com.interswitchng.smartpos.models.printer.slips.TransactionSlip
import com.interswitchng.smartpos.models.transaction.cardpaycode.CardType
import com.interswitchng.smartpos.shared.services.iso8583.utils.IsoUtils
import com.interswitchng.smartpos.shared.utilities.Logger
import com.interswitchng.smartpos.shared.utilities.reveal
import org.koin.android.viewmodel.ext.android.viewModel

class TransactionStatusFragment : Fragment() {

    private val receiptFragmentArgs by navArgs<TransactionStatusFragmentArgs>()
    private val transactionResponseModel by lazy { receiptFragmentArgs.transactionResponseModel }
    private val result by lazy { transactionResponseModel.transactionResult }
    private val paymentModel by lazy { receiptFragmentArgs.paymentModel }

    private var printSlip: TransactionSlip? = null
    private val viewModel: AppViewModel by viewModel()
    private lateinit var binding :FragmentTransactionStatusBinding

    private val terminalInfo by lazy {
        IswTxnHandler().getTerminalInfo()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_transaction_status, container, false)
        setUpUI()
        return binding.root
    }

//    private fun listenToviewModel() {
//        val owner = { lifecycle }
//        with(viewModel) {
//            printerMessage.observe(owner) {
//                showSnack(binding.iswAmountPaidLabelTransfer, it.toString())
//            }
//        }
//    }


    private fun setUpUI() {
        displayTransactionResultIconAndMessage()
        displayTransactionDetails()
//        logTransaction()
        displayButtons()
        handleClicks()
        handlePrint()
    }

    private fun handlePrint() {
        printSlip = terminalInfo.let { result?.getSlip(it!!) }

        binding.iswPrintReceiptTransfer.setOnClickListener {
            // print slip
            printSlip?.let {

                result?.let {
                    val action = TransactionStatusFragmentDirections.actionTransactionStatusFragmentToTransactionReceiptFragment(
                            transactionResponseModel
                    )
                    findNavController().navigate(action)
                }

            }
        }

        binding.iswPrintReceiptTransferAgent.setOnClickListener {
            // print slip
            printSlip?.let {



                result?.let {
                    val action = TransactionStatusFragmentDirections.actionTransactionStatusFragmentToTransactionReceiptFragment(
                            transactionResponseModel
                    )
                    findNavController().navigate(action)
                }

            }
        }
    }



    private fun handleClicks() {
        binding.transactionResponseIconTransfer.setOnClickListener {
            findNavController().popBackStack(R.id.home, false)
        }

        binding.iswGoToLanding.setOnClickListener {
            findNavController().popBackStack(R.id.home, false)
        }

        binding.iswShareReceiptTransfer.setOnClickListener {
            val shareIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, "")
                type = "image/*"
            }
            startActivity(Intent.createChooser(shareIntent, "Select Application"))
        }

    }

    private fun displayButtons() {
        binding.iswShareReceiptTransfer.hide()
        binding.iswPrintReceiptTransfer.reveal()
    }


    private fun displayTransactionDetails() {
        binding.iswDateTextTransfer.text = getString(com.interswitchng.smartpos.R.string.isw_receipt_date, result?.dateTime)
        val amountWithCurrency = result?.amount.let { getAmountWithCurrency(it.toString(), terminalInfo!!) }
        Logger.with("Reciept fragment").logErr(amountWithCurrency)
        Logger.with("Recipet fragment amount").logErr(result!!.amount)
        binding.iswAmountPaidLabelTransfer.text = getString(com.interswitchng.smartpos.R.string.isw_receipt_amount, amountWithCurrency)

        binding.iswStanTransfer.text = result?.stan?.padStart(6, '0')

        val cardTypeName = when (result?.cardType) {
            CardType.MASTER -> "Master Card"
            CardType.VISA -> "Visa Card"
            CardType.VERVE -> "Verve Card"
            CardType.AMERICANEXPRESS -> "American Express Card"
            CardType.CHINAUNIONPAY -> "China Union Pay Card "
            CardType.None -> "Unknown Card"
            else -> "Unknown Card"
        }

        binding.iswPaymentTypeTransfer.text = getString(com.interswitchng.smartpos.R.string.isw_receipt_payment_type, cardTypeName)
    }

    private fun displayTransactionResultIconAndMessage() {
        println("Called responseCode ----> ${result?.responseCode}")
        Logger.with("Reciept Fragment").logErr(result?.responseCode.toString())
        when (result?.responseCode) {
            IsoUtils.TIMEOUT_CODE -> {
                binding.transactionResponseIconTransfer.setImageResource(com.interswitchng.smartpos.R.drawable.isw_failure)
                binding.transactionstatusImage.setImageResource(com.interswitchng.smartpos.R.drawable.ic_not_successful)
                binding.iswGoToLanding.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this.requireContext(), com.interswitchng.smartpos.R.color.iswTextColorError))
                binding.iswReceiptTextTransfer.text = "Failed!"
                binding.iswTransactionMsgTransfer.text = "Your transaction was unsuccessful"
            }

            IsoUtils.OK -> {
                binding.transactionResponseIconTransfer.setImageResource(com.interswitchng.smartpos.R.drawable.isw_round_done_padded)
                binding.transactionstatusImage.setImageResource(com.interswitchng.smartpos.R.drawable.ic_finished)
                binding.iswGoToLanding.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this.requireContext(), com.interswitchng.smartpos.R.color.iswTextColorSuccessDark))
                binding.iswTransactionMsgTransfer.text = "Your transaction was successful"
                binding.iswReceiptTextTransfer.text =
                        getString(com.interswitchng.smartpos.R.string.isw_transfer_completed)
            }

            else -> {
                binding.transactionResponseIconTransfer.setImageResource(com.interswitchng.smartpos.R.drawable.isw_failure)
                binding.transactionstatusImage.setImageResource(com.interswitchng.smartpos.R.drawable.ic_not_successful)
                binding.iswGoToLanding.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this.requireContext(), com.interswitchng.smartpos.R.color.iswTextColorError))
                binding.iswReceiptTextTransfer.text = "Failed!"
                binding.iswTransactionMsgTransfer.text =
                        result?.responseMessage//"Your transaction was unsuccessful"
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = TransactionStatusFragment()
    }
}