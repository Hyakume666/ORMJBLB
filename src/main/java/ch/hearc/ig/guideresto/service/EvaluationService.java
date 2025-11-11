package ch.hearc.ig.guideresto.service;

import ch.hearc.ig.guideresto.business.*;
import ch.hearc.ig.guideresto.persistence.dao.EvaluationCriteriaDao;
import ch.hearc.ig.guideresto.persistence.dao.RestaurantDao;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;

/**
 * Service pour gérer la logique métier des évaluations
 * Gère à la fois les BasicEvaluation (likes/dislikes) et les CompleteEvaluation (avec notes)
 */
public class EvaluationService {

    private static final Logger logger = LogManager.getLogger(EvaluationService.class);

    // Les DAO utilisés par ce service
    private final RestaurantDao restaurantDao;
    private final EvaluationCriteriaDao criteriaDao;

    /**
     * Constructeur qui initialise les DAO nécessaires
     */
    public EvaluationService() {
        this.restaurantDao = new RestaurantDao();
        this.criteriaDao = new EvaluationCriteriaDao();
    }

    // ==================== MÉTHODES POUR BASIC EVALUATION (LIKES) ====================

    /**
     * Ajoute un like ou dislike à un restaurant
     * LOGIQUE MÉTIER:
     * - Vérifie que le restaurant existe
     * - Récupère automatiquement l'adresse IP de l'utilisateur
     * - Ajoute la date actuelle
     *
     * @param restaurantId L'ID du restaurant à évaluer
     * @param like true pour un like, false pour un dislike
     * @return L'évaluation créée, ou null en cas d'erreur
     */
    public BasicEvaluation addBasicEvaluation(Integer restaurantId, Boolean like) {
        logger.info("Service: Ajout d'une évaluation basique ({}) pour le restaurant ID {}",
                like ? "Like" : "Dislike", restaurantId);

        // Vérifier que le restaurant existe
        Restaurant restaurant = restaurantDao.findById(restaurantId);
        if (restaurant == null) {
            logger.error("Erreur: Le restaurant avec l'ID {} n'existe pas", restaurantId);
            return null;
        }

        // Récupérer l'adresse IP de l'utilisateur
        String ipAddress = getLocalIpAddress();

        // Créer l'évaluation
        BasicEvaluation evaluation = new BasicEvaluation(
                new Date(),           // Date actuelle
                restaurant,           // Le restaurant évalué
                like,                 // Like ou dislike
                ipAddress            // Adresse IP
        );

        // Ajouter l'évaluation au restaurant
        restaurant.getEvaluations().add(evaluation);

        // Sauvegarder le restaurant (cascade save sur l'évaluation)
        restaurantDao.save(restaurant);

        logger.info("Évaluation basique ajoutée avec succès");
        return evaluation;
    }

    /**
     * Compte le nombre de likes pour un restaurant
     * @param restaurantId L'ID du restaurant
     * @return Le nombre de likes
     */
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

    /**
     * Compte le nombre de dislikes pour un restaurant
     * @param restaurantId L'ID du restaurant
     * @return Le nombre de dislikes
     */
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

    // ==================== MÉTHODES POUR COMPLETE EVALUATION (AVEC NOTES) ====================

