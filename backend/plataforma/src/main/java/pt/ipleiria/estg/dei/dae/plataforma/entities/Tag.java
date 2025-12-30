package pt.ipleiria.estg.dei.dae.plataforma.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tags")
public class Tag extends Versionable implements Serializable {
    @Id
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Publication> getPublications() {
        return publications;
    }

    public void setPublications(List<Publication> publications) {
        this.publications = publications;
    }

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
}
