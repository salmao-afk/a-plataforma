package pt.ipleiria.estg.dei.dae.plataforma.ejbs;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.ConstraintViolationException;
import org.hibernate.Hibernate;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Comment;
import pt.ipleiria.estg.dei.dae.plataforma.entities.User;
import pt.ipleiria.estg.dei.dae.plataforma.entities.UserRole;
import pt.ipleiria.estg.dei.dae.plataforma.exceptions.MyEntityNotFoundException;
import pt.ipleiria.estg.dei.dae.plataforma.security.Hasher;

import java.util.List;

@Stateless
public class UserBean {
    @PersistenceContext
    private EntityManager em;

    //TODO: throw exception
    public void create(String username, String password, String name, String email, UserRole role){
        if(em.find(User.class, username) != null){
            throw new IllegalArgumentException("Username already exists");
        }

        try{
            User user = new User();

            user.setUsername(username);
            user.setPassword(Hasher.hash(password));
            user.setName(name);
            user.setEmail(email);
            user.setRole(role);
            user.setActive(true);

            em.persist(user);
            em.flush();
        }catch (ConstraintViolationException e){
            throw new IllegalArgumentException("Invalid user data");
        }

    }

    //permite atualizar o name e o email
    public void update(String username,String newEmail, String newName) throws MyEntityNotFoundException{
        User user = findOrFail(username);
        if(newEmail != null && !newEmail.equals(user.getEmail())) {
            User ownerOfEmail = em.createNamedQuery("findByEmail", User.class)
                                    .setParameter("email", newEmail)
                                    .getResultStream()
                                    .findFirst()
                                    .orElse(null);
            if (ownerOfEmail != null) {
                throw new IllegalArgumentException("O email " + newEmail + " já está a ser utilizado por outro utilizador.");
            }
            user.setEmail(newEmail);
        }
        user.setName(newName);
    }

    //apagar um user
    public void delete(String username) throws MyEntityNotFoundException {
        User user = findOrFail(username);

        em.createQuery("DELETE FROM Comment c WHERE c.author.username = :username")
                .setParameter("username", username)
                .executeUpdate();

        em.createQuery("UPDATE Comment c SET c.hiddenBy = NULL WHERE c.hiddenBy.username = :username")
                .setParameter("username", username)
                .executeUpdate();
        em.remove(user);
    }

    public User find(String username) {
        User user = em.find(User.class, username);
        if (user != null) {
            // Isto força o carregamento da lista enquanto a transação está aberta
            Hibernate.initialize(user.getSubscribedTags());
        }
        return user;
    }

    public User findOrFail(String username) throws MyEntityNotFoundException {
        try {
            // O "LEFT JOIN FETCH" obriga a carregar as tags imediatamente
            return em.createQuery(
                            "SELECT u FROM User u LEFT JOIN FETCH u.subscribedTags WHERE u.username = :username",
                            User.class)
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (Exception e) {
            throw new MyEntityNotFoundException();
        }
    }

    public List<User> findAll() {
        // Usamos 'LEFT JOIN FETCH' para garantir que trazemos o user
        // e as tags na mesma query.
        // O 'LEFT' garante que users sem tags também aparecem.
        return em.createQuery(
                "SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.subscribedTags",
                User.class
        ).getResultList();
    }

    public boolean canLogin(String username, String password) {
        var user = find(username);
        return user != null && Hasher.verify(password, user.getPassword());
    }
}
