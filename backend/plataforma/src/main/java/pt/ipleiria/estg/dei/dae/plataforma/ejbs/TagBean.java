package pt.ipleiria.estg.dei.dae.plataforma.ejbs;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import pt.ipleiria.estg.dei.dae.plataforma.dtos.TagDTO;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Tag;
import pt.ipleiria.estg.dei.dae.plataforma.entities.User;
import pt.ipleiria.estg.dei.dae.plataforma.exceptions.MyEntityExistsException;
import pt.ipleiria.estg.dei.dae.plataforma.exceptions.MyEntityNotFoundException;

import java.util.List;

@Stateless
public class TagBean {
    @PersistenceContext
    private EntityManager em;



    public Tag create(String name, String description) throws MyEntityExistsException {
        long count = em.createQuery(
                        "SELECT COUNT(t) FROM Tag t WHERE t.name = :name", Long.class)
                .setParameter("name", name)
                .getSingleResult();

        if (count > 0) {
            throw new MyEntityExistsException("Tag já existe");
        }

        Tag tag = new Tag(name, description);
        em.persist(tag);
        em.flush();
        return tag;
    }

    public List<Tag> findAll(){
        return em.createNamedQuery("getAllTags", Tag.class).getResultList();
    }

    public List<Tag> findAllVisible() {
        return em.createQuery("SELECT t FROM Tag t WHERE t.visible = true", Tag.class).getResultList();
    }

    public Tag findOrFail(long id) throws MyEntityNotFoundException {
        if(em.find(Tag.class, id) == null){
            throw new MyEntityNotFoundException("Tag not found");
        }
        return em.find(Tag.class, id);
    }

    public String getTagName(Tag tag) throws MyEntityNotFoundException {
        Tag tagFind = findOrFail(tag.getId());
        return tagFind.getName();
    }

    public Tag findByName(String tagName) throws NoResultException {
        try {
            return em.createQuery("SELECT t FROM Tag t WHERE t.name = :name", Tag.class)
                    .setParameter("name", tagName)
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new IllegalArgumentException("A tag '" + tagName + "' não existe. Por favor use apenas tags existentes.");
        }
    }


    public void delete(long id) throws MyEntityNotFoundException {
        if(em.find(Tag.class, id) == null){
            throw new MyEntityNotFoundException("Tag not found");
        }

        em.remove(em.find(Tag.class, id));
    }

    public Tag update(long id, String newName, String newDescription) throws MyEntityNotFoundException {
        Tag tag = em.find(Tag.class, id);
        if(tag == null){
            throw new MyEntityExistsException("Tag not found");
        }
        tag.setName(newName);
        tag.setDescription(newDescription);
        return tag;
    }

    //Método auxiliar para o PublicationBean usar
    public Tag findByNameOrCreate(String name) {
        try {
            //Tenta encontrar
            return em.createQuery("SELECT t FROM Tag t WHERE t.name = :name", Tag.class)
                    .setParameter("name", name)
                    .getSingleResult();
        } catch (Exception e) {
            //Se não encontrar, cria uma nova
            Tag newTag = new Tag(name, "Gerada automaticamente");
            em.persist(newTag);
            return newTag; //Retorna a tag já "gerida" pelo EntityManager
        }
    }

    public void toggleVisibility(long tagId) throws MyEntityNotFoundException {
        Tag tag = em.find(Tag.class, tagId);

        if (tag == null) {
            throw new MyEntityNotFoundException("Tag não encontrada com id: " + tagId);
        }

        tag.setVisible(!tag.isVisible());
        em.merge(tag);
    }

    public List<Tag> findHidden() {
        return em.createQuery("SELECT t FROM Tag t WHERE t.visible = FALSE", Tag.class)
            .getResultList();
    }
}
