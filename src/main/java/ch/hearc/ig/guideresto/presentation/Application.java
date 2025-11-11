package ch.hearc.ig.guideresto.presentation;

import ch.hearc.ig.guideresto.business.*;
import ch.hearc.ig.guideresto.service.RestaurantService;
import ch.hearc.ig.guideresto.service.EvaluationService;
import ch.hearc.ig.guideresto.persistence.dao.CityDao;
import ch.hearc.ig.guideresto.persistence.dao.RestaurantTypeDao;
import ch.hearc.ig.guideresto.persistence.dao.EvaluationCriteriaDao;
import ch.hearc.ig.guideresto.persistence.jpa.JpaUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import jakarta.persistence.EntityManager;

import java.util.*;

/**
 * Application principale GuideResto - VERSION MODIFIÉE AVEC SERVICES
 *
 * CHANGEMENTS PAR RAPPORT À LA VERSION ORIGINALE :
 * - Utilisation des services au lieu de FakeItems
 * - Utilisation des DAO pour City, RestaurantType, EvaluationCriteria
 * - Ajout de validation et gestion d'erreurs
 * - Ajout de statistiques dans l'affichage des restaurants
 *
 * @author cedric.baudet
 * @author alain.matile
 * @author loic.barthoulot (modifications avec services)
 * @author jeremie.bressoud (modifications avec services)
 */
public class Application {

    private static Scanner scanner;
    private static final Logger logger = LogManager.getLogger(Application.class);

    // ============= Déclaration des services =============
    private static RestaurantService restaurantService;
    private static EvaluationService evaluationService;
    private static CityDao cityDao;
    private static RestaurantTypeDao typeDao;
    private static EvaluationCriteriaDao criteriaDao;

    public static void main(String[] args) {

        // Test Hibernate
        logger.info("=== DÉMARRAGE APPLICATION GUIDERESTO ===");

        try {
            EntityManager em = JpaUtils.getEntityManager();
            logger.info("EntityManager créé avec succès !");

            // ============= Initialisation des services =============
            restaurantService = new RestaurantService();
            evaluationService = new EvaluationService();
            cityDao = new CityDao();
            typeDao = new RestaurantTypeDao();
            criteriaDao = new EvaluationCriteriaDao();
            logger.info("Services initialisés avec succès !");

        } catch (Exception e) {
            logger.error("ERREUR lors de l'initialisation", e);
            System.out.println("Erreur de connexion à la base de données. Veuillez vérifier vos paramètres.");
            return;
        }

        scanner = new Scanner(System.in);
        System.out.println("Bienvenue dans GuideResto ! Que souhaitez-vous faire ?");
        int choice;
        do {
            printMainMenu();
            choice = readInt();
            proceedMainMenu(choice);
        } while (choice != 0);
    }

    /**
     * Affichage du menu principal de l'application
     */
    private static void printMainMenu() {
        System.out.println("======================================================");
        System.out.println("Que voulez-vous faire ?");
        System.out.println("1. Afficher la liste de tous les restaurants");
        System.out.println("2. Rechercher un restaurant par son nom");
        System.out.println("3. Rechercher un restaurant par ville");
        System.out.println("4. Rechercher un restaurant par son type de cuisine");
        System.out.println("5. Saisir un nouveau restaurant");
        System.out.println("0. Quitter l'application");
    }

    /**
     * On gère le choix saisi par l'utilisateur
     */
    private static void proceedMainMenu(int choice) {
        switch (choice) {
            case 1:
                showRestaurantsList();
                break;
            case 2:
                searchRestaurantByName();
                break;
            case 3:
                searchRestaurantByCity();
                break;
            case 4:
                searchRestaurantByType();
                break;
            case 5:
                addNewRestaurant();
                break;
            case 0:
                System.out.println("Au revoir !");
                break;
            default:
                System.out.println("Erreur : saisie incorrecte. Veuillez réessayer");
                break;
        }
    }

