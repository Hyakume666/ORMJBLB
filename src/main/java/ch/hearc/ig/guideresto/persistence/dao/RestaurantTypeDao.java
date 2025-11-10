package ch.hearc.ig.guideresto.persistence.dao;

import ch.hearc.ig.guideresto.business.RestaurantType;
import jakarta.persistence.TypedQuery;

import java.util.List;

/**
 * Data Access Object pour l'entité RestaurantType
 */
public class RestaurantTypeDao extends AbstractDao<RestaurantType> {

    public RestaurantTypeDao() {
        super(RestaurantType.class);
    }

    /**
     * Récupère tous les types de restaurant (utilise une Named Query)
     * @return Liste de tous les types, triés par libellé
     */
    @Override
    public List<RestaurantType> findAll() {
        TypedQuery<RestaurantType> query = getEntityManager()
                .createNamedQuery("RestaurantType.findAll", RestaurantType.class);
        return query.getResultList();
    }

    /**
     * Recherche des types de restaurant par libellé (recherche partielle, insensible à la casse)
     * @param label Le libellé à rechercher (peut être partiel)
     * @return Liste des types correspondants
     */
    public List<RestaurantType> findByLabel(String label) {
        TypedQuery<RestaurantType> query = getEntityManager()
                .createNamedQuery("RestaurantType.findByLabel", RestaurantType.class)
                .setParameter("label", "%" + label + "%");

        return query.getResultList();
    }

    /**
     * Recherche un type de restaurant par son libellé exact
     * @param label Le libellé exact à rechercher
     * @return Le type trouvé, ou null si non trouvé
     */
    public RestaurantType findByExactLabel(String label) {
        List<RestaurantType> types = findByLabel(label);

        // Filtre pour avoir le match exact
        for (RestaurantType type : types) {
            if (type.getLabel().equalsIgnoreCase(label)) {
                return type;
            }
        }

        return null;
    }
}