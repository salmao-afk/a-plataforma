package pt.ipleiria.estg.dei.dae.plataforma.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users", uniqueConstraints = { @UniqueConstraint(columnNames = "email") })
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING)
public class User extends Versionable implements Serializable {

    @Version
    private int version;

    @Id
    @NotBlank
    private String username;

    @NotBlank
    private String password; //TODO: hashable

    @NotBlank
    @Email
    @Column(unique = true)
    String email;

    @NotNull
    private boolean active = true;
    @ManyToMany
    @JoinTable(name = "users_subscriptions",
            joinColumns = @JoinColumn(name = "username"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private List<Tag> subscribedTags = new ArrayList<>();

    @OneToMany(mappedBy = "uploader")
    private List<Publication> myPublications = new ArrayList<>();

    public User(int version, String username, String password, String email, boolean active) {
        this.version = version;
        this.username = username;
        this.password = password;
        this.email = email;
        this.active = active;
    }

    public User() {
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<Tag> getSubscribedTags() {
        return subscribedTags;
    }

    public void setSubscribedTags(List<Tag> subscribedTags) {
        this.subscribedTags = subscribedTags;
    }

    public void addSubscribedTag(Tag tag) {
        subscribedTags.add(tag);
    }

    public void removeSubscribedTag(Tag tag) {
        subscribedTags.remove(tag);
    }

    public List<Publication> getMyPublications() {
        return myPublications;
    }

    public void setMyPublications(List<Publication> myPublications) {
        this.myPublications = myPublications;
    }
}
