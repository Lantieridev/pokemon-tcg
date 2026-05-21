package ar.edu.utn.frc.tup.piii.persistence.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cards")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardEntity {

    @Id
    private String id;

    private String name;
    private String supertype;
    private String subtype;
    private Integer hp;

    @JdbcTypeCode(SqlTypes.JSON)
    private Object rules;

    @JdbcTypeCode(SqlTypes.JSON)
    private Object attacks;

    @JdbcTypeCode(SqlTypes.JSON)
    private Object weaknesses;

    @JdbcTypeCode(SqlTypes.JSON)
    private Object resistances;

    @JdbcTypeCode(SqlTypes.JSON)
    private Object retreatCost;

    @Column(name = "set_id")
    private String setId;
}
