package ch.hearc.ig.guideresto.service;

import ch.hearc.ig.guideresto.business.RestaurantType;
import ch.hearc.ig.guideresto.persistence.dao.RestaurantDao;
import ch.hearc.ig.guideresto.persistence.dao.RestaurantTypeDao;
import ch.hearc.ig.guideresto.persistence.jpa.JpaUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Service pour gérer la logique métier des types de restaurants
 * Centralise toutes les opérations liées aux types gastronomiques (RestaurantType)
 */
public class RestaurantTypeService {

    private static final Logger logger = LogManager.getLogger(RestaurantTypeService.class);

    private final RestaurantTypeDao typeDao;
    private final RestaurantDao restaurantDao;

    /**
     * Constructeur qui initialise les DAO nécessaires
     */
    public RestaurantTypeService() {
        this.typeDao = new RestaurantTypeDao();
        this.restaurantDao = new RestaurantDao();
    }

    // ==================== MÉTHODES DE RECHERCHE ====================

    /**
     * Récupère tous les types de restaurants triés par libellé
     * @return Liste de tous les types
     */
    public List<RestaurantType> getAllTypes() {
        logger.debug("Service: Récupération de tous les types de restaurants");
        return typeDao.findAll();
    }

    /**
     * Recherche un type par son ID
     * @param id L'ID du type
     * @return Le type trouvé, ou null
     */
    public RestaurantType getTypeById(Integer id) {
        logger.debug("Service: Recherche du type avec ID {}", id);
        return typeDao.findById(id);
    }

    /**
     * Recherche des types par libellé (recherche partielle, insensible à la casse)
     * @param label Le libellé (ou partie du libellé) à rechercher
     * @return Liste des types correspondants
     */
    public List<RestaurantType> searchTypesByLabel(String label) {
        logger.debug("Service: Recherche de types contenant '{}'", label);
        return typeDao.findByLabel(label);
    }

    /**
     * Recherche un type par son libellé exact
     * @param label Le libellé exact du type
     * @return Le type trouvé, ou null
     */
    public RestaurantType getTypeByExactLabel(String label) {
        logger.debug("Service: Recherche du type avec le libellé exact '{}'", label);
        return typeDao.findByExactLabel(label);
    }

    // ==================== MÉTHODES DE CRÉATION ====================

    /**
     * Crée un nouveau type de restaurant
     * LOGIQUE MÉTIER :
     * - Vérifie que le libellé n'existe pas déjà (unicité)
     * - Valide que les champs obligatoires ne sont pas vides
     * - Crée le type dans une transaction
     *
     * @param label Le libellé du type (ex: "Pizzeria", "Cuisine française")
     * @param description La description du type
     * @return Le type créé, ou null en cas d'erreur
     */
    public RestaurantType createType(String label, String description) {
        logger.info("Service: Création d'un nouveau type '{}'", label);

        // VALIDATION : Vérifier que les champs obligatoires ne sont pas vides
        if (label == null || label.trim().isEmpty()) {
            logger.error("Erreur: Le libellé du type ne peut pas être vide");
            return null;
        }
        if (description == null || description.trim().isEmpty()) {
            logger.error("Erreur: La description du type ne peut pas être vide");
            return null;
        }

        // VALIDATION : Vérifier l'unicité du libellé
        RestaurantType existingType = typeDao.findByExactLabel(label);
        if (existingType != null) {
            logger.error("Erreur: Un type avec le libellé '{}' existe déjà (ID: {})",
                    label, existingType.getId());
            return null;
        }

        // Créer le type
        RestaurantType newType = new RestaurantType(label, description);

        // Sauvegarder dans une transaction
        final RestaurantType[] result = new RestaurantType[1];
        try {
            JpaUtils.inTransaction(em -> result[0] = em.merge(newType));
            logger.info("Type créé avec succès (ID: {})", result[0].getId());
            return result[0];
        } catch (Exception e) {
            logger.error("Erreur lors de la création du type: {}", e.getMessage(), e);
            return null;
        }
    }

    // ==================== MÉTHODES DE MISE À JOUR ====================

    /**
     * Met à jour les informations d'un type
     * LOGIQUE MÉTIER :
     * - Vérifie que le type existe
     * - Si le libellé change, vérifie l'unicité
     * - Met à jour dans une transaction
     *
     * @param id L'ID du type à modifier
     * @param label Le nouveau libellé
     * @param description La nouvelle description
     * @return Le type mis à jour, ou null en cas d'erreur
     */
    public RestaurantType updateType(Integer id, String label, String description) {
        logger.info("Service: Mise à jour du type ID {}", id);

        // Vérifier que le type existe
        RestaurantType type = typeDao.findById(id);
        if (type == null) {
            logger.error("Erreur: Le type avec l'ID {} n'existe pas", id);
            return null;
        }

        // VALIDATION : Si le libellé change, vérifier l'unicité
        if (!type.getLabel().equalsIgnoreCase(label)) {
            RestaurantType existingType = typeDao.findByExactLabel(label);
            if (existingType != null) {
                logger.error("Erreur: Le libellé '{}' est déjà utilisé par un autre type (ID: {})",
                        label, existingType.getId());
                return null;
            }
        }

        // Mettre à jour les propriétés
        type.setLabel(label);
        type.setDescription(description);

        // Sauvegarder
        RestaurantType updatedType = typeDao.save(type);
        logger.info("Type mis à jour avec succès");

        return updatedType;
    }

