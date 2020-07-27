package gg.signal9.quarkus_esp_wrangler;

import javax.enterprise.context.ApplicationScoped;

import gg.signal9.quarkus_esp_wrangler.models.*;

@ApplicationScoped
public class SensorService {

    public SensorFleet fleet;
    public SensorService(){
        this.fleet = new SensorFleet();
    }

}