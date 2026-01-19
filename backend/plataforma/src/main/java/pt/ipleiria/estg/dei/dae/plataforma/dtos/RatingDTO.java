package pt.ipleiria.estg.dei.dae.plataforma.dtos;

import pt.ipleiria.estg.dei.dae.plataforma.entities.Rating;

public class RatingDTO {
    private int value;

    private long publicationId;
    private String publicationTitle;

    public RatingDTO() {}

    public RatingDTO(int value) {
        this.value = value;
    }

    public RatingDTO(int value, long publicationId, String publicationTitle) {
        this.value = value;
        this.publicationId = publicationId;
        this.publicationTitle = publicationTitle;
    }

    public static RatingDTO from(Rating r) {
        return new RatingDTO(
                r.getValue(),
                r.getPublication().getId(),
                r.getPublication().getTitle()
        );
    }

    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }

    public long getPublicationId() { return publicationId; }
    public void setPublicationId(long publicationId) { this.publicationId = publicationId; }

    public String getPublicationTitle() { return publicationTitle; }
    public void setPublicationTitle(String publicationTitle) { this.publicationTitle = publicationTitle; }
}
