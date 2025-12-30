package pt.ipleiria.estg.dei.dae.plataforma.ejbs;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateful;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Publication;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Tag;
import pt.ipleiria.estg.dei.dae.plataforma.entities.User;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
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
    public void create(String title, String scientificArea, List<String> authors, String type, String uploaderUsername, List<String> tagNames, String filename, InputStream stream) throws IOException {
        //Validar Uploader
        User uploader = userBean.find(uploaderUsername);
        if(uploader == null){
            throw new IllegalArgumentException("User not found");
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

        // Metadados do Ficheiro
        if (stream != null) {
            publication.setFilename(filename); // Nome original (ex: "relatorio.pdf")
            publication.setFilepath(targetFilePath.toString()); // Caminho físico
        }

        //tags
        List<Tag> tags = new ArrayList<>();
        if(tagNames != null && !tagNames.isEmpty()){
            for(String tagName : tagNames){
                //encontrar a tag na base de dados através do nome
                try {
                    Tag tag = em.createQuery("SELECT t FROM Tag t WHERE t.name = :name", Tag.class)
                            .setParameter("name", tagName)
                            .getSingleResult();
                    tags.add(tag);
                } catch (Exception e) {
                    // Se não encontra, CRIA NOVA (Melhor abordagem para MVP)
                    Tag newTag = new Tag(tagName, "nova tag default");// Assume construtor Tag(String name)
                    em.persist(newTag);
                    tags.add(newTag);
                }
            }
        }
        publication.setTags(tags);

        em.persist(publication);
    }

    public List<Publication> findAll(){
        return em.createNamedQuery("getAllPublications", Publication.class).getResultList();
    }
}
