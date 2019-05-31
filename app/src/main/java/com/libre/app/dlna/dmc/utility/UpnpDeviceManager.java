package com.libre.app.dlna.dmc.utility;

import android.util.Log;

import com.libre.LibreApplication;
import com.libre.app.dlna.dmc.processor.impl.UpnpProcessorImpl;
import com.libre.app.dlna.dmc.processor.interfaces.UpnpProcessor;

import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class UpnpDeviceManager implements UpnpProcessor.UpnpProcessorListener {
    private final static String TAG = UpnpDeviceManager.class.getSimpleName();
    private HashSet<RemoteDevice> remoteDms = new HashSet<RemoteDevice>();
    private HashSet<RemoteDevice> remoteDmr = new HashSet<RemoteDevice>();
    private HashMap<String, RemoteDevice> remoteDmsMap = new HashMap<String, RemoteDevice>();
    private HashMap<String, RemoteDevice> remoteDmrMap = new HashMap<String, RemoteDevice>();
    private HashMap<String, LocalDevice> localDmsMap = new HashMap<String, LocalDevice>();
    private HashMap<String, RemoteDevice> remoteDmrIPMap = new HashMap<String, RemoteDevice>();

    private ArrayList<String> remoteRemoved = new ArrayList<String>();
    private static UpnpDeviceManager manager = new UpnpDeviceManager();


    public static UpnpDeviceManager getInstance() {
        return manager;
    }

    @Override
    public void onRemoteDeviceAdded(RemoteDevice device) {
        // TODO Auto-generated method stub
		/*Log.d(TAG, "remote dev added:" + device.getDetails().getFriendlyName() +
				", addr:" + device.getIdentity().getDescriptorURL().getHost());*/
        //Log.d(TAG, device.toString());

        String udn = device.getIdentity().getUdn().toString();
        String ip = device.getIdentity().getDescriptorURL().getHost();

        if (device.getType().getNamespace().equals(UpnpProcessorImpl.DMS_NAMESPACE) &&
                device.getType().getType().equals(UpnpProcessorImpl.DMS_TYPE)) {
            if (remoteDmsMap.containsKey(udn)) {
                remoteDms.remove(device);
                remoteDmsMap.remove(udn);
            }
            remoteDms.add(device);
            remoteDmsMap.put(udn, device);
        } else if (device.getType().getNamespace().equals(UpnpProcessorImpl.DMR_NAMESPACE) &&
                device.getType().getType().equals(UpnpProcessorImpl.DMR_TYPE)) {

            Log.d(TAG, "remote dev added:" + device.getDetails().getFriendlyName() +
                    ", addr:" + device.getIdentity().getDescriptorURL().getHost());
            if (remoteDmrMap.containsKey(udn)) {
                remoteDmr.remove(device);
                remoteDmrMap.remove(udn);
                remoteDmrIPMap.remove(ip);
            }
            try {
                if (device != null && device.getDetails().getBaseURL().getHost() != null) {
                    if (device != null && remoteRemoved.contains(device.getDetails().getBaseURL().getHost())) {
                        remoteRemoved.remove(device.getDetails().getBaseURL().getHost());
                    }
                }
            } catch (Exception e) {

            }
            remoteDmr.add(device);
            remoteDmrMap.put(udn, device);
            remoteDmrIPMap.put(ip, device);


        }
    }

    @Override
    public void onRemoteDeviceRemoved(RemoteDevice device) {
        // TODO Auto-generated method stub


        if (device == null || device.getDetails() == null || device.getDetails().getFriendlyName() == null)
            return;


        Log.d(TAG, "remote dev removed:" + device.getDetails().getFriendlyName());
        String udn = device.getIdentity().getUdn().toString();
        if (remoteDmsMap.containsKey(udn)) {
            remoteDms.remove(device);
            remoteDmsMap.remove(udn);
        }
        try {
            if (device != null && device.getDetails().getBaseURL().getHost() != null) {
                remoteRemoved.add(device.getDetails().getBaseURL().getHost());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


		/* No REmovee for Testing 
		if (remoteDmrMap.containsKey(udn)) {
			remoteDmr.remove(device);
			remoteDmrMap.remove(udn);
			try {
				if (device != null && device.getDetails().getBaseURL().getHost() != null) {
					if (remoteDmrIPMap.containsKey(device.getDetails().getBaseURL().getHost())) {
						remoteDmrIPMap.remove(device.getDetails().getBaseURL().getHost());
					}
				}
			} catch (Exception e) {

			}
		}*/
    }

    @Override
    public void onLocalDeviceAdded(LocalDevice device) {
        // TODO Auto-generated method stub
        Log.i(TAG, "local dev added:" + device.getDetails().getFriendlyName());
        String udn = device.getIdentity().getUdn().toString();
        if (device.getType().getNamespace().equals(UpnpProcessorImpl.DMS_NAMESPACE) &&
                device.getType().getType().equals(UpnpProcessorImpl.DMS_TYPE)) {
            if (localDmsMap.containsKey(udn)) {
                localDmsMap.remove(udn);
            }
            localDmsMap.put(udn, device);
        }
    }

    @Override
    public void onLocalDeviceRemoved(LocalDevice device) {
        // TODO Auto-generated method stub
        Log.i(TAG, "local dev removed:" + device.getDetails().getFriendlyName());
        String udn = device.getIdentity().getUdn().toString();
        if (localDmsMap.containsKey(udn)) {
            localDmsMap.remove(udn);
            LibreApplication.LOCAL_UDN = "";
        }
    }

    @Override
    public void onStartComplete() {
        // TODO Auto-generated method stub
    }


    /* These to function doent have much of importance as they just indicate the connection to service - END*/

    public HashSet<RemoteDevice> getRemoteDms() {
        return remoteDms;
    }

    public HashSet<RemoteDevice> getRemoteDmr() {
        return remoteDmr;
    }

    public HashMap<String, RemoteDevice> getRemoteDmsMap() {
        return remoteDmsMap;
    }

    public HashMap<String, RemoteDevice> getRemoteDmrMap() {
        return remoteDmrMap;
    }

    public HashMap<String, LocalDevice> getLocalDmsMap() {
        return localDmsMap;
    }

    public void clearMaps() {
        remoteDmr = new HashSet<RemoteDevice>();
        remoteDmrMap = new HashMap<String, RemoteDevice>();
        remoteDms = new HashSet<RemoteDevice>();
        remoteDmsMap = new HashMap<String, RemoteDevice>();
        /*	localDmsMap = new HashMap<String, LocalDevice>();*/


    }

    /* Added by Praveen to get the device from the remote map*/
    public RemoteDevice getRemoteDMSDeviceByUDN(String udn) {

        return remoteDmsMap.get(udn);

    }

    public RemoteDevice getRemoteDMRDeviceByUDN(String udn) {

        return remoteDmrMap.get(udn);

    }

    public LocalDevice getLocalDMSDevicByUDN(String udn) {


        return localDmsMap.get(udn);

    }

    public RemoteDevice getRemoteDMRDeviceByIp(String ip) {

        return remoteDmrIPMap.get(ip);

    }

    public boolean getRemoteRemoved(String ipAddress) {
        return remoteRemoved.contains(ipAddress);
    }
}
