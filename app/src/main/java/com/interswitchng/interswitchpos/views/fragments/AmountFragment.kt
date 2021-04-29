package com.interswitchng.interswitchpos.views.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.interswitchng.interswitchpos.R
import com.interswitchng.smartpos.shared.Constants
import kotlinx.android.synthetic.main.fragment_amount.*
import java.text.NumberFormat
import com.interswitchng.smartpos.models.transaction.PaymentType

class AmountFragment : Fragment() {

    private val DEFAULT_AMOUNT = "0.00"
    private var amount = Constants.EMPTY_STRING
    private lateinit var paymentType: PaymentType
    private var formattedAmount = "0.00"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_amount, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpUI()
        initializeAmount()
        handleProceedToolbarClicks()
        handleDigitsClicks()
        paymentType = PaymentType.Card
    }

    private fun handleProceedToolbarClicks() {
        isw_proceed_transfer.setOnClickListener {
            if (amount == Constants.EMPTY_STRING || amount == DEFAULT_AMOUNT) {
                Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_LONG)
            } else {
                proceedWithPayment()
            }
        }
    }


    private fun initializeAmount() {
        amount = DEFAULT_AMOUNT
        isw_amount_transfer.text = amount
    }

    private fun setUpUI() {
//        DisplayUtils.hideKeyboard(this.requireActivity())
    }

    private fun handleDigitsClicks() {
        isw_keypad_zero_transfer.setOnClickListener {
            handleClickWithAmountLimit("0")
        }

        isw_keypad_one_transfer.setOnClickListener {
            handleClickWithAmountLimit("1")
        }

        isw_keypad_two_transfer.setOnClickListener {
            handleClickWithAmountLimit("2")
        }

        isw_keypad_three_transfer.setOnClickListener {
            handleClickWithAmountLimit("3")
        }

        isw_keypad_four_transfer.setOnClickListener {
            handleClickWithAmountLimit("4")
        }

        isw_keypad_five_transfer.setOnClickListener {
            handleClickWithAmountLimit("5")
        }

        isw_keypad_six_transfer.setOnClickListener {
            handleClickWithAmountLimit("6")
        }

        isw_keypad_seven_transfer.setOnClickListener {
            handleClickWithAmountLimit("7")
        }

        isw_keypad_eight_transfer.setOnClickListener {
            handleClickWithAmountLimit("8")
        }

        isw_keypad_nine_transfer.setOnClickListener {
            handleClickWithAmountLimit("9")
        }

        isw_dot_button_transfer.setOnClickListener {
            handleClickWithAmountLimit(".")
        }

        isw_back_delete_button_transfer.setOnClickListener {
            if (amount.isNotEmpty()) {
                amount = amount.substring(0 until amount.length - 1)
                updateAmount()
            }
        }

        isw_back_delete_button_transfer.setOnLongClickListener {
            amount = DEFAULT_AMOUNT
            isw_amount_text.text = amount
            true
        }
    }

    private fun handleClickWithAmountLimit(digit: String) {
        amount+= digit
        updateAmount()
    }

    private fun updateAmount() {
        val cleanString = amount.replace("[$,.]".toRegex(), "")

        val parsed = java.lang.Double.parseDouble(cleanString)
        val numberFormat = NumberFormat.getInstance()
        numberFormat.minimumFractionDigits = 2
        numberFormat.maximumFractionDigits = 2
        val formatted = numberFormat.format(parsed / 100)

        println("updated amount is $formatted")
        isw_amount_transfer.text = formatted
    }

    private fun proceedWithPayment() {
        val latestAmount = isw_amount_transfer.text.toString()
        val latestAmountWithoutComma = latestAmount.replace("[$,.]".toRegex(), "")
        amount = latestAmountWithoutComma.toInt().toString() //latestAmount.toDouble()
        formattedAmount = latestAmount

        println(amount)

        val direction = AmountFragmentDirections.actionAmountFragmentToCardTransactionFragment(amount, paymentType)
         findNavController().navigate(direction)
    }

    companion object {

        @JvmStatic
        fun newInstance(param1: String, param2: String) = AmountFragment()

    }

}