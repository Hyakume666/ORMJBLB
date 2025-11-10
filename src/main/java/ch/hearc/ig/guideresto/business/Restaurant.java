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
@NamedQueries({
        @NamedQuery(
                name = "Restaurant.findAll",
                query = "SELECT r FROM Restaurant r ORDER BY r.name"
        ),
        @NamedQuery(
                name = "Restaurant.findByName",
                query = "SELECT r FROM Restaurant r WHERE UPPER(r.name) LIKE UPPER(:name)"
        ),
        @NamedQuery(
                name = "Restaurant.findByCity",
                query = "SELECT r FROM Restaurant r WHERE r.address.city.id = :cityId"
        ),
        @NamedQuery(
                name = "Restaurant.findByType",
                query = "SELECT r FROM Restaurant r WHERE r.type.id = :typeId"
        )
})
public class Restaurant implements IBusinessObject {

    @Id
    @Column(name = "NUMERO")
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
    @Lob
    private String description;

    @Column(name = "SITE_WEB", length = 100)
    private String website;

    // ASSOCIATIONS

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_TYPE", nullable = false)
    private RestaurantType type;

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Evaluation> evaluations;

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
        this.evaluations = new HashSet<>();
        this.address = new Localisation(street, city);
        this.type = type;
    }

    public Restaurant(Integer id, String name, String description, String website, Localisation address, RestaurantType type) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.website = website;
        this.evaluations = new HashSet<>();
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