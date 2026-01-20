package pt.ipleiria.estg.dei.dae.plataforma.ejbs;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.ConstraintViolationException;
import org.apache.james.mime4j.dom.datetime.DateTime;
import org.hibernate.Hibernate;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Comment;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Tag;
import pt.ipleiria.estg.dei.dae.plataforma.entities.User;
import pt.ipleiria.estg.dei.dae.plataforma.entities.UserRole;
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

    public void subscribeTag(String username, long tagId) throws MyEntityNotFoundException {
        User user = findOrFail(username);
        Tag tag = em.find(Tag.class, tagId);

        if (tag == null) {
            throw new MyEntityNotFoundException("Tag não encontrada.");
        }

        // 1. A tal verificação (agora funciona graças ao Passo 1)
        if (user.getSubscribedTags().contains(tag)) {
            // Podes lançar erro ou simplesmente ignorar
            throw new IllegalArgumentException("Tag já subscrita");
        }

        // 2. Adicionar e Gravar
        user.addSubscribedTag(tag);

        // Importante: Forçar a gravação na tabela de junção
        em.merge(user);
    }

    public void unsubscribeTag(String username, long tagId) throws MyEntityNotFoundException {
        User user = findOrFail(username);
        Tag tag = em.find(Tag.class, tagId);

        if (tag != null) {
            user.removeSubscribedTag(tag);
        }
    }

    public User findWithTags(String username) throws MyEntityNotFoundException {
        // JOIN FETCH carrega o utilizador E as tags numa só query
        try {
            return em.createQuery(
                            "SELECT u FROM User u LEFT JOIN FETCH u.subscribedTags WHERE u.username = :username",
                            User.class)
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (Exception e) {
            throw new MyEntityNotFoundException("User não encontrado");
        }
    }

    public void sendPassowrdResetEmail(String email){
        try{
            User user = em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class).setParameter("email", email).getSingleResult();

            //gerar token
            String token = java.util.UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpirationDate(Date.from(Instant.now().plusSeconds(3600)));

            // Link simulado (o frontend depois trata disto)
            String link = "http://localhost:3000/auth/reset-password?token=" + token;

            String subject = "Recuperação de Password";
            String body = "Olá " + user.getName() + ",\n\n" +
                    "Para definir uma nova password, usa o seguinte token ou link:\n" +
                    "Token: " + token + "\n" +
                    "Link: " + link;

            emailBean.send(user.getEmail(), subject, body);

        } catch (NoResultException e) {
            // SEGURANÇA: Nunca digas "Email não encontrado". Ignora silenciosamente
            // para que hackers não consigam descobrir que emails existem no sistema.
        }
    }
    public void resetPassword(String token, String newPassword) throws IllegalArgumentException {
        try {
            User user = em.createQuery("SELECT u FROM User u WHERE u.resetToken = :token", User.class)
                    .setParameter("token", token)
                    .getSingleResult();

            // Verificar se o token expirou
            if (user.getResetTokenExpirationDate() == null || user.getResetTokenExpirationDate().before(new Date().from(Instant.now()))) {
                throw new IllegalArgumentException("O token expirou. Pede um novo.");
            }

            // Atualizar password
            user.setPassword(Hasher.hash(newPassword));

            // Limpar o token para não ser usado 2 vezes
            user.setResetToken(null);
            user.setResetTokenExpirationDate(null);

        } catch (NoResultException e) {
            throw new IllegalArgumentException("Token inválido.");
        }
    }
}
