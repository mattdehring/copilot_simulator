package com.rjginc.copilot.copilot_simulator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * The IP address of The Hub™
 */
@Component
public class HubAddress {

    public static Integer currentSchemaVersion = 1;
    private Integer schemaVersion = 1;

    private String hubInetAddress;
    private Integer hubPort;

    @Autowired
    private ObjectMapper mapper;

    public HubAddress() {
    }

    public HubAddress(String hubInetAddress, Integer hubPort) {
        super();
        this.hubInetAddress = hubInetAddress;
        this.hubPort = hubPort;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }
   
    public String getHubInetAddress() {
        return hubInetAddress;
    }

    public void setHubInetAddress(String hubInetAddress) {
        this.hubInetAddress = hubInetAddress;
    }

    public Integer getHubPort() {
        return hubPort;
    }

    public void setHubPort(Integer hubPort) {
        this.hubPort = hubPort;
    }

    @JsonIgnore
    public boolean isEnabled() {
        return hubInetAddress != null && !hubInetAddress.isEmpty();
    }

}
