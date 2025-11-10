package ch.hearc.ig.guideresto.business;

import jakarta.persistence.*;
import java.util.Date;

/**
 * @author cedric.baudet
 */
@Entity
@Table(name = "EVALUATIONS")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Evaluation implements IBusinessObject {

    @Id
    @Column(name = "NUMERO")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_eval")
    @SequenceGenerator(
            name = "seq_eval",
            sequenceName = "SEQ_EVAL",
            allocationSize = 1
    )
    private Integer id;

    @Column(name = "DATE_EVAL", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date visitDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_REST")
    private Restaurant restaurant;

    public Evaluation() {
        this(null, null, null);
    }

    public Evaluation(Integer id, Date visitDate, Restaurant restaurant) {
        this.id = id;
        this.visitDate = visitDate;
        this.restaurant = restaurant;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(Date visitDate) {
        this.visitDate = visitDate;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(Restaurant restaurant) {
        this.restaurant = restaurant;
    }
}