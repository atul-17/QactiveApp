package com.cumulations.libreV2.activity

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import androidx.core.content.ContextCompat.startActivity
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import android.text.TextUtils
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import com.cumulations.libreV2.AppUtils
import com.cumulations.libreV2.adapter.CTDeviceBrowserListAdapter
import com.cumulations.libreV2.closeKeyboard
import com.libre.qactive.LErrorHandeling.LibreError
import com.cumulations.libreV2.model.DataItem
import com.libre.qactive.R
import com.libre.qactive.Scanning.Constants
import com.libre.qactive.Scanning.Constants.NETWORK_TIMEOUT
import com.libre.qactive.Scanning.ScanningHandler
import com.libre.qactive.constants.LSSDPCONST
import com.libre.qactive.constants.LUCIMESSAGES
import com.libre.qactive.constants.LUCIMESSAGES.BACK
import com.libre.qactive.constants.MIDCONST
import com.libre.qactive.luci.LSSDPNodes
import com.libre.qactive.luci.LUCIControl
import com.libre.qactive.luci.LUCIPacket
import com.libre.qactive.netty.LibreDeviceInteractionListner
import com.libre.qactive.netty.NettyData
import com.libre.qactive.util.LibreLogger
import kotlinx.android.synthetic.main.ct_activity_device_browser.*
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class CTDeviceBrowserActivity : CTDeviceDiscoveryActivity(), LibreDeviceInteractionListner {

    companion object {
        const val TAG_CMD_ID = "CMD ID"
        const val TAG_WINDOW_CONTENT = "Window CONTENTS"
        const val TAG_BROWSER = "Browser"
        const val TAG_CUR_INDEX = "Index"
        const val TAG_ITEM_COUNT = "Item Count"
        const val TAG_ITEM_LIST = "ItemList"
        const val TAG_ITEM_ID = "Item ID"
        const val TAG_ITEM_TYPE = "ItemType"
        const val TAG_ITEM_NAME = "Name"
        const val TAG_ITEM_FAVORITE = "Favorite"
        const val TAG_ITEM_ALBUMURL = "StationImage"

        const val GET_PLAY = "GETUI:PLAY"
    }

    private var currentIpaddress: String? = null
    private var luciControl: LUCIControl? = null
    private var dataItems: ArrayList<DataItem>? = ArrayList()
    private var deviceBrowserListAdapter: CTDeviceBrowserListAdapter? = null
    private var mLayoutManager: LinearLayoutManager? = null
    private var gotolastpostion: Boolean = false
    private var browser = ""
    private var current_source_index_selected = -1
    internal var alert: AlertDialog? = null
    // private boolean isSearchEnabled;
    private var isSongSelected = false

    private var searchJsonHashCode: Int = 0
    private var presentJsonHashCode: Int = 0
    //searchOptionClicked is true when user clicks on search option. And then we calculate hash code for search JSON result
    private var searchOptionClicked = false

    @SuppressLint("HandlerLeak")
    internal var handler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.what == NETWORK_TIMEOUT) {
                LibreLogger.d(this, "handler message recieved")

                closeLoader()
                /*showing error*/
                val error = LibreError(currentIpaddress, getString(R.string.requestTimeout))
                showErrorMessage(error)

                ib_home?.performClick()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ct_activity_device_browser)

        currentIpaddress = intent.getStringExtra(Constants.CURRENT_DEVICE_IP)
        current_source_index_selected = intent.getIntExtra(Constants.CURRENT_SOURCE_INDEX_SELECTED, -1)
        setTitleForTheBrowser(current_source_index_selected)

        if (current_source_index_selected < 0) {
            showToast(R.string.sourceIndexWrong)
            val intent = Intent(this@CTDeviceBrowserActivity, CTMediaSourcesActivity::class.java)
            intent.putExtra(Constants.CURRENT_DEVICE_IP, currentIpaddress)
            val sceneObject = ScanningHandler.getInstance().getSceneObjectFromCentralRepo(currentIpaddress)
            intent.putExtra(Constants.CURRENT_SOURCE, sceneObject!!.currentSource)
            startActivity(intent)
            finish()
        }

        LibreLogger.d(this, " Registered for the device " + currentIpaddress!!)

        luciControl = LUCIControl(currentIpaddress)
        luciControl!!.SendCommand(MIDCONST.MID_REMOTE_UI.toInt(), LUCIMESSAGES.SELECT_ITEM + ":" + current_source_index_selected, LSSDPCONST.LUCI_SET)

        showLoader(R.string.loading_next_items)

        //////////// timeout for dialog - showLoader() ///////////////////
        if (current_source_index_selected == 0) {
            /*increasing timeout for media servers only*/
            handler.sendEmptyMessageDelayed(NETWORK_TIMEOUT, 60000)
        } else {
            handler.sendEmptyMessageDelayed(NETWORK_TIMEOUT, 15000)
        }

        mLayoutManager = LinearLayoutManager(this)
        deviceBrowserListAdapter = CTDeviceBrowserListAdapter(this, dataItems)
        rv_device_browser?.layoutManager = mLayoutManager
        rv_device_browser?.adapter = deviceBrowserListAdapter

        rv_device_browser?.setOnTouchListener(object : View.OnTouchListener {
            var beginY = 0f
            var endY = 0f

            override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
                //ACTION_DOWN when the user first touches the screen. We are taking the beginY co ordinate here
                if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                    LibreLogger.d(this, "action down, touched y co ordinate is " + motionEvent.y)
                    beginY = motionEvent.y
                }
                //ACTION_UP, when the user finally releases the touch. We are taking the endY co ordinate here
                if (motionEvent.action == MotionEvent.ACTION_UP) {
                    LibreLogger.d(this, "action up, touch lifted y co ordinate is " + motionEvent.y)
                    endY = motionEvent.y
                    if (beginY > endY) {
                        LibreLogger.d(this, "touched from bottom -> top, send scroll down")
                        sendScrollDown()
                    } else if (beginY < endY) {
                        LibreLogger.d(this, "touched from top -> bottom, send scroll up")
                        sendScrollUp()
                    }
                    //return false - means other touch events like onclick on the view item will now work.
                    return false
                }

                return false
            }
        })


        ///////////////////////////////////////////////////////////////// limit end //////////////////////////////

        ib_back?.setOnClickListener { onBackPressed() }

        ib_home?.setOnClickListener(object : View.OnClickListener {

            override fun onClick(v: View) {
                // TODO Auto-generated method stub

                LibreLogger.d(this, "user pressed home button ")
                unRegisterForDeviceEvents()
                //   luciControl.SendCommand(MIDCONST.MID_REMOTE_UI, GET_HOME, LSSDPCONST.LUCI_SET);

                val intent = Intent(this@CTDeviceBrowserActivity, CTMediaSourcesActivity::class.java)
                intent.putExtra(Constants.CURRENT_DEVICE_IP, currentIpaddress)
                val currentSceneObject = ScanningHandler.getInstance().getSceneObjectFromCentralRepo(currentIpaddress)
                intent.putExtra(Constants.CURRENT_SOURCE, "" + currentSceneObject?.currentSource)
                startActivity(intent)
                finish()

                //overridePendingTransition(R.anim.in_from_left, R.anim.out_to_right);
            }
        })
    }

    override fun onStart() {
        super.onStart()
        if (isSongSelected){
            deviceBrowserListAdapter?.dataItemList?.clear()
            deviceBrowserListAdapter?.notifyDataSetChanged()
            isSongSelected = false
            onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        /*Registering to receive messages*/
        registerForDeviceEvents(this)
        val musicPlayerView = findViewById<FrameLayout>(R.id.fl_music_play_widget)
        setMusicPlayerWidget(musicPlayerView, currentIpaddress!!)
    }

    private fun sendScrollUp() {
        if (mLayoutManager!!.findFirstVisibleItemPosition() == 0) {

            luciControl?.SendCommand(MIDCONST.MID_REMOTE.toInt(), LUCIMESSAGES.SCROLLUP, LSSDPCONST.LUCI_SET)
            gotolastpostion = true
            //  mLayoutManager.scrollToPosition(49);
            showProgressDialog(R.string.loading_next_items)
            //////////// timeout for dialog - showLoader() ///////////////////
            if (current_source_index_selected == 0) {
                /*increasing timeout for media servers only*/
                handler.sendEmptyMessageDelayed(NETWORK_TIMEOUT, 60000)
            } else {
                handler.sendEmptyMessageDelayed(NETWORK_TIMEOUT, 15000)
            }

            LibreLogger.d(this, "recycling " + "SCROLL UP" + "and find first visible item position=" +
                    mLayoutManager!!.findFirstVisibleItemPosition() + "find last visible item=" +
                    mLayoutManager!!.findLastVisibleItemPosition() + "find last completely visible item=" +
                    mLayoutManager!!.findLastCompletelyVisibleItemPosition())
        }
    }

    private fun sendScrollDown() {
        if (mLayoutManager!!.findLastVisibleItemPosition() == 49) {
            LibreLogger.d(this, "recycling " + "last visible Item Position" + mLayoutManager!!.findLastVisibleItemPosition())
            luciControl?.SendCommand(MIDCONST.MID_REMOTE.toInt(), LUCIMESSAGES.SCROLLDOWN, LSSDPCONST.LUCI_SET)
            gotolastpostion = false
            LibreLogger.d(this, "recycling " + "SCROLL DOWN")
            // mLayoutManager.scrollToPosition(0);
            showProgressDialog(R.string.loading_prev_items)
            //////////// timeout for dialog - showLoader() ///////////////////
            if (current_source_index_selected == 0) {
                /*increasing timeout for media servers only*/
                handler.sendEmptyMessageDelayed(NETWORK_TIMEOUT, 60000)
            } else {
                handler.sendEmptyMessageDelayed(NETWORK_TIMEOUT, 15000)
            }
            LibreLogger.d(this, "recycling " + "Last song")
            showToast(R.string.lastSong)
        }
    }

    /*Searching dialog */
    private fun searchingDialog(position: Int) {
        // custom dialog
        val dialog = Dialog(this@CTDeviceBrowserActivity)
        dialog.setContentView(R.layout.deezer_auth_dialog)
        dialog.setTitle(getString(R.string.searchTextPlaceHolder))

        // set the custom dialog components - text, image and button
        val searchString = dialog.findViewById<View>(R.id.user_name) as EditText
        val userPassword = dialog.findViewById<View>(R.id.user_password) as EditText

        searchString.hint = getString(R.string.enterText)
        userPassword.visibility = View.GONE

        val submitButton = dialog.findViewById<View>(R.id.deezer_ok_button) as Button
        // if button is clicked, close the custom dialog
        submitButton.setOnClickListener(View.OnClickListener { v ->
            if (TextUtils.isEmpty(searchString.text.toString().trim { it <= ' ' })) {
                Toast.makeText(this@CTDeviceBrowserActivity, getString(R.string.searchText), Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }

            closeKeyboard(this@CTDeviceBrowserActivity, v)

            val searchDataToBeSent = "SEARCH_" + searchString.text.toString()
            val selectedPositionToBeSent = LUCIMESSAGES.SELECT_ITEM + ":" + position

            Log.d("SearchString", "--$searchDataToBeSent")
            val luciPackets = ArrayList<LUCIPacket>()

            val searchPacket = LUCIPacket(searchDataToBeSent.toByteArray(), searchDataToBeSent.length.toShort(), MIDCONST.MID_REMOTE, LSSDPCONST.LUCI_SET.toByte())
            luciPackets.add(searchPacket)

            val positionPacket = LUCIPacket(selectedPositionToBeSent.toByteArray(),
                    selectedPositionToBeSent.length.toShort(), MIDCONST.MID_REMOTE, LSSDPCONST.LUCI_SET.toByte())
            luciPackets.add(positionPacket)
            luciControl!!.SendCommand(luciPackets)

            dialog.dismiss()
        })

        dialog.show()
    }

    /*showing dialog*/
    private fun showLoader(messageId: Int) {

        if (this@CTDeviceBrowserActivity.isFinishing)
            return
        showProgressDialog(messageId)
    }

    private fun closeLoader() {
        if (isFinishing) return
        dismissDialog()
    }

    override fun deviceDiscoveryAfterClearingTheCacheStarted() {

    }

    override fun newDeviceFound(node: LSSDPNodes) {

    }

    override fun deviceGotRemoved(mIpAddress: String) {

    }

    override fun messageRecieved(dataRecived: NettyData) {

        val buffer = dataRecived.getMessage()
        val ipaddressRecieved = dataRecived.getRemotedeviceIp()

        LibreLogger.d(this, "Message recieved for ipaddress $ipaddressRecieved")

        if (currentIpaddress!!.equals(ipaddressRecieved, ignoreCase = true)) {
            val packet = LUCIPacket(dataRecived.getMessage())
            when (packet.command) {

                MIDCONST.SET_UI -> {
                    val message = String(packet.getpayload())
                    LibreLogger.d(this, " message 42 recieved  $message")
                    try {
                        presentJsonHashCode = message.hashCode()
                        LibreLogger.d(this, " present hash code : the hash code for $message is $presentJsonHashCode")
                        parseJsonAndReflectInUI(message)

                    } catch (e: JSONException) {
                        e.printStackTrace()
                        LibreLogger.d(this, " Json exception ")
                        closeLoader()
                    }

                }
                MIDCONST.MID_DEVICE_ALERT_STATUS.toInt() -> {
                    val message = String(packet.getpayload())
                    LibreLogger.d(this, " message 54 recieved  $message")
                    try {
                        var error: LibreError? = null
                        when {
                            message.contains(Constants.FAIL) -> error = LibreError(currentIpaddress, Constants.FAIL_ALERT_TEXT)
                            message.contains(Constants.SUCCESS) -> {
                                closeLoader()
                                handler.removeMessages(NETWORK_TIMEOUT)
                            }
                            message.contains(Constants.NO_URL) -> error = LibreError(currentIpaddress, getString(R.string.NO_URL_ALERT_TEXT))
                            message.contains(Constants.NO_PREV_SONG) -> error = LibreError(currentIpaddress, getString(R.string.NO_PREV_SONG_ALERT_TEXT))
                            message.contains(Constants.NO_NEXT_SONG) -> error = LibreError(currentIpaddress, getString(R.string.NO_NEXT_SONG_ALERT_TEXT))
                        }
                        if (error != null){
                            closeLoader()
                            handler.removeMessages(NETWORK_TIMEOUT)
                            showErrorMessage(error)
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                        LibreLogger.d(this, " Json exception ")

                    }

                }
            }

        }
    }

    /**
     * This function gets the Json string
     */
    @Throws(JSONException::class)
    private fun parseJsonAndReflectInUI(jsonStr: String?) {
        LibreLogger.d(this, "Json Recieved from remote device $jsonStr")
        if (jsonStr != null) {
            try {
                val root = JSONObject(jsonStr)
                val cmd_id = root.getInt(TAG_CMD_ID)
                val window = root.getJSONObject(TAG_WINDOW_CONTENT)
                LibreLogger.d(this, "Command Id$cmd_id")

                if (cmd_id == 3) {
                    closeLoader()
                    handler.removeMessages(NETWORK_TIMEOUT)
                    /* This means user has selected the song to be playing and hence we will need to navigate
                     him to the Active scene list
                      */
                    unRegisterForDeviceEvents()

                    val mScanHandler = ScanningHandler.getInstance()
                    var currentSceneObject = mScanHandler.getSceneObjectFromCentralRepo(currentIpaddress)
                    currentSceneObject = AppUtils.updateSceneObjectWithPlayJsonWindow(window, currentSceneObject!!)

                    if (mScanHandler!!.isIpAvailableInCentralSceneRepo(currentIpaddress)) {
                        mScanHandler!!.putSceneObjectToCentralRepo(currentIpaddress, currentSceneObject)
                    }

                    //Intent intent = new Intent(RemoteSourcesList.this, ActiveScenesListActivity.class);
                    val intent = Intent(this@CTDeviceBrowserActivity, CTNowPlayingActivity::class.java)
                    intent.putExtra(Constants.CURRENT_DEVICE_IP, currentIpaddress)
                    startActivity(intent)

                } else if (cmd_id == 1) {

                    browser = window.getString(TAG_BROWSER)
                    val Cur_Index = window.getInt(TAG_CUR_INDEX)
                    val item_count = window.getInt(TAG_ITEM_COUNT)

                    if (browser.equals("HOME", ignoreCase = true)) {
                        closeLoader()
                        handler.removeMessages(NETWORK_TIMEOUT)
                        /* This means we have reached the home collection and hence we need to lauch the SourcesOptionEntry Activity */
                        unRegisterForDeviceEvents()
                        val intent = Intent(this@CTDeviceBrowserActivity, CTMediaSourcesActivity::class.java)
                        intent.putExtra(Constants.CURRENT_DEVICE_IP, currentIpaddress)

                        val currentSceneObject = ScanningHandler.getInstance().getSceneObjectFromCentralRepo(currentIpaddress)
                        intent.putExtra(Constants.CURRENT_SOURCE, "" + currentSceneObject!!.currentSource)
                        startActivity(intent)
                        finish()
                        return
                    }

                    if (item_count == 0) {
                        val error = LibreError(currentIpaddress, getString(R.string.no_item_empty))
                        showErrorMessage(error)
                        closeLoader()
                        handler.removeMessages(NETWORK_TIMEOUT)
                        tv_no_data?.visibility = View.VISIBLE
                    } else {
                        tv_no_data?.visibility = View.GONE
                    }

                    val ItemList = window.getJSONArray(TAG_ITEM_LIST)
                    LibreLogger.d(this, "JSON PARSER item_count =  " + item_count + "  Array SIZE = " + ItemList.length())

                    val tempDataItem = ArrayList<DataItem>()
                    for (i in 0 until ItemList.length()) {
                        val item = ItemList.getJSONObject(i)
                        val dataItem = DataItem()
                        dataItem.itemID = item.getInt(TAG_ITEM_ID)
                        dataItem.itemType = item.getString(TAG_ITEM_TYPE)
                        dataItem.itemName = item.getString(TAG_ITEM_NAME)
                        dataItem.favorite = item.getInt(TAG_ITEM_FAVORITE)

                        if (item.has(TAG_ITEM_ALBUMURL)
                                && item.getString(TAG_ITEM_ALBUMURL) != null
                                && item.getString(TAG_ITEM_ALBUMURL).isNotEmpty()) {
                            dataItem.itemAlbumURL = item.getString(TAG_ITEM_ALBUMURL)
                        }

                        if (searchOptionClicked) {
                            //This JSON is the result,when user clicked search
                            // put to hashcode
                            searchJsonHashCode = jsonStr.hashCode()
                            LibreLogger.d(this, "Search hash code : the hash code for $jsonStr is $searchJsonHashCode")
                            searchOptionClicked = false

                            //save it in shared preference
                            val savedInPref = saveInSharedPreference(searchJsonHashCode)
                            if (savedInPref) {
                                LibreLogger.d(this, "saved in shared preference")
                            } else {
                                LibreLogger.d(this, "not saved in shared preference")
                            }
                        }
                        tempDataItem.add(dataItem)
                    }

                    dataItems!!.clear()
                    dataItems!!.addAll(tempDataItem)
                    deviceBrowserListAdapter?.updateList(dataItems)
                    if (gotolastpostion)
                        mLayoutManager!!.scrollToPosition(49)
                    else
                        mLayoutManager!!.scrollToPosition(0)
                    gotolastpostion = false

                    if (handler.hasMessages(NETWORK_TIMEOUT)) handler.removeMessages(NETWORK_TIMEOUT)
                    closeLoader()

                }
            } catch (e: Exception) {
                e.printStackTrace()
                closeLoader()
            }

        } else closeLoader()


    }

    private fun saveInSharedPreference(hashResult: Int): Boolean {
        try {
            getSharedPreferences(Constants.SEARCH_RESULT_HASH_CODE, Context.MODE_PRIVATE).apply {
                edit()
                        .putInt(Constants.SEARCH_RESULT_HASH_CODE_VALUE, hashResult)
                        .apply()
            }
        } catch (e: Exception) {
            return false
        }

        return true
    }

    override fun onStop() {
        super.onStop()
        /*removing handler*/
        handler.removeCallbacksAndMessages(null)
        unRegisterForDeviceEvents()
    }

    override fun onBackPressed() {
        /* Sends the back command issues*/
        showLoader(R.string.loading_prev_items)
        luciControl!!.SendCommand(MIDCONST.MID_REMOTE_UI.toInt(), BACK, LSSDPCONST.LUCI_SET)

        //////////// timeout for dialog - showLoader() ///////////////////
        if (current_source_index_selected == 0) {
            /*increasing timeout for media servers only*/
            handler.sendEmptyMessageDelayed(NETWORK_TIMEOUT, 60000)
        } else {
            handler.sendEmptyMessageDelayed(NETWORK_TIMEOUT, 15000)
        }

    }

    private fun setTitleForTheBrowser(current_source_index_selected: Int) {
        when (current_source_index_selected) {

            0 -> {
                browser_title?.text = "Music Server"
                browser_title?.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            }
            1 -> {
                browser_title?.text = ""
                browser_title?.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.vtuner_logo_remotesources_title, 0, 0, 0)
            }
            2 -> {
                browser_title?.text = "Tune In"
                browser_title?.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            }
            3 -> {
                browser_title?.text = "USB"
                browser_title?.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            }
            4 -> {
                browser_title?.text = "SD Card"
                browser_title?.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            }
            5 -> {
                browser_title?.text = "Deezer"
                browser_title?.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.deezer_crtification_logo, 0, 0, 0)
            }
            6 -> {
                browser_title?.text = ""
                browser_title?.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.tidal_title, 0, 0, 0)
            }
            7 -> {
                browser_title?.text = "Favourites"
                browser_title?.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            }
        }

    }

    fun onDataItemClicked(position: Int) {
        LibreLogger.d(this, "Play is clciked at position $position")

        if (browser.equals("TUNEIN", ignoreCase = true) && dataItems!![position].itemName.equals("search", ignoreCase = true)) {
            searchingDialog(position)

        } else if (browser.equals("VTUNER", ignoreCase = true) && dataItems!![position].itemName.equals("search", ignoreCase = true)) {
            searchingDialog(position)

        } else if ((browser.equals("TIDAL", ignoreCase = true) || browser.equals("DEEZER", ignoreCase = true))
                //  && !isSearchEnabled
                && dataItems!![position].itemName.equals("search", ignoreCase = true)) {

            luciControl!!.SendCommand(MIDCONST.MID_REMOTE.toInt(), LUCIMESSAGES.SELECT_ITEM + ":" + position, LSSDPCONST.LUCI_SET)
            //isSearchEnabled = true;
            searchOptionClicked = true
            LibreLogger.d(this, "Next JSON that comes is search result. Make hash code with the next result")

        } else if ((browser.equals("TIDAL", ignoreCase = true) || browser.equals("DEEZER", ignoreCase = true))
                //  && isSearchEnabled
                && (dataItems!![position].itemName.toLowerCase().startsWith("playlist")
                        ||
                        dataItems!![position].itemName.toLowerCase().startsWith("artist")
                        ||
                        dataItems!![position].itemName.toLowerCase().startsWith("album")
                        ||
                        dataItems!![position].itemName.toLowerCase().startsWith("track")
                        ||
                        dataItems!![position].itemName.equals("podcast", ignoreCase = true))) {
            // dialog should be shown only when it is one stage above "search"

            if (presentJsonHashCode == searchJsonHashCode) {
                LibreLogger.d(this, "hash codes matched. Can show dialog")
                searchingDialog(position)
            } else {
                LibreLogger.d(this, "hash codes did not match.")
                luciControl!!.SendCommand(MIDCONST.MID_REMOTE.toInt(), LUCIMESSAGES.SELECT_ITEM + ":" + position, LSSDPCONST.LUCI_SET)
                showLoader(R.string.loading_next_items)
            }


        } else {
            if (dataItems!![position].itemType.contains("File")) {
                isSongSelected = true
            }

            luciControl!!.SendCommand(MIDCONST.MID_REMOTE.toInt(), LUCIMESSAGES.SELECT_ITEM + ":" + position, LSSDPCONST.LUCI_SET)
            //////////// timeout for dialog - showLoader() ///////////////////
            if (current_source_index_selected == 0) {
                /*increasing timeout for media servers only*/
                handler.sendEmptyMessageDelayed(NETWORK_TIMEOUT, 60000)
            } else {
                handler.sendEmptyMessageDelayed(NETWORK_TIMEOUT, 15000)
            }
            showLoader(R.string.loading_next_items)
        }
    }

    fun onFavClicked(position: Int) {
        val dataItem = dataItems!![position] ?: return

        if (dataItem.favorite == 2) {
            /*remove here*/
            luciControl!!.SendCommand(MIDCONST.MID_REMOTE.toInt(), LUCIMESSAGES.REMOVE_FAVORITE_ITEM + ":" + position, LSSDPCONST.LUCI_SET)
            dataItem.favorite = 1
            deviceBrowserListAdapter!!.notifyDataSetChanged()
        }

        if (dataItem.favorite == 1) {
            /*make fav here*/
            luciControl!!.SendCommand(MIDCONST.MID_REMOTE.toInt(), LUCIMESSAGES.FAVORITE_ITEM + ":" + position, LSSDPCONST.LUCI_SET)
            dataItem.favorite = 2
            deviceBrowserListAdapter!!.notifyDataSetChanged()
        }
    }
}
