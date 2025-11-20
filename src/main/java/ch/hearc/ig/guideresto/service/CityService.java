package ch.hearc.ig.guideresto.service;

import ch.hearc.ig.guideresto.business.City;
import ch.hearc.ig.guideresto.persistence.dao.CityDao;
import ch.hearc.ig.guideresto.persistence.dao.RestaurantDao;
import ch.hearc.ig.guideresto.persistence.jpa.JpaUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Service pour gérer la logique métier des villes
 * Centralise toutes les opérations liées aux villes (City)
 */
public class CityService {

    private static final Logger logger = LogManager.getLogger(CityService.class);

    private final CityDao cityDao;
    private final RestaurantDao restaurantDao;

    /**
     * Constructeur qui initialise les DAO nécessaires
     */
    public CityService() {
        this.cityDao = new CityDao();
        this.restaurantDao = new RestaurantDao();
    }

    // ==================== MÉTHODES DE RECHERCHE ====================

    /**
     * Récupère toutes les villes triées par nom
     * @return Liste de toutes les villes
     */
    public List<City> getAllCities() {
        logger.debug("Service: Récupération de toutes les villes");
        return cityDao.findAll();
    }

    /**
     * Recherche une ville par son ID
     * @param id L'ID de la ville
     * @return La ville trouvée, ou null
     */
    public City getCityById(Integer id) {
        logger.debug("Service: Recherche de la ville avec ID {}", id);
        return cityDao.findById(id);
    }

    /**
     * Recherche une ville par son code postal (NPA)
     * @param zipCode Le code postal
     * @return La ville trouvée, ou null si non trouvée
     */
    public City getCityByZipCode(String zipCode) {
        logger.debug("Service: Recherche de la ville avec NPA {}", zipCode);
        return cityDao.findByZipCode(zipCode);
    }

    /**
     * Recherche des villes par nom (recherche partielle, insensible à la casse)
     * @param cityName Le nom (ou partie du nom) à rechercher
     * @return Liste des villes correspondantes
     */
    public List<City> searchCitiesByName(String cityName) {
        logger.debug("Service: Recherche de villes contenant '{}'", cityName);
        return cityDao.findByCityName(cityName);
    }

    /**
     * Recherche une ville par nom exact
     * @param cityName Le nom exact de la ville
     * @return La ville trouvée, ou null
     */
    public City getCityByExactName(String cityName) {
        logger.debug("Service: Recherche de la ville avec le nom exact '{}'", cityName);
        List<City> cities = cityDao.findByCityName(cityName);

        // Filtrer pour obtenir le match exact
        for (City city : cities) {
            if (city.getCityName().equalsIgnoreCase(cityName)) {
                return city;
            }
        }
        return null;
    }

    // ==================== MÉTHODES DE CRÉATION ====================

    /**
     * Crée une nouvelle ville
     * LOGIQUE MÉTIER :
     * - Vérifie que le NPA n'existe pas déjà (unicité)
     * - Valide que les champs ne sont pas vides
     * - Crée la ville dans une transaction
     *
     * @param zipCode Le code postal (NPA)
     * @param cityName Le nom de la ville
     * @return La ville créée, ou null en cas d'erreur
     */
    public City createCity(String zipCode, String cityName) {
        logger.info("Service: Création d'une nouvelle ville '{}'", cityName);

        // VALIDATION : Vérifier que les champs ne sont pas vides
        if (zipCode == null || zipCode.trim().isEmpty()) {
            logger.error("Erreur: Le code postal ne peut pas être vide");
            return null;
        }
        if (cityName == null || cityName.trim().isEmpty()) {
            logger.error("Erreur: Le nom de la ville ne peut pas être vide");
            return null;
        }

        // VALIDATION : Vérifier l'unicité du NPA
        City existingCity = cityDao.findByZipCode(zipCode);
        if (existingCity != null) {
            logger.error("Erreur: Une ville avec le NPA {} existe déjà ({})",
                    zipCode, existingCity.getCityName());
            return null;
        }

        // Créer la ville
        City newCity = new City(zipCode, cityName);

        // Sauvegarder dans une transaction
        final City[] result = new City[1];
        try {
            JpaUtils.inTransaction(em -> result[0] = em.merge(newCity));
            logger.info("Ville créée avec succès (ID: {})", result[0].getId());
            return result[0];
        } catch (Exception e) {
            logger.error("Erreur lors de la création de la ville: {}", e.getMessage(), e);
            return null;
        }
    }

    // ==================== MÉTHODES DE MISE À JOUR ====================

