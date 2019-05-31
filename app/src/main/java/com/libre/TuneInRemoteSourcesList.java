package com.libre;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.cumulations.libreV2.activity.CTNowPlayingActivity;
import com.libre.ManageDevice.DataItem;
import com.libre.Scanning.Constants;
import com.libre.Scanning.ScanningHandler;
import com.libre.constants.LSSDPCONST;
import com.libre.constants.LUCIMESSAGES;
import com.libre.constants.MIDCONST;
import com.libre.luci.LSSDPNodes;
import com.libre.luci.LUCIControl;
import com.libre.luci.LUCIPacket;
import com.libre.netty.LibreDeviceInteractionListner;
import com.libre.netty.NettyData;
import com.libre.nowplaying.NowPlayingActivity;
import com.libre.util.LibreLogger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class TuneInRemoteSourcesList extends CTDeviceDiscoveryActivity implements LibreDeviceInteractionListner, RecyclerViewAdapter.RemoteItemClickListener {


    private String currentIpaddress;
    private LUCIControl luciControl;
    private ProgressDialog m_progressDlg;
    private static final String TAG_CMD_ID = "CMD ID";
    private static final String TAG_TITLE = "Title";
    private static final String TAG_WINDOW_CONTENT = "Window CONTENTS";
    private static final String TAG_BROWSER = "Browser";
    private static final String TAG_CUR_INDEX = "Index";
    private static final String TAG_ITEM_COUNT = "Item Count";
    private static final String TAG_ITEM_LIST = "ItemList";
    private static final String TAG_ITEM_ID = "Item ID";
    private static final String TAG_ITEM_TYPE = "ItemType";
    private static final String TAG_ITEM_NAME = "Name";
    private static final String TAG_ITEM_FAVORITE = "Favorite";
    private static final String TAG_ITEM_ALBUMURL = "StationImage";
    private RecyclerView recyclerView;
    private ArrayList<DataItem> ViewItemArray;
    private RecyclerViewAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    boolean gotolastpostion;
    private String Browser = "";

    private static final String BACK = "BACK";
    public static final String GET_PLAY = "GETUI:PLAY";
    public static final String GET_HOME = "GETUI:HOME";
    private ImageButton m_back;
    private ImageButton homeButton;
    private int current_source_index_selected = -1;
    final int TIMEUP = 0;
   // private boolean isSearchEnabled;

    private int searchJsonHashCode;
    private int presentJsonHashCode;
    //searchOptionClicked is true when user clicks on search option. And then we calculate hash code for search JSON result
    private boolean searchOptionClicked = false;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case TIMEUP:
                    LibreLogger.d(this, "handler message recieved");
                    closeLoader();
                    homeButton.performClick();
                    break;
            }
        }
    };
    private boolean isSongSlected;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tune_in_activity_remote_sources_list);

        registerForDeviceEvents(this);
        SharedPreferences sharedPreferences = getApplicationContext()
                .getSharedPreferences(Constants.SEARCH_RESULT_HASH_CODE, Context.MODE_PRIVATE);

        searchJsonHashCode = sharedPreferences.getInt(Constants.SEARCH_RESULT_HASH_CODE_VALUE,0);
        currentIpaddress = getIntent().getStringExtra(Constants.CURRENT_DEVICE_IP);
        current_source_index_selected = getIntent().getIntExtra("current_source_index_selected", -1);

        if (current_source_index_selected < 0) {
            Toast.makeText(this, getString(R.string.sourceIndexWrong), Toast.LENGTH_SHORT).show();
//            Intent intent = new Intent(TuneInRemoteSourcesList.this, SourcesOptionActivity.class);
            Intent intent = new Intent(TuneInRemoteSourcesList.this, CTNowPlayingActivity.class);
            intent.putExtra(Constants.CURRENT_DEVICE_IP, currentIpaddress);
            startActivity(intent);
            finish();
        }

        LibreLogger.d(this, " Registered for the device " + currentIpaddress);

        luciControl = new LUCIControl(currentIpaddress);
        luciControl.sendAsynchronousCommand();

        if (getIntent().hasExtra("tune_in_info_clicked")) {
            LibreLogger.d(this,"tune-in info clicked in nowplayingfragment");
            setTitleForTheBrowser(2);
            luciControl.SendCommand(MIDCONST.MID_REMOTE_UI, LUCIMESSAGES.SELECT_ITEM + ":" + 0 , LSSDPCONST.LUCI_SET);
        } else {
            setTitleForTheBrowser(current_source_index_selected);
            onBackPressed();
        }


        showLoader();
        handler.sendEmptyMessageDelayed(TIMEUP, 10000);


        recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        ViewItemArray = new ArrayList<DataItem>();
        mAdapter = new RecyclerViewAdapter(this,ViewItemArray, TuneInRemoteSourcesList.this);
        recyclerView.setAdapter(mAdapter);

        mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);

      /*  recyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {

                        LibreLogger.d(this, "Play is clciked at position " + position);


                        if (Browser.equalsIgnoreCase("TUNEIN")
                                && ViewItemArray.get(position).getItemName().equalsIgnoreCase("search")) {
                            searchingDialog(position);

                        } else if ((Browser.equalsIgnoreCase("TIDAL")
                                || Browser.equalsIgnoreCase("DEEZER"))
                                && !isSearchEnabled
                                && (ViewItemArray.get(position).getItemName().equalsIgnoreCase("search"))) {
                            if (!isSearchEnabled)
                                luciControl.SendCommand(MIDCONST.MID_REMOTE, LUCIMESSAGES.SELECT_ITEM + ":" + position, LSSDPCONST.LUCI_SET);
                            isSearchEnabled = true;

                        } else if ((Browser.equalsIgnoreCase("TIDAL")
                                || Browser.equalsIgnoreCase("DEEZER"))
                                && isSearchEnabled
                                &&
                                (ViewItemArray.get(position).getItemName().startsWith("playlist")
                                        ||
                                        ViewItemArray.get(position).getItemName().startsWith("artist")
                                        ||
                                        ViewItemArray.get(position).getItemName().startsWith("album")
                                        ||
                                        ViewItemArray.get(position).getItemName().startsWith("track")
                                        ||
                                        ViewItemArray.get(position).getItemName().equalsIgnoreCase("podcast"))) {
                            searchingDialog(position);

                        } else {
                            luciControl.SendCommand(MIDCONST.MID_REMOTE, LUCIMESSAGES.SELECT_ITEM + ":" + position, LSSDPCONST.LUCI_SET);
                            showLoader();
                        }

                    }
                })
        );*/

        /////////////////////////// tracks limit //////////////////////////////////////


        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                //  Toast.makeText(getApplicationContext(),"on scroll ",Toast.LENGTH_SHORT).show();
                if (newState == RecyclerView.SCROLL_STATE_SETTLING) {
                    if (mLayoutManager.findFirstVisibleItemPosition() == 0) {

                        LUCIControl luciControl = new LUCIControl(currentIpaddress);
                        luciControl.SendCommand(MIDCONST.MID_REMOTE, "SCROLLUP", LSSDPCONST.LUCI_SET);
                        gotolastpostion = true;
                        //  mLayoutManager.scrollToPosition(49);
                        m_progressDlg = ProgressDialog.show(TuneInRemoteSourcesList.this, getString(R.string.notice), getString(R.string.loading), true, true, null);
                        LibreLogger.d(this, "recycling " + "SCROLL UP" + "and find first visible item position" +
                                mLayoutManager.findFirstVisibleItemPosition() + "find last visible item" +
                                mLayoutManager.findLastVisibleItemPosition() + "find last completely visible item" +
                                mLayoutManager.findLastCompletelyVisibleItemPosition());
                        // Toast.makeText(getApplicationContext(),"0",Toast.LENGTH_SHORT).show();


                    }
                    if (mLayoutManager.findLastVisibleItemPosition() >= 49) {
                        LibreLogger.d(this, "recycling " + "last visible Item Position" + mLayoutManager.findLastVisibleItemPosition());
                        LUCIControl luciControl = new LUCIControl(currentIpaddress);
                        luciControl.SendCommand(MIDCONST.MID_REMOTE, "SCROLLDOWN", LSSDPCONST.LUCI_SET);
                        gotolastpostion = false;
                        LibreLogger.d(this, "recycling " + "SCROLL DOWN");
                        // mLayoutManager.scrollToPosition(0);
                        m_progressDlg = ProgressDialog.show(TuneInRemoteSourcesList.this, "Notice", "Loading...", true, true, null);
                        LibreLogger.d(this, "recycling " + "Last song");
                        Toast.makeText(getApplicationContext(), getString(R.string.lastSong), Toast.LENGTH_SHORT).show();
                    }
                }


            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

            }
        });


        ///////////////////////////////////////////////////////////////// limit end //////////////////////////////

        m_back = (ImageButton) findViewById(R.id.back);
        m_back.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        homeButton = (ImageButton) findViewById(R.id.homebutton);
        homeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

                LibreLogger.d(this, "user pressed home button ");
                unRegisterForDeviceEvents();
                //   luciControl.SendCommand(MIDCONST.MID_REMOTE_UI, GET_HOME, LSSDPCONST.LUCI_SET);