    /**
     * On affiche à l'utilisateur une liste de restaurants numérotés, et il doit en sélectionner un
     */
    private static Restaurant pickRestaurant(Set<Restaurant> restaurants) {
        if (restaurants.isEmpty()) {
            System.out.println("Aucun restaurant n'a été trouvé !");
            return null;
        }

        String result;
        for (Restaurant currentRest : restaurants) {
            result = "";
            result = "\"" + result + currentRest.getName() + "\" - " + currentRest.getAddress().getStreet() + " - ";
            result = result + currentRest.getAddress().getCity().getZipCode() + " " + currentRest.getAddress().getCity().getCityName();
            System.out.println(result);
        }

        System.out.println("Veuillez saisir le nom exact du restaurant dont vous voulez voir le détail, ou appuyez sur Enter pour revenir en arrière");
        String choice = readString();

        return searchRestaurantByName(restaurants, choice);
    }

    /**
     * ============= Utilise restaurantService au lieu de FakeItems =============
     */
    private static void showRestaurantsList() {
        System.out.println("Liste des restaurants : ");

        // AVANT : Restaurant restaurant = pickRestaurant(FakeItems.getAllRestaurants());
        // APRÈS :
        List<Restaurant> allRestaurants = restaurantService.getAllRestaurants();
        Restaurant restaurant = pickRestaurant(new LinkedHashSet<>(allRestaurants));

        if (restaurant != null) {
            showRestaurant(restaurant);
        }
    }

    /**
     * ============= Utilise restaurantService au lieu de FakeItems =============
     */
    private static void searchRestaurantByName() {
        System.out.println("Veuillez entrer une partie du nom recherché : ");
        String research = readString();

        // AVANT : Filtrage manuel avec FakeItems
        // APRÈS : Le service fait le filtrage
        List<Restaurant> filteredList = restaurantService.searchRestaurantsByName(research);
        Restaurant restaurant = pickRestaurant(new LinkedHashSet<>(filteredList));

        if (restaurant != null) {
            showRestaurant(restaurant);
        }
    }

    /**
     * ============= Utilise cityDao et restaurantService =============
     */
    private static void searchRestaurantByCity() {
        System.out.println("Veuillez entrer une partie du nom de la ville désirée : ");
        String research = readString();

        // NOUVEAU : Utiliser CityDao pour trouver les villes
        List<City> cities = cityDao.findByCityName(research);

        if (cities.isEmpty()) {
            System.out.println("Aucune ville trouvée avec ce nom.");
            return;
        }

        // Si plusieurs villes, prendre la première (ou on pourrait laisser choisir)
        City city = cities.get(0);
        logger.info("Ville sélectionnée : {} {}", city.getZipCode(), city.getCityName());

        // Le service récupère les restaurants de cette ville
        List<Restaurant> filteredList = restaurantService.getRestaurantsByCity(city.getId());
        Restaurant restaurant = pickRestaurant(new LinkedHashSet<>(filteredList));

        if (restaurant != null) {
            showRestaurant(restaurant);
        }
    }

    /**
     * L'utilisateur choisit une ville parmi celles présentes dans le système.
     * ============= Utilise cityDao au lieu de FakeItems =============
     */
    private static City pickCity(Set<City> cities) {
        System.out.println("Voici la liste des villes possibles, veuillez entrer le NPA de la ville désirée : ");

        for (City currentCity : cities) {
            System.out.println(currentCity.getZipCode() + " " + currentCity.getCityName());
        }
        System.out.println("Entrez \"NEW\" pour créer une nouvelle ville");
        String choice = readString();

        if (choice.equals("NEW")) {
            // Création avec le DAO
            City city = new City();
            System.out.println("Veuillez entrer le NPA de la nouvelle ville : ");
            city.setZipCode(readString());
            System.out.println("Veuillez entrer le nom de la nouvelle ville : ");
            city.setCityName(readString());

            // Sauvegarder la ville
            city = cityDao.save(city);
            logger.info("Nouvelle ville créée avec ID : {}", city.getId());

            return city;
        }

        return searchCityByZipCode(cities, choice);
    }

    /**
     * L'utilisateur choisit un type de restaurant parmi ceux présents dans le système.
     */
    private static RestaurantType pickRestaurantType(Set<RestaurantType> types) {
        System.out.println("Voici la liste des types possibles, veuillez entrer le libellé exact du type désiré : ");
        for (RestaurantType currentType : types) {
            System.out.println("\"" + currentType.getLabel() + "\" : " + currentType.getDescription());
        }
        String choice = readString();

        return searchTypeByLabel(types, choice);
    }

