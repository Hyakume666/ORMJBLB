package ch.hearc.ig.guideresto.service;

import ch.hearc.ig.guideresto.business.*;
import ch.hearc.ig.guideresto.persistence.dao.RestaurantDao;
import ch.hearc.ig.guideresto.persistence.jpa.JpaUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Service de gestion des évaluations (likes/dislikes et évaluations complètes avec notes)
 */
public class EvaluationService {

    private static final Logger logger = LogManager.getLogger(EvaluationService.class);

    private final RestaurantDao restaurantDao;
    private final EvaluationCriteriaService criteriaService;

    public EvaluationService() {
        this.restaurantDao = new RestaurantDao();
        this.criteriaService = new EvaluationCriteriaService();
    }

    /**
     * Ajoute une évaluation basique (like ou dislike) à un restaurant.
     * L'adresse IP de l'utilisateur est automatiquement récupérée et la date actuelle est enregistrée.
     *
     * @param restaurantId L'ID du restaurant à évaluer
     * @param like true pour un like, false pour un dislike
     * @return L'évaluation créée, ou null si le restaurant n'existe pas
     */
    public BasicEvaluation addBasicEvaluation(Integer restaurantId, Boolean like) {
        logger.info("Service: Ajout d'une évaluation basique ({}) pour le restaurant ID {}",
                like ? "Like" : "Dislike", restaurantId);

        Restaurant restaurant = restaurantDao.findById(restaurantId);
        if (restaurant == null) {
            logger.error("Impossible d'ajouter une évaluation basique : Le restaurant avec l'ID {} n'existe pas", restaurantId);
            return null;
        }

        String ipAddress = getLocalIpAddress();

        BasicEvaluation evaluation = new BasicEvaluation(
                LocalDate.now(),
                restaurant,
                like,
                ipAddress
        );

        restaurant.getEvaluations().add(evaluation);
        restaurantDao.save(restaurant);

        logger.info("Évaluation basique ajoutée avec succès");
        return evaluation;
    }

    public int countLikes(Integer restaurantId) {
        Restaurant restaurant = restaurantDao.findById(restaurantId);
        if (restaurant == null) {
            return 0;
        }

        return (int) restaurant.getEvaluations().stream()
                .filter(eval -> eval instanceof BasicEvaluation)
                .map(eval -> (BasicEvaluation) eval)
                .filter(BasicEvaluation::getLikeRestaurant)
                .count();
    }

    public int countDislikes(Integer restaurantId) {
        Restaurant restaurant = restaurantDao.findById(restaurantId);
        if (restaurant == null) {
            return 0;
        }

        return (int) restaurant.getEvaluations().stream()
                .filter(eval -> eval instanceof BasicEvaluation)
                .map(eval -> (BasicEvaluation) eval)
                .filter(be -> !be.getLikeRestaurant())
                .count();
    }

    /**
     * Crée une évaluation complète avec commentaire et notes pour plusieurs critères.
     * Cette méthode effectue une transaction atomique : si l'une des notes est invalide ou
     * si un critère n'existe pas, toute la transaction est annulée (rollback).
     *
     * Validations effectuées :
     * - Le restaurant doit exister
     * - Tous les critères doivent exister
     * - Les notes doivent être entre 1 et 5
     *
     * @param restaurantId L'ID du restaurant à évaluer
     * @param username Le nom de l'utilisateur qui évalue
     * @param comment Le commentaire textuel de l'évaluation
     * @param criteriaGrades Map des critères avec leurs notes (ex: {"Service": 5, "Cuisine": 4})
     * @return L'évaluation créée avec toutes ses notes, ou null si une validation échoue
     */
    public CompleteEvaluation addCompleteEvaluation(Integer restaurantId, String username,
                                                    String comment,
                                                    Map<String, Integer> criteriaGrades) {
        logger.info("Service: Ajout d'une évaluation complète par '{}' pour le restaurant ID {}",
                username, restaurantId);

        Restaurant restaurant = restaurantDao.findById(restaurantId);
        if (restaurant == null) {
            logger.error("Impossible d'ajouter une évaluation complète : Le restaurant avec l'ID {} n'existe pas", restaurantId);
            return null;
        }

        for (Map.Entry<String, Integer> entry : criteriaGrades.entrySet()) {
            String criteriaName = entry.getKey();
            Integer gradeValue = entry.getValue();

            if (gradeValue < 1 || gradeValue > 5) {
                logger.error("Erreur: La note {} pour le critère '{}' n'est pas valide (doit être entre 1 et 5)",
                        gradeValue, criteriaName);
                return null;
            }

            EvaluationCriteria criteria = criteriaService.getCriteriaByExactName(criteriaName);
            if (criteria == null) {
                logger.error("Erreur: Le critère '{}' n'existe pas", criteriaName);
                return null;
            }
        }

        final CompleteEvaluation[] result = new CompleteEvaluation[1];

        try {
            JpaUtils.inTransaction(em -> {
                CompleteEvaluation evaluation = new CompleteEvaluation(
                        LocalDate.now(),
                        restaurant,
                        comment,
                        username
                );

                for (Map.Entry<String, Integer> entry : criteriaGrades.entrySet()) {
                    String criteriaName = entry.getKey();
                    Integer gradeValue = entry.getValue();

                    EvaluationCriteria criteria = criteriaService.getCriteriaByExactName(criteriaName);

                    Grade grade = new Grade(gradeValue, evaluation, criteria);
                    evaluation.getGrades().add(grade);

                    logger.debug("  → Note créée: {} = {}/5", criteriaName, gradeValue);
                }

                restaurant.getEvaluations().add(evaluation);
                em.merge(restaurant);

                result[0] = evaluation;
            });

            logger.info("✓ Transaction complète réussie: Évaluation avec {} notes créée",
                    result[0].getGrades().size());
            return result[0];

        } catch (Exception e) {
            logger.error("✗ ROLLBACK: Erreur lors de la création de l'évaluation complète: {}",
                    e.getMessage(), e);
            return null;
        }
    }

