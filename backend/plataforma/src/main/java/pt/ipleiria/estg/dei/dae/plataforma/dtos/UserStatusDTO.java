package pt.ipleiria.estg.dei.dae.plataforma.dtos;

import jakarta.validation.constraints.NotNull;

import java.util.Date;

public class UserStatusDTO {
    @NotNull(message = "Campo 'active' é obrigatório" )
    Boolean active;
    String statusReason;

    public UserStatusDTO() {
    }

    public UserStatusDTO(Boolean active, String statusReason) {
        this.active = active;
        this.statusReason = statusReason;
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
}
