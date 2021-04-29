package com.interswitchng.interswitchpos.views.fragments

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.interswitchng.interswitchpos.R
import com.interswitchng.interswitchpos.utils.BankFilterDialog
import com.interswitchng.interswitchpos.utils.customdailog
import com.interswitchng.interswitchpos.views.viewmodels.AppViewModel
import com.interswitchng.smartpos.models.BankModel
import com.interswitchng.smartpos.models.BeneficiaryModel
import com.interswitchng.smartpos.models.CallbackListener
import com.interswitchng.smartpos.models.NameEnquiryResult
import com.interswitchng.smartpos.shared.Constants
import com.interswitchng.smartpos.shared.utilities.getTextValue
import com.pixplicity.easyprefs.library.Prefs
import kotlinx.android.synthetic.main.fragment_account_setup.*
import org.koin.android.viewmodel.ext.android.viewModel
import java.util.*


/**
 * A simple [Fragment] subclass.
 * Use the [AccountSetupFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AccountSetupFragment : Fragment(), CallbackListener {

    private var bankList = arrayListOf<BankModel>()
    lateinit var _selectedBank: BankModel
    lateinit var submitButton: Button
    lateinit var accountNumberEditor: EditText
    lateinit var _beneficiaryPayload: BeneficiaryModel
    var isValid = false
    lateinit var dialog: Dialog
    var accountNumber = ""
    var useNameEnquiry = true

    private val appViewModel: AppViewModel by viewModel()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_account_setup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        setupUI()
    }

    private fun setupUI() {
        submitButton = isw_transfer_input_proceed
        accountNumberEditor = isw_transfer_input_account
        isw_transfer_input_bank.setOnClickListener {
            parentFragmentManager?.let { it1 -> BankFilterDialog(this).show(it1, "show-bank-filter") }
        }
        accountNumberEditor.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                Timer().schedule(object : TimerTask() {
                    override fun run() {
                        this@AccountSetupFragment.requireActivity().runOnUiThread {
                            validateBeneficiary()
                        }
                    }

                }, 300)
                accountNumber = s.toString()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
        })

        isw_transfer_input_proceed.setOnClickListener { view ->
            submitForm(view)
        }
    }



    private fun validateBeneficiary() {
        if (accountNumber?.length == 10) {
            appViewModel.validateBeneficiary(_selectedBank.selBankCodes.toString(), accountNumber)
        }

        if (accountNumber?.length == 10 && this::_selectedBank.isInitialized) {

            if(!useNameEnquiry) {
                _beneficiaryPayload = BeneficiaryModel()
                _beneficiaryPayload.accountName = isw_transfer_input_account_name.text.toString()
                _beneficiaryPayload.accountNumber = accountNumber.toString()
                isValid = !isw_transfer_input_account_name.getTextValue().isNullOrEmpty()
                validateInput()
                return
            }

            if (!this::dialog.isInitialized) {
                dialog = customdailog(this.requireContext())
            }else {
                dialog.show()
            }
            appViewModel.validateBeneficiary(_selectedBank.selBankCodes!!, accountNumber!!)
        } else {
            isValid = false
            toggleAccountNameVisibility()
            validateInput()
        }
    }

    private fun toggleAccountNameVisibility() {
        if (isValid || !useNameEnquiry) {
            account_name.visibility = View.VISIBLE
            outlinedNameTextField.visibility = View.VISIBLE
            isw_transfer_input_account_name.visibility = View.VISIBLE
            if(this::_beneficiaryPayload.isInitialized) isw_transfer_input_account_name.setText(_beneficiaryPayload.accountName)
        } else {
            account_name.visibility = View.GONE
            outlinedNameTextField.visibility = View.GONE
            isw_transfer_input_account_name.visibility = View.GONE
        }

//      make Account name input focusable based on the state of use name enquiry

    }

    private fun validateInput() {
        submitButton.alpha = if (!isValid) 0.5F else 1F
        submitButton.isEnabled = isValid
        submitButton.isClickable = isValid
    }

    private fun observeViewModel() {

        val owner = { lifecycle }

        with(appViewModel) {
            //observe the benefiary call
            accountValidationResponse.observe(owner) {
                it?.let { beneficiary ->
                    dialog.let { d->
                        d.dismiss()
                    }
                    if (beneficiary != null){
                        when(beneficiary) {
                            is NameEnquiryResult.Success -> {
                                _beneficiaryPayload = beneficiary.value
                                isValid = true
                            }
                            is NameEnquiryResult.Failure -> {
                                Toast.makeText(context, beneficiary.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Name enquiry error please check the fields and try again", Toast.LENGTH_SHORT).show()
                    }
                    toggleAccountNameVisibility()
                    validateInput()
                }
            }
        }
    }


    override fun onDataReceived(data: BankModel) {
        _selectedBank = data
        val bankText: EditText = isw_transfer_input_bank
        bankText.setText(data.bankName)
        validateBeneficiary()
    }

    private fun submitForm(view: View) {
        Prefs.putString(Constants.SETTLEMENT_ACCOUNT_NUMBER, accountNumber)
        Prefs.putString(Constants.SETTLEMENT_BANK_CODE, _selectedBank.recvInstId)
        Prefs.putString(Constants.SETTLEMENT_BANK_NAME, _selectedBank.bankName)
        Prefs.putString(Constants.SETTLEMENT_ACCOUNT_NAME, _beneficiaryPayload.accountName)
        Toast.makeText(context, "Settlement account setup completed", Toast.LENGTH_SHORT).show()
        findNavController().popBackStack(com.interswitchng.smartpos.R.id.settings, false)
    }


    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) = AccountSetupFragment()
    }



}