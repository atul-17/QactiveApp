
package com.libre.qactive.Ls9Sac;

import java.util.HashMap;
import java.util.Map;


public class ClosedCaption {

    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
