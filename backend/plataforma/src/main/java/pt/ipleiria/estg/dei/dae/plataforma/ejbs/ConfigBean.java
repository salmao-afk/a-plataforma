package pt.ipleiria.estg.dei.dae.plataforma.ejbs;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import pt.ipleiria.estg.dei.dae.plataforma.entities.UserRole;

@Singleton
@Startup
public class ConfigBean {
    @EJB
    private UserBean userBean;

    @EJB
    private PublicationBean publicationBean;

    @PostConstruct
    public void populateDB() {
        try {
            // Criar o Admin inicial
            userBean.create(
                    "admin",
                    "123", // Será feito hash dentro do create
                    "Administrador Chefe",
                    "admin@ipleiria.pt",
                    UserRole.ADMINISTRATOR
            );
            userBean.create(
                    "user1",
                    "123", // Será feito hash dentro do create
                    "Colaborador",
                    "user1@ipleiria.pt",
                    UserRole.COLLABORATOR
            );
            // Criar outros users de teste para a defesa
            // ...


        } catch (Exception e) {
            // Ignora erro se já existirem (para não dar erro ao reiniciar o Docker)
        }
    }
}
