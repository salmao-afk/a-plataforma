package pt.ipleiria.estg.dei.dae.plataforma.ejbs;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
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

        return em.createNamedQuery("getVisibleCommentsFromPublication", Comment.class)
                .setParameter("pubId", publicationId)
                .getResultList();
    }

    public List<Comment> getUserComments(String username) throws MyEntityNotFoundException {
        User user = userBean.findOrFail(username);

        return em.createNamedQuery("getUserComments", Comment.class)
                .setParameter("username", user.getUsername())
                .getResultList();
    }

    public void toggleVisibility(long commentId, String adminUsername, boolean hidden, String motive) throws MyEntityNotFoundException {
        Comment comment = findOrFail(commentId);
        em.lock(comment, LockModeType.OPTIMISTIC);
        User responsibleUser = userBean.findOrFail(adminUsername);
        if(hidden){
            if(motive == null|| motive.isEmpty()){
                throw new IllegalArgumentException("Para ocultar comentário é necessário indicar o motivo!");
            }
            if(comment.isHidden()){
                throw new IllegalArgumentException("Comentário já está oculto!");
            }
            comment.setHidden(hidden);
            comment.setHideReason(motive);
            comment.setHiddenBy(responsibleUser);
        } else {
            //se o for para deixar o comentário visível,
            //tira as infos da ultima ocultação
            comment.setHidden(hidden);
            comment.setHideReason(null);
            comment.setHiddenBy(null);
        }
        em.merge(comment);
    }

    public void deleteComment(long commentId, String username) throws MyEntityNotFoundException {
        Comment comment = findOrFail(commentId);
        if(!comment.getAuthor().getUsername().equals(username)){
            throw new IllegalArgumentException("Impossível apagar um comentário de outro utilizador!");
        }
        if(comment.isHidden()){
            throw new IllegalArgumentException("Impossível apagar um comentário ocultado!");
        }
        em.remove(comment);
    }
    public Comment findOrFail(long commentId) throws MyEntityNotFoundException {
        Comment comment = em.find(Comment.class, commentId);
        if (comment == null) {
            throw new MyEntityNotFoundException("Comentário com id " + commentId + " não encontrado.");
        }
        return comment;
    }

    public List<Comment> findHidden() {
        return em.createQuery("SELECT c FROM Comment c WHERE c.hidden = TRUE ORDER BY c.postedAt DESC", Comment.class)
            .getResultList();
    }


}
