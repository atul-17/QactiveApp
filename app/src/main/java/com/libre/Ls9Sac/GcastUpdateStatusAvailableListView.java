package com.libre.Ls9Sac;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ListView;

import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.libre.LibreApplication;
import com.libre.PlayNewActivity;
import com.libre.R;
import com.libre.constants.MIDCONST;
import com.libre.luci.LSSDPNodes;
import com.libre.luci.LUCIPacket;
import com.libre.netty.LibreDeviceInteractionListner;
import com.libre.netty.NettyData;

public class GcastUpdateStatusAvailableListView extends CTDeviceDiscoveryActivity implements LibreDeviceInteractionListner {

    private  ListView mGcastListView;
    private  GcastUpdateAdapter mGcastAdapter;
    private Button btnDone;
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gcast_update_status_available);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        registerForDeviceEvents(this);

        mGcastListView = (ListView) findViewById(R.id.GcastUpdateListView);
        btnDone = (Button) findViewById(R.id.btnDone);
        mGcastAdapter = new GcastUpdateAdapter(GcastUpdateStatusAvailableListView.this);
        mGcastListView.setAdapter(mGcastAdapter);
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    @Override
    protected void onResume() {
        registerForDeviceEvents(this);
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_gcast_update_status_available, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void deviceDiscoveryAfterClearingTheCacheStarted() {

    }

    @Override
    public void newDeviceFound(LSSDPNodes node) {
        mGcastAdapter.notifyDataSetChanged();
        if(LibreApplication.GCAST_UPDATE_AVAILABE_LIST_DATA.keySet().size()==0)
            showDialogifNoDeviceFound(getResources().getString(R.string.noDeviceUpdating));


    }

    @Override
    public void deviceGotRemoved(String ipaddress) {

    }

    @Override
    public void messageRecieved(NettyData packet) {
        LUCIPacket mLuciPacket = new LUCIPacket(packet.getMessage());
        switch(mLuciPacket.getCommand()){
            case MIDCONST.GCAST_PROGRESS_STATUS:{

                String msg = new String (mLuciPacket.getpayload());
                if (msg!=null&&msg.equalsIgnoreCase("255")){
                    onBackPressed();
                }
                else {
                    mGcastAdapter.notifyDataSetChanged();
                }
                break;
            }

            case MIDCONST.GCAST_UPDATE_AVAILABLE: {
                mGcastAdapter.notifyDataSetChanged();
                break;
            }



        }
    }
    private AlertDialog NoDeviceFoundalert = null;
    private void showDialogifNoDeviceFound(String Message) {
        if (!GcastUpdateStatusAvailableListView.this.isFinishing()) {
            //alertDialog1 = null;
            AlertDialog.Builder builder = new AlertDialog.Builder(GcastUpdateStatusAvailableListView.this);

            builder.setMessage(Message)
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            NoDeviceFoundalert.dismiss();
                            if(LibreApplication.GCAST_UPDATE_AVAILABE_LIST_DATA.size()==0) {
                                Intent ssid = new Intent(GcastUpdateStatusAvailableListView.this,
                                        PlayNewActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(ssid);
                                finish();
                            }
                        }
                    });

            if (NoDeviceFoundalert == null) {
                NoDeviceFoundalert = builder.show();
                /*TextView messageView = (TextView) alertDialog1.findViewById(android.R.id.message);
                messageView.setGravity(Gravity.CENTER);*/
            }

            NoDeviceFoundalert.show();

        }
    }
}
