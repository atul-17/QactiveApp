package com.libre;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.github.johnpersano.supertoasts.SuperToast;
import com.libre.LErrorHandeling.LibreError;
import com.libre.luci.DiscoverySearchService;
import com.libre.luci.LSSDPNodes;
import com.libre.luci.LUCIPacket;
import com.libre.netty.BusProvider;
import com.libre.netty.LibreDeviceInteractionListner;
import com.libre.netty.NettyData;
import com.libre.util.LibreLogger;
import com.squareup.otto.Subscribe;

/**
 * Created by praveena on 8/4/15.
 */
public class DeviceDiscoveryActivityForFragmentManager extends FragmentActivity {

    private static final String TAG = DiscoverySearchService.class.getName();
    LibreDeviceInteractionListner libreDeviceInteractionListner;
    Object busEventListener = new Object() {
        @Subscribe
        public void newDeviceFound(final LSSDPNodes nodes) {

             /* This below if loop is introduced to handle the case where Device state from the DUT could be Null sometimes
            * Ideally the device state should not be null but we are just handling it to make sure it will not result in any crash!
            *
            * */
            if (nodes==null||nodes.getDeviceState()==null)
            {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(DeviceDiscoveryActivityForFragmentManager.this,getString(R.string.deviceStateNull)+ nodes.getDeviceState(),Toast.LENGTH_SHORT).show();                    }
                });
                return;
            }
            else if(libreDeviceInteractionListner!=null)
            {
                libreDeviceInteractionListner.newDeviceFound(nodes);
            }
        }

        @Subscribe
        public void newMessageRecieved(NettyData nettyData){

            if(libreDeviceInteractionListner!=null)
            {
                LUCIPacket packet=new LUCIPacket(nettyData.getMessage());
                libreDeviceInteractionListner.messageRecieved(nettyData);
            }
        }

        @Subscribe
        public void deviceGotRemoved(String ipaddress){

            if(libreDeviceInteractionListner!=null)
            {
                libreDeviceInteractionListner.deviceGotRemoved(ipaddress);
            }
        }


        @Subscribe
        public void libreErrorReceived(LibreError libreError) {
            showErrorMessage(libreError);
        }
    };



    /*this method is to show error in whole application*/
    public void showErrorMessage(final LibreError message) {
        try {


            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    LibreLogger.d(this, "Logging the error" + message);
                    SuperToast superToast = new SuperToast(DeviceDiscoveryActivityForFragmentManager.this);
                    superToast.setDuration(SuperToast.Duration.LONG);
                    superToast.setText("" + message);
                    superToast.setAnimations(SuperToast.Animations.FLYIN);
                    superToast.setIcon(SuperToast.Icon.Dark.INFO, SuperToast.IconPosition.LEFT);
                    superToast.show();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);


    }

    public  void registerForDeviceEvents(LibreDeviceInteractionListner libreListner){
        this.libreDeviceInteractionListner=libreListner;
    }

    public  void unRegisterForDeviceEvents(){
        this.libreDeviceInteractionListner=null;
    }



    @Override
    protected void onResume() {
        super.onResume();
        try {
            BusProvider.getInstance().register(busEventListener);
        }catch (Exception e){

        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            BusProvider.getInstance().unregister(busEventListener);
        }
        catch (Exception e){

        }
    }
    
}
