package pt.ipleiria.estg.dei.dae.plataforma.ejbs;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import pt.ipleiria.estg.dei.dae.plataforma.dtos.PublicationDTO;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Publication;
import pt.ipleiria.estg.dei.dae.plataforma.entities.PublicationHistory;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Tag;
import pt.ipleiria.estg.dei.dae.plataforma.entities.User;
import pt.ipleiria.estg.dei.dae.plataforma.exceptions.MyEntityExistsException;
import pt.ipleiria.estg.dei.dae.plataforma.exceptions.MyEntityNotFoundException;

// Importa Criteria API
import jakarta.persistence.criteria.*;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Stateless
public class PublicationBean {
    @PersistenceContext
    private EntityManager em;

    @EJB
    private UserBean userBean;

    @EJB
    private TagBean tagBean;

    // Diretório de uploads na pasta do utilizador (funciona em Windows/Linux/Mac)
    // Mudamos para /tmp/uploads porque é garantido que o Linux/Docker deixa escrever lá
    private static final String UPLOAD_DIR = "/tmp/uploads";

    // --- CREATE ---
    public Publication create(String title, String scientificArea, String description, List<String> authors, String type, String uploaderUsername, List<String> tagNames, String filename, InputStream stream) throws IOException {

        User uploader = userBean.find(uploaderUsername);
        if(uploader == null){
            throw new IllegalArgumentException("User not found: " + uploaderUsername);
        }

        if (stream == null || filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("É obrigatório fazer upload de um ficheiro.");
        }

        // 1. Gestão de Ficheiros
        Path targetDirectoryPath = Paths.get(UPLOAD_DIR);
        if(!Files.exists(targetDirectoryPath)){
            Files.createDirectories(targetDirectoryPath);
        }

        String uniqueFileName = "file_" + UUID.randomUUID() + "_" + filename;
        Path targetFilePath = targetDirectoryPath.resolve(uniqueFileName);
        Files.copy(stream, targetFilePath, StandardCopyOption.REPLACE_EXISTING);

        // 2. Criar Entidade
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

        // 3. Gestão de Tags
        List<Tag> tags = new ArrayList<>();
        if(tagNames != null && !tagNames.isEmpty()){
            for (String tagName : tagNames) {
                tags.add(findOrCreateTag(tagName.trim()));
            }
        }
        publication.setTags(tags);

        // Opcional: Criar histórico inicial (snapshot da criação)
        // PublicationHistory history = new PublicationHistory(uploader, publication);
        // em.persist(history);
        // publication.addHistory(history);

        em.persist(publication);
        return publication;
    }

    // --- READ (Buscas) ---

    public List<Publication> findAll() {
        return em.createQuery(
                "SELECT DISTINCT p FROM Publication p LEFT JOIN FETCH p.tags ORDER BY p.publicationDate DESC",
                Publication.class
        ).getResultList();
    }

    // Método essencial para a Barra de Pesquisa do Frontend funcionar
    public List<Publication> findAllWithFilters(String area, String tagsStr, Boolean hidden, boolean isAdmin) {
        // Construção dinâmica da Query em Texto (Mais fácil de controlar)
        StringBuilder queryStr = new StringBuilder("SELECT DISTINCT p FROM Publication p");

        // Se houver filtro de tag, fazemos logo o JOIN para filtrar
        if (tagsStr != null && !tagsStr.isEmpty()) {
            queryStr.append(" JOIN p.tags t");
        }

        queryStr.append(" WHERE 1=1"); // Truque para facilitar adicionar 'AND' depois

        // Filtro de Área Científica
        if (area != null && !area.isEmpty()) {
            queryStr.append(" AND LOWER(p.scientificArea) LIKE :area");
        }

        // Filtro de Tag (AQUI ESTÁ A CORREÇÃO)
        // Usamos 'JOIN' (Inner Join) acima, por isso só devolve publicações que tenham a tag
        if (tagsStr != null && !tagsStr.isEmpty()) {
            queryStr.append(" AND t.name = :tagName");
        }

        // Ordenação
        queryStr.append(" ORDER BY p.publicationDate DESC");

        // Criar a Query
        var query = em.createQuery(queryStr.toString(), Publication.class);

        // Preencher os parâmetros
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
            // LEFT JOIN FETCH é obrigatório para carregar as tags sem erro
            return em.createQuery(
                            "SELECT p FROM Publication p LEFT JOIN FETCH p.tags WHERE p.id = :id",
                            Publication.class)
                    .setParameter("id", id)
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new MyEntityNotFoundException("Publicação não encontrada com id: " + id);
        }
    }

    // --- MÉTODOS QUE PEDISTE EXPLICITAMENTE ---

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

    // ------------------------------------------

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

    public void delete(long id) throws MyEntityNotFoundException {
        Publication publication = findOrFail(id);

        if(publication.getFilepath() != null){
            try{
                Files.deleteIfExists(Paths.get(publication.getFilepath()));
            } catch(IOException e){
                System.err.println("Aviso: Não foi possível apagar o ficheiro físico: " + e.getMessage());
            }
        }
        em.remove(publication);
    }

    // --- UPDATE ---

    public void update(long id, PublicationDTO dto, String modifierUsername) throws MyEntityNotFoundException {
        Publication publication = findOrFail(id);
        User modifier = userBean.findOrFail(modifierUsername);

        // Snapshot histórico
        PublicationHistory history = new PublicationHistory(modifier, publication);
        em.persist(history);
        publication.addHistory(history);

        // Atualizar campos
        publication.setTitle(dto.getTitle());
        publication.setScientificArea(dto.getScientificArea());
        publication.setType(dto.getType());
        publication.setDescription(dto.getDescription());
        publication.setAuthors(dto.getAuthors());

        // Atualizar Tags
        if (dto.getTags() != null) {
            List<Tag> newTags = new ArrayList<>();
            for (String tagName : dto.getTags()) {
                newTags.add(findOrCreateTag(tagName.trim()));
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

    private Tag findOrCreateTag(String tagName) {
        try {
            return em.createQuery("SELECT t FROM Tag t WHERE t.name = :name", Tag.class)
                    .setParameter("name", tagName)
                    .getSingleResult();
        } catch (NoResultException e) {
            Tag newTag = new Tag(tagName, "Gerada automaticamente");
            em.persist(newTag);
            return newTag;
        }
    }

    public void addTag(long publicationId, String tagName) throws MyEntityNotFoundException, MyEntityExistsException {
        Publication publication = findOrFail(publicationId);
        Tag tag = findOrCreateTag(tagName);

        if(publication.getTags().contains(tag)){
            throw new MyEntityExistsException("Tag já associada.");
        }
        publication.getTags().add(tag);
    }

    public void removeTag(long publicationId, String tagName) throws MyEntityNotFoundException {
        Publication publication = findOrFail(publicationId);

        Tag tag;
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
}