package com.cumulations.libreV2.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity
import com.cumulations.libreV2.adapter.CTDIDLObjectListAdapter
import com.libre.qactive.R
import com.libre.qactive.Scanning.Constants
import com.libre.qactive.util.LibreLogger
import kotlinx.android.synthetic.main.ct_fragment_dms_browser.*
import org.fourthline.cling.support.model.DIDLObject


class CTDMRBrowserFragmentV2 : Fragment() {
    private var didlObjectArrayAdapter: CTDIDLObjectListAdapter? = null
    private val musicType: String? by lazy {
        arguments?.getString(Constants.MUSIC_TYPE)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        Log.e("onCreateView","called $musicType")
        return inflater?.inflate(R.layout.ct_fragment_dms_browser, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() {
        didlObjectArrayAdapter = CTDIDLObjectListAdapter(activity!!, ArrayList())
        rv_browser_list?.layoutManager = LinearLayoutManager(activity)
        rv_browser_list?.adapter = didlObjectArrayAdapter
        rv_browser_list?.setEmptyView(tv_no_data)
    }

    fun updateBrowserList(didlObjectList: List<DIDLObject>?) {
        activity?.runOnUiThread {
            didlObjectArrayAdapter?.updateList(didlObjectList as MutableList<DIDLObject>?)
            if (didlObjectArrayAdapter?.didlObjectList?.isEmpty()!!){
                tv_no_data?.text = getText(R.string.noItems)
                tv_no_data?.visibility = View.VISIBLE
//                (activity as CTDeviceDiscoveryActivity).showToast(R.string.noContent)
            } else {
                tv_no_data?.visibility = View.GONE
            }
        }
    }

    fun browsingOver(){
        LibreLogger.d(this,"browsingOver, type = $musicType")
        activity?.runOnUiThread {
            if (didlObjectArrayAdapter?.didlObjectList?.isEmpty()!!){
                tv_no_data?.text = getText(R.string.noItems)
                tv_no_data?.visibility = View.VISIBLE
            } else {
                tv_no_data?.visibility = View.GONE
            }
        }
    }

    fun scrollToPosition(position: Int) {
        rv_browser_list?.scrollToPosition(position)
    }

    fun getFirstVisibleItemPosition(): Int {
        return (rv_browser_list?.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
    }

    fun getCurrentDIDLObjectList(): ArrayList<DIDLObject>? {
        return /*dataItemList*/(didlObjectArrayAdapter?.didlObjectList as ArrayList<DIDLObject>?)!!
    }
}
