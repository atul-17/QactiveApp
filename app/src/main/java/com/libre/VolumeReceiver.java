package com.libre;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.libre.Scanning.ScanningHandler;
import com.libre.constants.LSSDPCONST;
import com.libre.constants.MIDCONST;
import com.libre.luci.LUCIControl;
import com.libre.nowplaying.NowPlayingActivity;
import com.libre.util.LibreLogger;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by khajan on 28/8/15.
 */
public class VolumeReceiver extends BroadcastReceiver {
    private LibreApplication m_myApp;
    private String ipAddress;
    private ScanningHandler mScanHandler = ScanningHandler.getInstance();
    private SceneObject currentSceneObject;


    public VolumeReceiver() {

    }

    public String getIpAddress(){
        return this.ipAddress;
    }
    public void setIpAddress(String mipAddress){
        this.ipAddress = mipAddress;
        this.currentSceneObject = mScanHandler.getSceneObjectFromCentralRepo(ipAddress);
    }

    public VolumeReceiver(String ipAddress) {
        this.ipAddress = ipAddress;
        this.currentSceneObject = mScanHandler.getSceneObjectFromCentralRepo(ipAddress);

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = "android.media.VOLUME_CHANGED_ACTION";
        m_myApp = (LibreApplication) context.getApplicationContext();
        if (intent.getAction().equalsIgnoreCase(action)) {

         if ( NowPlayingActivity.isPhoneCallbeingReceived) {

             LibreLogger.d(this,"Volume up event is ignored");
             return;

         }
            ConcurrentHashMap<String, SceneObject> centralSceneObjectRepo = mScanHandler.getSceneObjectFromCentralRepo();

                  /*which means no master present hence all devices are free so need to do anything*/
            if (centralSceneObjectRepo == null || centralSceneObjectRepo.size() == 0) {
                LibreLogger.d(this, "No master present");
                return;
            }

            Log.d("VolumeReceiver", "Volume broadcast receiver got called" + ipAddress);

            /*sending volume to all master devices*/
            String masterIPAddress = ipAddress;
            //for (String masterIPAddress : centralSceneObjectRepo.keySet()) {
                {    /*have to think */
                ipAddress = masterIPAddress;
              /*  if (masterIPAddress == null)
                    continue;
*/
                SceneObject currentSceneObject = mScanHandler.getSceneObjectFromCentralRepo(masterIPAddress);
  /*              if (currentSceneObject == null)
                    continue;
*/
                int volume = (Integer) intent.getExtras().get("android.media.EXTRA_VOLUME_STREAM_VALUE");
                LUCIControl luciControl = new LUCIControl(masterIPAddress);
                switch (volume) {
                    case 0:
                        luciControl.SendCommand(MIDCONST.VOLUME_CONTROL, Integer.toString(0), LSSDPCONST.LUCI_SET);
                        break;
                    case 1:
                        luciControl.SendCommand(MIDCONST.VOLUME_CONTROL, Integer.toString(10), LSSDPCONST.LUCI_SET);
                        break;

                    case 2:
                        luciControl.SendCommand(MIDCONST.VOLUME_CONTROL, Integer.toString(20), LSSDPCONST.LUCI_SET);
                        break;

                    case 3:
                        luciControl.SendCommand(MIDCONST.VOLUME_CONTROL, Integer.toString(30), LSSDPCONST.LUCI_SET);
                        break;
                    case 4:
                        luciControl.SendCommand(MIDCONST.VOLUME_CONTROL, Integer.toString(50), LSSDPCONST.LUCI_SET);
                        break;
                    case 5:
                        luciControl.SendCommand(MIDCONST.VOLUME_CONTROL, Integer.toString(60), LSSDPCONST.LUCI_SET);
                        break;
                    case 6:
                        luciControl.SendCommand(MIDCONST.VOLUME_CONTROL, Integer.toString(80), LSSDPCONST.LUCI_SET);
                        break;
                    case 7:
                        luciControl.SendCommand(MIDCONST.VOLUME_CONTROL, Integer.toString(100), LSSDPCONST.LUCI_SET);
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
