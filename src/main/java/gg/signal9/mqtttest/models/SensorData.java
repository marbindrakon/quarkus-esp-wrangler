package gg.signal9.mqtttest.models;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class SensorData {
    public SensorData(){
        
    }
    public float temperature;
    public float humidity;
}