package pt.ipleiria.estg.dei.dae.plataforma.entities;

import jakarta.persistence.*;

import jakarta.json.bind.annotation.JsonbTransient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
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
    private long id; // Agora é ID numérico!

    @Column(unique = true, nullable = false)
    private String name;

    // Relação 1: PUBLICAÇÕES (Passiva - a Publicação manda)
    @ManyToMany(mappedBy = "tags")
    private List<Publication> publications = new ArrayList<>();

    // Relação 2: SUBSCRITORES (Ativa - a Tag manda e cria a tabela)
    @ManyToMany
    @JoinTable(
            name = "tags_subscribers",
            joinColumns = @JoinColumn(name = "tag_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "user_username", referencedColumnName = "username")
    )
    private List<User> subscribers = new ArrayList<>();

    @Column(nullable = false)
    private boolean visible = true;

    private String description;

    private String hideReason;
    private Date hideDate;

    @ManyToOne
    private User hiddenBy;


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
        this.subscribers.add(user);
    }

    public void removeSubscriber(User user) {
        subscribers.remove(user);
    }

    public void removePublication(Publication publication) {
        publications.remove(publication);
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

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
