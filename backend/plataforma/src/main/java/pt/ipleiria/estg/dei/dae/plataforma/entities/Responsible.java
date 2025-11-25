package pt.ipleiria.estg.dei.dae.plataforma.entities;

import jakarta.persistence.Entity;

@Entity
public class Responsible extends User{


    public Responsible() {
    }

    public Responsible(int version, String username, String password, String email) {
        super(version, username, password, email, true);
    }
}
