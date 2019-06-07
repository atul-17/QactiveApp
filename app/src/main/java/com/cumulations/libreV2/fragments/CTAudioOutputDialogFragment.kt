package com.cumulations.libreV2.fragments

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.Gravity
import android.view.WindowManager
import com.cumulations.libreV2.activity.CTDeviceSettingsActivity
import com.cumulations.libreV2.tcp_tunneling.enums.AQModeSelect
import com.libre.R
import com.libre.Scanning.Constants
import kotlinx.android.synthetic.main.ct_dlg_fragment_audio_output.*


/**
 * Created by Amit Tumkur on 05-06-2018.
 */
class CTAudioOutputDialogFragment: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        val dialog = Dialog(activity!!, R.style.TransparentDialogTheme)
        dialog.setContentView(R.layout.ct_dlg_fragment_audio_output)
        dialog.setCanceledOnTouchOutside(true)
        isCancelable = true

        val lp = dialog.window.attributes
        lp.gravity = Gravity.BOTTOM //position
        lp.width = WindowManager.LayoutParams.MATCH_PARENT // full screen
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog.window.attributes = lp


        arguments?.let {
            when (it.getString(Constants.AUDIO_OUTPUT)) {
                AQModeSelect.Trillium.name -> {
                    dialog.rg_audio_output.check(R.id.rb_trillium)
                }
                AQModeSelect.Power.name -> {
                    dialog.rg_audio_output.check(R.id.rb_power)
                }
                AQModeSelect.Left.name -> {
                    dialog.rg_audio_output.check(R.id.rb_left)
                }

                AQModeSelect.Right.name -> {
                    dialog.rg_audio_output.check(R.id.rb_right)
                }
            }
        }

        dialog.rg_audio_output.setOnCheckedChangeListener { radioGroup, i ->
            when(i/*checkedId*/){
                R.id.rb_trillium -> {
                    (activity as CTDeviceSettingsActivity).updateAudioOutputOfDevice(AQModeSelect.Trillium)
                    dismiss()
                }

                R.id.rb_power -> {
                    (activity as CTDeviceSettingsActivity).updateAudioOutputOfDevice(AQModeSelect.Power)
                    dismiss()
                }

                R.id.rb_left -> {
                    (activity as CTDeviceSettingsActivity).updateAudioOutputOfDevice(AQModeSelect.Left)
                    dismiss()
                }

                R.id.rb_right -> {
                    (activity as CTDeviceSettingsActivity).updateAudioOutputOfDevice(AQModeSelect.Right)
                    dismiss()
                }
            }
        }

        return dialog
    }
}