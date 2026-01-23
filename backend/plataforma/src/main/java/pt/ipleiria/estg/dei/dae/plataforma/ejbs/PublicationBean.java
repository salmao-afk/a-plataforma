package pt.ipleiria.estg.dei.dae.plataforma.ejbs;

import jakarta.annotation.Resource;
import jakarta.ejb.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import jakarta.persistence.criteria.*;
import pt.ipleiria.estg.dei.dae.plataforma.dtos.PublicationDTO;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Publication;
import pt.ipleiria.estg.dei.dae.plataforma.entities.PublicationHistory;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Tag;
import pt.ipleiria.estg.dei.dae.plataforma.entities.User;
import pt.ipleiria.estg.dei.dae.plataforma.exceptions.MyEntityExistsException;
import pt.ipleiria.estg.dei.dae.plataforma.exceptions.MyEntityNotFoundException;
import pt.ipleiria.estg.dei.dae.plataforma.exceptions.MyOptimisticLockException;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Stateless
public class PublicationBean {
    @PersistenceContext
    private EntityManager em;

    @EJB
    private UserBean userBean;

    @EJB
    private LLMBean llmBean;
    @Resource
    private SessionContext ctx;

    @EJB
    private TagBean tagBean;

    @EJB
    private EmailBean emailBean;

    // Diretório de uploads na pasta do utilizador (funciona em Windows/Linux/Mac)
    // Mudamos para /tmp/uploads porque é garantido que o Linux/Docker deixa escrever lá
    private static final String UPLOAD_DIR = "/tmp/uploads";

    // --- CREATE ---
    public void create(String title, String scientificArea, String description, List<String> authors, String type, String uploaderUsername, List<String> tagNames, String filename, InputStream stream) throws IOException {

        User uploader = userBean.find(uploaderUsername);
        if(uploader == null){
            throw new IllegalArgumentException("User not found: " + uploaderUsername);
        }

        if (stream == null || filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("É obrigatório fazer upload de um ficheiro.");
        }

        //Gestão de Ficheiros
        Path targetDirectoryPath = Paths.get(UPLOAD_DIR);
        if(!Files.exists(targetDirectoryPath)){
            Files.createDirectories(targetDirectoryPath);
        }

        String uniqueFileName = "file_" + UUID.randomUUID() + "_" + filename;
        Path targetFilePath = targetDirectoryPath.resolve(uniqueFileName);
        Files.copy(stream, targetFilePath, StandardCopyOption.REPLACE_EXISTING);

        //Criar Entidade
        Publication publication = new Publication();
        publication.setTitle(title);
        publication.setType(type);
        publication.setScientificArea(scientificArea);
        publication.setAuthors(authors);
        publication.setUploader(uploader);
        publication.setPublicationDate(LocalDate.now());
        publication.setDescription(description);
        publication.setFilename(filename);
        publication.setFilepath(targetFilePath.toString());

        //Gestão de Tags
        List<Tag> tags = new ArrayList<>();
        Set<User> usersToNotify = new HashSet<>();

        if(tagNames != null && !tagNames.isEmpty()){
            for (String tagName : tagNames) {
                Tag tag = tagBean.findByName(tagName.trim());
                tags.add(tag);

                for (User subscriber : tag.getSubscribers()) {
                    if (!subscriber.getUsername().equals(uploaderUsername)) {
                        usersToNotify.add(subscriber);
                    }
                }
            }
        }
        publication.setTags(tags);

        em.persist(publication);
        em.flush();

        if (!usersToNotify.isEmpty()) {
            System.out.println(">>> A enviar notificações para " + usersToNotify.size() + " utilizadores.");
            for (User user : usersToNotify) {
                emailBean.send(
                        user.getEmail(),
                        "Nova Publicação: " + title,
                        "Olá " + user.getName() + ",\n\n" +
                                "Foi submetida uma nova publicação com uma tag que segues.\n" +
                                "Título: " + title + "\n" +
                                "Área: " + scientificArea + "\n" +
                                "Vem ver à plataforma!"
                );
            }
        }

        if (filename != null) {
            PublicationBean myProxy = ctx.getBusinessObject(PublicationBean.class);
            myProxy.generateSummaryAsync(publication.getId());
        }
    }

