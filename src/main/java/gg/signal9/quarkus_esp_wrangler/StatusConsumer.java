package gg.signal9.quarkus_esp_wrangler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import gg.signal9.quarkus_esp_wrangler.models.*;

@ApplicationScoped
public class StatusConsumer implements Runnable,MqttCallback {

    @Inject
    SensorService sensorService;

    @ConfigProperty(name = "wrangler.broker.url")
    String mqttBrokerUrl;

    @ConfigProperty(name = "wrangler.broker.clientIdPrefix")
    String mqttClientIdPrefix;

    MemoryPersistence mqttPersistence = new MemoryPersistence();

    private final ExecutorService scheduler = Executors.newSingleThreadExecutor();
    private Logger logger = Logger.getLogger("");

    void onStart(@Observes StartupEvent ev) {
        scheduler.submit(this);
    }

    void onStop(@Observes ShutdownEvent ev) {
        scheduler.shutdown();
    }
    private void update_sensor_status(SensorStatus newStatus){
        for (Sensor candidate : sensorService.fleet.sensors){
            if (candidate.chipId == newStatus.chipId){
                //logger.info("Updating status for chipID " + newSensor.chipId);
                candidate.status = newStatus;
                return;
            }
        }
        logger.info("Saw new chipID " + newStatus.chipId);
        Sensor newSensor = new Sensor();
        newSensor.chipId = newStatus.chipId;
        newSensor.status = newStatus;
        sensorService.fleet.sensors.add(newSensor);
    }

    @Override
    public void connectionLost(Throwable thrwbl) {
        logger.info("StatusConsumer lost connection");
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken imdt) {

    }
 
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception{
        Jsonb jsonb = JsonbBuilder.create();
        String msgBody = message.toString();
        try {
            SensorStatus newStatus = jsonb.fromJson(msgBody, SensorStatus.class);
            update_sensor_status(newStatus);
        } catch (Exception e) {
            logger.info("Caught Exception running getBody");
            logger.info(e.getMessage());
	    logger.info("Data: " + msgBody);
        }
    }

    @Override
    public void run() {
        try {
            String mqttClientId = "default-status-consumer";
            if (mqttClientIdPrefix != null) {
                mqttClientId = mqttClientIdPrefix.concat("-status-consumer");
            }
            MqttClient mqttClient = new MqttClient(mqttBrokerUrl, mqttClientId, mqttPersistence);
            mqttClient.setCallback(this);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            mqttClient.connect(connOpts);
            mqttClient.subscribe("sensors.*.sensor_status", 2);
            Quarkus.waitForExit();
            mqttClient.disconnect();
        } catch (Exception ex) {
            logger.info("Got exception in DataConsumer");
        }
    }
}
