package ch.hearc.ig.guideresto.business;

import jakarta.persistence.*;

@Embeddable  // ← Dit que cette classe peut être embarquée dans une entité
public class Localisation {

    @Column(name = "ADRESSE", length = 100)
    private String street;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_VILL", nullable = false)
    private City city;

    public Localisation() {
        this(null, null);
    }

    public Localisation(String street, City city) {
        this.street = street;
        this.city = city;
    }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public City getCity() { return city; }
    public void setCity(City city) { this.city = city; }
}