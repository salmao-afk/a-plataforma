package pt.ipleiria.estg.dei.dae.plataforma.ejbs;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import pt.ipleiria.estg.dei.dae.plataforma.dtos.PublicationDTO;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Publication;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Tag;
import pt.ipleiria.estg.dei.dae.plataforma.entities.UserRole;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

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

    private static final Logger logger = Logger.getLogger("ConfigBean");

    @PostConstruct
    public void populateDB() {
        try {
            //CRIAR USERS
            userBean.create("admin", "123", "Administrador Chefe", "admin@ipleiria.pt", UserRole.ADMINISTRATOR);
            userBean.create("user1", "123", "João Colaborador", "user1@ipleiria.pt", UserRole.COLLABORATOR);
            userBean.create("EduResponsavel", "123", "Eduardo Responsável", "edu@ipleiria.pt", UserRole.RESPONSIBLE);
            userBean.create("leitor", "123", "Maria Leitora", "maria@ipleiria.pt", UserRole.COLLABORATOR);
            userBean.create("Spammer", "123", "Spammer", "spammer@ipleiria.pt", UserRole.COLLABORATOR);

            //CRIAR TAGS
            tagBean.create("Bases de Dados", "SQL e NoSQL");
            tagBean.create("IA", "Machine Learning");
            tagBean.create("DevOps", "Docker e CI/CD");
            tagBean.create("Lixo", "Tag spam");

            //SUBSCREVER TAGS
            long idIA = getTagIdByName("IA");
            long idBD = getTagIdByName("Bases de Dados");
            long idDevOps = getTagIdByName("DevOps");

            if (idIA != -1) userBean.subscribeTag("user1", idIA);
            if (idBD != -1) userBean.subscribeTag("EduResponsavel", idBD);
            if (idDevOps != -1) userBean.subscribeTag("admin", idDevOps);

            //CRIAR PUBLICAÇÕES
            publicationBean.create(
                    "Como configurar WildFly",
                    "DevOps",
                    "Resumo a ser gerado pela IA...",
                    Arrays.asList("João Colaborador", "GPT"),
                    "Artigo",
                    "user1",
                    Arrays.asList("DevOps", "IA"),
                    "wildfly.pdf",
                    new ByteArrayInputStream("Conteudo WildFly".getBytes())
            );

            publicationBean.create(
                    "O Futuro das Bases de Dados",
                    "Engenharia",
                    "Resumo a ser gerado pela IA...",
                    Arrays.asList("Eduardo Responsável"),
                    "Tese",
                    "EduResponsavel",
                    Arrays.asList("Bases de Dados"),
                    "tese.pdf",
                    new ByteArrayInputStream("Conteudo BD".getBytes())
            );

            //Publi para ocultar
            publicationBean.create(
                    "Ganhe dinheiro com Forex",
                    "Marketing digital e criptomoedas",
                    "Resumo a ser gerado pela IA...",
                    Arrays.asList("Spammer"),
                    "Spam",
                    "leitor",
                    Arrays.asList("Lixo"),
                    "spam.pdf",
                    new ByteArrayInputStream("Lixo".getBytes())
            );

            List<Publication> todas = publicationBean.findAll();

            Publication pubWildfly = getPub(todas, "WildFly");
            Publication pubBD = getPub(todas, "Futuro");
            Publication pubSpam = getPub(todas, "dinheiro");

            // Interações
            commentBean.create(pubWildfly.getId(), "admin", "Bom tutorial.");
            ratingBean.setRating(pubWildfly.getId(), "admin", 5);
            ratingBean.setRating(pubWildfly.getId(), "EduResponsavel", 4);

            // Edição
            PublicationDTO dtoUpdate = PublicationDTO.from(pubWildfly);
            dtoUpdate.setTitle("Como configurar WildFly (v2)");
            publicationBean.update(pubWildfly.getId(), dtoUpdate, "user1");

            //Rating
            ratingBean.setRating(pubBD.getId(), "user1", 3);
            commentBean.create(pubBD.getId(), "leitor", "Não gostei.");

            // --- MODERAÇÃO ---
            ratingBean.setRating(pubSpam.getId(), "admin", 1);

            publicationBean.toggleVisibility(
                    pubSpam.getId(),
                    "admin",
                    "Violação dos termos de serviço (scam)"
            );

            logger.info(">>> Config Bean corrido com sucesso!");

        } catch (Exception e) {
            logger.severe(">>> ERRO NO CONFIG BEAN: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- Auxiliares ---
    private Publication getPub(List<Publication> lista, String termo) {
        return lista.stream()
                .filter(p -> p.getTitle().contains(termo))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Erro no Seed: Publicação '" + termo + "' não encontrada!"));
    }


    private long getTagIdByName(String name) {
        try {
            return tagBean.findAll().stream()
                    .filter(t -> t.getName().equals(name))
                    .findFirst()
                    .map(Tag::getId)
                    .orElse(-1L);
        } catch (Exception e) {
            return -1L;
        }
    }
}