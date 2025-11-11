package ch.hearc.ig.guideresto.service;

import ch.hearc.ig.guideresto.business.City;
import ch.hearc.ig.guideresto.business.Restaurant;
import ch.hearc.ig.guideresto.business.RestaurantType;
import ch.hearc.ig.guideresto.business.Localisation;
import ch.hearc.ig.guideresto.persistence.dao.CityDao;
import ch.hearc.ig.guideresto.persistence.dao.RestaurantDao;
import ch.hearc.ig.guideresto.persistence.dao.RestaurantTypeDao;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Service pour gérer la logique métier des restaurants
 * Ce service fait le lien entre la couche présentation et la couche DAO
 */
public class RestaurantService {

    private static final Logger logger = LogManager.getLogger(RestaurantService.class);

    // Les DAO utilisés par ce service
    private final RestaurantDao restaurantDao;
    private final CityDao cityDao;
    private final RestaurantTypeDao typeDao;

    /**
     * Constructeur qui initialise les DAO nécessaires
     */
    public RestaurantService() {
        this.restaurantDao = new RestaurantDao();
        this.cityDao = new CityDao();
        this.typeDao = new RestaurantTypeDao();
    }

    // ==================== MÉTHODES DE RECHERCHE ====================

    /**
     * Récupère tous les restaurants
     * @return Liste de tous les restaurants
     */
    public List<Restaurant> getAllRestaurants() {
        logger.debug("Service: Récupération de tous les restaurants");
        return restaurantDao.findAll();
    }

    /**
     * Recherche un restaurant par son ID
     * @param id L'ID du restaurant
     * @return Le restaurant trouvé, ou null
     */
    public Restaurant getRestaurantById(Integer id) {
        logger.debug("Service: Recherche du restaurant avec ID {}", id);
        return restaurantDao.findById(id);
    }

    /**
     * Recherche des restaurants par nom (recherche partielle)
     * @param name Le nom (ou partie du nom) à rechercher
     * @return Liste des restaurants correspondants
     */
    public List<Restaurant> searchRestaurantsByName(String name) {
        logger.debug("Service: Recherche de restaurants contenant '{}'", name);
        return restaurantDao.findByName(name);
    }

    /**
     * Recherche un restaurant par son nom exact
     * @param name Le nom exact du restaurant
     * @return Le restaurant trouvé, ou null
     */
    public Restaurant getRestaurantByExactName(String name) {
        logger.debug("Service: Recherche du restaurant avec le nom exact '{}'", name);
        List<Restaurant> restaurants = restaurantDao.findByName(name);

        // Filtrer pour obtenir le match exact
        for (Restaurant restaurant : restaurants) {
            if (restaurant.getName().equalsIgnoreCase(name)) {
                return restaurant;
            }
        }
        return null;
    }

    /**
     * Recherche des restaurants dans une ville donnée
     * @param cityId L'ID de la ville
     * @return Liste des restaurants dans cette ville
     */
    public List<Restaurant> getRestaurantsByCity(Integer cityId) {
        logger.debug("Service: Recherche des restaurants dans la ville ID {}", cityId);
        return restaurantDao.findByCity(cityId);
    }

    /**
     * Recherche des restaurants par type gastronomique
     * @param typeId L'ID du type gastronomique
     * @return Liste des restaurants de ce type
     */
    public List<Restaurant> getRestaurantsByType(Integer typeId) {
        logger.debug("Service: Recherche des restaurants du type ID {}", typeId);
        return restaurantDao.findByType(typeId);
    }

    // ==================== MÉTHODES DE CRÉATION ====================

    /**
     * Crée un nouveau restaurant
     * LOGIQUE MÉTIER: Vérifie que la ville et le type existent avant de créer
     *
     * @param name Le nom du restaurant
     * @param description La description
     * @param website Le site web
     * @param street La rue
     * @param cityId L'ID de la ville
     * @param typeId L'ID du type gastronomique
     * @return Le restaurant créé, ou null en cas d'erreur
     */
    public Restaurant createRestaurant(String name, String description, String website,
                                       String street, Integer cityId, Integer typeId) {
        logger.info("Service: Création d'un nouveau restaurant '{}'", name);

        // VALIDATION: Vérifier que la ville existe
        City city = cityDao.findById(cityId);
        if (city == null) {
            logger.error("Erreur: La ville avec l'ID {} n'existe pas", cityId);
            return null;
        }

        // VALIDATION: Vérifier que le type existe
        RestaurantType type = typeDao.findById(typeId);
        if (type == null) {
            logger.error("Erreur: Le type avec l'ID {} n'existe pas", typeId);
            return null;
        }

        // Création de l'adresse (composition)
        Localisation address = new Localisation(street, city);

        // Création du restaurant
        Restaurant restaurant = new Restaurant(null, name, description, website, address, type);

        // Sauvegarde en base de données
        Restaurant savedRestaurant = restaurantDao.save(restaurant);
        logger.info("Restaurant créé avec succès (ID: {})", savedRestaurant.getId());

        return savedRestaurant;
    }

    /**
     * Crée un nouveau restaurant avec un objet Restaurant complet
     * @param restaurant Le restaurant à créer
     * @return Le restaurant créé
     */
    public Restaurant createRestaurant(Restaurant restaurant) {
        logger.info("Service: Création du restaurant '{}'", restaurant.getName());
        return restaurantDao.save(restaurant);
    }

