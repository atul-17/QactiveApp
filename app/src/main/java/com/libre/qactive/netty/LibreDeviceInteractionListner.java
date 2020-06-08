package com.libre.qactive.netty;

import com.libre.qactive.luci.LSSDPNodes;

/**
 * Created by praveena on 7/27/15.
 */
public interface  LibreDeviceInteractionListner {

    /**
     * This function is called when device discovery is called by clearing the cache.
     * Framework will be calling Msearch request and will return each of discovered device in the newDeviceFound callback
     */
    void    deviceDiscoveryAfterClearingTheCacheStarted();

    /**
     This function will be called  by the framework, when a device that is not in our device cache.
     */
    void    newDeviceFound(LSSDPNodes node);


    /**
     * This usally happens when the device notification is not recieved (in Luci), infering that device to e removed.
     */
    void    deviceGotRemoved(String ipaddress);



    void  messageRecieved(NettyData packet);

}
