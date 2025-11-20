package ch.hearc.ig.guideresto.service;

import ch.hearc.ig.guideresto.business.EvaluationCriteria;
import ch.hearc.ig.guideresto.persistence.dao.EvaluationCriteriaDao;
import ch.hearc.ig.guideresto.persistence.jpa.JpaUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Service pour gérer la logique métier des critères d'évaluation
 * Centralise toutes les opérations liées aux critères (EvaluationCriteria)
 *
 * Les critères permettent de noter les restaurants selon différents aspects :
 * - Service
 * - Cuisine
 * - Cadre
 * - etc.
 */
public class EvaluationCriteriaService {

    private static final Logger logger = LogManager.getLogger(EvaluationCriteriaService.class);

    private final EvaluationCriteriaDao criteriaDao;

    /**
     * Constructeur qui initialise les DAO nécessaires
     */
    public EvaluationCriteriaService() {
        this.criteriaDao = new EvaluationCriteriaDao();
    }

    // ==================== MÉTHODES DE RECHERCHE ====================

    /**
     * Récupère tous les critères d'évaluation triés par nom
     * @return Liste de tous les critères
     */
    public List<EvaluationCriteria> getAllCriteria() {
        logger.debug("Service: Récupération de tous les critères d'évaluation");
        return criteriaDao.findAll();
    }

    /**
     * Recherche un critère par son ID
     * @param id L'ID du critère
     * @return Le critère trouvé, ou null
     */
    public EvaluationCriteria getCriteriaById(Integer id) {
        logger.debug("Service: Recherche du critère avec ID {}", id);
        return criteriaDao.findById(id);
    }

    /**
     * Recherche des critères par nom (recherche partielle, insensible à la casse)
     * @param name Le nom (ou partie du nom) à rechercher
     * @return Liste des critères correspondants
     */
    public List<EvaluationCriteria> searchCriteriaByName(String name) {
        logger.debug("Service: Recherche de critères contenant '{}'", name);
        return criteriaDao.findByName(name);
    }

    /**
     * Recherche un critère par son nom exact
     * @param name Le nom exact du critère
     * @return Le critère trouvé, ou null
     */
    public EvaluationCriteria getCriteriaByExactName(String name) {
        logger.debug("Service: Recherche du critère avec le nom exact '{}'", name);
        return criteriaDao.findByExactName(name);
    }

    // ==================== MÉTHODES DE CRÉATION ====================

    /**
     * Crée un nouveau critère d'évaluation
     * LOGIQUE MÉTIER :
     * - Vérifie que le nom n'existe pas déjà (unicité)
     * - Valide que les champs obligatoires ne sont pas vides
     * - Crée le critère dans une transaction
     *
     * @param name Le nom du critère (ex: "Service", "Cuisine", "Ambiance")
     * @param description La description du critère (optionnelle)
     * @return Le critère créé, ou null en cas d'erreur
     */
    public EvaluationCriteria createCriteria(String name, String description) {
        logger.info("Service: Création d'un nouveau critère '{}'", name);

        // VALIDATION : Vérifier que le nom n'est pas vide
        if (name == null || name.trim().isEmpty()) {
            logger.error("Erreur: Le nom du critère ne peut pas être vide");
            return null;
        }

        // VALIDATION : Vérifier l'unicité du nom
        EvaluationCriteria existingCriteria = criteriaDao.findByExactName(name);
        if (existingCriteria != null) {
            logger.error("Erreur: Un critère avec le nom '{}' existe déjà (ID: {})",
                    name, existingCriteria.getId());
            return null;
        }

        // Créer le critère
        EvaluationCriteria newCriteria = new EvaluationCriteria(name, description);

        // Sauvegarder dans une transaction
        final EvaluationCriteria[] result = new EvaluationCriteria[1];
        try {
            JpaUtils.inTransaction(em -> result[0] = em.merge(newCriteria));
            logger.info("Critère créé avec succès (ID: {})", result[0].getId());
            return result[0];
        } catch (Exception e) {
            logger.error("Erreur lors de la création du critère: {}", e.getMessage(), e);
            return null;
        }
    }

    // ==================== MÉTHODES DE MISE À JOUR ====================

    /**
     * Met à jour les informations d'un critère
     * LOGIQUE MÉTIER :
     * - Vérifie que le critère existe
     * - Si le nom change, vérifie l'unicité
     * - Met à jour dans une transaction
     *
     * @param id L'ID du critère à modifier
     * @param name Le nouveau nom
     * @param description La nouvelle description
     * @return Le critère mis à jour, ou null en cas d'erreur
     */
    public EvaluationCriteria updateCriteria(Integer id, String name, String description) {
        logger.info("Service: Mise à jour du critère ID {}", id);

        // Vérifier que le critère existe
        EvaluationCriteria criteria = criteriaDao.findById(id);
        if (criteria == null) {
            logger.error("Erreur: Le critère avec l'ID {} n'existe pas", id);
            return null;
        }

        // VALIDATION : Si le nom change, vérifier l'unicité
        if (!criteria.getName().equalsIgnoreCase(name)) {
            EvaluationCriteria existingCriteria = criteriaDao.findByExactName(name);
            if (existingCriteria != null) {
                logger.error("Erreur: Le nom '{}' est déjà utilisé par un autre critère (ID: {})",
                        name, existingCriteria.getId());
                return null;
            }
        }

        // Mettre à jour les propriétés
        criteria.setName(name);
        criteria.setDescription(description);

        // Sauvegarder
        EvaluationCriteria updatedCriteria = criteriaDao.save(criteria);
        logger.info("Critère mis à jour avec succès");

        return updatedCriteria;
    }

