package com.interswitchng.interswitchpos.views.fragments

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import com.interswitchng.interswitchpos.R
import com.interswitchng.interswitchpos.databinding.FragmentSettingsLandingBinding
import com.interswitchng.smartpos.IswPos
import com.interswitchng.smartpos.IswTxnHandler
import kotlinx.android.synthetic.main.fragment_settings_landing.*
import org.koin.standalone.KoinComponent
import com.interswitchng.interswitchpos.utils.SettlementAccountDetailsBottomSheetFragment
import com.interswitchng.smartpos.models.BeneficiaryModel
import com.interswitchng.smartpos.shared.Constants
import com.pixplicity.easyprefs.library.Prefs
import kotlinx.android.synthetic.main.fragment_settings_landing.*


class SettingsLandingFragment: Fragment() {


    private lateinit var binding : FragmentSettingsLandingBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private fun setupTerminalSettings(){
        binding.terminalSettings.setOnClickListener(){
            showScreen(TerminalSettingFragment::class.java)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_settings_landing, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTerminalSettings()
        setupUI()
    }

    private fun showScreen(clazz: Class<*>){
        val app = this.requireContext()
        val intent = Intent(app, clazz).addFlags((Intent.FLAG_ACTIVITY_NEW_TASK))
        if (clazz.isAssignableFrom(TerminalSettingFragment::class.java)) {
            intent.putExtra("FROM_SETTINGS", true)
        }
        this.requireContext().startActivity(intent)
    }


    private fun setupUI() {
        configure_settlement_account_text.setOnClickListener {
            val action = SettingsLandingFragmentDirections.actionSettingsToAccountSetupFragment2()
            findNavController().navigate(action)
        }

        view_settlement_account.setOnClickListener {
            showSettlementAccount()
        }
    }

    private fun showSettlementAccount() {
        val savedBeneficiary = BeneficiaryModel(
                accountNumber = Prefs.getString(Constants.SETTLEMENT_ACCOUNT_NUMBER, ""),
                accountName = Prefs.getString(Constants.SETTLEMENT_ACCOUNT_NAME, ""),
                bankName = Prefs.getString(Constants.SETTLEMENT_BANK_NAME, "")
        )

        parentFragmentManager.let { it1 -> SettlementAccountDetailsBottomSheetFragment(savedBeneficiary).show(it1, SettlementAccountDetailsBottomSheetFragment.TAG) }

    }

    companion object {
        @JvmStatic
        fun newInstance() = SettingsLandingFragment()
    }
}