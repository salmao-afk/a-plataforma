package pt.ipleiria.estg.dei.dae.plataforma.entities;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "users", uniqueConstraints = { @UniqueConstraint(columnNames = "email") })
//@NamedQueries({
//        @NamedQuery(name = "getAllUsers", query = "SELECT u FROM User u ORDER BY u.username ASC")
//})
@NamedQuery(name = "findByEmail", query = "SELECT u FROM User u WHERE u.email = :email")
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
    String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @NotNull
    private Boolean active = true;

    private String statusReason;

    @ManyToMany(mappedBy = "subscribers")
    private List<Tag> subscribedTags = new ArrayList<>();

    @OneToMany(mappedBy = "uploader", cascade = CascadeType.REMOVE)
    private List<Publication> myPublications = new ArrayList<>();

    @Column(nullable = true)
    private String resetToken;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = true)
    private Date tokenExpiry;

    public User(int version, String username, String password, String email, String name, UserRole role, Boolean active, String statusReason) {
        this.version = version;
        this.username = username;
        this.password = password;
        this.email = email;
        this.name = name;
        this.role = role;
        this.active = active;
        this.statusReason = statusReason;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Boolean isActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @JsonbTransient
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

    public String getStatusReason() {
        return statusReason;
    }
    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public String getResetToken() {
        return resetToken;
    }
    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public Date getTokenExpiry() {
        return tokenExpiry;
    }

    public void setTokenExpiry(Date tokenExpiry) {
        this.tokenExpiry = tokenExpiry;
    }
}
