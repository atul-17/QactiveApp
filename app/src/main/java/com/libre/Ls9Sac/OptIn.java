
package com.libre.Ls9Sac;

import java.util.HashMap;
import java.util.Map;

public class OptIn {

    private Boolean crash;
    private Boolean location;
    private Boolean opencast;
    private Boolean stats;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * 
     * @return
     *     The crash
     */
    public Boolean getCrash() {
        return crash;
    }

    /**
     * 
     * @param crash
     *     The crash
     */
    public void setCrash(Boolean crash) {
        this.crash = crash;
    }

    /**
     * 
     * @return
     *     The location
     */
    public Boolean getLocation() {
        return location;
    }

    /**
     * 
     * @param location
     *     The location
     */
    public void setLocation(Boolean location) {
        this.location = location;
    }

    /**
     * 
     * @return
     *     The opencast
     */
    public Boolean getOpencast() {
        return opencast;
    }

    /**
     * 
     * @param opencast
     *     The opencast
     */
    public void setOpencast(Boolean opencast) {
        this.opencast = opencast;
    }

    /**
     * 
     * @return
     *     The stats
     */
    public Boolean getStats() {
        return stats;
    }

    /**
     * 
     * @param stats
     *     The stats
     */
    public void setStats(Boolean stats) {
        this.stats = stats;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
