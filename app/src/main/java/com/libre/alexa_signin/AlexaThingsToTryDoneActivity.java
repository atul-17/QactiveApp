package com.libre.alexa_signin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.cumulations.libreV2.activity.CTConnectingToMainNetwork;
import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.libre.Network.LSSDPDeviceNetworkSettings;
import com.libre.R;
import com.libre.Scanning.Constants;
import com.libre.SourcesOptionActivity;
import com.libre.constants.LSSDPCONST;
import com.libre.constants.LUCIMESSAGES;
import com.libre.constants.MIDCONST;
import com.libre.luci.LSSDPNodeDB;
import com.libre.luci.LSSDPNodes;
import com.libre.luci.LUCIControl;
import com.libre.util.LibreLogger;


/**
 * Created by amrit on 12/14/2016.
 */

public class AlexaThingsToTryDoneActivity extends CTDeviceDiscoveryActivity implements View.OnClickListener {

    private String speakerIpaddress;
    private TextView text_done,mAlexaApp;
    private Button signOutBtn,changeLangBtn;
    private TextView back;
    private String fromActivity;
    private String prevScreen;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.alexa_things_to_try_done);

        Intent myIntent = getIntent();
        if (myIntent != null) {
            speakerIpaddress = myIntent.getStringExtra(Constants.CURRENT_DEVICE_IP);
        }

        text_done = (TextView) findViewById(R.id.text_done);
        mAlexaApp = (TextView)findViewById(R.id.tv3);
        changeLangBtn = (Button) findViewById(R.id.changeLangBtn);
        signOutBtn = (Button) findViewById(R.id.signOutBtn);
        back = (TextView) findViewById(R.id.back);

        text_done.setOnClickListener(this);
        mAlexaApp.setOnClickListener(this);
        changeLangBtn.setOnClickListener(this);
        signOutBtn.setOnClickListener(this);
        back.setOnClickListener(this);

        fromActivity = getIntent().getStringExtra(Constants.FROM_ACTIVITY);
        prevScreen = getIntent().getStringExtra(Constants.PREV_SCREEN);

        if (prevScreen!=null && !prevScreen.isEmpty()) {
            if (prevScreen.equalsIgnoreCase(AlexaSignInActivity.class.getSimpleName())) {
                changeLangBtn.setVisibility(View.GONE);
                signOutBtn.setVisibility(View.GONE);
            }
        }

//        if (fromActivity.equalsIgnoreCase(ConnectingToMainNetwork.class.getSimpleName())){
//            signOutBtn.setVisibility(View.GONE);
//        }
    }

    public void launchTheApp(String appPackageName) {

        Intent intent = getPackageManager().getLaunchIntentForPackage(appPackageName);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            redirectingToPlayStore(intent, appPackageName);
        }

    }


    public void redirectingToPlayStore(Intent intent, String appPackageName) {

        try {

            intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("market://details?id=" + appPackageName));
            startActivity(intent);

        } catch (android.content.ActivityNotFoundException anfe) {

            intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("http://play.google.com/store/apps/details?id=" + appPackageName));
            startActivity(intent);

        }

    }
    @Override
    public void onClick(View v) {

        LUCIControl luciControl = new LUCIControl(speakerIpaddress);
        switch (v.getId()){
            case R.id.signOutBtn:
                luciControl.SendCommand(MIDCONST.ALEXA_COMMAND, LUCIMESSAGES.SIGN_OUT, LSSDPCONST.LUCI_SET);
                LSSDPNodes mNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(speakerIpaddress);
                if(mNode!=null) {
                    mNode.setAlexaRefreshToken("");
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Intent i = new Intent(AlexaThingsToTryDoneActivity.this, AlexaSignInActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.putExtra(Constants.CURRENT_DEVICE_IP, speakerIpaddress);
                i.putExtra(Constants.FROM_ACTIVITY, AlexaThingsToTryDoneActivity.class.getSimpleName());
                startActivity(i);
                LibreLogger.d(this, "clicked sign out");
                break;

            case R.id.changeLangBtn:

                Intent newIntent = new Intent(AlexaThingsToTryDoneActivity.this, AlexaLangUpdateActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                newIntent.putExtra(Constants.CURRENT_DEVICE_IP, speakerIpaddress);
                newIntent.putExtra(Constants.FROM_ACTIVITY,fromActivity);
                newIntent.putExtra(Constants.PREV_SCREEN, AlexaThingsToTryDoneActivity.class.getSimpleName());
                startActivity(newIntent);
                finish();
                break;

            case R.id.text_done:
            case R.id.back:

                if (fromActivity != null && !fromActivity.isEmpty()) {
                    if (fromActivity.equals(SourcesOptionActivity.class.getSimpleName())
                        || fromActivity.equals(AlexaSignInActivity.class.getSimpleName())
                        || fromActivity.equals(AlexaThingsToTryDoneActivity.class.getSimpleName())) {

                        Intent intent = new Intent(AlexaThingsToTryDoneActivity.this, SourcesOptionActivity.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(Constants.CURRENT_DEVICE_IP, speakerIpaddress);
                        startActivity(intent);
                        finish();
                    } else if (fromActivity.equals(CTConnectingToMainNetwork.class.getSimpleName())) {
                        intentToHome(this);
                    } else if (fromActivity.equals(LSSDPDeviceNetworkSettings.class.getSimpleName())) {
                        Intent intent = new Intent(AlexaThingsToTryDoneActivity.this, LSSDPDeviceNetworkSettings.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(Constants.CURRENT_DEVICE_IP, speakerIpaddress);
                        startActivity(intent);
                        finish();
                    }
                }
                break;

            case R.id.tv3:
                launchTheApp("com.amazon.dee.app");
                break;
        }

    }

}
