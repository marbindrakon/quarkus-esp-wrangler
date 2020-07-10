package gg.signal9.mqtttest;

import java.util.*;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SensorService {
    public class Sensor {
        public int chipId;
        public String status;
        public String fwVersion;
        public boolean waterEnabled;
        public String configHash;
    public String toString(){
            return "Sensor[ID: "+ this.chipId + " Status: " + this.status + " FW: " + this.fwVersion + "]";
        }
    }
    public class SensorData {
        public float temperature;
        public float humidity;
    }
    public class SensorZone {
        public LinkedList<SensorData> readings = new LinkedList<SensorData>();
        public String zoneName;
    }
    public class SensorFleet{
        public LinkedList<Sensor> sensors = new LinkedList<Sensor>();
        public LinkedList<SensorZone> zones = new LinkedList<SensorZone>();
    }

    public SensorFleet fleet = new SensorFleet();
}