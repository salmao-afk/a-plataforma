package pt.ipleiria.estg.dei.dae.plataforma.ejbs;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.ConstraintViolationException;
import org.apache.james.mime4j.dom.datetime.DateTime;
import org.hibernate.Hibernate;
import pt.ipleiria.estg.dei.dae.plataforma.dtos.*;
import pt.ipleiria.estg.dei.dae.plataforma.entities.*;
import pt.ipleiria.estg.dei.dae.plataforma.exceptions.MyEntityNotFoundException;
import pt.ipleiria.estg.dei.dae.plataforma.security.Hasher;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Stateless
public class UserBean {
    @PersistenceContext
    private EntityManager em;

    @EJB
    private EmailBean emailBean;
    @EJB
    private PublicationBean publicationBean;
    @EJB
    private CommentBean commentBean;
    @EJB
    private RatingBean ratingBean;

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
        em.lock(user, LockModeType.OPTIMISTIC);
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

    public List<UserDTO> getAllUsers() {
        return UserDTO.fromUsers(findAll());
    }
    //suspender um user
    public void suspend(String username, String motive) throws MyEntityNotFoundException {
        if(motive == null || motive.isEmpty()){
            throw new IllegalArgumentException("Precisa de dar um motivo para suspender o utilizador!");
        }
        User user = findOrFail(username);
        if(!user.isActive()){
            throw new IllegalArgumentException("Utilizador já se encontra inativo");
        }
        user.setActive(false);
        user.setStatusReason(motive);
    }

    //ativar um user
    public void activate(String username) throws MyEntityNotFoundException {
        User user = findOrFail(username);
        if(user.isActive()){
            throw new IllegalArgumentException("Utilizador já se encontra ativo");
        }
        user.setActive(true);
        user.setStatusReason(" ");
    }

    //mudar a role de um user
    public void changeRole(String username, UserRole newRole) throws MyEntityNotFoundException {
        User user = findOrFail(username);
        if(!user.isActive() || user.getRole() == UserRole.ADMINISTRATOR){
            throw new IllegalArgumentException("Utilizador tem de estar ativo e não pode ser Administrador");
        }
        user.setRole(newRole);

    }


    //Atividade do user
    public UserActivityDTO getUserActivity(String username) throws MyEntityNotFoundException {
        User user = findOrFail(username);

        List<Publication> publicacoes = publicationBean.findUserPublications(username);
        List<PublicationDTO> publicacoesDtos = PublicationDTO.from(publicacoes);

        List<Comment> comentarios = commentBean.getUserComments(username);
        List<CommentUserDTO> comentariosDtos = CommentUserDTO.from(comentarios);

        List<Rating> ratings = ratingBean.getUserRatings(username);
        List<RatingDTO> ratingsDtos = RatingDTO.from(ratings);

        UserActivityDTO activityDTO = new UserActivityDTO(
                user.getUsername(),
                user.getName(),
                publicacoesDtos,
                comentariosDtos,
                ratingsDtos
        );
        return activityDTO;
    }

    //Profile do User
    public UserProfileDTO getUserProfile(String username) throws MyEntityNotFoundException {
        //Vai buscar user com as tags carregadas
        User user = findOrFail(username);

        //Listas das coisas do user
        var publicacoes = publicationBean.findUserPublications(username);
        var comentarios = commentBean.getUserComments(username);
        var ratings = ratingBean.getUserRatings(username);

        //DTO que vai juntar todos os dados, para devolver como queremos
        UserProfileDTO dto = new UserProfileDTO(
                user,
                publicacoes,
                comentarios,
                ratings
        );
        return dto;
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
            //obriga a carregar as tags imediatamente
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
        return em.createQuery(
                "SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.subscribedTags",
                User.class
        ).getResultList();
    }

    public boolean canLogin(String username, String password) {
        var user = find(username);
        return user != null && Hasher.verify(password, user.getPassword());
    }

    public void subscribeTag(String username, long tagId) throws MyEntityNotFoundException {
        User user = findOrFail(username);
        Tag tag = em.find(Tag.class, tagId);

        if (tag == null) {
            throw new MyEntityNotFoundException("Tag não encontrada.");
        }

        // Verificacao de repeticao
        if (tag.getSubscribers().contains(user)) {
            throw new IllegalArgumentException("Tag já subscrita");
        }

        // Adicionar e Gravar
        tag.addSubscriber(user);
        user.addSubscribedTag(tag);
    }

    public void unsubscribeTag(String username, long tagId) throws MyEntityNotFoundException {
        User user = findOrFail(username);
        Tag tag = em.find(Tag.class, tagId);

        if (tag != null) {
            tag.removeSubscriber(user);
            user.removeSubscribedTag(tag);
        }
    }

    public void sendPasswordResetEmail(String email) {
        try {
            User user = em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                    .setParameter("email", email)
                    .getSingleResult();

            String token = java.util.UUID.randomUUID().toString();
            user.setResetToken(token);

            Date validade = Date.from(Instant.now().plusSeconds(86400));
            user.setTokenExpiry(validade);

            em.flush();

            String link = "http://localhost:3000/auth/reset-password?token=" + token;
            emailBean.send(user.getEmail(), "Recuperar Password", "Token: " + token);

        } catch (NoResultException e) { }
    }

    public void resetPassword(String token, String newPassword) {
        User user = em.createQuery("SELECT u FROM User u WHERE u.resetToken = :token", User.class)
                .setParameter("token", token)
                .getSingleResult();

        if (user.getTokenExpiry() == null || user.getTokenExpiry().before(new Date())) {
            throw new IllegalArgumentException("O token expirou ou é inválido.");
        }

        user.setPassword(Hasher.hash(newPassword));
        user.setResetToken(null);
        user.setTokenExpiry(null);
    }

    public void updatePassword(String username, String oldPassword, String newPassword, String confirmPassword) throws MyEntityNotFoundException {
        User user = findOrFail(username);
        em.lock(user, LockModeType.OPTIMISTIC);
        if (!Hasher.verify(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Password incorreta.");
        }
        if(!newPassword.equals(confirmPassword) || newPassword.isEmpty()){
            throw new IllegalArgumentException("A nova password e a confirmação não coincidem.");
        }
        if(newPassword.equals(oldPassword)){
            throw new IllegalArgumentException("A nova password não pode ser igual à antiga.");
        }
        user.setPassword(Hasher.hash(newPassword));
    }
}
