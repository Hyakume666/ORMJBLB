package ch.hearc.ig.guideresto.persistence.dao;

import ch.hearc.ig.guideresto.business.EvaluationCriteria;
import jakarta.persistence.TypedQuery;

import java.util.List;

/**
 * Data Access Object pour l'entité EvaluationCriteria
 */
public class EvaluationCriteriaDao extends AbstractDao<EvaluationCriteria> {

    public EvaluationCriteriaDao() {
        super(EvaluationCriteria.class);
    }

    /**
     * Récupère tous les critères d'évaluation (utilise une Named Query)
     * @return Liste de tous les critères, triés par nom
     */
    @Override
    public List<EvaluationCriteria> findAll() {
        TypedQuery<EvaluationCriteria> query = getEntityManager()
                .createNamedQuery("EvaluationCriteria.findAll", EvaluationCriteria.class);
        return query.getResultList();
    }

    /**
     * Recherche des critères par nom (recherche partielle, insensible à la casse)
     * @param name Le nom à rechercher (peut être partiel)
     * @return Liste des critères correspondants
     */
    public List<EvaluationCriteria> findByName(String name) {
        TypedQuery<EvaluationCriteria> query = getEntityManager()
                .createNamedQuery("EvaluationCriteria.findByName", EvaluationCriteria.class)
                .setParameter("name", "%" + name + "%");

        return query.getResultList();
    }

    /**
     * Recherche un critère par son nom exact
     * @param name Le nom exact à rechercher
     * @return Le critère trouvé, ou null si non trouvé
     */
    public EvaluationCriteria findByExactName(String name) {
        List<EvaluationCriteria> criterias = findByName(name);

        // Filtrer pour avoir le match exact
        for (EvaluationCriteria criteria : criterias) {
            if (criteria.getName().equalsIgnoreCase(name)) {
                return criteria;
            }
        }

        return null;
    }
}