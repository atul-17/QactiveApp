package com.libre;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.cumulations.libreV2.activity.CTMediaSourcesActivity;
import com.cumulations.libreV2.activity.CTNowPlayingActivity;
import com.libre.LErrorHandeling.LibreError;
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

public class RemoteSourcesList extends CTDeviceDiscoveryActivity implements LibreDeviceInteractionListner, RecyclerViewAdapter.RemoteItemClickListener {


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
    final int NETWORK_TIMEOUT = 01;

    private static final String BACK = "BACK";
    public static final String GET_PLAY = "GETUI:PLAY";
    public static final String GET_HOME = "GETUI:HOME";
    private ImageButton m_back;
    private ImageButton homeButton;
    private int current_source_index_selected = -1;
    AlertDialog alert;
    // private boolean isSearchEnabled;
    private boolean isSongSlected = false;

    private int searchJsonHashCode;
    private int presentJsonHashCode;
    //searchOptionClicked is true when user clicks on search option. And then we calculate hash code for search JSON result
    private boolean searchOptionClicked = false;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case NETWORK_TIMEOUT:
                    LibreLogger.d(this, "handler message recieved");

                    closeLoader();
                    /*showing error*/
                    LibreError error = new LibreError(currentIpaddress, getString(R.string.requestTimeout));
                    showErrorMessage(error);

