package com.cumulations.libreV2.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;

import com.cumulations.libreV2.AppConstants;
import com.libre.LibreApplication;
import com.libre.R;
import com.libre.Scanning.Constants;
import com.libre.app.dlna.dmc.DMSBrowserActivity;
import com.libre.app.dlna.dmc.processor.impl.UpnpProcessorImpl;
import com.libre.app.dlna.dmc.utility.UpnpDeviceManager;
import com.libre.util.LibreLogger;

import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;

import java.util.Collection;
import java.util.HashMap;

/**
 * This activity gets called when user clicks on Local/Network from the sources option.
 * <p/>
 * This activity binds to the CoreUPNPActivity and provides the devices list into UPNPDeviceManager file.
 * <p/>
 * If the selected option is Local then we will directly launch the DMSBrowser Atcivity to see the files from the local phone.
 * <p/>
 * If the selected option is Network from Sources we will first display list of DMS servers availabel and then continue to
 * browse through the files.
 * <p/>
 * Note: LibreApplication.Pla
 */
public class CTDMSDeviceListActivity extends CTDeviceDiscoveryActivity {
    private ArrayAdapter<String> listAdapter;
    ListView listView;
    private HashMap<String, String> nameToUDNMap = new HashMap<>();
    private boolean isLocalDeviceSelected;
    private String currentDeviceIp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ct_activity_dms_devices_list);

        LibreLogger.d(this, "onCreate of the activity is getting called");

        isLocalDeviceSelected = getIntent().getBooleanExtra(AppConstants.IS_LOCAL_DEVICE_SELECTED, true);
        currentDeviceIp = getIntent().getStringExtra(Constants.CURRENT_DEVICE_IP);

        if (isLocalDeviceSelected && !LibreApplication.LOCAL_UDN.trim().isEmpty()) {
            openDMSBrowser();
            return;
        }

        showLoader();

        listView = findViewById(R.id.deviceList);
        listAdapter = new ArrayAdapter<>(CTDMSDeviceListActivity.this, R.layout.ct_list_item_dms_device);
        listView.setAdapter(listAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        setMusicPlayerWidget((ViewGroup) findViewById(R.id.fl_music_play_widget),currentDeviceIp);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showLoader();
                    }
                });
                String item = listAdapter.getItem(i);
                Intent intent = new Intent(CTDMSDeviceListActivity.this, CTUpnpFileBrowserActivity.class);
                intent.putExtra(Constants.DEVICE_UDN, nameToUDNMap.get(item));
                intent.putExtra(Constants.CURRENT_DEVICE_IP, currentDeviceIp);
                startActivity(intent);
            }
        });


        findViewById(R.id.refresh).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getUpnpProcessor().searchAll();
                listAdapter.clear();
                listAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getUpnpProcessor()!=null) {
            getUpnpProcessor().addListener(UpnpDeviceManager.getInstance());
        }

    }

    @Override
    public void onStop() {
        /*if (m_upnpProcessor != null) {
            m_upnpProcessor.unbindUpnpService();
            m_upnpProcessor.removeListener(UpnpDeviceManager.getInstance());
        }*/
        super.onStop();
        closeLoader();
    }


    public void showLoader() {
        showProgressDialog(R.string.loadingMusic);
    }

    public void closeLoader() {
        dismissDialog();
    }

    @Override
    public void onRemoteDeviceAdded(final RemoteDevice device) {
        super.onRemoteDeviceAdded(device);
        LibreLogger.d(this, "Added Remote device");
        runOnUiThread(new Runnable() {
            public void run() {
                Log.d("onRemoteDeviceAdded","runOnUiThread "+device.getIdentity().getUdn().toString());
                if (device.getType().getNamespace().equals(UpnpProcessorImpl.DMS_NAMESPACE) &&
                        device.getType().getType().equals(UpnpProcessorImpl.DMS_TYPE)) {
                    int position = listAdapter.getPosition(device.getDetails().getFriendlyName());
                    if (position >= 0) {
                        // Device already in the list, re-set new value at same position
                        listAdapter.remove(device.getDetails().getFriendlyName());
                        listAdapter.insert(device.getDetails().getFriendlyName(), position);
                    } else {
                        listAdapter.add(device.getDetails().getFriendlyName());
                        listAdapter.notifyDataSetChanged();
                    }
                    closeLoader();
                }
            }
        });

        String udn = device.getIdentity().getUdn().toString();
        nameToUDNMap.put(device.getDetails().getFriendlyName(), udn);
    }

    @Override
    public void onLocalDeviceAdded(final LocalDevice device) {
        super.onLocalDeviceAdded(device);
        LibreLogger.d(this, "Added local device");
        String udn = device.getIdentity().getUdn().toString();
        nameToUDNMap.put(device.getDetails().getFriendlyName(), udn);
        if (isLocalDeviceSelected) {
            openDMSBrowser();
        }
    }

    @Override
    public void onStartComplete() {
        LibreLogger.d(this, "on Start complete");
        getUpnpProcessor().searchAll();
        Collection<LocalDevice> localDevices = getUpnpProcessor().getLocalDevices();
        if (localDevices != null && localDevices.size() > 0) {
            if (isLocalDeviceSelected) {
                openDMSBrowser();
            }
        }
    }

    private void openDMSBrowser() {
        startActivity(new Intent(CTDMSDeviceListActivity.this, CTDMSBrowserActivityV2.class)
                .putExtra(Constants.DEVICE_UDN, LibreApplication.LOCAL_UDN)
                .putExtra(Constants.CURRENT_DEVICE_IP, currentDeviceIp));
        finish();
    }


}
