package com.libre.app.dlna.dmc;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cumulations.libreV2.activity.CTNowPlayingActivity;
import com.libre.ActiveScenesListActivity;
import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.libre.LErrorHandeling.LibreError;
import com.libre.LibreApplication;
import com.libre.R;
import com.libre.Scanning.Constants;
import com.libre.Scanning.ScanningHandler;
import com.libre.SceneObject;
import com.libre.SourcesOptionActivity;
import com.libre.app.dlna.dmc.processor.impl.DMSProcessorImpl;
import com.libre.app.dlna.dmc.processor.interfaces.DMSProcessor;
import com.libre.app.dlna.dmc.server.ContentTree;
import com.libre.app.dlna.dmc.server.MusicServer;
import com.libre.app.dlna.dmc.utility.DMRControlHelper;
import com.libre.app.dlna.dmc.utility.DMSBrowseHelper;
import com.libre.app.dlna.dmc.utility.PlaybackHelper;
import com.libre.app.dlna.dmc.utility.UpnpDeviceManager;
import com.libre.luci.LSSDPNodes;
import com.libre.luci.LUCIPacket;
import com.libre.netty.LibreDeviceInteractionListner;
import com.libre.netty.NettyData;
import com.libre.util.LibreLogger;

import org.fourthline.cling.model.meta.Action;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.model.types.ServiceType;
import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.Item;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class DMSBrowserActivity extends CTDeviceDiscoveryActivity implements DMSProcessor.DMSProcessorListener, LibreDeviceInteractionListner {
    private static final String TAG = DMSBrowserActivity.class.getName();
    protected DIDLObjectArrayAdapter m_adapter;
    //    protected UpnpProcessor upnpProcessor;
    protected DMSProcessor m_dmsProcessor;
    private TextView m_textHeadLine;
    private ListView m_listView;
    private EditText searchList;

    private Stack<DIDLObject> m_browseObjectStack = new Stack<DIDLObject>();
    private DMSBrowseHelper m_browseHelper;
    private ImageButton m_back;
    private ImageButton m_nowPlaying;
    private Animation m_operatingAnim = null;
    private int m_position = 0;
    private boolean m_isBrowseCancel = false;
    private boolean m_needSetListViewScroll = false;
    private List<DIDLObject> m_didlObjectList = new ArrayList<DIDLObject>();
    private List<DIDLObject> m_didlObjectListSearchResult = new ArrayList<DIDLObject>();
    private LibreApplication m_myApp;

    private int DO_BACKGROUND_DMR_TIMEOUT = 6000;

    private ProgressDialog m_progressDlg;
    AudioManager m_audioManager;
    private String dmsDeviceUDN;
    private String ipAddressOfTheRenderingDevice;
    private DIDLObject playerObjectSelectedByUser;

    final int NETWORK_TIMEOUT = 0x1;
    final int SERVICE_NOT_FOUND = 0x2;
    final int DO_BACKGROUND_DMR=0X3;

    final int DMR_TIMEOUT = 15000;


    private static final String TAG_CMD_ID = "CMD ID";
    private static final String TAG_WINDOW_CONTENT = "Window CONTENTS";
    private static final String TAG_BROWSER = "Browser";


    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case NETWORK_TIMEOUT: {
                    LibreLogger.d(this, "handler message recieved");
//                    Toast.makeText(DMSBrowserActivity.this, "Request timeout... Please try again", Toast.LENGTH_SHORT).show();
//                    showErrorMessage("Request timeout... Please try again");
                    LibreError error = new LibreError(ipAddressOfTheRenderingDevice, getString(R.string.requestTimeout));
                    showErrorMessage(error);
                    closeLoader();

                    Intent intent = new Intent(DMSBrowserActivity.this, CTNowPlayingActivity.class);

        /* This change is done to make sure that album art is reflectd after source swithing from Aux to other-START*/
                    SceneObject sceneObjectFromCentralRepo = ScanningHandler.getInstance().getSceneObjectFromCentralRepo(ipAddressOfTheRenderingDevice);
                    if (sceneObjectFromCentralRepo != null) {
            /* Setting the DMR source */
                        sceneObjectFromCentralRepo.setCurrentSource(2);
                    }
        /* This change is done to make sure that album art is reflectd after source swithing from Aux to other*- END*/

                    intent.putExtra(Constants.CURRENT_DEVICE_IP, ipAddressOfTheRenderingDevice);
                    startActivity(intent);
                    finish();
                    break;
                }
                case SERVICE_NOT_FOUND: {

                     /*Error case when we not able to find AVTransport Service*/
                    LibreError error = new LibreError(ipAddressOfTheRenderingDevice, getResources().getString(R.string.AVTRANSPORT_NOT_FOUND));
                    showErrorMessage(error);



                            Intent intent = new Intent(DMSBrowserActivity.this, CTNowPlayingActivity.class);

        /* This change is done to make sure that album art is reflectd after source swithing from Aux to other-START*/
                            SceneObject sceneObjectFromCentralRepo = ScanningHandler.getInstance().getSceneObjectFromCentralRepo(ipAddressOfTheRenderingDevice);
                            if (sceneObjectFromCentralRepo != null) {
            /* Setting the DMR source */
                                sceneObjectFromCentralRepo.setCurrentSource(2);
                            }
        /* This change is done to make sure that album art is reflectd after source swithing from Aux to other*- END*/

                            intent.putExtra(Constants.CURRENT_DEVICE_IP, ipAddressOfTheRenderingDevice);
                            startActivity(intent);
                            finish();

                    break;
                }

                case DO_BACKGROUND_DMR:
                    LibreLogger.d(this, "DMR search DO_BACKGROUND_DMR");
                    getUpnpProcessor().searchDMR();

                    RemoteDevice renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(ipAddressOfTheRenderingDevice);
                    if (renderingDevice==null && getUpnpProcessor() !=null){

                        LibreLogger.d(this, "we dont have the renderer in the Device Manager");
                        LibreLogger.d(this, "Checking the renderer in the registry");

                        /* Special check to make sure we dont have the device in the registry the */
                       renderingDevice= getUpnpProcessor().getTheRendererFromRegistryIp(ipAddressOfTheRenderingDevice);
                        if(renderingDevice!=null) {
                            LibreLogger.d(this, "WOW! We found the device in registry and hence we will start the playback with the new helper");
                            closeLoader();
                            play(playerObjectSelectedByUser);
                            handler.removeMessages(DO_BACKGROUND_DMR);
                            break;

                        }
                    }

                    if (renderingDevice!=null) {

                        String renderingUDN = renderingDevice.getIdentity().getUdn().toString();
                        /*if (LibreApplication.PLAYBACK_HELPER_MAP.get(renderingUDN) == null) {
                            handler.sendEmptyMessageDelayed(DO_BACKGROUND_DMR, DO_BACKGROUND_DMR_TIMEOUT);
                            LibreLogger.d(this, "DMR search request issued");
                        }
                        else*/
                        {
                            closeLoader();
                            play(playerObjectSelectedByUser);
                            handler.removeMessages(DO_BACKGROUND_DMR);
                        }
                    }else
                    {
                        handler.sendEmptyMessageDelayed(DO_BACKGROUND_DMR, DO_BACKGROUND_DMR_TIMEOUT);
                        LibreLogger.d(this, "DMR search request issued");
                    }



                    break;
            }


        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_browse_dmrcontent);

        dmsDeviceUDN = getIntent().getStringExtra(Constants.DEVICE_UDN);
        ipAddressOfTheRenderingDevice = getIntent().getStringExtra(Constants.CURRENT_DEVICE_IP);

        m_myApp = (LibreApplication) getApplication();
        /*m_operatingAnim = AnimationUtils.loadAnimation(this, R.anim.btnani);
        LinearInterpolator lin = new LinearInterpolator();
		m_operatingAnim.setInterpolator(lin);*/

        m_adapter = new DIDLObjectArrayAdapter(this, 0, 0, new ArrayList<DIDLObject>());
        m_listView = (ListView) findViewById(R.id.lv_ServerContent);
        m_listView.setAdapter(m_adapter);
        m_listView.setOnItemClickListener(itemClickListener);

        // hide the softkeyboard when touched anywhere in the activity except for search edittext
        m_listView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                LibreLogger.d(this," listview touched");
                hideSoftKeyboard();
                return false;
            }
        });

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                LibreLogger.d(this,"touched toolbar");
                hideSoftKeyboard();
                return false;
            }
        });

