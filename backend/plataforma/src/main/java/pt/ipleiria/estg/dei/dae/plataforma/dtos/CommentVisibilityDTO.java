package pt.ipleiria.estg.dei.dae.plataforma.dtos;

import jakarta.validation.constraints.NotNull;

public class CommentVisibilityDTO {
    @NotNull(message = "Campo 'hidden' é obrigatório" )
    private Boolean hidden;
    private String hideReason;

    public CommentVisibilityDTO() {
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
}
