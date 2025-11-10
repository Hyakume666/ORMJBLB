package ch.hearc.ig.guideresto.business;

import jakarta.persistence.*;

/**
 * @author cedric.baudet
 */
@Entity
@Table(name = "CRITERES_EVALUATION")
@NamedQueries({
        @NamedQuery(
                name = "EvaluationCriteria.findAll",
                query = "SELECT ec FROM EvaluationCriteria ec ORDER BY ec.name"
        ),
        @NamedQuery(
                name = "EvaluationCriteria.findByName",
                query = "SELECT ec FROM EvaluationCriteria ec WHERE UPPER(ec.name) LIKE UPPER(:name)"
        )
})
public class EvaluationCriteria implements IBusinessObject {

    @Id
    @Column(name = "NUMERO")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_criteres")
    @SequenceGenerator(
            name = "seq_criteres",
            sequenceName = "SEQ_CRITERES_EVALUATION",
            allocationSize = 1
    )
    private Integer id;

    @Column(name = "NOM", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "DESCRIPTION", length = 512)
    private String description;

    public EvaluationCriteria() {
        this(null, null);
    }

    public EvaluationCriteria(String name, String description) {
        this(null, name, description);
    }

    public EvaluationCriteria(Integer id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
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
}