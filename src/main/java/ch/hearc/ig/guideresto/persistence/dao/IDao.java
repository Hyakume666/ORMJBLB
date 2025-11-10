package ch.hearc.ig.guideresto.persistence.dao;

import java.util.List;

/**
 * Interface générique pour les Data Access Objects
 * @param <T> Le type d'entité manipulée par ce DAO
 */
public interface IDao<T> {

    /**
     * Recherche une entité par son ID
     * @param id L'identifiant de l'entité
     * @return L'entité trouvée, ou null si non trouvée
     */
    T findById(Integer id);

    /**
     * Récupère toutes les instances de l'entité
     * @return Liste de toutes les entités
     */
    List<T> findAll();

    /**
     * Persiste une nouvelle entité ou met à jour une entité existante
     * @param entity L'entité à sauvegarder
     * @return L'entité sauvegardée
     */
    T save(T entity);

    /**
     * Supprime une entité
     * @param entity L'entité à supprimer
     */
    void delete(T entity);

    /**
     * Supprime une entité par son ID
     * @param id L'identifiant de l'entité à supprimer
     */
    void deleteById(Integer id);
}