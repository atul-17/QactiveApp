package com.libre.app.dlna.dmc.processor.upnp;


import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.cumulations.libreV2.AppUtils;
import com.libre.LibreApplication;
import com.libre.Ls9Sac.GcastUpdateData;
import com.libre.Scanning.ScanningHandler;
import com.libre.SceneObject;
import com.libre.app.dlna.dmc.processor.http.HttpThread;
import com.libre.app.dlna.dmc.server.MusicServer;
import com.libre.app.dlna.dmc.utility.PlaybackHelper;
import com.libre.app.dlna.dmc.utility.UpnpDeviceManager;
import com.libre.constants.LSSDPCONST;
import com.libre.constants.LUCIMESSAGES;
import com.libre.constants.MIDCONST;
import com.libre.luci.LSSDPNodeDB;
import com.libre.luci.LSSDPNodes;
import com.libre.luci.LUCIControl;
import com.libre.luci.LUCIPacket;
import com.libre.netty.BusProvider;
import com.libre.netty.NettyData;
import com.libre.netty.RemovedLibreDevice;
import com.libre.util.LibreLogger;
import com.squareup.otto.Subscribe;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceConfiguration;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.android.AndroidRouter;
import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.android.AndroidUpnpServiceConfiguration;
import org.fourthline.cling.android.AndroidUpnpServiceImpl;
import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.protocol.ProtocolFactory;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

public class CoreUpnpService extends AndroidUpnpServiceImpl {
    private static final int NOTIFICATION = 1500;
    //	private static final String TAG = CoreUpnpService.class.getkName();
    private HttpThread m_httpThread;
    private UpnpService upnpService;
    private Binder binder = new Binder();
    private NotificationManager m_notificationManager;
    private WifiLock m_wifiLock;
    public boolean isPaniceCaseGot;
    private WifiManager.MulticastLock multicastLock;

    @Override
    protected AndroidRouter createRouter(UpnpServiceConfiguration configuration, ProtocolFactory protocolFactory, Context context) {
        return super.createRouter(configuration, protocolFactory, context);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        WifiManager m_wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        m_wifiLock = m_wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "UpnpWifiLock");
        m_wifiLock.acquire();
        //HTTPServerData.HOST = Utility.intToIp(m_wifiManager.getDhcpInfo().ipAddress);

	/*	m_notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		m_httpThread = new HttpThread();
		m_httpThread.start();*/

        upnpService = new UpnpServiceImpl(createConfiguration());
        try {
            BusProvider.getInstance().register(busEventListener);
        } catch (Exception e) {

        }
        LibreLogger.d(this, "CoreUpnpService is getting created");

