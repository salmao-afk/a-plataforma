package pt.ipleiria.estg.dei.dae.plataforma.dtos;

import jakarta.validation.constraints.NotNull;
import pt.ipleiria.estg.dei.dae.plataforma.entities.UserRole;

public class UserRoleDTO {
    @NotNull(message = "É obrigatório indicar a nova role.")
    private UserRole role;

    public UserRoleDTO() {
    }

    public UserRoleDTO(UserRole role) {
        this.role = role;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}
