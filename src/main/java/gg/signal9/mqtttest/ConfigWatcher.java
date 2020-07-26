package gg.signal9.mqtttest;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.nio.file.*;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.Session;
import javax.xml.bind.DatatypeConverter;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import gg.signal9.mqtttest.models.*;

@ApplicationScoped
public class ConfigWatcher implements Runnable {
    
    @Inject
    SensorService sensorService;

    @ConfigProperty(name = "wrangler.config.root")
    String configRoot;

    @ConfigProperty(name = "wrangler.config.baseurl")
    String configBaseUrl;

    @ConfigProperty(name = "wrangler.firmware.baseurl")
    String configFwBaseUrl;

    @Inject
    ConnectionFactory connectionFactory;
 
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private Logger logger = Logger.getLogger("");

    void onStart(@Observes StartupEvent ev) {
        scheduler.scheduleWithFixedDelay(this, 0L, 10L, TimeUnit.SECONDS);
    }

    void onStop(@Observes ShutdownEvent ev) {
        scheduler.shutdown();
    }

    public String get_config_value(Path path, String value) {
        try{
            Path targetPath = path.resolve(value);
            String retVal = Files.readString(targetPath).trim();
            return retVal;
        } catch (IOException ex){
            logger.info("Got IOException for value" + value);
            return "";
        } 
        
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

    public void update_from_path(Path path, Sensor sensor) {
        SensorConfig newConfig = new SensorConfig();
        newConfig.hostname = get_config_value(path, "hostname");
        newConfig.mqttBroker = get_config_value(path, "mqtt_broker");
        newConfig.ntpServer = get_config_value(path, "ntp_server");
        newConfig.wifiSsid = get_config_value(path, "wifi_ssid");
        newConfig.wifiPassword = get_config_value(path, "wifi_password");
        newConfig.zone = get_config_value(path, "zone");
        newConfig.sensorName = get_config_value(path, "sensor_name");
        newConfig.statusTopic = get_config_value(path, "status_topic");
        newConfig.dataTopic = get_config_value(path, "data_topic");
        newConfig.commandTopic = get_config_value(path, "command_topic");
        newConfig.mqttPort = Integer.parseInt(get_config_value(path, "mqtt_port"));
        newConfig.waterEnabled = Boolean.parseBoolean(get_config_value(path, "water_enabled"));
        newConfig.mqttTls = Boolean.parseBoolean(get_config_value(path, "mqtt_tls"));
        newConfig.desiredFirmware = get_config_value(path, "desired_firmware");
        logger.info("Updating config for chipID " + sensor.chipId);
        sensor.config = newConfig;
    }

    @Override
    public void run() {
        Path configRootPath = Paths.get(configRoot);

        logger.info("Starting config filesystem sync");
        logger.info("Config Path " + configRoot);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(configRootPath)) {
            for (Path file: stream) {
                for (Sensor candidate : sensorService.fleet.sensors){
                    if (candidate.chipId == Integer.parseInt(file.getFileName().toString())){
                        update_from_path(file, candidate);
                    }
                }
            }
        } catch (IOException | DirectoryIteratorException x) {
            // IOException can never be thrown by the iteration.
            // In this snippet, it can only be thrown by newDirectoryStream.
            System.err.println(x);
        }
        logger.info("Ensuring configuration states");
        try (JMSContext context = connectionFactory.createContext(Session.AUTO_ACKNOWLEDGE)){
            context.setClientID("wrangler-config-producer");
            JMSProducer producer = context.createProducer();
            for (Sensor candidate : sensorService.fleet.sensors){
                if (candidate.status.status == "reconfigure"){
                    continue;
                }
                if (candidate.config == null){
                    continue;
                }
                if (get_config_sha(candidate.config).compareTo(candidate.status.configHash) != 0) {
                    logger.info("Reconfiguring Sensor " + candidate.chipId);
                    logger.info("Desired: " + get_config_sha(candidate.config) + " Actual: " + candidate.status.configHash);
                    candidate.status.status = "reconfigure";
                    String realTopic = candidate.status.commandTopic.replace('/', '.');
                    String renderedUrl = String.format("%s/sensor/%d/config", configBaseUrl, candidate.chipId);
                    String commandMessage = String.format("{\"chip_id\": %d, \"command\": \"get_config\", \"config_uri\": \"%s\"}", candidate.chipId, renderedUrl);
                    logger.info("Message " + commandMessage + " To: " + realTopic);
                    producer.send(context.createTopic(realTopic), commandMessage);
                    
                }
                if (!candidate.config.desiredFirmware.contains(candidate.status.fwVersion)){
                    if (candidate.status.status == "upgrade"){
                        continue;
                    }
                    logger.info("Updating Sensor " + candidate.chipId);
                    candidate.status.status = "upgrade";
                    String realTopic = candidate.status.commandTopic.replace('/', '.');
                    String renderedUrl = String.format("%s/%s", configFwBaseUrl, candidate.config.desiredFirmware);
                    String commandMessage = String.format("{\"chip_id\": %d, \"command\": \"get_firmware\", \"update_uri\": \"%s\"}", candidate.chipId, renderedUrl);
                    logger.info("Message " + commandMessage + " To: " + realTopic);
                    producer.send(context.createTopic(realTopic), commandMessage);
                }
            }
        } catch (Exception ex) {
            logger.info("Got Exception " + ex.toString());
        }
    }
}