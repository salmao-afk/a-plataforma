package pt.ipleiria.estg.dei.dae.plataforma.entities;

import jakarta.persistence.Entity;

@Entity
public class Administrator extends User{
    public Administrator() {
    }

    public Administrator(int version, String username, String password, String email) {
        super(version, username, password, email, true);
    }
}
