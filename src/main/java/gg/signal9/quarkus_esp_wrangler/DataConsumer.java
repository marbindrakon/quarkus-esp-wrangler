package gg.signal9.quarkus_esp_wrangler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.logging.Logger;

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

import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.RegistryType;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import gg.signal9.quarkus_esp_wrangler.models.*;

@ApplicationScoped
public class DataConsumer implements Runnable {

    @Inject
    ConnectionFactory connectionFactory;
    
    @Inject
    SensorService sensorService;

    @Inject
    @RegistryType(type = MetricRegistry.Type.APPLICATION)
    MetricRegistry registry;

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
    public void run() {
        JMSContext context = connectionFactory.createContext(Session.AUTO_ACKNOWLEDGE);
        context.setClientID("wrangler-data-consumer");
        logger.info("Initialized JMS conext.");
        JMSConsumer consumer = context.createConsumer(context.createQueue("sensors.*.data"));
        context.start();
        logger.info("Started connection");
        logger.info("Client ID: " + context.getClientID());
        while (true) {
            Message message = consumer.receive();
            if (message != null) {
                Jsonb jsonb = JsonbBuilder.create();
                String msgBody = "";
				try {
                    msgBody = new String(message.getBody(byte[].class));
                    SensorData newData = jsonb.fromJson(msgBody, SensorData.class);
                    update_sensor_reading(newData);
				} catch (Exception e) {
                    logger.info("Caught Exception running getBody");
                    logger.info(e.getMessage());
		    logger.info("Data: " + msgBody);
				}

            }
            expire_stale_readings();
        }
    }
}