    /**
     * ============= Utilise typeDao et restaurantService =============
     */
    private static void searchRestaurantByType() {
        // NOUVEAU : Récupérer les types depuis le DAO
        List<RestaurantType> types = typeDao.findAll();
        RestaurantType chosenType = pickRestaurantType(new LinkedHashSet<>(types));

        if (chosenType == null) {
            return;
        }

        // Le service récupère les restaurants de ce type
        List<Restaurant> filteredList = restaurantService.getRestaurantsByType(chosenType.getId());
        Restaurant restaurant = pickRestaurant(new LinkedHashSet<>(filteredList));

        if (restaurant != null) {
            showRestaurant(restaurant);
        }
    }

    /**
     * ============= Utilise restaurantService pour créer le restaurant =============
     */
    private static void addNewRestaurant() {
        System.out.println("Vous allez ajouter un nouveau restaurant !");
        System.out.println("Quel est son nom ?");
        String name = readString();
        System.out.println("Veuillez entrer une courte description : ");
        String description = readString();
        System.out.println("Veuillez entrer l'adresse de son site internet : ");
        String website = readString();
        System.out.println("Rue : ");
        String street = readString();

        City city = null;
        do {
            // Utilise cityDao
            city = pickCity(new LinkedHashSet<>(cityDao.findAll()));
        } while (city == null);

        RestaurantType restaurantType = null;
        do {
            // Utilise typeDao
            restaurantType = pickRestaurantType(new LinkedHashSet<>(typeDao.findAll()));
        } while (restaurantType == null);

        // ============= Création via le service (avec validation !) =============
        Restaurant restaurant = restaurantService.createRestaurant(
                name,
                description,
                website,
                street,
                city.getId(),
                restaurantType.getId()
        );

        if (restaurant != null) {
            System.out.println("Restaurant créé avec succès !");
            showRestaurant(restaurant);
        } else {
            System.out.println("Erreur lors de la création du restaurant.");
        }
    }

    /**
     * ============= Affiche aussi les statistiques via evaluationService =============
     */
    private static void showRestaurant(Restaurant restaurant) {
        System.out.println("Affichage d'un restaurant : ");
        StringBuilder sb = new StringBuilder();
        sb.append(restaurant.getName()).append("\n");
        sb.append(restaurant.getDescription()).append("\n");
        sb.append(restaurant.getType().getLabel()).append("\n");
        sb.append(restaurant.getWebsite()).append("\n");
        sb.append(restaurant.getAddress().getStreet()).append(", ");
        sb.append(restaurant.getAddress().getCity().getZipCode()).append(" ").append(restaurant.getAddress().getCity().getCityName()).append("\n");

        // ============= Utilise evaluationService pour les statistiques =============
        int likes = evaluationService.countLikes(restaurant.getId());
        int dislikes = evaluationService.countDislikes(restaurant.getId());
        sb.append("Nombre de likes : ").append(likes).append("\n");
        sb.append("Nombre de dislikes : ").append(dislikes).append("\n");

        sb.append("\nEvaluations reçues : ").append("\n");

        // Récupérer les évaluations complètes via le service
        List<CompleteEvaluation> completeEvaluations = evaluationService.getCompleteEvaluations(restaurant.getId());
        for (CompleteEvaluation ce : completeEvaluations) {
            sb.append(getCompleteEvaluationDescription(ce)).append("\n");
        }

        // ============= Afficher les moyennes =============
        double avgOverall = evaluationService.getOverallAverageGrade(restaurant.getId());
        if (avgOverall > 0) {
            sb.append("\n=== MOYENNES DES NOTES ===\n");
            double avgService = evaluationService.getAverageGradeForCriteria(restaurant.getId(), "Service");
            double avgCuisine = evaluationService.getAverageGradeForCriteria(restaurant.getId(), "Cuisine");
            double avgCadre = evaluationService.getAverageGradeForCriteria(restaurant.getId(), "Cadre");

            sb.append("Service : ").append(String.format("%.1f", avgService)).append("/5\n");
            sb.append("Cuisine : ").append(String.format("%.1f", avgCuisine)).append("/5\n");
            sb.append("Cadre : ").append(String.format("%.1f", avgCadre)).append("/5\n");
            sb.append("Moyenne générale : ").append(String.format("%.1f", avgOverall)).append("/5\n");
        }

        System.out.println(sb);

        int choice;
        do {
            showRestaurantMenu();
            choice = readInt();
            proceedRestaurantMenu(choice, restaurant);
        } while (choice != 0 && choice != 6);
    }