//                Intent intent = new Intent(TuneInRemoteSourcesList.this, SourcesOptionActivity.class);
                Intent intent = new Intent(TuneInRemoteSourcesList.this, CTNowPlayingActivity.class);
                intent.putExtra(Constants.CURRENT_DEVICE_IP, currentIpaddress);
                startActivity(intent);
                finish();

                //overridePendingTransition(R.anim.in_from_left, R.anim.out_to_right);
            }
        });


    }


    /*Searching dialog */
    private void searchingDialog(final int position) {
        // custom dialog
        final Dialog dialog = new Dialog(TuneInRemoteSourcesList.this);
        dialog.setContentView(R.layout.deezer_auth_dialog);
        dialog.setTitle(getString(R.string.searchText));

        // set the custom dialog components - text, image and button
        final EditText searchString = (EditText) dialog.findViewById(R.id.user_name);
        final EditText userPassword = (EditText) dialog.findViewById(R.id.user_password);


        searchString.setHint(getString(R.string.enterText));
        userPassword.setVisibility(View.GONE);

        Button submitButton = (Button) dialog.findViewById(R.id.deezer_ok_button);
        // if button is clicked, close the custom dialog
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (TextUtils.isEmpty(searchString.getText().toString())) {
                    Toast.makeText(TuneInRemoteSourcesList.this, getString(R.string.enterText), Toast.LENGTH_SHORT).show();
                    return;
                }
                m_progressDlg = ProgressDialog.show(TuneInRemoteSourcesList.this, getString(R.string.notice), getString(R.string.loading), true, true, null);


                hideKeyBoard(searchString);

                String searchDataToBeSent = "SEARCH_" + searchString.getText().toString();
                String selectedPositionToBeSent = LUCIMESSAGES.SELECT_ITEM + ":" + position;

                Log.d("SearchString", "--" + searchDataToBeSent);
                ArrayList<LUCIPacket> luciPackets = new ArrayList<>();

                LUCIPacket searchPacket = new LUCIPacket(searchDataToBeSent.getBytes(), (short) searchDataToBeSent.length(), (short) MIDCONST.MID_REMOTE, (byte) LSSDPCONST.LUCI_SET);
                luciPackets.add(searchPacket);

                LUCIPacket positionPacket = new LUCIPacket(selectedPositionToBeSent.getBytes(),
                        (short) selectedPositionToBeSent.length(), (short) MIDCONST.MID_REMOTE, (byte) LSSDPCONST.LUCI_SET);
                luciPackets.add(positionPacket);
                luciControl.SendCommand(luciPackets);


                dialog.dismiss();
            }
        });

        dialog.show();
    }


    /*showing dialog*/
    private void showLoader() {

        if (TuneInRemoteSourcesList.this.isFinishing())
            return;

        if (m_progressDlg == null)
            m_progressDlg = ProgressDialog.show(TuneInRemoteSourcesList.this, getString(R.string.notice), getString(R.string.loading), true, true, null);

        if (m_progressDlg.isShowing() == false) {
            m_progressDlg.show();
        }
    }

    private void closeLoader() {

        if (m_progressDlg != null) {

            if (m_progressDlg.isShowing()) {
                m_progressDlg.dismiss();
            }

        }
    }

    @Override
    public void deviceDiscoveryAfterClearingTheCacheStarted() {

    }

    @Override
    public void newDeviceFound(LSSDPNodes node) {

    }

    @Override
    public void deviceGotRemoved(String mIpAddress) {

    }

    @Override
    public void messageRecieved(NettyData dataRecived) {

        byte[] buffer = dataRecived.getMessage();
        String ipaddressRecieved = dataRecived.getRemotedeviceIp();

        LibreLogger.d(this, "Message recieved for ipaddress " + ipaddressRecieved);

        if (currentIpaddress.equalsIgnoreCase(ipaddressRecieved)) {
            LUCIPacket packet = new LUCIPacket(dataRecived.getMessage());
            switch (packet.getCommand()) {

                case 42:
                    String message = new String(packet.getpayload());
                    LibreLogger.d(this, " message 42 recieved  " + message);
                    try {
                        presentJsonHashCode = message.hashCode();
                        LibreLogger.d(this," present hash code : the hash code for "+message +" is "+presentJsonHashCode);
                        parseJsonAndReflectInUI(message);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        LibreLogger.d(this, " Json exception ");

                    }
                    break;

            }

        }
    }

    /**
     * This function gets the Json string
     */
    private void parseJsonAndReflectInUI(String jsonStr) throws JSONException {

        LibreLogger.d(this, "Json Recieved from remote device " + jsonStr);
        if (jsonStr != null) {

            try {

                JSONObject root = new JSONObject(jsonStr);
                int cmd_id = root.getInt(TAG_CMD_ID);
                JSONObject window = root.getJSONObject(TAG_WINDOW_CONTENT);
                LibreLogger.d(this, "Command Id" + cmd_id);

                if (isSongSlected==false)
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            handler.removeMessages(TIMEUP);
                            closeLoader();
                        }
                    });


                }
                if (cmd_id == 3) {
                    /* This means user has selected the song to be playing and hence we will need to navigate
                     him to the Active scene list
                      */

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            handler.removeMessages(TIMEUP);
                            closeLoader();
                        }
                    });


                    unRegisterForDeviceEvents();
                    //Intent intent = new Intent(RemoteSourcesList.this, ActiveScenesListActivity.class);
                    Intent intent = new Intent(TuneInRemoteSourcesList.this, CTNowPlayingActivity.class);
                    intent.putExtra(Constants.CURRENT_DEVICE_IP, currentIpaddress);
                    SceneObject sceneObjectFromCentralRepo = ScanningHandler.getInstance().getSceneObjectFromCentralRepo(currentIpaddress);
                    if (sceneObjectFromCentralRepo != null) {
                        sceneObjectFromCentralRepo.setCurrentSource(current_source_index_selected);
                    }
                    startActivity(intent);
                    finish();

                } else if (cmd_id == 1) {

                    Browser = window.getString(TAG_BROWSER);
                    int Cur_Index = window.getInt(TAG_CUR_INDEX);
                    Integer item_count = window.getInt(TAG_ITEM_COUNT);

                    if (Browser.equalsIgnoreCase("HOME")) {
                       /* This means we have reached the home collection and hence we need to lauch the SourcesOptionEntry Activity */
                        unRegisterForDeviceEvents();
//                        Intent intent = new Intent(TuneInRemoteSourcesList.this, SourcesOptionActivity.class);
                        Intent intent = new Intent(TuneInRemoteSourcesList.this, CTNowPlayingActivity.class);
                        intent.putExtra(Constants.CURRENT_DEVICE_IP, currentIpaddress);
                        startActivity(intent);
                        finish();
                        return;
                    }

                    if (item_count == 0)
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(TuneInRemoteSourcesList.this, "No item-Empty", Toast.LENGTH_SHORT).show();
                            }
                        });

                    JSONArray ItemList = window.getJSONArray(TAG_ITEM_LIST);
                    LibreLogger.d(this, "JSON PARSER item_count =  " + item_count + "  Array SIZE = " + ItemList.length());

                    final ArrayList<DataItem> tempArray = new ArrayList<>();

                    for (int i = 0; i < ItemList.length(); i++) {
                        JSONObject item = ItemList.getJSONObject(i);
                        DataItem viewItem = new DataItem();
                        viewItem.setItemID(item.getInt(TAG_ITEM_ID));
                        viewItem.setItemType(item.getString(TAG_ITEM_TYPE));
                        viewItem.setItemName(item.getString(TAG_ITEM_NAME));
                        viewItem.setFavorite(item.getInt(TAG_ITEM_FAVORITE));

                        if(current_source_index_selected == 6){
                            viewItem.setItemAlbumURL(item.getString(TAG_ITEM_ALBUMURL));
                        }

                        if (searchOptionClicked){
                            //This JSON is the result,when user clicked search
                            // put to hashcode
                            searchJsonHashCode = jsonStr.hashCode();
                            LibreLogger.d(this,"Search hash code : the hash code for "+jsonStr +" is "+ searchJsonHashCode);
                            searchOptionClicked = false;

                            //save it in shared preference
                            boolean savedInPref = saveInSharedPreference(searchJsonHashCode);
                            if (savedInPref){
                                LibreLogger.d(this,"saved in shared preference");
                            }else{
                                LibreLogger.d(this,"not saved in shared preference");
                            }
                        }

                        /*this is temp for search screen*/
                    /*    if (viewItem.getItemType().trim().equalsIgnoreCase("Folder") && viewItem.getItemName().trim().equalsIgnoreCase("search"))
                            isSearchEnabled = false;*/

                        tempArray.add(viewItem);
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ViewItemArray.clear();
                            ViewItemArray.addAll(tempArray);
                            mAdapter.notifyDataSetChanged();
                            if (gotolastpostion)
                                mLayoutManager.scrollToPosition(49);
                            else
                                mLayoutManager.scrollToPosition(0);
                            gotolastpostion = false;

                        }
                    });

                }
            } catch (Exception e) {

            }

        }


    }

    private boolean saveInSharedPreference(int hashResult){
        try {
            SharedPreferences sharedpreferences = getApplicationContext()
                    .getSharedPreferences(Constants.SEARCH_RESULT_HASH_CODE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putInt(Constants.SEARCH_RESULT_HASH_CODE_VALUE, hashResult);
            editor.commit();
        }catch (Exception e){
            return false;
        }
        return true;
    }

    protected void onStop() {
        super.onStop();
        unRegisterForDeviceEvents();
        handler.removeMessages(TIMEUP);
    }

    @Override
    public void onBackPressed() {
       /* Sends the back command issues*/
//        isSearchEnabled = false;
        luciControl.SendCommand(MIDCONST.MID_REMOTE_UI, BACK, LSSDPCONST.LUCI_SET);

        /*Showing loader*/
        showLoader();
        handler.sendEmptyMessageDelayed(TIMEUP, 10000);
    }

    private void setTitleForTheBrowser(int current_source_index_selected) {
        TextView title = (TextView) findViewById(R.id.choosesong);

        switch (current_source_index_selected) {

            case 0:
                title.setText("Music Server");
                title.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                break;
            case 1:
                title.setText("vTuner");
                title.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.vtuner_logo_certification, 0, 0, 0);
                break;
            case 2:
                title.setText("");
                title.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.tune_in_title_logo, 0, 0, 0);
                break;
            case 3:
                title.setText("USB");
                title.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                //sourceIcon.setVisibility(View.GONE);
                break;
            case 4:
                title.setText("SD Card");
                title.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
               // sourceIcon.setVisibility(View.GONE);
                break;
            case 5:
                title.setText("Deezer");
                title.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.deezer_crtification_logo, 0, 0, 0);
                break;
            case 6:
                title.setText("");
                title.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.tidal_title, 0, 0, 0);
               // sourceIcon.setVisibility(View.GONE);
                break;
            case 7:
                title.setText("Favourites");
                title.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                //sourceIcon.setVisibility(View.GONE);
                break;


        }

    }

    private void hideKeyBoard(View view) {
//        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }


    @Override
    public void onRemoteItemClicked(int position) {

        LibreLogger.d(this, "Play is clciked at position " + position);

        if (Browser.equalsIgnoreCase("TUNEIN")
                && ViewItemArray.get(position).getItemName().equalsIgnoreCase("search")) {
            searchingDialog(position);

        } else if (Browser.equalsIgnoreCase("VTUNER")
                && ViewItemArray.get(position).getItemName().equalsIgnoreCase("search")) {
            searchingDialog(position);

        } else if ((Browser.equalsIgnoreCase("TIDAL")
                || Browser.equalsIgnoreCase("DEEZER"))
            //    && !isSearchEnabled
                && (ViewItemArray.get(position).getItemName().equalsIgnoreCase("search"))) {

            luciControl.SendCommand(MIDCONST.MID_REMOTE, LUCIMESSAGES.SELECT_ITEM + ":" + position, LSSDPCONST.LUCI_SET);
          //  isSearchEnabled = true;
            searchOptionClicked = true;
            LibreLogger.d(this,"Next JSON that comes is search result. Make hash code with the next result");

        } else if ((Browser.equalsIgnoreCase("TIDAL")
                || Browser.equalsIgnoreCase("DEEZER"))
               // && isSearchEnabled
                && (ViewItemArray.get(position).getItemName().toLowerCase().startsWith("playlist")
                ||
                ViewItemArray.get(position).getItemName().toLowerCase().startsWith("artist")
                ||
                ViewItemArray.get(position).getItemName().toLowerCase().startsWith("album")
                ||
                ViewItemArray.get(position).getItemName().toLowerCase().startsWith("track")
                ||
                ViewItemArray.get(position).getItemName().equalsIgnoreCase("podcast"))) {

            if(presentJsonHashCode == searchJsonHashCode){
                LibreLogger.d(this,"hash codes matched. Can show dialog");
                searchingDialog(position);
            }else {
                LibreLogger.d(this,"hash codes did not match.");
                luciControl.SendCommand(MIDCONST.MID_REMOTE, LUCIMESSAGES.SELECT_ITEM + ":" + position, LSSDPCONST.LUCI_SET);
                showLoader();
            }

        } else {
            if (ViewItemArray.get(position).getItemType().contains("File")) {
  
        /* Commented as we need not force DMR stopMediaServer now
              try {
                    LibreLogger.d(this, "Going to Remove the DMR Playback");
                    RemoteDevice renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(currentIpaddress);
                    /* For the New DMR Implementation , whenever a Source Switching is happening
                    * we are Stopping the Playback
                    if (renderingDevice != null) {
                        String renderingUDN = renderingDevice.getIdentity().getUdn().toString();
                        PlaybackHelper playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP.get(renderingUDN);
                        playbackHelper.StopPlayback();
                        playbackHelper = null;
                        LibreApplication.PLAYBACK_HELPER_MAP.remove(renderingUDN);
                    }
                } catch (Exception e) {
                    LibreLogger.d(this, "Removing the DMR Playback is Exception");
                }*/

                isSongSlected = true;
            }

            luciControl.SendCommand(MIDCONST.MID_REMOTE, LUCIMESSAGES.SELECT_ITEM + ":" + position, LSSDPCONST.LUCI_SET);
            showLoader();
        }


    }

    @Override
    public void onRemoteFavClicked(int position) {
        DataItem dataItem = ViewItemArray.get(position);
        if (dataItem == null)
            return;
        if (dataItem.getFavorite() == null)
            return;

        if (dataItem.getFavorite() == 2) {
            /*remove here*/
            luciControl.SendCommand(MIDCONST.MID_REMOTE, LUCIMESSAGES.REMOVE_FAVORITE_ITEM + ":" + position, LSSDPCONST.LUCI_SET);
            dataItem.setFavorite(1);
            mAdapter.notifyDataSetChanged();
            return;
        }
        if (dataItem.getFavorite() == 1) {
            /*make fav here*/
            luciControl.SendCommand(MIDCONST.MID_REMOTE, LUCIMESSAGES.FAVORITE_ITEM + ":" + position, LSSDPCONST.LUCI_SET);
            dataItem.setFavorite(2);
            mAdapter.notifyDataSetChanged();
        }


    }
}