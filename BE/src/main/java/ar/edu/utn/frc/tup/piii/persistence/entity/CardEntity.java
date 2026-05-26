package ar.edu.utn.frc.tup.piii.persistence.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
    
    @Column(name = "evolves_from")
    private String evolvesFrom;

    @Convert(converter = JsonbConverter.class)
    @Column(columnDefinition = "jsonb")
    private Object abilities;

    @Convert(converter = JsonbConverter.class)
    @Column(columnDefinition = "jsonb")
    private Object rules;

    @Convert(converter = JsonbConverter.class)
    @Column(columnDefinition = "jsonb")
    private Object attacks;

    @Convert(converter = JsonbConverter.class)
    @Column(columnDefinition = "jsonb")
    private Object weaknesses;

    @Convert(converter = JsonbConverter.class)
    @Column(columnDefinition = "jsonb")
    private Object resistances;

    @Convert(converter = JsonbConverter.class)
    @Column(columnDefinition = "jsonb")
    private Object retreatCost;

    @Column(name = "set_id")
    private String setId;
}
