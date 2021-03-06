package gg.signal9.quarkus_esp_wrangler;

import java.util.LinkedList;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.NotFoundException;

import org.jboss.resteasy.annotations.jaxrs.PathParam;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.Template;

import gg.signal9.quarkus_esp_wrangler.models.*;



@Path("/sensor")
@Produces(MediaType.APPLICATION_JSON)
public class SensorResource {
    @Inject
    SensorService sensorService;

    @Inject
    Template sensorMetrics;
 
    @GET
    public LinkedList<Sensor> sensor_list(){
        return sensorService.fleet.sensors;
    }

    @Path("/readings")
    @GET
    public TemplateInstance readings(){
        return sensorMetrics.data("fleet", sensorService.fleet.sensors);
    }
    
    @Path("/{chipId}")
    @GET
    public Sensor sensor(@PathParam int chipId){
        for (Sensor candidate : sensorService.fleet.sensors){
            if (candidate.chipId == chipId){
                return candidate;
            }
        }
        throw new NotFoundException("A sensor with that chip ID is not known.");
    }

    @Path("/{chipId}/config")
    @GET
    public SensorConfig sensor_config(@PathParam int chipId){
        for (Sensor candidate : sensorService.fleet.sensors){
            if (candidate.chipId == chipId){
                if (candidate.config != null) {
                    return candidate.config;
                } else {
                    throw new NotFoundException("The sensor has no configuration");
                }
            }
        }
        throw new NotFoundException("A sensor with that chip ID is not known.");
    }
}