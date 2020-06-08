
package com.libre.qactive.Ls9Sac;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class EurekaJSON implements Serializable {

    private String bssid;
    private String buildVersion;
    private String castBuildRevision;
    private ClosedCaption closedCaption;
    private Boolean connected;
    private Boolean ethernetConnected;
    private Boolean hasUpdate;
    private String hotspotBssid;
    private String ipAddress;
    private String locale;
    private Location location;
    private String macAddress;
    private String name;
    private OptIn optIn;
    private String publicKey;
    private String releaseTrack;
    private Integer setupState;
    private SetupStats setupStats;
    private String ssdpUdn;
    private String ssid;
    private Integer timeFormat;
    private Boolean tosAccepted;
    private String umaClientId;
    private Double uptime;
    private Integer version;
    private Boolean wpaConfigured;
    private Integer wpaId;
    private Integer wpaState;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * 
     * @return
     *     The bssid
     */
    public String getBssid() {
        return bssid;
    }

    /**
     * 
     * @param bssid
     *     The bssid
     */
    public void setBssid(String bssid) {
        this.bssid = bssid;
    }

    /**
     * 
     * @return
     *     The buildVersion
     */
    public String getBuildVersion() {
        return buildVersion;
    }

    /**
     * 
     * @param buildVersion
     *     The build_version
     */
    public void setBuildVersion(String buildVersion) {
        this.buildVersion = buildVersion;
    }

    /**
     * 
     * @return
     *     The castBuildRevision
     */
    public String getCastBuildRevision() {
        return castBuildRevision;
    }

    /**
     * 
     * @param castBuildRevision
     *     The cast_build_revision
     */
    public void setCastBuildRevision(String castBuildRevision) {
        this.castBuildRevision = castBuildRevision;
    }

    /**
     * 
     * @return
     *     The closedCaption
     */
    public ClosedCaption getClosedCaption() {
        return closedCaption;
    }

    /**
     * 
     * @param closedCaption
     *     The closed_caption
     */
    public void setClosedCaption(ClosedCaption closedCaption) {
        this.closedCaption = closedCaption;
    }

    /**
     * 
     * @return
     *     The connected
     */
    public Boolean getConnected() {
        return connected;
    }

    /**
     * 
     * @param connected
     *     The connected
     */
    public void setConnected(Boolean connected) {
        this.connected = connected;
    }

    /**
     * 
     * @return
     *     The ethernetConnected
     */
    public Boolean getEthernetConnected() {
        return ethernetConnected;
    }

    /**
     * 
     * @param ethernetConnected
     *     The ethernet_connected
     */
    public void setEthernetConnected(Boolean ethernetConnected) {
        this.ethernetConnected = ethernetConnected;
    }

    /**
     * 
     * @return
     *     The hasUpdate
     */
    public Boolean getHasUpdate() {
        return hasUpdate;
    }

    /**
     * 
     * @param hasUpdate
     *     The has_update
     */
    public void setHasUpdate(Boolean hasUpdate) {
        this.hasUpdate = hasUpdate;
    }

    /**
     * 
     * @return
     *     The hotspotBssid
     */
    public String getHotspotBssid() {
        return hotspotBssid;
    }

    /**
     * 
     * @param hotspotBssid
     *     The hotspot_bssid
     */
    public void setHotspotBssid(String hotspotBssid) {
        this.hotspotBssid = hotspotBssid;
    }

    /**
     * 
     * @return
     *     The ipAddress
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * 
     * @param ipAddress
     *     The ip_address
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * 
     * @return
     *     The locale
     */
    public String getLocale() {
        return locale;
    }

    /**
     * 
     * @param locale
     *     The locale
     */
    public void setLocale(String locale) {
        this.locale = locale;
    }

    /**
     * 
     * @return
     *     The location
     */
    public Location getLocation() {
        return location;
    }

    /**
     * 
     * @param location
     *     The location
     */
    public void setLocation(Location location) {
        this.location = location;
    }

    /**
     * 
     * @return
     *     The macAddress
     */
    public String getMacAddress() {
        return macAddress;
    }

    /**
     * 
     * @param macAddress
     *     The mac_address
     */
    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    /**
     * 
     * @return
     *     The name
     */
    public String getName() {
        return name;
    }

    /**
     * 
     * @param name
     *     The name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 
     * @return
     *     The optIn
     */
    public OptIn getOptIn() {
        return optIn;
    }

    /**
     * 
     * @param optIn
     *     The opt_in
     */
    public void setOptIn(OptIn optIn) {
        this.optIn = optIn;
    }

    /**
     * 
     * @return
     *     The publicKey
     */
    public String getPublicKey() {
        return publicKey;
    }

    /**
     * 
     * @param publicKey
     *     The public_key
     */
    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    /**
     * 
     * @return
     *     The releaseTrack
     */
    public String getReleaseTrack() {
        return releaseTrack;
    }

    /**
     * 
     * @param releaseTrack
     *     The release_track
     */
    public void setReleaseTrack(String releaseTrack) {
        this.releaseTrack = releaseTrack;
    }

    /**
     * 
     * @return
     *     The setupState
     */
    public Integer getSetupState() {
        return setupState;
    }

    /**
     * 
     * @param setupState
     *     The setup_state
     */
    public void setSetupState(Integer setupState) {
        this.setupState = setupState;
    }

    /**
     * 
     * @return
     *     The setupStats
     */
    public SetupStats getSetupStats() {
        return setupStats;
    }

    /**
     * 
     * @param setupStats
     *     The setup_stats
     */
    public void setSetupStats(SetupStats setupStats) {
        this.setupStats = setupStats;
    }

    /**
     * 
     * @return
     *     The ssdpUdn
     */
    public String getSsdpUdn() {
        return ssdpUdn;
    }

    /**
     * 
     * @param ssdpUdn
     *     The ssdp_udn
     */
    public void setSsdpUdn(String ssdpUdn) {
        this.ssdpUdn = ssdpUdn;
    }

    /**
     * 
     * @return
     *     The ssid
     */
    public String getSsid() {
        return ssid;
    }

    /**
     * 
     * @param ssid
     *     The ssid
     */
    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    /**
     * 
     * @return
     *     The timeFormat
     */
    public Integer getTimeFormat() {
        return timeFormat;
    }

    /**
     * 
     * @param timeFormat
     *     The time_format
     */
    public void setTimeFormat(Integer timeFormat) {
        this.timeFormat = timeFormat;
    }

    /**
     * 
     * @return
     *     The tosAccepted
     */
    public Boolean getTosAccepted() {
        return tosAccepted;
    }

    /**
     * 
     * @param tosAccepted
     *     The tos_accepted
     */
    public void setTosAccepted(Boolean tosAccepted) {
        this.tosAccepted = tosAccepted;
    }

    /**
     * 
     * @return
     *     The umaClientId
     */
    public String getUmaClientId() {
        return umaClientId;
    }

    /**
     * 
     * @param umaClientId
     *     The uma_client_id
     */
    public void setUmaClientId(String umaClientId) {
        this.umaClientId = umaClientId;
    }

    /**
     * 
     * @return
     *     The uptime
     */
    public Double getUptime() {
        return uptime;
    }

    /**
     * 
     * @param uptime
     *     The uptime
     */
    public void setUptime(Double uptime) {
        this.uptime = uptime;
    }

    /**
     * 
     * @return
     *     The version
     */
    public Integer getVersion() {
        return version;
    }

    /**
     * 
     * @param version
     *     The version
     */
    public void setVersion(Integer version) {
        this.version = version;
    }

    /**
     * 
     * @return
     *     The wpaConfigured
     */
    public Boolean getWpaConfigured() {
        return wpaConfigured;
    }

    /**
     * 
     * @param wpaConfigured
     *     The wpa_configured
     */
    public void setWpaConfigured(Boolean wpaConfigured) {
        this.wpaConfigured = wpaConfigured;
    }

    /**
     * 
     * @return
     *     The wpaId
     */
    public Integer getWpaId() {
        return wpaId;
    }

    /**
     * 
     * @param wpaId
     *     The wpa_id
     */
    public void setWpaId(Integer wpaId) {
        this.wpaId = wpaId;
    }

    /**
     * 
     * @return
     *     The wpaState
     */
    public Integer getWpaState() {
        return wpaState;
    }

    /**
     * 
     * @param wpaState
     *     The wpa_state
     */
    public void setWpaState(Integer wpaState) {
        this.wpaState = wpaState;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
