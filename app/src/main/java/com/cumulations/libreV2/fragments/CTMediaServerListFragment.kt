package com.cumulations.libreV2.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity
import com.cumulations.libreV2.activity.CTUpnpFileBrowserActivity
import com.libre.qactive.R
import com.libre.qactive.Scanning.Constants
import com.libre.qactive.app.dlna.dmc.processor.impl.UpnpProcessorImpl
import com.libre.qactive.app.dlna.dmc.processor.interfaces.UpnpProcessor

import com.libre.qactive.util.LibreLogger
import kotlinx.android.synthetic.main.ct_dlg_fragment_media_servers.*
import kotlinx.android.synthetic.main.ct_list_item_dms_device.*
import org.fourthline.cling.model.meta.LocalDevice
import org.fourthline.cling.model.meta.RemoteDevice
import java.util.HashMap


/**
 * Created by Amit Tumkur on 05-06-2018.
 */
class CTMediaServerListFragment: DialogFragment(), UpnpProcessor.UpnpProcessorListener {
    private var listAdapter: ArrayAdapter<String>? = null
    private val nameToUDNMap = HashMap<String, String>()
    private var isLocalDeviceSelected: Boolean = false
    private val currentDeviceIp: String? by lazy {
        arguments?.getString(Constants.CURRENT_DEVICE_IP)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)

        val dialog = Dialog(activity!!, R.style.TransparentDialogTheme)
        dialog.setContentView(R.layout.ct_dlg_fragment_media_servers)
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        isCancelable = true

        val lp = dialog.window?.attributes
        lp?.gravity = Gravity.BOTTOM //position
        dialog.window?.attributes = lp

        listAdapter = ArrayAdapter(activity!!, R.layout.ct_list_item_dms_device)

        text1?.isSelected = true
        dialog?.deviceList?.adapter = listAdapter

        dialog?.deviceList?.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, i, l ->
            activity?.runOnUiThread {
                showLoader()
            }

            val friendlyName = listAdapter?.getItem(i)
            val intent = Intent(activity, CTUpnpFileBrowserActivity::class.java)
            intent.putExtra(Constants.DIDL_TITLE, friendlyName)
            intent.putExtra(Constants.DEVICE_UDN, nameToUDNMap[friendlyName])
            intent.putExtra(Constants.CURRENT_DEVICE_IP, currentDeviceIp)
            startActivity(intent)
        }


        dialog?.iv_refresh?.setOnClickListener {
            showLoader()
            listAdapter?.clear()
            listAdapter?.notifyDataSetChanged()
            (activity as CTDeviceDiscoveryActivity).upnpProcessor!!.searchAll()
        }

        return dialog
    }

    override fun onStart() {
        super.onStart()
        if ((activity as CTDeviceDiscoveryActivity).upnpProcessor != null) {
            (activity as CTDeviceDiscoveryActivity).upnpProcessor!!.addListener(this)
        }
    }

    override fun onStartComplete() {}

    override fun onResume() {
        super.onResume()
        showLoader()
        (activity as CTDeviceDiscoveryActivity).upnpProcessor?.searchAll()
    }

    override fun onRemoteDeviceAdded(device: RemoteDevice?) {
        LibreLogger.d(this, "Added Remote device")

        activity?.runOnUiThread {
            Log.d("onRemoteDeviceAdded", "runOnUiThread " + device?.identity?.udn.toString())
            if (device?.type?.namespace == UpnpProcessorImpl.DMS_NAMESPACE && device.type.type == UpnpProcessorImpl.DMS_TYPE) {
                val position = listAdapter?.getPosition(device.details.friendlyName)
                if (position!! >= 0) {
                    // Device already in the list, re-set new value at same position
                    listAdapter?.remove(device.details.friendlyName)
                    listAdapter?.insert(device.details.friendlyName, position)
                } else {
                    listAdapter?.add(device.details.friendlyName)
                    listAdapter?.notifyDataSetChanged()
                }
                closeLoader()
            }
        }

        val udn = device?.identity?.udn.toString()
        nameToUDNMap[device?.details?.friendlyName!!] = udn
    }

    override fun onRemoteDeviceRemoved(device: RemoteDevice?) {
        val udn = device?.identity?.udn.toString()
        LibreLogger.d(this, "onRemoteDeviceRemoved $udn")
        if (nameToUDNMap.containsKey(device?.details?.friendlyName))
            nameToUDNMap.remove(device?.details?.friendlyName)

        activity?.runOnUiThread {
            Log.d("onRemoteDeviceAdded", "runOnUiThread " + device?.identity?.udn.toString())
            val position = listAdapter?.getPosition(device?.details?.friendlyName)
            if (position!! >= 0) {
                // Device exist in the list
                listAdapter?.remove(device?.details?.friendlyName)
                listAdapter?.notifyDataSetChanged()
            }
        }
    }

    override fun onLocalDeviceAdded(device: LocalDevice?) {
        LibreLogger.d(this, "onLocalDeviceAdded udn = ${device?.identity?.udn}")
    }

    override fun onLocalDeviceRemoved(device: LocalDevice?) {
        LibreLogger.d(this, "onLocalDeviceRemoved udn = ${device?.identity?.udn}")
    }

    private fun showLoader() {
        dialog?.iv_refresh?.visibility = View.INVISIBLE
        dialog?.loader?.visibility = View.VISIBLE
    }

    private fun closeLoader() {
        dialog?.iv_refresh?.visibility = View.VISIBLE
        dialog?.loader?.visibility = View.INVISIBLE
    }

    override fun onDismiss(dialog: DialogInterface) {
        closeLoader()
        if ((activity as CTDeviceDiscoveryActivity).upnpProcessor != null) {
            (activity as CTDeviceDiscoveryActivity).upnpProcessor!!.removeListener(this)
        }
        super.onDismiss(dialog)
    }
}