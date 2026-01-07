package ch.hearc.ig.guideresto.service;

import ch.hearc.ig.guideresto.business.City;
import ch.hearc.ig.guideresto.business.Restaurant;
import ch.hearc.ig.guideresto.business.RestaurantType;
import ch.hearc.ig.guideresto.business.Localisation;
import ch.hearc.ig.guideresto.persistence.dao.RestaurantDao;
import ch.hearc.ig.guideresto.persistence.jpa.JpaUtils;
import jakarta.persistence.OptimisticLockException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Service de gestion des restaurants avec logique métier et validations.
 * Ce service encapsule toutes les opérations relatives aux restaurants :
 * création, modification, suppression, recherche et gestion de la concurrence.
 */
public class RestaurantService {

    private static final Logger logger = LogManager.getLogger(RestaurantService.class);

    private final RestaurantDao restaurantDao;
    private final CityService cityService;
    private final RestaurantTypeService typeService;

    /**
     * Constructeur par défaut initialisant les dépendances nécessaires.
     */
    public RestaurantService() {
        this.restaurantDao = new RestaurantDao();
        this.cityService = new CityService();
        this.typeService = new RestaurantTypeService();
    }

    /**
     * Récupère tous les restaurants enregistrés dans le système.
     *
     * @return Liste de tous les restaurants, triés par nom
     */
    public List<Restaurant> getAllRestaurants() {
        logger.debug("Service: Récupération de tous les restaurants");
        return restaurantDao.findAll();
    }

    /**
     * Recherche un restaurant par son ID.
     *
     * @param id L'identifiant du restaurant
     * @return Le restaurant trouvé, ou null si non trouvé
     */
    public Restaurant getRestaurantById(Integer id) {
        logger.debug("Service: Recherche du restaurant avec ID {}", id);
        return restaurantDao.findById(id);
    }

    /**
     * Recherche des restaurants par nom (recherche partielle, insensible à la casse).
     *
     * @param name Le nom à rechercher (peut être partiel)
     * @return Liste des restaurants correspondants
     */
    public List<Restaurant> searchRestaurantsByName(String name) {
        logger.debug("Service: Recherche de restaurants contenant '{}'", name);
        return restaurantDao.findByName(name);
    }

    /**
     * Recherche un restaurant par son nom exact (insensible à la casse).
     *
     * @param name Le nom exact du restaurant
     * @return Le restaurant trouvé, ou null si non trouvé
     */
    public Restaurant getRestaurantByExactName(String name) {
        logger.debug("Service: Recherche du restaurant avec le nom exact '{}'", name);
        List<Restaurant> restaurants = restaurantDao.findByName(name);

        for (Restaurant restaurant : restaurants) {
            if (restaurant.getName().equalsIgnoreCase(name)) {
                return restaurant;
            }
        }
        return null;
    }

    /**
     * Récupère tous les restaurants d'une ville donnée.
     *
     * @param cityId L'ID de la ville
     * @return Liste des restaurants dans cette ville
     */
    public List<Restaurant> getRestaurantsByCity(Integer cityId) {
        logger.debug("Service: Recherche des restaurants dans la ville ID {}", cityId);
        return restaurantDao.findByCity(cityId);
    }

    /**
     * Récupère tous les restaurants d'un type gastronomique donné.
     *
     * @param typeId L'ID du type gastronomique
     * @return Liste des restaurants de ce type
     */
    public List<Restaurant> getRestaurantsByType(Integer typeId) {
        logger.debug("Service: Recherche des restaurants du type ID {}", typeId);
        return restaurantDao.findByType(typeId);
    }

