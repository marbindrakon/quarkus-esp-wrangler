package gg.signal9.quarkus_esp_wrangler;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.xml.bind.DatatypeConverter;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.api.model.ConfigMap;

import gg.signal9.quarkus_esp_wrangler.models.*;

@ApplicationScoped
public class ConfigWatcher implements Runnable {
    
    @Inject
    SensorService sensorService;

    @Inject
    KubernetesClient kubeClient;

    @ConfigProperty(name = "wrangler.config.configmap")
    String configMapName;

    @ConfigProperty(name = "wrangler.config.baseurl")
    String configBaseUrl;

    @ConfigProperty(name = "wrangler.firmware.baseurl")
    String configFwBaseUrl;

    @ConfigProperty(name = "wrangler.broker.url")
    String mqttBrokerUrl;

    @ConfigProperty(name = "wrangler.broker.clientIdPrefix")
    String mqttClientIdPrefix;

    MemoryPersistence mqttPersistence = new MemoryPersistence();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private Logger logger = Logger.getLogger("");
    private Map<String,String> configData;

    void onStart(@Observes StartupEvent ev) {
        scheduler.scheduleWithFixedDelay(this, 0L, 10L, TimeUnit.SECONDS);
    }

    void onStop(@Observes ShutdownEvent ev) {
        scheduler.shutdown();
    }

    public String get_config_value(int chipId, String value) {

        String defaultKey = String.format("default_%s", value);
        String chipKey = String.format("%d_%s", chipId, value);
        String defaultValue = this.configData.getOrDefault(defaultKey, "");
        return this.configData.getOrDefault(chipKey, defaultValue);
    }

    public String get_config_sha(SensorConfig subjectConfig) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            Jsonb jsonb = JsonbBuilder.create();
            String jsonString = jsonb.toJson(subjectConfig);
            byte[] hashBytes = digest.digest(jsonString.getBytes());
            return DatatypeConverter.printHexBinary(hashBytes);
		} catch (NoSuchAlgorithmException e) {
            return "";
		}
    }

    public void update_config(Sensor sensor) {
        SensorConfig newConfig = new SensorConfig();
        int chipId = sensor.chipId;
        newConfig.hostname = get_config_value(chipId, "hostname");
        newConfig.mqttBroker = get_config_value(chipId, "mqtt_broker");
        newConfig.ntpServer = get_config_value(chipId, "ntp_server");
        newConfig.wifiSsid = get_config_value(chipId, "wifi_ssid");
        newConfig.wifiPassword = get_config_value(chipId, "wifi_password");
        newConfig.zone = get_config_value(chipId, "zone");
        newConfig.sensorName = get_config_value(chipId, "sensor_name");
        newConfig.statusTopic = get_config_value(chipId, "status_topic");
        newConfig.dataTopic = get_config_value(chipId, "data_topic");
        newConfig.commandTopic = get_config_value(chipId, "command_topic");
        newConfig.mqttPort = Integer.parseInt(get_config_value(chipId, "mqtt_port"));
        newConfig.waterEnabled = Boolean.parseBoolean(get_config_value(chipId, "water_enabled"));
        newConfig.mqttTls = Boolean.parseBoolean(get_config_value(chipId, "mqtt_tls"));
        newConfig.desiredFirmware = get_config_value(chipId, "desired_firmware");
        newConfig.area = get_config_value(chipId, "area");
        logger.info("Updating config for chipID " + sensor.chipId);
        sensor.config = newConfig;
    }

    private void read_config() {
        logger.info("Reading ConfigMap");
        ConfigMap sensorConfig = null;
        try {
            sensorConfig = kubeClient.configMaps().withName(configMapName).get();
        } catch (KubernetesClientException ex){
            logger.info(ex.toString());
            return;
        }
        if (sensorConfig == null) {
            logger.info("ConfigMap not found!");
        } else {
            this.configData = sensorConfig.getData();
            if (configData != null){
                for (Sensor sensor : sensorService.fleet.sensors){
                    update_config(sensor);
                }    
            } else {
                logger.info("ConfigMap data was null!");
            }   
        }
    }

    @Override
    public void run() {
        read_config();
        ensure_config();
    }

    private void ensure_config() {
        logger.info("Ensuring configuration states");
        try {
            String mqttClientId = "default-config";
            if (mqttClientIdPrefix != null) {
                mqttClientId = mqttClientIdPrefix.concat("-config");
            }
            MqttClient mqttClient = new MqttClient(mqttBrokerUrl, mqttClientId, mqttPersistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            mqttClient.connect(connOpts);
            for (Sensor candidate : sensorService.fleet.sensors){
                if (candidate.status.status == "reconfigure" || candidate.status.status == "upgrade"){
                    continue;
                }
                if (candidate.config == null){
                    continue;
                }
                if (get_config_sha(candidate.config).compareTo(candidate.status.configHash) != 0) {
                    logger.info("Reconfiguring Sensor " + candidate.chipId);
                    logger.info("Desired: " + get_config_sha(candidate.config) + " Actual: " + candidate.status.configHash);
                    candidate.status.status = "reconfigure";
                    String realTopic = candidate.status.commandTopic;
                    String renderedUrl = String.format("%s/sensor/%d/config", configBaseUrl, candidate.chipId);
                    String commandMessage = String.format("{\"chip_id\": %d, \"command\": \"get_config\", \"config_uri\": \"%s\"}", candidate.chipId, renderedUrl);
                    logger.info("Message " + commandMessage + " To: " + realTopic);
                    MqttMessage message = new MqttMessage(commandMessage.getBytes());
                    message.setQos(2);
                    mqttClient.publish(realTopic, message);
                }
                if (!candidate.config.desiredFirmware.contains(candidate.status.fwVersion)){
                    if (candidate.status.status == "upgrade" || candidate.status.status == "reconfigure"){
                        continue;
                    }
                    logger.info("Updating Sensor " + candidate.chipId);
                    candidate.status.status = "upgrade";
                    String realTopic = candidate.status.commandTopic;
                    String renderedUrl = String.format("%s/%s", configFwBaseUrl, candidate.config.desiredFirmware);
                    String commandMessage = String.format("{\"chip_id\": %d, \"command\": \"get_firmware\", \"update_uri\": \"%s\"}", candidate.chipId, renderedUrl);
                    logger.info("Message " + commandMessage + " To: " + realTopic);
                    MqttMessage message = new MqttMessage(commandMessage.getBytes());
                    message.setQos(2);
                    mqttClient.publish(realTopic, message);
                }
            }
            mqttClient.disconnect();
        } catch (Exception ex) {
            logger.info("Got Exception " + ex.toString());
        }
    }
}
