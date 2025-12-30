package pt.ipleiria.estg.dei.dae.plataforma.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "critical_analysis")
public class CriticalAnalysis extends Versionable implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @NotNull
    private String title;

    @NotNull
    private String description;

    private int technicalRating;

    private Date reviewDate;

    @Enumerated(EnumType.STRING)
    private PublicationStatus status = PublicationStatus.VISIBLE;

    private String hideReason;

    @ManyToOne
    private User hiddenBy;

    @ManyToOne
    @NotNull
    private Publication publication;

    @ManyToOne
    @NotNull
    private User author;

    public CriticalAnalysis() {
    }

    public CriticalAnalysis(String title, String description, Publication publication, User author) {
        this.title = title;
        this.description = description;
        this.publication = publication;
        this.author = author;
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

    public int getTechnicalRating() {
        return technicalRating;
    }

    public void setTechnicalRating(int technicalRating) {
        this.technicalRating = technicalRating;
    }

    public Date getReviewDate() {
        return reviewDate;
    }

    public void setReviewDate(Date reviewDate) {
        this.reviewDate = reviewDate;
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
