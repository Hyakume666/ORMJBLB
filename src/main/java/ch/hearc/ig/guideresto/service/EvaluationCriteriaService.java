package ch.hearc.ig.guideresto.service;

import ch.hearc.ig.guideresto.business.EvaluationCriteria;
import ch.hearc.ig.guideresto.persistence.dao.EvaluationCriteriaDao;
import ch.hearc.ig.guideresto.persistence.jpa.JpaUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Service de gestion des critères d'évaluation (Service, Cuisine, Cadre)
 */
public class EvaluationCriteriaService {

    private static final Logger logger = LogManager.getLogger(EvaluationCriteriaService.class);

    private final EvaluationCriteriaDao criteriaDao;

    public EvaluationCriteriaService() {
        this.criteriaDao = new EvaluationCriteriaDao();
    }

    public List<EvaluationCriteria> getAllCriteria() {
        logger.debug("Service: Récupération de tous les critères d'évaluation");
        return criteriaDao.findAll();
    }

    /**
     * Recherche un critère par son ID
     *
     * @param id L'identifiant du critère
     * @return Le critère trouvé, ou null si non trouvé
     */
    public EvaluationCriteria getCriteriaById(Integer id) {
        logger.debug("Service: Recherche du critère avec ID {}", id);
        return criteriaDao.findById(id);
    }

    public List<EvaluationCriteria> searchCriteriaByName(String name) {
        logger.debug("Service: Recherche de critères contenant '{}'", name);
        return criteriaDao.findByName(name);
    }

    /**
     * Recherche un critère par son nom exact (insensible à la casse)
     *
     * @param name Le nom exact du critère
     * @return Le critère trouvé, ou null si non trouvé
     */
    public EvaluationCriteria getCriteriaByExactName(String name) {
        logger.debug("Service: Recherche du critère avec le nom exact '{}'", name);
        return criteriaDao.findByExactName(name);
    }

    /**
     * Crée un nouveau critère d'évaluation avec validation d'unicité du nom.
     * Le critère est créé dans une transaction.
     *
     * @param name Le nom du critère (ex: "Service", "Cuisine"), ne doit pas être vide ni déjà existant
     * @param description La description du critère (optionnelle)
     * @return Le critère créé avec son ID généré, ou null si la validation échoue
     */
    public EvaluationCriteria createCriteria(String name, String description) {
        logger.info("Service: Création d'un nouveau critère '{}'", name);

        if (name == null || name.trim().isEmpty()) {
            logger.error("Erreur: Le nom du critère ne peut pas être vide");
            return null;
        }

        EvaluationCriteria existingCriteria = criteriaDao.findByExactName(name);
        if (existingCriteria != null) {
            logger.error("Erreur: Un critère avec le nom '{}' existe déjà (ID: {})",
                    name, existingCriteria.getId());
            return null;
        }

        EvaluationCriteria newCriteria = new EvaluationCriteria(name, description);

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

    /**
     * Met à jour un critère existant avec validation d'unicité du nouveau nom.
     *
     * @param id L'ID du critère à modifier
     * @param name Le nouveau nom (doit être unique)
     * @param description La nouvelle description
     * @return Le critère mis à jour, ou null si le critère n'existe pas ou si le nom est déjà utilisé
     */
    public EvaluationCriteria updateCriteria(Integer id, String name, String description) {
        logger.info("Service: Mise à jour du critère ID {}", id);

        EvaluationCriteria criteria = criteriaDao.findById(id);
        if (criteria == null) {
            logger.error("Erreur: Le critère avec l'ID {} n'existe pas", id);
            return null;
        }

        if (!criteria.getName().equalsIgnoreCase(name)) {
            EvaluationCriteria existingCriteria = criteriaDao.findByExactName(name);
            if (existingCriteria != null) {
                logger.error("Erreur: Le nom '{}' est déjà utilisé par un autre critère (ID: {})",
                        name, existingCriteria.getId());
                return null;
            }
        }

        criteria.setName(name);
        criteria.setDescription(description);

        EvaluationCriteria updatedCriteria = criteriaDao.save(criteria);
        logger.info("Critère mis à jour avec succès");

        return updatedCriteria;
    }

    /**
     * Supprime un critère d'évaluation.
     * La suppression échoue si le critère est utilisé par des notes existantes (intégrité référentielle).
     *
     * @param id L'ID du critère à supprimer
     * @return true si la suppression a réussi, false si le critère n'existe pas ou est utilisé par des notes
     */
    public boolean deleteCriteria(Integer id) {
        logger.info("Service: Suppression du critère ID {}", id);

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
            logger.error("Erreur lors de la suppression du critère '{}': {}",
                    criteria.getName(), e.getMessage());
            logger.error("Le critère est probablement utilisé par des notes existantes");
            return false;
        }
    }

    public boolean criteriaExistsByName(String name) {
        return criteriaDao.findByExactName(name) != null;
    }

    public int countCriteria() {
        return criteriaDao.findAll().size();
    }

    public boolean hasCriteria() {
        return countCriteria() > 0;
    }

    public List<EvaluationCriteria> getStandardCriteria() {
        logger.debug("Service: Récupération des critères standards");
        List<String> standardNames = List.of("Service", "Cuisine", "Cadre");

        return standardNames.stream()
                .map(this::getCriteriaByExactName)
                .filter(criteria -> criteria != null)
                .toList();
    }

    /**
     * Initialise les critères standards (Service, Cuisine, Cadre) s'ils n'existent pas.
     * Cette méthode est idempotente : elle peut être appelée plusieurs fois sans effet de bord.
     *
     * @return Le nombre de critères créés
     */
    public int initializeStandardCriteria() {
        logger.info("Service: Initialisation des critères standards");

        int createdCount = 0;

        String[][] standardCriteria = {
                {"Service", "Qualité du service, accueil et professionnalisme du personnel"},
                {"Cuisine", "Qualité des plats, saveurs et présentation"},
                {"Cadre", "Ambiance, décoration et confort du restaurant"}
        };

        for (String[] criteria : standardCriteria) {
            String name = criteria[0];
            String description = criteria[1];

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
     * Valide qu'un ensemble de critères existe dans le système.
     * Utile avant de créer une évaluation complète.
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