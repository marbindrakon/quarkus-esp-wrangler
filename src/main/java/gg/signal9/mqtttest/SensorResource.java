package gg.signal9.mqtttest;

import java.util.LinkedList;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.NotFoundException;

import org.jboss.resteasy.annotations.jaxrs.PathParam;

import gg.signal9.mqtttest.models.Sensor;



@Path("/sensor")
@Produces(MediaType.APPLICATION_JSON)
public class SensorResource {
    @Inject
    SensorService sensorService;
 
    @GET
    public LinkedList<Sensor> sensor_list(){
        return sensorService.fleet.sensors;
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
}