        try {
            multicastLock = m_wifiManager.createMulticastLock("multicastLock");
            multicastLock.setReferenceCounted(true);
            multicastLock.acquire();
        } catch (Exception e) {
            multicastLock = null;
        }
    }

    protected AndroidUpnpServiceConfiguration createConfiguration(WifiManager wifiManager) {
        return new AndroidUpnpServiceConfiguration();
    }

	/*protected AndroidWifiSwitchableRouter createRouter(UpnpServiceConfiguration configuration, ProtocolFactory protocolFactory,
            WifiManager wifiManager, ConnectivityManager connectivityManager) {
		return new AndroidWifiSwitchableRouter(configuration, protocolFactory, wifiManager, connectivityManager);
	}*/

    @Override
    public void onDestroy() {

        if (multicastLock != null) {
            multicastLock.release();
            multicastLock = null;
        }

        //unregisterReceiver(((AndroidWifiSwitchableRouter) upnpService.getRouter()).getBroadcastReceiver());
        try {

            LibreLogger.d(this, "CoreUpnpService is  Trying shutting down");


            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        upnpService.shutdown();
                        LibreLogger.d(this, "CoreUpnpService is  shutting down is Completed");

                        LibreLogger.d(this, "CoreUpnpService is  Trying to release WifiLock");
                        m_wifiLock.release();
                        LibreLogger.d(this, "CoreUpnpService is  Trying to WifiLock is Completed");
                    } catch (Exception ex) {
                        LibreLogger.d(this, "CoreUpnpService is  Trying to WifiLock  Got Exception" + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            }).start();


            try {
                BusProvider.getInstance().unregister(busEventListener);
            } catch (Exception e) {
                e.printStackTrace();
            }


        } catch (Exception ex) {
            LibreLogger.d(this, "CoreUpnpService is  Trying to shutting down but Got Exception"
                    + ex.getMessage());
            ex.printStackTrace();
        }


        super.onDestroy();
        //m_httpThread.stopHttpThread();

    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    protected boolean isListeningForConnectivityChanges() {
        return true;
    }

    public class Binder extends android.os.Binder implements AndroidUpnpService {

        public UpnpService get() {
            return upnpService;
        }

        public UpnpServiceConfiguration getConfiguration() {
            return upnpService.getConfiguration();
        }

        public Registry getRegistry() {
            return upnpService.getRegistry();
        }

        public ControlPoint getControlPoint() {
            return upnpService.getControlPoint();
        }

        public void renew() {


            Collection<RegistryListener> listeners = upnpService.getRegistry().getListeners();
            try {
                upnpService.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("CoreUpnpService", "Exception while switching networks");
            }

            WifiManager m_wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            m_wifiLock = m_wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "UpnpWifiLock");
            m_wifiLock.acquire();
            //HTTPServerData.HOST = Utility.intToIp(m_wifiManager.getDhcpInfo().ipAddress);

	/*	m_notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		m_httpThread = new HttpThread();
		m_httpThread.start();*/

            final WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

            final ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

            upnpService = new UpnpServiceImpl(createConfiguration());
            upnpService.getRegistry().removeAllRemoteDevices();
            Iterator<RegistryListener> iterator = listeners.iterator();

            while (iterator.hasNext()) {
                RegistryListener list = iterator.next();
                upnpService.getRegistry().addListener(list);
            }


            return;

        }


    }

	/*private void showNotification() {
        Notification notification = new Notification(R.drawable.ic_launcher, "CoreUpnpService started",
				System.currentTimeMillis());

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(), 0);

		notification.setLatestEventInfo(this, "CoreUpnpService", "Service is running", contentIntent);

		m_notificationManager.notify(NOTIFICATION, notification);
	}*/

    public void GoAndRemoveTheDevice(String ipadddress) {
        if (ipadddress != null) {
            LUCIControl.luciSocketMap.remove(ipadddress);
            BusProvider.getInstance().post(new RemovedLibreDevice(ipadddress));

            LSSDPNodeDB mNodeDB1 = LSSDPNodeDB.getInstance();
            LSSDPNodes mNode = mNodeDB1.getTheNodeBasedOnTheIpAddress(ipadddress);
            String mIpAddress = ipadddress;


            LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();
            try {
                if (ScanningHandler.getInstance().isIpAvailableInCentralSceneRepo(mIpAddress)) {
                    boolean status = ScanningHandler.getInstance().removeSceneMapFromCentralRepo(mIpAddress);
                    LibreLogger.d(this, "Active Scene Adapter For the Master " + status);
                }

            } catch (Exception e) {
                LibreLogger.d(this, "Active Scene Adapter" + "Removal Exception ");
            }
            mNodeDB.clearNode(mIpAddress);
        }

    }


    Object busEventListener = new Object() {


        @Subscribe
        public void newDeviceFound(final LSSDPNodes nodes) {

            /* This below if loop is introduced to handle the case where Device state from the DUT could be Null sometimes
             * Ideally the device state should not be null but we are just handling it to make sure it will not result in any crash!
             *
             * */
            checkForUPnPReinitialization();

            if (nodes == null || nodes.getDeviceState() == null) {
                Toast.makeText(CoreUpnpService.this, "Alert! Device State is null " + nodes.getDeviceState(), Toast.LENGTH_SHORT).show();
            } else if (nodes.getDeviceState() != null) {
                GcastUpdateData mGCastData = LibreApplication.GCAST_UPDATE_AVAILABE_LIST_DATA.get(nodes.getIP());
                if (mGCastData != null) {
                    LibreApplication.GCAST_UPDATE_AVAILABE_LIST_DATA.remove(nodes.getIP());
                }
                ArrayList<LUCIPacket> luciPackets = new ArrayList<LUCIPacket>();
                LUCIControl control = new LUCIControl(nodes.getIP());
               /* NetworkInterface mNetIf = com.libre.luci.Utils.getActiveNetworkInterface();
                String messageStr=  com.libre.luci.Utils.getLocalV4Address(mNetIf).getHostAddress() + "," + LUCIControl.LUCI_RESP_PORT;
                LUCIPacket packet1 = new LUCIPacket(messageStr.getBytes(), (short) messageStr.length(), (short) 3, (byte) LSSDPCONST.LUCI_SET);
                luciPackets.add(packet1);*/

                String readBTController = "READ_BT_CONTROLLER";

                LUCIPacket packet = new LUCIPacket(readBTController.getBytes(), (short) readBTController.length(),
                        (short) MIDCONST.MID_ENV_READ, (byte) LSSDPCONST.LUCI_GET);
                luciPackets.add(packet);

                String readAlexaRefreshToken = "READ_AlexaRefreshToken";
                LUCIPacket alexaPacket = new LUCIPacket(readAlexaRefreshToken.getBytes(), (short) readAlexaRefreshToken.length(),
                        (short) MIDCONST.MID_ENV_READ, (byte) LSSDPCONST.LUCI_GET);
                luciPackets.add(alexaPacket);


                String readModelType = "READ_Model";
                /*sending first 208*/
                LUCIPacket packet3 = new LUCIPacket(readModelType.getBytes(),
                        (short) readModelType.length(), (short) MIDCONST.MID_ENV_READ, (byte) LSSDPCONST.LUCI_GET);
                luciPackets.add(packet3);

                LUCIPacket packet2 = new LUCIPacket(null, (short) 0, (short) MIDCONST.VOLUME_CONTROL, (byte) LSSDPCONST.LUCI_GET);
                luciPackets.add(packet2);

                /*LUCIPacket packet3 = new LUCIPacket(null, (short) 0, (short) MIDCONST.NEW_SOURCE, (byte) LSSDPCONST.LUCI_GET);
                luciPackets.add(packet3);*/

                if (nodes.getgCastVerision() != null) {
                    LibreLogger.d(this, "Sending GCAST 226 value read command");
                    LUCIPacket packet4 = new LUCIPacket(null, (short) 0, (short) MIDCONST.GCAST_TOS_SHARE_COMMAND, (byte) LSSDPCONST.LUCI_GET);
                    luciPackets.add(packet4);
                }

                LUCIPacket packet6 = new LUCIPacket(null, (short) 0, (short) MIDCONST.MID_CURRENT_SOURCE, (byte) LSSDPCONST.LUCI_GET);
                luciPackets.add(packet6);
                LUCIPacket packet7 = new LUCIPacket(null, (short) 0, (short) MIDCONST.MID_CURRENT_PLAY_STATE, (byte) LSSDPCONST.LUCI_GET);
                luciPackets.add(packet7);

//                if (nodes.getDeviceState().contains("M")) {
//                    LUCIPacket packet5 = new LUCIPacket(null, (short) 0, (short) MIDCONST.ZONE_VOLUME, (byte) LSSDPCONST.LUCI_GET);
//                    luciPackets.add(packet5);
//                }

                control.SendCommand(luciPackets);
                if (nodes.getDeviceState() != null) {
                    if (!ScanningHandler.getInstance().isIpAvailableInCentralSceneRepo(nodes.getIP())) {
                        SceneObject sceneObjec = new SceneObject(" ", nodes.getFriendlyname(), 0, nodes.getIP());
                        ScanningHandler.getInstance().putSceneObjectToCentralRepo(nodes.getIP(), sceneObjec);
                        LibreLogger.d(this, "Device is not available in Central Repo So Created SceneObject " + nodes.getIP());
                    }
                }


            }
        }

        public void changeDeviceStateStereoConcurrentZoneids(String mIpAddress, String mMessage) {
            ScanningHandler mScanHandler = ScanningHandler.getInstance();
            LSSDPNodes mToBeUpdateNode = mScanHandler.getLSSDPNodeFromCentralDB(mIpAddress);
            if (mToBeUpdateNode != null && mMessage != null) {
                String[] mMessageArray = mMessage.split(",");
                if (mMessageArray.length > 0) {
                    if (mMessageArray[0].equalsIgnoreCase("Master")) {
                        mToBeUpdateNode.setDeviceState("M");
                        /* Create Scene Object */
                        {
                            if (!ScanningHandler.getInstance().isIpAvailableInCentralSceneRepo(mIpAddress)) {
                                SceneObject sceneObjec = new SceneObject(" ", mToBeUpdateNode.getFriendlyname(), 0, mIpAddress);
                                ScanningHandler.getInstance().putSceneObjectToCentralRepo(mToBeUpdateNode.getIP(), sceneObjec);
                                LibreLogger.d(this, "Master is not available in Central Repo So Created SceneObject " + mToBeUpdateNode.getIP());
                            }
                        }
                    }
                    if (mMessageArray[0].equalsIgnoreCase("Slave")) {
                        mToBeUpdateNode.setDeviceState("S");
                        mToBeUpdateNode.setCurrentState(LSSDPNodes.STATE_CHANGE_STAGE.CHANGE_SUCCESS);
                        /* Remove the SceneObject if a Device transition is happened */
                        ScanningHandler.getInstance().removeSceneMapFromCentralRepo(mIpAddress);
                    }
                    if (mMessageArray[0].equalsIgnoreCase("Free")) {
                        mToBeUpdateNode.setDeviceState("F");
                        mToBeUpdateNode.setCurrentState(LSSDPNodes.STATE_CHANGE_STAGE.CHANGE_SUCCESS);
                        /* Remove the SceneObject if a Device transition is happened */
                        ScanningHandler.getInstance().removeSceneMapFromCentralRepo(mIpAddress);
                    }

                    if (mMessageArray[1].equalsIgnoreCase("STEREO")) {
                        mToBeUpdateNode.setSpeakerType("0");
                    }
                    if (mMessageArray[1].equalsIgnoreCase("LEFT")) {
                        mToBeUpdateNode.setSpeakerType("1");
                    }
                    if (mMessageArray[1].equalsIgnoreCase("RIGHT")) {
                        mToBeUpdateNode.setSpeakerType("2");
                    }
                    if (mToBeUpdateNode.getNetworkMode().equalsIgnoreCase("WLAN")) {
                        mToBeUpdateNode.setcSSID(mMessageArray[2]);
                    } else {
                        mToBeUpdateNode.setZoneID(mMessageArray[2]);
                    }
                    LSSDPNodeDB.getInstance().renewLSSDPNodeDataWithNewNode(mToBeUpdateNode);
                }
            }
        }

        @Subscribe
        public void newMessageRecieved(final NettyData nettyData) {
            LUCIPacket dummyPacket = new LUCIPacket(nettyData.getMessage());
            LibreLogger.d(this, "New message appeared for the device " + nettyData.getRemotedeviceIp() +
                    "For the msg " +
                    new String(dummyPacket.getpayload()) + " for the command " + dummyPacket.getCommand());

            /*Updating the last notified Time for all the Device*/
            if (nettyData != null) {
                if (LUCIControl.luciSocketMap.containsKey(nettyData.getRemotedeviceIp()))
                    LUCIControl.luciSocketMap.get(nettyData.getRemotedeviceIp()).setLastNotifiedTime(System.currentTimeMillis());
            }


            LUCIPacket packet = new LUCIPacket(nettyData.getMessage());

            if (packet.getCommandStatus() == 1) {
                LibreLogger.d(this, "Command status 1 recieved for " + packet.getCommandStatus());

            }

            if (packet.getCommand() == MIDCONST.VOLUME_CONTROL) {
                String volumeMessage = new String(packet.getpayload());
                try {
                    int volume = Integer.parseInt(volumeMessage);
                    /*this map is having all volumes*/
                    LibreApplication.INDIVIDUAL_VOLUME_MAP.put(nettyData.getRemotedeviceIp(), volume);
                    Log.d("VolumeMapUpdating", "" + volume);
                } catch (Exception e) {
                    LibreLogger.d(this, "Exception occurred in newMessageReceived");
                }

            }

            if (packet.getCommand() == MIDCONST.SET_UI) {

                try {
                    LibreLogger.d(this, "Recieved Playback url in 42 at coreup");
                    String message = new String(packet.payload);
                    JSONObject root = new JSONObject(message);
                    int cmd_id = root.getInt(LUCIMESSAGES.TAG_CMD_ID);

                    if (cmd_id == 3) {
                        JSONObject window = root.getJSONObject(LUCIMESSAGES.ALEXA_KEY_WINDOW_CONTENT);
                        ScanningHandler mScanHandler = ScanningHandler.getInstance();
                        SceneObject mSceneObj = mScanHandler.getSceneObjectFromCentralRepo(nettyData.getRemotedeviceIp());
                        mSceneObj.setPlayUrl(window.getString("PlayUrl").toLowerCase());

                    }

                } catch (Exception e) {

                    e.printStackTrace();
                }

            }

            if (packet.getCommand() == MIDCONST./*ZONE_VOLUME*/VOLUME_CONTROL) {
                String volumeMessage = new String(packet.getpayload());
                try {
                    int volume = Integer.parseInt(volumeMessage);
                    /*this map is having Masters volume*/
                    LibreApplication./*ZONE_VOLUME_MAP*/INDIVIDUAL_VOLUME_MAP.put(nettyData.getRemotedeviceIp(), volume);
                    Log.d("VolumeMapUpdating", "" + volume);
                } catch (Exception e) {
                    LibreLogger.d(this, "Exception occurred in newMessageReceived");
                }
            }

            if (packet.getCommand() == MIDCONST.MID_SCENE_NAME) {
                String msg = new String(packet.getpayload());
                try {
                    LibreLogger.d(this, "Scene Name updation in CTDeviceDiscoveryActivity");
                         /* if Command Type 1 , then Scene Name information will be come in the same packet
if command type 2 and command status is 1 , then data will be empty., at that time we should not update the value .*/
                    if (packet.getCommandStatus() == 1
                            && packet.getCommandType() == 2)
                        return;
                    ScanningHandler mScanHandler = ScanningHandler.getInstance();
                    SceneObject mSceneObj = mScanHandler.getSceneObjectFromCentralRepo(nettyData.getRemotedeviceIp());
                    if (mSceneObj != null)
                        mSceneObj.setSceneName(msg);
                } catch (Exception e) {
                    LibreLogger.d(this, "Exception occurred in newMessageReceived");
                }
            }

            /*this has been added for DMR hijacking issue*/
            if (packet.getCommand() == MIDCONST.NEW_SOURCE) {
                String msg = new String(packet.getpayload());
                try {
                    int duration = Integer.parseInt(msg);
                    LibreLogger.d(this, "Current source updation in CTDeviceDiscoveryActivity");
                    Log.d("DMR_CURRENT_SOURCE_BASE", "" + duration);
                    ScanningHandler mScanHandler = ScanningHandler.getInstance();
                    SceneObject mSceneObj = mScanHandler.getSceneObjectFromCentralRepo(nettyData.getRemotedeviceIp());
                    if (mSceneObj != null)
                        mSceneObj.setCurrentSource(duration);
                    LSSDPNodeDB mLssdpNodeDb = LSSDPNodeDB.getInstance();
                    final LSSDPNodes mNode = mLssdpNodeDb.getTheNodeBasedOnTheIpAddress(nettyData.getRemotedeviceIp());
                    if (mNode != null) {
                        mNode.setCurrentSource(duration);
                    }
                    mLssdpNodeDb.renewLSSDPNodeDataWithNewNode(mNode);
                } catch (Exception e) {
                    LibreLogger.d(this, "Exception occurred in newMessageReceived");
                }
            }
            /* Device State ACK Implementation */
            if (packet.getCommand() == MIDCONST.MID_DEVICE_STATE_ACK) {
                try {
                    changeDeviceStateStereoConcurrentZoneids(nettyData.getRemotedeviceIp(), new String(packet.getpayload()));
                } catch (Exception e) {
                    e.printStackTrace();
                    LibreLogger.d(this, "Exception occurred in newMessageReceived of 103");
                }
            }

            if (packet.getCommand() == MIDCONST.NETWORK_STATUS_CHANGE) {
                String msg = new String(packet.getpayload());
                try {

                    LibreLogger.d(this, "Current NetworkS Status updation in CTDeviceDiscoveryActivity" + nettyData.getRemotedeviceIp());
                    LibreLogger.d(this, "Current Net status" + msg);
                        /*ScanningHandler mScanHandler = ScanningHandler.getInstance();
                        SceneObject mSceneObj = mScanHandler.getSceneObjectFromCentralRepo(nettyData.getRemotedeviceIp());
                        if (mSceneObj != null)
                            mSceneObj.setCurrentSource(duration);*/
                } catch (Exception e) {
                    LibreLogger.d(this, "Exception occurred in newMessageReceived");
                }
            }
            if (packet.getCommand() == MIDCONST.MID_ENV_READ) {
                try {
                    // for getting env item
                    String messages = new String(packet.getpayload());
                    LibreLogger.d(this, "MID_ENV_READ value is " + messages);

                    if (messages.contains("speechvolume")) {
                        String speechvolume = String.valueOf(messages.substring(messages.indexOf(":") + 1)); /*40,1*/
                        LibreLogger.d(this, "got READ_speechvolume " + messages);
                        LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();
                        LSSDPNodes mNode = mNodeDB.getTheNodeBasedOnTheIpAddress(nettyData.getRemotedeviceIp());
                        if (mNode != null) {
                            mNode.setSpeechVolume(speechvolume);
                        }
                    }
                    if (messages.contains("AlexaRefreshToken")) {
                        String token = messages.substring(messages.indexOf(":") + 1);
                        LibreLogger.d(this, "AlexaRefreshToken value " + token);
                        LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();
                        LSSDPNodes mNode = mNodeDB.getTheNodeBasedOnTheIpAddress(nettyData.getRemotedeviceIp());
                        if (mNode != null) {
                            mNode.setAlexaRefreshToken(token);
                        }
                    } else {
                        int value = Integer.parseInt(messages.substring(messages.indexOf(":") + 1));
                        LibreLogger.d(this, "BT_CONTROLLER value after parse is " + value);

                        ScanningHandler mScanHandler = ScanningHandler.getInstance();
                        SceneObject mSceneObj = mScanHandler.getSceneObjectFromCentralRepo(nettyData.getRemotedeviceIp());
                        mSceneObj.setBT_CONTROLLER(value);
                    }

                } catch (Exception e) {
                    LibreLogger.d(this, "error in 208 " + e.toString());
                }
            }


            if (packet.getCommand() == MIDCONST.MID_CURRENT_SOURCE) {
                String msg = new String(packet.getpayload());
                try {

                    int duration = Integer.parseInt(msg);
                    LibreLogger.d(this, "Current source updation in CTDeviceDiscoveryActivity");
                    Log.d("DMR_CURRENT_SOURCE_BASE", "" + duration);
                    ScanningHandler mScanHandler = ScanningHandler.getInstance();
                    SceneObject mSceneObj = mScanHandler.getSceneObjectFromCentralRepo(nettyData.getRemotedeviceIp());
                    if (mSceneObj != null)
                        mSceneObj.setCurrentSource(duration);
                    LSSDPNodeDB mLssdpNodeDb = LSSDPNodeDB.getInstance();
                    final LSSDPNodes mNode = mLssdpNodeDb.getTheNodeBasedOnTheIpAddress(nettyData.getRemotedeviceIp());
                    if (mNode != null) {
                        mNode.setCurrentSource(duration);
                    }
                    mLssdpNodeDb.renewLSSDPNodeDataWithNewNode(mNode);
                } catch (Exception e) {
                    LibreLogger.d(this, "Exception occurred in newMessageReceived");
                }
            }
            if (packet.getCommand() == MIDCONST.MID_CURRENT_PLAY_STATE) {
                String msg = new String(packet.getpayload());
                try {

                    int duration = Integer.parseInt(msg);
                    LibreLogger.d(this, "Current source updation in CTDeviceDiscoveryActivity");
                    Log.d("DMR_CRNT_PLAYSTATE", "" + duration);
                    ScanningHandler mScanHandler = ScanningHandler.getInstance();
                    SceneObject mSceneObj = mScanHandler.getSceneObjectFromCentralRepo(nettyData.getRemotedeviceIp());
                    if (mSceneObj != null)
                        mSceneObj.setPlaystatus(duration);
                    LSSDPNodeDB mLssdpNodeDb = LSSDPNodeDB.getInstance();
                    final LSSDPNodes mNode = mLssdpNodeDb.getTheNodeBasedOnTheIpAddress(nettyData.getRemotedeviceIp());
                    if (mNode != null) {
                        mNode.setmPlayStatus(duration);
                    }
                    mLssdpNodeDb.renewLSSDPNodeDataWithNewNode(mNode);
                } catch (Exception e) {
                    LibreLogger.d(this, "Exception occurred in newMessageReceived");
                }
            }
            /* Sending Whenver a Command 3 When i got Zero From Device */
            if (packet.getCommand() == 0) {
                new LUCIControl(nettyData.getRemotedeviceIp()).sendAsynchronousCommandSpecificPlaces();
            }


        }

        @Subscribe
        public void deviceGotRemoved(String ipaddress) {
            LibreLogger.d(this, "deviceGotRemoved, ip " + ipaddress);
            GoAndRemoveTheDevice(ipaddress);
        }


    };


    private synchronized void checkForUPnPReinitialization() {
        String localIpAddress = AppUtils.INSTANCE.getWifiIp(this);
        try {
            LibreLogger.d(this, "checkForUPnPReinitialization Phone ip " + localIpAddress + ", LOCAL_IP " + LibreApplication.LOCAL_IP);
            /*if its an Empty String, Contains will pass  , because contains checking >=0 ; if its Not Empty or Not equal we need ot reinitate it*/
            if (localIpAddress != null
                    && (LibreApplication.LOCAL_IP.isEmpty() || !localIpAddress.equalsIgnoreCase(LibreApplication.LOCAL_IP))) {

                LibreLogger.d(this, "Phone ip and upnp ip is different");
                LibreApplication.LOCAL_IP = localIpAddress;
                binder.renew();
                MusicServer ms = MusicServer.getMusicServer();
                ms.clearMediaServer();
                UpnpDeviceManager.getInstance().clearMaps();

                LibreApplication.PLAYBACK_HELPER_MAP = new HashMap<String, PlaybackHelper>();
                binder.getRegistry().addDevice(ms.getMediaServer().getDevice());
                LibreApplication.LOCAL_UDN = ms.getMediaServer().getDevice().getIdentity().getUdn().toString();

                startService(new Intent(CoreUpnpService.this, LoadLocalContentService.class));
//              ((LibreApplication) getApplication()).setControlPoint(binder.getControlPoint());
            }
        } catch (Exception ee) {
            ee.printStackTrace();
            LibreLogger.d(this, "Exception " + ee.getMessage());
        }
    }
}