    /**
     * Crée un nouveau restaurant avec validation d'unicité (nom + ville).
     * Un restaurant ne peut pas avoir le même nom qu'un autre restaurant dans la même ville.
     * Le restaurant est créé dans une transaction.
     *
     * @param name Le nom du restaurant
     * @param description La description du restaurant
     * @param website Le site web (optionnel)
     * @param street La rue
     * @param cityId L'ID de la ville (doit exister)
     * @param typeId L'ID du type gastronomique (doit exister)
     * @return Le restaurant créé avec son ID généré, ou null si la validation échoue ou si la ville/type n'existe pas
     */
    public Restaurant createRestaurant(String name, String description, String website,
                                       String street, Integer cityId, Integer typeId) {
        logger.info("Service: Création d'un nouveau restaurant '{}'", name);

        City city = cityService.getCityById(cityId);
        if (city == null) {
            logger.error("[createRestaurant] La ville avec l'ID {} n'existe pas", cityId);
            return null;
        }

        RestaurantType type = typeService.getTypeById(typeId);
        if (type == null) {
            logger.error("[createRestaurant] Le type avec l'ID {} n'existe pas", typeId);
            return null;
        }

        if (restaurantExistsInCity(name, cityId)) {
            logger.error("[createRestaurant] Un restaurant nommé '{}' existe déjà dans cette ville", name);
            return null;
        }

        Localisation address = new Localisation(street, city);
        Restaurant restaurant = new Restaurant(null, name, description, website, address, type);

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
     * Crée un restaurant ET une nouvelle ville dans une transaction unique atomique.
     * Si l'une des créations échoue, toute la transaction est annulée (rollback complet).
     * Ceci garantit la cohérence des données : soit les deux sont créés, soit aucun.
     * <p>
     * Validations effectuées :
     * <ul>
     *   <li>Le type de restaurant doit exister</li>
     *   <li>Le code postal de la nouvelle ville ne doit pas déjà exister</li>
     * </ul>
     *
     * @param name Le nom du restaurant
     * @param description La description du restaurant
     * @param website Le site web (optionnel)
     * @param street La rue
     * @param zipCode Le code postal de la nouvelle ville (doit être unique)
     * @param cityName Le nom de la nouvelle ville
     * @param typeId L'ID du type gastronomique (doit exister)
     * @return Le restaurant créé (avec la ville créée), ou null si une validation échoue ou en cas d'erreur
     */
    public Restaurant createRestaurantWithNewCity(String name, String description, String website,
                                                  String street, String zipCode, String cityName,
                                                  Integer typeId) {
        logger.info("Service: Création d'un restaurant '{}' avec nouvelle ville '{}' dans UNE transaction",
                name, cityName);

        RestaurantType type = typeService.getTypeById(typeId);
        if (type == null) {
            logger.error("[createRestaurantWithNewCity] Le type avec l'ID {} n'existe pas", typeId);
            return null;
        }

        if (cityService.cityExistsByZipCode(zipCode)) {
            logger.error("Erreur: Une ville avec le NPA {} existe déjà", zipCode);
            return null;
        }

        final Restaurant[] result = new Restaurant[1];

        try {
            JpaUtils.inTransaction(em -> {
                City newCity = new City(zipCode, cityName);
                City savedCity = em.merge(newCity);
                logger.info("  → Ville créée avec ID: {}", savedCity.getId());

                Localisation address = new Localisation(street, savedCity);

                Restaurant restaurant = new Restaurant(null, name, description, website, address, type);
                result[0] = em.merge(restaurant);
                logger.info("  → Restaurant créé avec ID: {}", result[0].getId());
            });

            logger.info("Transaction complète réussie: Ville ET Restaurant créés");
            return result[0];

        } catch (Exception e) {
            logger.error("ROLLBACK: Erreur lors de la création (ville ET restaurant annulés): {}",
                    e.getMessage(), e);
            return null;
        }
    }

    /**
     * Met à jour les informations de base d'un restaurant avec validation d'unicité et gestion de la concurrence.
     * Si le nom change, vérifie qu'il n'y a pas déjà un restaurant avec ce nom dans la même ville.
     * Utilise le verrouillage optimiste pour détecter les modifications concurrentes.
     *
     * @param id L'ID du restaurant à modifier
     * @param name Le nouveau nom
     * @param description La nouvelle description
     * @param website Le nouveau site web
     * @return Le restaurant mis à jour, ou null si le restaurant n'existe pas, si le nom est déjà utilisé ou en cas de conflit de concurrence
     */
    public Restaurant updateRestaurant(Integer id, String name, String description, String website) {
        logger.info("Service: Mise à jour du restaurant ID {}", id);

        Restaurant restaurant = restaurantDao.findById(id);
        if (restaurant == null) {
            logger.error("[updateRestaurant] Le restaurant avec l'ID {} n'existe pas", id);
            return null;
        }

        if (!restaurant.getName().equals(name)) {
            if (restaurantExistsInCity(name, restaurant.getAddress().getCity().getId())) {
                logger.error("[updateRestaurant] Un restaurant nommé '{}' existe déjà dans cette ville", name);
                return null;
            }
        }

        restaurant.setName(name);
        restaurant.setDescription(description);
        restaurant.setWebsite(website);

        // Gestion du verrouillage optimiste (Exercice 7)
        try {
            Restaurant updatedRestaurant = restaurantDao.save(restaurant);
            logger.info("Restaurant mis à jour avec succès");
            return updatedRestaurant;
        } catch (Exception e) {
            // Vérification si l'erreur vient d'un verrou optimiste
            if (e instanceof OptimisticLockException || (e.getCause() != null && e.getCause() instanceof OptimisticLockException)) {
                logger.error("ERREUR DE CONCURRENCE : Le restaurant a été modifié par un autre utilisateur. Veuillez recharger les données.");
            } else {
                logger.error("Erreur lors de la mise à jour du restaurant", e);
            }
            return null;
        }
    }

    /**
     * Change l'adresse d'un restaurant avec validation d'unicité dans la nouvelle ville.
     *
     * @param restaurantId L'ID du restaurant
     * @param street La nouvelle rue
     * @param cityId L'ID de la nouvelle ville (doit exister)
     * @return Le restaurant avec l'adresse mise à jour, ou null si le restaurant/ville n'existe pas
     *         ou si un restaurant avec le même nom existe déjà dans la nouvelle ville
     */
    public Restaurant updateRestaurantAddress(Integer restaurantId, String street, Integer cityId) {
        logger.info("Service: Mise à jour de l'adresse du restaurant ID {}", restaurantId);

        Restaurant restaurant = restaurantDao.findById(restaurantId);
        if (restaurant == null) {
            logger.error("[updateRestaurantAddress] Le restaurant avec l'ID {} n'existe pas", restaurantId);
            return null;
        }

        City city = cityService.getCityById(cityId);
        if (city == null) {
            logger.error("Erreur: La ville avec l'ID {} n'existe pas", cityId);
            return null;
        }

        if (!restaurant.getAddress().getCity().getId().equals(cityId)) {
            if (restaurantExistsInCity(restaurant.getName(), cityId)) {
                logger.error("Erreur: Un restaurant nommé '{}' existe déjà dans cette ville",
                        restaurant.getName());
                return null;
            }
        }

        restaurant.getAddress().setStreet(street);
        restaurant.getAddress().setCity(city);

        Restaurant updatedRestaurant = restaurantDao.save(restaurant);
        logger.info("Adresse mise à jour avec succès");

        return updatedRestaurant;
    }

    /**
     * Change le type gastronomique d'un restaurant.
     *
     * @param restaurantId L'ID du restaurant
     * @param typeId L'ID du nouveau type (doit exister)
     * @return Le restaurant avec le type mis à jour, ou null si le restaurant/type n'existe pas
     */
    public Restaurant updateRestaurantType(Integer restaurantId, Integer typeId) {
        logger.info("Service: Mise à jour du type du restaurant ID {}", restaurantId);

        Restaurant restaurant = restaurantDao.findById(restaurantId);
        if (restaurant == null) {
            logger.error("[updateRestaurantType] Le restaurant avec l'ID {} n'existe pas", restaurantId);
            return null;
        }

        RestaurantType type = typeService.getTypeById(typeId);
        if (type == null) {
            logger.error("Erreur: Le type avec l'ID {} n'existe pas", typeId);
            return null;
        }

        restaurant.setType(type);

        Restaurant updatedRestaurant = restaurantDao.save(restaurant);
        logger.info("Type mis à jour avec succès");

        return updatedRestaurant;
    }

    /**
     * Supprime un restaurant.
     * Les évaluations liées sont supprimées en cascade (orphanRemoval).
     *
     * @param id L'ID du restaurant à supprimer
     * @return true si la suppression a réussi, false si le restaurant n'existe pas
     */
    public boolean deleteRestaurant(Integer id) {
        logger.info("Service: Suppression du restaurant ID {}", id);

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

    /**
     * Vérifie si un restaurant avec le nom donné existe déjà dans une ville.
     *
     * @param name Le nom du restaurant à vérifier
     * @param cityId L'ID de la ville
     * @return true si un restaurant avec ce nom existe dans cette ville, false sinon
     */
    private boolean restaurantExistsInCity(String name, Integer cityId) {
        List<Restaurant> restaurants = restaurantDao.findByCity(cityId);
        return restaurants.stream()
                .anyMatch(r -> r.getName().equalsIgnoreCase(name));
    }

    /**
     * Compte le nombre total de restaurants enregistrés.
     *
     * @return Le nombre total de restaurants
     */
    public int countRestaurants() {
        return restaurantDao.findAll().size();
    }
}