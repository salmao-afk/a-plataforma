package pt.ipleiria.estg.dei.dae.plataforma.ejbs;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Comment;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Publication;
import pt.ipleiria.estg.dei.dae.plataforma.entities.User;
import pt.ipleiria.estg.dei.dae.plataforma.exceptions.MyEntityNotFoundException;

import java.util.List;

@Stateless
public class CommentBean {
    @PersistenceContext
    private EntityManager em;

    @EJB
    private UserBean userBean;

    @EJB
    private PublicationBean publicationBean;

    public Comment create(long publicationId, String username, String content) throws MyEntityNotFoundException {
        //Validar se a publi existe
        Publication publication = publicationBean.findOrFail(publicationId);

        //Validar se o user existe
        User author = userBean.findOrFail(username);

        //Criar o comentario
        Comment comment = new Comment(content, publication, author);

        em.persist(comment);

        //Aqui meter o código para notificar os subscritores da Tag
        // notificationBean.notifySubscribers(publication, "Novo comentário adicionado!");

        return comment;
    }

    public List<Comment> getComments(long publicationId) throws MyEntityNotFoundException {
        //Validar se a publi existe antes
        Publication publication = publicationBean.findOrFail(publicationId);

        return em.createNamedQuery("getAllCommentsFromPublication", Comment.class)
                .setParameter("pubId", publicationId)
                .getResultList();
    }
}