    /**
     * Compte le nombre de likes - GARDÉ pour compatibilité mais maintenant redondant
     * (evaluationService.countLikes fait la même chose)
     */
    private static int countLikes(Set<Evaluation> evaluations, Boolean likeRestaurant) {
        int count = 0;
        for (Evaluation currentEval : evaluations) {
            if (currentEval instanceof BasicEvaluation && ((BasicEvaluation) currentEval).getLikeRestaurant() == likeRestaurant) {
                count++;
            }
        }
        return count;
    }

    /**
     * Retourne un String qui contient le détail complet d'une CompleteEvaluation
     */
    private static String getCompleteEvaluationDescription(Evaluation eval) {
        StringBuilder result = new StringBuilder();

        if (eval instanceof CompleteEvaluation) {
            CompleteEvaluation ce = (CompleteEvaluation) eval;
            result.append("Evaluation de : ").append(ce.getUsername()).append("\n");
            result.append("Commentaire : ").append(ce.getComment()).append("\n");
            for (Grade currentGrade : ce.getGrades()) {
                result.append(currentGrade.getCriteria().getName()).append(" : ").append(currentGrade.getGrade()).append("/5").append("\n");
            }
        }

        return result.toString();
    }

    /**
     * Affiche dans la console un ensemble d'actions réalisables sur le restaurant
     */
    private static void showRestaurantMenu() {
        System.out.println("======================================================");
        System.out.println("Que souhaitez-vous faire ?");
        System.out.println("1. J'aime ce restaurant !");
        System.out.println("2. Je n'aime pas ce restaurant !");
        System.out.println("3. Faire une évaluation complète de ce restaurant !");
        System.out.println("4. Editer ce restaurant");
        System.out.println("5. Editer l'adresse du restaurant");
        System.out.println("6. Supprimer ce restaurant");
        System.out.println("0. Revenir au menu principal");
    }

    /**
     * Traite le choix saisi par l'utilisateur
     */
    private static void proceedRestaurantMenu(int choice, Restaurant restaurant) {
        switch (choice) {
            case 1:
                addBasicEvaluation(restaurant, true);
                break;
            case 2:
                addBasicEvaluation(restaurant, false);
                break;
            case 3:
                evaluateRestaurant(restaurant);
                break;
            case 4:
                editRestaurant(restaurant);
                break;
            case 5:
                editRestaurantAddress(restaurant);
                break;
            case 6:
                deleteRestaurant(restaurant);
                break;
            case 0:
                break;
            default:
                break;
        }
    }

    /**
     * ============= Utilise evaluationService =============
     */
    private static void addBasicEvaluation(Restaurant restaurant, Boolean like) {
        // NOUVEAU : Le service gère tout (IP, date, etc.)
        BasicEvaluation eval = evaluationService.addBasicEvaluation(restaurant.getId(), like);

        if (eval != null) {
            System.out.println("Votre vote a été pris en compte !");
        } else {
            System.out.println("Erreur lors de l'enregistrement de votre vote.");
        }
    }

    /**
     * ============= Utilise evaluationService =============
     */
    private static void evaluateRestaurant(Restaurant restaurant) {
        System.out.println("Merci d'évaluer ce restaurant !");
        System.out.println("Quel est votre nom d'utilisateur ? ");
        String username = readString();
        System.out.println("Quel commentaire aimeriez-vous publier ?");
        String comment = readString();

        // Créer la map des notes
        Map<String, Integer> criteriaGrades = new HashMap<>();

        // MODIFIÉ : Utilise criteriaDao
        List<EvaluationCriteria> allCriteria = criteriaDao.findAll();

        System.out.println("Veuillez svp donner une note entre 1 et 5 pour chacun de ces critères : ");
        for (EvaluationCriteria currentCriteria : allCriteria) {
            System.out.println(currentCriteria.getName() + " : " + currentCriteria.getDescription());
            Integer note = readInt();

            // Valider que la note est entre 1 et 5
            while (note < 1 || note > 5) {
                System.out.println("La note doit être entre 1 et 5. Veuillez réessayer : ");
                note = readInt();
            }

            criteriaGrades.put(currentCriteria.getName(), note);
        }

        // ============= NOUVEAU : Création via le service (avec validation !) =============
        CompleteEvaluation eval = evaluationService.addCompleteEvaluation(
                restaurant.getId(),
                username,
                comment,
                criteriaGrades
        );

        if (eval != null) {
            System.out.println("Votre évaluation a bien été enregistrée, merci !");
        } else {
            System.out.println("Erreur lors de l'enregistrement de votre évaluation.");
        }
    }