                    homeButton.performClick();
                    break;
            }
        }
    };

    private AppCompatTextView tvNoItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_sources_list);

        currentIpaddress = getIntent().getStringExtra(Constants.CURRENT_DEVICE_IP);
        current_source_index_selected = getIntent().getIntExtra(Constants.CURRENT_SOURCE_INDEX_SELECTED, -1);
        setTitleForTheBrowser(current_source_index_selected);

        if (current_source_index_selected < 0) {
            Toast.makeText(this, getString(R.string.sourceIndexWrong), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(RemoteSourcesList.this, CTMediaSourcesActivity.class);
            intent.putExtra(Constants.CURRENT_DEVICE_IP, currentIpaddress);
            SceneObject sceneObject = ScanningHandler.getInstance().getSceneObjectFromCentralRepo(currentIpaddress);
            intent.putExtra(Constants.CURRENT_SOURCE, sceneObject.getCurrentSource());
            startActivity(intent);
            finish();
        }

        LibreLogger.d(this, " Registered for the device " + currentIpaddress);

        luciControl = new LUCIControl(currentIpaddress);
        luciControl.sendAsynchronousCommand();
        luciControl.SendCommand(MIDCONST.MID_REMOTE_UI, LUCIMESSAGES.SELECT_ITEM + ":" + current_source_index_selected, LSSDPCONST.LUCI_SET);

        showLoader();

        //////////// timeout for dialog - showLoader() ///////////////////
        if (current_source_index_selected == 0) {
            /*increasing timeout for media servers only*/
            handler.sendEmptyMessageDelayed(NETWORK_TIMEOUT, 60000);
        } else {
            handler.sendEmptyMessageDelayed(NETWORK_TIMEOUT, 15000);
        }


        recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        tvNoItems = findViewById(R.id.tv_no_data);

        mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        ViewItemArray = new ArrayList<DataItem>();
        mAdapter = new RecyclerViewAdapter(this, ViewItemArray, this);
        recyclerView.setAdapter(mAdapter);


        /////////////////////////// tracks limit //////////////////////////////////////


    /*    recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
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
                        m_progressDlg = ProgressDialog.show(RemoteSourcesList.this, getString(R.string.notice),getString(R.string.loading), true, true, null);
                        LibreLogger.d(this, "recycling " + "SCROLL UP" + "and find first visible item position" +
                                mLayoutManager.findFirstVisibleItemPosition() + "find last visible item" +
                                mLayoutManager.findLastVisibleItemPosition() + "find last completely visible item" +
                                mLayoutManager.findLastCompletelyVisibleItemPosition());
                        // Toast.makeText(getApplicationContext(),"0",Toast.LENGTH_SHORT).show();


                    }
                    if (mLayoutManager.findLastVisibleItemPosition() == 49) {
                        LibreLogger.d(this, "recycling " + "last visible Item Position" + mLayoutManager.findLastVisibleItemPosition());
                        LUCIControl luciControl = new LUCIControl(currentIpaddress);
                        luciControl.SendCommand(MIDCONST.MID_REMOTE, "SCROLLDOWN", LSSDPCONST.LUCI_SET);
                        gotolastpostion = false;
                        LibreLogger.d(this, "recycling " + "SCROLL DOWN");
                        // mLayoutManager.scrollToPosition(0);
                        m_progressDlg = ProgressDialog.show(RemoteSourcesList.this, getString(R.string.notice),getString(R.string.loading), true, true, null);
                        LibreLogger.d(this, "recycling " + "Last song");
                        Toast.makeText(getApplicationContext(),getString(R.string.lastSong), Toast.LENGTH_SHORT).show();
                    }
                }


            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

            }
        });*/

        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            float beginY = 0, endY = 0;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                //ACTION_DOWN when the user first touches the screen. We are taking the beginY co ordinate here
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    LibreLogger.d(this, "action down, touched y co ordinate is " + motionEvent.getY());
                    beginY = motionEvent.getY();


                }
                //ACTION_UP, when the user finally releases the touch. We are taking the endY co ordinate here
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    LibreLogger.d(this, "action up, touch lifted y co ordinate is " + motionEvent.getY());
                    endY = motionEvent.getY();
                    if (beginY > endY) {
                        LibreLogger.d(this, "touched from bottom -> top, send scroll down");
                        sendScrollDown();
                    } else if (beginY < endY) {
                        LibreLogger.d(this, "touched from top -> bottom, send scroll up");
                        sendScrollUp();
                    }
                    //return false - means other touch events like onclick on the view item will now work.
                    return false;
                }

                return false;
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

                Intent intent = new Intent(RemoteSourcesList.this, CTMediaSourcesActivity.class);
                intent.putExtra(Constants.CURRENT_DEVICE_IP, currentIpaddress);
                SceneObject currentSceneObject = ScanningHandler.getInstance().getSceneObjectFromCentralRepo(currentIpaddress);
                intent.putExtra(Constants.CURRENT_SOURCE, "" + currentSceneObject.getCurrentSource());
                startActivity(intent);
                finish();

                //overridePendingTransition(R.anim.in_from_left, R.anim.out_to_right);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        /*Registering to receive messages*/
        registerForDeviceEvents(this);
    }

    private void sendScrollUp() {
        if (mLayoutManager.findFirstVisibleItemPosition() == 0) {

            LUCIControl luciControl = new LUCIControl(currentIpaddress);
            luciControl.SendCommand(MIDCONST.MID_REMOTE, "SCROLLUP", LSSDPCONST.LUCI_SET);
            gotolastpostion = true;
            //  mLayoutManager.scrollToPosition(49);
            m_progressDlg = ProgressDialog.show(RemoteSourcesList.this, getString(R.string.notice), getString(R.string.loading), true, true, null);
            LibreLogger.d(this, "recycling " + "SCROLL UP" + "and find first visible item position" +
                    mLayoutManager.findFirstVisibleItemPosition() + "find last visible item" +
                    mLayoutManager.findLastVisibleItemPosition() + "find last completely visible item" +
                    mLayoutManager.findLastCompletelyVisibleItemPosition());
            // Toast.makeText(getApplicationContext(),"0",Toast.LENGTH_SHORT).show();


        }
    }

    private void sendScrollDown() {
        if (mLayoutManager.findLastVisibleItemPosition() == 49) {
            LibreLogger.d(this, "recycling " + "last visible Item Position" + mLayoutManager.findLastVisibleItemPosition());
            LUCIControl luciControl = new LUCIControl(currentIpaddress);
            luciControl.SendCommand(MIDCONST.MID_REMOTE, "SCROLLDOWN", LSSDPCONST.LUCI_SET);
            gotolastpostion = false;
            LibreLogger.d(this, "recycling " + "SCROLL DOWN");
            // mLayoutManager.scrollToPosition(0);
            m_progressDlg = ProgressDialog.show(RemoteSourcesList.this, getString(R.string.notice), getString(R.string.loading), true, true, null);
            LibreLogger.d(this, "recycling " + "Last song");
            Toast.makeText(getApplicationContext(), getString(R.string.lastSong), Toast.LENGTH_SHORT).show();
        }
    }

    /*Searching dialog */
    private void searchingDialog(final int position) {
        // custom dialog
        final Dialog dialog = new Dialog(RemoteSourcesList.this);
        dialog.setContentView(R.layout.deezer_auth_dialog);
        dialog.setTitle(getString(R.string.searchTextPlaceHolder));

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

                if (TextUtils.isEmpty(searchString.getText().toString().trim())) {
                    Toast.makeText(RemoteSourcesList.this, getString(R.string.searchText), Toast.LENGTH_SHORT).show();
                    return;
                }
                m_progressDlg = ProgressDialog.show(RemoteSourcesList.this, getString(R.string.notice), getString(R.string.loading), true, true, null);


                hideKeyBoard(searchString);

                String searchDataToBeSent = "SEARCH_" + searchString.getText().toString();
                String selectedPositionToBeSent = LUCIMESSAGES.SELECT_ITEM + ":" + position;

                Log.d("SearchString", "--" + searchDataToBeSent);
                ArrayList<LUCIPacket> luciPackets = new ArrayList<LUCIPacket>();

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

    private void hideKeyBoard(View view) {
//        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /*showing dialog*/
    private void showLoader() {

        if (RemoteSourcesList.this.isFinishing())
            return;

        if (m_progressDlg == null) {
            m_progressDlg = ProgressDialog.show(RemoteSourcesList.this, getString(R.string.notice), getString(R.string.loading), true, true, null);
            m_progressDlg.setCancelable(false);
        }
        if (!m_progressDlg.isShowing()) {
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

                case 42: {
                    String message = new String(packet.getpayload());
                    LibreLogger.d(this, " message 42 recieved  " + message);
                    try {
                        closeLoader();
                        presentJsonHashCode = message.hashCode();
                        LibreLogger.d(this, " present hash code : the hash code for " + message + " is " + presentJsonHashCode);
                        parseJsonAndReflectInUI(message);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        LibreLogger.d(this, " Json exception ");

                    }
                }
                break;
                case 54: {
                    String message = new String(packet.getpayload());
                    LibreLogger.d(this, " message 54 recieved  " + message);
                    try {
                        LibreError error = null;
                        if (message.contains(Constants.FAIL)) {
                            error = new LibreError(currentIpaddress, Constants.FAIL_ALERT_TEXT);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    closeLoader();
                                    handler.removeMessages(NETWORK_TIMEOUT);
                                }
                            });

                        } else if (message.contains(Constants.SUCCESS)) {
                            closeLoader();
                        } else if (message.contains(Constants.NO_URL)) {
                            error = new LibreError(currentIpaddress, getResources().getString(R.string.NO_URL_ALERT_TEXT));
                        } else if (message.contains(Constants.NO_PREV_SONG)) {
                            error = new LibreError(currentIpaddress, getResources().getString(R.string.NO_PREV_SONG_ALERT_TEXT));
                        } else if (message.contains(Constants.NO_NEXT_SONG)) {
                            error = new LibreError(currentIpaddress, getResources().getString(R.string.NO_NEXT_SONG_ALERT_TEXT));
                        }
                        if (error != null)
                            showErrorMessage(error);

                    } catch (Exception e) {
                        e.printStackTrace();
                        LibreLogger.d(this, " Json exception ");

                    }
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

                if (isSongSlected == false) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeLoader();
                            handler.removeMessages(NETWORK_TIMEOUT);
                        }
                    });
                }
                JSONObject root = new JSONObject(jsonStr);
                int cmd_id = root.getInt(TAG_CMD_ID);
                JSONObject window = root.getJSONObject(TAG_WINDOW_CONTENT);
                LibreLogger.d(this, "Command Id" + cmd_id);

                if (cmd_id == 3) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeLoader();
                            handler.removeMessages(NETWORK_TIMEOUT);
                        }
                    });
                    /* This means user has selected the song to be playing and hence we will need to navigate
                     him to the Active scene list
                      */
                    unRegisterForDeviceEvents();


                    String newTrackname = window.getString("TrackName");
                    int newPlayState = window.getInt("PlayState");
                    int currentPlayState = window.getInt("Current_time");
                    int currentSource = window.getInt("Current Source");
                    String album_arturl = window.getString("CoverArtUrl");
                    String genre = window.getString("Genre");


                    String nAlbumName = window.getString("Album");
                    String nArtistName = window.getString("Artist");
                    long totaltime = window.getLong("TotalTime");

                    ScanningHandler mScanHandler = ScanningHandler.getInstance();
                    SceneObject currentSceneObject = mScanHandler.getSceneObjectFromCentralRepo(currentIpaddress);

                    if (currentSceneObject != null) {
                        // currentSceneObject.setSceneName(window.getString("TrackName"));
                        currentSceneObject.setPlaystatus(window.getInt("PlayState"));
                        currentSceneObject.setAlbum_art(album_arturl);
                        currentSceneObject.setPlayUrl(window.getString("PlayUrl"));
                        currentSceneObject.setTrackName(window.getString("TrackName"));
                        /*For favourite*/
                        currentSceneObject.setIsFavourite(window.getBoolean("Favourite"));
                        currentSceneObject.setCurrentSource(currentSource);


                        currentSceneObject.setShuffleState(window.getInt("Shuffle"));
                        currentSceneObject.setRepeatState(window.getInt("Repeat"));

                    }


                    //Intent intent = new Intent(RemoteSourcesList.this, ActiveScenesListActivity.class);
                    Intent intent = new Intent(RemoteSourcesList.this, CTNowPlayingActivity.class);
                    intent.putExtra(Constants.CURRENT_DEVICE_IP, currentIpaddress);
                    /*SceneObject sceneObjectFromCentralRepo = ScanningHandler.getInstance().getSceneObjectFromCentralRepo(currentIpaddress);
                    if (sceneObjectFromCentralRepo != null) {
                        sceneObjectFromCentralRepo.setCurrentSource(current_source_index_selected);
                    }*/
                    startActivity(intent);
