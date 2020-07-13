package gg.signal9.mqtttest.models;

import java.util.LinkedList;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class SensorZone {
    public LinkedList<SensorData> readings = new LinkedList<SensorData>();
    public String zoneName;
}