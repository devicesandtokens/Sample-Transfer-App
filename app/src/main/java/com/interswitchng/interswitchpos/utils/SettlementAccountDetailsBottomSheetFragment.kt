package com.interswitchng.interswitchpos.utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.interswitchng.interswitchpos.R
import com.interswitchng.smartpos.models.BeneficiaryModel
import kotlinx.android.synthetic.main.isw_settement_account_details_bottomsheet.*

class SettlementAccountDetailsBottomSheetFragment(private val accountDetails: BeneficiaryModel ): BottomSheetDialogFragment() {
    companion object {

        const val TAG = "SettlementAccountDetailsBottomSheetFragment"

    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.isw_settement_account_details_bottomsheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isw_setmt_account_number.text = accountDetails.accountNumber
        isw_setmt_account_name.text = accountDetails.accountName
        isw_setmt_account_bank.text = accountDetails.bankName
    }


}