    /**
     * ============= Utilise restaurantService =============
     */
    private static void editRestaurant(Restaurant restaurant) {
        System.out.println("Edition d'un restaurant !");

        System.out.println("Nouveau nom : ");
        String newName = readString();
        System.out.println("Nouvelle description : ");
        String newDescription = readString();
        System.out.println("Nouveau site web : ");
        String newWebsite = readString();

        // Mise à jour via le service
        Restaurant updated = restaurantService.updateRestaurant(
                restaurant.getId(),
                newName,
                newDescription,
                newWebsite
        );

        if (updated != null) {
            System.out.println("Restaurant de base mis à jour !");
        }

        System.out.println("Nouveau type de restaurant : ");
        RestaurantType newType = pickRestaurantType(new LinkedHashSet<>(typeDao.findAll()));

        if (newType != null && !newType.getId().equals(restaurant.getType().getId())) {
            // NOUVEAU : Changement de type via le service
            updated = restaurantService.updateRestaurantType(restaurant.getId(), newType.getId());
            if (updated != null) {
                System.out.println("Type du restaurant mis à jour !");
            }
        }

        System.out.println("Merci, le restaurant a bien été modifié !");
    }

    /**
     * ============= Utilise restaurantService =============
     */
    private static void editRestaurantAddress(Restaurant restaurant) {
        System.out.println("Edition de l'adresse d'un restaurant !");

        System.out.println("Nouvelle rue : ");
        String newStreet = readString();

        City newCity = pickCity(new LinkedHashSet<>(cityDao.findAll()));

        if (newCity != null) {
            // Mise à jour de l'adresse via le service
            Restaurant updated = restaurantService.updateRestaurantAddress(
                    restaurant.getId(),
                    newStreet,
                    newCity.getId()
            );

            if (updated != null) {
                System.out.println("L'adresse a bien été modifiée ! Merci !");
            } else {
                System.out.println("Erreur lors de la modification de l'adresse.");
            }
        }
    }

    /**
     * ============= Utilise restaurantService =============
     */
    private static void deleteRestaurant(Restaurant restaurant) {
        System.out.println("Etes-vous sûr de vouloir supprimer ce restaurant ? (O/n)");
        String choice = readString();
        if (choice.equals("o") || choice.equals("O")) {
            // NOUVEAU : Suppression via le service
            boolean deleted = restaurantService.deleteRestaurant(restaurant.getId());

            if (deleted) {
                System.out.println("Le restaurant a bien été supprimé !");
            } else {
                System.out.println("Erreur lors de la suppression du restaurant.");
            }
        }
    }

    /**
     * Recherche dans le Set le restaurant comportant le nom passé en paramètre.
     */
    private static Restaurant searchRestaurantByName(Set<Restaurant> restaurants, String name) {
        for (Restaurant current : restaurants) {
            if (current.getName().equalsIgnoreCase(name)) {
                return current;
            }
        }
        return null;
    }

    /**
     * Recherche dans le Set la ville comportant le code NPA passé en paramètre.
     */
    private static City searchCityByZipCode(Set<City> cities, String zipCode) {
        for (City current : cities) {
            if (current.getZipCode().equalsIgnoreCase(zipCode)) {
                return current;
            }
        }
        return null;
    }

    /**
     * Recherche dans le Set le type comportant le libellé passé en paramètre.
     */
    private static RestaurantType searchTypeByLabel(Set<RestaurantType> types, String label) {
        for (RestaurantType current : types) {
            if (current.getLabel().equalsIgnoreCase(label)) {
                return current;
            }
        }
        return null;
    }

    /**
     * readInt ne repositionne pas le scanner au début d'une ligne
     */
    private static int readInt() {
        int i = 0;
        boolean success = false;
        do {
            try {
                i = scanner.nextInt();
                success = true;
            } catch (InputMismatchException e) {
                System.out.println("Erreur ! Veuillez entrer un nombre entier s'il vous plaît !");
            } finally {
                scanner.nextLine();
            }

        } while (!success);

        return i;
    }

    /**
     * Méthode readString pour rester consistant avec readInt !
     */
    private static String readString() {
        return scanner.nextLine();
    }

}