package com.libre.app.dlna.dmc;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import com.cumulations.libreV2.AppConstants;
import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.libre.LibreApplication;
import com.libre.R;
import com.libre.Scanning.Constants;
import com.libre.app.dlna.dmc.processor.impl.UpnpProcessorImpl;
import com.libre.app.dlna.dmc.processor.upnp.CoreUpnpService;
import com.libre.util.LibreLogger;

import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;

import java.util.ArrayList;
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
public class DMSDeviceListActivity extends CTDeviceDiscoveryActivity {


    CoreUpnpService.Binder upnpService;
    private String TAG = "SAMPLE_SSDP";
    private ArrayAdapter<String> listAdapter;
    private ArrayList<String> udnList = new ArrayList<>();
    public final static String DMR_NAMESPACE = "schemas-upnp-org";
    public final static String DMR_TYPE = "MediaRenderer";


    ListView listView;
    private ProgressDialog mProgressDialog;
    private UpnpProcessorImpl m_upnpProcessor;
    private HashMap<String, String> nameToUDNMap = new HashMap<>();
    private boolean isLocalDeviceSelected;

    private String current_ipaddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dms_devices_list);

        LibreLogger.d(this, "onCreate of the activity is getting called");

        isLocalDeviceSelected = getIntent().getBooleanExtra(AppConstants.IS_LOCAL_DEVICE_SELECTED, true);
        current_ipaddress = getIntent().getStringExtra(Constants.CURRENT_DEVICE_IP);

        if (isLocalDeviceSelected) {
            if (!(LibreApplication.LOCAL_UDN.trim().equalsIgnoreCase(""))) {
                /* Local device is already exists in the registery and hence launch the DMSBrowsingActivity or else wait*/
                Intent intent = new Intent(DMSDeviceListActivity.this, DMSBrowserActivity.class);
                intent.putExtra("device_udn", LibreApplication.LOCAL_UDN);
                intent.putExtra(Constants.CURRENT_DEVICE_IP, current_ipaddress);

                startActivity(intent);

                finish();
                return;
            }
        }
        showLoader();

        listView = (ListView) findViewById(R.id.deviceList);
        listAdapter = new ArrayAdapter<>(DMSDeviceListActivity.this, R.layout.dms_device_list_item);
        listView.setAdapter(listAdapter);

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
                Intent intent = new Intent(DMSDeviceListActivity.this, DMSBrowserActivity.class);
                intent.putExtra("device_udn", nameToUDNMap.get(item));
                intent.putExtra(Constants.CURRENT_DEVICE_IP, current_ipaddress);
                startActivity(intent);
//                finish();
            }
        });


        ImageView refreshView = (ImageView) findViewById(R.id.refresh);
        refreshView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                m_upnpProcessor.searchAll();
                listAdapter.clear();
                listAdapter.notifyDataSetChanged();
            }
        });

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();

    }

    @Override
    protected void onResume() {
        super.onResume();
/*
        upnpProcessor = new UpnpProcessorImpl(DMSDeviceListActivity.this);
        upnpProcessor.bindUpnpService();
        upnpProcessor.addListener(UpnpDeviceManager.getInstance());
*/

    }


    @Override
    public void onStop() {
        super.onStop();
        closeLoader();

    }


    public void showLoader() {
        if (mProgressDialog == null)
            mProgressDialog = ProgressDialog.show(this, getString(R.string.notice), getString(R.string.loading), true, true);

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


     /*   if (upnpProcessor != null) {
            upnpProcessor.unbindUpnpService();
            upnpProcessor.removeListener(UpnpDeviceManager.getInstance());
        }  */
        super.onDestroy();


    }


    @Override
    public void onRemoteDeviceAdded(final RemoteDevice device) {
        super.onRemoteDeviceAdded(device);

        LibreLogger.d(this, "Added Remote device");


        runOnUiThread(new Runnable() {
            public void run() {

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
            Intent intent = new Intent(DMSDeviceListActivity.this, DMSBrowserActivity.class);
            intent.putExtra("device_udn", LibreApplication.LOCAL_UDN);
            intent.putExtra(Constants.CURRENT_DEVICE_IP, current_ipaddress);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onStartComplete() {

        LibreLogger.d(this, "on Start complete");
        m_upnpProcessor.searchAll();

        Collection<LocalDevice> localDevices = m_upnpProcessor.getLocalDevices();

        if (localDevices != null && localDevices.size() > 0) {
            if (isLocalDeviceSelected) {
                LocalDevice device = localDevices.iterator().next();
                Intent intent = new Intent(DMSDeviceListActivity.this, DMSBrowserActivity.class);
                intent.putExtra("device_udn", LibreApplication.LOCAL_UDN);
                intent.putExtra(Constants.CURRENT_DEVICE_IP, current_ipaddress);
                startActivity(intent);
                finish();
            }
        }
    }


}
