package ch.hearc.ig.guideresto.persistence.dao;

import ch.hearc.ig.guideresto.business.City;
import jakarta.persistence.TypedQuery;

import java.util.List;

/**
 * Data Access Object pour l'entité City
 */
public class CityDao extends AbstractDao<City> {

    public CityDao() {
        super(City.class);
    }

    /**
     * Récupère toutes les villes (utilise une Named Query)
     * @return Liste de toutes les villes, triées par nom
     */
    @Override
    public List<City> findAll() {
        TypedQuery<City> query = getEntityManager().createNamedQuery("City.findAll", City.class);
        return query.getResultList();
    }

    /**
     * Recherche une ville par son code postal
     * @param zipCode Le code postal à rechercher
     * @return La ville trouvée, ou null si non trouvée
     */
    public City findByZipCode(String zipCode) {
        TypedQuery<City> query = getEntityManager()
                .createNamedQuery("City.findByZipCode", City.class)
                .setParameter("zipCode", zipCode);

        List<City> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Recherche des villes par nom (recherche partielle, insensible à la casse)
     * @param cityName Le nom de ville à rechercher (peut être partiel)
     * @return Liste des villes correspondantes
     */
    public List<City> findByCityName(String cityName) {
        TypedQuery<City> query = getEntityManager()
                .createNamedQuery("City.findByCityName", City.class)
                .setParameter("cityName", "%" + cityName + "%");

        return query.getResultList();
    }
}