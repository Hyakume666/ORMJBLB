package ch.hearc.ig.guideresto.presentation;

import ch.hearc.ig.guideresto.business.*;
import ch.hearc.ig.guideresto.persistence.dao.*;
import ch.hearc.ig.guideresto.persistence.jpa.JpaUtils;
import jakarta.persistence.EntityManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Classe de test pour les DAO (Exercice 5)
 */
public class DaoTest {

    private static final Logger logger = LogManager.getLogger(DaoTest.class);

    public static void main(String[] args) {
        logger.info("=== TESTS DES DAO (EXERCICE 5) ===\n");

        try {
            EntityManager em = JpaUtils.getEntityManager();

            // Création des DAO
            CityDao cityDao = new CityDao();
            RestaurantTypeDao typeDao = new RestaurantTypeDao();
            RestaurantDao restaurantDao = new RestaurantDao();
            EvaluationCriteriaDao criteriaDao = new EvaluationCriteriaDao();

            // ==================== TEST 1 : City DAO ====================
            logger.info("--- Test 1 : CityDao ---");

            // findAll
            List<City> allCities = cityDao.findAll();
            logger.info("Nombre de villes : {}", allCities.size());

            // findByZipCode
            City city = cityDao.findByZipCode("2000");
            if (city != null) {
                logger.info("Ville avec NPA 2000 : {}", city.getCityName());
            }

            // findByCityName (recherche partielle)
            List<City> cities = cityDao.findByCityName("neu");
            logger.info("Villes contenant 'neu' : {}", cities.size());
            for (City c : cities) {
                logger.info("  - {} {}", c.getZipCode(), c.getCityName());
            }

            // ==================== TEST 2 : RestaurantType DAO ====================
            logger.info("\n--- Test 2 : RestaurantTypeDao ---");

            // findAll
            List<RestaurantType> allTypes = typeDao.findAll();
            logger.info("Nombre de types : {}", allTypes.size());
            for (RestaurantType type : allTypes) {
                logger.info("  - {}", type.getLabel());
            }

            // findByLabel
            List<RestaurantType> types = typeDao.findByLabel("pizza");
            logger.info("Types contenant 'pizza' : {}", types.size());

            // findByExactLabel
            RestaurantType pizzeria = typeDao.findByExactLabel("Pizzeria");
            if (pizzeria != null) {
                logger.info("Type trouvé : {} (ID: {})", pizzeria.getLabel(), pizzeria.getId());
            }

            // ==================== TEST 3 : Restaurant DAO ====================
            logger.info("\n--- Test 3 : RestaurantDao ---");

            // findAll
            List<Restaurant> allRestaurants = restaurantDao.findAll();
            logger.info("Nombre de restaurants : {}", allRestaurants.size());

            // findByName
            List<Restaurant> restaurants = restaurantDao.findByName("fleur");
            logger.info("Restaurants contenant 'fleur' : {}", restaurants.size());
            for (Restaurant r : restaurants) {
                logger.info("  - {} ({})", r.getName(), r.getAddress().getCity().getCityName());
            }

            // findByCity
            if (city != null) {
                List<Restaurant> restaurantsInCity = restaurantDao.findByCity(city.getId());
                logger.info("Restaurants à {} : {}", city.getCityName(), restaurantsInCity.size());
            }

            // findByType
            if (pizzeria != null) {
                List<Restaurant> pizzerias = restaurantDao.findByType(pizzeria.getId());
                logger.info("Pizzerias : {}", pizzerias.size());
            }

            // ==================== TEST 4 : EvaluationCriteria DAO ====================
            logger.info("\n--- Test 4 : EvaluationCriteriaDao ---");

            // findAll
            List<EvaluationCriteria> allCriteria = criteriaDao.findAll();
            logger.info("Nombre de critères : {}", allCriteria.size());
            for (EvaluationCriteria criteria : allCriteria) {
                logger.info("  - {} : {}", criteria.getName(), criteria.getDescription());
            }

            // findByName
            List<EvaluationCriteria> criterias = criteriaDao.findByName("service");
            logger.info("Critères contenant 'service' : {}", criterias.size());

            // findByExactName
            EvaluationCriteria serviceCriteria = criteriaDao.findByExactName("Service");
            if (serviceCriteria != null) {
                logger.info("Critère 'Service' trouvé (ID: {})", serviceCriteria.getId());
            }

            // ==================== TEST 5 : Opérations CRUD ====================
            logger.info("\n--- Test 5 : Opérations CRUD (Create, Update, Delete) ---");

            // CREATE : Créer une nouvelle ville
            City newCity = new City("2500", "Bienne");
            logger.info("Création d'une nouvelle ville : {} {}", newCity.getZipCode(), newCity.getCityName());
            newCity = cityDao.save(newCity);  // ← Récupérer l'entité retournée
            logger.info("Ville créée avec ID : {}", newCity.getId());

            // READ : Relire la ville
            City readCity = cityDao.findById(newCity.getId());
            logger.info("Ville relue : {} {}", readCity.getZipCode(), readCity.getCityName());

            // UPDATE : Modifier la ville
            readCity.setCityName("Biel/Bienne");
            readCity = cityDao.save(readCity);  // ← Récupérer l'entité retournée
            logger.info("Ville modifiée : {}", readCity.getCityName());

            // DELETE : Supprimer la ville
            cityDao.delete(readCity);
            logger.info("Ville supprimée");

            // Vérifier la suppression
            City deletedCity = cityDao.findById(newCity.getId());
            logger.info("Ville après suppression : {}", deletedCity == null ? "null (OK)" : "encore présente (ERREUR)");

            em.close();
            logger.info("\n✓ Tous les tests DAO réussis !");

        } catch (Exception e) {
            logger.error("ERREUR lors des tests DAO", e);
        }

        logger.info("\n=== FIN TESTS DAO ===");
    }
}