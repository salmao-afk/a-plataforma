package pt.ipleiria.estg.dei.dae.plataforma.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@NamedQueries({
        @NamedQuery(name = "getAllCommentsFromPublication",
                query = "SELECT c " +
                        "FROM Comment c " +
                        "WHERE c.publication.id = :pubId " +
                        "ORDER BY c.postedAt DESC"),

        @NamedQuery(name = "getVisibleCommentsFromPublication",
                query = "SELECT c " +
                        "FROM Comment c " +
                        "WHERE c.publication.id = :pubId " +
                        "AND c.hidden = FALSE " +
                        "ORDER BY c.postedAt DESC"),

        @NamedQuery(name = "getUserComments",
                query = "SELECT c " +
                        "FROM Comment c " +
                        "WHERE c.author.username = :username " +
                        "ORDER BY c.postedAt DESC")
})
public class Comment extends Versionable implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "publication_id", nullable = false)
    private Publication publication;

    @Lob
    @NotNull
    @NotBlank(message = "O comentário não pode estar vazio")
    @Column(nullable = false)
    private String content;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "author_username", nullable = false)
    private User author;

    @Column(nullable = false)
    private boolean hidden = false;

    private String hideReason;

    @ManyToOne
    @JoinColumn(name = "hidden_by_username")
    private User hiddenBy;

    @Column(nullable = false)
    private LocalDateTime postedAt;

    public Comment() {
        postedAt = LocalDateTime.now();
    }

    public Comment(String content, Publication publication, User author) {
        this();
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

    public Publication getPublication() {
        return publication;
    }

    public void setPublication(Publication publication) {
        this.publication = publication;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
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

    public LocalDateTime getPostedAt() {
        return postedAt;
    }

}
