package gg.signal9.mqtttest.models;

import java.util.Date;
import javax.json.bind.annotation.JsonbProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class SensorStatus {
    public SensorStatus(){
        this.lastSeen = new Date();
    }
    @JsonbProperty("chip_id")
    public int chipId;

    @JsonbProperty("zone")
    public String zone;

    @JsonbProperty("sensor_name")
    public String sensorName;

    @JsonbProperty("command_topic")
    public String commandTopic;
    
    @JsonbProperty("message")
    public String status;

    @JsonbProperty("fw_version")
    public String fwVersion;

    @JsonbProperty("water_enabled")
    public boolean waterEnabled;

    @JsonbProperty("config_hash")
    public String configHash;

    @JsonbProperty("last_seen")
    public Date lastSeen;

public String toString(){
        return "Sensor[ID: "+ this.chipId + " Status: " + this.status + " FW: " + this.fwVersion + "]";
    }
}