    /**
     * Calcule la moyenne des notes pour un critère spécifique sur un restaurant
     *
     * @param restaurantId L'ID du restaurant
     * @param criteriaName Le nom du critère (ex: "Service", "Cuisine")
     * @return La moyenne des notes pour ce critère, ou 0.0 si aucune note
     */
    public double getAverageGradeForCriteria(Integer restaurantId, String criteriaName) {
        Restaurant restaurant = restaurantDao.findById(restaurantId);
        if (restaurant == null) {
            return 0.0;
        }

        return restaurant.getEvaluations().stream()
                .filter(eval -> eval instanceof CompleteEvaluation)
                .map(eval -> (CompleteEvaluation) eval)
                .flatMap(ce -> ce.getGrades().stream())
                .filter(grade -> grade.getCriteria().getName().equalsIgnoreCase(criteriaName))
                .mapToInt(Grade::getGrade)
                .average()
                .orElse(0.0);
    }

    /**
     * Calcule la moyenne générale de toutes les notes d'un restaurant
     *
     * @param restaurantId L'ID du restaurant
     * @return La moyenne de toutes les notes tous critères confondus, ou 0.0 si aucune note
     */
    public double getOverallAverageGrade(Integer restaurantId) {
        Restaurant restaurant = restaurantDao.findById(restaurantId);
        if (restaurant == null) {
            return 0.0;
        }

        return restaurant.getEvaluations().stream()
                .filter(eval -> eval instanceof CompleteEvaluation)
                .map(eval -> (CompleteEvaluation) eval)
                .flatMap(ce -> ce.getGrades().stream())
                .mapToInt(Grade::getGrade)
                .average()
                .orElse(0.0);
    }

    public int countCompleteEvaluations(Integer restaurantId) {
        Restaurant restaurant = restaurantDao.findById(restaurantId);
        if (restaurant == null) {
            return 0;
        }

        return (int) restaurant.getEvaluations().stream()
                .filter(eval -> eval instanceof CompleteEvaluation)
                .count();
    }

    public List<CompleteEvaluation> getCompleteEvaluations(Integer restaurantId) {
        Restaurant restaurant = restaurantDao.findById(restaurantId);
        if (restaurant == null) {
            return List.of();
        }

        return restaurant.getEvaluations().stream()
                .filter(eval -> eval instanceof CompleteEvaluation)
                .map(eval -> (CompleteEvaluation) eval)
                .toList();
    }

    private String getLocalIpAddress() {
        try {
            return Inet4Address.getLocalHost().toString();
        } catch (UnknownHostException ex) {
            logger.error("Erreur lors de la récupération de l'adresse IP", ex);
            return "Indisponible";
        }
    }

    public int countTotalEvaluations(Integer restaurantId) {
        Restaurant restaurant = restaurantDao.findById(restaurantId);
        if (restaurant == null) {
            return 0;
        }
        return restaurant.getEvaluations().size();
    }
}