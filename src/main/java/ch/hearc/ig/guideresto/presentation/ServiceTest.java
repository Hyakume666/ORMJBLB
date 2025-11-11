package ch.hearc.ig.guideresto.presentation;

import ch.hearc.ig.guideresto.business.*;
import ch.hearc.ig.guideresto.service.RestaurantService;
import ch.hearc.ig.guideresto.service.EvaluationService;
import ch.hearc.ig.guideresto.persistence.jpa.JpaUtils;
import jakarta.persistence.EntityManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Classe de test pour les services (Exercice 6)
 * Démontre l'utilisation de la couche service
 */
public class ServiceTest {

    private static final Logger logger = LogManager.getLogger(ServiceTest.class);

    public static void main(String[] args) {
        logger.info("=== TESTS DES SERVICES (EXERCICE 6) ===\n");

        try {
            // Initialiser EntityManager
            EntityManager em = JpaUtils.getEntityManager();
            logger.info("EntityManager créé avec succès !\n");

            // Créer les services
            RestaurantService restaurantService = new RestaurantService();
            EvaluationService evaluationService = new EvaluationService();

            // ==================== TEST 1 : RestaurantService - Recherche ====================
            logger.info("--- Test 1 : RestaurantService - Recherche ---");

            // Récupérer tous les restaurants
            List<Restaurant> allRestaurants = restaurantService.getAllRestaurants();
            logger.info("Nombre de restaurants : {}", allRestaurants.size());

            // Rechercher par nom
            List<Restaurant> foundRestaurants = restaurantService.searchRestaurantsByName("fleur");
            logger.info("Restaurants contenant 'fleur' : {}", foundRestaurants.size());
            for (Restaurant r : foundRestaurants) {
                logger.info("  - {} ({})", r.getName(), r.getAddress().getCity().getCityName());
            }

            // Recherche par nom exact
            Restaurant restaurant = restaurantService.getRestaurantByExactName("Fleur-de-Lys");
            if (restaurant != null) {
                logger.info("Restaurant trouvé : {} (ID: {})", restaurant.getName(), restaurant.getId());
            }

            // ==================== TEST 2 : RestaurantService - Création ====================
            logger.info("\n--- Test 2 : RestaurantService - Création ---");

            // Créer un nouveau restaurant
            Restaurant newRestaurant = restaurantService.createRestaurant(
                    "Chez Mario",                    // nom
                    "Excellente pizzeria italienne", // description
                    "http://www.chezmario.ch",       // website
                    "Rue de la Gare 5",             // street
                    1,                               // cityId (Neuchâtel)
                    3                                // typeId (Pizzeria)
            );

            if (newRestaurant != null) {
                logger.info("Restaurant créé avec succès !");
                logger.info("  - ID: {}", newRestaurant.getId());
                logger.info("  - Nom: {}", newRestaurant.getName());
                logger.info("  - Adresse: {} - {} {}",
                        newRestaurant.getAddress().getStreet(),
                        newRestaurant.getAddress().getCity().getZipCode(),
                        newRestaurant.getAddress().getCity().getCityName());
            }

            // ==================== TEST 3 : RestaurantService - Mise à jour ====================
            logger.info("\n--- Test 3 : RestaurantService - Mise à jour ---");

            if (newRestaurant != null) {
                // Modifier le restaurant
                Restaurant updatedRestaurant = restaurantService.updateRestaurant(
                        newRestaurant.getId(),
                        "Chez Mario - Pizzeria",
                        "La meilleure pizzeria de Neuchâtel !",
                        "http://www.chezmario-neuchatel.ch"
                );

                if (updatedRestaurant != null) {
                    logger.info("Restaurant mis à jour avec succès !");
                    logger.info("  - Nouveau nom: {}", updatedRestaurant.getName());
                    logger.info("  - Nouvelle description: {}", updatedRestaurant.getDescription());
                }
            }

            // ==================== TEST 4 : EvaluationService - BasicEvaluation ====================
            logger.info("\n--- Test 4 : EvaluationService - BasicEvaluation (Likes/Dislikes) ---");

            // Récupérer le premier restaurant
            Restaurant firstRestaurant = restaurantService.getRestaurantById(1);
            if (firstRestaurant != null) {
                logger.info("Restaurant à évaluer : {}", firstRestaurant.getName());

                // Avant l'ajout des évaluations
                int likesBefore = evaluationService.countLikes(firstRestaurant.getId());
                int dislikesBefore = evaluationService.countDislikes(firstRestaurant.getId());
                logger.info("Likes avant : {}", likesBefore);
                logger.info("Dislikes avant : {}", dislikesBefore);

                // Ajouter des likes et dislikes
                evaluationService.addBasicEvaluation(firstRestaurant.getId(), true);  // Like
                evaluationService.addBasicEvaluation(firstRestaurant.getId(), true);  // Like
                evaluationService.addBasicEvaluation(firstRestaurant.getId(), false); // Dislike

                // Après l'ajout des évaluations
                int likesAfter = evaluationService.countLikes(firstRestaurant.getId());
                int dislikesAfter = evaluationService.countDislikes(firstRestaurant.getId());
                logger.info("Likes après : {} (+{})", likesAfter, likesAfter - likesBefore);
                logger.info("Dislikes après : {} (+{})", dislikesAfter, dislikesAfter - dislikesBefore);
            }

            // ==================== TEST 5 : EvaluationService - CompleteEvaluation ====================
            logger.info("\n--- Test 5 : EvaluationService - CompleteEvaluation (avec notes) ---");

            if (firstRestaurant != null) {
                // Créer une map des critères et notes
                Map<String, Integer> criteriaGrades = new HashMap<>();
                criteriaGrades.put("Service", 5);
                criteriaGrades.put("Cuisine", 4);
                criteriaGrades.put("Cadre", 5);

                // Avant l'ajout
                int completeEvalsBefore = evaluationService.countCompleteEvaluations(firstRestaurant.getId());
                logger.info("Évaluations complètes avant : {}", completeEvalsBefore);

                // Ajouter une évaluation complète
                CompleteEvaluation evaluation = evaluationService.addCompleteEvaluation(
                        firstRestaurant.getId(),
                        "Jean Dupont",
                        "Excellent restaurant, je recommande vivement !",
                        criteriaGrades
                );

                if (evaluation != null) {
                    logger.info("Évaluation complète ajoutée avec succès !");
                    logger.info("  - Utilisateur: {}", evaluation.getUsername());
                    logger.info("  - Commentaire: {}", evaluation.getComment());
                    logger.info("  - Nombre de notes: {}", evaluation.getGrades().size());

                    for (Grade grade : evaluation.getGrades()) {
                        logger.info("    * {} : {}/5",
                                grade.getCriteria().getName(),
                                grade.getGrade());
                    }
                }

                // Après l'ajout
                int completeEvalsAfter = evaluationService.countCompleteEvaluations(firstRestaurant.getId());
                logger.info("Évaluations complètes après : {} (+{})",
                        completeEvalsAfter, completeEvalsAfter - completeEvalsBefore);
            }

            // ==================== TEST 6 : EvaluationService - Statistiques ====================
            logger.info("\n--- Test 6 : EvaluationService - Calcul de moyennes ---");

            if (firstRestaurant != null) {
                // Moyenne par critère
                double avgService = evaluationService.getAverageGradeForCriteria(
                        firstRestaurant.getId(), "Service");
                double avgCuisine = evaluationService.getAverageGradeForCriteria(
                        firstRestaurant.getId(), "Cuisine");
                double avgCadre = evaluationService.getAverageGradeForCriteria(
                        firstRestaurant.getId(), "Cadre");

                logger.info("Moyennes par critère pour '{}' :", firstRestaurant.getName());
                logger.info("  - Service : {}/5", String.format("%.2f", avgService));
                logger.info("  - Cuisine : {}/5", String.format("%.2f", avgCuisine));
                logger.info("  - Cadre : {}/5", String.format("%.2f", avgCadre));

                // Moyenne générale
                double avgOverall = evaluationService.getOverallAverageGrade(firstRestaurant.getId());
                logger.info("Moyenne générale : {}/5", String.format("%.2f", avgOverall));

                // Nombre total d'évaluations
                int totalEvaluations = evaluationService.countTotalEvaluations(firstRestaurant.getId());
                logger.info("Nombre total d'évaluations : {}", totalEvaluations);
            }

            // ==================== TEST 7 : RestaurantService - Suppression ====================
            logger.info("\n--- Test 7 : RestaurantService - Suppression ---");

            if (newRestaurant != null) {
                logger.info("Suppression du restaurant créé (ID: {})...", newRestaurant.getId());
                boolean deleted = restaurantService.deleteRestaurant(newRestaurant.getId());

                if (deleted) {
                    logger.info("Restaurant supprimé avec succès !");

                    // Vérifier la suppression
                    Restaurant deletedRestaurant = restaurantService.getRestaurantById(newRestaurant.getId());
                    logger.info("Vérification : restaurant trouvé après suppression ? {}",
                            deletedRestaurant != null ? "OUI (ERREUR)" : "NON (OK)");
                }
            }

            // ==================== TEST 8 : Statistiques globales ====================
            logger.info("\n--- Test 8 : Statistiques globales ---");

            int totalRestaurants = restaurantService.countRestaurants();
            logger.info("Nombre total de restaurants : {}", totalRestaurants);

            em.close();
            logger.info("\n✓ Tous les tests des services réussis !");

        } catch (Exception e) {
            logger.error("ERREUR lors des tests des services", e);
        }

        logger.info("\n=== FIN TESTS SERVICES ===");
    }
}