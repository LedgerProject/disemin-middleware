package gr.exm.tbproxy.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.validation.constraints.NotNull;

public class Location {

    @NotNull
    private Double latitude;
    @NotNull
    private Double longitude;

    public Location() {

    }

    public Location(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public JsonNode asJsonNode() {
        return new ObjectMapper().createObjectNode()
                .put("latitude", latitude)
                .put("longitude", longitude);
    }
}
