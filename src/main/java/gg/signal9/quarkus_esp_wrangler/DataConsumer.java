package gg.signal9.quarkus_esp_wrangler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import gg.signal9.quarkus_esp_wrangler.models.*;

@ApplicationScoped
public class DataConsumer implements Runnable,MqttCallback {

    @Inject
    SensorService sensorService;

    @Inject
    @RegistryType(type = MetricRegistry.Type.APPLICATION)
    MetricRegistry registry;

    @ConfigProperty(name = "wrangler.broker.url")
    String mqttBrokerUrl;

    @ConfigProperty(name = "wrangler.broker.clientIdPrefix")
    String mqttClientIdPrefix;

    String mqttClientId = mqttClientIdPrefix + "-data-consumer";
    MemoryPersistence mqttPersistence = new MemoryPersistence();

    private final ExecutorService scheduler = Executors.newSingleThreadExecutor();
    private Logger logger = Logger.getLogger("");

    void onStart(@Observes StartupEvent ev) {
        scheduler.submit(this);
    }

    void onStop(@Observes ShutdownEvent ev) {
        scheduler.shutdown();
    }
    private void update_sensor_reading(SensorData newData){
        for (Sensor candidate : sensorService.fleet.sensors) {
            if (candidate.chipId == newData.chipId){
                candidate.currentReading = newData;
                return;
            }
        }
    }

    private void expire_stale_readings(){
        Date thresholdTime = Date.from(ZonedDateTime.now().minusMinutes(5).toInstant());
        for (Sensor candidate : sensorService.fleet.sensors) {
            if (candidate.status.lastSeen.before(thresholdTime)){
                logger.info(String.format("Sensor %d has not reported data in over 5 minutes", candidate.chipId));
                candidate.currentReading = null;
                candidate.status.status = "offline";
            }
        }
    }

    @Override
    public void connectionLost(Throwable thrwbl) {
        logger.info("DataConsumer lost connection");
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken imdt) {

    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        Jsonb jsonb = JsonbBuilder.create();
        String msgBody = message.toString();
        try {
            SensorData newData = jsonb.fromJson(msgBody, SensorData.class);
            update_sensor_reading(newData);
        } catch (Exception e) {
            logger.info("Caught Exception running getBody");
            logger.info(e.getMessage());
	    logger.info("Data: " + msgBody);
        }
        expire_stale_readings();
    }

    @Override
    public void run() {
        try {
            MqttClient mqttClient = new MqttClient(mqttBrokerUrl, mqttClientId, mqttPersistence);
            mqttClient.setCallback(this);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            mqttClient.connect(connOpts);
            mqttClient.subscribe("sensors.*.data", 2);
        } catch (Exception ex) {
            logger.info("Got exception in DataConsumer");
        }

    }
}
