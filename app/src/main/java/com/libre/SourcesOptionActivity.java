package com.libre;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cumulations.libreV2.AppConstants;
import com.cumulations.libreV2.activity.CTDMSBrowserActivityV2;
import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.cumulations.libreV2.activity.CTNowPlayingActivity;
import com.kyleduo.switchbutton.SwitchButton;
import com.libre.LErrorHandeling.LibreError;
import com.libre.Scanning.Constants;
import com.libre.Scanning.ScanningHandler;
import com.libre.StaticInstructions.spotifyInstructions;
import com.libre.alexa.DeviceProvisioningInfo;
import com.libre.alexa_signin.AlexaSignInActivity;
import com.libre.alexa_signin.AlexaThingsToTryDoneActivity;
import com.libre.app.dlna.dmc.processor.upnp.LoadLocalContentService;
import com.libre.app.dlna.dmc.utility.PlaybackHelper;
import com.libre.app.dlna.dmc.utility.UpnpDeviceManager;
import com.libre.constants.LSSDPCONST;
import com.libre.constants.LUCIMESSAGES;
import com.libre.constants.MIDCONST;
import com.libre.luci.LSSDPNodeDB;
import com.libre.luci.LSSDPNodes;
import com.libre.luci.LUCIControl;
import com.libre.luci.LUCIPacket;
import com.libre.netty.LibreDeviceInteractionListner;
import com.libre.netty.NettyData;
import com.libre.util.LibreLogger;
import com.libre.util.Sources;

