package ch.hearc.ig.guideresto.presentation;

import ch.hearc.ig.guideresto.business.Restaurant;
import ch.hearc.ig.guideresto.persistence.jpa.JpaUtils;
import ch.hearc.ig.guideresto.service.RestaurantService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Classe de test pour l'Exercice 7 (Gestion de la concurrence)
 * Simule un conflit d'édition pour vérifier le Verrouillage Optimiste.
 */
public class VerrouillageTest {

    private static final Logger logger = LogManager.getLogger(VerrouillageTest.class);

    public static void main(String[] args) {
        logger.info("=== TEST DE CONCURRENCE (EXERCICE 7) ===\n");

        try {
            EntityManager em = JpaUtils.getEntityManager();
            RestaurantService service = new RestaurantService();

            // 1. On récupère un restaurant cible (ex: ID 1)
            Integer targetId = 1;
            Restaurant restaurant = service.getRestaurantById(targetId);

            if (restaurant == null) {
                logger.error("Le restaurant ID {} n'existe pas. Veuillez lancer le script d'insertion de données.", targetId);
                return;
            }

            logger.info("1. Lecture du restaurant : {} (Version actuelle : {})",
                    restaurant.getName(), restaurant.getVersion());

            // 2. SIMULATION D'UN CONFLIT
            // On va modifier la version en base de données "dans le dos" de notre objet Java
            // pour faire croire à Hibernate qu'un autre utilisateur a modifié la ligne.
            logger.info("2. Simulation : Un autre utilisateur modifie le restaurant en base...");

            EntityTransaction tx = em.getTransaction();
            tx.begin();
            // On force l'incrémentation de la version en SQL natif pour contourner le cache Hibernate
            em.createNativeQuery("UPDATE RESTAURANTS SET VERSION = VERSION + 1 WHERE NUMERO = ?")
                    .setParameter(1, targetId)
                    .executeUpdate();
            tx.commit();

            // On vide le cache (detach) pour être sûr, même si le service fera un findById
            // Note: Dans ton implémentation actuelle de updateRestaurant, le service refait un findById.
            // Si le cache L1 est actif, il reprendra la version 0. Si on a de la chance, le conflit sera détecté au flush.

            logger.info("   -> La version en base est maintenant incrémentée (Version n+1)");

            // 3. Tentative de mise à jour avec notre service
            // Le service va travailler avec l'entité qu'il a en contexte (qui a l'ancienne version)
            // ou va tenter d'écraser la version DB.
            logger.info("3. Tentative de mise à jour par l'utilisateur courant...");

            Restaurant updated = service.updateRestaurant(
                    targetId,
                    restaurant.getName() + " - Modifié",
                    restaurant.getDescription(),
                    restaurant.getWebsite()
            );

            // 4. Analyse du résultat
            if (updated == null) {
                logger.info("\n>>> SUCCÈS DU TEST : La mise à jour a été bloquée !");
                logger.info("Le système a détecté que la donnée avait changé entre temps.");
                logger.info("Une OptimisticLockException a été gérée par le service.");
            } else {
                logger.error("\n>>> ÉCHEC DU TEST : La mise à jour a écrasé les données !");
                logger.error("Le verrouillage optimiste ne semble pas fonctionner.");
                logger.error("Version finale : {}", updated.getVersion());
            }

        } catch (Exception e) {
            logger.error("Erreur inattendue lors du test", e);
        } finally {
            JpaUtils.closeEntityManagerFactory();
            logger.info("\n=== FIN DU TEST DE CONCURRENCE ===");
        }
    }
}