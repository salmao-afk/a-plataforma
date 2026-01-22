package pt.ipleiria.estg.dei.dae.plataforma.dtos;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Comment;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Publication;
import pt.ipleiria.estg.dei.dae.plataforma.entities.PublicationStatus;
import pt.ipleiria.estg.dei.dae.plataforma.entities.User;

import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class CommentDTO {
    private long id;

    @NotNull
    @NotBlank(message = "O comentário não pode estar vazio")
    private String content;

    private Boolean hidden;
    private String hideReason;

    private String authorUsername;
    private String authorName;

    private long publicationId;
    private String date;

    public CommentDTO() {
    }

    public CommentDTO(long id, String content, boolean hidden, String hideReason, String authorUsername, String authorName, String date, long publicationId) {
        this.id = id;
        this.content = content;
        this.hidden = hidden;
        this.hideReason = hideReason;
        this.authorUsername = authorUsername;
        this.authorName = authorName;
        this.date = date;
        this.publicationId = publicationId;
    }

    public static CommentDTO from(Comment comment) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return new CommentDTO(
                comment.getId(),
                comment.getContent(),
                comment.isHidden(),
                comment.getHideReason(),
                comment.getAuthor().getUsername(),
                comment.getAuthor().getName(),
                comment.getPostedAt().format(formatter),
                comment.getPublication().getId()
        );
    }

    public static List<CommentDTO> from(List<Comment> comments) {
        return comments.stream().map(CommentDTO::from).collect(Collectors.toList());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean isHidden() {
        return hidden;
    }

    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }

    public String getHideReason() {
        return hideReason;
    }

    public void setHideReason(String hideReason) {
        this.hideReason = hideReason;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public void setAuthorUsername(String authorUsername) {
        this.authorUsername = authorUsername;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public long getPublicationId() {
        return publicationId;
    }

    public void setPublicationId(long publicationId) {
        this.publicationId = publicationId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