import org.fourthline.cling.model.meta.RemoteDevice;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SourcesOptionActivity extends CTDeviceDiscoveryActivity implements LibreDeviceInteractionListner {
    private ArrayList<String> listDataHeader;
    private ListView mListView;
    private static final String TAG_CMD_ID = "CMD ID";
    private static final String TAG_WINDOW_CONTENT = "Window CONTENTS";
    private static final String TAG_BROWSER = "Browser";
    public static final String GET_HOME = "GETUI:HOME";
    public static final String GET_BROWSE = "GETUI:BROWSER";
    public static final String GET_DEEZER_USERNAME = "DeezerUserName";
    public static final String BLUETOOTH_OFF = "OFF";
    public static final String BLUETOOTH_ON = "ON";
    private boolean doneClicked;
    final int NETWORK_TIMEOUT = 01;
    final int AUX_BT_TIMEOUT = 0x2;
    final int ALEXA_REFRESH_TOKEN_TIMER = 0x12;
    private final int ACTION_INITIATED = 12345;
    private final int BT_AUX_INITIATED = 12345;
    private final int CREDENTIAL_SUCCESSFULL = 30;
    private final int CREDENTIAL_USER_NAME = 31;
    private final int CREDENTIAL_PASSWORD = 32;
    private final int CREDENTIAL_TIMEOUT = 330;
    private final int TIMEOUT = 3000;

    private String userName = "";
    private String userPassword = "";
    ListViewAdapter adapter;


    ScanningHandler mScanHandler = ScanningHandler.getInstance();


    private static String deezer = "Deezer";
    private static String tidal = "TIDAL";
    //    private static String favourite = "Favourites";
    private static String vtuner = "vTuner";
    private static String spotify = "Spotify";
    private static String tuneIn = "TuneIn";
    private static String qmusic = "QQ Music";

    private String current_ipaddress;
    private String current_source;
    private ProgressDialog m_progressDlg;
    private int current_source_index_selected = -1;
    private SwitchButton bluetooth, auxbutton;

    View headerlayout;
    View footerlayout;
    final static List<String> mediaserverList = new ArrayList<String>();

    /*Decalartion in Globally */
    TextView doneBut;
    LinearLayout localId;
    LinearLayout networkid;
    LinearLayout usbid;
    LinearLayout sdcard;
    LinearLayout favouritesButton;
    LinearLayout auxLayout;
    LinearLayout btLayout;
    LinearLayout alexaLayout;

    String productId = "", dsn = "", sessionId = "", codeChallenge = "", codeChallengeMethod = "";
    private String locale = "";
    private DeviceProvisioningInfo mDeviceProvisioningInfo;

    static {
        mediaserverList.add(spotify);
        mediaserverList.add(deezer);
        mediaserverList.add(tidal);
        mediaserverList.add(vtuner);
        mediaserverList.add(tuneIn);
        mediaserverList.add(qmusic);
    }

    public String getconnectedSSIDname() {
        WifiManager wifiManager;
        wifiManager = (WifiManager) SourcesOptionActivity.this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ssid = wifiInfo.getSSID();
        Log.d("CreateNewScene", "getConnectedSSIDName wifiInfo = " + wifiInfo.toString());
        if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
            ssid = ssid.substring(1, ssid.length() - 1);
        }
        Log.d("SacListActivity", "Connected SSID" + ssid);
        return ssid;
    }


    Handler credentialHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            if (msg.what == NETWORK_TIMEOUT) {
                LibreLogger.d(this, "recieved handler msg");
                closeLoader();

                /*showing error to user*/
                LibreError error = new LibreError(current_ipaddress, Constants.INTERNET_ITEM_SELECTED_TIMEOUT_MESSAGE);
                showErrorMessage(error);
//                Toast.makeText(getApplicationContext(), Constants.LOADING_TIMEOUT_REBOOT, Toast.LENGTH_SHORT).show();
            }

            if (msg.what == ACTION_INITIATED) {
                showLoader();
            }


            if (msg.what == CREDENTIAL_USER_NAME) {
                userName = (String) msg.obj;
            }

            if (msg.what == CREDENTIAL_PASSWORD) {
                userPassword = (String) msg.obj;
            }

            if (msg.what == CREDENTIAL_TIMEOUT) {


                if (!(SourcesOptionActivity.this.isFinishing())) {
                    try {
                        if ((userName == null || userName.isEmpty()) && (userPassword == null || userPassword.isEmpty())) {
                            closeLoader();
                            sourceAuthDialogue(current_source_index_selected);
                        } else {
                            LUCIControl luciControl = new LUCIControl(current_ipaddress);
                            if (current_source_index_selected == 5) {
                                /*deezer*/
                                luciControl.SendCommand(MIDCONST.MID_DEEZER, LUCIMESSAGES.DEEZER_USERNAME + userName,
                                        LSSDPCONST.LUCI_SET);
                                try {
                                    Thread.sleep(100);
                                } catch (Exception e) {
                                }
                                luciControl.SendCommand(MIDCONST.MID_DEEZER, LUCIMESSAGES.DEEZER_PASSWORD + userPassword,
                                        LSSDPCONST.LUCI_SET);
                                try {
                                    Thread.sleep(100);
                                } catch (Exception e) {
                                }
                                luciControl.SendCommand(MIDCONST.MID_REMOTE_UI, GET_HOME, LSSDPCONST.LUCI_SET);
                            } else if (current_source_index_selected == 6) {
                                /*tidal*/
                                luciControl.SendCommand(MIDCONST.MID_DEEZER, LUCIMESSAGES.TIDAL_USERNAME + userName,
                                        LSSDPCONST.LUCI_SET);
                                try {
                                    Thread.sleep(100);
                                } catch (Exception e) {
                                }
                                luciControl.SendCommand(MIDCONST.MID_DEEZER, LUCIMESSAGES.TIDAL_PASSWORD + userPassword,
                                        LSSDPCONST.LUCI_SET);
                                try {
                                    Thread.sleep(100);
                                } catch (Exception e) {
                                }
                                luciControl.SendCommand(MIDCONST.MID_REMOTE_UI, GET_HOME, LSSDPCONST.LUCI_SET);

                            }

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            if (msg.what == BT_AUX_INITIATED) {
                String showMessage = msg.getData().getString("MessageText");
                showLoaderAndAskSource(showMessage);
            }
            if (msg.what == AUX_BT_TIMEOUT) {
                closeLoader();
            }
            if (msg.what == ALEXA_REFRESH_TOKEN_TIMER) {
                closeLoader();
                showSomethingWentWrongAlert(SourcesOptionActivity.this);
            }
        }
    };

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        doneClicked=true;
        if (getIntent().hasExtra(Constants.FROM_ACTIVITY)) {
            Intent intent = new Intent(SourcesOptionActivity.this, CTNowPlayingActivity.class);
            intent.putExtra(Constants.CURRENT_DEVICE_IP, current_ipaddress);
            startActivity(intent);
            finish();
        } else {
            Intent intent = new Intent(SourcesOptionActivity.this, ActiveScenesListActivity.class);
            startActivity(intent);
            finish();
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_sources_option);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        doneClicked= false;
        mListView = (ListView) findViewById(R.id.media_list);
        headerlayout = getLayoutInflater().inflate(R.layout.sources_header, mListView, false);
        footerlayout = getLayoutInflater().inflate(R.layout.sources_footer, mListView, false);
        mListView.addHeaderView(headerlayout, null, false);
        mListView.addFooterView(footerlayout, null, false);

        registerForDeviceEvents(this);

        current_ipaddress = getIntent().getStringExtra(Constants.CURRENT_DEVICE_IP);
        current_source = getIntent().getStringExtra("current_source");
        LSSDPNodeDB lssdpNodeDB1 = LSSDPNodeDB.getInstance();
        LSSDPNodes theNodeBasedOnTheIpAddress1 = lssdpNodeDB1.getTheNodeBasedOnTheIpAddress(current_ipaddress);
        if (getconnectedSSIDname().contains(Constants.DDMS_SSID) || (theNodeBasedOnTheIpAddress1 != null &&
                theNodeBasedOnTheIpAddress1.getNetworkMode().contains("P2P") == true)) {
            final List<String> mEmptyMediaServerList = new ArrayList<String>();
            final LinearLayout musicServicesButton = (LinearLayout) headerlayout.findViewById(R.id.musicServices);
            musicServicesButton.setVisibility(View.GONE);
            adapter = new ListViewAdapter(this, mEmptyMediaServerList);
            mListView.setAdapter(adapter);


        } else {

            adapter = new ListViewAdapter(this, mediaserverList);
            mListView.setAdapter(adapter);

            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                //   @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                    if (getconnectedSSIDname().contains(Constants.DDMS_SSID)) {

                        //  Toast.makeText(getApplicationContext(), "No Internet Connection ,in SA Mode", Toast.LENGTH_SHORT).show();
                        LibreError error = new LibreError(current_ipaddress, Constants.NO_INTERNET_CONNECTION);
                        showErrorMessage(error);
                        return;
                    }
                    String s = mediaserverList.get(i - 1);

                    if (s.equalsIgnoreCase(spotify)) {
                        if (SourcesOptionActivity.this.isFinishing())
                            return;

                        Intent spotifyIntent = new Intent(SourcesOptionActivity.this, spotifyInstructions.class);
                        spotifyIntent.putExtra(Constants.CURRENT_DEVICE_IP, current_ipaddress);
                        spotifyIntent.putExtra("current_source", "" + current_source);
                        startActivity(spotifyIntent);
                 /*   Dialog customDialog = new Dialog(SourcesOptionActivity.this);
                    customDialog.setContentView(R.layout.spotify_dialog);
                    customDialog.setTitle("Spotify App");
                    customDialog.setCancelable(true);
                    customDialog.getWindow().setLayout(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
                    customDialog.show();
                    Button SpotifyBtn = (Button) customDialog.findViewById(R.id.SpotifyBtn);
                    SpotifyBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String appPackageName = "com.spotify.music";
                            launchTheApp(appPackageName);
                        }
                    });

                    Button nowPlayingBtn = (Button) customDialog.findViewById(R.id.nowplayingbutton);
                    nowPlayingBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            *//*navigating user to NowPlaying Screen*//*
                            Intent intent = new Intent(SourcesOptionActivity.this, NowPlayingActivity.class);
                            intent.putExtra(Constants.CURRENT_DEVICE_IP,current_ipaddress);
                            startActivity(intent);
                            finish();
                        }
                    });


                    TextView tv = (TextView) customDialog.findViewById(R.id.learnMore);
                    tv.setText(Html.fromHtml("<a href= https://spotify.com/connect > Learn More </a>"));
                    tv.setMovementMethod(LinkMovementMethod.getInstance());*/


                    } else if (s.equalsIgnoreCase(qmusic)) {

                        launchTheApp("com.tencent.qqmusic");

                    } else if (s.equalsIgnoreCase(deezer)) {
                        showLoader();
                        credentialHandler.sendEmptyMessageDelayed(CREDENTIAL_TIMEOUT, TIMEOUT);

                        if (!getconnectedSSIDname().contains(Constants.DDMS_SSID)) {

//                        Toast.makeText(getApplicationContext(), "No Internet Connection ,in SA Mode", Toast.LENGTH_SHORT).show();
                            LUCIControl luciControl = new LUCIControl(current_ipaddress);
                            luciControl.sendAsynchronousCommand();
                            luciControl.SendCommand(208, "READ_DeezerUserName", LSSDPCONST.LUCI_SET);
                            try {
                                Thread.sleep(100);
                            } catch (Exception e) {

                            }
                            luciControl.SendCommand(208, "READ_DeezerUserPassword", LSSDPCONST.LUCI_SET);
                        }

                        current_source_index_selected = 5;

                    } else if (s.equalsIgnoreCase(tidal)) {
                        showLoader();
                        credentialHandler.sendEmptyMessageDelayed(CREDENTIAL_TIMEOUT, TIMEOUT);


                        if (!getconnectedSSIDname().contains(Constants.DDMS_SSID)) {
                            LUCIControl luciControl = new LUCIControl(current_ipaddress);
                            luciControl.sendAsynchronousCommand();
                            luciControl.SendCommand(208, "READ_TidalUserName", LSSDPCONST.LUCI_SET);
                            try {
                                Thread.sleep(100);
                            } catch (Exception e) {

                            }
                            luciControl.SendCommand(208, "READ_TidalUserPassword", LSSDPCONST.LUCI_SET);
                        }


                        current_source_index_selected = 6;

                    }
//                else if (s.equalsIgnoreCase(favourite)) {
//
//                    current_source_index_selected = 7;
//                     /*Reset the UI to Home ,, will wait for the confirmation of home command completion and then start the required activity*/
//                    LUCIControl luciControl = new LUCIControl(current_ipaddress);
//                    luciControl.sendAsynchronousCommand();
//                    luciControl.SendCommand(MIDCONST.MID_REMOTE_UI, GET_HOME, LSSDPCONST.LUCI_SET);
//                    showLoader();
//
//                }
                    else if (s.equalsIgnoreCase(vtuner)) {

                        current_source_index_selected = 1;
                /*Reset the UI to Home ,, will wait for the confirmation of home command completion and then start the required activity*/
                        LUCIControl luciControl = new LUCIControl(current_ipaddress);
                        luciControl.sendAsynchronousCommand();
                        luciControl.SendCommand(MIDCONST.MID_REMOTE_UI, GET_HOME, LSSDPCONST.LUCI_SET);

                        ///////////// timeout for dialog - showLoader() ///////////////////
                        credentialHandler.sendEmptyMessageDelayed(NETWORK_TIMEOUT, Constants.ITEM_CLICKED_TIMEOUT);

                        showLoader();


                    } else if (s.equalsIgnoreCase(tuneIn)) {

                        current_source_index_selected = 2;
                /*Reset the UI to Home ,, will wait for the confirmation of home command completion and then start the required activity*/
                        LUCIControl luciControl = new LUCIControl(current_ipaddress);
                        luciControl.sendAsynchronousCommand();
                        luciControl.SendCommand(MIDCONST.MID_REMOTE_UI, GET_HOME, LSSDPCONST.LUCI_SET);

                        ///////////// timeout for dialog - showLoader() ///////////////////
                        credentialHandler.sendEmptyMessageDelayed(NETWORK_TIMEOUT, Constants.ITEM_CLICKED_TIMEOUT);
                        showLoader();


                    }
                }
            });
        }
        LSSDPNodeDB lssdpNodeDB = LSSDPNodeDB.getInstance();
        LSSDPNodes theNodeBasedOnTheIpAddress = lssdpNodeDB.getTheNodeBasedOnTheIpAddress(current_ipaddress);

        localId = (LinearLayout) headerlayout.findViewById(R.id.local);
        localId.setVisibility(View.VISIBLE);

        localId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(SourcesOptionActivity.this, getString(R.string.locationEnable), Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                showLoader();
                Intent localIntent = new Intent(SourcesOptionActivity.this, CTDMSBrowserActivityV2.class);
                localIntent.putExtra(AppConstants.IS_LOCAL_DEVICE_SELECTED, true);
                localIntent.putExtra(Constants.CURRENT_DEVICE_IP, current_ipaddress);
                startActivity(localIntent);
                finish();

            }
        });
        /*selecting favourites click*/
        favouritesButton = (LinearLayout) headerlayout.findViewById(R.id.favourite_button);

        if (theNodeBasedOnTheIpAddress != null && theNodeBasedOnTheIpAddress.getgCastVerision() != null) {
            favouritesButton.setVisibility(View.GONE);
            LibreLogger.d(this, "Favourite should not be displayed for LS9 device");
        } else {
            favouritesButton.setVisibility(View.VISIBLE);
        }

        if (getconnectedSSIDname().contains(Constants.DDMS_SSID)) {
            favouritesButton.setVisibility(View.GONE);
        }

        favouritesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //take from shared pref
                // always show messsage checked
                final SharedPreferences sharedpreferences = getSharedPreferences("fav_dialog", Context.MODE_PRIVATE);
                Boolean showFavDialog = sharedpreferences.getBoolean("neverAskFav", false);
                LibreLogger.d(this, "Always show message in shared pref is " + showFavDialog);

                if (showFavDialog != null && !showFavDialog) {
                    final Dialog dialog = new Dialog(SourcesOptionActivity.this);
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    dialog.setContentView(R.layout.favourite_dialog);
                    dialog.getWindow().setLayout(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
                    Button showFavs = (Button) dialog.findViewById(R.id.ok);
                    final CheckBox neverAsk = (CheckBox) dialog.findViewById(R.id.neverAsk);
                    neverAsk.setChecked(false);
                    showFavs.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (neverAsk.isChecked()) {
                                //put to shared pref - user wants to show it once
                                SharedPreferences.Editor editor = sharedpreferences.edit();
                                editor.putBoolean("neverAskFav", true);
                                editor.commit();
                                dialog.cancel();
                                showFavContents();
                            } else {
                                //put to shared pref - user wants to show it again
                                SharedPreferences.Editor editor = sharedpreferences.edit();
                                editor.putBoolean("neverAskFav", false);
                                editor.commit();
                                dialog.cancel();
                                showFavContents();
                            }
                        }
                    });


                    dialog.show();
                } else {
                    // always show messsage unchecked
                    showFavContents();
                }
            }
        });


        TextView gCastTuneinView = (TextView) headerlayout.findViewById(R.id.gcast_tunein);
        gCastTuneinView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (googleTosCheckIsSuccess())
                    launchTheApp("tunein.player");
            }
        });
        TextView gCastDeezerinView = (TextView) headerlayout.findViewById(R.id.gcast_deezer);
        gCastDeezerinView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (googleTosCheckIsSuccess())
                    launchTheApp("deezer.android.app");
            }
        });

        TextView gCastGooglePlay = (TextView) headerlayout.findViewById(R.id.gcast_google_play);
        gCastGooglePlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (googleTosCheckIsSuccess())
                    launchTheApp("com.google.android.music");
            }
        });


        TextView gCastPandoraView = (TextView) headerlayout.findViewById(R.id.gcast_pandora);
        gCastPandoraView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (googleTosCheckIsSuccess())
                    launchTheApp("com.pandora.android");

            }
        });

        TextView gcastSpotify = (TextView) headerlayout.findViewById(R.id.gcast_spotify);
        gcastSpotify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (googleTosCheckIsSuccess())
                    launchTheApp("com.spotify.music");

            }
        });


        TextView gmoreApps = (TextView) headerlayout.findViewById(R.id.moreapps);
        SpannableString content = new SpannableString("Google Cast-enabled apps");
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        gmoreApps.setText(content);

        gmoreApps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (googleTosCheckIsSuccess()) {

                    Intent intent = new Intent(SourcesOptionActivity.this, LibreWebViewActivity.class);
                    intent.putExtra("url", "http://g.co/cast/audioapps");
                    intent.putExtra("title", "Sources");
                    startActivity(intent);

                }
            }
        });


        if (theNodeBasedOnTheIpAddress != null && theNodeBasedOnTheIpAddress.getgCastVerision() != null
                && theNodeBasedOnTheIpAddress1.getNetworkMode().contains("P2P") == false) {
            final LinearLayout castServicesButton = (LinearLayout) headerlayout.findViewById(R.id.chromecast_services);
            castServicesButton.setVisibility(View.VISIBLE);

            try {
                mediaserverList.clear();
                mediaserverList.add(spotify);
                /*mediaserverList.add(tidal);
                mediaserverList.add(vtuner);
                mediaserverList.add(qmusic);*/

                adapter.notifyDataSetChanged();
            } catch (Exception e) {
                e.printStackTrace();
            }


        } else {
            final LinearLayout castServicesButton = (LinearLayout) headerlayout.findViewById(R.id.chromecast_services);
            castServicesButton.setVisibility(View.GONE);
            try {
                mediaserverList.clear();
                mediaserverList.add(spotify);
                mediaserverList.add(deezer);
                mediaserverList.add(tidal);
                mediaserverList.add(vtuner);
                mediaserverList.add(tuneIn);
                mediaserverList.add(qmusic);
                adapter.notifyDataSetChanged();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        networkid = (LinearLayout) footerlayout.findViewById(R.id.network);
        if (theNodeBasedOnTheIpAddress != null && theNodeBasedOnTheIpAddress.getgCastVerision() != null) {
            networkid.setVisibility(View.GONE);
            LibreLogger.d(this, "DMP should not be displayed for LS9 device");
        } else {
            networkid.setVisibility(View.VISIBLE);
        }

        /* This change is done to make sure that the network and media Network Devices/Player for SA mode
        *
        * issue nnumber 517 */
        if (theNodeBasedOnTheIpAddress != null && theNodeBasedOnTheIpAddress.getNetworkMode().contains("P2P") == true) {
            networkid.setVisibility(View.GONE);

        } else {
            networkid.setVisibility(View.VISIBLE);

        }


        networkid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


//                showLoader();
//                Intent localIntent = new Intent(SourcesOptionActivity.this, DMSDeviceListActivity.class);
//                localIntent.putExtra(AppConstants.IS_LOCAL_DEVICE_SELECTED, false);
//                localIntent.putExtra(Constants.CURRENT_DEVICE_IP, current_ipaddress);
//                startActivity(localIntent);


                  /*OPTSpec*/
                /*Changing Music server DMR to DMP*/

                current_source_index_selected = 0;
                /*Reset the UI to Home ,, will wait for the confirmation of home command completion and then start the required activity*/
                LUCIControl luciControl = new LUCIControl(current_ipaddress);
                luciControl.SendCommand(MIDCONST.MID_REMOTE_UI, GET_HOME, LSSDPCONST.LUCI_SET);
                ///////////// timeout for dialog - showLoader() ///////////////////
                credentialHandler.sendEmptyMessageDelayed(NETWORK_TIMEOUT, Constants.ITEM_CLICKED_TIMEOUT);
                LibreLogger.d(this, "sending handler msg");
                showLoader();


            }
        });

        usbid = (LinearLayout) footerlayout.findViewById(R.id.usb);
        usbid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                current_source_index_selected = 3;
                /*Reset the UI to Home ,, will wait for the confirmation of home command completion and then start the required activity*/
                LUCIControl luciControl = new LUCIControl(current_ipaddress);
                luciControl.sendAsynchronousCommand();
                luciControl.SendCommand(MIDCONST.MID_REMOTE_UI, GET_HOME, LSSDPCONST.LUCI_SET);
                ///////////// timeout for dialog - showLoader() ///////////////////
                credentialHandler.sendEmptyMessageDelayed(NETWORK_TIMEOUT, Constants.ITEM_CLICKED_TIMEOUT);

                showLoader();
            }
        });


        sdcard = (LinearLayout) footerlayout.findViewById(R.id.sdcard);
        if (theNodeBasedOnTheIpAddress != null && theNodeBasedOnTheIpAddress.getgCastVerision() != null) {
            sdcard.setVisibility(View.GONE);
            LibreLogger.d(this, "SDCard should not be displayed for LS9 device");
        } else
            sdcard.setVisibility(View.VISIBLE);
        {
            sdcard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    current_source_index_selected = 4;
                /*Reset the UI to Home ,, will wait for the confirmation of home command completion and then start the required activity*/
                    LUCIControl luciControl = new LUCIControl(current_ipaddress);
                    luciControl.sendAsynchronousCommand();
                    luciControl.SendCommand(MIDCONST.MID_REMOTE_UI, GET_HOME, LSSDPCONST.LUCI_SET);

                    ///////////// timeout for dialog - showLoader() ///////////////////
                    credentialHandler.sendEmptyMessageDelayed(NETWORK_TIMEOUT, Constants.ITEM_CLICKED_TIMEOUT);
                    showLoader();
                }
            });
        }

        btLayout = (LinearLayout) footerlayout.findViewById(R.id.bluetoothSourceLayout);
        bluetooth = (SwitchButton) footerlayout.findViewById(R.id.bluetooth_switch);


        bluetooth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                final LUCIControl luciControl = new LUCIControl(current_ipaddress);
                luciControl.sendAsynchronousCommand();
                credentialHandler.sendEmptyMessageDelayed(AUX_BT_TIMEOUT, 5000);
                if (isChecked) {
//                    showLoaderAndAskSource(getString(R.string.BtOnAlert));
                    Message msg = new Message();
                    msg.what = BT_AUX_INITIATED;
                    Bundle b = new Bundle();
                    b.putString("MessageText", getString(R.string.BtOnAlert));
                    msg.setData(b);
                    credentialHandler.sendMessage(msg);

                    if (auxbutton.isChecked()) {
                        luciControl.SendCommand(MIDCONST.MID_AUX_STOP, null, LSSDPCONST.LUCI_SET);
                        auxbutton.setChecked(false, false);
                    }
                    // sleep when BT to AUX switch
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            luciControl.SendCommand(MIDCONST.MID_BLUETOOTH, BLUETOOTH_ON, LSSDPCONST.LUCI_SET);
                        }
                    }, 1000);
                    /*try {
                    /*try {
                        mRemoveRenderingWhenAuxOrBTisOn(current_ipaddress);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }*/
