package pt.ipleiria.estg.dei.dae.plataforma.ejbs;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateful;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import pt.ipleiria.estg.dei.dae.plataforma.dtos.PublicationDTO;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Publication;
import pt.ipleiria.estg.dei.dae.plataforma.entities.PublicationHistory;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Tag;
import pt.ipleiria.estg.dei.dae.plataforma.entities.User;
import pt.ipleiria.estg.dei.dae.plataforma.exceptions.MyEntityExistsException;
import pt.ipleiria.estg.dei.dae.plataforma.exceptions.MyEntityNotFoundException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Stateless
public class PublicationBean {
    @PersistenceContext
    private EntityManager em;

    @EJB
    private UserBean userBean;

    private static final String UPLOAD_DIR = "/tmp/uploads/";

    //TODO: throw exception
    public void create(String title, String scientificArea, String description, List<String> authors, String type, String uploaderUsername, List<String> tagNames, String filename, InputStream stream) throws IOException {
        //Validar Uploader
        User uploader = userBean.find(uploaderUsername);
        if(uploader == null){
            throw new IllegalArgumentException("User not found");
        }

        if (stream == null || filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("É obrigatório fazer upload de um ficheiro.");
        }

        //Tratar do file
        //Criar folder se não existir
        var targetDirectoryPath = Paths.get(UPLOAD_DIR);
        if(!Files.exists(targetDirectoryPath)){
            Files.createDirectories(targetDirectoryPath);
        }

        //garantir nome unico
        var targetFilePath = targetDirectoryPath.resolve("file_" + UUID.randomUUID());
        //guardar ficheiro no disco
        if(stream != null){
            Files.copy(stream, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
        }

        //instanciar a entidade
        Publication publication = new Publication();
        publication.setTitle(title);
        publication.setType(type);
        publication.setScientificArea(scientificArea);
        publication.setAuthors(authors);
        publication.setUploader(uploader);
        publication.setPublicationDate(LocalDate.now());
        publication.setDescription(description);

        // Metadados do Ficheiro
        if (stream != null) {
            publication.setFilename(filename); // Nome original (ex: "relatorio.pdf")
            publication.setFilepath(targetFilePath.toString()); // Caminho físico
        }

        //tags
        List<Tag> tags = new ArrayList<>();
        if(tagNames != null && !tagNames.isEmpty()){
            for (String tagName : tagNames) {
                try {
                    // Busca pelo atributo 'name', não pelo ID
                    Tag tag = em.createQuery(
                                    "SELECT t FROM Tag t WHERE t.id = :id", Tag.class)
                            .setParameter("name", tagName)
                            .getSingleResult();

                    publication.getTags().add(tag);

                } catch (IllegalArgumentException e) {
                    // Se não existe, cria nova
                    Tag newTag = new Tag(tagName, "Gerada automaticamente");
                    em.persist(newTag);
                    publication.getTags().add(newTag);
                }
            }
        }
        publication.setTags(tags);

        em.persist(publication);
    }

    public List<Publication> findAll() {
        // O "DISTINCT" é importante porque o JOIN pode duplicar resultados
        // se uma publicação tiver 3 tags (apareceria 3 vezes).
        return em.createQuery(
                "SELECT DISTINCT p FROM Publication p LEFT JOIN FETCH p.tags ORDER BY p.publicationDate DESC",
                Publication.class
        ).getResultList();
    }


    public Publication find(long id) {
        return em.find(Publication.class, id);
    }

    public Publication findOrFail(long id) throws MyEntityNotFoundException {
        try {
            // LEFT JOIN FETCH carrega as tags na mesma query
            return em.createQuery(
                            "SELECT p FROM Publication p LEFT JOIN FETCH p.tags WHERE p.id = :id",
                            Publication.class)
                    .setParameter("id", id)
                    .getSingleResult();
        } catch (Exception e) {
            throw new MyEntityNotFoundException("Publicação não encontrada com id: " + id);
        }
    }

    public List<Publication> findUserPublications(String username) {
        return em.createQuery(
                        "SELECT DISTINCT p FROM Publication p LEFT JOIN FETCH p.tags WHERE p.uploader.username = :username ORDER BY p.publicationDate DESC",
                        Publication.class)
                .setParameter("username", username)
                .getResultList();
    }

    // Descarregar ficheiro
    public File getFile(long id) throws MyEntityNotFoundException {
        // Encontrar publicação na BD
        Publication publication = findOrFail(id);

        // Verificar se existe ficheiro
        if(publication.getFilepath() == null || publication.getFilepath().isEmpty()){
            throw new MyEntityNotFoundException("Publicação sem ficheiro!");
        }

        // Descarregar ficheiro
        File file = new File(publication.getFilepath());

        if(!file.exists()){
            throw new MyEntityNotFoundException("Ficheiro físico não encontrado!");
        }

        return file;
    }

    // Apagar  publicacao
    public void delete(long id) throws MyEntityNotFoundException {
        Publication publication = findOrFail(id);

        //apagar ficheiro do disco se tiver
        if(publication.getFilepath() != null && !publication.getFilepath().isEmpty()){
            Path path = Paths.get(publication.getFilepath());
            try{
                Files.deleteIfExists(path);
            }catch(IOException e){
                System.out.println("Erro ao apagar ficheiro: " + e.getMessage());
            }
        }

        //apagar publicacao da BD
        em.remove(publication);
    }

    public void update(long id, PublicationDTO dto, String modifierUsername)
            throws MyEntityNotFoundException {

        // 1. Buscas as Entidades
        Publication publication = findOrFail(id);
        User modifier = userBean.findOrFail(modifierUsername);

        // 2. Histórico (Snapshot do que estava ANTES)
        // Nota: O construtor do History deve copiar os valores atuais da 'publication'
        PublicationHistory history = new PublicationHistory(modifier,publication);
        em.persist(history);
        publication.addHistory(history);

        // 3. Atualizar Dados Simples (Lê do DTO)
        publication.setTitle(dto.getTitle());
        publication.setScientificArea(dto.getScientificArea());
        publication.setType(dto.getType());
        publication.setDescription(dto.getDescription());
        publication.setAuthors(dto.getAuthors()); // Assume que a tua entidade aceita a lista

        // 4. Atualizar Tags (A lógica que falámos na resposta anterior)
        if (dto.getTags() != null) {
            List<Tag> newTags = new ArrayList<>();
            for (String tagName : dto.getTags()) {
                try {
                    Tag tag = em.createQuery("SELECT t FROM Tag t WHERE t.name = :name", Tag.class)
                            .setParameter("name", tagName)
                            .getSingleResult();
                    newTags.add(tag);
                } catch (Exception e) {
                    Tag newTag = new Tag(tagName, "Default tag description");
                    em.persist(newTag);
                    newTags.add(newTag);
                }
            }
            publication.setTags(newTags);
        }
    }

    public List<PublicationHistory> getHistory(long id) throws MyEntityNotFoundException {
        return em.createQuery(
                        "SELECT h FROM PublicationHistory h WHERE h.publication.id = :id ORDER BY h.modificationDate DESC",
                        PublicationHistory.class)
                .setParameter("id", id)
                .getResultList();
    }

    public void addTag(long publicationId, String tagName) throws MyEntityNotFoundException, MyEntityExistsException{
        Publication publication = findOrFail(publicationId);

        Tag tag = em.find(Tag.class, tagName);
        if(tag == null){
            throw new MyEntityNotFoundException("Tag not found");
        }

        if(publication.getTags().contains(tag)){
            throw new MyEntityExistsException("Tag already associated with publication");
        }

        publication.getTags().add(tag);
    }

    public void removeTag(long publicationId, String tagName) throws MyEntityNotFoundException{
        Publication publication = findOrFail(publicationId);

        Tag tag = em.find(Tag.class, tagName);
        if(tag == null){
            throw new MyEntityNotFoundException("Tag not found");
        }

        if(!publication.getTags().contains(tag)){
            throw new IllegalArgumentException("Tag not associated with publication");
        }

        publication.getTags().remove(tag);
    }
}
