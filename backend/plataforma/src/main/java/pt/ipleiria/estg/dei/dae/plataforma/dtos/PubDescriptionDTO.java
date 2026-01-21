package pt.ipleiria.estg.dei.dae.plataforma.dtos;

public class PubDescriptionDTO {
    private String description;

    public PubDescriptionDTO() {
    }

    public PubDescriptionDTO(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