//                    luciControl.SendCommand(MIDCONST.MID_BLUETOOTH, BLUETOOTH_ON, LSSDPCONST.LUCI_SET);
                    /* Setting the source to 19 */
                /*    SceneObject sceneObject = mScanHandler.getSceneObjectFromCentralRepo().get(current_ipaddress);
                    sceneObject.setCurrentSource(19);
                    mScanHandler.getSceneObjectFromCentralRepo().put(current_ipaddress, sceneObject);*/

                } else {
                       /* Setting the source to default */
//                    showLoaderAndAskSource(getString(R.string.BtOffAlert));

                    Message msg = new Message();
                    msg.what = BT_AUX_INITIATED;
                    Bundle b = new Bundle();
                    b.putString("MessageText", getString(R.string.BtOffAlert));
                    msg.setData(b);
                    credentialHandler.sendMessage(msg);


                    luciControl.SendCommand(MIDCONST.MID_BLUETOOTH, BLUETOOTH_OFF, LSSDPCONST.LUCI_SET);
                  /*  SceneObject sceneObject = mScanHandler.getSceneObjectFromCentralRepo().get(current_ipaddress);
                    sceneObject.setCurrentSource(-1);*/
                }
/*
                if (isChecked)
                    luciContkrol.SendCommand(MIDCONST.MID_BLUETOOTH, BLUETOOTH_ON, LSSDPCONST.LUCI_SET);
                else
                    luciControl.SendCommand(MIDCONST.MID_BLUETOOTH, BLUETOOTH_OFF, LSSDPCONST.LUCI_SET);*/
            }
        });

        auxLayout = (LinearLayout) footerlayout.findViewById(R.id.auxSourceLayout);
        auxbutton = (SwitchButton) footerlayout.findViewById(R.id.aux_in);

        auxbutton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                final LUCIControl luciControl = new LUCIControl(current_ipaddress);
                luciControl.sendAsynchronousCommand();
                credentialHandler.sendEmptyMessage(ACTION_INITIATED);
                credentialHandler.sendEmptyMessageDelayed(AUX_BT_TIMEOUT, 5000);
                if (isChecked) {
//                    showLoaderAndAskSource(getString(R.string.AuxOnAlert));

                    Message msg = new Message();
                    msg.what = BT_AUX_INITIATED;
                    Bundle b = new Bundle();
                    b.putString("MessageText", getString(R.string.AuxOnAlert));
                    msg.setData(b);
                    credentialHandler.sendMessage(msg);
                    if (bluetooth.isChecked()) {
                        luciControl.SendCommand(MIDCONST.MID_BLUETOOTH, BLUETOOTH_OFF, LSSDPCONST.LUCI_SET);
                        bluetooth.setChecked(false, false);

                    }
                    /*try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }*/

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            luciControl.SendCommand(MIDCONST.MID_AUX_START, null, LSSDPCONST.LUCI_SET);
                        }
                    }, 1000);

                    /*try {
                        mRemoveRenderingWhenAuxOrBTisOn(current_ipaddress);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }*/
