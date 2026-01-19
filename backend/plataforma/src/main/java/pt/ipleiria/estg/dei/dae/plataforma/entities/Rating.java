package pt.ipleiria.estg.dei.dae.plataforma.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Entity
@Table(name = "ratings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_username", "publication_id"}) // Impede duplicados
})
@NamedQueries({
        @NamedQuery(name = "Rating.findByUserAndPub",
                query = "SELECT r FROM Rating r WHERE r.user.username = :username AND r.publication.id = :pubId"),
        @NamedQuery(name = "Rating.getAverage",
                query = "SELECT AVG(r.value) FROM Rating r WHERE r.publication.id = :pubId")
})
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Min(1) @Max(5)
    private int value;

    @ManyToOne
    @JoinColumn(name = "user_username")
    private User user;

    @ManyToOne
    @JoinColumn(name = "publication_id")
    private Publication publication;

    public Rating() {}

    public Rating(int value, User user, Publication publication) {
        this.value = value;
        this.user = user;
        this.publication = publication;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Publication getPublication() {
        return publication;
    }

    public void setPublication(Publication publication) {
        this.publication = publication;
    }
}
