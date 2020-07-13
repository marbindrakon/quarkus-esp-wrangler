package gg.signal9.mqtttest;

import java.util.LinkedList;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.NotFoundException;

import org.jboss.resteasy.annotations.jaxrs.PathParam;

import gg.signal9.mqtttest.models.SensorZone;

@Path("/zone")
@Produces(MediaType.APPLICATION_JSON)
public class ZoneResource {
    @Inject
    SensorService sensorService;
 
    @GET
    public LinkedList<SensorZone> zone_list(){
        return sensorService.fleet.zones;
    }

    @Path("/{zoneName}")
    @GET
    public SensorZone zone(@PathParam String zoneName){
        for (SensorZone candidate : sensorService.fleet.zones){
            if (candidate.zoneName == zoneName){
                return candidate;
            }
        }
        throw new NotFoundException("A zone with that name is not known.");
    }
}