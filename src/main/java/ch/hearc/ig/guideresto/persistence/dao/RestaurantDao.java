package ch.hearc.ig.guideresto.persistence.dao;

import ch.hearc.ig.guideresto.business.Restaurant;
import jakarta.persistence.TypedQuery;

import java.util.List;

/**
 * Data Access Object pour l'entité Restaurant
 */
public class RestaurantDao extends AbstractDao<Restaurant> {

    public RestaurantDao() {
        super(Restaurant.class);
    }

    /**
     * Récupère tous les restaurants (utilise une Named Query)
     * @return Liste de tous les restaurants, triés par nom
     */
    @Override
    public List<Restaurant> findAll() {
        TypedQuery<Restaurant> query = getEntityManager()
                .createNamedQuery("Restaurant.findAll", Restaurant.class);
        return query.getResultList();
    }

    /**
     * Recherche des restaurants par nom (recherche partielle, insensible à la casse)
     * @param name Le nom à rechercher (peut être partiel)
     * @return Liste des restaurants correspondants
     */
    public List<Restaurant> findByName(String name) {
        TypedQuery<Restaurant> query = getEntityManager()
                .createNamedQuery("Restaurant.findByName", Restaurant.class)
                .setParameter("name", "%" + name + "%");

        return query.getResultList();
    }

    /**
     * Recherche des restaurants par ville
     * @param cityId L'ID de la ville
     * @return Liste des restaurants dans cette ville
     */
    public List<Restaurant> findByCity(Integer cityId) {
        TypedQuery<Restaurant> query = getEntityManager()
                .createNamedQuery("Restaurant.findByCity", Restaurant.class)
                .setParameter("cityId", cityId);

        return query.getResultList();
    }

    /**
     * Recherche des restaurants par type gastronomique
     * @param typeId L'ID du type gastronomique
     * @return Liste des restaurants de ce type
     */
    public List<Restaurant> findByType(Integer typeId) {
        TypedQuery<Restaurant> query = getEntityManager()
                .createNamedQuery("Restaurant.findByType", Restaurant.class)
                .setParameter("typeId", typeId);

        return query.getResultList();
    }
}