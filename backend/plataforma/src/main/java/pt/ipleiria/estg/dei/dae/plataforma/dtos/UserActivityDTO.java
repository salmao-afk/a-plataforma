package pt.ipleiria.estg.dei.dae.plataforma.dtos;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

public class UserActivityDTO implements Serializable {

    private String username;
    private String name;

    private List<PublicationDTO> publicacoes;
    private List<CommentUserDTO> comentarios;
    private List<RatingDTO> ratings;

    public UserActivityDTO() {
    }

    public UserActivityDTO(String username, String name,
                           List<PublicationDTO> publicacoes,
                           List<CommentUserDTO> comentarios,
                           List<RatingDTO> ratings) {
        this.username = username;
        this.name = name;
        this.publicacoes = publicacoes != null ? publicacoes : new ArrayList<>();
        this.comentarios = comentarios != null ? comentarios : new ArrayList<>();
        this.ratings = ratings != null ? ratings : new ArrayList<>();
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<PublicationDTO> getPublicacoes() { return publicacoes; }
    public void setPublicacoes(List<PublicationDTO> publicacoes) { this.publicacoes = publicacoes; }

    public List<CommentUserDTO> getComentarios() { return comentarios; }
    public void setComentarios(List<CommentUserDTO> comentarios) { this.comentarios = comentarios; }

    public List<RatingDTO> getRatings() { return ratings; }
    public void setRatings(List<RatingDTO> ratings) { this.ratings = ratings; }
}