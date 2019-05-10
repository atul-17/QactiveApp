
package com.libre.Ls9Sac;

import java.util.HashMap;
import java.util.Map;

public class SetupStats {

    private Boolean historicallySucceeded;
    private Integer numCheckConnectivity;
    private Integer numConnectWifi;
    private Integer numConnectedWifiNotSaved;
    private Integer numInitialEurekaInfo;
    private Integer numObtainIp;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * 
     * @return
     *     The historicallySucceeded
     */
    public Boolean getHistoricallySucceeded() {
        return historicallySucceeded;
    }

    /**
     * 
     * @param historicallySucceeded
     *     The historically_succeeded
     */
    public void setHistoricallySucceeded(Boolean historicallySucceeded) {
        this.historicallySucceeded = historicallySucceeded;
    }

    /**
     * 
     * @return
     *     The numCheckConnectivity
     */
    public Integer getNumCheckConnectivity() {
        return numCheckConnectivity;
    }

    /**
     * 
     * @param numCheckConnectivity
     *     The num_check_connectivity
     */
    public void setNumCheckConnectivity(Integer numCheckConnectivity) {
        this.numCheckConnectivity = numCheckConnectivity;
    }

    /**
     * 
     * @return
     *     The numConnectWifi
     */
    public Integer getNumConnectWifi() {
        return numConnectWifi;
    }

    /**
     * 
     * @param numConnectWifi
     *     The num_connect_wifi
     */
    public void setNumConnectWifi(Integer numConnectWifi) {
        this.numConnectWifi = numConnectWifi;
    }

    /**
     * 
     * @return
     *     The numConnectedWifiNotSaved
     */
    public Integer getNumConnectedWifiNotSaved() {
        return numConnectedWifiNotSaved;
    }

    /**
     * 
     * @param numConnectedWifiNotSaved
     *     The num_connected_wifi_not_saved
     */
    public void setNumConnectedWifiNotSaved(Integer numConnectedWifiNotSaved) {
        this.numConnectedWifiNotSaved = numConnectedWifiNotSaved;
    }

    /**
     * 
     * @return
     *     The numInitialEurekaInfo
     */
    public Integer getNumInitialEurekaInfo() {
        return numInitialEurekaInfo;
    }

    /**
     * 
     * @param numInitialEurekaInfo
     *     The num_initial_eureka_info
     */
    public void setNumInitialEurekaInfo(Integer numInitialEurekaInfo) {
        this.numInitialEurekaInfo = numInitialEurekaInfo;
    }

    /**
     * 
     * @return
     *     The numObtainIp
     */
    public Integer getNumObtainIp() {
        return numObtainIp;
    }

    /**
     * 
     * @param numObtainIp
     *     The num_obtain_ip
     */
    public void setNumObtainIp(Integer numObtainIp) {
        this.numObtainIp = numObtainIp;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
