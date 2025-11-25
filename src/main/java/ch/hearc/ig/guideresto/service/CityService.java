package ch.hearc.ig.guideresto.service;

import ch.hearc.ig.guideresto.business.City;
import ch.hearc.ig.guideresto.persistence.dao.CityDao;
import ch.hearc.ig.guideresto.persistence.dao.RestaurantDao;
import ch.hearc.ig.guideresto.persistence.jpa.JpaUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Service de gestion des villes avec logique métier et validations
 */
public class CityService {

    private static final Logger logger = LogManager.getLogger(CityService.class);

    private final CityDao cityDao;
    private final RestaurantDao restaurantDao;

    public CityService() {
        this.cityDao = new CityDao();
        this.restaurantDao = new RestaurantDao();
    }

    public List<City> getAllCities() {
        logger.debug("Service: Récupération de toutes les villes");
        return cityDao.findAll();
    }

    /**
     * Recherche une ville par son ID
     *
     * @param id L'identifiant de la ville
     * @return La ville trouvée, ou null si non trouvée
     */
    public City getCityById(Integer id) {
        logger.debug("Service: Recherche de la ville avec ID {}", id);
        return cityDao.findById(id);
    }

    /**
     * Recherche une ville par son code postal (NPA)
     *
     * @param zipCode Le code postal
     * @return La ville trouvée, ou null si non trouvée
     */
    @SuppressWarnings("unused")
    public City getCityByZipCode(String zipCode) {
        logger.debug("Service: Recherche de la ville avec NPA {}", zipCode);
        return cityDao.findByZipCode(zipCode);
    }

    public List<City> searchCitiesByName(String cityName) {
        logger.debug("Service: Recherche de villes contenant '{}'", cityName);
        return cityDao.findByCityName(cityName);
    }

    /**
     * Recherche une ville par son nom exact (insensible à la casse)
     *
     * @param cityName Le nom exact de la ville
     * @return La ville trouvée, ou null si non trouvée
     */
    @SuppressWarnings("unused")
    public City getCityByExactName(String cityName) {
        logger.debug("Service: Recherche de la ville avec le nom exact '{}'", cityName);
        List<City> cities = cityDao.findByCityName(cityName);

        for (City city : cities) {
            if (city.getCityName().equalsIgnoreCase(cityName)) {
                return city;
            }
        }
        return null;
    }

    /**
     * Crée une nouvelle ville avec validation d'unicité du code postal.
     * La ville est créée dans une transaction.
     *
     * @param zipCode Le code postal (NPA), ne doit pas être vide ni déjà existant
     * @param cityName Le nom de la ville, ne peut pas être vide
     * @return La ville créée avec son ID généré, ou null si la validation échoue
     */
    public City createCity(String zipCode, String cityName) {
        logger.info("Service: Création d'une nouvelle ville '{}'", cityName);

        if (zipCode == null || zipCode.trim().isEmpty()) {
            logger.error("Erreur: Le code postal ne peut pas être vide");
            return null;
        }
        if (cityName == null || cityName.trim().isEmpty()) {
            logger.error("Erreur: Le nom de la ville ne peut pas être vide");
            return null;
        }

        City existingCity = cityDao.findByZipCode(zipCode);
        if (existingCity != null) {
            logger.error("Erreur: Une ville avec le NPA {} existe déjà ({})",
                    zipCode, existingCity.getCityName());
            return null;
        }

        City newCity = new City(zipCode, cityName);

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

    /**
     * Met à jour une ville existante avec validation d'unicité du nouveau code postal.
     *
     * @param id L'ID de la ville à modifier
     * @param zipCode Le nouveau code postal (doit être unique).
     * @param cityName Le nouveau nom de la ville
     * @return La ville mise à jour, ou null si la ville n'existe pas ou si le NPA est déjà utilisé
     */
    @SuppressWarnings("unused")
    public City updateCity(Integer id, String zipCode, String cityName) {
        logger.info("Service: Mise à jour de la ville ID {}", id);

        City city = cityDao.findById(id);
        if (city == null) {
            logger.error("Erreur: La ville avec l'ID {} n'existe pas", id);
            return null;
        }

        if (!city.getZipCode().equals(zipCode)) {
            City existingCity = cityDao.findByZipCode(zipCode);
            if (existingCity != null) {
                logger.error("Erreur: Le NPA {} est déjà utilisé par '{}'",
                        zipCode, existingCity.getCityName());
                return null;
            }
        }

        city.setZipCode(zipCode);
        city.setCityName(cityName);

        City updatedCity = cityDao.save(city);
        logger.info("Ville mise à jour avec succès");

        return updatedCity;
    }

    /**
     * Supprime une ville si aucun restaurant n'y est lié.
     * La suppression respecte l'intégrité référentielle.
     *
     * @param id L'ID de la ville à supprimer
     * @return true si la suppression a réussi, false si la ville n'existe pas ou a des restaurants liés
     */
    @SuppressWarnings("unused")
    public boolean deleteCity(Integer id) {
        logger.info("Service: Suppression de la ville ID {}", id);

        City city = cityDao.findById(id);
        if (city == null) {
            logger.error("Erreur: La ville a supprimé avec l'ID {} n'existe pas", id);
            return false;
        }

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

    public int countRestaurantsInCity(Integer cityId) {
        logger.debug("Service: Comptage des restaurants dans la ville ID {}", cityId);
        return restaurantDao.findByCity(cityId).size();
    }

    public boolean cityExistsByZipCode(String zipCode) {
        return cityDao.findByZipCode(zipCode) != null;
    }

    public boolean cityHasRestaurants(Integer cityId) {
        return countRestaurantsInCity(cityId) > 0;
    }

    @SuppressWarnings("unused")
    public int countCities() {
        return cityDao.findAll().size();
    }

    @SuppressWarnings("unused")
    public List<City> getCitiesWithRestaurants() {
        logger.debug("Service: Récupération des villes avec restaurants");
        List<City> allCities = cityDao.findAll();

        return allCities.stream()
                .filter(city -> cityHasRestaurants(city.getId()))
                .toList();
    }

    @SuppressWarnings("unused")
    public List<City> getCitiesWithoutRestaurants() {
        logger.debug("Service: Récupération des villes sans restaurant");
        List<City> allCities = cityDao.findAll();

        return allCities.stream()
                .filter(city -> !cityHasRestaurants(city.getId()))
                .toList();
    }
}