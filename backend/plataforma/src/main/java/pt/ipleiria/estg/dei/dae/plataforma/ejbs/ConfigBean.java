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

    @EJB
    private CommentBean commentBean;

    @EJB
    private TagBean tagBean;

    @EJB
    private RatingBean ratingBean;
    @PostConstruct
    public void populateDB() {
        try {
            //Criar users
            userBean.create(
                    "admin",
                    "123",
                    "Administrador Chefe",
                    "admin@ipleiria.pt",
                    UserRole.ADMINISTRATOR
            );
            userBean.create(
                    "user1",
                    "123",
                    "Colaborador",
                    "user1@ipleiria.pt",
                    UserRole.COLLABORATOR
            );
            userBean.create(
                    "EduResponsavel",
                    "123",
                    "EduMuitoResponsavel",
                    "eduGamer@ipleiria.pt",
                    UserRole.RESPONSIBLE
            );

            //Criar tags
            tagBean.create("Bases de Dados",
                    "Tópicos relacionados com BDs");
            tagBean.create("IA",
                    "Tópicos relacionados com IA");


        } catch (Exception e) {
        }
    }
}