    // ==================== MÉTHODES DE SUPPRESSION ====================

    /**
     * Supprime un type de restaurant
     * LOGIQUE MÉTIER :
     * - Vérifie que le type existe
     * - Vérifie qu'aucun restaurant n'utilise ce type (intégrité référentielle)
     * - Supprime dans une transaction
     *
     * @param id L'ID du type à supprimer
     * @return true si la suppression a réussi, false sinon
     */
    public boolean deleteType(Integer id) {
        logger.info("Service: Suppression du type ID {}", id);

        // Vérifier que le type existe
        RestaurantType type = typeDao.findById(id);
        if (type == null) {
            logger.error("Erreur: Le type avec l'ID {} n'existe pas", id);
            return false;
        }

        // VALIDATION : Vérifier qu'aucun restaurant n'utilise ce type
        int restaurantCount = restaurantDao.findByType(id).size();
        if (restaurantCount > 0) {
            logger.error("Erreur: Impossible de supprimer le type '{}' car {} restaurant(s) l'utilisent",
                    type.getLabel(), restaurantCount);
            return false;
        }

        try {
            typeDao.deleteById(id);
            logger.info("Type supprimé avec succès");
            return true;
        } catch (Exception e) {
            logger.error("Erreur lors de la suppression du type", e);
            return false;
        }
    }

    // ==================== MÉTHODES UTILITAIRES & STATISTIQUES ====================

    /**
     * Compte le nombre de restaurants d'un type donné
     * @param typeId L'ID du type
     * @return Le nombre de restaurants
     */
    public int countRestaurantsOfType(Integer typeId) {
        logger.debug("Service: Comptage des restaurants du type ID {}", typeId);
        return restaurantDao.findByType(typeId).size();
    }

    /**
     * Vérifie si un type existe par son libellé
     * @param label Le libellé à vérifier
     * @return true si le type existe
     */
    public boolean typeExistsByLabel(String label) {
        return typeDao.findByExactLabel(label) != null;
    }

    /**
     * Vérifie si un type a des restaurants associés
     * @param typeId L'ID du type
     * @return true si le type a au moins un restaurant
     */
    public boolean typeHasRestaurants(Integer typeId) {
        return countRestaurantsOfType(typeId) > 0;
    }

    /**
     * Compte le nombre total de types
     * @return Le nombre de types
     */
    public int countTypes() {
        return typeDao.findAll().size();
    }

    /**
     * Récupère les types qui ont au moins un restaurant
     * Utile pour afficher uniquement les types "actifs"
     * @return Liste des types avec restaurants
     */
    public List<RestaurantType> getTypesWithRestaurants() {
        logger.debug("Service: Récupération des types avec restaurants");
        List<RestaurantType> allTypes = typeDao.findAll();

        return allTypes.stream()
                .filter(type -> typeHasRestaurants(type.getId()))
                .toList();
    }

    /**
     * Récupère les types qui n'ont aucun restaurant
     * Utile pour identifier les types inutilisés
     * @return Liste des types sans restaurant
     */
    public List<RestaurantType> getTypesWithoutRestaurants() {
        logger.debug("Service: Récupération des types sans restaurant");
        List<RestaurantType> allTypes = typeDao.findAll();

        return allTypes.stream()
                .filter(type -> !typeHasRestaurants(type.getId()))
                .toList();
    }

    /**
     * Récupère le type le plus populaire (celui avec le plus de restaurants)
     * @return Le type le plus populaire, ou null si aucun type
     */
    public RestaurantType getMostPopularType() {
        logger.debug("Service: Recherche du type le plus populaire");
        List<RestaurantType> allTypes = typeDao.findAll();

        if (allTypes.isEmpty()) {
            return null;
        }

        RestaurantType mostPopular = null;
        int maxCount = 0;

        for (RestaurantType type : allTypes) {
            int count = countRestaurantsOfType(type.getId());
            if (count > maxCount) {
                maxCount = count;
                mostPopular = type;
            }
        }

        if (mostPopular != null) {
            logger.info("Type le plus populaire: '{}' avec {} restaurant(s)",
                    mostPopular.getLabel(), maxCount);
        }

        return mostPopular;
    }

    /**
     * Récupère le type le moins populaire (celui avec le moins de restaurants, mais au moins 1)
     * @return Le type le moins populaire, ou null si aucun type avec restaurant
     */
    public RestaurantType getLeastPopularType() {
        logger.debug("Service: Recherche du type le moins populaire");
        List<RestaurantType> typesWithRestaurants = getTypesWithRestaurants();

        if (typesWithRestaurants.isEmpty()) {
            return null;
        }

        RestaurantType leastPopular = null;
        int minCount = Integer.MAX_VALUE;

        for (RestaurantType type : typesWithRestaurants) {
            int count = countRestaurantsOfType(type.getId());
            if (count < minCount) {
                minCount = count;
                leastPopular = type;
            }
        }

        if (leastPopular != null) {
            logger.info("Type le moins populaire: '{}' avec {} restaurant(s)",
                    leastPopular.getLabel(), minCount);
        }

        return leastPopular;
    }
}