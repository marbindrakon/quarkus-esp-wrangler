package gg.signal9.quarkus_esp_wrangler.models;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class SensorConfig {
    public SensorConfig(){
        
    }
    @JsonbProperty("water_enabled")
    public boolean waterEnabled;

    @JsonbProperty("mqtt_port")
    public int mqttPort;
    
    @JsonbProperty("hostname")
    public String hostname;
    
    @JsonbProperty("wifi_ssid")
    public String wifiSsid;
    
    @JsonbProperty("wifi_password")
    public String wifiPassword;
    
    @JsonbProperty("mqtt_broker")
    public String mqttBroker;
    
    @JsonbProperty("ntp_server")
    public String ntpServer;
    
    @JsonbProperty("data_topic")
    public String dataTopic;
    
    @JsonbProperty("status_topic")
    public String statusTopic;
    
    @JsonbProperty("command_topic")
    public String commandTopic;
    
    @JsonbProperty("mqtt_tls")
    public boolean mqttTls;

    @JsonbProperty("zone")
    public String zone;

    @JsonbProperty("sensor_name")
    public String sensorName;

    @JsonbTransient
    public String desiredFirmware;
}