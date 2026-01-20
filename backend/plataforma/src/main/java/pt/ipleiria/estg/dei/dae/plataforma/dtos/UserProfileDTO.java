package pt.ipleiria.estg.dei.dae.plataforma.dtos;

import pt.ipleiria.estg.dei.dae.plataforma.entities.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UserProfileDTO {
    private String username;
    private String name;
    private String email;

    private String password;

    private UserRole role;

    private boolean active;

    private List<PublicationDTO> publicacoes;
    private List<CommentUserDTO> comentarios;
    private List<RatingDTO> ratings;

    private List<TagDTO> tagsSubscritas;

    public UserProfileDTO() {
    }

    public UserProfileDTO(User user,
                          List<Publication> pubs,
                          List<Comment> comments,
                          List<Rating> ratings,
                          List<Tag> tags) {

        this.username = user.getUsername();
        this.name = user.getName();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.active = user.isActive();
        this.role = user.getRole();

        this.publicacoes = (pubs != null) ? PublicationDTO.from(pubs) : new ArrayList<>();
        this.comentarios = (comments != null) ? CommentUserDTO.from(comments) : new ArrayList<>();
        this.ratings = (ratings != null) ? ratings.stream().map(RatingDTO::from).collect(Collectors.toList()) : new ArrayList<>();
        this.tagsSubscritas = (tags != null) ? TagDTO.from(tags) : new ArrayList<>();
    }

    public List<TagDTO> getTagsSubscritas() {
        return tagsSubscritas;
    }

    public void setTagsSubscritas(List<TagDTO> tagsSubscritas) {
        this.tagsSubscritas = tagsSubscritas;
    }

    public List<RatingDTO> getRatings() {
        return ratings;
    }

    public void setRatings(List<RatingDTO> ratings) {
        this.ratings = ratings;
    }

    public List<CommentUserDTO> getComentarios() {
        return comentarios;
    }

    public void setComentarios(List<CommentUserDTO> comentarios) {
        this.comentarios = comentarios;
    }

    public List<PublicationDTO> getPublicacoes() {
        return publicacoes;
    }

    public void setPublicacoes(List<PublicationDTO> publicacoes) {
        this.publicacoes = publicacoes;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