//        upnpProcessor = new UpnpProcessorImpl(this);
//        upnpProcessor.bindUpnpService();
//        upnpProcessor.addListener(UpnpDeviceManager.getInstance());
//        upnpProcessor.addListener(this);


        m_back = (ImageButton) findViewById(R.id.back);
        m_back.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });


        m_nowPlaying = (ImageButton) findViewById(R.id.nowplaying);
        m_nowPlaying.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                /* Commenting For the Bug While in DMR if we go to some folder and then Select Home button leads to Active Scene in below case, where if we play from music services and press home it comes back to Select Source/Content Scene */
                //Intent intent = new Intent(DMSBrowserActivity.this, ActiveScenesListActivity.class);
                Intent intent = new Intent(DMSBrowserActivity.this, SourcesOptionActivity.class);
                intent.putExtra(Constants.CURRENT_DEVICE_IP,ipAddressOfTheRenderingDevice);
                startActivity(intent);
                finish();
                //overridePendingTransition(R.anim.in_from_left, R.anim.out_to_right);
            }
        });

        searchList = (EditText)findViewById(R.id.searchList);
        searchList.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            LibreLogger.d(this,"text changed "+s );
                m_adapter.clear();
                m_didlObjectListSearchResult.clear();
                for (DIDLObject searchDid : m_didlObjectList){
                    if(searchDid.getTitle().toLowerCase().contains(s.toString().toLowerCase())){
                        LibreLogger.d(this, "exist " + s + " in " + searchDid.getTitle());
                       m_adapter.add(searchDid);
                        m_didlObjectListSearchResult.add(searchDid);
                    }
                    //m_listView.setAdapter(didlObjectArrayAdapter);
                    m_adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        m_textHeadLine = (TextView) findViewById(R.id.choosesong);

        m_audioManager = (AudioManager) getSystemService(Service.AUDIO_SERVICE);

    }

    @Override
    protected void onResume() {
        Log.d("UpnpSplashScreen", "DMS Resume");
        super.onResume();

        registerForDeviceEvents(this);

        if (getUpnpProcessor() != null) {
            getUpnpProcessor().addListener(this);
        }
    }


    @Override
    protected void onDestroy() {
        Log.d("UpnpSplashScreen", "DMS onDestroy"); // e
        /*if (upnpProcessor != null) {

            upnpProcessor.removeListener(this);
            upnpProcessor.unbindUpnpService();

        }*/
        super.onDestroy();

    }
    private Handler mTaskHandler;
    Runnable mMyTaskRunnable = new Runnable() {

        @Override

        public void run() {

            if(MusicServer.getMusicServer().isMediaServerReady()){
                m_listView.performItemClick(m_listView.getAdapter().getView(mPositionClicked, null, null), mPositionClicked, m_listView.getItemIdAtPosition(mPositionClicked));
/*                closeLoader();*/
                mPositionClicked=-1;
                Toast.makeText(DMSBrowserActivity.this,"Local content loading Done!", Toast.LENGTH_SHORT).show();
                return;
            }

            LibreLogger.d(this,"Loading the Songs..");
              /* and here comes the "trick" */
            mTaskHandler.postDelayed(mMyTaskRunnable, 100);

        }

    };
    private  int mPositionClicked=-1;
    private OnItemClickListener itemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> adapter, View view, int position, long arg3) {
            if(!MusicServer.getMusicServer().isMediaServerReady()) {
                Toast.makeText(DMSBrowserActivity.this,"Loading all contents,Please Wait", Toast.LENGTH_SHORT).show();
                mPositionClicked=position;
                showLoader();
                mTaskHandler = new Handler();
                mTaskHandler.postDelayed(mMyTaskRunnable,0);
                return;
            }
        //    m_position = position;
            final DIDLObject object = m_adapter.getItem(position);
            m_position = m_didlObjectList.indexOf(object);
            Log.i(TAG, "play item position:" + m_position);
            searchList.setText("");
        /*    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);*/
            hideSoftKeyboard();
            if (object instanceof Container) {
                m_browseObjectStack.push(object);
                browse(object);

            } else if (object instanceof Item) {

				/* Check the rendering the device is present or not .
                if it is present then we
				 */
                RemoteDevice renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(ipAddressOfTheRenderingDevice);

                if (renderingDevice != null && !UpnpDeviceManager.getInstance().getRemoteRemoved(ipAddressOfTheRenderingDevice)) {
                    play(object);
                } else {
                    m_progressDlg = ProgressDialog.show(DMSBrowserActivity.this, getString(R.string.searchingRenderer),getString(R.string.pleaseWait) ,true, true, cancelListener);
                    playerObjectSelectedByUser = object;
                    handler.sendEmptyMessage(DO_BACKGROUND_DMR);
                }
            }
        }
    };

    private OnCancelListener cancelListener = new OnCancelListener() {
        @Override
        public void onCancel(DialogInterface dialog) {
            // TODO Auto-generated method stub
            m_isBrowseCancel = true;
            onBackPressed();
        }
    };

    protected void browse(DIDLObject object) {
        String id = object.getId();
        Log.i(TAG, "Browse id:" + id);
        m_isBrowseCancel = false;
        if (id.equals(ContentTree.ROOT_ID)) {
            m_textHeadLine.setText(getString(R.string.browseSongs));
        } else {
            m_textHeadLine.setText(object.getTitle());
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    showLoader();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        try {
            m_dmsProcessor.browse(id);
        } catch (Throwable t) {
            closeLoader();
            Toast.makeText(this, getString(R.string.browseFailed), Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoader() {

        if (DMSBrowserActivity.this.isFinishing())
            return;

        if (m_progressDlg == null) {
            m_progressDlg = ProgressDialog.show(DMSBrowserActivity.this, getString(R.string.notice), getString(R.string.loading), true, true, cancelListener);
        }
        m_progressDlg.show();

    }

    private void closeLoader() {
        if (DMSBrowserActivity.this.isFinishing())
            return;

        if (m_progressDlg != null) {
            if (m_progressDlg.isShowing() == true) {
                m_progressDlg.dismiss();
            }

        }
    }


    private void play(DIDLObject object) {
        Log.i(TAG, "play item title:" + object.getTitle());

//        showLoader();
        hideSoftKeyboard();

        m_browseHelper.saveDidlListAndPosition(m_didlObjectList, m_position);
        Log.i(getClass().getName(), "position" + m_position);
        m_browseHelper.setBrowseObjectStack(m_browseObjectStack);
        m_browseHelper.setScrollPosition(m_listView.getFirstVisiblePosition());



		/* here first find the UDN of the current selected device */
        RemoteDevice renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(ipAddressOfTheRenderingDevice);
         if (renderingDevice==null && getUpnpProcessor() !=null){
             renderingDevice= getUpnpProcessor().getTheRendererFromRegistryIp(ipAddressOfTheRenderingDevice);

             UpnpDeviceManager.getInstance().onRemoteDeviceAdded(renderingDevice);



         }
        if (renderingDevice == null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(DMSBrowserActivity.this, getString(R.string.deviceNotFound), Toast.LENGTH_SHORT).show();

                }
            });
            return;
        }
        String renderingUDN = renderingDevice.getIdentity().getUdn().toString();


        if (LibreApplication.PLAYBACK_HELPER_MAP.get(renderingUDN) == null) {

			/* If it is local device then assign local audio manager */
            if (LibreApplication.LOCAL_UDN.equalsIgnoreCase(renderingUDN)) {

                DMRControlHelper dmr = new DMRControlHelper(m_audioManager);
                PlaybackHelper playbackHelper = new PlaybackHelper(dmr);
                LibreApplication.PLAYBACK_HELPER_MAP.put(renderingUDN, playbackHelper);
            } else {
                if (renderingDevice == null) return;
                RemoteService service = renderingDevice.findService(new ServiceType(DMRControlHelper.SERVICE_NAMESPACE,
                        DMRControlHelper.SERVICE_AVTRANSPORT_TYPE));
                if (service == null) {
                    /*AVTransport not found so showing error to user*/
                    handler.sendEmptyMessage(SERVICE_NOT_FOUND);
                    return;
                }
                /*crash Fix when Service is Null , stil lwe trying to get Actions.*/
                Action<RemoteService>[] actions = service.getActions();

                DMRControlHelper dmrControl = new DMRControlHelper(renderingUDN,
                        getUpnpProcessor().getControlPoint(), renderingDevice, service);
                PlaybackHelper m_playbackHelper = new PlaybackHelper(dmrControl);
                LibreApplication.PLAYBACK_HELPER_MAP.put(renderingUDN, m_playbackHelper);
            }
        } else {
            PlaybackHelper mPlay = LibreApplication.PLAYBACK_HELPER_MAP.get(renderingUDN);
            if (mPlay.getPlaybackStopped()) {
                RemoteService service = renderingDevice.findService(new ServiceType(DMRControlHelper.SERVICE_NAMESPACE,
                        DMRControlHelper.SERVICE_AVTRANSPORT_TYPE));
                if (service == null) {
                    /*AVTransport not found so showing error to user*/
                    handler.sendEmptyMessage(SERVICE_NOT_FOUND);
                    return;
                }
                DMRControlHelper dmrControl = new DMRControlHelper(renderingUDN,
                        getUpnpProcessor().getControlPoint(), renderingDevice, service);
                PlaybackHelper m_playbackHelper = new PlaybackHelper(dmrControl);
                LibreApplication.PLAYBACK_HELPER_MAP.put(renderingUDN, m_playbackHelper);
            }
        }

        try {
            PlaybackHelper playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP.get(renderingUDN);
            playbackHelper.setDmsHelper(m_browseHelper.clone());
            playbackHelper.playSong();

        m_myApp = (LibreApplication) getApplication();
        m_myApp.setDeviceIpAddress(ipAddressOfTheRenderingDevice);


//        commenting in the service
//        Intent serviceintent = new Intent(DMSBrowserActivity.this, ForegroundService.class);
//        serviceintent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
//        startService(serviceintent);


		/*m_myApp.setDmsBrowseHelperSaved(m_browseHelper);
        m_myApp.setPlayNewSong(true);
		if (m_myApp.getCurrentPlaybackHelper() != null) {
			m_myApp.getCurrentPlaybackHelper().setDmsHelper(m_browseHelper.clone());
		}*/
//		Intent intent = new Intent(this, ActiveScenesListActivity.class);


        //////////// timeout for dialog - showLoader() ///////////////////
//        handler.sendEmptyMessageDelayed(NETWORK_TIMEOUT, DMR_TIMEOUT);

        Intent intent = new Intent(this, CTNowPlayingActivity.class);

        /* This change is done to make sure that album art is reflectd after source swithing from Aux to other-START*/
        SceneObject sceneObjectFromCentralRepo = ScanningHandler.getInstance().getSceneObjectFromCentralRepo(ipAddressOfTheRenderingDevice);
        if (sceneObjectFromCentralRepo != null) {
            /* Setting the DMR source */
            sceneObjectFromCentralRepo.setCurrentSource(Constants.DMR_SOURCE);
            sceneObjectFromCentralRepo.setCurrentPlaybackSeekPosition(0);
            sceneObjectFromCentralRepo.setTotalTimeOfTheTrack(0);

        }
        /* This change is done to make sure that album art is reflectd after source swithing from Aux to other*- END*/

        intent.putExtra(Constants.CURRENT_DEVICE_IP, ipAddressOfTheRenderingDevice);
        startActivity(intent);
        finish();
        } catch (Exception e) {
                //handling the excetion when the device is rebooted
            LibreLogger.d(this,"EXCEPTION while setting the song!!!");
        }
    }

    @Override
    public void onBrowseComplete(String parentObjectId, final Map<String, List<? extends DIDLObject>> result) {
        LibreLogger.d(this,"Browse Completed");
        if (m_isBrowseCancel) {
            m_isBrowseCancel = false;
            return;
        }
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                m_adapter.clear();
                m_didlObjectList.clear();
                m_didlObjectListSearchResult.clear();
                searchList.setVisibility(View.VISIBLE);
                if(m_browseObjectStack.size()==1){
                    searchList.setVisibility(View.GONE);
                    LibreLogger.d(this,"Hide search at first page");
                }
                for (DIDLObject container : result.get("Containers")) {
                    m_adapter.add(container);
                    m_didlObjectList.add(container);
                }

                for (DIDLObject item : result.get("Items")) {
                    m_adapter.add(item);
                    m_didlObjectList.add(item);
                }
//				didlObjectArrayAdapter.notifyDataSetChanged();
                m_listView.setAdapter(m_adapter);

                closeLoader();

                if (m_adapter.isEmpty()) {
                    Toast.makeText(getApplicationContext(), getString(R.string.noContent), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (m_needSetListViewScroll) {
                    m_needSetListViewScroll = false;
                    m_listView.setSelection(m_browseHelper.getScrollPosition());
                }
            }
        });
    }

    @Override
    public void onBrowseFail(final String message) {
        LibreLogger.d(this,"Browse Failed");
        if (m_isBrowseCancel) {
            m_isBrowseCancel = false;
            return;
        }
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                closeLoader();
                Toast.makeText(DMSBrowserActivity.this,  getString(R.string.loadingMusic)+
                        message,Toast.LENGTH_LONG).show();
                onBackPressed();

            /*                if (!DMSBrowserActivity.this.isFinishing()) {
            new AlertDialog.Builder(DMSBrowserActivity.this)
            .setTitle("Error occurs")
            .setMessage(message)
            .setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog,
                                            int which) {
                            //TODO act failed
                            onBackPressed();
                        }
                    }).show();
            }*/

            }
        });
    }

    private void hideSoftKeyboard(){
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onBackPressed() {
        hideSoftKeyboard();

        if (m_browseObjectStack.isEmpty() || m_browseObjectStack.peek().getId().equals(ContentTree.ROOT_ID)) {

            Intent intent = new Intent(this, CTNowPlayingActivity.class);
             /* This change is done to make sure that whenever a Back Button is Pressed it should go to Now Playing window
             * without Selecting any Song *- END*/

            intent.putExtra(Constants.CURRENT_DEVICE_IP, ipAddressOfTheRenderingDevice);
            startActivity(intent);

            finish();
            //	overridePendingTransition(R.anim.in_from_left, R.anim.out_to_right);
//			super.onBackPressed();
        } else {
            m_browseObjectStack.pop();
            if (!m_browseObjectStack.isEmpty()) {
                browse(m_browseObjectStack.peek());
            }


        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void onStartComplete() {
        // TODO Auto-generated method stub

        if (LibreApplication.LOCAL_UDN.equalsIgnoreCase(dmsDeviceUDN))
            m_browseHelper = new DMSBrowseHelper(true, dmsDeviceUDN);
        else
            m_browseHelper = new DMSBrowseHelper(false, dmsDeviceUDN);

        if (m_browseHelper == null) return;

        Device dmsDev = m_browseHelper.getDevice(UpnpDeviceManager.getInstance());
        m_browseObjectStack = (Stack<DIDLObject>) m_browseHelper.getBrowseObjectStack().clone();

        if (dmsDev == null) {
            getUpnpProcessor().searchDMR();

            AlertDialog.Builder builder = new AlertDialog.Builder(DMSBrowserActivity.this);
            builder.setMessage(getString(R.string.deviceMissing))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.retry), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {


                            onStartComplete();
                        }

                    })
                    .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }

                    });


            AlertDialog alert = builder.create();
            alert.show();


        } else {
            m_dmsProcessor = new DMSProcessorImpl(dmsDev, getUpnpProcessor().getControlPoint());
            if (m_dmsProcessor == null) {
                Toast.makeText(this, getString(R.string.cannotCreateDMS), Toast.LENGTH_SHORT).show();
                this.finish();
            }
            else if(getIntent().hasExtra(Constants.FROM_ACTIVITY)){
                RemoteDevice renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(ipAddressOfTheRenderingDevice);
                if (renderingDevice != null) {
                    String renderingUDN = renderingDevice.getIdentity().getUdn().toString();
                    PlaybackHelper playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP.get(renderingUDN);
                    if (playbackHelper!=null && playbackHelper.getDmsHelper()!=null) {
                        m_browseObjectStack = playbackHelper.getDmsHelper().getBrowseObjectStack();
                        if (m_browseObjectStack != null) {
                            m_textHeadLine.setText(dmsDev.getDetails().getFriendlyName());
                            m_dmsProcessor.addListener(this);
                            m_needSetListViewScroll = true;
                            browse(m_browseObjectStack.peek());
                        }
                    }/*else{
                        *//* when we play from DMR and kill the app and open again. The source is 2(DMR).
                        At that instance playbackHelper or playbackHelper.getDmsHelper() is null.
                          So browse to root folder.*//*
                        dmsProcessor.addListener(this);
                        dmsProcessor.browse("0");
                    }*/
                }
            }
            else {
                m_textHeadLine.setText(dmsDev.getDetails().getFriendlyName());
                m_dmsProcessor.addListener(this);
                m_needSetListViewScroll = true;
                browse(m_browseObjectStack.peek());
            }
        }


		 /* initiating the search DMR */
        getUpnpProcessor().searchDMR();



    }

    @Override
    public void onRemoteDeviceAdded(RemoteDevice device) {
        super.onRemoteDeviceAdded(device);

        String ip = device.getIdentity().getDescriptorURL().getHost();
        LibreLogger.d(this, "Remote device with added with ip " + ip);

        if (ip.equalsIgnoreCase(ipAddressOfTheRenderingDevice)) {


            if (playerObjectSelectedByUser != null) {
                /* This is a case where user has selected a DMS source but that time  rendering device was null and hence we play with the storedPlayer object*/


                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                closeLoader();
                                play(playerObjectSelectedByUser);
                            }
                        }, 2000);

                    }
                });

            }

        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        unRegisterForDeviceEvents();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onRemoteDeviceRemoved(RemoteDevice device) {
        super.onRemoteDeviceRemoved(device);
        String ip = device.getIdentity().getDescriptorURL().getHost();
        LibreLogger.d(this, "Remote device with removed with ip " + ip);
        try {
		/* KK , Change for Continuing the play after Device Got connected */
            if(ip.equalsIgnoreCase(ipAddressOfTheRenderingDevice)){
                playerObjectSelectedByUser = null;
            }
            LibreLogger.d(this, "Remote device with removed with ip And Device Got Removed Funcitonn is Calling" + ip);
            //mDeviceGotRemoved(ip);
        } catch (Exception e) {
            LibreLogger.d(this, "Remote device with removed with ip And Device Got Removed Funcitonn is Called" + ip + "And Exception Happend");
        }
      /*  BusProvider.getInstance().post(ip);
        LUCIControl.luciSocketMap.remove(ip);*/
    }


    @Override
    public void deviceDiscoveryAfterClearingTheCacheStarted() {

    }

    @Override
    public void newDeviceFound(LSSDPNodes node) {

    }

    @Override
    public void deviceGotRemoved(String ipaddress) {


        if (ipAddressOfTheRenderingDevice!=null && ipAddressOfTheRenderingDevice.equalsIgnoreCase(ipaddress)){

            Intent intent = new Intent(this, ActiveScenesListActivity.class);
            startActivity(intent);
            finish();

        }
    }

    @Override
    public void messageRecieved(NettyData dataRecived) {

        LUCIPacket packet = new LUCIPacket(dataRecived.getMessage());

        if (packet.getCommand() == 42) {
            String message = new String(packet.getpayload());
            LibreLogger.d(this, " message 42 recieved  " + message);
            try {
                parseJsonAndReflectInUI(message);

            } catch (JSONException e) {
                e.printStackTrace();
                LibreLogger.d(this, " Json exception ");

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
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeLoader();
                        handler.removeMessages(NETWORK_TIMEOUT);
                    }
                });

                JSONObject root = new JSONObject(jsonStr);
                int cmd_id = root.getInt(TAG_CMD_ID);
                JSONObject window = root.getJSONObject(TAG_WINDOW_CONTENT);
                LibreLogger.d(this, "Command Id" + cmd_id);

                if (cmd_id == 3) {
                    /* This means user has selected the song to be playing and hence we will need to navigate
                     him to the Active scene list
                      */
                    unRegisterForDeviceEvents();
                    //Intent intent = new Intent(RemoteSourcesList.this, ActiveScenesListActivity.class);
                    Intent intent = new Intent(DMSBrowserActivity.this, CTNowPlayingActivity.class);
                    intent.putExtra(Constants.CURRENT_DEVICE_IP, ipAddressOfTheRenderingDevice);
                    SceneObject sceneObjectFromCentralRepo = ScanningHandler.getInstance().getSceneObjectFromCentralRepo(ipAddressOfTheRenderingDevice);
                    if (sceneObjectFromCentralRepo != null) {
                        /*this is DMR playback*/
                        sceneObjectFromCentralRepo.setCurrentSource(2);
                    }
                    startActivity(intent);
                    finish();

                }
            } catch (Exception e) {

            }

        }


    }
}
