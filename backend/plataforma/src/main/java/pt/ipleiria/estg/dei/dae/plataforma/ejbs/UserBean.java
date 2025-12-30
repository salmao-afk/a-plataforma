package pt.ipleiria.estg.dei.dae.plataforma.ejbs;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.ConstraintViolationException;
import pt.ipleiria.estg.dei.dae.plataforma.entities.User;
import pt.ipleiria.estg.dei.dae.plataforma.entities.UserRole;
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

    public User find(String username){
        return em.find(User.class, username);
    }

    public List<User> findAll(){
        return em.createNamedQuery("getAllUsers", User.class).getResultList();
    }

    public boolean canLogin(String username, String password) {
        var user = find(username);
        return user != null && Hasher.verify(password, user.getPassword());
    }
}