//                    luciControl.SendCommand(MIDCONST.MID_AUX_START, null, LSSDPCONST.LUCI_SET);
                    /* Setting the source to 14 */


                } else {
//                    showLoaderAndAskSource(getString(R.string.AuxOffAlert));

                    Message msg = new Message();
                    msg.what = BT_AUX_INITIATED;
                    Bundle b = new Bundle();
                    b.putString("MessageText", getString(R.string.AuxOffAlert));
                    msg.setData(b);
                    credentialHandler.sendMessage(msg);

                       /* Setting the source to default */
                    luciControl.SendCommand(MIDCONST.MID_AUX_STOP, null, LSSDPCONST.LUCI_SET);
                 /*   SceneObject sceneObject = mScanHandler.getSceneObjectFromCentralRepo().get(current_ipaddress);
                    sceneObject.setCurrentSource(-1);*/
                }
            }
        });


//        readTheCurrentAuxStatus();
//        readTheCurrentBluetoothStatus();

        if (current_source != null) {
            int currentSource = Integer.valueOf(current_source);
            SceneObject sceneObject = mScanHandler.getSceneObjectFromCentralRepo().get(current_ipaddress);

			/* Karuna , if Zone is playing in BT/AUX and We Released the Zone
            and we creating the same Guy as a Master then Aux should not Switch ON as a Default*/
            if (currentSource == 14 && sceneObject != null && sceneObject.getPlaystatus() == SceneObject.CURRENTLY_STOPPED) {
                //auxbutton.setChecked(true, false);
                Log.d("AUXSTATE", "--" + sceneObject.getPlaystatus());
                auxbutton.setChecked(false, false);
            }
            /* Karuna , if Zone is playing in BT/AUX and We Released the Zone
            and we creating the same Guy as a Master then Aux should not Switch ON as a Default*/

            if (currentSource == 19 && sceneObject != null &&
                    (sceneObject.getPlaystatus() == SceneObject.CURRENTLY_STOPPED
                            || sceneObject.getPlaystatus() == SceneObject.CURRENTLY_NOTPLAYING)) {
                //bluetooth.setChecked(true, false);
                Log.d("BTSTATE", "--" + sceneObject.getPlaystatus());
                bluetooth.setChecked(false, false);
            }
        }

        doneBut = (TextView) findViewById(R.id.doneButton);
        doneBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doneClicked = true;
                doneAction();
            }
        });
        //  DoDisableViewsForMPRelease();
        EnableOrDisableViews();

        final LSSDPNodes mNode = mScanHandler.getLSSDPNodeFromCentralDB(current_ipaddress);
        alexaLayout = (LinearLayout) footerlayout.findViewById(R.id.amazonAlexa);
