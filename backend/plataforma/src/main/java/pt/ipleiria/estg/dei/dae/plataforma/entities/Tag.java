package pt.ipleiria.estg.dei.dae.plataforma.entities;

import jakarta.persistence.*;

import jakarta.json.bind.annotation.JsonbTransient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "tags")
@NamedQueries({
        @NamedQuery(
                name = "getAllTags",
                query = "SELECT t FROM Tag t ORDER BY t.name ASC" // JPQL
        )
})
public class Tag extends Versionable implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String name;

    @ManyToMany(mappedBy = "tags")
    private List<Publication> publications = new ArrayList<>();

    @ManyToMany(mappedBy = "subscribedTags")
    private List<User> subscribers = new ArrayList<>();

    //private User creator;

    private String description;

    public Tag() {
    }

    public Tag(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonbTransient
    public List<Publication> getPublications() {
        return publications;
    }

    public void setPublications(List<Publication> publications) {
        this.publications = publications;
    }

    @JsonbTransient
    public List<User> getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(List<User> subscribers) {
        this.subscribers = subscribers;
    }

    public void addPublication(Publication publication) {
        publications.add(publication);
    }

    public void addSubscriber(User user) {
        subscribers.add(user);
    }

    public void removePublication(Publication publication) {
        publications.remove(publication);
    }

    public void removeSubscriber(User user) {
        subscribers.remove(user);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tag tag = (Tag) o;
        return id == tag.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
