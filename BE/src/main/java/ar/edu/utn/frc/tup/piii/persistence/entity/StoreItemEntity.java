package ar.edu.utn.frc.tup.piii.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "store_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    
    private String description;
    
    private Integer price;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type")
    private StoreItemType itemType;
    
    @Column(name = "image_url")
    private String imageUrl;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
