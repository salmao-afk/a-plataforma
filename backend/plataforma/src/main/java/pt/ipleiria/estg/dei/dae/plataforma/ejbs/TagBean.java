package pt.ipleiria.estg.dei.dae.plataforma.ejbs;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Tag;
import pt.ipleiria.estg.dei.dae.plataforma.exceptions.MyEntityExistsException;
import pt.ipleiria.estg.dei.dae.plataforma.exceptions.MyEntityNotFoundException;

import java.util.List;

@Stateless
public class TagBean {
    @PersistenceContext
    private EntityManager em;



    public void create(String name, String description) throws MyEntityExistsException {
        long count = em.createQuery(
                        "SELECT COUNT(t) FROM Tag t WHERE t.name = :name", Long.class)
                .setParameter("name", name)
                .getSingleResult();

        if (count > 0) {
            throw new MyEntityExistsException("Tag já existe");
        }

        Tag tag = new Tag(name, description);
        em.persist(tag);
    }

    public List<Tag> findAll(){
        return em.createNamedQuery("getAllTags", Tag.class).getResultList();
    }

    public Tag findOrFail(long id) throws MyEntityNotFoundException {
        if(em.find(Tag.class, id) == null){
            throw new MyEntityNotFoundException("Tag not found");
        }
        return em.find(Tag.class, id);
    }

    public void delete(long id) throws MyEntityNotFoundException {
        if(em.find(Tag.class, id) == null){
            throw new MyEntityNotFoundException("Tag not found");
        }

        em.remove(em.find(Tag.class, id));
    }

    public void update(long id, String newName, String newDescription) throws MyEntityNotFoundException {
        Tag tag = em.find(Tag.class, id);
        if(tag == null){
            throw new MyEntityExistsException("Tag not found");
        }
        tag.setName(newName);
        tag.setDescription(newDescription);
    }

}
