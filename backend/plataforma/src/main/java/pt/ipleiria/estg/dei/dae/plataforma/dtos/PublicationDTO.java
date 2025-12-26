package pt.ipleiria.estg.dei.dae.plataforma.dtos;

import pt.ipleiria.estg.dei.dae.plataforma.entities.Publication;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Tag;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class PublicationDTO implements Serializable {
    private long id;
    private String title;
    private String scientificArea;
    private String type;
    private String uploaderUsername;
    private List<String> tags = new ArrayList<>();
    private List<String> authors = new ArrayList<>();
    private LocalDate uploadDate;

    public PublicationDTO() {
    }

    public PublicationDTO(long id, String title, String scientificArea, List<String> authors, String type, String uploaderUsername, List<String> tags, LocalDate uploadDate) {
        this.id = id;
        this.title = title;
        this.scientificArea = scientificArea;
        this.authors = authors;
        this.type = type;
        this.uploaderUsername = uploaderUsername;
        this.tags = tags;
        this.uploadDate = uploadDate;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getScientificArea() { return scientificArea; }
    public void setScientificArea(String scientificArea) { this.scientificArea = scientificArea; }

    public List<String> getAuthors() { return authors; }
    public void setAuthors(List<String> authors) { this.authors = authors; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUploaderUsername() { return uploaderUsername; }
    public void setUploaderUsername(String uploaderUsername) { this.uploaderUsername = uploaderUsername; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public LocalDate getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDate uploadDate) {
        this.uploadDate = uploadDate;
    }

    // --- Métodos de Conversão (Helpers) ---

    // Converte de Entidade para DTO
    public static PublicationDTO from(Publication publication) {
        return new PublicationDTO(
                publication.getId(),
                publication.getTitle(),
                publication.getScientificArea(),
                publication.getAuthors(), //TODO: verificar se é null
                publication.getType(),
                publication.getUploader().getUsername(),
                publication.getTags() != null ?
                        publication.getTags().stream().map(Tag::getName).collect(Collectors.toList()) :
                        new ArrayList<>(),
                publication.getPublicationDate()
        );
    }

    // Converte uma lista inteira de Entidades para lista de DTOs
    public static List<PublicationDTO> from(List<Publication> publications) {
        return publications.stream().map(PublicationDTO::from).collect(Collectors.toList());
    }
}