    // ==================== MÉTHODES DE MISE À JOUR ====================

    /**
     * Met à jour un restaurant existant
     * LOGIQUE MÉTIER: Vérifie que le restaurant existe avant de le modifier
     *
     * @param id L'ID du restaurant à modifier
     * @param name Le nouveau nom
     * @param description La nouvelle description
     * @param website Le nouveau site web
     * @return Le restaurant mis à jour, ou null si non trouvé
     */
    public Restaurant updateRestaurant(Integer id, String name, String description, String website) {
        logger.info("Service: Mise à jour du restaurant ID {}", id);

        // Vérifier que le restaurant existe
        Restaurant restaurant = restaurantDao.findById(id);
        if (restaurant == null) {
            logger.error("Erreur: Le restaurant avec l'ID {} n'existe pas", id);
            return null;
        }

        // Mettre à jour les propriétés
        restaurant.setName(name);
        restaurant.setDescription(description);
        restaurant.setWebsite(website);

        // Sauvegarder les modifications
        Restaurant updatedRestaurant = restaurantDao.save(restaurant);
        logger.info("Restaurant mis à jour avec succès");

        return updatedRestaurant;
    }

    /**
     * Change l'adresse d'un restaurant
     * LOGIQUE MÉTIER: Vérifie que la nouvelle ville existe
     *
     * @param restaurantId L'ID du restaurant
     * @param street La nouvelle rue
     * @param cityId L'ID de la nouvelle ville
     * @return Le restaurant mis à jour, ou null en cas d'erreur
     */
    public Restaurant updateRestaurantAddress(Integer restaurantId, String street, Integer cityId) {
        logger.info("Service: Mise à jour de l'adresse du restaurant ID {}", restaurantId);

        // Vérifier que le restaurant existe
        Restaurant restaurant = restaurantDao.findById(restaurantId);
        if (restaurant == null) {
            logger.error("Erreur: Le restaurant avec l'ID {} n'existe pas", restaurantId);
            return null;
        }

        // Vérifier que la nouvelle ville existe
        City city = cityDao.findById(cityId);
        if (city == null) {
            logger.error("Erreur: La ville avec l'ID {} n'existe pas", cityId);
            return null;
        }

        // Mettre à jour l'adresse
        restaurant.getAddress().setStreet(street);
        restaurant.getAddress().setCity(city);

        // Sauvegarder
        Restaurant updatedRestaurant = restaurantDao.save(restaurant);
        logger.info("Adresse mise à jour avec succès");

        return updatedRestaurant;
    }

    /**
     * Change le type gastronomique d'un restaurant
     * LOGIQUE MÉTIER: Vérifie que le nouveau type existe
     *
     * @param restaurantId L'ID du restaurant
     * @param typeId L'ID du nouveau type
     * @return Le restaurant mis à jour, ou null en cas d'erreur
     */
    public Restaurant updateRestaurantType(Integer restaurantId, Integer typeId) {
        logger.info("Service: Mise à jour du type du restaurant ID {}", restaurantId);

        // Vérifier que le restaurant existe
        Restaurant restaurant = restaurantDao.findById(restaurantId);
        if (restaurant == null) {
            logger.error("Erreur: Le restaurant avec l'ID {} n'existe pas", restaurantId);
            return null;
        }

        // Vérifier que le nouveau type existe
        RestaurantType type = typeDao.findById(typeId);
        if (type == null) {
            logger.error("Erreur: Le type avec l'ID {} n'existe pas", typeId);
            return null;
        }

        // Mettre à jour le type
        restaurant.setType(type);

        // Sauvegarder
        Restaurant updatedRestaurant = restaurantDao.save(restaurant);
        logger.info("Type mis à jour avec succès");

        return updatedRestaurant;
    }

    // ==================== MÉTHODES DE SUPPRESSION ====================

    /**
     * Supprime un restaurant
     * LOGIQUE MÉTIER: Vérifie que le restaurant existe avant de le supprimer
     *
     * @param id L'ID du restaurant à supprimer
     * @return true si la suppression a réussi, false sinon
     */
    public boolean deleteRestaurant(Integer id) {
        logger.info("Service: Suppression du restaurant ID {}", id);

        // Vérifier que le restaurant existe
        Restaurant restaurant = restaurantDao.findById(id);
        if (restaurant == null) {
            logger.error("Erreur: Le restaurant avec l'ID {} n'existe pas", id);
            return false;
        }

        try {
            restaurantDao.deleteById(id);
            logger.info("Restaurant supprimé avec succès");
            return true;
        } catch (Exception e) {
            logger.error("Erreur lors de la suppression du restaurant", e);
            return false;
        }
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Compte le nombre total de restaurants
     * @return Le nombre de restaurants
     */
    public int countRestaurants() {
        return restaurantDao.findAll().size();
    }

    /**
     * Vérifie si un restaurant existe
     * @param id L'ID du restaurant
     * @return true si le restaurant existe, false sinon
     */
    public boolean restaurantExists(Integer id) {
        return restaurantDao.findById(id) != null;
    }
}