package com.interswitchng.interswitchpos.views.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.interswitchng.interswitchpos.R
import com.interswitchng.interswitchpos.databinding.FragmentAccountTypeBinding
import com.interswitchng.smartpos.shared.utilities.SingleArgsClickListener


class AccountTypeFragment constructor(private val clickListener: SingleArgsClickListener<Int>) : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentAccountTypeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_account_type, container, false)

        binding.iswDefaultAccount.setOnClickListener {
            clickListener.invoke(0)
            this.dismiss()
        }
        binding.iswSavingsAccount.setOnClickListener {
            clickListener.invoke(1)
            this.dismiss()
        }
        binding.iswCurrentAccount.setOnClickListener {
            clickListener.invoke(2)
            this.dismiss()
        }

        return binding.root
    }

    companion object {
    }
}