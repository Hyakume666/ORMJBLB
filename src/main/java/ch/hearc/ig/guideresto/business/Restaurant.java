package ch.hearc.ig.guideresto.business;

import jakarta.persistence.*;
import org.apache.commons.collections4.CollectionUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * @author cedric.baudet
 */

@Entity  // ← Dit à Hibernate : "Cette classe est une entité à persister"
@Table(name = "RESTAURANTS")  // ← Le nom de la table en BD
public class Restaurant implements IBusinessObject {

    @Id  // ← C'est la clé primaire
    @Column(name = "NUMERO")  // ← Le nom de la colonne en BD
    private Integer id;

    @Column(name = "NOM", nullable = false, length = 100)
    private String name;

    @Column(name = "DESCRIPTION")
    @Lob  // ← Pour les CLOB (grands textes) en Oracle
    private String description;

    @Column(name = "SITE_WEB", length = 100)
    private String website;

    // IGNORER les associations pour l'instant
    @Transient  // ← Dit à Hibernate : "N'essaie pas de persister ça"
    private Set<Evaluation> evaluations;

    @Transient
    private Localisation address;

    @Transient
    private RestaurantType type;

    public Restaurant() {
        this(null, null, null, null, null, null);
    }

    public Restaurant(Integer id, String name, String description, String website, String street, City city, RestaurantType type) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.website = website;
        this.evaluations = new HashSet();
        this.address = new Localisation(street, city);
        this.type = type;
    }

    public Restaurant(Integer id, String name, String description, String website, Localisation address, RestaurantType type) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.website = website;
        this.evaluations = new HashSet();
        this.address = address;
        this.type = type;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public Set<Evaluation> getEvaluations() {
        return evaluations;
    }

    public void setEvaluations(Set<Evaluation> evaluations) {
        this.evaluations = evaluations;
    }

    public Localisation getAddress() {
        return address;
    }

    public void setAddress(Localisation address) {
        this.address = address;
    }

    public RestaurantType getType() {
        return type;
    }

    public void setType(RestaurantType type) {
        this.type = type;
    }

    public boolean hasEvaluations() {
        return CollectionUtils.isNotEmpty(evaluations);
    }
}