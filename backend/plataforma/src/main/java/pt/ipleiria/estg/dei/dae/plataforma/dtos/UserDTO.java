package pt.ipleiria.estg.dei.dae.plataforma.dtos;

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
    private List<String> subscribedTags = new ArrayList<>();
    private UserRole role;

    public UserDTO() {
    }

    public UserDTO(String username, String email, String name, List<String> subscribedTags, UserRole role) {
        this.username = username;
        this.email = email;
        this.name = name;
        this.subscribedTags = subscribedTags;
        this.role = role;
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

    public List<String> getSubscribedTags() {
        return subscribedTags;
    }

    public void setSubscribedTags(List<String> subscribedTags) {
        this.subscribedTags = subscribedTags;
    }

    public void addSubscribedTag(String tag) {
        subscribedTags.add(tag);
    }

    public void removeSubscribedTag(String tag) {
        subscribedTags.remove(tag);
    }

    public static UserDTO fromUser(User user) {
        return new UserDTO(
                user.getUsername(),
                user.getEmail(),
                user.getName(),
                // Converte a lista de Entidades Tag para lista de Strings (nomes das tags)
                user.getSubscribedTags() == null ? new ArrayList<>() : user.getSubscribedTags().stream().map(Tag::getName).collect(Collectors.toList()),
                user.getRole()
        );
    }

    public static List<UserDTO> fromUsers(List<User> users) {
        return users.stream().map(UserDTO::fromUser).collect(Collectors.toList());
    }
}
