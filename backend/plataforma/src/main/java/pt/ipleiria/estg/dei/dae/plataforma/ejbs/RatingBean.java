package pt.ipleiria.estg.dei.dae.plataforma.ejbs;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Publication;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Rating;
import pt.ipleiria.estg.dei.dae.plataforma.entities.User;
import pt.ipleiria.estg.dei.dae.plataforma.exceptions.MyEntityNotFoundException;

import java.util.List;

@Stateless
public class RatingBean {
    @PersistenceContext
    EntityManager em;

    private Rating findOrFail(long publicationId, String username){
        var results = em.createNamedQuery("Rating.findByUserAndPub", Rating.class)
                .setParameter("username", username)
                .setParameter("pubId", publicationId)
                .getResultList();

        return results.isEmpty() ? null : results.get(0);
    }

    public List<Rating> getUserRatings(String username) {
        return em.createQuery(
                        "SELECT r FROM Rating r JOIN FETCH r.publication WHERE r.user.username = :username ORDER BY r.id DESC",
                        Rating.class)
                .setParameter("username", username)
                .getResultList();
    }

    public void setRating(long publicationId, String username, int value) throws MyEntityNotFoundException {
        if (value < 1 || value > 5) {
            throw new IllegalArgumentException("O rating deve ser entre 1 e 5.");
        }

        Rating existingRating = findOrFail(publicationId, username);

        if (existingRating != null) {
            // Se já existe
            existingRating.setValue(value);
        } else {
            // Se não existe
            Publication pub = em.find(Publication.class, publicationId);
            if (pub == null) throw new MyEntityNotFoundException("Publicação não encontrada: " + publicationId);

            User user = em.find(User.class, username);
            if (user == null) throw new MyEntityNotFoundException("Utilizador não encontrado: " + username);

            Rating newRating = new Rating(value, user, pub);
            em.persist(newRating);
        }
    }

    public void removeRating(long publicationId, String username) throws MyEntityNotFoundException {
        Rating rating = findOrFail(publicationId, username);
        if (rating == null) {
            throw new MyEntityNotFoundException("Ainda não avaliaste esta publicação.");
        }
        em.remove(rating);
    }

    public double getAverageRating(long publicationId) {
        Double avg = em.createNamedQuery("Rating.getAverage", Double.class)
                .setParameter("pubId", publicationId)
                .getSingleResult();
        return avg == null ? 0.0 : avg;
    }
}