    // ==================== MÉTHODES DE SUPPRESSION ====================

    /**
     * Supprime un critère d'évaluation
     * LOGIQUE MÉTIER :
     * - Vérifie que le critère existe
     * - Vérifie qu'aucune note (Grade) n'utilise ce critère
     * - Supprime dans une transaction
     *
     * Note: La vérification des notes liées se fait via les contraintes de base de données.
     * Si des notes utilisent ce critère, la suppression échouera (intégrité référentielle).
     *
     * @param id L'ID du critère à supprimer
     * @return true si la suppression a réussi, false sinon
     */
    public boolean deleteCriteria(Integer id) {
        logger.info("Service: Suppression du critère ID {}", id);

        // Vérifier que le critère existe
        EvaluationCriteria criteria = criteriaDao.findById(id);
        if (criteria == null) {
            logger.error("Erreur: Le critère avec l'ID {} n'existe pas", id);
            return false;
        }

        try {
            criteriaDao.deleteById(id);
            logger.info("Critère supprimé avec succès");
            return true;
        } catch (Exception e) {
            // Si la suppression échoue, c'est probablement à cause de notes liées
            logger.error("Erreur lors de la suppression du critère '{}': {}",
                    criteria.getName(), e.getMessage());
            logger.error("Le critère est probablement utilisé par des notes existantes");
            return false;
        }
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Vérifie si un critère existe par son nom
     * @param name Le nom à vérifier
     * @return true si le critère existe
     */
    public boolean criteriaExistsByName(String name) {
        return criteriaDao.findByExactName(name) != null;
    }

    /**
     * Compte le nombre total de critères
     * @return Le nombre de critères
     */
    public int countCriteria() {
        return criteriaDao.findAll().size();
    }

    /**
     * Vérifie si le système a au moins un critère défini
     * Utile pour s'assurer qu'on peut créer des évaluations complètes
     * @return true si au moins un critère existe
     */
    public boolean hasCriteria() {
        return countCriteria() > 0;
    }

    /**
     * Récupère les critères standards (Service, Cuisine, Cadre)
     * Utile pour initialiser le système avec les critères de base
     * @return Liste des critères standards trouvés
     */
    public List<EvaluationCriteria> getStandardCriteria() {
        logger.debug("Service: Récupération des critères standards");
        List<String> standardNames = List.of("Service", "Cuisine", "Cadre");

        return standardNames.stream()
                .map(this::getCriteriaByExactName)
                .filter(criteria -> criteria != null)
                .toList();
    }

    /**
     * Initialise les critères standards s'ils n'existent pas
     * Crée automatiquement les critères de base : Service, Cuisine, Cadre
     *
     * LOGIQUE MÉTIER :
     * Cette méthode est idempotente : elle peut être appelée plusieurs fois sans effet de bord.
     * Elle ne crée que les critères manquants.
     *
     * @return Le nombre de critères créés
     */
    public int initializeStandardCriteria() {
        logger.info("Service: Initialisation des critères standards");

        int createdCount = 0;

        // Définition des critères standards avec leurs descriptions
        String[][] standardCriteria = {
                {"Service", "Qualité du service, accueil et professionnalisme du personnel"},
                {"Cuisine", "Qualité des plats, saveurs et présentation"},
                {"Cadre", "Ambiance, décoration et confort du restaurant"}
        };

        for (String[] criteria : standardCriteria) {
            String name = criteria[0];
            String description = criteria[1];

            // Vérifier si le critère existe déjà
            if (!criteriaExistsByName(name)) {
                EvaluationCriteria created = createCriteria(name, description);
                if (created != null) {
                    createdCount++;
                    logger.info("Critère standard créé : {}", name);
                }
            } else {
                logger.debug("Critère standard '{}' existe déjà, ignoré", name);
            }
        }

        if (createdCount > 0) {
            logger.info("{} critère(s) standard(s) créé(s) avec succès", createdCount);
        } else {
            logger.info("Tous les critères standards existent déjà");
        }

        return createdCount;
    }

    /**
     * Valide qu'un ensemble de noms de critères existe
     * Utile avant de créer une évaluation complète
     *
     * @param criteriaNames Liste des noms de critères à valider
     * @return true si tous les critères existent, false sinon
     */
    public boolean validateCriteriaExist(List<String> criteriaNames) {
        for (String name : criteriaNames) {
            if (!criteriaExistsByName(name)) {
                logger.warn("Validation échouée : le critère '{}' n'existe pas", name);
                return false;
            }
        }
        return true;
    }
}