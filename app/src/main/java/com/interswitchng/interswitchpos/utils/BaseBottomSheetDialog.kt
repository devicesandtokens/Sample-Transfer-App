package com.interswitchng.interswitchpos.utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.interswitchng.smartpos.R

abstract class BaseBottomSheetDialog : BottomSheetDialogFragment() {


    override fun getTheme(): Int = R.style.IswBottomSheet
}