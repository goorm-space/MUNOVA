package com.space.munova.product.domain;



import com.space.munova.core.entity.BaseEntity;
import com.space.munova.product.domain.enums.ProductImageType;
import com.space.munova.product.infra.ProductCategoryConverter;
import com.space.munova.product.infra.ProductImageTypeConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "product_image")
@Builder
public class ProductImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_image_id")
    private Long id;

    @Convert(converter = ProductImageTypeConverter.class)
    private ProductImageType imageType;

    private String originName;

    private String savedName;

    @ColumnDefault("0")
    private boolean isDeleted;

    @ManyToOne
    @JoinColumn(name = "product_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Product product;


    public static ProductImage createDefaultProductImage(ProductImageType imageType,
                                                  String originName,
                                                  String savedName,
                                                  Product product) {
        return ProductImage.builder()
                .imageType(imageType)
                .originName(originName)
                .savedName(savedName)
                .product(product)
                .build();
    }
}
