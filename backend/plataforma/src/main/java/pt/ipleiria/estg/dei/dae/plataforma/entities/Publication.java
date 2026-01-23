package pt.ipleiria.estg.dei.dae.plataforma.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "publications")
@NamedQueries({
        @NamedQuery(name = "getAllPublications", query = "SELECT p FROM Publication p ORDER BY p.publicationDate DESC"),
        @NamedQuery(name = "Publication.findByUsername",
        query = "SELECT p " +
                "FROM Publication p " +
                "LEFT JOIN FETCH p.tags " +
                "WHERE p.uploader.username = :username")
})
public class Publication extends Versionable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @NotNull
    private String title;

    @jakarta.persistence.Column(length = 10000)
    private String description;

    @NotNull
    private LocalDate publicationDate;

    @NotNull
    private String filename;

    @NotNull
    private String filepath;

    // Relação: Muitas publicações, 1 User
    @ManyToOne
    @JoinColumn(name = "uploader_name", nullable = false)
    @NotNull
    private User uploader;

    @NotNull
    private String scientificArea;

    @NotNull
    private List<String> authors = new ArrayList<>();

    @NotNull
    private String type;

    @Column(nullable = false)
    private boolean visible = true;

    private String hideReason;
    private Date hideDate;

    @ManyToOne
    private User hiddenBy;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    @JoinTable(
            name = "publications_tags",
            joinColumns = @JoinColumn(name = "publication_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id", referencedColumnName = "id")
    )
    private List<Tag> tags = new ArrayList<>();

    @OneToMany(mappedBy = "publication", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY) //do I need cascade and fetch?
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "publication", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PublicationHistory> history = new ArrayList<>();

    public Publication() {
    }

    public Publication(String title, String description, LocalDate publicationDate, String scientificArea, String type) {
        this.title = title;
        this.description = description;
        this.publicationDate = publicationDate;
        this.scientificArea = scientificArea;
        this.type = type;
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

    public LocalDate getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(LocalDate publicationDate) {
        this.publicationDate = publicationDate;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFilepath() {
        return filepath;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public User getUploader() {
        return uploader;
    }

    public void setUploader(User uploader) {
        this.uploader = uploader;
    }

    public String getScientificArea() {
        return scientificArea;
    }

    public void setScientificArea(String scientificArea) {
        this.scientificArea = scientificArea;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public String getHideReason() {
        return hideReason;
    }

    public void setHideReason(String hideReason) {
        this.hideReason = hideReason;
    }

    public Date getHideDate() {
        return hideDate;
    }

    public void setHideDate(Date hideDate) {
        this.hideDate = hideDate;
    }

    public User getHiddenBy() {
        return hiddenBy;
    }

    public void setHiddenBy(User hiddenBy) {
        this.hiddenBy = hiddenBy;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public List<PublicationHistory> getHistory() {
        return history;
    }

    public void addHistory(PublicationHistory log) {
        history.add(log);
    }
}
