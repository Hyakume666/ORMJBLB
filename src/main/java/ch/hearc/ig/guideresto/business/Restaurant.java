package ch.hearc.ig.guideresto.business;

import jakarta.persistence.*;
import org.apache.commons.collections4.CollectionUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * @author cedric.baudet
 */

@Entity
@Table(name = "RESTAURANTS")
public class Restaurant implements IBusinessObject {

    @Id
    @Column(name = "NUMERO")  // ← Le nom de la colonne en BD
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_restaurants")
    @SequenceGenerator(
            name = "seq_restaurants",
            sequenceName = "SEQ_RESTAURANTS",
            allocationSize = 1
    )
    private Integer id;

    @Column(name = "NOM", nullable = false, length = 100)
    private String name;

    @Column(name = "DESCRIPTION")
    @Lob  // Pour les CLOB (grands textes) spéc. Oracle
    private String description;

    @Column(name = "SITE_WEB", length = 100)
    private String website;

    // ASSOCIATIONS
    // Restaurant -> RestaurantType (ManyToOne)
    @ManyToOne(fetch = FetchType.LAZY)  // LAZY = chargement à la demande
    @JoinColumn(name = "FK_TYPE", nullable = false)  // Nom de la colonne FK en BD
    private RestaurantType type;

    // Restaurant -> Restaurant (OneToMany)
    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Evaluation> evaluations;

    // Restaurant -> Localisation (Embedded)
    @Embedded
    private Localisation address;

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