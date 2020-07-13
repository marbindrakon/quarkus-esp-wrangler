package gg.signal9.mqtttest.models;

import java.util.LinkedList;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class SensorFleet{
    public LinkedList<Sensor> sensors;
    public LinkedList<SensorZone> zones;
    
    public SensorFleet(){
        this.sensors = new LinkedList<Sensor>();
        this.zones = new LinkedList<SensorZone>();
    }
}