package com.interswitchng.interswitchpos.views.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.interswitchng.interswitchpos.R
import com.interswitchng.interswitchpos.databinding.FragmentCardTransactionBinding
import com.interswitchng.interswitchpos.utils.*
import com.interswitchng.interswitchpos.views.viewmodels.AppViewModel
import com.interswitchng.smartpos.IswPos
import com.interswitchng.smartpos.IswTxnHandler
import com.interswitchng.smartpos.models.core.TerminalInfo
import com.interswitchng.smartpos.models.posconfig.PosType
import com.interswitchng.smartpos.models.transaction.cardpaycode.CardType
import com.interswitchng.smartpos.models.transaction.cardpaycode.EmvMessage
import com.interswitchng.smartpos.models.transaction.cardpaycode.request.AccountType
import com.interswitchng.smartpos.shared.utilities.DialogUtils
import org.koin.android.viewmodel.ext.android.viewModel

class CardTransactionFragment : Fragment() {

    private var accountType = AccountType.Default
    private lateinit var binding: FragmentCardTransactionBinding
    private var cardType = CardType.None

    private val terminalInfo: TerminalInfo by lazy {
        IswTxnHandler().getTerminalInfo()!!
    }

    private lateinit var accountTypeDialog: AccountTypeFragment


    private val alert by lazy { DialogUtils.getAlertDialog(requireContext()).create() }

    private lateinit var loading: Dialog

    private val args by navArgs<CardTransactionFragmentArgs>()
    private val paymentType by lazy { args.paymentType }
    private val amount by lazy { args.amount }

