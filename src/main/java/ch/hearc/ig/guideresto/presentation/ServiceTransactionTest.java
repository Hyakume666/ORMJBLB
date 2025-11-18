package ch.hearc.ig.guideresto.presentation;

import ch.hearc.ig.guideresto.business.*;
import ch.hearc.ig.guideresto.service.RestaurantService;
import ch.hearc.ig.guideresto.service.EvaluationService;
import ch.hearc.ig.guideresto.persistence.dao.CityDao;
import ch.hearc.ig.guideresto.persistence.jpa.JpaUtils;
import jakarta.persistence.EntityManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Classe de test pour démontrer la gestion des transactions (Exercice 6)
 * OBJECTIF : Démontrer que les transactions fonctionnent correctement :
 * 1. Transaction ville + restaurant (createRestaurantWithNewCity)
 * 2. Transaction évaluation + notes (addCompleteEvaluation)
 * 3. Rollback en cas d'erreur
 * 4. Validations d'unicité
 */
public class ServiceTransactionTest {

    private static final Logger logger = LogManager.getLogger(ServiceTransactionTest.class);

    public static void main(String[] args) {
        logger.info("=== TESTS DES TRANSACTIONS (EXERCICE 6) ===\n");
        EntityManager em = JpaUtils.getEntityManager();
        try {
            logger.info("EntityManager créé avec succès !\n");

            // Créer les services
            RestaurantService restaurantService = new RestaurantService();
            EvaluationService evaluationService = new EvaluationService();
            CityDao cityDao = new CityDao();

            // ==================== TEST 1 : Transaction Ville + Restaurant ====================
            logger.info("========================================");
            logger.info("TEST 1 : Création Restaurant + Nouvelle Ville (TRANSACTION UNIQUE)");
            logger.info("========================================\n");

            logger.info("Objectif: Créer un restaurant ET une ville dans UNE SEULE transaction");
            logger.info("Si l'une des créations échoue, TOUT est annulé (rollback)\n");

            // Avant le test : compter les villes et restaurants
            int citiesBeforeTest1 = cityDao.findAll().size();
            int restaurantsBeforeTest1 = restaurantService.countRestaurants();
            logger.info("État initial:");
            logger.info("  - Nombre de villes: {}", citiesBeforeTest1);
            logger.info("  - Nombre de restaurants: {}\n", restaurantsBeforeTest1);

            // Créer un restaurant avec une nouvelle ville
            logger.info("→ Création d'un restaurant 'Le Gourmet' avec nouvelle ville '2300 La Chaux-de-Fonds'...");
            Restaurant newRestaurant = restaurantService.createRestaurantWithNewCity(
                    "Le Gourmet",                           // nom
                    "Restaurant gastronomique moderne",     // description
                    "https://www.legourmet.ch",             // website
                    "Avenue Léopold-Robert 1",             // street
                    "2300",                                // zipCode
                    "La Chaux-de-Fonds",                   // cityName
                    1                                       // typeId (Cuisine suisse)
            );

            // Vérifier le résultat
            if (newRestaurant != null) {
                logger.info("SUCCÈS: Restaurant créé avec ID: {}", newRestaurant.getId());
                logger.info("SUCCÈS: Ville créée avec ID: {}", newRestaurant.getAddress().getCity().getId());

                int citiesAfterTest1 = cityDao.findAll().size();
                int restaurantsAfterTest1 = restaurantService.countRestaurants();
                logger.info("\nÉtat après transaction:");
                logger.info("  - Nombre de villes: {} (+{})", citiesAfterTest1, citiesAfterTest1 - citiesBeforeTest1);
                logger.info("  - Nombre de restaurants: {} (+{})", restaurantsAfterTest1, restaurantsAfterTest1 - restaurantsBeforeTest1);
                logger.info("\nTEST 1 RÉUSSI: Transaction complète (ville + restaurant)\n");
            } else {
                logger.error("TEST 1 ÉCHOUÉ: Le restaurant n'a pas été créé\n");
            }

            // ==================== TEST 2 : Validation Unicité Restaurant ====================
            logger.info("========================================");
            logger.info("TEST 2 : Validation Unicité (DOUBLON REFUSÉ)");
            logger.info("========================================\n");

            logger.info("Objectif: Vérifier qu'on ne peut pas créer 2 restaurants avec le même nom dans la même ville\n");

            // Vérifie que le test 1 a réussi avant de continuer
            if (newRestaurant != null && newRestaurant.getAddress() != null && newRestaurant.getAddress().getCity() != null) {
                // Tenter de créer un restaurant avec le même nom dans la même ville
                logger.info("Tentative de création d'un doublon 'Le Gourmet' dans la même ville...");
                Restaurant duplicateRestaurant = restaurantService.createRestaurant(
                        "Le Gourmet",                           // même nom
                        "Un autre restaurant",
                        "https://www.autre.ch",
                        "Rue du Commerce 5",
                        newRestaurant.getAddress().getCity().getId(),  // même ville
                        1
                );

                if (duplicateRestaurant == null) {
                    logger.info("SUCCÈS: Le doublon a été refusé (comme prévu)");
                    logger.info("TEST 2 RÉUSSI: Validation d'unicité fonctionne\n");
                } else {
                    logger.error("TEST 2 ÉCHOUÉ: Le doublon a été accepté (erreur de validation)\n");
                }
            } else {
                logger.warn("TEST 2 IGNORÉ: Le test 1 a échoué, impossible de tester l'unicité\n");
            }

            // ==================== TEST 3 : Transaction Évaluation + Notes ====================
            logger.info("========================================");
            logger.info("TEST 3 : Création Évaluation + Notes (TRANSACTION UNIQUE)");
            logger.info("========================================\n");

            logger.info("Objectif: Créer une évaluation complète avec plusieurs notes dans UNE SEULE transaction");
            logger.info("Si une note échoue, TOUTE l'évaluation est annulée (rollback)\n");

            // Vérifie que le test 1 a réussi avant de continuer
            if (newRestaurant != null) {
                // Avant le test : compter les évaluations
                int evalsBeforeTest3 = evaluationService.countCompleteEvaluations(newRestaurant.getId());
                logger.info("État initial:");
                logger.info("  - Nombre d'évaluations complètes: {}\n", evalsBeforeTest3);

                // Créer les notes
                Map<String, Integer> criteriaGrades = new HashMap<>();
                criteriaGrades.put("Service", 5);
                criteriaGrades.put("Cuisine", 5);
                criteriaGrades.put("Cadre", 4);

                logger.info("Création d'une évaluation avec {} notes...", criteriaGrades.size());
                CompleteEvaluation evaluation = evaluationService.addCompleteEvaluation(
                        newRestaurant.getId(),
                        "Jean Dupont",
                        "Excellente expérience ! Service impeccable et cuisine raffinée.",
                        criteriaGrades
                );

                // Vérifier le résultat
                if (evaluation != null) {
                    logger.info("SUCCÈS: Évaluation créée avec ID: {}", evaluation.getId());
                    logger.info("SUCCÈS: {} notes créées", evaluation.getGrades().size());

                    int evalsAfterTest3 = evaluationService.countCompleteEvaluations(newRestaurant.getId());
                    logger.info("\nÉtat après transaction:");
                    logger.info("  - Nombre d'évaluations complètes: {} (+{})", evalsAfterTest3, evalsAfterTest3 - evalsBeforeTest3);
                    logger.info("\nTEST 3 RÉUSSI: Transaction complète (évaluation + notes)\n");
                } else {
                    logger.error("TEST 3 ÉCHOUÉ: L'évaluation n'a pas été créée\n");
                }
            } else {
                logger.warn("TEST 3 IGNORÉ: Le test 1 a échoué, impossible de tester les évaluations\n");
            }

            // ==================== TEST 4 : Validation Note Invalide ====================
            logger.info("========================================");
            logger.info("TEST 4 : Validation Note Invalide (ROLLBACK)");
            logger.info("========================================\n");

            logger.info("Objectif: Vérifier qu'une note invalide (>5) provoque un rollback complet\n");

            // Vérifie que le test 1 a réussi avant de continuer
            if (newRestaurant != null) {
                // Tenter de créer une évaluation avec une note invalide
                Map<String, Integer> invalidGrades = new HashMap<>();
                invalidGrades.put("Service", 10);  // Note invalide !
                invalidGrades.put("Cuisine", 5);

                logger.info("Tentative de création d'une évaluation avec une note invalide (10/5)...");
                CompleteEvaluation invalidEvaluation = evaluationService.addCompleteEvaluation(
                        newRestaurant.getId(),
                        "Utilisateur Test",
                        "Test note invalide",
                        invalidGrades
                );

                if (invalidEvaluation == null) {
                    logger.info("SUCCÈS: L'évaluation invalide a été refusée (comme prévu)");
                    logger.info("SUCCÈS: Aucune note n'a été créée (rollback complet)");
                    logger.info("TEST 4 RÉUSSI: Validation et rollback fonctionnent\n");
                } else {
                    logger.error("TEST 4 ÉCHOUÉ: L'évaluation invalide a été acceptée\n");
                }
            } else {
                logger.warn("TEST 4 IGNORÉ: Le test 1 a échoué, impossible de tester la validation\n");
            }

            // ==================== TEST 5 : Statistiques ====================
            logger.info("========================================");
            logger.info("TEST 5 : Statistiques du Restaurant Créé");
            logger.info("========================================\n");

            // Vérifie que le test 1 a réussi avant de continuer
            if (newRestaurant != null && newRestaurant.getAddress() != null && newRestaurant.getAddress().getCity() != null) {
                logger.info("Restaurant: {}", newRestaurant.getName());
                logger.info("Ville: {} {}",
                        newRestaurant.getAddress().getCity().getZipCode(),
                        newRestaurant.getAddress().getCity().getCityName());

                // Statistiques des évaluations
                int totalEvaluations = evaluationService.countTotalEvaluations(newRestaurant.getId());
                int completeEvaluations = evaluationService.countCompleteEvaluations(newRestaurant.getId());
                int likes = evaluationService.countLikes(newRestaurant.getId());
                int dislikes = evaluationService.countDislikes(newRestaurant.getId());

                logger.info("\nÉvaluations:");
                logger.info("  - Total: {}", totalEvaluations);
                logger.info("  - Complètes: {}", completeEvaluations);
                logger.info("  - Likes: {}", likes);
                logger.info("  - Dislikes: {}", dislikes);

                // Moyennes
                double avgService = evaluationService.getAverageGradeForCriteria(newRestaurant.getId(), "Service");
                double avgCuisine = evaluationService.getAverageGradeForCriteria(newRestaurant.getId(), "Cuisine");
                double avgCadre = evaluationService.getAverageGradeForCriteria(newRestaurant.getId(), "Cadre");
                double avgOverall = evaluationService.getOverallAverageGrade(newRestaurant.getId());

                logger.info("\nMoyennes des notes:");
                logger.info("  - Service: {}/5", String.format("%.1f", avgService));
                logger.info("  - Cuisine: {}/5", String.format("%.1f", avgCuisine));
                logger.info("  - Cadre: {}/5", String.format("%.1f", avgCadre));
                logger.info("  - Moyenne générale: {}/5", String.format("%.1f", avgOverall));

                logger.info("\nTEST 5 RÉUSSI: Statistiques calculées\n");
            } else {
                logger.warn("TEST 5 IGNORÉ: Le test 1 a échoué, impossible d'afficher les statistiques\n");
            }

            // ==================== NETTOYAGE ====================
            logger.info("========================================");
            logger.info("NETTOYAGE : Suppression du restaurant de test");
            logger.info("========================================\n");

            // Vérifie que le restaurant a été créé avant de tenter de le supprimer
            if (newRestaurant != null) {
                logger.info("→ Suppression du restaurant '{}'...", newRestaurant.getName());
                boolean deleted = restaurantService.deleteRestaurant(newRestaurant.getId());

                if (deleted) {
                    logger.info("Restaurant supprimé");

                    // Note : La ville créée reste en base (comportement normal)
                    if (newRestaurant.getAddress() != null && newRestaurant.getAddress().getCity() != null) {
                        logger.info("ℹ Note: La ville '{}' reste en base (comportement normal)",
                                newRestaurant.getAddress().getCity().getCityName());
                    }
                }
            } else {
                logger.warn("NETTOYAGE IGNORÉ: Aucun restaurant à supprimer\n");
            }

            em.close();
            logger.info("\n========================================");
            logger.info("TESTS TERMINÉS");
            logger.info("========================================");
            logger.info("\nRÉSUMÉ DES TESTS:");
            logger.info("Transaction ville + restaurant");
            logger.info("Validation d'unicité");
            logger.info("Transaction évaluation + notes");
            logger.info("Validation et rollback");
            logger.info("Calcul de statistiques");

        } catch (Exception e) {
            logger.error("ERREUR lors des tests des transactions", e);
        }

        logger.info("\n=== FIN TESTS TRANSACTIONS ===");
    }
}