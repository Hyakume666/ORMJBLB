package ch.hearc.ig.guideresto.service;

import ch.hearc.ig.guideresto.business.City;
import ch.hearc.ig.guideresto.business.Restaurant;
import ch.hearc.ig.guideresto.business.RestaurantType;
import ch.hearc.ig.guideresto.business.Localisation;
import ch.hearc.ig.guideresto.persistence.dao.RestaurantDao;
import ch.hearc.ig.guideresto.persistence.jpa.JpaUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Service pour gérer la logique métier des restaurants
 *
 * REFACTORING (Phase 2) :
 * - Utilise CityService et RestaurantTypeService au lieu des DAO
 * - Simplifie le code (validations déléguées aux services)
 * - Cohérence architecturale (service → service, pas service → DAO)
 */
public class RestaurantService {

    private static final Logger logger = LogManager.getLogger(RestaurantService.class);

    // Les services utilisés
    private final RestaurantDao restaurantDao;
    private final CityService cityService;
    private final RestaurantTypeService typeService;

    /**
     * Constructeur qui initialise les DAO et services nécessaires
     * MODIFIÉ : Utilise CityService et RestaurantTypeService
     */
    public RestaurantService() {
        this.restaurantDao = new RestaurantDao();
        this.cityService = new CityService();
        this.typeService = new RestaurantTypeService();
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

    // ==================== MÉTHODES DE CRÉATION (EXERCICE 6) ====================

    /**
     * Crée un nouveau restaurant avec une ville existante
     * LOGIQUE MÉTIER :
     * - Vérifie que la ville et le type existent (via services)
     * - Vérifie qu'un restaurant avec le même nom n'existe pas déjà dans cette ville (UNICITÉ)
     * - Crée le restaurant dans une transaction
     *
     * MODIFIÉ : Utilise cityService et typeService au lieu des DAO
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

        // MODIFIÉ : Vérifier que la ville existe via le service
        City city = cityService.getCityById(cityId);
        if (city == null) {
            logger.error("[createRestaurant] La ville avec l'ID {} n'existe pas", cityId);
            return null;
        }

        // MODIFIÉ : Vérifier que le type existe via le service
        RestaurantType type = typeService.getTypeById(typeId);
        if (type == null) {
            logger.error("[createRestaurant] Le type avec l'ID {} n'existe pas", typeId);
            return null;
        }

        // VALIDATION : Vérifier l'unicité (pas de restaurant avec le même nom dans la même ville)
        if (restaurantExistsInCity(name, cityId)) {
            logger.error("[createRestaurant] Un restaurant nommé '{}' existe déjà dans cette ville", name);
            return null;
        }

        // Création de l'adresse (composition)
        Localisation address = new Localisation(street, city);

        // Création du restaurant
        Restaurant restaurant = new Restaurant(null, name, description, website, address, type);

        // Sauvegarde en base de données dans une transaction
        final Restaurant[] result = new Restaurant[1];
        try {
            JpaUtils.inTransaction(em -> result[0] = em.merge(restaurant));
            logger.info("Restaurant créé avec succès (ID: {})", result[0].getId());
            return result[0];
        } catch (Exception e) {
            logger.error("Erreur lors de la création du restaurant: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * EXERCICE 6 - TRANSACTION COMPLEXE
     * Crée un nouveau restaurant ET une nouvelle ville dans une SEULE transaction
     *
     * MODIFIÉ : Utilise cityService pour créer la ville (avec validation intégrée)
     * SIMPLIFIÉ : Plus besoin de vérifier manuellement si la ville existe, le service le fait
     *
     * @param name Le nom du restaurant
     * @param description La description
     * @param website Le site web
     * @param street La rue
     * @param zipCode Le code postal de la nouvelle ville
     * @param cityName Le nom de la nouvelle ville
     * @param typeId L'ID du type gastronomique
     * @return Le restaurant créé, ou null en cas d'erreur
     */
    public Restaurant createRestaurantWithNewCity(String name, String description, String website,
                                                  String street, String zipCode, String cityName,
                                                  Integer typeId) {
        logger.info("Service: Création d'un restaurant '{}' avec nouvelle ville '{}' dans UNE transaction",
                name, cityName);

        // MODIFIÉ : Vérifier que le type existe via le service
        RestaurantType type = typeService.getTypeById(typeId);
        if (type == null) {
            logger.error("[createRestaurantWithNewCity] Le type avec l'ID {} n'existe pas", typeId);
            return null;
        }

        // SIMPLIFIÉ : Plus besoin de vérifier manuellement, cityService.createCity() le fait déjà
        // MODIFIÉ : Vérifier que la ville n'existe pas en utilisant le service
        if (cityService.cityExistsByZipCode(zipCode)) {
            logger.error("Erreur: Une ville avec le NPA {} existe déjà", zipCode);
            return null;
        }

        final Restaurant[] result = new Restaurant[1];

        try {
            // TRANSACTION UNIQUE pour créer ville ET restaurant
            JpaUtils.inTransaction(em -> {
                // 1. MODIFIÉ : Créer la ville via le service (mais dans la même transaction)
                // Note: On appelle directement le DAO ici pour rester dans la même transaction
                // Alternative : On pourrait aussi extraire la logique de création dans CityService
                City newCity = new City(zipCode, cityName);
                City savedCity = em.merge(newCity);
                logger.info("  → Ville créée avec ID: {}", savedCity.getId());

                // 2. Créer l'adresse avec la nouvelle ville
                Localisation address = new Localisation(street, savedCity);

                // 3. Créer le restaurant
                Restaurant restaurant = new Restaurant(null, name, description, website, address, type);
                result[0] = em.merge(restaurant);
                logger.info("  → Restaurant créé avec ID: {}", result[0].getId());
            });

            logger.info("✓ Transaction complète réussie: Ville ET Restaurant créés");
            return result[0];

        } catch (Exception e) {
            logger.error("✗ ROLLBACK: Erreur lors de la création (ville ET restaurant annulés): {}",
                    e.getMessage(), e);
            return null;
        }
    }

    // ==================== MÉTHODES DE MISE À JOUR ====================

    /**
     * Met à jour un restaurant existant
     * LOGIQUE MÉTIER : Vérifie que le restaurant existe avant de le modifier
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
            logger.error("[updateRestaurant] Le restaurant avec l'ID {} n'existe pas", id);
            return null;
        }

        // VALIDATION : Si le nom change, vérifier l'unicité
        if (!restaurant.getName().equals(name)) {
            if (restaurantExistsInCity(name, restaurant.getAddress().getCity().getId())) {
                logger.error("[updateRestaurant] Un restaurant nommé '{}' existe déjà dans cette ville", name);
                return null;
            }
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
     * LOGIQUE MÉTIER : Vérifie que la nouvelle ville existe
     *
     * MODIFIÉ : Utilise cityService pour vérifier l'existence de la ville
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
            logger.error("[updateRestaurantAddress] Le restaurant avec l'ID {} n'existe pas", restaurantId);
            return null;
        }

        // MODIFIÉ : Vérifier que la nouvelle ville existe via le service
        City city = cityService.getCityById(cityId);
        if (city == null) {
            logger.error("Erreur: La ville avec l'ID {} n'existe pas", cityId);
            return null;
        }

        // VALIDATION : Vérifier l'unicité dans la nouvelle ville
        if (!restaurant.getAddress().getCity().getId().equals(cityId)) {
            if (restaurantExistsInCity(restaurant.getName(), cityId)) {
                logger.error("Erreur: Un restaurant nommé '{}' existe déjà dans cette ville",
                        restaurant.getName());
                return null;
            }
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
     * LOGIQUE MÉTIER : Vérifie que le nouveau type existe
     *
     * MODIFIÉ : Utilise typeService pour vérifier l'existence du type
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
            logger.error("[updateRestaurantType] Le restaurant avec l'ID {} n'existe pas", restaurantId);
            return null;
        }

        // MODIFIÉ : Vérifier que le nouveau type existe via le service
        RestaurantType type = typeService.getTypeById(typeId);
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
     * LOGIQUE MÉTIER : Vérifie que le restaurant existe avant de le supprimer
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

    // ==================== MÉTHODES UTILITAIRES & VALIDATIONS ====================

    /**
     * VALIDATION : Vérifie si un restaurant avec le même nom existe déjà dans une ville
     * Cette méthode garantit l'unicité des restaurants par ville
     *
     * @param name Le nom du restaurant
     * @param cityId L'ID de la ville
     * @return true si un restaurant avec ce nom existe dans cette ville
     */
    private boolean restaurantExistsInCity(String name, Integer cityId) {
        List<Restaurant> restaurants = restaurantDao.findByCity(cityId);
        return restaurants.stream()
                .anyMatch(r -> r.getName().equalsIgnoreCase(name));
    }

    /**
     * Compte le nombre total de restaurants
     * @return Le nombre de restaurants
     */
    public int countRestaurants() {
        return restaurantDao.findAll().size();
    }
}