    @Asynchronous
    public void generateSummaryAsync(long publicationId) {
        System.out.println(">>> A INICIAR RESUMO GERADO PELA IA PARA PUBLI COM ID: " + publicationId);
        generateSmartSummary(publicationId);
        System.out.println(">>> RESUMO CONCLUIDO PARA PUBLI COM ID: " + publicationId);
    }

    public void updateDescription(long publicationId, String newDescription) throws MyEntityNotFoundException {
        Publication publication = findOrFail(publicationId);
        publication.setDescription(newDescription);
    }


    public List<Publication> findAll() {
        return em.createQuery(
                "SELECT DISTINCT p FROM Publication p LEFT JOIN FETCH p.tags ORDER BY p.publicationDate DESC",
                Publication.class
        ).getResultList();
    }

    // Método essencial para a Barra de Pesquisa do Frontend funcionar
    public List<Publication> findAllWithFilters(String area, String tagsStr, Boolean hidden, boolean isAdmin) {
        StringBuilder queryStr = new StringBuilder("SELECT DISTINCT p FROM Publication p");
        if (tagsStr != null && !tagsStr.isEmpty()) {
            queryStr.append(" JOIN p.tags t");
        }

        queryStr.append(" WHERE 1=1");

        //Filtro de Área Científica
        if (area != null && !area.isEmpty()) {
            queryStr.append(" AND LOWER(p.scientificArea) LIKE :area");
        }

        //Filtro de Tag
        if (tagsStr != null && !tagsStr.isEmpty()) {
            queryStr.append(" AND t.name = :tagName");
        }

        //Ordenação
        queryStr.append(" ORDER BY p.publicationDate DESC");

        //Criar a Query
        var query = em.createQuery(queryStr.toString(), Publication.class);

        //Preencher os parâmetros
        if (area != null && !area.isEmpty()) {
            query.setParameter("area", "%" + area.toLowerCase() + "%");
        }
        if (tagsStr != null && !tagsStr.isEmpty()) {
            query.setParameter("tagName", tagsStr);
        }

        return query.getResultList();
    }

    public Publication findOrFail(long id) throws MyEntityNotFoundException {
        try {
            //LEFT JOIN FETCH por causa do Lazy Loading
            return em.createQuery(
                            "SELECT p FROM Publication p LEFT JOIN FETCH p.tags WHERE p.id = :id",
                            Publication.class)
                    .setParameter("id", id)
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new MyEntityNotFoundException("Publicação não encontrada com id: " + id);
        }
    }

    public Publication getPublication(long id) throws MyEntityNotFoundException {
        return findOrFail(id);
    }

    public List<Publication> findUserPublications(String username) {
        return em.createQuery(
                        "SELECT DISTINCT p FROM Publication p LEFT JOIN FETCH p.tags WHERE p.uploader.username = :username ORDER BY p.publicationDate DESC",
                        Publication.class)
                .setParameter("username", username)
                .getResultList();
    }


    // --- FILES & DELETE ---

    public File getFile(long id) throws MyEntityNotFoundException {
        Publication publication = findOrFail(id);
        if(publication.getFilepath() == null || publication.getFilepath().isEmpty()){
            throw new MyEntityNotFoundException("Registo sem ficheiro associado.");
        }
        File file = new File(publication.getFilepath());
        if(!file.exists()){
            throw new MyEntityNotFoundException("O ficheiro físico foi apagado do servidor.");
        }
        return file;
    }


    // --- UPDATE ---

