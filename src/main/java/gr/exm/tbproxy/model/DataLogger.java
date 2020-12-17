package gr.exm.tbproxy.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityView;

import javax.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataLogger {

    private String id;
    @NotNull
    private String name;
    @NotNull
    private String type;
    @NotNull
    private Location location;

    public DataLogger() {

    }

    public DataLogger(EntityView ev) throws JsonProcessingException {
        this.id = ev.getId().toString();
        this.name = ev.getAdditionalInfo().get("name").asText();
        this.type = ev.getType();
        this.location = new ObjectMapper()
                .readValue(ev.getAdditionalInfo().get("location").toString(), Location.class);
    }

    public DataLogger(Device dev) throws JsonProcessingException {
        this.id = dev.getId().toString();
        this.name = dev.getAdditionalInfo().get("name").asText();
        this.type = dev.getType();
        this.location = new ObjectMapper()
                .readValue(dev.getAdditionalInfo().get("location").toString(), Location.class);
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

}
