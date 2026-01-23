package pt.ipleiria.estg.dei.dae.plataforma.ejbs;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import pt.ipleiria.estg.dei.dae.plataforma.dtos.PublicationDTO;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Publication;
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
            // 1. CRIAR USERS (Direto, sem verificações)
            userBean.create("admin", "123", "Administrador Chefe", "admin@ipleiria.pt", UserRole.ADMINISTRATOR);
            userBean.create("user1", "123", "João Colaborador", "user1@ipleiria.pt", UserRole.COLLABORATOR);
            userBean.create("EduResponsavel", "123", "Eduardo Responsável", "edu@ipleiria.pt", UserRole.RESPONSIBLE);
            userBean.create("leitor", "123", "Maria Leitora", "maria@ipleiria.pt", UserRole.COLLABORATOR);

            // 2. CRIAR TAGS
            tagBean.create("Bases de Dados", "SQL e NoSQL");
            tagBean.create("IA", "Machine Learning");
            tagBean.create("DevOps", "Docker e CI/CD");
            tagBean.create("Lixo", "Tag spam");

            // 3. SUBSCREVER TAGS (Alimentar o Feed)
            // Assumindo que criaste o método subscribeTag no UserBean como falámos
            userBean.subscribeTag("user1", "IA");
            userBean.subscribeTag("EduResponsavel", "Bases de Dados");
            userBean.subscribeTag("admin", "DevOps");

            // 4. CRIAR PUBLICAÇÕES
            // Nota: Criamos um novo InputStream para cada uma para não haver erros de leitura

            // --- Pub 1: Completa ---
            publicationBean.create(
                    "Como configurar WildFly",
                    "DevOps",
                    "Tutorial completo sobre servidores.",
                    Arrays.asList("João Colaborador", "GPT"),
                    "Artigo",
                    "user1",
                    Arrays.asList("DevOps", "IA"),
                    "wildfly.pdf",
                    new ByteArrayInputStream("Conteudo WildFly".getBytes())
            );

            // --- Pub 2: Tese ---
            publicationBean.create(
                    "O Futuro das Bases de Dados",
                    "Engenharia",
                    "Tese comparativa.",
                    Arrays.asList("Eduardo Responsável"),
                    "Tese",
                    "EduResponsavel",
                    Arrays.asList("Bases de Dados"),
                    "tese.pdf",
                    new ByteArrayInputStream("Conteudo BD".getBytes())
            );

            // --- Pub 3: Spam (Para ocultar) ---
            publicationBean.create(
                    "Ganhe dinheiro rápido",
                    "Marketing",
                    "Esquema duvidoso.",
                    Arrays.asList("Spammer"),
                    "Spam",
                    "leitor",
                    Arrays.asList("Lixo"),
                    "spam.pdf",
                    new ByteArrayInputStream("Lixo".getBytes())
            );

            // 5. INTERAÇÕES E ESTADOS
            // Vamos buscar tudo o que acabámos de criar
            List<Publication> todas = publicationBean.findAll();

            // Recuperar referências (Assume-se que existem porque acabámos de as criar)
            Publication pubWildfly = getPub(todas, "WildFly");
            Publication pubBD = getPub(todas, "Futuro");
            Publication pubSpam = getPub(todas, "dinheiro");

            // --- Interações Pub 1 (Wildfly) ---
            commentBean.create(pubWildfly.getId(), "admin", "Bom tutorial.");
            ratingBean.setRating(pubWildfly.getId(), "admin", 5);
            ratingBean.setRating(pubWildfly.getId(), "EduResponsavel", 4);

            // Simular Edição (Histórico)
            PublicationDTO dtoUpdate = PublicationDTO.from(pubWildfly);
            dtoUpdate.setTitle("Como configurar WildFly (v2)");
            publicationBean.update(pubWildfly.getId(), dtoUpdate, "user1");

            // --- Interações Pub 2 (BD) ---
            commentBean.create(pubBD.getId(), "leitor", "Não concordo com nada, lixo.");
            ratingBean.setRating(pubBD.getId(), "user1", 3);

            // --- Moderação Pub 3 (Spam) ---
            ratingBean.setRating(pubSpam.getId(), "admin", 1);
            publicationBean.toggleVisibility(pubSpam.getId()); // Admin oculta

            logger.info(">>> SEED DATA CONCLUÍDO COM SUCESSO!");

        } catch (Exception e) {
            // Se falhar aqui, o deploy mostra o erro no log
            logger.severe(">>> ERRO FATAL NO SEED DATA: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Helper simples para não sujar o código principal
    private Publication getPub(List<Publication> lista, String termo) {
        return lista.stream()
                .filter(p -> p.getTitle().contains(termo))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Erro no Seed: Publicação '" + termo + "' não encontrada!"));
    }
}