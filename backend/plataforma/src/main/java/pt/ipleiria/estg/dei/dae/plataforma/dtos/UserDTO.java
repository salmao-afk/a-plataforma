package pt.ipleiria.estg.dei.dae.plataforma.dtos;

import jakarta.validation.constraints.Email;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Tag;
import pt.ipleiria.estg.dei.dae.plataforma.entities.User;
import pt.ipleiria.estg.dei.dae.plataforma.entities.UserRole;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UserDTO implements Serializable {
    private String username;
    private String email;
    private String password;
    private String name;
    private List<TagDTO> subscribedTags;
    private UserRole role;
    Boolean active;
    String statusReason;

    public UserDTO() {
    }

    public UserDTO(String username, String email, String name, List<TagDTO> subscribedTags, UserRole role, Boolean active, String statusReason) {
        this.username = username;
        this.email = email;
        this.name = name;
        this.subscribedTags = subscribedTags;
        this.role = role;
        this.active = active;
        this.statusReason = statusReason;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public List<TagDTO> getSubscribedTags() {
        return subscribedTags;
    }

    public void setSubscribedTags(List<TagDTO> subscribedTags) {
        this.subscribedTags = subscribedTags;
    }

    public void addSubscribedTag(TagDTO tag) {
        subscribedTags.add(tag);
    }

    public void removeSubscribedTag(TagDTO tag) {
        subscribedTags.remove(tag);
    }

    public Boolean isActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public static UserDTO fromUser(User user) {
        return new UserDTO(
                user.getUsername(),
                user.getEmail(),
                user.getName(),
                // Converte a lista de Entidades Tag para lista de Strings (nomes das tags)
                user.getSubscribedTags() == null ? new ArrayList<>() :
                        user.getSubscribedTags().stream()
                                .map(tag -> new TagDTO(tag.getId(), tag.getName(), tag.getDescription())) // Mapeia para objeto com ID
                                .collect(Collectors.toList()),
                user.getRole(),
                user.isActive(),
                user.getStatusReason()
        );
    }

    public static List<UserDTO> fromUsers(List<User> users) {
        return users.stream().map(UserDTO::fromUser).collect(Collectors.toList());
    }
}
