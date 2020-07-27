package gg.signal9.quarkus_esp_wrangler.models;

import java.util.LinkedList;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class SensorFleet{
    public LinkedList<Sensor> sensors;
    
    public SensorFleet(){
        this.sensors = new LinkedList<Sensor>();
    }
}