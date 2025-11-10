package ch.hearc.ig.guideresto.persistence.dao;

import ch.hearc.ig.guideresto.persistence.jpa.JpaUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.List;

/**
 * Classe abstraite qui implémente les méthodes communes à tous les DAO
 * @param <T> Le type d'entité manipulée
 */
public abstract class AbstractDao<T> implements IDao<T> {

    protected final Class<T> entityClass;

    protected AbstractDao(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    protected EntityManager getEntityManager() {
        return JpaUtils.getEntityManager();
    }

    @Override
    public T findById(Integer id) {
        return getEntityManager().find(entityClass, id);
    }

    @Override
    public List<T> findAll() {
        String jpql = "SELECT e FROM " + entityClass.getSimpleName() + " e";
        TypedQuery<T> query = getEntityManager().createQuery(jpql, entityClass);
        return query.getResultList();
    }

    @Override
    public T save(T entity) {
        final T[] result = (T[]) new Object[1];

        JpaUtils.inTransaction(entityManager -> {
            result[0] = entityManager.merge(entity);
        });

        return result[0];
    }

    @Override
    public void delete(T entity) {
        JpaUtils.inTransaction(entityManager -> {
            T managedEntity = entityManager.merge(entity);
            entityManager.remove(managedEntity);
        });
    }

    @Override
    public void deleteById(Integer id) {
        JpaUtils.inTransaction(entityManager -> {
            T entity = entityManager.find(entityClass, id);
            if (entity != null) {
                entityManager.remove(entity);
            }
        });
    }
}