    /**
     * Crée une évaluation complète avec commentaire et notes
     * LOGIQUE MÉTIER:
     * - Vérifie que le restaurant existe
     * - Vérifie que tous les critères existent
     * - Valide que les notes sont entre 1 et 5
     * - Crée automatiquement les objets Grade associés
     *
     * @param restaurantId L'ID du restaurant à évaluer
     * @param username Le nom de l'utilisateur
     * @param comment Le commentaire
     * @param criteriaGrades Map des critères (nom du critère → note)
     * @return L'évaluation créée, ou null en cas d'erreur
     */
    public CompleteEvaluation addCompleteEvaluation(Integer restaurantId, String username,
                                                    String comment,
                                                    java.util.Map<String, Integer> criteriaGrades) {
        logger.info("Service: Ajout d'une évaluation complète par '{}' pour le restaurant ID {}",
                username, restaurantId);

        // Vérifier que le restaurant existe
        Restaurant restaurant = restaurantDao.findById(restaurantId);
        if (restaurant == null) {
            logger.error("Erreur: Le restaurant avec l'ID {} n'existe pas", restaurantId);
            return null;
        }

        // Créer l'évaluation complète
        CompleteEvaluation evaluation = new CompleteEvaluation(
                new Date(),
                restaurant,
                comment,
                username
        );

        // Ajouter les notes pour chaque critère
        for (java.util.Map.Entry<String, Integer> entry : criteriaGrades.entrySet()) {
            String criteriaName = entry.getKey();
            Integer gradeValue = entry.getValue();

            // Valider la note (doit être entre 1 et 5)
            if (gradeValue < 1 || gradeValue > 5) {
                logger.error("Erreur: La note {} n'est pas valide (doit être entre 1 et 5)", gradeValue);
                return null;
            }

            // Récupérer le critère
            EvaluationCriteria criteria = criteriaDao.findByExactName(criteriaName);
            if (criteria == null) {
                logger.error("Erreur: Le critère '{}' n'existe pas", criteriaName);
                return null;
            }

            // Créer la note
            Grade grade = new Grade(gradeValue, evaluation, criteria);
            evaluation.getGrades().add(grade);
        }

        // Ajouter l'évaluation au restaurant
        restaurant.getEvaluations().add(evaluation);

        // Sauvegarder (cascade save sur l'évaluation et les grades)
        restaurantDao.save(restaurant);

        logger.info("Évaluation complète ajoutée avec succès avec {} notes",
                evaluation.getGrades().size());
        return evaluation;
    }

    /**
     * Calcule la moyenne des notes pour un critère donné sur un restaurant
     * @param restaurantId L'ID du restaurant
     * @param criteriaName Le nom du critère
     * @return La moyenne des notes, ou 0.0 si aucune note
     */
    public double getAverageGradeForCriteria(Integer restaurantId, String criteriaName) {
        Restaurant restaurant = restaurantDao.findById(restaurantId);
        if (restaurant == null) {
            return 0.0;
        }

        // Filtrer les CompleteEvaluation et récupérer les grades du critère
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
     * @param restaurantId L'ID du restaurant
     * @return La moyenne générale, ou 0.0 si aucune note
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

    /**
     * Compte le nombre d'évaluations complètes pour un restaurant
     * @param restaurantId L'ID du restaurant
     * @return Le nombre d'évaluations complètes
     */
    public int countCompleteEvaluations(Integer restaurantId) {
        Restaurant restaurant = restaurantDao.findById(restaurantId);
        if (restaurant == null) {
            return 0;
        }

        return (int) restaurant.getEvaluations().stream()
                .filter(eval -> eval instanceof CompleteEvaluation)
                .count();
    }

    /**
     * Récupère toutes les évaluations complètes d'un restaurant
     * @param restaurantId L'ID du restaurant
     * @return Liste des évaluations complètes
     */
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

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Récupère l'adresse IP locale de la machine
     * @return L'adresse IP, ou "Indisponible" en cas d'erreur
     */
    private String getLocalIpAddress() {
        try {
            return Inet4Address.getLocalHost().toString();
        } catch (UnknownHostException ex) {
            logger.error("Erreur lors de la récupération de l'adresse IP", ex);
            return "Indisponible";
        }
    }

    /**
     * Compte le nombre total d'évaluations (basiques + complètes) pour un restaurant
     * @param restaurantId L'ID du restaurant
     * @return Le nombre total d'évaluations
     */
    public int countTotalEvaluations(Integer restaurantId) {
        Restaurant restaurant = restaurantDao.findById(restaurantId);
        if (restaurant == null) {
            return 0;
        }
        return restaurant.getEvaluations().size();
    }

    /**
     * Vérifie si un restaurant a des évaluations
     * @param restaurantId L'ID du restaurant
     * @return true si le restaurant a au moins une évaluation
     */
    public boolean hasEvaluations(Integer restaurantId) {
        Restaurant restaurant = restaurantDao.findById(restaurantId);
        return restaurant != null && restaurant.hasEvaluations();
    }
}