    private val viewmodel: AppViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        loading = Dialog(this.requireContext())
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_card_transaction, container, false)
        if (IswPos.isConfigured()) {
            terminalInfo?.let {
                viewmodel.readCard(amount.toInt(), it)
                observeViewModel()
            }
        } else {
           Toast.makeText(requireContext(), "POS not configured", Toast.LENGTH_LONG).show()
        }
        return binding.root
    }

    private fun observeViewModel() {
        with(viewmodel) {
            emvMessage.observe(viewLifecycleOwner, Observer {
                it?.let{
                    processMessage(it)
                }
            })
        }
    }

    private fun processMessage(message: EmvMessage) {

        // assigns value to ensure the when expression is exhausted
        val ignore = when (message) {

            // when card is detected
            is EmvMessage.CardDetected -> {
                println("me: CardDetected")
//                showCardDetectedView(false)

//                loading = customdailog(requireContext(), "Please wait while we load card")
            }

            // when card should be inserted
            is EmvMessage.InsertCard -> {
                Toast.makeText(requireContext(), "Please tap your card", Toast.LENGTH_LONG).show()
            }

            is EmvMessage.ChooseApplication -> {
                val candidateList = message.values
                var options = Array<String>(candidateList.size){"$it"}
                candidateList.forEachIndexed { index, candidateAID ->
                    options[index] = String(candidateAID?.appName!!)
                 }
                val builder = MaterialAlertDialogBuilder(context)
                builder.setTitle("choose an application")

                builder.setSingleChoiceItems(options, -1){dialog, i ->
                    Toast.makeText(context, "I selected $i", Toast.LENGTH_SHORT).show()
                    viewmodel.appSelected(i)
                    dialog.dismiss()
                }

                builder.show()

                Toast.makeText(context, "Please select an application", Toast.LENGTH_SHORT).show()
            }
            // when error is trown from clss

            is EmvMessage.ClssTransError -> {
                if (loading.isShowing) {
                    loading.dismiss()
                }
                Toast.makeText(requireContext(), message.error, Toast.LENGTH_LONG).show()
            }

            // when card has been read
            is EmvMessage.CardRead -> {
                //Dismiss the dialog showing "Reading Card"
                if (loading.isShowing) {
                    loading.dismiss()
                }

                println("I have read the card")
                cardType = message.cardType

                //Show Card detected view
                showCardDetectedView(false)
            }

            is EmvMessage.ClssCardDetected -> {
                //Dismiss the dialog showing "Reading Card"
                if (loading.isShowing) {
                    loading.dismiss()
                }

                //Show Card detected view
                showCardDetectedView(true)
            }

            // card details is for telpo terminals
            is EmvMessage.CardDetails -> {
                cardType = message.cardType
            }

            // when card gets removed
            is EmvMessage.CardRemoved -> {
                showInsertCardView()
                cancelTransaction("Transaction Cancelled: Card was removed")
                Toast.makeText(requireContext(), "Transaction Cancelled: Card was removed", Toast.LENGTH_LONG).show()
            }
            // when user should enter pin
            is EmvMessage.EnterPin -> {
                println(message)
//                loading.dismiss()
                binding.iswCardPaymentViewAnimator.displayedChild = 1
                binding.iswPinFragmentTransfer.iswAmount.text = formatString(amount.toInt() / 100)
                Toast.makeText(requireContext(), "Enter Your Pin", Toast.LENGTH_LONG).show()
            }

            // when user types in pin
            is EmvMessage.PinText -> {
                binding.iswPinFragmentTransfer.cardPin.setText(message.text)
            }

            // when pin has been validated
            is EmvMessage.PinOk -> {
                println("Called PIN OKAY")
                toast(requireContext(),"Pin OK")
            }

            // when the user enters an incomplete pin
            is EmvMessage.IncompletePin -> {

                alert.setTitle("Invalid Pin")
                alert.setMessage("Please press the CANCEL (X) button and try again")
                alert.show()
            }

            // when the user enters an incomplete pin
            is EmvMessage.EmptyPin -> {
                alert.setTitle("Empty Pin")
                alert.setMessage("Please press the CANCEL (X) button and try again")
                alert.show()
            }

            // when pin is incorrect
            is EmvMessage.PinError -> {
                alert.setTitle("Invalid Pin")
                alert.setMessage("Please ensure you put the right pin.")
                alert.show()

                val isPosted = Handler().postDelayed({ alert.dismiss() }, 3000)
            }

            // when user cancels transaction
            is EmvMessage.TransactionCancelled -> {
                println("canceled")
                loading.dismiss()
                cancelTransaction(message.reason)
            }

            // when transaction is processing
            is EmvMessage.ProcessingTransaction -> {
                println("got here")
              val direction = CardTransactionFragmentDirections.actionCardTransactionFragmentToProcessingFragment(
                      accountType, CardType.MASTER, amount
              )
              findNavController().navigate(direction)
            }
            else -> {

            }
        }
    }

        private fun showInsertCardView() {
            binding.iswScanningCardTransfer.show()
            binding.iswCardFoundTransfer.hide()
        }

    private fun cancelTransaction(reason: String) {
        viewmodel.cancelTransaction()
    }

    private fun showCardDetectedView(isClss: Boolean) {
        //Hide Scanning Card View
        binding.iswScanningCardTransfer.hide()

        //Show Card Detected View
        binding.iswCardFoundTransfer.show()

        //Show account dialog

        showAccountTypeDialog(isClss)


    }

    private fun showAccountTypeDialog(isClss: Boolean) {
        if (isClss) {
            accountType = AccountType.Default
            IswTxnHandler().runWithInternet(requireContext()) {
                // this is where the card pin is validated
                viewmodel.startClssTransaction()
            }
        } else {
            accountTypeDialog = AccountTypeFragment {
                accountType = when (it) {
                    0 -> AccountType.Default
                    1 -> AccountType.Savings
                    2 -> AccountType.Current
                    else -> AccountType.Default
                }

                    IswTxnHandler().runWithInternet(requireContext()) {
                        // this is where the card pin is validated
                        viewmodel.startTransaction()
                    }

            }
            accountTypeDialog.show(childFragmentManager, "account_type")
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = CardTransactionFragment()
    }
}