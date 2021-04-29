package com.interswitchng.interswitchpos.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.*
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView
import com.interswitchng.interswitchpos.databinding.CustomDialogBinding
import com.interswitchng.smartpos.R
import com.interswitchng.smartpos.models.core.IswLocal
import com.interswitchng.smartpos.models.core.TerminalInfo
import com.interswitchng.smartpos.shared.utilities.Logger
import org.koin.standalone.get
import java.text.NumberFormat

fun customdailog(context: Context?, message: String? = ",", action: (() -> Unit?)? = null): Dialog {
    val dialog = Dialog(context!!, R.style.Theme_AppCompat)
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    val customProgressBinding = DataBindingUtil.inflate<CustomDialogBinding>(LayoutInflater.from(context), R.layout.custom_dialog, null, false)
    dialog.setContentView(customProgressBinding.root)
    dialog.setCanceledOnTouchOutside(false)
    dialog.setCancelable(false)
    val layoutParams = dialog.window!!.attributes
    layoutParams.dimAmount = 0.7f
    layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
    layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
    dialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    dialog.window!!.setGravity(Gravity.CENTER)
    dialog.window!!.attributes = layoutParams
    dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    customProgressBinding.iswDialogMessage.text = message
    dialog.show()
    return dialog
}

fun View.hideMe() {
    if (visibility == View.VISIBLE && alpha == 1f) {
        animate()
                .alpha(0f)
                .withEndAction { visibility = View.GONE }
    }
}

fun View.showMe() {
    if (visibility == View.GONE && alpha == 0f) {
        animate()
                .alpha(1f)
                .withEndAction { visibility = View.VISIBLE }
    }
}

fun View.hide() {
    this.visibility = View.GONE
}

fun View.show() {
    this.visibility = View.VISIBLE
}

 fun EditText.getString(): String {
    return this.text.toString()
}

/**
 * This method converts interger amount notation to string (e.g. 1000 -> 1,000.00)
 *
 * @param amount A value in integer representing the transaction amount
 * @return A string representation of the decimal notation for the amount
 */
fun formatString(amount: Int): String {

    val numberFormat = NumberFormat.getInstance()
    numberFormat.minimumFractionDigits = 2
    numberFormat.maximumFractionDigits = 2

    return numberFormat.format(amount)
}

fun toast(context: Context, message: String ) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}

fun showSnack(view: View, message: String) {
    Snackbar.make(view,message, Snackbar.LENGTH_LONG).show()
}

fun getAmountWithCurrency(amount: String, terminalInfo: TerminalInfo): String {

    // get the currency based on the terminal's configured currency code
    val currency = when (terminalInfo) {
        null -> ""
        else -> when(terminalInfo.currencyCode) {
            IswLocal.NIGERIA.code -> IswLocal.NIGERIA.currency
            IswLocal.GHANA.code -> IswLocal.GHANA.currency
            IswLocal.USA.code -> IswLocal.USA.currency
            else -> ""
        }
    }
    Logger.with("Display Utils").logErr( amount)
    var formattedAmount = formatString(amount.toInt())
    return "$formattedAmount $currency"
}