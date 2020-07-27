package gg.signal9.quarkus_esp_wrangler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
//import java.util.LinkedList;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import gg.signal9.quarkus_esp_wrangler.models.*;

@ApplicationScoped
public class StatusConsumer implements Runnable {

    @Inject
    ConnectionFactory connectionFactory;
    
    @Inject
    SensorService sensorService;

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
    public void run() {

        JMSContext context = connectionFactory.createContext(Session.AUTO_ACKNOWLEDGE);
        context.setClientID("wrangler-status-consumer");
        logger.info("Initialized JMS conext.");
        JMSConsumer consumer = context.createConsumer(context.createQueue("sensors.*.sensor_status"));
        context.start();
        logger.info("Started connection");
        logger.info("Client ID: " + context.getClientID());
        while (true) {
            Message message = consumer.receive();
            if (message != null) {
                //logger.info("Got mew status message");
                Jsonb jsonb = JsonbBuilder.create();
                String msgBody;
				try {
                    msgBody = new String(message.getBody(byte[].class));
                    SensorStatus newStatus = jsonb.fromJson(msgBody, SensorStatus.class);
                    update_sensor_status(newStatus);
				} catch (JMSException e) {
                    logger.info("Caught JMSException running getBody");
                    logger.info(e.toString());
				}

            }
        }
    }
}