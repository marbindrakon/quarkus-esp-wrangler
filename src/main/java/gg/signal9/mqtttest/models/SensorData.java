package gg.signal9.mqtttest.models;

import javax.json.bind.annotation.JsonbProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class SensorData {
    public SensorData(){
        
    }
    
    @JsonbProperty("chip_id")
    public int chipId;

    @JsonbProperty("temperature")
    public float temperature;

    @JsonbProperty("humidity")
    public float humidity;
}