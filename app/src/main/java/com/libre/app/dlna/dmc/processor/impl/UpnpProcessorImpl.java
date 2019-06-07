package com.libre.app.dlna.dmc.processor.impl;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.libre.LibreApplication;
import com.libre.app.dlna.dmc.gui.abstractactivity.UpnpListenerActivity;
import com.libre.app.dlna.dmc.processor.interfaces.UpnpProcessor;
import com.libre.app.dlna.dmc.processor.upnp.CoreUpnpService;
import com.libre.app.dlna.dmc.server.MusicServer;
import com.libre.app.dlna.dmc.utility.PlaybackHelper;
import com.libre.app.dlna.dmc.utility.UpnpDeviceManager;

import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.message.header.DeviceTypeHeader;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class UpnpProcessorImpl implements UpnpProcessor, RegistryListener {
    private final static String TAG = UpnpProcessorImpl.class.getSimpleName();
    public final static String DMS_NAMESPACE = "schemas-upnp-org";
    public final static String DMS_TYPE = "MediaServer";
    public final static String DMR_NAMESPACE = "schemas-upnp-org";
    public final static String DMR_TYPE = "MediaRenderer";


    private Activity m_activity;
    private Context mContext;


    private static CoreUpnpService.Binder m_upnpService;

    private ServiceConnection m_serviceConnection;


    private final List<UpnpProcessorListener> m_listeners;

    private MusicServer ms;


    public UpnpProcessorImpl(UpnpListenerActivity activity) {
        m_activity = activity;
        mContext=activity;
        m_listeners = new ArrayList<UpnpProcessorListener>();
        m_listeners.add(activity);
    }


    public UpnpProcessorImpl(Context context){
        mContext=context;
        m_listeners = new ArrayList<UpnpProcessorListener>();

    }
    public void bindUpnpService() {

        Log.d(TAG, "Upnp Service initiated");

        m_serviceConnection = new ServiceConnection() {

            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "Upnp Service is Null");
                m_upnpService = null;
            }

            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "Upnp Service Ready");
                m_upnpService = (CoreUpnpService.Binder) service;
                m_upnpService.getRegistry().addListener(UpnpProcessorImpl.this);

                fireOnStartCompleteEvent();
            }
        };

        Intent intent = new Intent(mContext, CoreUpnpService.class);
        mContext.bindService(intent, m_serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public void stopMusicServer() {
        if (ms != null) {
            ms.stopMediaServer();
        }
    }

    public void unbindUpnpService() {
        Log.d(TAG, "Unbind to service");
        if (m_upnpService != null) {
            try {
                if(m_serviceConnection!=null && mContext!=null) /*We Got the Crash in the CrashAnalaytics*/
				   mContext.unbindService(m_serviceConnection);


            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                                /* Added by Praveen to get the callback for service disconnection */
                //	fireOnServiceDisconnected();

                /* context resetting it to null*/
                mContext=null;


            }
        }
    }

    public void searchAll() {

        Log.d(TAG, "Search invoke");
        m_upnpService.getRegistry().removeAllRemoteDevices();
        m_upnpService.getControlPoint().search();

    }


    public void addListener(UpnpProcessorListener listener) {
        synchronized (m_listeners) {
            if (!m_listeners.contains(listener)) {
                Log.d("UpnpPrcosesorImpl", "addListener....");
                m_listeners.add(listener);
            }
        }
    }

    public void removeListener(UpnpProcessorListener listener) {
        synchronized (m_listeners) {
            if (m_listeners.contains(listener)) {
                Log.d("UpnpPrcosesorImpl", "removeListener....");
                m_listeners.remove(listener);
                /*to prevent memory leaks*/
                m_activity = null;
            }
        }
    }

    public ControlPoint getControlPoint() {
        return m_upnpService.getControlPoint();
    }

    @Override
    public RemoteDevice getRemoteDevice(String UDN) {
        for (RemoteDevice device : m_upnpService.getRegistry().getRemoteDevices()) {
            if (device.getIdentity().getUdn().toString().equals(UDN))
                return device;
        }

        return null;
    }

    public Collection<LocalDevice> getLocalDevices() {
        if (m_upnpService != null)
            return m_upnpService.getRegistry().getLocalDevices();

        return null;
    }

    public Collection<RemoteDevice> getRemoteDevices() {
        if (m_upnpService != null)
            return m_upnpService.getRegistry().getRemoteDevices();
        return null;
    }

    @SuppressWarnings("rawtypes")
    public Collection<RemoteDevice> getRemoteDMS() {
        DeviceType dmstype = new DeviceType(DMS_NAMESPACE, DMS_TYPE, 1);
        if (m_upnpService == null) return null;

        Collection<Device> devices = m_upnpService.getRegistry().getDevices(dmstype);
        if (devices == null) return null;

        ArrayList<RemoteDevice> remoteDev = new ArrayList<RemoteDevice>();
        for (Device dev : devices) {
            if (dev instanceof RemoteDevice)
                remoteDev.add((RemoteDevice) dev);
        }

        return remoteDev;
    }

    @SuppressWarnings("rawtypes")
    public Collection<RemoteDevice> getRemoteDMR() {
        DeviceType dmrtype = new DeviceType(DMR_NAMESPACE, DMR_TYPE, 1);
        if (m_upnpService == null) return null;

        Collection<Device> devices = m_upnpService.getRegistry().getDevices(dmrtype);
        if (devices == null) return null;

        ArrayList<RemoteDevice> remoteDev = new ArrayList<RemoteDevice>();
        for (Device dev : devices) {
            if (dev instanceof RemoteDevice)
                remoteDev.add((RemoteDevice) dev);
        }

        return remoteDev;
    }

    public LocalDevice getLocalDevice(String UDN) {

        for (LocalDevice device : m_upnpService.getRegistry().getLocalDevices()) {
            Log.i(TAG, "Local device:" + device.getDetails().getFriendlyName() + "," + device.getIdentity().getUdn().toString());
            if (device.getIdentity().getUdn().toString().compareTo(UDN) == 0)
                return device;
        }
        return null;
    }


    private void fireOnStartCompleteEvent() {
        synchronized (m_listeners) {
            for (UpnpProcessorListener listener : m_listeners) {
                listener.onStartComplete();
            }
        }
    }

    /* Added by Praveen to get the callback on service disconnection

     */
    private void fireOnServiceDisconnected() {
        synchronized (m_listeners) {
            for (UpnpProcessorListener listener : m_listeners) {
              //    listener.onServiceDisconnected();
            }
        }
    }

    @Override
    public void searchDMS() {
        {
            Log.d(TAG, "SearchDMS invoke");
            DeviceType type = new DeviceType(DMS_NAMESPACE, DMS_TYPE, 1);
            if (m_upnpService != null) {
                m_upnpService.getControlPoint().search(new DeviceTypeHeader(type));
            } else {
                Log.w(TAG, "UPnP Service is null");
            }
        }

    }

    @Override
    public void searchDMR() {

        Log.d(TAG, "SearchDMR invoke");
        DeviceType type = new DeviceType(DMR_NAMESPACE, DMR_TYPE, 1);
        if (m_upnpService != null) {
            //m_upnpService.getRegistry().removeAllRemoteDevices();
            m_upnpService.getControlPoint().search(new DeviceTypeHeader(type));
        } else {
            Log.w(TAG, "UPnP Service is null");
        }

    }

    public void ClearAll() {
        Log.d(TAG, "SearchDMR invoke");
        if (m_upnpService != null) {
            m_upnpService.getRegistry().removeAllRemoteDevices();
//				m_upnpService.getControlPoint().search(new DeviceTypeHeader(type));
        } else {
            Log.w(TAG, "UPnP Service is null");
        }

    }

    /* this function will check the renderer in the registry for the presence of the ip address*/

    public RemoteDevice getTheRendererFromRegistryIp(String ipAdd) {

        for (RemoteDevice device : m_upnpService.getRegistry().getRemoteDevices()) {

            String ip = device.getIdentity().getDescriptorURL().getHost();

            if (ip.equals(ipAdd))
                return device;
        }
        return null;
    }

    @Override
    public void remoteDeviceDiscoveryStarted(Registry registry,
                                             RemoteDevice device) {
        // TODO Auto-generated method stub
    }

    @Override
    public void remoteDeviceDiscoveryFailed(Registry registry,
                                            RemoteDevice device, Exception ex) {


        // TODO Auto-generated method stub
    }

    @Override
    public synchronized void remoteDeviceAdded(Registry registry, RemoteDevice device) {
        Log.d(TAG, "Device added call back for device " + device.getDisplayString());
        fireRemoteDeviceAddedEvent(device);
    }

    @Override
    public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
        // TODO Auto-generated method stub
    }

    @Override
    public synchronized void remoteDeviceRemoved(Registry registry, RemoteDevice device) {

        Log.d(TAG, "Device removed call back for device " );
        fireRemoteDeviceRemovedEvent(device);
    }

    @Override
    public void localDeviceAdded(Registry registry, LocalDevice device) {
        fireLocalDeviceAddedEvent(device);
    }

    @Override
    public void localDeviceRemoved(Registry registry, LocalDevice device) {

        fireLocalDeviceRemovedEvent(device);
    }

    @Override
    public void beforeShutdown(Registry registry) {


    }

    @Override
    public void afterShutdown() {


    }

    private void fireRemoteDeviceAddedEvent(RemoteDevice remoteDevice) {
        synchronized (m_listeners) {
            for (UpnpProcessorListener listener : m_listeners) {

                listener.onRemoteDeviceAdded(remoteDevice);
            }
        }
    }

    private void fireRemoteDeviceRemovedEvent(RemoteDevice remoteDevice) {
        synchronized (m_listeners) {
            for (UpnpProcessorListener listener : m_listeners) {
                listener.onRemoteDeviceRemoved(remoteDevice);

            }

        }
    }

    private void fireLocalDeviceAddedEvent(LocalDevice localDevice) {
        synchronized (m_listeners) {
            for (UpnpProcessorListener listener : m_listeners) {
                listener.onLocalDeviceAdded(localDevice);
            }
        }
    }

    private void fireLocalDeviceRemovedEvent(LocalDevice localDevice) {
        synchronized (m_listeners) {
            for (UpnpProcessorListener listener : m_listeners) {
                listener.onLocalDeviceRemoved(localDevice);
            }
        }
    }


    public void renew() {
        m_upnpService.renew();
        ms = MusicServer.getMusicServer();
        ms.clearMediaServer();
        UpnpDeviceManager.getInstance().clearMaps();
        LibreApplication.PLAYBACK_HELPER_MAP = new HashMap<String, PlaybackHelper>();

        m_upnpService.getRegistry().addDevice(ms.getMediaServer().getDevice());
        LibreApplication.LOCAL_UDN = ms.getMediaServer().getDevice().getIdentity().getUdn().toString();

        if(m_activity!=null)
//        ((LibreApplication) m_activity.getApplication()).setControlPoint(m_upnpService.getControlPoint());

        unbindUpnpService();
        bindUpnpService();

    }

    public CoreUpnpService.Binder getBinder() {
        return m_upnpService;
    }
}