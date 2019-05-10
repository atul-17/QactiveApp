package com.cumulations.libreV2.fragments

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.WindowManager
import com.cumulations.libreV2.activity.CTDeviceSettingsActivity
import com.libre.R
import com.libre.Scanning.Constants
import com.libre.alexa.LibreAlexaConstants
import kotlinx.android.synthetic.main.ct_dlg_fragment_select_locale.*
import android.view.Gravity



/**
 * Created by Amit Tumkur on 05-06-2018.
 */
class CTAlexaLocaleDialogFragment: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        val dialog = Dialog(activity!!, R.style.StyledDialogTheme)
        dialog.setContentView(R.layout.ct_dlg_fragment_select_locale)
        dialog.setCanceledOnTouchOutside(true)
        isCancelable = true

        val lp = dialog.window.attributes
        lp.gravity = Gravity.BOTTOM //psotion
        lp.width = WindowManager.LayoutParams.MATCH_PARENT // fuill screen
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog.window.attributes = lp


        arguments?.let {
            when (it.getString(Constants.CURRENT_LOCALE)) {
                LibreAlexaConstants.Languages.ENG_US -> {
                    dialog.rg_select_locale.check(R.id.rb_en_us)
                }
                LibreAlexaConstants.Languages.ENG_GB -> {
                    dialog.rg_select_locale.check(R.id.rb_en_uk)
                }
                LibreAlexaConstants.Languages.DE -> {
                    dialog.rg_select_locale.check(R.id.rb_deu)
                }
            }
        }

        dialog.rg_select_locale.setOnCheckedChangeListener { radioGroup, i ->
            when(i/*checkedId*/){
                R.id.rb_en_us -> {
                    (activity as CTDeviceSettingsActivity).sendUpdatedLangToDevice(LibreAlexaConstants.Languages.ENG_US)
                    dismiss()
                }

                R.id.rb_en_uk -> {
                    (activity as CTDeviceSettingsActivity).sendUpdatedLangToDevice(LibreAlexaConstants.Languages.ENG_GB)
                    dismiss()
                }

                R.id.rb_deu -> {
                    (activity as CTDeviceSettingsActivity).sendUpdatedLangToDevice(LibreAlexaConstants.Languages.DE)
                    dismiss()
                }
            }
        }

        return dialog
    }
}