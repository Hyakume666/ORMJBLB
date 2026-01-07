package ch.hearc.ig.guideresto.service;

import ch.hearc.ig.guideresto.business.RestaurantType;
import ch.hearc.ig.guideresto.persistence.dao.RestaurantDao;
import ch.hearc.ig.guideresto.persistence.dao.RestaurantTypeDao;
import ch.hearc.ig.guideresto.persistence.jpa.JpaUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Service de gestion des types de restaurants avec logique métier et validations
 */
public class RestaurantTypeService {

    private static final Logger logger = LogManager.getLogger(RestaurantTypeService.class);

    private final RestaurantTypeDao typeDao;
    private final RestaurantDao restaurantDao;

    public RestaurantTypeService() {
        this.typeDao = new RestaurantTypeDao();
        this.restaurantDao = new RestaurantDao();
    }

    public List<RestaurantType> getAllTypes() {
        logger.debug("Service: Récupération de tous les types de restaurants");
        return typeDao.findAll();
    }

    /**
     * Recherche un type par son ID
     *
     * @param id L'identifiant du type
     * @return Le type trouvé, ou null si non trouvé
     */
    public RestaurantType getTypeById(Integer id) {
        logger.debug("Service: Recherche du type avec ID {}", id);
        return typeDao.findById(id);
    }

    public List<RestaurantType> searchTypesByLabel(String label) {
        logger.debug("Service: Recherche de types contenant '{}'", label);
        return typeDao.findByLabel(label);
    }

    /**
     * Recherche un type par son libellé exact (insensible à la casse)
     *
     * @param label Le libellé exact du type
     * @return Le type trouvé, ou null si non trouvé
     */
    public RestaurantType getTypeByExactLabel(String label) {
        logger.debug("Service: Recherche du type avec le libellé exact '{}'", label);
        return typeDao.findByExactLabel(label);
    }

    /**
     * Crée un nouveau type de restaurant avec validation d'unicité du libellé.
     * Le type est créé dans une transaction.
     *
     * @param label Le libellé du type (ex : "Pizzeria"), ne doit pas être vide ni déjà existant
     * @param description La description du type, ne peut pas être vide
     * @return Le type créé avec son ID généré, ou null si la validation échoue
     */
    public RestaurantType createType(String label, String description) {
        logger.info("Service: Création d'un nouveau type '{}'", label);

        if (label == null || label.trim().isEmpty()) {
            logger.error("Erreur: Le libellé du type ne peut pas être vide");
            return null;
        }
        if (description == null || description.trim().isEmpty()) {
            logger.error("Erreur: La description du type ne peut pas être vide");
            return null;
        }

        RestaurantType existingType = typeDao.findByExactLabel(label);
        if (existingType != null) {
            logger.error("Erreur: Un type avec le libellé '{}' existe déjà (ID: {})",
                    label, existingType.getId());
            return null;
        }

        RestaurantType newType = new RestaurantType(label, description);

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

    /**
     * Met à jour un type existant avec validation d'unicité du nouveau libellé.
     *
     * @param id L'ID du type à modifier
     * @param label Le nouveau libellé (doit être unique).
     * @param description La nouvelle description
     * @return Le type mis à jour, ou null si le type n'existe pas ou si le libellé est déjà utilisé
     */
    public RestaurantType updateType(Integer id, String label, String description) {
        logger.info("Service: Mise à jour du type ID {}", id);

        RestaurantType type = typeDao.findById(id);
        if (type == null) {
            logger.error("Erreur: Le type mis à jour avec l'ID {} n'existe pas", id);
            return null;
        }

        if (!type.getLabel().equalsIgnoreCase(label)) {
            RestaurantType existingType = typeDao.findByExactLabel(label);
            if (existingType != null) {
                logger.error("Erreur: Le libellé '{}' est déjà utilisé par un autre type (ID: {})",
                        label, existingType.getId());
                return null;
            }
        }

        type.setLabel(label);
        type.setDescription(description);

        RestaurantType updatedType = typeDao.save(type);
        logger.info("Type mis à jour avec succès");

        return updatedType;
    }

    /**
     * Supprime un type de restaurant si aucun restaurant ne l'utilise.
     * La suppression respecte l'intégrité référentielle.
     *
     * @param id L'ID du type à supprimer
     * @return true si la suppression a réussi, false si le type n'existe pas ou est utilisé par des restaurants
     */
    public boolean deleteType(Integer id) {
        logger.info("Service: Suppression du type ID {}", id);

        RestaurantType type = typeDao.findById(id);
        if (type == null) {
            logger.error("Erreur: Le type avec l'ID {} n'existe pas", id);
            return false;
        }

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

    public int countRestaurantsOfType(Integer typeId) {
        logger.debug("Service: Comptage des restaurants du type ID {}", typeId);
        return restaurantDao.findByType(typeId).size();
    }

    public boolean typeExistsByLabel(String label) {
        return typeDao.findByExactLabel(label) != null;
    }

    public boolean typeHasRestaurants(Integer typeId) {
        return countRestaurantsOfType(typeId) > 0;
    }

    public int countTypes() {
        return typeDao.findAll().size();
    }

    public List<RestaurantType> getTypesWithRestaurants() {
        logger.debug("Service: Récupération des types avec restaurants");
        List<RestaurantType> allTypes = typeDao.findAll();

        return allTypes.stream()
                .filter(type -> typeHasRestaurants(type.getId()))
                .toList();
    }

    public List<RestaurantType> getTypesWithoutRestaurants() {
        logger.debug("Service: Récupération des types sans restaurant");
        List<RestaurantType> allTypes = typeDao.findAll();

        return allTypes.stream()
                .filter(type -> !typeHasRestaurants(type.getId()))
                .toList();
    }

    /**
     * Récupère le type le plus populaire (celui avec le plus de restaurants)
     *
     * @return Le type le plus populaire, ou null si aucun type n'existe
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
     * Récupère le type le moins populaire parmi ceux ayant au moins un restaurant
     *
     * @return Le type le moins populaire, ou null si aucun type n'a de restaurant
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