//        alexaLayout.setVisibility(View.VISIBLE);
        alexaLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestRefreshToken();
            }
        });
    }

    private void requestRefreshToken() {
        credentialHandler.sendEmptyMessageDelayed(ALEXA_REFRESH_TOKEN_TIMER, 15000);
        showLoader();
        String readAlexaRefreshToken = "READ_AlexaRefreshToken";
        new LUCIControl(current_ipaddress).SendCommand(208, readAlexaRefreshToken, 1);
    }


    private void DoDisableViewsForMPRelease() {
        DisableViews();
        localId.setVisibility(View.VISIBLE);
       /* usbid.setVisibility(View.VISIBLE);*/
        btLayout.setVisibility(View.VISIBLE);
        auxLayout.setVisibility(View.VISIBLE);
     /*   LSSDPNodes mNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(current_ipaddress);
        SceneObject mSceneObj = mScanHandler.getSceneObjectFromCentralRepo(current_ipaddress);

        if(mNode!=null &&
                mNode.ismDeviceType()) {
            if(mNode.ismSpotifyEnabled()) {
                final LinearLayout musicServicesButton = (LinearLayout) headerlayout.
                        findViewById(R.id.musicServices);
                musicServicesButton.setVisibility(View.VISIBLE);
                mediaserverList.add(spotify);
            }
            adapter.notifyDataSetChanged();
        }else{
            final LinearLayout musicServicesButton = (LinearLayout) headerlayout.
                    findViewById(R.id.musicServices);
            musicServicesButton.setVisibility(View.VISIBLE);
            mediaserverList.add(spotify);
            adapter.notifyDataSetChanged();
        }*/

    }

    private void DisableViews() {
        localId.setVisibility(View.GONE);
        networkid.setVerticalGravity(View.GONE);
        mediaserverList.clear();
        networkid.setVisibility(View.GONE);
        usbid.setVisibility(View.GONE);
        sdcard.setVisibility(View.GONE);
        btLayout.setVisibility(View.GONE);
        auxLayout.setVisibility(View.GONE);
        favouritesButton.setVisibility(View.GONE);

        final LinearLayout castServicesButton = (LinearLayout) headerlayout.findViewById(R.id.chromecast_services);
        castServicesButton.setVisibility(View.GONE);
        final LinearLayout musicServicesButton = (LinearLayout) headerlayout.
                findViewById(R.id.musicServices);
        musicServicesButton.setVisibility(View.GONE);
    }

    private void EnableOrDisableViews() {
        LSSDPNodes theNodeBasedOnTheIpAddress = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(current_ipaddress);


        if (theNodeBasedOnTheIpAddress == null || theNodeBasedOnTheIpAddress.getmDeviceCap() == null || theNodeBasedOnTheIpAddress.getmDeviceCap().getmSource() == null) {
            return;
        }
        DisableViews();
      /*  String DEFAULT_HEXVALUE = "00000000";

        *//*if Value coming as 0000000*//*
        if(theNodeBasedOnTheIpAddress.getmDeviceCap().getmHexValue().contains(DEFAULT_HEXVALUE)){
            *//*localId.setVisibility(View.VISIBLE);*//*
            LibreError error = new LibreError(current_ipaddress, getResources().getString(R.string.appsourcelistnotSet));
            showErrorMessage(error);
            showAlertDialog();
            return;
        }*/
        Sources mSourceForThisDevice = theNodeBasedOnTheIpAddress.getmDeviceCap().getmSource();
        boolean isAnyoneEnabled = false;
        {

            if (mSourceForThisDevice != null) {
                if (mSourceForThisDevice.isDmr()) {
                    isAnyoneEnabled = true;
                    localId.setVisibility(View.VISIBLE);
                } else {
                    localId.setVisibility(View.GONE);
                }

                if (mSourceForThisDevice.isDmp()) {
                    isAnyoneEnabled = true;
                    networkid.setVisibility(View.VISIBLE);
                } else {
                    networkid.setVisibility(View.GONE);
                }

                mediaserverList.clear();


                if (mSourceForThisDevice.isUsb()) {
                    usbid.setVisibility(View.VISIBLE);
                } else {
                    usbid.setVisibility(View.GONE);
                }

                if (mSourceForThisDevice.isSDcard()) {
                    isAnyoneEnabled = true;
                    sdcard.setVisibility(View.VISIBLE);
                } else {
                    sdcard.setVisibility(View.GONE);
                }
                if (!theNodeBasedOnTheIpAddress.getNetworkMode().contains("P2P")) {
                    if (mSourceForThisDevice.isvTuner()) {
                        mediaserverList.add(vtuner);
                    }

                    if (mSourceForThisDevice.isTuneIn()) {
                        mediaserverList.add(tuneIn);
                    }

                    if (mSourceForThisDevice.isSpotify()) {
                        mediaserverList.add(spotify);
                    }

                    if (mSourceForThisDevice.isDeezer()) {
                        mediaserverList.add(deezer);
                    }

                    if (mSourceForThisDevice.isTidal()) {
                        mediaserverList.add(tidal);
                    }

                    if (mSourceForThisDevice.isFavourites()) {
                        isAnyoneEnabled = true;
                        favouritesButton.setVisibility(View.VISIBLE);
                    } else {
                        favouritesButton.setVisibility(View.GONE);
                    }


                }

                if (mSourceForThisDevice.isAuxIn()) {
                    isAnyoneEnabled = true;
                    auxLayout.setVisibility(View.VISIBLE);
                } else {
                    auxLayout.setVisibility(View.GONE);
                }

                if (mSourceForThisDevice.isBluetooth()) {
                    isAnyoneEnabled = true;
                    btLayout.setVisibility(View.VISIBLE);
                } else {
                    btLayout.setVisibility(View.GONE);
                }


                if (theNodeBasedOnTheIpAddress.getmDeviceCap().
                        getmDeviceCap().equalsIgnoreCase(Constants.LS9)
                        && mSourceForThisDevice.isGoogleCast()
                        && !theNodeBasedOnTheIpAddress.getNetworkMode().contains("P2P")) {
                    final LinearLayout castServicesButton = (LinearLayout) headerlayout.findViewById(R.id.chromecast_services);
                    castServicesButton.setVisibility(View.VISIBLE);
                    sdcard.setVisibility(View.GONE);
                    try {
                        mediaserverList.clear();
                        isAnyoneEnabled = true;
                        if (mSourceForThisDevice.isSpotify())
                            mediaserverList.add(spotify);

                        adapter.notifyDataSetChanged();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    final LinearLayout castServicesButton = (LinearLayout) headerlayout.findViewById(R.id.chromecast_services);
                    castServicesButton.setVisibility(View.GONE);
                }
                if (mediaserverList.size() == 0) {
                    final LinearLayout musicServicesButton = (LinearLayout) headerlayout.
                            findViewById(R.id.musicServices);
                    musicServicesButton.setVisibility(View.GONE);
                } else {
                    final LinearLayout musicServicesButton = (LinearLayout) headerlayout.
                            findViewById(R.id.musicServices);
                    isAnyoneEnabled = true;
                    musicServicesButton.setVisibility(View.VISIBLE);
                }
                if (!isAnyoneEnabled) {
                    showAlertDialog();
                }
                adapter.notifyDataSetChanged();
            }
        }
    }
    AlertDialog appsourcealertDialog;;
    private void showAlertDialog() {

            {

            if (!SourcesOptionActivity.this.isFinishing())
                {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                            SourcesOptionActivity.this);

                    // set title
                    alertDialogBuilder.setTitle("Alert");

                    // set dialog message
                    alertDialogBuilder
                            .setMessage(getResources().getString(R.string.appsourcelistnotSet))
                            .setCancelable(false)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    if (getIntent().hasExtra(Constants.FROM_ACTIVITY)) {
                                        Intent intent = new Intent(SourcesOptionActivity.this, CTNowPlayingActivity.class);
                                        intent.putExtra(Constants.CURRENT_DEVICE_IP, current_ipaddress);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Intent intent = new Intent(SourcesOptionActivity.this, ActiveScenesListActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }
                                    appsourcealertDialog = null;

                                }
                            });

                    // create alertDialog1 dialog
                    if (appsourcealertDialog == null)
                        appsourcealertDialog = alertDialogBuilder.create();
                /*alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);*/
                    // show it

                    appsourcealertDialog.show();
                }
            }

    }
    private void showFavContents() {
        current_source_index_selected = 7;
                     /*Reset the UI to Home ,, will wait for the confirmation of home command completion and then start the required activity*/
        LUCIControl luciControl = new LUCIControl(current_ipaddress);
        luciControl.sendAsynchronousCommand();
        luciControl.SendCommand(MIDCONST.MID_REMOTE_UI, GET_HOME, LSSDPCONST.LUCI_SET);

        ///////////// timeout for dialog - showLoader() ///////////////////
        credentialHandler.sendEmptyMessageDelayed(NETWORK_TIMEOUT, Constants.ITEM_CLICKED_TIMEOUT);

        showLoader();
    }

    private boolean googleTosCheckIsSuccess() {
        SharedPreferences sharedPreferences = getApplicationContext()
                .getSharedPreferences(Constants.SHOWN_GOOGLE_TOS, Context.MODE_PRIVATE);
        String shownGoogle = sharedPreferences.getString(Constants.SHOWN_GOOGLE_TOS, null);
        if (shownGoogle == null) {

            showGoogleTosDialog(this);
            return false;
        }

        return true;
    }

    private void doneAction() {
        if (credentialHandler != null)
            credentialHandler.removeCallbacksAndMessages(null);
        if (getIntent().hasExtra(Constants.FROM_ACTIVITY)) {
            Intent intent = new Intent(SourcesOptionActivity.this, CTNowPlayingActivity.class);
            intent.putExtra(Constants.CURRENT_DEVICE_IP, current_ipaddress);
            startActivity(intent);
            finish();
        } else {
            Intent intent = new Intent(SourcesOptionActivity.this, ActiveScenesListActivity.class);
            startActivity(intent);
            finish();
        }
    }

    public void showGoogleTosDialog(Context context) {

        LayoutInflater factory = LayoutInflater.from(this);
        final View gcastDialog = factory.inflate(
                R.layout.gcast_alert_dialog, null);
        TextView textView = (TextView) gcastDialog.findViewById(R.id.google_text);
        textView.setText(clickableString(getString((R.string.google_tos))));
        textView.setAutoLinkMask(RESULT_OK);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setGravity(Gravity.LEFT);
        Linkify.addLinks(clickableString(getString((R.string.google_tos))), Linkify.WEB_URLS);

        final AlertDialog.Builder builder = new AlertDialog.Builder(SourcesOptionActivity.this);
        builder.setTitle(getString(R.string.googleTerms))
                .setCancelable(false)
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {


                    }
                })
                .setPositiveButton(getString(R.string.accept), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        new Thread(new Runnable() {
                            @Override
                            public void run() {

                                SharedPreferences sharedpreferences = getApplicationContext()
                                        .getSharedPreferences(Constants.SHOWN_GOOGLE_TOS, Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedpreferences.edit();
                                editor.putString(Constants.SHOWN_GOOGLE_TOS, "Yes");
                                editor.commit();

                                LSSDPNodes node = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(current_ipaddress);
                                LUCIControl control = new LUCIControl(current_ipaddress);
                                control.sendGoogleTOSAcceptanceIfRequired(SourcesOptionActivity.this, node.getgCastVerision());
                            }
                        }).start();

                        Toast.makeText(SourcesOptionActivity.this, getString(R.string.canUseGcastOptions), Toast.LENGTH_LONG).show();

                    }

                });


        AlertDialog googleTosAlert = builder.create();
        googleTosAlert.setView(gcastDialog);
        googleTosAlert.show();


