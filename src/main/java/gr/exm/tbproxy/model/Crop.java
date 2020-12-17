package gr.exm.tbproxy.model;

public class Crop {
    private String name;
    private String description;
    private Long timestamp;

    public Crop() {

    }

    public Crop(String name, String description, Long timestamp) {
        this.name = name;
        this.description = description;
        this.timestamp = timestamp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
