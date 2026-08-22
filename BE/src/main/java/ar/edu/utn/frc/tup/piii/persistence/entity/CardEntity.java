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

    private static final String JSONB_COLUMN_TYPE = "jsonb";

    @Id
    private String id;

    private String name;
    private String supertype;
    private String subtype;
    private Integer hp;
    
    @Column(name = "evolves_from")
    private String evolvesFrom;

    @Convert(converter = JsonbConverter.class)
    @Column(columnDefinition = JSONB_COLUMN_TYPE)
    private Object abilities;

    @Convert(converter = JsonbConverter.class)
    @Column(columnDefinition = JSONB_COLUMN_TYPE)
    private Object rules;

    @Convert(converter = JsonbConverter.class)
    @Column(columnDefinition = JSONB_COLUMN_TYPE)
    private Object attacks;

    @Convert(converter = JsonbConverter.class)
    @Column(columnDefinition = JSONB_COLUMN_TYPE)
    private Object weaknesses;

    @Convert(converter = JsonbConverter.class)
    @Column(columnDefinition = JSONB_COLUMN_TYPE)
    private Object resistances;

    @Convert(converter = JsonbConverter.class)
    @Column(columnDefinition = JSONB_COLUMN_TYPE)
    private Object retreatCost;

    @Column(name = "set_id")
    private String setId;
}
