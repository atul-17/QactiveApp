package com.cumulations.libreV2.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cumulations.libreV2.activity.CTDMSBrowserActivity
import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity
import com.cumulations.libreV2.adapter.CTDIDLObjectListAdapter
import com.libre.R
import com.libre.Scanning.Constants
import com.libre.app.dlna.dmc.processor.interfaces.DMSProcessor
import com.libre.app.dlna.dmc.server.ContentTree
import com.libre.app.dlna.dmc.server.MusicServer
import com.libre.util.LibreLogger
import kotlinx.android.synthetic.main.ct_fragment_dms_browser.*
import org.fourthline.cling.support.model.DIDLObject
import org.fourthline.cling.support.model.item.Item


class CTDMRBrowserFragment : Fragment(), DMSProcessor.DMSProcessorListener {
    private var didlObjectList: ArrayList<DIDLObject>? = ArrayList()
    private var didlObjectArrayAdapter: CTDIDLObjectListAdapter? = null
    private val musicType: String? by lazy {
        arguments?.getString(Constants.MUSIC_TYPE)
    }

    private var didlContainer: DIDLObject? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        Log.e("onCreateView","called $musicType")
        return inflater?.inflate(R.layout.ct_fragment_dms_browser, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() {
        didlObjectArrayAdapter = CTDIDLObjectListAdapter(activity, ArrayList())
        rv_browser_list?.layoutManager = LinearLayoutManager(activity)
        rv_browser_list?.adapter = didlObjectArrayAdapter
    }

    override fun onBrowseComplete(parentObjectId: String?, result: MutableMap<String, MutableList<out DIDLObject>>?) {
        LibreLogger.d(this, "Browse Completed")

        activity?.runOnUiThread {

            /*Handling clearing of list*/
            when {
                parentObjectId?.contains(ContentTree.AUDIO_ALBUMS_ID)!!
                        && musicType.equals(MusicServer.ALBUMS) -> didlObjectList?.clear()

                parentObjectId?.contains(ContentTree.AUDIO_ARTISTS_ID)!!
                        && musicType.equals(MusicServer.ARTISTS) -> didlObjectList?.clear()

                parentObjectId?.contains(ContentTree.AUDIO_SONGS_ID)!!
                        && musicType.equals(MusicServer.SONGS) -> didlObjectList?.clear()
            }

            val containersList = result!!["Containers"]
            if (containersList?.isNotEmpty()!!) {
                for (container in containersList) {
                    Log.e("onBrowseComplete", "container id ${container.id}, clazz = ${container.clazz.value}")
                    if (musicType?.equals(MusicServer.ALBUMS)!! && container?.clazz?.value?.contains("musicAlbum", false)!!) {
                        didlObjectList?.add(container)
                    } else if (musicType?.equals(MusicServer.ARTISTS)!! && container?.clazz?.value?.contains("musicArtist", false)!!) {
                        didlObjectList?.add(container)
                    }
                }
            }

            val itemsList = result["Items"] as List<Item>
            if (itemsList?.isNotEmpty()!!) {
                (activity as CTDeviceDiscoveryActivity).showProgressDialog(R.string.pleaseWait)
                for (item in itemsList) {
                    Log.e("CTDMRBrowserFragment", "item = " + item.title + " musicType $musicType")
                    Log.e("onBrowseComplete", "item id ${item.id}, clazz = ${item.clazz.value}")

                    when (musicType) {
                        MusicServer.ALBUMS -> {
                            if (item.refID?.contains(ContentTree.AUDIO_ALBUMS_ID)!!) {
                                didlObjectList?.add(item)
                            }
                        }

                        MusicServer.ARTISTS -> {
                            if (item.refID?.contains(ContentTree.AUDIO_ARTISTS_ID)!!) {
                                didlObjectList?.add(item)
                            }
                        }

                        MusicServer.SONGS -> {
                            if (item.refID?.contains(ContentTree.AUDIO_SONGS_ID)!!) {
                                didlObjectList?.add(item)
                            }
                        }
                    }
                }
            }

            didlObjectArrayAdapter?.updateList(didlObjectList)
            (activity as CTDeviceDiscoveryActivity).dismissDialog()
            if (didlObjectList?.isEmpty()!!) {
                (activity as CTDeviceDiscoveryActivity).showToast(R.string.noContent)
            }

        }
    }

    override fun onBrowseFail(message: String?) {
        (activity as CTDMSBrowserActivity).showToast(message!!)
    }

    fun provideContainer(container: DIDLObject) {
        didlContainer = container
        didlObjectList?.clear()
        (activity as CTDMSBrowserActivity).browse(didlContainer)
        (activity as CTDMSBrowserActivity).dmsProcessor?.addListener(this)
    }

    fun updateBrowserList(didlObjectList: List<DIDLObject>?) {
//        (activity as CTDMSBrowserActivity).dmsProcessor?.removeListener(this)
        this.didlObjectList = (didlObjectList as ArrayList<DIDLObject>?)!!
        didlObjectArrayAdapter?.updateList(didlObjectList as MutableList<DIDLObject>?)
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