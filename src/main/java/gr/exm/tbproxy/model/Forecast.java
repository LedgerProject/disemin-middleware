package gr.exm.tbproxy.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityView;

public class Forecast {

    private String id;
    private String name;
    private Location location;

    public Forecast() {

    }

    public Forecast(Device device) throws JsonProcessingException {
        this.id = device.getId().toString();
        this.name = device.getAdditionalInfo().get("name").asText();
        this.location = new ObjectMapper().readValue(device.getAdditionalInfo().get("location").toString(), Location.class);
    }

    public Forecast(EntityView ev) throws JsonProcessingException {
        this.id = ev.getId().toString();
        this.name = ev.getAdditionalInfo().get("name").asText();
        this.location = new ObjectMapper().readValue(ev.getAdditionalInfo().get("location").toString(), Location.class);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}