    /**
     * Met à jour les informations d'une ville
     * LOGIQUE MÉTIER :
     * - Vérifie que la ville existe
     * - Si le NPA change, vérifie l'unicité
     * - Met à jour dans une transaction
     *
     * @param id L'ID de la ville à modifier
     * @param zipCode Le nouveau code postal
     * @param cityName Le nouveau nom
     * @return La ville mise à jour, ou null en cas d'erreur
     */
    public City updateCity(Integer id, String zipCode, String cityName) {
        logger.info("Service: Mise à jour de la ville ID {}", id);

        // Vérifier que la ville existe
        City city = cityDao.findById(id);
        if (city == null) {
            logger.error("Erreur: La ville avec l'ID {} n'existe pas", id);
            return null;
        }

        // VALIDATION : Si le NPA change, vérifier l'unicité
        if (!city.getZipCode().equals(zipCode)) {
            City existingCity = cityDao.findByZipCode(zipCode);
            if (existingCity != null) {
                logger.error("Erreur: Le NPA {} est déjà utilisé par '{}'",
                        zipCode, existingCity.getCityName());
                return null;
            }
        }

        // Mettre à jour les propriétés
        city.setZipCode(zipCode);
        city.setCityName(cityName);

        // Sauvegarder
        City updatedCity = cityDao.save(city);
        logger.info("Ville mise à jour avec succès");

        return updatedCity;
    }

    // ==================== MÉTHODES DE SUPPRESSION ====================

    /**
     * Supprime une ville
     * LOGIQUE MÉTIER :
     * - Vérifie que la ville existe
     * - Vérifie qu'aucun restaurant n'est lié à cette ville
     * - Supprime dans une transaction
     *
     * @param id L'ID de la ville à supprimer
     * @return true si la suppression a réussi, false sinon
     */
    public boolean deleteCity(Integer id) {
        logger.info("Service: Suppression de la ville ID {}", id);

        // Vérifier que la ville existe
        City city = cityDao.findById(id);
        if (city == null) {
            logger.error("Erreur: La ville avec l'ID {} n'existe pas", id);
            return false;
        }

        // VALIDATION : Vérifier qu'aucun restaurant n'est lié à cette ville
        int restaurantCount = restaurantDao.findByCity(id).size();
        if (restaurantCount > 0) {
            logger.error("Erreur: Impossible de supprimer la ville '{}' car {} restaurant(s) y sont liés",
                    city.getCityName(), restaurantCount);
            return false;
        }

        try {
            cityDao.deleteById(id);
            logger.info("Ville supprimée avec succès");
            return true;
        } catch (Exception e) {
            logger.error("Erreur lors de la suppression de la ville", e);
            return false;
        }
    }

    // ==================== MÉTHODES UTILITAIRES & STATISTIQUES ====================

    /**
     * Compte le nombre de restaurants dans une ville
     * @param cityId L'ID de la ville
     * @return Le nombre de restaurants
     */
    public int countRestaurantsInCity(Integer cityId) {
        logger.debug("Service: Comptage des restaurants dans la ville ID {}", cityId);
        return restaurantDao.findByCity(cityId).size();
    }

    /**
     * Vérifie si une ville existe par son NPA
     * @param zipCode Le code postal à vérifier
     * @return true si la ville existe
     */
    public boolean cityExistsByZipCode(String zipCode) {
        return cityDao.findByZipCode(zipCode) != null;
    }

    /**
     * Vérifie si une ville a des restaurants associés
     * @param cityId L'ID de la ville
     * @return true si la ville a au moins un restaurant
     */
    public boolean cityHasRestaurants(Integer cityId) {
        return countRestaurantsInCity(cityId) > 0;
    }

    /**
     * Compte le nombre total de villes
     * @return Le nombre de villes
     */
    public int countCities() {
        return cityDao.findAll().size();
    }

    /**
     * Récupère les villes qui ont au moins un restaurant
     * @return Liste des villes avec restaurants
     */
    public List<City> getCitiesWithRestaurants() {
        logger.debug("Service: Récupération des villes avec restaurants");
        List<City> allCities = cityDao.findAll();

        return allCities.stream()
                .filter(city -> cityHasRestaurants(city.getId()))
                .toList();
    }

    /**
     * Récupère les villes qui n'ont aucun restaurant
     * @return Liste des villes sans restaurant
     */
    public List<City> getCitiesWithoutRestaurants() {
        logger.debug("Service: Récupération des villes sans restaurant");
        List<City> allCities = cityDao.findAll();

        return allCities.stream()
                .filter(city -> !cityHasRestaurants(city.getId()))
                .toList();
    }
}