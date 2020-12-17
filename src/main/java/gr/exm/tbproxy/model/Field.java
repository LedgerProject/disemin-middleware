package gr.exm.tbproxy.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.lang.Nullable;
import org.thingsboard.server.common.data.EntityView;

import javax.validation.constraints.NotNull;
import java.util.Optional;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Field {

    private String id;
    @NotNull
    private String name;
    private String perimeter;
    @NotNull
    private Location location;
    private Crop currentCrop;

    public Field() {

    }

    public Field(EntityView device) throws JsonProcessingException {
        this.id = device.getId().getId().toString();
        this.name = device.getAdditionalInfo().get("name").asText();
        this.perimeter = Optional.ofNullable(device.getAdditionalInfo().get("perimeter")).map(JsonNode::asText).orElse(null);
        this.location = new ObjectMapper()
                .readValue(device.getAdditionalInfo().get("location").toString(), Location.class);
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

    public String getPerimeter() {
        return perimeter;
    }

    public void setPerimeter(String perimeter) {
        this.perimeter = perimeter;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Crop getCurrentCrop() {
        return currentCrop;
    }

    public void setCurrentCrop(Crop currentCrop) {
        this.currentCrop = currentCrop;
    }
}
