package pt.ipleiria.estg.dei.dae.plataforma.entities;

import jakarta.persistence.Entity;

@Entity
public class Collaborator extends User{

    public Collaborator() {
    }

    public Collaborator(int version, String username, String password, String email) {
        super(version, username, password, email, true);
    }


}
