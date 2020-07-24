package gg.signal9.mqtttest.models;

import javax.json.bind.annotation.JsonbProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Sensor {
    public Sensor(){
    }

    @JsonbProperty("chip_id")
    public int chipId;

    @JsonbProperty("status")
    public SensorStatus status;

    @JsonbProperty("config")
    public SensorConfig config;

    @JsonbProperty("current_reading")
    public SensorData currentReading;

public String toString(){
        return "Sensor[ID: "+ this.chipId + "]";
    }
}