//                    finish();

                } else if (cmd_id == 1) {

                    Browser = window.getString(TAG_BROWSER);
                    int Cur_Index = window.getInt(TAG_CUR_INDEX);
                    Integer item_count = window.getInt(TAG_ITEM_COUNT);

                    if (Browser.equalsIgnoreCase("HOME")) {
                        /* This means we have reached the home collection and hence we need to lauch the SourcesOptionEntry Activity */
                        unRegisterForDeviceEvents();
                        Intent intent = new Intent(RemoteSourcesList.this, CTMediaSourcesActivity.class);
                        intent.putExtra(Constants.CURRENT_DEVICE_IP, currentIpaddress);

                        SceneObject currentSceneObject = ScanningHandler.getInstance().getSceneObjectFromCentralRepo(currentIpaddress);
                        intent.putExtra(Constants.CURRENT_SOURCE, "" + currentSceneObject.getCurrentSource());
                        startActivity(intent);
                        finish();
                        return;
                    }

                    if (item_count == 0) {
                        LibreError error = new LibreError(currentIpaddress, getString(R.string.no_item_empty));
                        showErrorMessage(error);
                        tvNoItems.setVisibility(View.VISIBLE);
                    } else {
                        tvNoItems.setVisibility(View.GONE);
                    }

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


                        /* Accomudating older version of tidal implementation from device side */
                        if (current_source_index_selected == 6 && item.has(TAG_ITEM_ALBUMURL)) {
                            viewItem.setItemAlbumURL(item.getString(TAG_ITEM_ALBUMURL));
                        }

                        if (item.has(TAG_ITEM_ALBUMURL) && item.getString(TAG_ITEM_ALBUMURL) != null
                                && !item.getString(TAG_ITEM_ALBUMURL).isEmpty()) {
                            viewItem.setItemAlbumURL(item.getString(TAG_ITEM_ALBUMURL));
                        }

                        if (searchOptionClicked) {
                            //This JSON is the result,when user clicked search
                            // put to hashcode
                            searchJsonHashCode = jsonStr.hashCode();
                            LibreLogger.d(this, "Search hash code : the hash code for " + jsonStr + " is " + searchJsonHashCode);
                            searchOptionClicked = false;

                            //save it in shared preference
                            boolean savedInPref = saveInSharedPreference(searchJsonHashCode);
                            if (savedInPref) {
                                LibreLogger.d(this, "saved in shared preference");
                            } else {
                                LibreLogger.d(this, "not saved in shared preference");
                            }
                        }
//                         this is temp for search screen
                     /*   if (viewItem.getItemType().trim().equalsIgnoreCase("Folder") && viewItem.getItemName().trim().equalsIgnoreCase("search")) {
                            isSearchEnabled = false;
                       }
*/


                        tempArray.add(viewItem);
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ViewItemArray.clear();
                            ViewItemArray.addAll(tempArray);
                            mAdapter.replaceAdapterArray(ViewItemArray);
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
                e.printStackTrace();
            }

        }


    }

    private boolean saveInSharedPreference(int hashResult) {
        try {
            SharedPreferences sharedpreferences = getApplicationContext()
                    .getSharedPreferences(Constants.SEARCH_RESULT_HASH_CODE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putInt(Constants.SEARCH_RESULT_HASH_CODE_VALUE, hashResult);
            editor.commit();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    protected void onStop() {
        super.onStop();
        /*removing handler*/
        handler.removeCallbacksAndMessages(null);
        unRegisterForDeviceEvents();
    }

    @Override
    public void onBackPressed() {
        /* Sends the back command issues*/
        luciControl.SendCommand(MIDCONST.MID_REMOTE_UI, BACK, LSSDPCONST.LUCI_SET);
        showLoader();

        //////////// timeout for dialog - showLoader() ///////////////////
        if (current_source_index_selected == 0) {
            /*increasing timeout for media servers only*/
            handler.sendEmptyMessageDelayed(NETWORK_TIMEOUT, 60000);
        } else {
            handler.sendEmptyMessageDelayed(NETWORK_TIMEOUT, 15000);
        }

    }

    private void setTitleForTheBrowser(int current_source_index_selected) {
        TextView title = (TextView) findViewById(R.id.choosesong);
        ImageView sourceIcon = (ImageView) findViewById(R.id.sourceIcon);

        switch (current_source_index_selected) {

            case 0:
                title.setText("Music Server");
                title.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                break;
            case 1:
                title.setText("");
                title.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.vtuner_logo_remotesources_title, 0, 0, 0);


                break;
            case 2:
                title.setText("Tune In");
                title.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                break;
            case 3:
                title.setText("USB");
                title.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                break;
            case 4:
                title.setText("SD Card");
                title.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                break;
            case 5:
                title.setText("Deezer");
                title.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.deezer_crtification_logo, 0, 0, 0);
                break;
            case 6:
                title.setText("");
                title.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.tidal_title, 0, 0, 0);

                break;
            case 7:
                title.setText("Favourites");
                title.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                break;
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
                //  && !isSearchEnabled
                && (ViewItemArray.get(position).getItemName().equalsIgnoreCase("search"))) {

            luciControl.SendCommand(MIDCONST.MID_REMOTE, LUCIMESSAGES.SELECT_ITEM + ":" + position, LSSDPCONST.LUCI_SET);
            //isSearchEnabled = true;
            searchOptionClicked = true;
            LibreLogger.d(this, "Next JSON that comes is search result. Make hash code with the next result");

        } else if ((Browser.equalsIgnoreCase("TIDAL")
                || Browser.equalsIgnoreCase("DEEZER"))
                //  && isSearchEnabled
                && (ViewItemArray.get(position).getItemName().toLowerCase().startsWith("playlist")
                ||
                ViewItemArray.get(position).getItemName().toLowerCase().startsWith("artist")
                ||
                ViewItemArray.get(position).getItemName().toLowerCase().startsWith("album")
                ||
                ViewItemArray.get(position).getItemName().toLowerCase().startsWith("track")
                ||
                ViewItemArray.get(position).getItemName().equalsIgnoreCase("podcast"))) {
            // dialog should be shown only when it is one stage above "search"

            if (presentJsonHashCode == searchJsonHashCode) {
                LibreLogger.d(this, "hash codes matched. Can show dialog");
                searchingDialog(position);
            } else {
                LibreLogger.d(this, "hash codes did not match.");
                luciControl.SendCommand(MIDCONST.MID_REMOTE, LUCIMESSAGES.SELECT_ITEM + ":" + position, LSSDPCONST.LUCI_SET);
                showLoader();
            }


        } else {
            if (ViewItemArray.get(position).getItemType().contains("File")) {
               /*
                Commenting to avoid forcefull DMR stopping
                try {
                    LibreLogger.d(this, "Going to Remove the DMR Playback");
                    RemoteDevice renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(currentIpaddress);
                    *//* For the New DMR Implementation , whenever a Source Switching is happening
                 * we are Stopping the Playback *//*
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

            //////////// timeout for dialog - showLoader() ///////////////////
            if (current_source_index_selected == 0) {
                /*increasing timeout for media servers only*/
                handler.sendEmptyMessageDelayed(NETWORK_TIMEOUT, 60000);
            } else {
                handler.sendEmptyMessageDelayed(NETWORK_TIMEOUT, Constants.INTERNET_PLAY_TIMEOUT);
            }
            showLoader();
        }


    }

    @Override
    public void onRemoteFavClicked(int position) {
        DataItem dataItem = ViewItemArray.get(position);
        if (dataItem == null)
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
