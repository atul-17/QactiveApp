package com.libre.ManageDevice;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.libre.ActiveScenesListActivity;
import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.libre.R;
import com.libre.constants.LUCIMESSAGES;
import com.libre.constants.MIDCONST;
import com.libre.luci.LSSDPNodes;
import com.libre.luci.LUCIControl;
import com.libre.luci.LUCIPacket;
import com.libre.netty.LibreDeviceInteractionListner;
import com.libre.netty.NettyData;
import com.libre.nowplaying.NowPlayingActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by karunakaran on 8/10/2015.
 */
public class BrowserActivity extends CTDeviceDiscoveryActivity implements LibreDeviceInteractionListner {
    RecyclerView recyclerView;
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
    LinearLayoutManager mLayoutManager;
    ArrayList<DataItem> ViewItemArray;
    MyAdapter mAdapter;
    String mMasterIP;
    boolean gotolastpostion;
    private int cmd_id =0;
    String Browser;
    Integer Cur_Index;
    Button Back;

    public static final ArrayList<String> sourcesList = new ArrayList<>();

    static {
        sourcesList.add("Deezer");
        sourcesList.add("Tidal");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        ViewItemArray = new ArrayList<>();
        setContentView(R.layout.activity_browser);
        Back = (Button) findViewById(R.id.btnBack);
        Back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent newIntent = new Intent(BrowserActivity.this, ActiveScenesListActivity.class);
                startActivity(newIntent);
                finish();
            }
        });
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        Intent myIntent = getIntent(); // gets the previously created intent
        mMasterIP = myIntent.getStringExtra("master_ip");
        recyclerView = (RecyclerView) findViewById(R.id.broswerRecyclerview);
        recyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
        mAdapter = new MyAdapter(ViewItemArray);
        registerForDeviceEvents(this);
        recyclerView.setAdapter(mAdapter);

        LUCIControl luciControl = new LUCIControl(mMasterIP);
        luciControl.sendAsynchronousCommand();
        luciControl.SendCommand(MIDCONST.MID_REMOTE, "GETUI", 2);
        luciControl.SendCommand(MIDCONST.MID_REMOTE, LUCIMESSAGES.BACK, 2);

        recyclerView.setAdapter(mAdapter);
        recyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        if (Browser.equalsIgnoreCase("Home") && autoFill(ViewItemArray.get(position).getItemName())) {
                            /*show dialog here*/
                            deezerAuthDialog(position);

                        }else {
                            LUCIControl luciControl = new LUCIControl(mMasterIP);
                            luciControl.sendAsynchronousCommand();
                            luciControl.SendCommand(MIDCONST.MID_REMOTE, LUCIMESSAGES.SELECT_ITEM + ":" + position, 2);
                        }

                    }
                })
        );


        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (newState == 2) {
                    if (mLayoutManager.findFirstVisibleItemPosition() == 0) {

                        LUCIControl luciControl = new LUCIControl(mMasterIP);
                        luciControl.sendAsynchronousCommand();
                        luciControl.SendCommand(MIDCONST.MID_REMOTE, LUCIMESSAGES.SCROLL_UP, 2);

                        gotolastpostion = true;
                          mLayoutManager.scrollToPosition(49);
  //                      m_progressDlg = ProgressDialog.show(LmpListViewActivity.this, "Notice", "Loading...", true, true, null);
    //                    Log.e(TAG, "SCROLL UP" + "and find first visible item position" +
      //                          mLayoutManager.findFirstVisibleItemPosition() + "find last visible item" +
        //                        mLayoutManager.findLastVisibleItemPosition() + "find last completely visible item" +
          //                      mLayoutManager.findLastCompletelyVisibleItemPosition());


                    }
                    if (mLayoutManager.findLastVisibleItemPosition() == 49) {
            //            Log.d(TAG, "last visible Item Position" + mLayoutManager.findLastVisibleItemPosition());
                        LUCIControl luciControl = new LUCIControl(mMasterIP);
                        luciControl.SendCommand(MIDCONST.MID_REMOTE, LUCIMESSAGES.SCROLL_DOWN, 2);

                        gotolastpostion = false;
                      //  Log.d(TAG, "SCROLL DOWN");
                        mLayoutManager.scrollToPosition(0);
                        //m_progressDlg = ProgressDialog.show(LmpListViewActivity.this, "Notice", "Loading...", true, true, null);
                        //Log.d(TAG, "Last song");
                    }
                }


            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });
    }
    /*need to check this message*/
    private boolean autoFill(String key) {

        for (String values : sourcesList) {
            if (values.equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }


    /*Dezzer dialog */
    private void deezerAuthDialog(final int position) {
        // custom dialog
        final Dialog dialog = new Dialog(BrowserActivity.this);
        dialog.setContentView(R.layout.deezer_auth_dialog);
        dialog.setTitle(getString(R.string.enterUsernamePassword));

        // set the custom dialog components - text, image and button
        final EditText userName = (EditText) dialog.findViewById(R.id.user_name);
        final EditText userPassword = (EditText) dialog.findViewById(R.id.user_password);



            /*get name from preferences and set it here*/
   /*     String storedInfo = preference.getLoginCredential(ViewItemArray.get(position).getItemName());
        if (storedInfo != null) {
            String[] credentials = storedInfo.split("::");

            userName.setText(credentials[0]);
            userPassword.setText(credentials[1]);
        }

*/
        Button submitButton = (Button) dialog.findViewById(R.id.deezer_ok_button);
        // if button is clicked, close the custom dialog
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (TextUtils.isEmpty(userName.getText().toString())) {
                    Toast.makeText(BrowserActivity.this, getString(R.string.enterUsername), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(userPassword.getText().toString())) {
                    Toast.makeText(BrowserActivity.this, getString(R.string.enterPassword), Toast.LENGTH_SHORT).show();
                    return;
                }

                LUCIControl luciControl = new LUCIControl(mMasterIP);
                luciControl.SendCommand(MIDCONST.MID_DEEZER, "DEEZERUSERNAME_" + userName.getText().toString(),
                        2);
                luciControl.SendCommand(MIDCONST.MID_DEEZER, "DEEZERPASSWORD_" + userPassword.getText().toString(),
                        2);


                /*Add to sharedpreferences */
              //  preference.storeSources(ViewItemArray.get(position).getItemName(), userName.getText().toString(), userPassword.getText().toString());
                /*Send this command*/
                luciControl.SendCommand(MIDCONST.MID_REMOTE, LUCIMESSAGES.SELECT_ITEM + ":" + position, 2);

                dialog.dismiss();
            }
        });

        dialog.show();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_remote, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        LUCIControl luciControl = new LUCIControl(mMasterIP);
        luciControl.sendAsynchronousCommand();
        // Take appropriate action for each action item click
        switch (item.getItemId()) {
            case R.id.remote_action_home:

                luciControl.SendCommand(MIDCONST.MID_REMOTE, LUCIMESSAGES.GET_HOME, 2);

                return true;

            case R.id.remote_action_play:

                luciControl.SendCommand(MIDCONST.MID_REMOTE, LUCIMESSAGES.GET_PLAY, 2);
                Intent newIntent = new Intent(BrowserActivity.this, NowPlayingActivity.class);
                finish();
                startActivity(newIntent);

                return true;

            case android.R.id.home: {
                if (Browser.equals("HOME"))

                {
                   // m_progressDlg = ProgressDialog.show(LmpListViewActivity.this, "Notice", "Going back....", true, true, null);
                    luciControl.SendCommand(MIDCONST.MID_REMOTE, LUCIMESSAGES.BACK, 2);


                }
            }
            return true;

           /* case R.id.now_playing:
                Intent i = new Intent(LmpListViewActivity.this, AlexaSignInActivity.class);
                i.putExtra("key", played);

                startActivity(i);
                startActivity(new Intent(this, AlexaSignInActivity.class));

                finish();
                return true;

*/
            default:
                return super.onOptionsItemSelected(item);
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
    public void messageRecieved(NettyData dataReceived) {
        try {
            Log.e("BrowserActivity", "Command Id" + dataReceived.getRemotedeviceIp());
            LUCIPacket packet = new LUCIPacket(dataReceived.getMessage());
            String jsonStr = new String(packet.payload);
            Log.e("BrowserActivity", "Command Id" + jsonStr);
            if(jsonStr!=null) {
                try {
                    JSONObject root = new JSONObject(jsonStr);
                    cmd_id = root.getInt(TAG_CMD_ID);
                    JSONObject window = root.getJSONObject(TAG_WINDOW_CONTENT);
                    Log.e("BrowserActivity", "Command Id" + cmd_id);
                    if(cmd_id==1) {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                recyclerView.setVisibility(View.VISIBLE);
                                //        playview.setVisibility(View.INVISIBLE);
                            }
                        });


                        Browser = window.getString(TAG_BROWSER);

                        Cur_Index = window.getInt(TAG_CUR_INDEX);
                        Integer item_count = window.getInt(TAG_ITEM_COUNT);
                        if (item_count == 0)
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(BrowserActivity.this, "No item-Empty", Toast.LENGTH_SHORT).show();
                                }
                            });


                        JSONArray ItemList = window.getJSONArray(TAG_ITEM_LIST);
                        Log.v("Browser Activity", "JSON PARSER item_count =  " + item_count + "  Array SIZE = " + ItemList.length());
                        // looping through All Contacts
                        final ArrayList<DataItem> tempArray = new ArrayList<>();

                        for (int i = 0; i < ItemList.length(); i++) {
                            JSONObject item = ItemList.getJSONObject(i);
                            DataItem viewItem = new DataItem();
                            viewItem.setItemID(item.getInt(TAG_ITEM_ID));
                            viewItem.setItemType(item.getString(TAG_ITEM_TYPE));
                            viewItem.setItemName(item.getString(TAG_ITEM_NAME));

                            tempArray.add(viewItem);
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                ViewItemArray.clear();
                                ViewItemArray.addAll(tempArray);


                            }
                        });

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mAdapter.notifyDataSetChanged();
                                if (gotolastpostion)
                                    mLayoutManager.scrollToPosition(49);
                                else
                                    mLayoutManager.scrollToPosition(0);
                                gotolastpostion = false;

                            }
                        });
                    }else if(cmd_id==3){
                        LUCIControl luciControl = new LUCIControl(mMasterIP);
                        luciControl.sendAsynchronousCommand();
                        luciControl.SendCommand(MIDCONST.MID_REMOTE, LUCIMESSAGES.GET_PLAY, 2);
                       /* try {
                            Intent newIntent = new Intent(BrowserActivity.this, ActiveScenesListActivity.class);
                            finish();
                            startActivity(newIntent);
                        }catch(Exception e){

                        }*/
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();



    }
}
