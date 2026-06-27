package ar.edu.utn.frc.tup.piii.dtos;

import ar.edu.utn.frc.tup.piii.persistence.entity.StoreItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreItemDTO {
    private Long id;
    private String name;
    private String description;
    private Integer price;
    private StoreItemType itemType;
    private String imageUrl;
}