    public void update(long id, PublicationDTO dto, String modifierUsername) throws MyEntityNotFoundException{
        Publication publication = findOrFail(id);
        User modifier = userBean.findOrFail(modifierUsername);

        em.lock(publication, LockModeType.OPTIMISTIC);
        //Snapshot histórico
        PublicationHistory history = new PublicationHistory(modifier, publication);
        em.persist(history);
        publication.addHistory(history);

        //Atualizar campos
        publication.setTitle(dto.getTitle());
        publication.setScientificArea(dto.getScientificArea());
        publication.setType(dto.getType());
        publication.setDescription(dto.getDescription());
        publication.setAuthors(dto.getAuthors());

        //Atualizar Tags
        if (dto.getTags() != null) {
            List<Tag> newTags = new ArrayList<>();
            for (String tagName : dto.getTags()) {
                newTags.add(tagBean.findByName(tagName.trim()));
            }
            publication.setTags(newTags);
        }
    }

    public List<PublicationHistory> getHistory(long id) {
        return em.createQuery(
                        "SELECT h FROM PublicationHistory h WHERE h.publication.id = :id ORDER BY h.modificationDate DESC",
                        PublicationHistory.class)
                .setParameter("id", id)
                .getResultList();
    }

    // --- AUXILIARES TAGS ---

    public void addTag(long publicationId, String tagName) throws MyEntityNotFoundException, MyEntityExistsException {
        Publication publication = findOrFail(publicationId);
        Tag tag = tagBean.findByName(tagName);

        if(publication.getTags().contains(tag)){
            throw new MyEntityExistsException("Tag já associada.");
        }
        publication.getTags().add(tag);
    }

    public void removeTag(long publicationId, long tagId) throws MyEntityNotFoundException {
        Publication publication = findOrFail(publicationId);
        Tag tag = em.find(Tag.class, tagId);
        String tagName = tagBean.getTagName(tag);
        try {
            tag = em.createQuery("SELECT t FROM Tag t WHERE t.name = :name", Tag.class)
                    .setParameter("name", tagName)
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new MyEntityNotFoundException("Tag não encontrada.");
        }

        if(!publication.getTags().contains(tag)){
            throw new IllegalArgumentException("Tag não está associada a esta publicação.");
        }
        publication.getTags().remove(tag);
    }


    public Publication find(long id) {
        return em.find(Publication.class, id);
    }

    public void toggleVisibility(long publicationId, String username, String hideReason) throws MyEntityNotFoundException {
        Publication publication = findOrFail(publicationId);
        User user = userBean.findOrFail(username);
        if (publication == null) {
            throw new MyEntityNotFoundException("Publicação não encontrada com id: " + publicationId);
        }

        boolean estadoAntigo = publication.isVisible();

        publication.setVisible(!estadoAntigo);

        if(estadoAntigo == true){
            publication.setHideReason(hideReason);
            publication.setHiddenBy(user);
            publication.setHideDate(LocalDate.now());
        }
        else{
            publication.setHideReason(null);
            publication.setHiddenBy(null);
            publication.setHideDate(null);
        }

        em.flush();
    }

    public List<Publication> findHidden() {
        return em.createQuery("SELECT p FROM Publication p WHERE p.visible = FALSE ORDER BY p.publicationDate DESC", Publication.class)
            .getResultList();
    }

    //------------IA------------
    public String generateSmartSummary(long publicationId) {
        Publication pub = find(publicationId);
        if (pub == null || pub.getFilepath() == null) return "Publicação sem ficheiro.";

        try {
            String pdfText = extractTextFromPdf(pub.getFilepath());

            if (pdfText != null && !pdfText.isEmpty()) {
                String resumo = llmBean.generateSummary(pdfText);

                pub.setDescription(resumo);
                em.merge(pub);
                em.flush();
                return resumo;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Erro: " + e.getMessage();
        }
        return "Não foi possível gerar resumo.";
    }

    //metodo para ler pdf
    private String extractTextFromPdf(String filePath) {
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            if (!document.isEncrypted()) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler PDF: " + e.getMessage());
        }
        return null;
    }


}
