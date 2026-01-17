package pt.ipleiria.estg.dei.dae.plataforma.entities;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "publication_history")
public class PublicationHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    //dados antigos da publicação
    private String title;
    private String description;

    @Column(name = "tags_snapshot")
    private String tagsSnapshot;

    private String type;
    private List<String> authors;
    private String scientificArea;

    private LocalDate modificationDate;

    @ManyToOne
    private User modifier;

    @ManyToOne
    private Publication publication;

    public PublicationHistory() {
    }

    public PublicationHistory(User modifier, Publication publication) {
        this.title = publication.getTitle();
        this.scientificArea = publication.getScientificArea();
        this.type = publication.getType();
        if (publication.getTags() != null && !publication.getTags().isEmpty()) {
            this.tagsSnapshot = publication.getTags().stream()
                    .map(Tag::getName)
                    .collect(Collectors.joining(", "));
        }
        this.authors = publication.getAuthors();
        this.scientificArea = publication.getScientificArea();
        this.description = publication.getDescription();
        this.modificationDate = LocalDate.now();
        this.modifier = modifier;
        this.publication = publication;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    public String getScientificArea() {
        return scientificArea;
    }

    public void setScientificArea(String scientificArea) {
        this.scientificArea = scientificArea;
    }

    public LocalDate getModificationDate() {
        return modificationDate;
    }

    public void setModificationDate(LocalDate modificationDate) {
        this.modificationDate = modificationDate;
    }

    public User getModifier() {
        return modifier;
    }

    public void setModifier(User modifier) {
        this.modifier = modifier;
    }

    public Publication getPublication() {
        return publication;
    }

    public void setPublication(Publication publication) {
        this.publication = publication;
    }

    public String getTagsSnapshot() {
        return tagsSnapshot;
    }

    public void setTagsSnapshot(String tagsSnapshot) {
        this.tagsSnapshot = tagsSnapshot;
    }
}
