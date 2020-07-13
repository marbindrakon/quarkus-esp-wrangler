package gg.signal9.mqtttest;

import javax.enterprise.context.ApplicationScoped;

import gg.signal9.mqtttest.models.*;

@ApplicationScoped
public class SensorService {

    public SensorFleet fleet;
    public SensorService(){
        this.fleet = new SensorFleet();
    }

}