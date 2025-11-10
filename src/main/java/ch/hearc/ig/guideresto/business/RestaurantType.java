package ch.hearc.ig.guideresto.business;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * @author cedric.baudet
 */

@Entity
@Table(name = "TYPES_GASTRONOMIQUES")
public class RestaurantType implements IBusinessObject {

    @Id
    @Column(name = "NUMERO")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_types_gastro")
    @SequenceGenerator(
            name = "seq_types_gastro",
            sequenceName = "SEQ_TYPES_GASTRONOMIQUES",
            allocationSize = 1
    )
    private Integer id;

    @Column(name = "LIBELLE", nullable = false, unique = true, length = 100)
    private String label;

    @Column(name = "DESCRIPTION", nullable = false)
    @Lob
    private String description;

    // ASSOCIATION INVERSE

    // RestaurantType -> Restaurants (OneToMany)
    @OneToMany(mappedBy = "type", fetch = FetchType.LAZY)
    private Set<Restaurant> restaurants;

    public RestaurantType() {
        this(null, null);
    }

    public RestaurantType(String label, String description) {
        this(null, label, description);
    }

    public RestaurantType(Integer id, String label, String description) {
        this.restaurants = new HashSet();
        this.id = id;
        this.label = label;
        this.description = description;
    }

    @Override
    public String toString() {
        return label;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<Restaurant> getRestaurants() {
        return restaurants;
    }

    public void setRestaurants(Set<Restaurant> restaurants) {
        this.restaurants = restaurants;
    }

}