/*        final TextView message = new TextView(context);
        // i.e.: R.string.dialog_message =>
        // "Test this dialog following the link to dtmilano.blogspot.com"
        final SpannableString s =
                new SpannableString(context.getText(R.string.google_tos));
        Linkify.addLinks(s, Linkify.WEB_URLS);
        message.setText(s);
        message.setMovementMethod(LinkMovementMethod.getInstance());

         new AlertDialog.Builder(context)
                .setTitle(R.string.google_tos)
                .setCancelable(true)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton("Accept", null)
                .setView(message)
                .create().show();*/
    }

    private SpannableString clickableString(String sampleString) {
        SpannableString spannableString = new SpannableString(sampleString);
        ClickableSpan googleTermsClicked = new ClickableSpan() {
            @Override
            public void onClick(View textView) {

                Intent intent = new Intent(getApplicationContext(), LibreWebViewActivity.class);
                intent.putExtra("url", "http://www.google.com/intl/en/policies/privacy/");
                startActivity(intent);
/*
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("http://www.google.com/intl/en/policies/privacy/"));
                startActivity(intent);
*/


            }
        };


        ClickableSpan termsClicked = new ClickableSpan() {
            @Override
            public void onClick(View textView) {

                Intent intent = new Intent(getApplicationContext(), LibreWebViewActivity.class);
                intent.putExtra("url", "https://www.google.com/intl/en/policies/terms/");
                startActivity(intent);

/*
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("https://www.google.com/intl/en/policies/terms/"));
                startActivity(intent);
*/
            }
        };

        ClickableSpan privacyClicked = new ClickableSpan() {
            @Override
            public void onClick(View textView) {

                Intent intent = new Intent(getApplicationContext(), LibreWebViewActivity.class);
                intent.putExtra("url", "http://www.google.com/intl/en/policies/privacy/");
                startActivity(intent);

/*
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("https://www.google.com/intl/en/policies/privacy/"));
                startActivity(intent);
*/
            }
        };

        spannableString.setSpan(googleTermsClicked, 4, 25, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(termsClicked, 395, 418, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(privacyClicked, 422, 444, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.brand_orange)), 4, 25,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.brand_orange)), 395, 418,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.brand_orange)), 422, 444,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spannableString;
    }

    private void readTheCurrentBluetoothStatus() {
        LUCIControl luciControl = new LUCIControl(current_ipaddress);
        luciControl.sendAsynchronousCommand();
        luciControl.SendCommand(MIDCONST.MID_BLUETOOTH, null, LSSDPCONST.LUCI_GET);
    }


    private void readTheCurrentAuxStatus() {
        LUCIControl luciControl = new LUCIControl(current_ipaddress);
        luciControl.SendCommand(50, null, LSSDPCONST.LUCI_GET);
        ///luciControl.SendCommand(MIDCONST.MID_REMOTE_UI, GET_HOME, LSSDPCONST.LUCI_SET);
    }

    class ListViewAdapter extends BaseAdapter {

        private Context _context;
        private List<String> _listDataHeader;

        public ListViewAdapter(Context context, List<String> listDataHeader) {
            this._context = context;
            this._listDataHeader = listDataHeader;
        }


        @Override
        public int getCount() {
            return _listDataHeader.size();
        }

        @Override
        public Object getItem(int i) {
            return _listDataHeader.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup viewGroup) {
            final String childText = _listDataHeader.get(i);

            if (convertView == null) {
                LayoutInflater infalInflater = (LayoutInflater) this._context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = infalInflater.inflate(R.layout.mediaserver_expandable_child, null);
            }

            TextView txtListChild = (TextView) convertView
                    .findViewById(R.id.lblListItem);
            ImageView view = (ImageView) convertView.findViewById(R.id.itemIcon);

            if (childText.equalsIgnoreCase(spotify))
                view.setImageResource(R.mipmap.spotify);
            else if (childText.equalsIgnoreCase(deezer))
                view.setImageResource(R.mipmap.deezer_logo);
            else if (childText.equalsIgnoreCase(tidal))
                view.setImageResource(R.mipmap.tidal_white_logo);
            else if (childText.equalsIgnoreCase(vtuner))
                view.setImageResource(R.mipmap.vtuner_logo);
            else if (childText.equalsIgnoreCase(tuneIn))
                view.setImageResource(R.mipmap.tunein_logo1);
            else if (childText.equalsIgnoreCase(qmusic))
                view.setImageResource(R.mipmap.qqmusic);
//            else if (childText.equalsIgnoreCase(favourite))
//                view.setImageResource(R.drawable.ic_favorites_white);


            txtListChild.setText(childText);
            return convertView;
        }
    }

    private void showLoaderAndAskSource(String source) {
        if (SourcesOptionActivity.this.isFinishing())
            return;
        //asking source
        LUCIControl luciControl = new LUCIControl(current_ipaddress);
        luciControl.SendCommand(50, null, LSSDPCONST.LUCI_GET);
        if (m_progressDlg == null) {
            m_progressDlg = new ProgressDialog(SourcesOptionActivity.this);
            m_progressDlg.setTitle("Please wait");
            m_progressDlg.setIndeterminate(true);
        }

        m_progressDlg.setMessage(source);
        if (!m_progressDlg.isShowing()) {
            m_progressDlg.show();
        }
        //asking source
        /*LUCIControl luciControl = new LUCIControl(current_ipaddress);
        luciControl.SendCommand(50, null, LSSDPCONST.LUCI_GET);*/
    }

    private void showLoader() {

        if (SourcesOptionActivity.this.isFinishing())
            return;

        if (m_progressDlg == null) {
            m_progressDlg = new ProgressDialog(SourcesOptionActivity.this);
        }
            m_progressDlg.setMessage(getString(R.string.loading));
            m_progressDlg.setCancelable(false);
           /* m_progressDlg.setButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    m_progressDlg.cancel();
                }
            });*/
//            m_progressDlg.show();


        if (!m_progressDlg.isShowing()) {
            m_progressDlg.show();
        }
    }

    private void closeLoader() {

        if (m_progressDlg != null) {

            if (m_progressDlg.isShowing() == true) {
                m_progressDlg.dismiss();
            }

        }

    }

    private void mRemoveRenderingWhenAuxOrBTisOn(String currentIpaddress) {
        try {
            LibreLogger.d(this, "Going to Remove the DMR Playback");
            RemoteDevice renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(currentIpaddress);
                    /* For the New DMR Implementation , whenever a Source Switching is happening
                    * we are Stopping the Playback */
            if (renderingDevice != null) {
                String renderingUDN = renderingDevice.getIdentity().getUdn().toString();
                PlaybackHelper playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP.get(renderingUDN);
                playbackHelper.StopPlayback();
                playbackHelper = null;
                LibreApplication.PLAYBACK_HELPER_MAP.remove(renderingUDN);
            }
        } catch (Exception e) {
            LibreLogger.d(this, "Removing the DMR Playback is Exception");
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

        LUCIPacket packet = new LUCIPacket(dataRecived.getMessage());
        LibreLogger.d(this, "Message recieved for ipaddress " + ipaddressRecieved + "command is " + packet.getCommand());

        if (current_ipaddress.equalsIgnoreCase(ipaddressRecieved)) {
            switch (packet.getCommand()) {

                case 42: {

                    String message = new String(packet.getpayload());
                    LibreLogger.d(this, " message 42 recieved  " + message);
                    try {
                        parseJsonAndReflectInUI(message);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        LibreLogger.d(this, " Json exception ");

                    }
                }
                break;

                /* This indicates the bluetooth status*/
                case 209: {


                    String message = new String(packet.getpayload());
                    LibreLogger.d(this, " message 209 is recieved  " + message);
                   /* if (BLUETOOTH_ON.equalsIgnoreCase(message)) {
                        bluetooth.setChecked(true, false);
                    } else {
                        bluetooth.setChecked(false, false);
                    }
*/
                }
                break;
                case 51: {

                    String message = new String(packet.getpayload());
                    try {
                        int duration = Integer.parseInt(message);
                        SceneObject sceneObject = mScanHandler.getSceneObjectFromCentralRepo().get(current_ipaddress);
                        sceneObject.setPlaystatus(duration);


                        if (mScanHandler.isIpAvailableInCentralSceneRepo(current_ipaddress)) {
                            mScanHandler.putSceneObjectToCentralRepo(current_ipaddress, sceneObject);
                        }

                        if (sceneObject.getCurrentSource() == 14) {
                            if (bluetooth.isChecked()) {
                                bluetooth.setChecked(false, false);
                            }
                            if (sceneObject.getPlaystatus() == SceneObject.CURRENTLY_PLAYING) {
                                auxbutton.setChecked(true, false);
                            }
                        } else if (sceneObject.getCurrentSource() == 19) {
                            if (auxbutton.isChecked()) {
                                auxbutton.setChecked(false, false);
                            }
                            if (sceneObject.getPlaystatus() == SceneObject.CURRENTLY_PLAYING)
                                bluetooth.setChecked(true, false);
                        } else {
                            auxbutton.setChecked(false, false);
                            bluetooth.setChecked(false, false);
                        }
                        LibreLogger.d(this, "Recieved the playstate to be" + sceneObject.getPlaystatus());
                    } catch (Exception e) {

                    }

                }
                break;

                case 50: {
                    String message = new String(packet.getpayload());
                    // Toast.makeText(getApplicationContext(),"Message 50 is Received"+message,Toast.LENGTH_SHORT).show();
                    LibreLogger.d(this, " message 50 is recieved  " + message);
                    if (message.contains("14")) {
                        credentialHandler.removeMessages(AUX_BT_TIMEOUT);
                        credentialHandler.sendEmptyMessage(AUX_BT_TIMEOUT);

                        SceneObject sceneObject = mScanHandler.getSceneObjectFromCentralRepo().get(current_ipaddress);
                        if (sceneObject != null) {
                            sceneObject.setCurrentSource(14);
                            mScanHandler.getSceneObjectFromCentralRepo().put(current_ipaddress, sceneObject);
                        }
                        /* Karunakaran : CrashAnalytics crash fix
                        if (sceneObject != null && sceneObject.getPlaystatus() == SceneObject.CURRENTLY_STOPPED || sceneObject.getPlaystatus() == SceneObject.CURRENTLY_NOTPLAYING) {
                            //auxbutton.setChecked(false,false);
                            return;
                        }*/
                        if (bluetooth.isChecked())
                            bluetooth.setChecked(false, false);
                        auxbutton.setChecked(true, false);
                    } else if (message.contains("19")) {
                        /*removing timeout and closing loader*/
                        credentialHandler.removeMessages(AUX_BT_TIMEOUT);
                        credentialHandler.sendEmptyMessage(AUX_BT_TIMEOUT);

                        SceneObject sceneObject = mScanHandler.getSceneObjectFromCentralRepo().get(current_ipaddress);
                        if (sceneObject != null) {
                            sceneObject.setCurrentSource(19);
                            mScanHandler.getSceneObjectFromCentralRepo().put(current_ipaddress, sceneObject);
                        }
                     /* This code is of No  Use
                     * *//* Karunakaran : CrashAnalytics crash fix *//*
                        if (sceneObject != null && (sceneObject.getPlaystatus() == SceneObject.CURRENTLY_STOPPED || sceneObject.getPlaystatus() == SceneObject.CURRENTLY_NOTPLAYING)) {
                            //bluetooth.setChecked(false,false);
                            return;
                        }*/
                        if (auxbutton.isChecked())
                            auxbutton.setChecked(false, false);
                        bluetooth.setChecked(true, false);
                    } else if (message.contains("0") || message.contains("NO_SOURCE")) {
                        LibreLogger.d(this, " No Source received hence closing dialog");
                        /**Closing loader after 1.5 second*/
                        credentialHandler.removeMessages(AUX_BT_TIMEOUT);
                        credentialHandler.sendEmptyMessageDelayed(AUX_BT_TIMEOUT, 1500);
                        bluetooth.setChecked(false, false);
                        auxbutton.setChecked(false, false);
                    } else {
                        SceneObject sceneObject = mScanHandler.getSceneObjectFromCentralRepo().get(current_ipaddress);
                        if (sceneObject != null)
                            sceneObject.setCurrentSource(-1);
                        auxbutton.setChecked(false, false);
                        bluetooth.setChecked(false, false);
                    }
                }
                break;
                case 208: {


                    /* This code is crashing for array out of index exception and hence will be handled with a try and catch -Praveen*/

                    try {
                        String messages = new String(packet.getpayload());


                        if (messages.contains("AlexaRefreshToken")) {
//                            alexaLayout.setVisibility(View.VISIBLE);
                            LibreLogger.d(this, " got alexa token " + messages);
                            String token = String.valueOf(messages.substring(messages.indexOf(":") + 1));
                            LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();
                            LSSDPNodes mNode = mNodeDB.getTheNodeBasedOnTheIpAddress(dataRecived.getRemotedeviceIp());
                            if (mNode != null) {
                                mNode.setAlexaRefreshToken(token);
                            }
                            credentialHandler.removeMessages(ALEXA_REFRESH_TOKEN_TIMER);
                            if (!doneClicked) {
                                handleAlexaRefreshTokenStatus(mNode);
                            }
                        }

                        LibreLogger.d(this, " got deezer " + messages);
                        String credentials[] = messages.split(":");

                        if (credentials.length == 0)
                            return;

                        String userName = "";
                        String userPassword = "";
                        String tidalUserName = "";
                        String tidalUserPassword = "";

                        /*Added for device state*/
                        Message message = Message.obtain();


                        if (credentials[0].contains("DeezerUserName")) {
                            userName = credentials[1];

                            message.obj = userName;
                            message.what = CREDENTIAL_USER_NAME;
                            credentialHandler.sendMessage(message);

                        }
                        if (credentials[0].contains("DeezerUserPassword")) {
                            userPassword = credentials[1];

                            message.obj = userPassword;
                            message.what = CREDENTIAL_PASSWORD;
                            credentialHandler.sendMessage(message);

                        }
                        if (credentials[0].contains("TidalUserName")) {
                            tidalUserName = credentials[1];

                            message.obj = tidalUserName;
                            message.what = CREDENTIAL_USER_NAME;
                            credentialHandler.sendMessage(message);


                        }
                        if (credentials[0].contains("TidalUserPassword")) {
                            tidalUserPassword = credentials[1];

                            message.obj = tidalUserPassword;
                            message.what = CREDENTIAL_PASSWORD;
                            credentialHandler.sendMessage(message);

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        LibreLogger.d(this, "Credentials not read! Error!! Khajan Bhai!");
                    }
                }
                break;

                case MIDCONST.ALEXA_COMMAND: {
                    String alexaMessage = new String(packet.getpayload());
                    LibreLogger.d(this, "Alexa Value From 230  " + alexaMessage);
                    try {
                        JSONObject jsonRootObject = new JSONObject(alexaMessage);
                        // JSONArray jsonArray = jsonRootObject.optJSONArray("Window CONTENTS");
                        JSONObject jsonObject = jsonRootObject.getJSONObject(LUCIMESSAGES.ALEXA_KEY_WINDOW_CONTENT);
                        productId = jsonObject.optString("PRODUCT_ID").toString();
                        dsn = jsonObject.optString("DSN").toString();
                        sessionId = jsonObject.optString("SESSION_ID").toString();
                        codeChallenge = jsonObject.optString("CODE_CHALLENGE").toString();
                        codeChallengeMethod = jsonObject.optString("CODE_CHALLENGE_METHOD").toString();
                        if (jsonObject.has("LOCALE"))
                            locale = jsonObject.optString("LOCALE").toString();

                        mDeviceProvisioningInfo = new DeviceProvisioningInfo(productId, dsn, sessionId, codeChallenge, codeChallengeMethod, locale);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (mDeviceProvisioningInfo != null)
                        alexaLayout.setVisibility(View.VISIBLE);
                }
                break;


            }

        }
    }

    private void handleAlexaRefreshTokenStatus(LSSDPNodes mNode) {
        closeLoader();
        if (mNode.getAlexaRefreshToken() == null || mNode.getAlexaRefreshToken().isEmpty()) {
                    /*not logged in*/
            Intent newIntent = new Intent(SourcesOptionActivity.this, AlexaSignInActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            newIntent.putExtra(Constants.CURRENT_DEVICE_IP, current_ipaddress);
            newIntent.putExtra(AppConstants.DEVICE_PROVISIONING_INFO, mDeviceProvisioningInfo);
            newIntent.putExtra(Constants.FROM_ACTIVITY, SourcesOptionActivity.class.getSimpleName());
            startActivity(newIntent);
//                    finish();
        } else {
            Intent i = new Intent(SourcesOptionActivity.this, AlexaThingsToTryDoneActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra(Constants.CURRENT_DEVICE_IP, current_ipaddress);
            i.putExtra(Constants.FROM_ACTIVITY, SourcesOptionActivity.class.getSimpleName());
            startActivity(i);
//                    finish();
        }
    }


    private void parseJsonAndReflectInUI(String jsonStr) throws JSONException {

        LibreLogger.d(this, "Json Recieved from remote device " + jsonStr);
        if (jsonStr != null) {

            try {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeLoader();
                    }
                });

                JSONObject root = new JSONObject(jsonStr);
                int cmd_id = root.getInt(TAG_CMD_ID);
                JSONObject window = root.getJSONObject(TAG_WINDOW_CONTENT);

                LibreLogger.d(this, "Command Id" + cmd_id);

                if (cmd_id == 1) {
                    /*
                      */
                    String Browser = window.getString(TAG_BROWSER);
                    if (Browser.equalsIgnoreCase("HOME")) {

                        /* Now we have succseefully got the stack intialiized to home */
                        credentialHandler.removeMessages(NETWORK_TIMEOUT);
                        unRegisterForDeviceEvents();
                        Intent intent = new Intent(SourcesOptionActivity.this, RemoteSourcesList.class);
                        intent.putExtra(Constants.CURRENT_DEVICE_IP, current_ipaddress);
                        intent.putExtra("current_source_index_selected", current_source_index_selected);
                        LibreLogger.d(this, "removing handler message");
                        startActivity(intent);
                        finish();
                    }
                }
            } catch (Exception e) {

            }
        }
    }


    /*this dialog will open when we click on deezer or tidal*/
    private void sourceAuthDialogue(final int position) {
        // custom dialog

        if (SourcesOptionActivity.this.isFinishing())
            return;

        final Dialog dialog = new Dialog(SourcesOptionActivity.this);
        dialog.setContentView(R.layout.deezer_auth_dialog);
        dialog.setTitle(getString(R.string.enterUsernamePassword));

        // set the custom dialog components - text, image and button
        final EditText userName = (EditText) dialog.findViewById(R.id.user_name);
        final EditText userPassword = (EditText) dialog.findViewById(R.id.user_password);


        userName.setText(this.userName);
        userPassword.setText(this.userPassword);

        Button submitButton = (Button) dialog.findViewById(R.id.deezer_ok_button);
        // if button is clicked, close the custom dialog
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (TextUtils.isEmpty(userName.getText().toString())) {
                    Toast.makeText(SourcesOptionActivity.this, getString(R.string.enterUsername), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(userPassword.getText().toString())) {
                    Toast.makeText(SourcesOptionActivity.this, getString(R.string.enterPassword), Toast.LENGTH_SHORT).show();
                    return;
                }

                //  m_progressDlg = ProgressDialog.show(SourcesOptionActivity.this, "Notice", "Loading...", true, true, null);
                showLoader();

                LUCIControl luciControl = new LUCIControl(current_ipaddress);
                if (position == 5) {
                    luciControl.SendCommand(MIDCONST.MID_DEEZER, LUCIMESSAGES.DEEZER_USERNAME + userName.getText().toString(),
                            LSSDPCONST.LUCI_SET);
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                    }
                    luciControl.SendCommand(MIDCONST.MID_DEEZER, LUCIMESSAGES.DEEZER_PASSWORD + userPassword.getText().toString(),
                            LSSDPCONST.LUCI_SET);
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                    }
                    luciControl.SendCommand(MIDCONST.MID_REMOTE_UI, GET_HOME, LSSDPCONST.LUCI_SET);
                } else if (position == 6) {
                    luciControl.SendCommand(MIDCONST.MID_DEEZER, LUCIMESSAGES.TIDAL_USERNAME + userName.getText().toString(),
                            LSSDPCONST.LUCI_SET);
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                    }
                    luciControl.SendCommand(MIDCONST.MID_DEEZER, LUCIMESSAGES.TIDAL_PASSWORD + userPassword.getText().toString(),
                            LSSDPCONST.LUCI_SET);
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                    }
                    luciControl.SendCommand(MIDCONST.MID_REMOTE_UI, GET_HOME, LSSDPCONST.LUCI_SET);

                }


                dialog.dismiss();
            }
        });

        dialog.show();
    }

    public void launchTheApp(String appPackageName) {

        /* Special check for the sources optin */
        LSSDPNodes masterNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(current_ipaddress);
        int mode = 1;

        if (masterNode.getNetworkMode().contains("WLAN") == true)
            mode = 0;
        ArrayList<LSSDPNodes> mSlaveList = mScanHandler.getSlaveListForMasterIp(current_ipaddress, mode);
        if (mSlaveList != null && mSlaveList.size() > 0) {
            if (LSSDPNodeDB.isLS9ExistsInTheNetwork())
                Toast.makeText(SourcesOptionActivity.this, getString(R.string.castGroupToast), Toast.LENGTH_LONG).show();
        }


        Intent intent = getPackageManager().getLaunchIntentForPackage(appPackageName);


        if (intent != null) {

/*
    commenting for avoidig forcefull DMR playback
            try {
                LibreLogger.d(this, "Going to Remove the DMR Playback");
                RemoteDevice renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(current_ipaddress);
                    */
/* For the New DMR Implementation , whenever a Source Switching is happening
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
            }
*/


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

    final private int PERMISSION_DEVICE_DISCOVERY = 100;
    public void afterPermit(){
        Intent msgIntent = new Intent(SourcesOptionActivity.this, LoadLocalContentService.class);
        startService(msgIntent);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_DEVICE_DISCOVERY: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && permissions[0].contains(Manifest.permission.READ_EXTERNAL_STORAGE)
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    LibreApplication.mStoragePermissionGranted = true;
                    LibreLogger.d(this, permissions[0] + " storage permission Granted");
                    afterPermit();
                    return;

                } else {

                    LibreApplication.mStoragePermissionGranted = false;
                    // permission denied!
//                    LibreLogger.d(this, permissions[0]+" seeked permission Denied. So, requesting again");
                    Toast.makeText(SourcesOptionActivity.this, getString(R.string.storagePermitToast), Toast.LENGTH_SHORT).show();
                    //request();

                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void storagePermit() {
        LibreLogger.d(this, "permit storagePermit");
        //seeking permission
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            LibreLogger.d(this, "permit READ_EXTERNAL_STORAGE Granted");
            afterPermit();
            return;
        } else {
            LibreLogger.d(this, "permit READ_EXTERNAL_STORAGE not Granted");
            // request is denied by the user. so requesting
            request();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void request() {
        if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            // user checked Never Ask again
            LibreLogger.d(this, "permit READ_EXTERNAL_STORAGE Denied for ever");
            // show dialog
            AlertDialog.Builder requestPermission = new AlertDialog.Builder(SourcesOptionActivity.this);
            requestPermission.setTitle(getString(R.string.permitNotAvailable))
                    .setMessage(getString(R.string.enableStoragePermit))
                    .setPositiveButton(getString(R.string.gotoSettings), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //navigate to settings
                            getAlertDialog1().dismiss();
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        }
                    })
                    .setCancelable(false);
            if (getAlertDialog1() == null) {
                setAlertDialog1(requestPermission.create());
            }
            if (getAlertDialog1() != null && !getAlertDialog1().isShowing())
                getAlertDialog1().show();

            return;
        }
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_DEVICE_DISCOVERY);

        return;
    }

    @Override
    protected void onResume() {
        super.onResume();
         /*Registering to receive messages*/
        registerForDeviceEvents(this);
        readTheCurrentBluetoothStatus();
        readTheCurrentAuxStatus();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            LibreLogger.d(this, "seeking permission for storage");
            storagePermit();
        } else {
            LibreApplication.mStoragePermissionGranted = true;
            afterPermit();
        }
        setAlexaSource();
    }

    private void setAlexaSource() {
        LSSDPNodes node = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(current_ipaddress);
        if (node != null
                && node.getmDeviceCap().getmSource().isAlexaAvsSource()) {
            alexaLayout.setVisibility(View.VISIBLE);
            return;
        }
        alexaLayout.setVisibility(View.GONE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        /*unregister events */
        if (credentialHandler != null)
            credentialHandler.removeCallbacksAndMessages(null);
        unRegisterForDeviceEvents();
        closeLoader();
    }
}
