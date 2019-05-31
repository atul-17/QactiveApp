package com.libre;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import com.libre.alexa.MicExceptionListener;
import com.libre.luci.LSSDPNodes;
import com.libre.luci.LUCIPacket;
import com.libre.netty.BusProvider;
import com.libre.netty.LibreDeviceInteractionListner;
import com.libre.netty.NettyData;
import com.squareup.otto.Subscribe;

/**
 * Created by praveena on 8/4/15.
 */
public class DeviceDiscoveryFragment extends Fragment implements MicExceptionListener {

    LibreDeviceInteractionListner libreDeviceInteractionListner;
    MicExceptionListener fragmentMicExceptionActivityListener;
    Object busEventListener = new Object() {
        @Subscribe
        public void newDeviceFound(final LSSDPNodes nodes) {

            /* This below if loop is introduced to handle the case where Device state from the DUT could be Null sometimes
            * Ideally the device state should not be null but we are just handling it to make sure it will not result in any crash!
            *
            * */
            if (nodes==null||nodes.getDeviceState()==null)
            {

                if (getActivity()!=null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(), getString(R.string.deviceStateNull) + nodes.getDeviceState(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
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

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public  void registerForDeviceEvents(LibreDeviceInteractionListner libreListner){
        this.libreDeviceInteractionListner=libreListner;
    }

    public  void unRegisterForDeviceEvents(){
        this.libreDeviceInteractionListner=null;
    }




    @Override
    public void onResume() {
        super.onResume();
        try {
            BusProvider.getInstance().register(busEventListener);
            LibreApplication libreApplication = (LibreApplication) getActivity().getApplication();
            libreApplication.registerForMicException(this);
        }catch (Exception e){

        }
    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            BusProvider.getInstance().unregister(busEventListener);
        }
        catch (Exception e){

        }
    }

    @Override
    public void micExceptionCaught(Exception e) {
        if (fragmentMicExceptionActivityListener!=null)
            fragmentMicExceptionActivityListener.micExceptionCaught(e);
    }

    public void registerFragmentMicExceptionActivityListener(MicExceptionListener fragmentMicExceptionActivityListener){
        this.fragmentMicExceptionActivityListener = fragmentMicExceptionActivityListener;
    }

    public void unregisterFragmentMicExceptionActivityListener(){
        fragmentMicExceptionActivityListener = null;
    }
}
