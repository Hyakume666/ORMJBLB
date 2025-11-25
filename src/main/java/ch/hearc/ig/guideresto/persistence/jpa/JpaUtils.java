package ch.hearc.ig.guideresto.persistence.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import java.util.function.Consumer;

public class JpaUtils {

    private static EntityManagerFactory emf;
    private static EntityManager em;


    public static EntityManager getEntityManager() {
        if (em == null || !em.isOpen()) {
            if (emf == null) {
                emf = Persistence.createEntityManagerFactory("guideRestoJPA");
            }
            em = emf.createEntityManager();
        }
        return em;
    }

    public static void inTransaction(Consumer<EntityManager> consumer) {
        EntityManager em = JpaUtils.getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            consumer.accept(em);
            em.flush();
            transaction.commit();
        } catch (Exception ex) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw ex;
        }
    }
    /**
     * Ferme l'EntityManagerFactory.
     * À appeler à la fin de l'application pour libérer les ressources.
     */
    public static void closeEntityManagerFactory() {
        if (em != null && em.isOpen()) {
            em.close();
        }
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}
