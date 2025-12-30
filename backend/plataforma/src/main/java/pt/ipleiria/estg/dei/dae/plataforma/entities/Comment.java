package pt.ipleiria.estg.dei.dae.plataforma.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

@Entity
@Table(name = "comments")
public class Comment extends Versionable implements Serializable {
    @Id
    private long id;

    @ManyToOne
    @JoinColumn(name = "publication_id", nullable = false)
    private Publication publication;

    private String content;

    @ManyToOne
    @JoinColumn(name = "author_username", nullable = false)
    private User author;

    @Enumerated(EnumType.STRING)
    private PublicationStatus status = PublicationStatus.VISIBLE;

    private String hideReason;

    @ManyToOne
    private User hiddenBy;

    public Comment() {
    }

    public Comment(String content, Publication publication, User author) {
        this.content = content;
        this.publication = publication;
        this.author = author;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public PublicationStatus getStatus() {
        return status;
    }

    public void setStatus(PublicationStatus status) {
        this.status = status;
    }

    public String getHideReason() {
        return hideReason;
    }

    public void setHideReason(String hideReason) {
        this.hideReason = hideReason;
    }

    public User getHiddenBy() {
        return hiddenBy;
    }

    public void setHiddenBy(User hiddenBy) {
        this.hiddenBy = hiddenBy;
    }

    public Publication getPublication() {
        return publication;
    }

    public void setPublication(Publication publication) {
        this.publication = publication;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }
}
