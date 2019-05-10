package com.libre.app.dlna.dmc;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.cumulations.libreV2.AppConstants;
import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.libre.LibreApplication;
import com.libre.R;
import com.libre.Scanning.Constants;
import com.libre.app.dlna.dmc.processor.impl.UpnpProcessorImpl;
import com.libre.app.dlna.dmc.processor.upnp.CoreUpnpService;
import com.libre.app.dlna.dmc.processor.upnp.LoadLocalContentService;
import com.libre.luci.Utils;
import com.libre.util.LibreLogger;

import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;

import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.HashMap;


public class LocalDMSActivity extends CTDeviceDiscoveryActivity {


    private static final int MEDIA_PROCESS_INIT = 1000;
    private static final int MEDIA_PROCESS_DONE = 1001;
    private static final int MEDIA_LOADING_FAIL = 1002;
            ;

    CoreUpnpService.Binder upnpService;
    private String TAG = "SAMPLE_SSDP";
    private ArrayAdapter<String> listAdapter;
    private ArrayList<String> udnList = new ArrayList<>();
    public final static String DMR_NAMESPACE = "schemas-upnp-org";
    public final static String DMR_TYPE = "MediaRenderer";


    private ProgressDialog mProgressDialog;
    private UpnpProcessorImpl m_upnpProcessor;
    private HashMap<String, String> nameToUDNMap = new HashMap<>();
    private boolean isLocalDeviceSelected;

    private String current_ipaddress;
    private HandlerThread mThread;
    private ServiceHandler mServiceHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dms_devices_list);

        LibreLogger.d(this, "onCreate of the activity is getting called");

        isLocalDeviceSelected = getIntent().getBooleanExtra(AppConstants.IS_LOCAL_DEVICE_SELECTED, true);
        current_ipaddress = getIntent().getStringExtra(Constants.CURRENT_DEVICE_IP);

        mThread = new HandlerThread(TAG, android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mThread.start();
        mServiceHandler = new ServiceHandler(mThread.getLooper());


            if (!(LibreApplication.LOCAL_UDN.trim().equalsIgnoreCase(""))) {
                /* Local device is already exists in the registery and hence launch the DMSBrowsingActivity or else wait*/
                Intent intent = new Intent(LocalDMSActivity.this, DMSBrowserActivity.class);
                intent.putExtra("device_udn", LibreApplication.LOCAL_UDN);
                intent.putExtra(Constants.CURRENT_DEVICE_IP, current_ipaddress);
                if (getIntent().hasExtra(Constants.FROM_ACTIVITY)){
                    intent.putExtra(Constants.FROM_ACTIVITY,"Nowplaying");
                }
                startActivity(intent);

                finish();
                return;
            }
            else{
                LibreLogger.d(this, "Interesting! local content ");
                /* this is the case where the content is not present and hence we need to load */
                showLoader();
                mServiceHandler.sendEmptyMessage(MEDIA_PROCESS_INIT);
            }
        

    }

    @Override
    public void onBackPressed() {


        super.onBackPressed();

    }

    @Override
    protected void onResume() {
        super.onResume();
      /*  upnpProcessor = new UpnpProcessorImpl(LocalDMSActivity.this);
        upnpProcessor.bindUpnpService();
        upnpProcessor.addListener(UpnpDeviceManager.getInstance());*/

    }


    @Override
    public void onStop() {
        super.onStop();
        closeLoader();

    }


    public void showLoader() {
        if (mProgressDialog == null)
            mProgressDialog = ProgressDialog.show(this, getString(R.string.pleaseWait), getString(R.string.loadingLocalContent), true, true);

        if (mProgressDialog.isShowing() == false)
            mProgressDialog.show();

    }

    public void closeLoader() {
        if (mProgressDialog != null && mProgressDialog.isShowing())
            mProgressDialog.cancel();

    }


    @Override
    public void onDestroy() {

        Log.v(TAG, "Destroy Main Activity");


       /* if (upnpProcessor != null) {
            upnpProcessor.unbindUpnpService();
            upnpProcessor.removeListener(UpnpDeviceManager.getInstance());
        }*/
        super.onDestroy();


    }


    @Override
    public void onRemoteDeviceAdded(final RemoteDevice device) {}

    @Override
    public void onLocalDeviceAdded(final LocalDevice device) {
        super.onLocalDeviceAdded(device);

        LibreLogger.d(this, "Added local device");


    }

    @Override
    public void onStartComplete() {

        LibreLogger.d(this, "on Start complete");
       /* upnpProcessor.searchDMR();
        upnpProcessor.searchDMS();*/

    //    Collection<LocalDevice> localDevices = upnpProcessor.getLocalDevices();

/*
        if (localDevices != null && localDevices.size() > 0) {
                LocalDevice device = localDevices.iterator().next();
                Intent intent = new Intent(LocalDMSActivity.this, DMSBrowserActivity.class);
                intent.putExtra("device_udn", LibreApplication.LOCAL_UDN);
                intent.putExtra(Constants.CURRENT_DEVICE_IP, current_ipaddress);
                startActivity(intent);
                finish();

        }
*/
    }


    Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);


            switch (msg.what) {

                case MEDIA_PROCESS_DONE:
                {
                    closeLoader();
                    LibreLogger.d(this, "Local songs are loaded" );
                    /*loading stop with success message*/
                    Toast.makeText(LocalDMSActivity.this,"Local content loading Done!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(LocalDMSActivity.this, DMSBrowserActivity.class);
                    intent.putExtra("device_udn", LibreApplication.LOCAL_UDN);
                    intent.putExtra(Constants.CURRENT_DEVICE_IP, current_ipaddress);
                    startActivity(intent);
                    finish();

                    break;

                }

                case MEDIA_LOADING_FAIL:{
                    closeLoader();
                    Toast.makeText(LocalDMSActivity.this,getString(R.string.restartAppManually), Toast.LENGTH_SHORT).show();
                }
                    break;

            }
        }
    };

    class ServiceHandler extends Handler {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            try {

                //isCommandinProcessing=true;;
                switch (msg.what) {
                    case MEDIA_PROCESS_INIT: {
                        long startTime = System.currentTimeMillis();
                        NetworkInterface mNetIf = Utils.getActiveNetworkInterface();

                        if(mNetIf==null) {
                            LibreLogger.d(this,"My Netif is Null");
                            handler.sendEmptyMessage(MEDIA_LOADING_FAIL);
                        }

                        else {
                            Intent msgIntent = new Intent(LocalDMSActivity.this, LoadLocalContentService.class);
                            startService(msgIntent);
                            Toast.makeText(LocalDMSActivity.this,getString(R.string.loadingLocalContent),Toast.LENGTH_LONG).show();
                        }

                        while (LibreApplication.LOCAL_UDN.trim().equalsIgnoreCase("")) {
                                Thread.sleep(200);
                        }

                            handler.sendEmptyMessageDelayed(MEDIA_PROCESS_DONE,1000l);

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                handler.sendEmptyMessage(MEDIA_LOADING_FAIL);

            }
        }
    }


}
