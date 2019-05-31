package com.libre.alexa_signin;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.IdRes;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.cumulations.libreV2.activity.CTAlexaThingsToTryActivity;
import com.libre.ActiveScenesListActivity;
import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.libre.LErrorHandeling.LibreError;
import com.libre.Ls9Sac.ConnectingToMainNetwork;
import com.libre.Network.LSSDPDeviceNetworkSettings;
import com.libre.R;
import com.libre.Scanning.Constants;
import com.libre.SourcesOptionActivity;
import com.libre.alexa.LibreAlexaConstants;
import com.libre.constants.LSSDPCONST;
import com.libre.constants.LUCIMESSAGES;
import com.libre.constants.MIDCONST;
import com.libre.luci.LSSDPNodes;
import com.libre.luci.LUCIControl;
import com.libre.luci.LUCIPacket;
import com.libre.netty.LibreDeviceInteractionListner;
import com.libre.netty.NettyData;
import com.libre.util.LibreLogger;

import org.json.JSONException;
import org.json.JSONObject;

public class AlexaLangUpdateActivity extends CTDeviceDiscoveryActivity implements LibreDeviceInteractionListner {
    private RadioGroup chooseLangRg;
    private TextView done, back;
    private String selectedLang = "";
    private boolean changesDone;
    private LUCIControl luciControl;
    private String currentDeviceIp;
    private String fromActivity;
    private String prevScreen;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            if (msg.what == Constants.ALEXA_CHECK_TIMEOUT) {
                closeLoader();
                LibreError libreError = new LibreError(currentDeviceIp,getString(R.string.requestTimeout));
                showErrorMessage(libreError);
            }
        }
    };

    private ProgressDialog progressDialog;

    private void closeLoader() {
        if (progressDialog!=null && progressDialog.isShowing())
            progressDialog.dismiss();
        mHandler.removeMessages(Constants.ALEXA_CHECK_TIMEOUT);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alexa_update_lang);

        chooseLangRg = (RadioGroup) findViewById(R.id.chooseLangRg);
        done = (TextView) findViewById(R.id.done);
        back = (TextView) findViewById(R.id.back);

        fromActivity = getIntent().getStringExtra(Constants.FROM_ACTIVITY);
        prevScreen = getIntent().getStringExtra(Constants.PREV_SCREEN);

        chooseLangRg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
                changesDone = true;
                done.setText("Done");
                switch (i/*checkedId*/){
                    case R.id.enUsRb:
                        selectedLang = LibreAlexaConstants.Languages.ENG_US;
                        break;
                    case R.id.engUkRb:
                        selectedLang = LibreAlexaConstants.Languages.ENG_GB;
                        break;
                   /* case R.id.engINRb:
                        selectedLang = AlexaConstants.Languages.ENG_IN;
                        break;*/
                    case R.id.deutschRb:
                        selectedLang = LibreAlexaConstants.Languages.DE;
                        break;
                    case R.id.japanRb:
                        selectedLang = LibreAlexaConstants.Languages.JP;
                        break;
                }
            }
        });

        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (done.getText().equals("Skip")){
                    Intent newIntent = new Intent(AlexaLangUpdateActivity.this, CTAlexaThingsToTryActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    newIntent.putExtra(Constants.CURRENT_DEVICE_IP, currentDeviceIp);
                    newIntent.putExtra(Constants.FROM_ACTIVITY, fromActivity);
                    startActivity(newIntent);
                    finish();
                    return;
                }

                if (changesDone && !selectedLang.isEmpty()){
                    sendUpdatedLangToDevice();
                }
                if (fromActivity!=null && !fromActivity.isEmpty()){
                    Intent newIntent = new Intent(AlexaLangUpdateActivity.this, CTAlexaThingsToTryActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    newIntent.putExtra(Constants.CURRENT_DEVICE_IP, currentDeviceIp);
                    newIntent.putExtra(Constants.FROM_ACTIVITY, fromActivity);
                    startActivity(newIntent);
                    finish();
                }
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (fromActivity != null && !fromActivity.isEmpty()) {
                    if (fromActivity.equals(SourcesOptionActivity.class.getSimpleName())
                            || fromActivity.equals(AlexaSignInActivity.class.getSimpleName())
                            || fromActivity.equals(CTAlexaThingsToTryActivity.class.getSimpleName())) {
                        Intent intent = new Intent(AlexaLangUpdateActivity.this, SourcesOptionActivity.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(Constants.CURRENT_DEVICE_IP, currentDeviceIp);
                        startActivity(intent);
                        finish();
                    } else if (fromActivity.equals(ConnectingToMainNetwork.class.getSimpleName())) {
                        Intent ssid = new Intent(AlexaLangUpdateActivity.this, ActiveScenesListActivity.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(ssid);
                        finish();
                    } else if (fromActivity.equals(LSSDPDeviceNetworkSettings.class.getSimpleName())) {
                        Intent intent = new Intent(AlexaLangUpdateActivity.this, LSSDPDeviceNetworkSettings.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(Constants.CURRENT_DEVICE_IP, currentDeviceIp);
                        startActivity(intent);
                        finish();
                    }
                }
            }
        });

        if (getIntent()!=null
                && getIntent().getStringExtra(Constants.CURRENT_DEVICE_IP)!=null
                && !getIntent().getStringExtra(Constants.CURRENT_DEVICE_IP).isEmpty()){
            currentDeviceIp = getIntent().getStringExtra(Constants.CURRENT_DEVICE_IP);
        }

        luciControl = new LUCIControl(currentDeviceIp);
        if (prevScreen.equalsIgnoreCase(AlexaSignInActivity.class.getSimpleName())){
            done.setText("Done");
            back.setVisibility(View.VISIBLE);
        } else {
            back.setVisibility(View.GONE);
        }
    }

    private void sendUpdatedLangToDevice() {
        luciControl.SendCommand(MIDCONST.ALEXA_COMMAND, LUCIMESSAGES.UPDATE_LOCALE+ selectedLang, LSSDPCONST.LUCI_SET);
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerForDeviceEvents(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        luciControl.SendCommand(MIDCONST.ALEXA_COMMAND, "DEVICE_METADATA_REQUEST", LSSDPCONST.LUCI_SET);
    }

    @Override
    public void deviceDiscoveryAfterClearingTheCacheStarted() {

    }

    @Override
    public void newDeviceFound(LSSDPNodes node) {

    }

    @Override
    public void deviceGotRemoved(String ipaddress) {

    }



    @Override
    public void messageRecieved(NettyData dataRecived) {
        String ipaddressRecieved = dataRecived.getRemotedeviceIp();

        LUCIPacket packet = new LUCIPacket(dataRecived.getMessage());
        LibreLogger.d(this, "Message recieved for ipaddress " + ipaddressRecieved + "command is " + packet.getCommand());

        if (currentDeviceIp.equalsIgnoreCase(ipaddressRecieved)) {
            switch (packet.getCommand()) {

                case MIDCONST.ALEXA_COMMAND: {
                    closeLoader();
                    String alexaMessage = new String(packet.getpayload());
                    LibreLogger.d(this, "Alexa Value From 230  " + alexaMessage);
                    try {
                        JSONObject jsonRootObject = new JSONObject(alexaMessage);
                        JSONObject jsonObject = jsonRootObject.getJSONObject(LUCIMESSAGES.ALEXA_KEY_WINDOW_CONTENT);
                        String locale = jsonObject.optString("LOCALE").toString();
                        updateLang(locale);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                }
                break;
            }
        }
    }

    private void updateLang(String locale) {
        switch (locale){
            case LibreAlexaConstants.Languages.ENG_US:
                chooseLangRg.check(R.id.enUsRb);
                break;
            case LibreAlexaConstants.Languages.ENG_GB:
                chooseLangRg.check(R.id.engUkRb);
                break;
            /*case AlexaConstants.Languages.ENG_IN:
                chooseLangRg.check(R.id.engINRb);
                break;*/
            case LibreAlexaConstants.Languages.DE:
                chooseLangRg.check(R.id.deutschRb);
                break;
            case LibreAlexaConstants.Languages.JP:
                chooseLangRg.check(R.id.japanRb);
                break;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unRegisterForDeviceEvents();
    }
}
