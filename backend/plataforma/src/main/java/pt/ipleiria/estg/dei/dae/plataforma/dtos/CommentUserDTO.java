package pt.ipleiria.estg.dei.dae.plataforma.dtos;

import pt.ipleiria.estg.dei.dae.plataforma.entities.Comment;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class CommentUserDTO {
    private long id;
    private String content;
    private boolean hidden;
    private String date;

    public CommentUserDTO() {
    }

    public CommentUserDTO(long id, String content,boolean hidden,String date) {
        this.date = date;
        this.hidden = hidden;
        this.content = content;
        this.id = id;
    }

    public static CommentUserDTO from(Comment comment) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return new CommentUserDTO(
                comment.getId(),
                comment.getContent(),
                comment.isHidden(),
                comment.getPostedAt().format(formatter));
    }

    public static List<CommentUserDTO> from(List<Comment> comments) {
        return comments.stream()
                .map(CommentUserDTO::from)
                .collect(Collectors.toList());
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

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
