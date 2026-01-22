package pt.ipleiria.estg.dei.dae.plataforma.dtos;

import pt.ipleiria.estg.dei.dae.plataforma.entities.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UserProfileDTO {
    private UserDTO user;

    private List<PublicationDTO> publicacoes;
    private List<CommentUserDTO> comentarios;
    private List<RatingDTO> ratings;


    public UserProfileDTO() {
    }

    public UserProfileDTO(User user,
                          List<Publication> pubs,
                          List<Comment> comments,
                          List<Rating> ratings) {

        this.user = UserDTO.fromUser(user);

        this.publicacoes = (pubs != null) ? PublicationDTO.from(pubs) : new ArrayList<>();
        this.comentarios = (comments != null) ? CommentUserDTO.from(comments) : new ArrayList<>();
        this.ratings = (ratings != null) ? ratings.stream().map(RatingDTO::from).collect(Collectors.toList()) : new ArrayList<>();
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    public List<PublicationDTO> getPublicacoes() {
        return publicacoes;
    }

    public void setPublicacoes(List<PublicationDTO> publicacoes) {
        this.publicacoes = publicacoes;
    }

    public List<CommentUserDTO> getComentarios() {
        return comentarios;
    }

    public void setComentarios(List<CommentUserDTO> comentarios) {
        this.comentarios = comentarios;
    }

    public List<RatingDTO> getRatings() {
        return ratings;
    }

    public void setRatings(List<RatingDTO> ratings) {
        this.ratings = ratings;
    }
}
